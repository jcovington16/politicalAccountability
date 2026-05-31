import type { Article, Bill, BillVote, Citation, IssueStance, Politician, TimelineItem, VoteRecord } from './types';

export const politicians: Politician[] = [
  {
    id: '550e8400-e29b-41d4-a716-446655440000',
    firstName: 'Alex',
    lastName: 'Rivera',
    party: 'Independent',
    state: 'CO',
    office: 'U.S. Representative',
    biography:
      'Former city auditor focused on procurement transparency, election access, rural broadband oversight, and open public records.',
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
      'Legislator focused on housing affordability, public records reform, consumer data protection, and transit funding.',
    startDate: '2021-01-04',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440002',
    firstName: 'Taylor',
    lastName: 'Morgan',
    party: 'Republican',
    state: 'CO',
    office: 'U.S. Representative Candidate',
    biography:
      'Former emergency management director focused on disaster response authority, business regulation, and infrastructure resilience.',
    startDate: '2026-01-01',
  },
];

export const votes: VoteRecord[] = [
  {
    id: 'vote-1',
    billId: 'bill-hr-204',
    billNumber: 'HR-204',
    title: 'Public Contract Disclosure Act',
    vote: 'YEA',
    date: '2026-04-18',
  },
  {
    id: 'vote-2',
    billId: 'bill-hr-311',
    billNumber: 'HR-311',
    title: 'Emergency Procurement Exception Expansion',
    vote: 'NAY',
    date: '2026-03-09',
  },
  {
    id: 'vote-3',
    billId: 'bill-s-78',
    billNumber: 'S-78',
    title: 'Election Infrastructure Grants',
    vote: 'YEA',
    date: '2026-02-14',
  },
];

export const bills: Bill[] = [
  {
    id: 'bill-hr-204',
    billNumber: 'HR-204',
    title: 'Public Contract Disclosure Act',
    summary: 'Requires machine-readable publication of public procurement awards, vendors, delivery timelines, and final contract values.',
    status: 'Passed',
    introducedDate: '2026-01-22',
    lastActionDate: '2026-04-18',
    sponsor: 'Alex Rivera',
    chamber: 'U.S. House',
    jurisdiction: 'Federal',
    sourceUrl: 'https://example.gov/bills/hr204',
  },
  {
    id: 'bill-hr-311',
    billNumber: 'HR-311',
    title: 'Emergency Procurement Exception Expansion',
    summary: 'Expands noncompetitive purchasing windows during emergency declarations while requiring less frequent public reporting.',
    status: 'Pending',
    introducedDate: '2026-02-02',
    lastActionDate: '2026-03-09',
    sponsor: 'Taylor Morgan',
    chamber: 'U.S. House',
    jurisdiction: 'Federal',
    sourceUrl: 'https://example.gov/bills/hr311',
  },
  {
    id: 'bill-s-78',
    billNumber: 'S-78',
    title: 'Election Infrastructure Grants',
    summary: 'Funds county-level ballot tracking, public audit dashboards, and cybersecurity upgrades for election offices.',
    status: 'Passed',
    introducedDate: '2026-01-12',
    lastActionDate: '2026-02-14',
    sponsor: 'Alex Rivera',
    chamber: 'U.S. Senate',
    jurisdiction: 'Federal',
    sourceUrl: 'https://example.gov/bills/s78',
  },
];

export const billVotes: Record<string, BillVote[]> = {
  'bill-hr-204': [
    { id: 'bv-1', politicianId: '550e8400-e29b-41d4-a716-446655440000', politicianName: 'Alex Rivera', party: 'Independent', state: 'CO', vote: 'YEA', date: '2026-04-18' },
    { id: 'bv-2', politicianId: '550e8400-e29b-41d4-a716-446655440001', politicianName: 'Morgan Lee', party: 'Democratic', state: 'CA', vote: 'YEA', date: '2026-04-18' },
    { id: 'bv-3', politicianId: '550e8400-e29b-41d4-a716-446655440002', politicianName: 'Taylor Morgan', party: 'Republican', state: 'CO', vote: 'NAY', date: '2026-04-18' },
  ],
  'bill-hr-311': [
    { id: 'bv-4', politicianId: '550e8400-e29b-41d4-a716-446655440000', politicianName: 'Alex Rivera', party: 'Independent', state: 'CO', vote: 'NAY', date: '2026-03-09' },
    { id: 'bv-5', politicianId: '550e8400-e29b-41d4-a716-446655440002', politicianName: 'Taylor Morgan', party: 'Republican', state: 'CO', vote: 'YEA', date: '2026-03-09' },
  ],
  'bill-s-78': [
    { id: 'bv-6', politicianId: '550e8400-e29b-41d4-a716-446655440000', politicianName: 'Alex Rivera', party: 'Independent', state: 'CO', vote: 'YEA', date: '2026-02-14' },
    { id: 'bv-7', politicianId: '550e8400-e29b-41d4-a716-446655440001', politicianName: 'Morgan Lee', party: 'Democratic', state: 'CA', vote: 'YEA', date: '2026-02-14' },
  ],
};

export const stances: IssueStance[] = [
  {
    issue: 'Public records',
    stance: 'Supports machine-readable disclosure of procurement, votes, and office meeting logs.',
    confidence: 'High',
    basis: 'Voting record plus official floor statement.',
  },
  {
    issue: 'Emergency procurement',
    stance: 'Opposes broader emergency exemptions without public reporting requirements.',
    confidence: 'Medium',
    basis: 'One vote and one interview citation.',
  },
  {
    issue: 'Election administration',
    stance: 'Supports county audit grants and voter-facing ballot tracking.',
    confidence: 'High',
    basis: 'Sponsored bill and committee record.',
  },
];

export const timeline: TimelineItem[] = [
  {
    id: 'timeline-1',
    date: '2026-04-18',
    title: 'Voted YEA on HR-204',
    detail: 'Supported public contract disclosure requirements.',
    category: 'Vote',
  },
  {
    id: 'timeline-2',
    date: '2026-04-17',
    title: 'Floor statement on transparency',
    detail: 'Direct quote from official transcript added to profile.',
    category: 'Statement',
  },
  {
    id: 'timeline-3',
    date: '2026-03-28',
    title: 'Disclosure filing reviewed',
    detail: 'Late filing confirmed and marked corrected.',
    category: 'Fact check',
  },
];

export const citations: Citation[] = [
  {
    id: 'citation-1',
    source: 'Congressional Record',
    quality: 'Official record',
    date: '2026-04-18',
    url: 'https://example.gov/record/hr204',
  },
  {
    id: 'citation-2',
    source: 'State Ethics Filing',
    quality: 'Official record',
    date: '2026-03-28',
    url: 'https://example.gov/ethics/filing',
  },
  {
    id: 'citation-3',
    source: 'Capitol Daily',
    quality: 'Reputable news',
    date: '2026-03-10',
    url: 'https://example.news/interview',
  },
];

export const articles: Article[] = [
  {
    id: 'article-1',
    headline: 'Rivera presses for public contract transparency',
    source: 'Capitol Daily',
    date: '2026-04-17',
    summary: 'Coverage of Rivera arguing for machine-readable procurement records and public reporting.',
    tone: 'Positive',
  },
  {
    id: 'article-2',
    headline: 'Watchdog questions campaign vendor overlap',
    source: 'Civic Ledger',
    date: '2026-03-21',
    summary: 'An unresolved watchdog allegation involving campaign vendors and later grant recipients.',
    tone: 'Controversy',
  },
  {
    id: 'article-3',
    headline: 'Committee advances election infrastructure grants',
    source: 'Public Affairs Wire',
    date: '2026-02-14',
    summary: 'The bill moved forward with bipartisan support after county election officials testified.',
    tone: 'Neutral',
  },
];
