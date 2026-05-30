import React, { useMemo, useState } from 'react';
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
import { searchPoliticians } from './api';
import {
  accomplishments,
  controversies,
  sampleBills,
  sampleCitations,
  samplePoliticians,
  sampleStatements,
  sampleVotes,
  securityControls,
  timeline,
} from './mockData';
import type { Bill, Politician, SecurityControl } from './types';
import './styles.css';

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
  const [activeTab, setActiveTab] = useState<Tab>('Overview');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Politician>(samplePoliticians[0]);
  const [results, setResults] = useState<Politician[]>(samplePoliticians);
  const [apiState, setApiState] = useState('Using sample dashboard data');

  async function runSearch() {
    const trimmed = query.trim();
    if (!trimmed) {
      setResults(samplePoliticians);
      setApiState('Using sample dashboard data');
      return;
    }

    try {
      const found = await searchPoliticians(trimmed);
      if (found.length > 0) {
        setResults(found);
        setSelected(found[0]);
        setApiState('Connected to API');
      } else {
        setResults([]);
        setApiState('No API matches found');
      }
    } catch {
      const local = samplePoliticians.filter((p) =>
        `${p.firstName} ${p.lastName} ${p.party} ${p.state} ${p.office}`
          .toLowerCase()
          .includes(trimmed.toLowerCase()),
      );
      setResults(local);
      if (local[0]) setSelected(local[0]);
      setApiState('API unavailable, filtered sample data');
    }
  }

  const supported = sampleVotes.filter((vote) => vote.voteType === 'YEA');
  const opposed = sampleVotes.filter((vote) => vote.voteType === 'NAY');

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

        <div className="search-block">
          <label htmlFor="politician-search">Politician search</label>
          <div className="search-row">
            <Search size={18} aria-hidden="true" />
            <input
              id="politician-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') runSearch();
              }}
              placeholder="Name, state, office"
            />
            <button type="button" onClick={runSearch} title="Search politicians">
              <FileSearch size={18} aria-hidden="true" />
            </button>
          </div>
          <p>{apiState}</p>
        </div>

        <div className="result-list">
          {results.map((politician) => (
            <button
              key={politician.id}
              className={politician.id === selected.id ? 'selected result-item' : 'result-item'}
              type="button"
              onClick={() => setSelected(politician)}
            >
              <span>{politician.firstName[0]}{politician.lastName[0]}</span>
              <div>
                <strong>{politician.firstName} {politician.lastName}</strong>
                <small>{politician.office} · {politician.state}</small>
              </div>
            </button>
          ))}
        </div>

        <nav className="tab-list" aria-label="Dashboard sections">
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
        </nav>
      </aside>

      <main className="content">
        <Header politician={selected} />
        {activeTab === 'Overview' && <Overview politician={selected} />}
        {activeTab === 'Votes' && <Votes supportedCount={supported.length} opposedCount={opposed.length} />}
        {activeTab === 'Bills' && <Bills />}
        {activeTab === 'Statements' && <Statements />}
        {activeTab === 'Controversies' && <Controversies />}
        {activeTab === 'Citations' && <Citations />}
        {activeTab === 'Timeline' && <Timeline />}
        {activeTab === 'Security' && <SecurityArchitecture />}
      </main>
    </div>
  );
}

function Header({ politician }: { politician: Politician }) {
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

function Votes({ supportedCount, opposedCount }: { supportedCount: number; opposedCount: number }) {
  return (
    <section className="stack">
      <div className="summary-strip">
        <Metric label="Supported" value={String(supportedCount)} tone="good" />
        <Metric label="Opposed" value={String(opposedCount)} tone="warn" />
        <Metric label="Abstained" value="0" />
      </div>
      <Panel title="Voting Record" icon={<Vote size={18} />}>
        <DataTable
          columns={['Date', 'Bill', 'Title', 'Vote']}
          rows={sampleVotes.map((vote) => [
            vote.voteDate,
            vote.billNumber ?? vote.billId,
            vote.billTitle ?? 'Unknown bill',
            vote.voteType,
          ])}
        />
      </Panel>
    </section>
  );
}

function Bills() {
  const grouped = useMemo(() => ({
    supported: sampleBills.filter((bill) => sampleVotes.some((vote) => vote.billId === bill.id && vote.voteType === 'YEA')),
    opposed: sampleBills.filter((bill) => sampleVotes.some((vote) => vote.billId === bill.id && vote.voteType === 'NAY')),
  }), []);

  return (
    <section className="grid two">
      <Panel title="Bills Supported" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.supported} />
      </Panel>
      <Panel title="Bills Opposed" icon={<BookOpen size={18} />}>
        <BillList bills={grouped.opposed} />
      </Panel>
    </section>
  );
}

function BillList({ bills }: { bills: Bill[] }) {
  return (
    <div className="bill-list">
      {bills.map((bill) => (
        <article key={bill.id}>
          <div>
            <strong>{bill.billNumber}</strong>
            <span>{bill.status}</span>
          </div>
          <h3>{bill.title}</h3>
          <p>{bill.description}</p>
          <small>Introduced {bill.introducedDate}</small>
        </article>
      ))}
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
