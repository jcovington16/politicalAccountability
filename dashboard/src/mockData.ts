import type {
  Bill,
  Citation,
  Politician,
  SecurityControl,
  Statement,
  TimelineItem,
  VotingRecord,
} from './types';

export const samplePoliticians: Politician[] = [
  {
    id: '550e8400-e29b-41d4-a716-446655440000',
    firstName: 'Alex',
    lastName: 'Rivera',
    party: 'Independent',
    state: 'CO',
    office: 'U.S. Representative',
    biography:
      'Former city auditor focused on procurement transparency, election access, and rural broadband oversight.',
    profileImageUrl: '',
    startDate: '2023-01-03',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440001',
    firstName: 'Morgan',
    lastName: 'Lee',
    party: 'Democratic',
    state: 'CA',
    office: 'State Senator',
    biography:
      'Legislator focused on housing affordability, public records reform, and consumer data protection.',
    profileImageUrl: '',
    startDate: '2021-01-04',
  },
];

export const sampleVotes: VotingRecord[] = [
  {
    id: 'vote-1',
    politicianId: samplePoliticians[0].id,
    billId: 'bill-1',
    billNumber: 'HR-204',
    billTitle: 'Public Contract Disclosure Act',
    voteType: 'YEA',
    voteDate: '2026-04-18',
  },
  {
    id: 'vote-2',
    politicianId: samplePoliticians[0].id,
    billId: 'bill-2',
    billNumber: 'HR-311',
    billTitle: 'Emergency Procurement Exception Expansion',
    voteType: 'NAY',
    voteDate: '2026-03-09',
  },
  {
    id: 'vote-3',
    politicianId: samplePoliticians[0].id,
    billId: 'bill-3',
    billNumber: 'S-78',
    billTitle: 'Election Infrastructure Grants',
    voteType: 'YEA',
    voteDate: '2026-02-14',
  },
];

export const sampleBills: Bill[] = [
  {
    id: 'bill-1',
    billNumber: 'HR-204',
    title: 'Public Contract Disclosure Act',
    description: 'Requires machine-readable publication of state and federal procurement awards.',
    status: 'Passed',
    introducedDate: '2026-01-22',
    lastActionDate: '2026-04-18',
  },
  {
    id: 'bill-2',
    billNumber: 'HR-311',
    title: 'Emergency Procurement Exception Expansion',
    description: 'Expands noncompetitive purchasing windows during emergency declarations.',
    status: 'Pending',
    introducedDate: '2026-02-02',
    lastActionDate: '2026-03-09',
  },
  {
    id: 'bill-3',
    billNumber: 'S-78',
    title: 'Election Infrastructure Grants',
    description: 'Funds county-level ballot tracking and public audit dashboards.',
    status: 'Passed',
    introducedDate: '2026-01-12',
    lastActionDate: '2026-02-14',
  },
];

export const sampleStatements: Statement[] = [
  {
    id: 'statement-1',
    title: 'Floor statement on contract transparency',
    type: 'Speech',
    date: '2026-04-17',
    excerpt:
      'Taxpayers deserve to know who receives public money, what was promised, and what was delivered.',
    trust: {
      informationType: 'DIRECT_QUOTE',
      sourceQuality: 'PRIMARY_SOURCE',
      citationCount: 3,
      recencyDays: 42,
      confidenceLevel: 'HIGH',
      score: 0.88,
      explanation: 'Direct quote from official floor transcript with supporting citations.',
    },
  },
  {
    id: 'statement-2',
    title: 'Interview on emergency procurement rules',
    type: 'Interview',
    date: '2026-03-10',
    excerpt:
      'The office argued emergency exceptions should stay narrow unless public reporting is strengthened.',
    trust: {
      informationType: 'VERIFIED_FACT',
      sourceQuality: 'REPUTABLE_NEWS',
      citationCount: 2,
      recencyDays: 80,
      confidenceLevel: 'MEDIUM',
      score: 0.73,
      explanation: 'Reported by a reputable outlet and corroborated by vote record.',
    },
  },
];

export const controversies = [
  {
    title: 'Campaign vendor overlap allegation',
    status: 'Unresolved claim',
    risk: 'Medium',
    summary:
      'A watchdog group alleged overlap between campaign vendors and later grant recipients. No official finding has been issued.',
  },
  {
    title: 'Delayed disclosure filing',
    status: 'Verified fact',
    risk: 'Low',
    summary: 'A quarterly disclosure was filed 12 days late and later corrected.',
  },
];

export const accomplishments = [
  'Sponsored procurement disclosure bill that passed committee unanimously.',
  'Published office meeting logs in CSV format.',
  'Supported county election audit grants with bipartisan cosponsors.',
];

export const sampleCitations: Citation[] = [
  {
    id: 'citation-1',
    source: 'Congressional Record',
    sourceQuality: 'OFFICIAL_RECORD',
    url: 'https://example.gov/record/hr204',
    date: '2026-04-18',
    citedBy: 'Vote HR-204',
  },
  {
    id: 'citation-2',
    source: 'State Ethics Filing',
    sourceQuality: 'OFFICIAL_RECORD',
    url: 'https://example.gov/ethics/filing',
    date: '2026-03-28',
    citedBy: 'Delayed disclosure filing',
  },
  {
    id: 'citation-3',
    source: 'Capitol Daily',
    sourceQuality: 'REPUTABLE_NEWS',
    url: 'https://example.news/interview',
    date: '2026-03-10',
    citedBy: 'Interview on emergency procurement rules',
  },
];

export const timeline: TimelineItem[] = [
  {
    id: 'timeline-1',
    date: '2026-04-18',
    category: 'Vote',
    title: 'Voted YEA on HR-204',
    description: 'Supported public contract disclosure requirements.',
    risk: 'Low',
  },
  {
    id: 'timeline-2',
    date: '2026-04-17',
    category: 'Statement',
    title: 'Floor statement on transparency',
    description: 'Direct quote added from official transcript.',
    risk: 'Low',
  },
  {
    id: 'timeline-3',
    date: '2026-03-28',
    category: 'Fact Check',
    title: 'Disclosure filing reviewed',
    description: 'Late filing confirmed and marked corrected.',
    risk: 'Medium',
  },
  {
    id: 'timeline-4',
    date: '2026-03-09',
    category: 'Vote',
    title: 'Voted NAY on HR-311',
    description: 'Opposed emergency procurement exception expansion.',
    risk: 'Low',
  },
];

export const securityControls: SecurityControl[] = [
  {
    id: 'integrity',
    area: 'Data integrity',
    risk: 'High',
    status: 'In Progress',
    owner: 'Platform',
    control: 'Hash ingested records, require citation provenance, and verify schema migrations before import.',
    evidence: 'Liquibase migrations, content_hash fields, citation workflow planned.',
    nextAction: 'Add import batch table with source file hash and row-level validation results.',
  },
  {
    id: 'misinfo',
    area: 'Misinformation risk',
    risk: 'Critical',
    status: 'Designed',
    owner: 'Trust',
    control: 'Separate verified facts, quotes, votes, allegations, opinion, and unresolved claims.',
    evidence: 'Trust scoring model and /trust/score endpoint.',
    nextAction: 'Require trust score on every public-facing claim card.',
  },
  {
    id: 'source-manipulation',
    area: 'Source manipulation',
    risk: 'High',
    status: 'Needs Owner',
    owner: 'Data Ops',
    control: 'Track source reputation, citation count, publisher ownership, and update frequency anomalies.',
    evidence: 'Source quality enum exists; citation tables still needed.',
    nextAction: 'Create source_registry and source_citations migrations.',
  },
  {
    id: 'prompt-injection',
    area: 'Prompt injection',
    risk: 'High',
    status: 'Designed',
    owner: 'AI Safety',
    control: 'Treat scraped text as untrusted data, strip hidden instructions, and isolate model prompts from raw content.',
    evidence: 'Not yet implemented in processing service.',
    nextAction: 'Add sanitizer before NLP/LLM enrichment and log rejected payloads.',
  },
  {
    id: 'scraping',
    area: 'Scraping risks',
    risk: 'Medium',
    status: 'Needs Owner',
    owner: 'Data Ops',
    control: 'Honor robots and rate limits, retain source URL, timestamp, and extractor version.',
    evidence: 'Provenance metadata exists on content items.',
    nextAction: 'Add per-source rate policy config and retry budget.',
  },
  {
    id: 'privacy',
    area: 'Privacy concerns',
    risk: 'High',
    status: 'Designed',
    owner: 'Security',
    control: 'Limit scope to public-interest records, avoid private contact info, and classify sensitive fields.',
    evidence: 'No PII classification yet.',
    nextAction: 'Add privacy review status to ingestion batches.',
  },
  {
    id: 'authorization',
    area: 'Authorization',
    risk: 'Critical',
    status: 'Needs Owner',
    owner: 'Backend',
    control: 'Require role-based access for admin imports, moderation, trust overrides, and audit views.',
    evidence: 'Public read API has no auth yet.',
    nextAction: 'Add auth middleware and roles before write/admin endpoints ship.',
  },
  {
    id: 'audit',
    area: 'Audit logs',
    risk: 'High',
    status: 'Designed',
    owner: 'Platform',
    control: 'Record who changed trust labels, citations, claims, and source quality with before/after values.',
    evidence: 'Audit schema not yet present.',
    nextAction: 'Create audit_log migration and append-only repository.',
  },
  {
    id: 'abuse',
    area: 'Abuse prevention',
    risk: 'Medium',
    status: 'Designed',
    owner: 'Security',
    control: 'Rate-limit search, detect scraping of profiles, and throttle suspicious query patterns.',
    evidence: 'Not yet implemented in gateway.',
    nextAction: 'Add gateway rate limiting and request telemetry.',
  },
];
