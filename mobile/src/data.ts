import type { Citation, IssueStance, Politician, TimelineItem, VoteRecord } from './types';

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
];

export const votes: VoteRecord[] = [
  {
    id: 'vote-1',
    billNumber: 'HR-204',
    title: 'Public Contract Disclosure Act',
    vote: 'YEA',
    date: '2026-04-18',
  },
  {
    id: 'vote-2',
    billNumber: 'HR-311',
    title: 'Emergency Procurement Exception Expansion',
    vote: 'NAY',
    date: '2026-03-09',
  },
  {
    id: 'vote-3',
    billNumber: 'S-78',
    title: 'Election Infrastructure Grants',
    vote: 'YEA',
    date: '2026-02-14',
  },
];

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
