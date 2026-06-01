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
import { searchBills as searchBillsApi, searchPoliticians } from './api';
import {
  accomplishments,
  controversies,
  sampleBillVotes,
  sampleBills,
  sampleCitations,
  samplePoliticians,
  sampleStatements,
  sampleVotes,
  securityControls,
  timeline,
} from './mockData';
import type { Bill, BillVote, Politician, SecurityControl } from './types';
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

function App() {
  const [view, setView] = useState<View>('search');
  const [activeTab, setActiveTab] = useState<Tab>('Overview');
  const [query, setQuery] = useState('');
  const [billQuery, setBillQuery] = useState('');
  const [selected, setSelected] = useState<Politician>(samplePoliticians[0]);
  const [selectedBill, setSelectedBill] = useState<Bill>(sampleBills[0]);
  const [savedIds, setSavedIds] = useState<string[]>([samplePoliticians[0].id]);
  const [results, setResults] = useState<Politician[]>(samplePoliticians);
  const [billResults, setBillResults] = useState<Bill[]>(sampleBills);
  const [apiState, setApiState] = useState('Using sample dashboard data');
  const [billApiState, setBillApiState] = useState('Using sample dashboard bill data');
  const latestPoliticianSearch = useRef(0);
  const latestBillSearch = useRef(0);

  useEffect(() => {
    const trimmed = query.trim();

    if (!trimmed) {
      setResults(samplePoliticians);
      setApiState('Using sample dashboard data');
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
      setBillResults(sampleBills);
      setBillApiState('Using sample dashboard bill data');
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
      setResults(samplePoliticians);
      setApiState('Using sample dashboard data');
      setView('search');
      return;
    }

    setApiState('Searching live politician data...');

    try {
      const found = await searchPoliticians(searchTerm);
      if (searchId !== latestPoliticianSearch.current) return;
      if (found.length > 0) {
        setResults(found);
        setSelected(found[0]);
        setApiState('Connected to API');
        setView('search');
      } else {
        const local = filterSamplePoliticians(trimmed);
        setResults(local);
        if (local[0]) setSelected(local[0]);
        setApiState(local.length > 0 ? 'No API matches, showing sample data' : 'No matches found');
        setView('search');
      }
    } catch {
      if (searchId !== latestPoliticianSearch.current) return;
      const local = filterSamplePoliticians(searchTerm);
      setResults(local);
      if (local[0]) setSelected(local[0]);
      setApiState('API unavailable, filtered sample data');
      setView('search');
    }
  }

  const supported = sampleVotes.filter((vote) => vote.voteType === 'YEA');
  const opposed = sampleVotes.filter((vote) => vote.voteType === 'NAY');

  function openPolitician(politician: Politician) {
    setSelected(politician);
    setActiveTab('Overview');
    setView('profile');
  }

  function openBill(bill: Bill) {
    setSelectedBill(bill);
    setView('billDetail');
  }

  function toggleSaved(id: string) {
    setSavedIds((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));
  }

  async function runBillSearch(searchTerm = billQuery.trim()) {
    const searchId = latestBillSearch.current + 1;
    latestBillSearch.current = searchId;
    const trimmed = searchTerm.trim();
    setBillQuery(trimmed);
    setView('bills');

    if (!trimmed) {
      setBillResults(sampleBills);
      setBillApiState('Using sample dashboard bill data');
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
        const local = filterSampleBills(trimmed);
        setBillResults(local);
        setBillApiState(local.length > 0 ? 'No API matches, showing sample bill data' : 'No bill matches found');
        if (local[0]) setSelectedBill(local[0]);
      }
    } catch {
      if (searchId !== latestBillSearch.current) return;
      const local = filterSampleBills(trimmed);
      setBillResults(local);
      setBillApiState('API unavailable, filtered sample bill data');
      if (local[0]) setSelectedBill(local[0]);
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
                  setApiState(value.trim() ? 'Searching live politician data...' : 'Using sample dashboard data');
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
            <p>{apiState}</p>
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
                  setBillApiState(value.trim() ? 'Searching live bill data...' : 'Using sample dashboard bill data');
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
            <p>{billApiState}</p>
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
          </div>
        )}

        {view === 'profile' && <nav className="tab-list" aria-label="Dashboard sections">
          {tabs.map((tab) => (
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
        {view === 'search' && <SearchLanding results={results} onOpenPolitician={openPolitician} />}
        {view === 'saved' && <SavedPoliticians savedIds={savedIds} onOpenPolitician={openPolitician} />}
        {view === 'bills' && <BillSearchLanding bills={billResults} onOpenBill={openBill} />}
        {view === 'billDetail' && <BillDetail bill={selectedBill} onBack={() => setView('bills')} onOpenPolitician={openPolitician} />}
        {view === 'profile' && (
          <>
            <Header politician={selected} saved={savedIds.includes(selected.id)} onToggleSaved={() => toggleSaved(selected.id)} />
            {activeTab === 'Overview' && <Overview politician={selected} />}
            {activeTab === 'Votes' && <Votes supportedCount={supported.length} opposedCount={opposed.length} onOpenBill={openBill} />}
            {activeTab === 'Bills' && <Bills onOpenBill={openBill} />}
            {activeTab === 'Statements' && <Statements />}
            {activeTab === 'Controversies' && <Controversies />}
            {activeTab === 'Citations' && <Citations />}
            {activeTab === 'Timeline' && <Timeline />}
            {activeTab === 'Security' && <SecurityArchitecture />}
          </>
        )}
      </main>
    </div>
  );
}

function filterSamplePoliticians(query: string): Politician[] {
  const value = query.trim().toLowerCase();
  if (!value) return samplePoliticians;
  return samplePoliticians.filter((politician) =>
    `${politician.firstName} ${politician.lastName} ${politician.party} ${politician.state} ${politician.office}`
      .toLowerCase()
      .includes(value),
  );
}

function filterSampleBills(query: string): Bill[] {
  const value = query.trim().toLowerCase();
  if (!value) return sampleBills;
  return sampleBills.filter((bill) =>
    `${bill.billNumber} ${bill.title} ${bill.description ?? ''} ${bill.sponsor ?? ''} ${bill.status}`
      .toLowerCase()
      .includes(value),
  );
}

function Header({ politician, saved, onToggleSaved }: { politician: Politician; saved: boolean; onToggleSaved: () => void }) {
  return (
    <section className="profile-header">
      <div className="avatar" aria-hidden="true">
        {politician.firstName[0]}{politician.lastName[0]}
      </div>
      <div>
        <div className="eyebrow">{politician.party} · {politician.state}</div>
        <h1>{politician.firstName} {politician.lastName}</h1>
        <p>{politician.office}</p>
      </div>
      <div className="header-metrics">
        <Metric label="Trust avg" value="82%" tone="good" />
        <Metric label="Citations" value="41" />
        <Metric label="Open risks" value="3" tone="warn" />
        <button type="button" className="primary-action" onClick={onToggleSaved}>{saved ? 'Saved' : 'Save'}</button>
      </div>
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

function SearchLanding({ results, onOpenPolitician }: { results: Politician[]; onOpenPolitician: (politician: Politician) => void }) {
  return (
    <section className="stack">
      <div className="hero-panel">
        <div className="eyebrow">Neutral public record</div>
        <h1>Search who represents you.</h1>
        <p>Find state and federal politicians, then review what they voted for, sponsored, said, and had reported about them.</p>
      </div>
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
        </div>
      </Panel>
    </section>
  );
}

function SavedPoliticians({ savedIds, onOpenPolitician }: { savedIds: string[]; onOpenPolitician: (politician: Politician) => void }) {
  const saved = samplePoliticians.filter((politician) => savedIds.includes(politician.id));

  return (
    <Panel title="Saved Politicians" icon={<BadgeCheck size={18} />}>
      <div className="card-list">
        {saved.map((politician) => (
          <button key={politician.id} type="button" className="record-card" onClick={() => onOpenPolitician(politician)}>
            <span className="small-avatar">{politician.firstName[0]}{politician.lastName[0]}</span>
            <div>
              <strong>{politician.firstName} {politician.lastName}</strong>
              <small>{politician.office} · {politician.state}</small>
            </div>
          </button>
        ))}
        {saved.length === 0 && <p className="body-copy">No saved politicians yet.</p>}
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

function Overview({ politician }: { politician: Politician }) {
  return (
    <section className="grid two">
      <Panel title="Biography" icon={<UserRound size={18} />}>
        <p className="body-copy">{politician.biography}</p>
        <div className="fact-grid">
          <Fact label="Office" value={politician.office} />
          <Fact label="State" value={politician.state} />
          <Fact label="Term start" value={politician.startDate} />
          <Fact label="Current party" value={politician.party} />
        </div>
      </Panel>

      <Panel title="Accountability Snapshot" icon={<BadgeCheck size={18} />}>
        <div className="score-stack">
          <ScoreRow label="Verified records" value={91} />
          <ScoreRow label="Direct quotes" value={84} />
          <ScoreRow label="Unresolved claims" value={28} inverse />
          <ScoreRow label="Opinion reliance" value={22} inverse />
        </div>
      </Panel>

      <Panel title="Accomplishments" icon={<Gavel size={18} />}>
        <ul className="clean-list">
          {accomplishments.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </Panel>

      <Panel title="Recent Activity" icon={<CalendarClock size={18} />}>
        <div className="mini-timeline">
          {timeline.slice(0, 4).map((item) => (
            <div key={item.id}>
              <time>{item.date}</time>
              <strong>{item.title}</strong>
              <span>{item.category}</span>
            </div>
          ))}
        </div>
      </Panel>
    </section>
  );
}

function Votes({ supportedCount, opposedCount, onOpenBill }: { supportedCount: number; opposedCount: number; onOpenBill: (bill: Bill) => void }) {
  return (
    <section className="stack">
      <div className="summary-strip">
        <Metric label="Supported" value={String(supportedCount)} tone="good" />
        <Metric label="Opposed" value={String(opposedCount)} tone="warn" />
        <Metric label="Abstained" value="0" />
      </div>
      <Panel title="Voting Record" icon={<Vote size={18} />}>
        <div className="card-list">
          {sampleVotes.map((vote) => {
            const bill = sampleBills.find((item) => item.id === vote.billId);
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

function Bills({ onOpenBill }: { onOpenBill: (bill: Bill) => void }) {
  const grouped = useMemo(() => ({
    supported: sampleBills.filter((bill) => sampleVotes.some((vote) => vote.billId === bill.id && vote.voteType === 'YEA')),
    opposed: sampleBills.filter((bill) => sampleVotes.some((vote) => vote.billId === bill.id && vote.voteType === 'NAY')),
  }), []);

  return (
    <section className="grid two">
      <Panel title="Bills Supported" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.supported} onOpenBill={onOpenBill} />
      </Panel>
      <Panel title="Bills Opposed" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.opposed} onOpenBill={onOpenBill} />
      </Panel>
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
          <small>Introduced {bill.introducedDate} · Sponsor: {bill.sponsor ?? 'Unknown'}</small>
        </button>
      ))}
    </div>
  );
}

function BillDetail({
  bill,
  onBack,
  onOpenPolitician,
}: {
  bill: Bill;
  onBack: () => void;
  onOpenPolitician: (politician: Politician) => void;
}) {
  const votes = sampleBillVotes[bill.id] ?? [];
  const yea = votes.filter((vote) => vote.voteType === 'YEA');
  const nay = votes.filter((vote) => vote.voteType === 'NAY');
  const abstain = votes.filter((vote) => vote.voteType === 'ABSTAIN');

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
            <Fact label="Sponsor" value={bill.sponsor} />
            <Fact label="Introduced" value={bill.introducedDate} />
            <Fact label="Last action" value={bill.lastActionDate} />
          </div>
        </Panel>
        <Panel title="Source" icon={<Link size={18} />}>
          <p className="body-copy">{bill.billUrl ?? 'Official source URL will appear here after API ingestion is normalized.'}</p>
        </Panel>
      </section>

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

function BillVoteList({ votes, onOpenPolitician }: { votes: BillVote[]; onOpenPolitician: (politician: Politician) => void }) {
  return (
    <div className="card-list">
      {votes.map((vote) => {
        const politician = samplePoliticians.find((item) => item.id === vote.politicianId);
        return (
          <button key={vote.id} type="button" className="record-card" onClick={() => politician && onOpenPolitician(politician)}>
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

function Statements() {
  return (
    <section className="stack">
      {sampleStatements.map((statement) => (
        <Panel key={statement.id} title={statement.title} icon={<Newspaper size={18} />}>
          <div className="statement">
            <div>
              <span>{statement.type}</span>
              <time>{statement.date}</time>
            </div>
            <blockquote>{statement.excerpt}</blockquote>
            <TrustBadge score={statement.trust.score} confidence={statement.trust.confidenceLevel} />
          </div>
        </Panel>
      ))}
    </section>
  );
}

function Controversies() {
  return (
    <section className="grid two">
      {controversies.map((item) => (
        <Panel key={item.title} title={item.title} icon={<AlertTriangle size={18} />}>
          <div className="risk-row">
            <span className={`risk ${item.risk.toLowerCase()}`}>{item.risk}</span>
            <span>{item.status}</span>
          </div>
          <p className="body-copy">{item.summary}</p>
        </Panel>
      ))}
    </section>
  );
}

function Citations() {
  return (
    <Panel title="Source Citations" icon={<Link size={18} />}>
      <DataTable
        columns={['Date', 'Source', 'Quality', 'Used for']}
        rows={sampleCitations.map((citation) => [
          citation.date,
          citation.source,
          citation.sourceQuality,
          citation.citedBy,
        ])}
      />
    </Panel>
  );
}

function Timeline() {
  return (
    <Panel title="Timeline of Activity" icon={<CalendarClock size={18} />}>
      <div className="timeline">
        {timeline.map((item) => (
          <article key={item.id}>
            <time>{item.date}</time>
            <div>
              <span>{item.category}</span>
              <h3>{item.title}</h3>
              <p>{item.description}</p>
            </div>
            <span className={`risk ${item.risk.toLowerCase()}`}>{item.risk}</span>
          </article>
        ))}
      </div>
    </Panel>
  );
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

function ScoreRow({ label, value, inverse }: { label: string; value: number; inverse?: boolean }) {
  const good = inverse ? value < 35 : value > 70;
  return (
    <div className="score-row">
      <span>{label}</span>
      <div><i style={{ width: `${value}%` }} className={good ? 'good' : 'warn'} /></div>
      <strong>{value}%</strong>
    </div>
  );
}

function TrustBadge({ score, confidence }: { score: number; confidence: string }) {
  return (
    <div className="trust-badge">
      <ShieldCheck size={16} aria-hidden="true" />
      <span>{Math.round(score * 100)}%</span>
      <strong>{confidence}</strong>
    </div>
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
