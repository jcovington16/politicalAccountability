import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  AlertTriangle,
  BadgeCheck,
  BookOpen,
  CalendarClock,
  FileSearch,
  Gavel,
  Link,
  LockKeyhole,
  Newspaper,
  Search,
  ShieldCheck,
  UserRound,
  Vote,
} from 'lucide-react';
import { ApiError, getBillDetail, getPoliticianClaims, getPoliticianProfile, getPoliticianTimeline, getPublicStatements, globalSearch, searchBills as searchBillsApi, searchPoliticians } from './api';
import { securityControls } from './mockData';
import type { Bill, BillDetail as BillDetailData, BillVote, ClaimRecord, Politician, PoliticianProfile, PublicStatement, SearchResponse, SecurityControl, TimelineAggregate, VotingRecord } from './types';
import './styles.css';

type View = 'search' | 'bills' | 'saved' | 'profile' | 'billDetail';

const tabs = [
  'Overview',
  'Votes',
  'Bills',
  'Statements',
  'Controversies',
  'Citations',
  'Timeline',
  'Security',
] as const;

type Tab = (typeof tabs)[number];
const showInternalTabs = import.meta.env.VITE_SHOW_INTERNAL === 'true';
const visibleTabs = tabs.filter((tab) => showInternalTabs || tab !== 'Security');
type SavedPoliticianSnapshot = {
  politician: Politician;
  savedAt: string;
  latestActivity?: string;
  dataGaps: string[];
};
const savedStorageKey = 'public-record:saved-politicians:v1';
const emptyPolitician: Politician = {
  id: '', firstName: '', lastName: '', party: '', state: '', office: '', startDate: '',
};
const emptyBill: Bill = {
  id: '', billNumber: '', title: '', status: 'Pending', introducedDate: '',
};

function App() {
  const [view, setView] = useState<View>('search');
  const [activeTab, setActiveTab] = useState<Tab>('Overview');
  const [query, setQuery] = useState('');
  const [billQuery, setBillQuery] = useState('');
  const [selected, setSelected] = useState<Politician>(emptyPolitician);
  const [selectedProfile, setSelectedProfile] = useState<PoliticianProfile | null>(null);
  const [profileState, setProfileState] = useState('Live profile not loaded');
  const [selectedBill, setSelectedBill] = useState<Bill>(emptyBill);
  const [selectedBillDetail, setSelectedBillDetail] = useState<BillDetailData | null>(null);
  const [publicStatements, setPublicStatements] = useState<PublicStatement[]>([]);
  const [claims, setClaims] = useState<ClaimRecord[]>([]);
  const [timelineAggregate, setTimelineAggregate] = useState<TimelineAggregate | null>(null);
  const [savedRecords, setSavedRecords] = useState<SavedPoliticianSnapshot[]>(() => loadSavedSnapshots());
  const savedIds = savedRecords.map((record) => record.politician.id);
  const [results, setResults] = useState<Politician[]>([]);
  const [searchResponse, setSearchResponse] = useState<SearchResponse | null>(null);
  const [billResults, setBillResults] = useState<Bill[]>([]);
  const [apiState, setApiState] = useState('Enter a name to search live records');
  const [billApiState, setBillApiState] = useState('Enter a bill number, title, sponsor, or topic');
  const latestPoliticianSearch = useRef(0);
  const latestBillSearch = useRef(0);

  useEffect(() => {
    const trimmed = query.trim();

    if (!trimmed) {
      setResults([]);
      setSearchResponse(null);
      setApiState('Enter a name to search live records');
      return;
    }

    const timeout = window.setTimeout(() => {
      void runSearch(trimmed);
    }, 350);

    return () => window.clearTimeout(timeout);
  }, [query]);

  useEffect(() => {
    const trimmed = billQuery.trim();

    if (!trimmed) {
      setBillResults([]);
      setBillApiState('Enter a bill number, title, sponsor, or topic');
      return;
    }

    const timeout = window.setTimeout(() => {
      void runBillSearch(trimmed);
    }, 350);

    return () => window.clearTimeout(timeout);
  }, [billQuery]);

  async function runSearch(searchTerm = query.trim()) {
    const searchId = latestPoliticianSearch.current + 1;
    latestPoliticianSearch.current = searchId;
    const trimmed = searchTerm.trim();
    if (!trimmed) {
      setResults([]);
      setSearchResponse(null);
      setApiState('Enter a name to search live records');
      setView('search');
      return;
    }

    setApiState('Searching live politician data...');

    try {
      const [found, grouped] = await Promise.all([
        searchPoliticians(searchTerm),
        globalSearch(searchTerm),
      ]);
      if (searchId !== latestPoliticianSearch.current) return;
      setSearchResponse(grouped);
      if (found.length > 0) {
        setResults(found);
        setSelected(found[0]);
        setApiState('Connected to API');
        setView('search');
      } else {
        setResults([]);
        setApiState('No live politician matches found');
        setView('search');
      }
    } catch (error) {
      if (searchId !== latestPoliticianSearch.current) return;
      setResults([]);
      setSearchResponse(null);
      setApiState(error instanceof ApiError && error.status === 429 ? 'Search is busy. Please try again in a minute.' : 'Live API unavailable. No sample records are shown.');
      setView('search');
    }
  }

  const visibleVotes = selectedProfile?.votingRecords ?? [];
  const supported = visibleVotes.filter((vote) => vote.voteType === 'YEA');
  const opposed = visibleVotes.filter((vote) => vote.voteType === 'NAY');

  function openPolitician(politician: Politician) {
    setSelected(politician);
    setSelectedProfile(null);
    setClaims([]);
    setPublicStatements([]);
    setTimelineAggregate(null);
    setProfileState('Loading live profile...');
    setActiveTab('Overview');
    setView('profile');
    void getPoliticianProfile(politician.id)
      .then((profile) => {
        setSelectedProfile(profile);
        setSelected(profile.politician);
        setProfileState('Connected to API profile');
      })
      .catch(() => {
        setProfileState('Live profile unavailable. No sample records are shown.');
      });
    void getPublicStatements(politician.id)
      .then(setPublicStatements)
      .catch(() => setPublicStatements([]));
    void getPoliticianClaims(politician.id)
      .then(setClaims)
      .catch(() => setClaims([]));
    void getPoliticianTimeline(politician.id)
      .then(setTimelineAggregate)
      .catch(() => setTimelineAggregate(null));
  }

  function openBill(bill: Bill) {
    setSelectedBill(bill);
    setSelectedBillDetail(null);
    setView('billDetail');
    void getBillDetail(bill.id)
      .then((detail) => {
        setSelectedBill(detail.bill);
        setSelectedBillDetail(detail);
      })
      .catch(() => setSelectedBillDetail(null));
  }

  function openBillById(id: string) {
    const known = billResults.find((bill) => bill.id === id);
    if (known) {
      openBill(known);
      return;
    }
    setSelectedBill({
      id,
      billNumber: 'Loading',
      title: 'Loading bill detail...',
      status: 'Pending',
      introducedDate: new Date().toISOString().slice(0, 10),
    });
    setSelectedBillDetail(null);
    setView('billDetail');
    void getBillDetail(id)
      .then((detail) => {
        setSelectedBill(detail.bill);
        setSelectedBillDetail(detail);
      })
      .catch(() => setSelectedBillDetail(null));
  }

  useEffect(() => {
    window.localStorage.setItem(savedStorageKey, JSON.stringify(savedRecords));
  }, [savedRecords]);

  function toggleSaved(politician: Politician, profile: PoliticianProfile | null, aggregate: TimelineAggregate | null) {
    setSavedRecords((current) => {
      if (current.some((item) => item.politician.id === politician.id)) {
        return current.filter((item) => item.politician.id !== politician.id);
      }
      return [
        ...current,
        {
          politician,
          savedAt: new Date().toISOString(),
          latestActivity: aggregate?.stats.latestActivityAt,
          dataGaps: profileDataGaps(profile),
        },
      ];
    });
  }

  async function runBillSearch(searchTerm = billQuery.trim()) {
    const searchId = latestBillSearch.current + 1;
    latestBillSearch.current = searchId;
    const trimmed = searchTerm.trim();
    setBillQuery(trimmed);
    setView('bills');

    if (!trimmed) {
      setBillResults([]);
      setBillApiState('Enter a bill number, title, sponsor, or topic');
      return;
    }

    setBillApiState('Searching live bill data...');

    try {
      const found = await searchBillsApi(trimmed);
      if (searchId !== latestBillSearch.current) return;
      if (found.length > 0) {
        setBillResults(found);
        setBillApiState('Connected to API');
        setSelectedBill(found[0]);
      } else {
        setBillResults([]);
        setBillApiState('No live bill matches found');
      }
    } catch (error) {
      if (searchId !== latestBillSearch.current) return;
      setBillResults([]);
      setBillApiState(error instanceof ApiError && error.status === 429 ? 'Bill search is busy. Please try again in a minute.' : 'Live API unavailable. No sample records are shown.');
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <ShieldCheck size={28} aria-hidden="true" />
          <div>
            <strong>Public Record</strong>
            <span>Accountability console</span>
          </div>
        </div>

        <nav className="entry-list" aria-label="Primary app sections">
          <button type="button" className={view === 'search' ? 'active' : ''} onClick={() => setView('search')}>Search</button>
          <button type="button" className={view === 'bills' ? 'active' : ''} onClick={() => setView('bills')}>Bills</button>
          <button type="button" className={view === 'saved' ? 'active' : ''} onClick={() => setView('saved')}>Saved</button>
        </nav>

        {view !== 'bills' && (
          <div className="search-block">
            <label htmlFor="politician-search">Politician search</label>
            <div className="search-row">
              <Search size={18} aria-hidden="true" />
              <input
                id="politician-search"
                value={query}
                onChange={(event) => {
                  const value = event.target.value;
                  setQuery(value);
                  setApiState(value.trim() ? 'Searching live politician data...' : 'Enter a name to search live records');
                }}
              onFocus={() => setView('search')}
              onKeyDown={(event) => {
                if (event.key === 'Enter') runSearch();
              }}
                placeholder="Name, state, office"
              />
              <button type="button" onClick={() => void runSearch()} title="Search politicians">
                <FileSearch size={18} aria-hidden="true" />
              </button>
            </div>
            <p aria-live="polite">{apiState}</p>
          </div>
        )}

        {view === 'bills' && (
          <div className="search-block">
            <label htmlFor="bill-search">Bill search</label>
            <div className="search-row">
              <Search size={18} aria-hidden="true" />
              <input
                id="bill-search"
                value={billQuery}
                onChange={(event) => {
                  const value = event.target.value;
                  setBillQuery(value);
                  setBillApiState(value.trim() ? 'Searching live bill data...' : 'Enter a bill number, title, sponsor, or topic');
                }}
                onFocus={() => setView('bills')}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') runBillSearch();
                }}
                placeholder="Bill number, title, sponsor"
              />
              <button type="button" onClick={() => void runBillSearch()} title="Search bills">
                <FileSearch size={18} aria-hidden="true" />
              </button>
            </div>
            <p aria-live="polite">{billApiState}</p>
          </div>
        )}

        {view !== 'bills' && (
          <div className="result-list">
          {results.map((politician) => (
            <button
              key={politician.id}
              className={politician.id === selected.id ? 'selected result-item' : 'result-item'}
              type="button"
              onClick={() => openPolitician(politician)}
            >
              <span>{politician.firstName[0]}{politician.lastName[0]}</span>
              <div>
                <strong>{politician.firstName} {politician.lastName}</strong>
                <small>{politician.office} · {politician.state}</small>
              </div>
            </button>
          ))}
          {results.length === 0 && <p className="body-copy">No politician matches yet.</p>}
          </div>
        )}

        {view === 'bills' && (
          <div className="result-list">
            {billResults.map((bill) => (
              <button key={bill.id} type="button" className={bill.id === selectedBill.id ? 'selected result-item' : 'result-item'} onClick={() => openBill(bill)}>
                <span>{bill.billNumber.split('-')[0]}</span>
                <div>
                  <strong>{bill.billNumber}</strong>
                  <small>{bill.title}</small>
                </div>
              </button>
            ))}
            {billResults.length === 0 && <p className="body-copy">No bill matches yet.</p>}
          </div>
        )}

        {view === 'profile' && <nav className="tab-list" aria-label="Dashboard sections">
          {visibleTabs.map((tab) => (
            <button
              key={tab}
              type="button"
              className={activeTab === tab ? 'active' : ''}
              onClick={() => setActiveTab(tab)}
            >
              {tab}
            </button>
          ))}
        </nav>}
      </aside>

      <main className="content">
        {view === 'search' && <SearchLanding results={results} searchResponse={searchResponse} onOpenPolitician={openPolitician} onOpenBillById={openBillById} />}
        {view === 'saved' && <SavedPoliticians records={savedRecords} onOpenPolitician={openPolitician} />}
        {view === 'bills' && <BillSearchLanding bills={billResults} onOpenBill={openBill} />}
        {view === 'billDetail' && <BillDetail bill={selectedBill} detail={selectedBillDetail} onBack={() => setView('bills')} onOpenPolitician={openPolitician} />}
        {view === 'profile' && (
          <>
            <Header politician={selected} profile={selectedProfile} profileState={profileState} saved={savedIds.includes(selected.id)} onToggleSaved={() => toggleSaved(selected, selectedProfile, timelineAggregate)} />
            {activeTab === 'Overview' && <Overview politician={selected} profile={selectedProfile} />}
            {activeTab === 'Votes' && <Votes votes={visibleVotes} supportedCount={supported.length} opposedCount={opposed.length} onOpenBill={openBill} />}
            {activeTab === 'Bills' && <Bills profile={selectedProfile} onOpenBill={openBill} />}
            {activeTab === 'Statements' && <Statements statements={publicStatements} />}
            {activeTab === 'Controversies' && <Controversies claims={claims} />}
            {activeTab === 'Citations' && <Citations profile={selectedProfile} />}
            {activeTab === 'Timeline' && <Timeline aggregate={timelineAggregate} />}
            {showInternalTabs && activeTab === 'Security' && <SecurityArchitecture />}
          </>
        )}
      </main>
    </div>
  );
}

function loadSavedSnapshots(): SavedPoliticianSnapshot[] {
  try {
    const raw = window.localStorage.getItem(savedStorageKey);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as SavedPoliticianSnapshot[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function profileDataGaps(profile: PoliticianProfile | null): string[] {
  if (!profile) return ['Live profile not refreshed yet'];
  const gaps: string[] = [];
  if (profile.votingRecords.length === 0) gaps.push('Votes');
  if (profile.billsSponsored.length === 0) gaps.push('Sponsored bills');
  if (profile.citations.length === 0) gaps.push('Citations');
  if (profile.timeline.length === 0) gaps.push('Timeline');
  return gaps.length > 0 ? gaps : ['Core profile data present'];
}

function Header({
  politician,
  profile,
  profileState,
  saved,
  onToggleSaved,
}: {
  politician: Politician;
  profile: PoliticianProfile | null;
  profileState: string;
  saved: boolean;
  onToggleSaved: () => void;
}) {
  const trust = profile?.trustSummary;
  return (
    <section className="profile-header">
      {politician.profileImageUrl ? (
        <img
          className="avatar portrait"
          src={politician.profileImageUrl}
          alt={`${politician.firstName} ${politician.lastName}`}
        />
      ) : (
        <div className="avatar" aria-hidden="true">
          {politician.firstName[0]}{politician.lastName[0]}
        </div>
      )}
      <div>
        <div className="eyebrow">{politician.party} · {politician.state}</div>
        <h1>{politician.firstName} {politician.lastName}</h1>
        <p>{politician.office}</p>
      </div>
      <div className="header-metrics">
        <Metric label="Trust avg" value={trust ? `${Math.round(trust.averageScore * 100)}%` : '—'} tone="good" />
        <Metric label="Citations" value={String(trust?.citationCount ?? 0)} />
        <Metric label="Open risks" value={String(trust?.openRiskCount ?? 0)} tone="warn" />
        <button type="button" className="primary-action" onClick={onToggleSaved}>{saved ? 'Saved' : 'Save'}</button>
      </div>
      <p className="profile-state">{profileState}</p>
    </section>
  );
}

function Metric({ label, value, tone }: { label: string; value: string; tone?: 'good' | 'warn' }) {
  return (
    <div className={`metric ${tone ?? ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function SearchLanding({
  results,
  searchResponse,
  onOpenPolitician,
  onOpenBillById,
}: {
  results: Politician[];
  searchResponse: SearchResponse | null;
  onOpenPolitician: (politician: Politician) => void;
  onOpenBillById: (id: string) => void;
}) {
  const grouped = searchResponse?.groups.filter((group) => group.results.length > 0) ?? [];
  return (
    <section className="stack">
      <div className="hero-panel">
        <div className="eyebrow">Neutral public record</div>
        <h1>Search who represents you.</h1>
        <p>Find state and federal politicians, then review what they voted for, sponsored, said, and had reported about them.</p>
      </div>
      {grouped.length > 0 && (
        <Panel title={`Live Search Results (${searchResponse?.total ?? 0})`} icon={<Search size={18} />}>
          <div className="search-groups">
            {grouped.map((group) => (
              <section key={group.type} className="search-group">
                <h3>{group.label}</h3>
                <div className="card-list">
                  {group.results.map((result) => {
                    const politician = group.type === 'politicians' ? results.find((item) => item.id === result.id) : undefined;
                    const canOpenBill = group.type === 'bills';
                    return (
                      <button
                        key={`${group.type}-${result.id}`}
                        type="button"
                        className="record-card"
                        onClick={() => {
                          if (politician) onOpenPolitician(politician);
                          if (canOpenBill) onOpenBillById(result.id);
                        }}
                      >
                        <span className="small-avatar">{group.type.slice(0, 3).toUpperCase()}</span>
                        <div>
                          <strong>{result.title}</strong>
                          <small>{[result.subtitle, result.source, result.date && formatDisplayDate(result.date)].filter(Boolean).join(' · ')}</small>
                          {result.description && <p>{redactPrivateDisplayText(result.description).slice(0, 180)}</p>}
                          {result.trustContext && <small>{result.trustContext}</small>}
                          {result.reviewWarnings.length > 0 && <small>{result.reviewWarnings.join(' ')}</small>}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </section>
            ))}
          </div>
        </Panel>
      )}
      <Panel title="Politicians" icon={<UserRound size={18} />}>
        <div className="card-list">
          {results.map((politician) => (
            <button key={politician.id} type="button" className="record-card" onClick={() => onOpenPolitician(politician)}>
              <span className="small-avatar">{politician.firstName[0]}{politician.lastName[0]}</span>
              <div>
                <strong>{politician.firstName} {politician.lastName}</strong>
                <small>{politician.office} · {politician.state} · {politician.party}</small>
              </div>
            </button>
          ))}
          {results.length === 0 && <p className="body-copy">No politician matches yet. Try another name, state, office, or party.</p>}
        </div>
      </Panel>
    </section>
  );
}

function SavedPoliticians({ records, onOpenPolitician }: { records: SavedPoliticianSnapshot[]; onOpenPolitician: (politician: Politician) => void }) {
  return (
    <Panel title="Saved Politicians" icon={<BadgeCheck size={18} />}>
      <div className="card-list">
        {records.map(({ politician, savedAt, latestActivity, dataGaps }) => (
          <button key={politician.id} type="button" className="record-card" onClick={() => onOpenPolitician(politician)}>
            <span className="small-avatar">{politician.firstName[0]}{politician.lastName[0]}</span>
            <div>
              <strong>{politician.firstName} {politician.lastName}</strong>
              <small>{politician.office} · {politician.state}</small>
              <small>Saved {formatDisplayDate(savedAt)} · Latest activity {formatDisplayDate(latestActivity)}</small>
              <small>Data gaps: {dataGaps.join(', ')}</small>
            </div>
          </button>
        ))}
        {records.length === 0 && <p className="body-copy">No saved politicians yet.</p>}
      </div>
    </Panel>
  );
}

function BillSearchLanding({ bills, onOpenBill }: { bills: Bill[]; onOpenBill: (bill: Bill) => void }) {
  return (
    <section className="stack">
      <div className="hero-panel">
        <div className="eyebrow">Legislation lookup</div>
        <h1>Search bills and votes.</h1>
        <p>Look up a bill, see who introduced it, when it moved, and how politicians voted.</p>
      </div>
      <Panel title="Bills" icon={<BookOpen size={18} />}>
        <BillList bills={bills} onOpenBill={onOpenBill} />
      </Panel>
    </section>
  );
}

function Overview({ politician, profile }: { politician: Politician; profile: PoliticianProfile | null }) {
  const office = profile?.offices[0];
  const recentActivity = profile?.timeline.length
    ? profile.timeline.slice(0, 4).map((item, index) => ({
      id: `${item.date}-${item.title}-${index}`,
      date: item.date,
      title: item.title,
      category: item.category,
    }))
    : [];
  return (
    <section className="grid two">
      <Panel title="Biography" icon={<UserRound size={18} />}>
        <p className="body-copy">{redactPrivateDisplayText(politician.biography) || 'No official biography has been ingested.'}</p>
        <div className="fact-grid">
          <Fact label="Office" value={politician.office} />
          <Fact label="State" value={politician.state} />
          <Fact label="Term start" value={formatDisplayDate(politician.startDate)} />
          <Fact label="Current party" value={politician.party} />
          {office && <Fact label="Seat" value={office.seatIdentifier} />}
          {profile?.elections[0] && <Fact label="Next election" value={`${profile.elections[0].electionType} ${profile.elections[0].cycleYear}`} />}
        </div>
      </Panel>

      <Panel title="Accountability Snapshot" icon={<BadgeCheck size={18} />}>
        <div className="fact-grid">
          <Fact label="Evidence records" value={String(profile?.trustSummary.records.length ?? 0)} />
          <Fact label="Source citations" value={String(profile?.trustSummary.citationCount ?? 0)} />
          <Fact label="Open review risks" value={String(profile?.trustSummary.openRiskCount ?? 0)} />
          <Fact label="Average trust" value={profile ? `${Math.round(profile.trustSummary.averageScore * 100)}%` : 'Not scored'} />
        </div>
      </Panel>

      <Panel title="Accomplishments" icon={<Gavel size={18} />}>
        <p className="body-copy">No reviewed accomplishment records are available. The app does not infer accomplishments from unrelated activity.</p>
      </Panel>

      <Panel title="Recent Activity" icon={<CalendarClock size={18} />}>
        <div className="mini-timeline">
          {recentActivity.map((item) => (
            <div key={item.id}>
              <time>{formatDisplayDate(item.date)}</time>
              <strong>{item.title}</strong>
              <span>{item.category}</span>
            </div>
          ))}
          {recentActivity.length === 0 && <p className="body-copy">No live activity records have been ingested.</p>}
        </div>
      </Panel>
    </section>
  );
}

function Votes({
  votes,
  supportedCount,
  opposedCount,
  onOpenBill,
}: {
  votes: VotingRecord[];
  supportedCount: number;
  opposedCount: number;
  onOpenBill: (bill: Bill) => void;
}) {
  return (
    <section className="stack">
      <div className="summary-strip">
        <Metric label="Supported" value={String(supportedCount)} tone="good" />
        <Metric label="Opposed" value={String(opposedCount)} tone="warn" />
        <Metric label="Abstained" value="0" />
      </div>
      <Panel title="Voting Record" icon={<Vote size={18} />}>
        <div className="card-list">
          {votes.map((vote) => {
            const bill = {
              id: vote.billId,
              billNumber: vote.billNumber ?? 'Unknown',
              title: vote.billTitle ?? 'Unknown bill',
              status: 'Pending' as const,
              introducedDate: vote.voteDate,
              billUrl: vote.billUrl,
            };
            return (
              <button key={vote.id} type="button" className="vote-card" onClick={() => bill && onOpenBill(bill)}>
                <time>{vote.voteDate}</time>
                <strong>{vote.billNumber ?? vote.billId}</strong>
                <span>{vote.billTitle ?? 'Unknown bill'}</span>
                <i className={vote.voteType === 'YEA' ? 'good' : vote.voteType === 'NAY' ? 'warn' : ''}>{vote.voteType}</i>
              </button>
            );
          })}
        </div>
      </Panel>
    </section>
  );
}

function Bills({ profile, onOpenBill }: { profile: PoliticianProfile | null; onOpenBill: (bill: Bill) => void }) {
  const grouped = useMemo(() => ({
    supported: profile?.billsSupported ?? [],
    opposed: profile?.billsOpposed ?? [],
    sponsored: profile?.billsSponsored ?? [],
  }), [profile]);

  return (
    <section className="grid two">
      <Panel title="Bills Supported" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.supported} onOpenBill={onOpenBill} />
      </Panel>
      <Panel title="Bills Opposed" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.opposed} onOpenBill={onOpenBill} />
      </Panel>
      {grouped.sponsored.length > 0 && (
        <Panel title="Bills Sponsored" icon={<BookOpen size={18} />}>
          <BillList bills={grouped.sponsored} onOpenBill={onOpenBill} />
        </Panel>
      )}
    </section>
  );
}

function BillList({ bills, onOpenBill }: { bills: Bill[]; onOpenBill: (bill: Bill) => void }) {
  return (
    <div className="bill-list">
      {bills.map((bill) => (
        <button key={bill.id} type="button" className="bill-card" onClick={() => onOpenBill(bill)}>
          <div>
            <strong>{bill.billNumber}</strong>
            <span>{bill.status}</span>
          </div>
          <h3>{bill.title}</h3>
          <p>{bill.description}</p>
          <small>Introduced {formatDisplayDate(bill.introducedDate)} · Sponsor: {bill.sponsor ?? 'Unknown'}</small>
        </button>
      ))}
      {bills.length === 0 && <p className="body-copy">No bills found. Try a bill number, title, sponsor, or topic.</p>}
    </div>
  );
}

function BillDetail({
  bill,
  detail,
  onBack,
  onOpenPolitician,
}: {
  bill: Bill;
  detail: BillDetailData | null;
  onBack: () => void;
  onOpenPolitician: (politician: Politician) => void;
}) {
  const votes = (detail?.votes ?? []).map((vote) => ({
      id: vote.id,
      politicianId: vote.politicianId,
      politicianName: vote.politicianName ?? 'Unknown politician',
      party: vote.party ?? 'Unknown',
      state: vote.state ?? 'Unknown',
      voteType: vote.voteType,
      voteDate: formatDisplayDate(vote.voteDate),
    }));
  const yea = votes.filter((vote) => vote.voteType === 'YEA');
  const nay = votes.filter((vote) => vote.voteType === 'NAY');
  const abstain = votes.filter((vote) => vote.voteType === 'ABSTAIN');
  const sponsorNames = detail?.sponsors.map((sponsor) => sponsor.politicianName).join(', ') || bill.sponsor;

  return (
    <section className="stack">
      <button type="button" className="text-action" onClick={onBack}>Back to bills</button>
      <section className="profile-header">
        <div className="bill-avatar" aria-hidden="true">{bill.billNumber.split('-')[0]}</div>
        <div>
          <div className="eyebrow">{bill.jurisdiction ?? 'Federal'} · {bill.chamber ?? 'Legislature'}</div>
          <h1>{bill.billNumber}</h1>
          <p>{bill.title}</p>
        </div>
        <div className="header-metrics">
          <Metric label="For" value={String(yea.length)} tone="good" />
          <Metric label="Against" value={String(nay.length)} tone="warn" />
          <Metric label="Abstain" value={String(abstain.length)} />
        </div>
      </section>

      <section className="grid two">
        <Panel title="Legislation Details" icon={<BookOpen size={18} />}>
          <p className="body-copy">{bill.description}</p>
          <div className="fact-grid">
            <Fact label="Status" value={bill.status} />
            <Fact label="Sponsor" value={sponsorNames} />
            <Fact label="Introduced" value={formatDisplayDate(bill.introducedDate)} />
            <Fact label="Last action" value={formatDisplayDate(bill.lastActionDate)} />
          </div>
        </Panel>
        <Panel title="Source" icon={<Link size={18} />}>
          <p className="body-copy">{bill.billUrl ?? 'Official source URL will appear here after API ingestion is normalized.'}</p>
        </Panel>
      </section>

      {detail && (
        <section className="grid two">
          <Panel title="Sponsors" icon={<UserRound size={18} />}>
            <SponsorList sponsors={detail.sponsors} emptyText="No primary sponsor recorded." />
          </Panel>
          <Panel title="Cosponsors" icon={<UserRound size={18} />}>
            <SponsorList sponsors={detail.cosponsors} emptyText="No cosponsors recorded." />
          </Panel>
          <Panel title="Bill Actions" icon={<CalendarClock size={18} />}>
            <div className="mini-timeline">
              {detail.actions.map((action) => (
                <div key={action.id}>
                  <time>{formatDisplayDate(action.actionDate)}</time>
                  <strong>{action.actionText}</strong>
                  <span>Official action</span>
                </div>
              ))}
              {detail.actions.length === 0 && <p className="body-copy">No official actions recorded.</p>}
            </div>
          </Panel>
          <Panel title="Citations" icon={<Link size={18} />}>
            <div className="card-list">
              {detail.citations.map((citation) => (
                <a key={citation.id} className="record-card" href={citation.url} target="_blank" rel="noreferrer">
                  <span className="small-avatar">SRC</span>
                  <div>
                    <strong>{citation.title ?? citation.sourceName ?? 'Official source'}</strong>
                    <small>{citation.sourceQuality}</small>
                  </div>
                </a>
              ))}
              {detail.citations.length === 0 && <p className="body-copy">No citations recorded.</p>}
            </div>
          </Panel>
        </section>
      )}

      <section className="grid two">
        <Panel title="Votes For" icon={<Vote size={18} />}>
          <BillVoteList votes={yea} onOpenPolitician={onOpenPolitician} />
        </Panel>
        <Panel title="Votes Against" icon={<Vote size={18} />}>
          <BillVoteList votes={nay} onOpenPolitician={onOpenPolitician} />
        </Panel>
      </section>
    </section>
  );
}

function SponsorList({ sponsors, emptyText }: { sponsors: BillDetailData['sponsors']; emptyText: string }) {
  return (
    <div className="card-list">
      {sponsors.map((sponsor) => (
        <div key={sponsor.id} className="record-card">
          <span className="small-avatar">{sponsor.politicianName.split(' ').map((part) => part[0]).join('').slice(0, 2)}</span>
          <div>
            <strong>{sponsor.politicianName}</strong>
            <small>{sponsor.sponsorType} · {sponsor.party ?? 'Unknown party'} · {sponsor.state ?? 'Unknown state'}</small>
          </div>
        </div>
      ))}
      {sponsors.length === 0 && <p className="body-copy">{emptyText}</p>}
    </div>
  );
}

function BillVoteList({ votes, onOpenPolitician }: { votes: BillVote[]; onOpenPolitician: (politician: Politician) => void }) {
  return (
    <div className="card-list">
      {votes.map((vote) => {
        const names = vote.politicianName.trim().split(/\s+/);
        const politician: Politician = {
          id: vote.politicianId,
          firstName: names[0] ?? '',
          lastName: names.slice(1).join(' '),
          party: vote.party,
          state: vote.state,
          office: 'Public official',
          startDate: '',
        };
        return (
          <button key={vote.id} type="button" className="record-card" onClick={() => onOpenPolitician(politician)}>
            <span className="small-avatar">{vote.politicianName.split(' ').map((part) => part[0]).join('').slice(0, 2)}</span>
            <div>
              <strong>{vote.politicianName}</strong>
              <small>{vote.voteType} · {vote.party} · {vote.state} · {vote.voteDate}</small>
            </div>
          </button>
        );
      })}
      {votes.length === 0 && <p className="body-copy">No votes recorded.</p>}
    </div>
  );
}

function Statements({ statements }: { statements: PublicStatement[] }) {
  if (statements.length > 0) {
    return (
      <section className="stack">
        {statements.map((statement) => (
          <Panel key={statement.id} title={statement.title} icon={<Newspaper size={18} />}>
            <div className="statement">
              <div>
                <span>{statement.statementType.replace(/_/g, ' ')}</span>
                <time>{formatDisplayDate(statement.statementDate)}</time>
              </div>
              <blockquote>{redactPrivateDisplayText(statement.quote ?? statement.body ?? 'No statement text recorded.')}</blockquote>
              <div className="risk-row">
                <span className={`risk ${statement.suspiciousContent ? 'high' : 'low'}`}>
                  {statement.suspiciousContent ? 'Needs review' : 'Publishable'}
                </span>
                <span>{statement.venue ?? 'Unknown venue'}</span>
              </div>
            </div>
          </Panel>
        ))}
      </section>
    );
  }

  return <p className="body-copy">No live public statements have been ingested for this politician.</p>;
}

function Controversies({ claims }: { claims: ClaimRecord[] }) {
  if (claims.length > 0) {
    return (
      <section className="grid two">
        {claims.map((claim) => (
          <Panel key={claim.id} title={claim.claimType.replace(/_/g, ' ')} icon={<AlertTriangle size={18} />}>
            <div className="risk-row">
              <span className={`risk ${claim.publishable ? 'medium' : 'high'}`}>
                {claim.status}
              </span>
              <span>{claim.trust.confidenceLevel} confidence · {claim.citationCount} citation{claim.citationCount === 1 ? '' : 's'}</span>
            </div>
            <p className="body-copy">{redactPrivateDisplayText(claim.claimText)}</p>
            {claim.reviewWarnings.length > 0 && (
              <ul className="clean-list">
                {claim.reviewWarnings.map((warning) => <li key={warning}>{warning}</li>)}
              </ul>
            )}
            {claim.factChecks.length > 0 && (
              <div className="mini-timeline">
                {claim.factChecks.map((factCheck) => (
                  <div key={factCheck.id}>
                    <time>{formatDisplayDate(factCheck.checkedAt)}</time>
                    <strong>{factCheck.rating}</strong>
                    <span>{factCheck.summary}</span>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        ))}
      </section>
    );
  }

  return <p className="body-copy">No reviewed live claim records are available. Missing data is not evidence that no controversies exist.</p>;
}

function Citations({ profile }: { profile: PoliticianProfile | null }) {
  const liveCitations = profile?.citations ?? [];

  if (liveCitations.length > 0) {
    return (
      <Panel title="Source Citations" icon={<Link size={18} />}>
        <DataTable
          columns={['Date', 'Source', 'Quality', 'Used for']}
          rows={liveCitations.map((citation) => [
            formatDisplayDate(citation.publishedAt ?? citation.retrievedAt),
            citation.sourceName ?? 'Unknown source',
            citation.sourceQuality ?? 'UNKNOWN',
            citation.citationType ?? 'Evidence',
          ])}
        />
      </Panel>
    );
  }

  return <Panel title="Source Citations" icon={<Link size={18} />}><p className="body-copy">No live source citations are attached to this profile.</p></Panel>;
}

function Timeline({ aggregate }: { aggregate: TimelineAggregate | null }) {
  const [activeCategory, setActiveCategory] = useState('All');
  const categories = aggregate ? ['All', ...Object.keys(aggregate.stats.byCategory)] : ['All'];
  const visibleItems = aggregate
    ? aggregate.items.filter((item) => activeCategory === 'All' || item.category === activeCategory)
    : [];

  if (aggregate && aggregate.items.length > 0) {
    return (
      <Panel title="Timeline of Activity" icon={<CalendarClock size={18} />}>
        <div className="metric-row">
          <Metric label="Events" value={String(aggregate.stats.total)} />
          <Metric label="Publishable" value={String(aggregate.stats.publishableCount)} />
          <Metric label="Needs review" value={String(aggregate.stats.reviewRequiredCount)} />
        </div>
        <div className="filter-row" aria-label="Timeline filters">
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              className={activeCategory === category ? 'filter-chip active' : 'filter-chip'}
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>
        <div className="timeline">
          {visibleItems.map((item) => (
            <article key={item.id}>
              <time>{formatDisplayDate(item.date)}</time>
              <div>
                <span>{item.category} · {item.evidenceType.replace(/_/g, ' ')}</span>
                <h3>{item.title}</h3>
                <p>{redactPrivateDisplayText(item.description)}</p>
                {item.sourceUrl && (
                  <a href={item.sourceUrl} target="_blank" rel="noreferrer">{item.sourceName ?? 'Source'}</a>
                )}
                {item.warnings.length > 0 && (
                  <ul className="clean-list">
                    {item.warnings.map((warning) => <li key={warning}>{warning}</li>)}
                  </ul>
                )}
              </div>
              <span className={`risk ${item.publishable ? 'low' : 'high'}`}>
                {item.publishable ? 'Publishable' : 'Review'}
              </span>
            </article>
          ))}
        </div>
      </Panel>
    );
  }

  return <Panel title="Timeline of Activity" icon={<CalendarClock size={18} />}><p className="body-copy">No live timeline events have been ingested for this politician.</p></Panel>;
}

function SecurityArchitecture() {
  return (
    <section className="stack">
      <div className="security-header">
        <div>
          <h2>Security and Integrity Controls</h2>
          <p>Senior-security view of data integrity, misinformation, source abuse, privacy, authorization, and audit readiness.</p>
        </div>
        <LockKeyhole size={28} aria-hidden="true" />
      </div>
      <div className="control-grid">
        {securityControls.map((control) => <SecurityCard key={control.id} control={control} />)}
      </div>
    </section>
  );
}

function SecurityCard({ control }: { control: SecurityControl }) {
  return (
    <article className="control-card">
      <div className="control-title">
        <strong>{control.area}</strong>
        <span className={`risk ${control.risk.toLowerCase()}`}>{control.risk}</span>
      </div>
      <p>{control.control}</p>
      <dl>
        <div><dt>Status</dt><dd>{control.status}</dd></div>
        <div><dt>Owner</dt><dd>{control.owner}</dd></div>
        <div><dt>Evidence</dt><dd>{control.evidence}</dd></div>
        <div><dt>Next</dt><dd>{control.nextAction}</dd></div>
      </dl>
    </article>
  );
}

function Panel({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <section className="panel">
      <header>
        <div>{icon}<h2>{title}</h2></div>
      </header>
      {children}
    </section>
  );
}

function Fact({ label, value }: { label: string; value?: string }) {
  return (
    <div className="fact">
      <span>{label}</span>
      <strong>{value || 'Unknown'}</strong>
    </div>
  );
}

function formatDisplayDate(value?: string | [number, number, number]): string {
  if (!value) return 'Unknown';

  if (Array.isArray(value)) {
    const [year, month, day] = value;
    return formatDateParts(year, month, day);
  }

  const isoMatch = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (isoMatch) {
    return formatDateParts(Number(isoMatch[1]), Number(isoMatch[2]), Number(isoMatch[3]));
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(parsed);
}

function redactPrivateDisplayText(value?: string): string {
  if (!value) return '';
  return value
    .replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, '[redacted email]')
    .replace(/(?:\+?1[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)\d{3}[\s.-]?\d{4}/g, '[redacted phone]')
    .replace(/\b\d{3}-\d{2}-\d{4}\b/g, '[redacted ssn]');
}

function formatDateParts(year: number, month: number, day: number): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(
    new Date(Date.UTC(year, month - 1, day, 12)),
  );
}

function DataTable({ columns, rows }: { columns: string[]; rows: string[][] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.join('-')}>
              {row.map((cell, index) => <td key={`${cell}-${index}`}>{cell}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
