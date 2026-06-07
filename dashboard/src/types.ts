export type Politician = {
  id: string;
  firstName: string;
  lastName: string;
  party: string;
  state: string;
  office: string;
  biography?: string;
  profileImageUrl?: string;
  startDate: string | [number, number, number];
  endDate?: string | [number, number, number];
};

export type Bill = {
  id: string;
  billNumber: string;
  title: string;
  description?: string;
  introducedBy?: string;
  sponsor?: string;
  chamber?: string;
  jurisdiction?: string;
  status: 'Pending' | 'Passed' | 'Failed' | 'Vetoed';
  introducedDate: string | [number, number, number];
  lastActionDate?: string | [number, number, number];
  billUrl?: string;
};

export type BillSponsor = {
  id: string;
  billId: string;
  politicianId: string;
  politicianName: string;
  party?: string;
  state?: string;
  sponsorType: 'SPONSOR' | 'COSPONSOR';
  sponsorshipDate?: string | [number, number, number];
  sourceCitationId?: string;
};

export type BillDetail = {
  bill: Bill;
  sponsors: BillSponsor[];
  cosponsors: BillSponsor[];
  actions: Array<{ id: string; billId: string; actionDate: string | [number, number, number]; actionText: string; sourceCitationId?: string }>;
  citations: Array<{ id: string; sourceName?: string; title?: string; url: string; sourceQuality: SourceQuality; confidence?: number }>;
  votes: VotingRecord[];
};

export type VotingRecord = {
  id: string;
  politicianId: string;
  politicianName?: string;
  party?: string;
  state?: string;
  billId: string;
  billNumber?: string;
  billTitle?: string;
  billUrl?: string;
  voteType: 'YEA' | 'NAY' | 'ABSTAIN';
  voteDate: string | [number, number, number];
};

export type OfficeHistoryItem = {
  officeId: string;
  title: string;
  branch: 'EXECUTIVE' | 'LEGISLATIVE' | 'JUDICIAL';
  officeLevel: 'STATE' | 'FEDERAL';
  jurisdiction: string;
  state?: string;
  district?: string;
  seatIdentifier: string;
  startDate: string;
  endDate?: string;
  isCurrent: boolean;
  sourceUrl: string;
};

export type ElectionCandidate = {
  politicianId: string;
  fullName: string;
  party?: string;
  ballotStatus: 'FILED' | 'CERTIFIED' | 'WITHDRAWN' | 'DISQUALIFIED';
  resultStatus?: 'WON' | 'LOST' | 'RUNOFF' | 'PENDING';
  voteTotal?: number;
  votePercentage?: number;
  sourceUrl: string;
};

export type ElectionHistoryItem = {
  electionId: string;
  officeId: string;
  electionDate: string;
  electionType: 'PRIMARY' | 'GENERAL' | 'SPECIAL' | 'RUNOFF';
  cycleYear: number;
  jurisdiction: string;
  sourceUrl: string;
  candidates: ElectionCandidate[];
};

export type ProfileTimelineItem = {
  date: string;
  category: string;
  title: string;
  description?: string;
  sourceUrl?: string;
};

export type TimelineAggregate = {
  politicianId: string;
  generatedAt: string;
  stats: {
    total: number;
    byCategory: Record<string, number>;
    publishableCount: number;
    reviewRequiredCount: number;
    latestActivityAt?: string;
  };
  items: TimelineAggregateItem[];
};

export type TimelineAggregateItem = {
  id: string;
  date: string | [number, number, number];
  category: string;
  title: string;
  description?: string;
  sourceUrl?: string;
  sourceName?: string;
  targetType: string;
  targetId?: string;
  evidenceType: string;
  publishable: boolean;
  warnings: string[];
};

export type ProfileTrustSummary = {
  averageScore: number;
  citationCount: number;
  openRiskCount: number;
  records: TrustScore[];
};

export type PoliticianProfile = {
  politician: Politician;
  offices: OfficeHistoryItem[];
  elections: ElectionHistoryItem[];
  trustSummary: ProfileTrustSummary;
  votingRecords: VotingRecord[];
  votedBills: Array<{ bill: Bill; voteType: VotingRecord['voteType']; voteDate: string }>;
  billsSupported: Bill[];
  billsOpposed: Bill[];
  billsSponsored: Bill[];
  contentItems: Array<{ id: string; title: string; contentType: string; textBody?: string; sourceUrl?: string; publishedAt: string }>;
  citations: Array<{
    id: string;
    sourceName?: string;
    sourceType?: string;
    citationType?: string;
    targetId?: string;
    title?: string;
    url?: string;
    archiveUrl?: string;
    publishedAt?: string | [number, number, number];
    retrievedAt?: string | [number, number, number];
    quote?: string;
    sourceQuality?: SourceQuality;
    confidence?: number;
  }>;
  timeline: ProfileTimelineItem[];
};

export type BillVote = {
  id: string;
  politicianId: string;
  politicianName: string;
  party: string;
  state: string;
  voteType: 'YEA' | 'NAY' | 'ABSTAIN';
  voteDate: string;
};

export type Statement = {
  id: string;
  title: string;
  type: 'Speech' | 'Interview' | 'Social' | 'Press Release';
  date: string;
  excerpt: string;
  trust: TrustScore;
};

export type PublicStatement = {
  id: string;
  politicianId?: string;
  statementType: 'SPEECH' | 'INTERVIEW' | 'SOCIAL' | 'PRESS_RELEASE' | 'DEBATE' | 'HEARING' | 'OTHER';
  title: string;
  body?: string;
  quote?: string;
  venue?: string;
  statementDate: string;
  sourceCitationId?: string;
  confidence?: number;
  suspiciousContent: boolean;
};

export type ClaimRecord = {
  id: string;
  politicianId?: string;
  statementId?: string;
  claimText: string;
  claimType: InformationType;
  status: 'VERIFIED' | 'DISPUTED' | 'UNRESOLVED' | 'RETRACTED';
  confidence?: number;
  firstSeenAt?: string | [number, number, number];
  lastReviewedAt?: string | [number, number, number];
  citationCount: number;
  trust: TrustScore;
  publishable: boolean;
  reviewWarnings: string[];
  factChecks: Array<{ id: string; rating: string; summary: string; checkedBy?: string; checkedAt: string; sourceCitationId?: string }>;
};

export type SourceCitationRecord = {
  id: string;
  sourceName?: string;
  sourceType?: SourceQuality;
  citationType: string;
  targetId?: string;
  title?: string;
  url: string;
  archiveUrl?: string;
  publishedAt?: string | [number, number, number];
  retrievedAt: string | [number, number, number];
  quote?: string;
  sourceQuality: SourceQuality;
  confidence?: number;
  manipulationWarnings: string[];
};

export type Citation = {
  id: string;
  source: string;
  sourceQuality: SourceQuality;
  url: string;
  date: string;
  citedBy: string;
};

export type TimelineItem = {
  id: string;
  date: string;
  category: 'Vote' | 'Bill' | 'Statement' | 'News' | 'Fact Check';
  title: string;
  description: string;
  risk: 'Low' | 'Medium' | 'High';
};

export type TrustScore = {
  informationType: InformationType;
  sourceQuality: SourceQuality;
  citationCount: number;
  recencyDays?: number;
  confidenceLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  score: number;
  explanation: string;
};

export type SearchResponse = {
  query: string;
  total: number;
  groups: SearchGroup[];
};

export type SearchGroup = {
  type: string;
  label: string;
  results: SearchResult[];
};

export type SearchResult = {
  id: string;
  title: string;
  subtitle?: string;
  description?: string;
  url?: string;
  source?: string;
  date?: string;
  trustContext?: string;
  reviewWarnings: string[];
};

export type InformationType =
  | 'VERIFIED_FACT'
  | 'DIRECT_QUOTE'
  | 'VOTING_RECORD'
  | 'ALLEGATION'
  | 'OPINION_PIECE'
  | 'UNRESOLVED_CLAIM';

export type SourceQuality =
  | 'OFFICIAL_RECORD'
  | 'PRIMARY_SOURCE'
  | 'REPUTABLE_NEWS'
  | 'ADVOCACY_OR_PARTISAN'
  | 'SOCIAL_MEDIA'
  | 'UNKNOWN';

export type SecurityControl = {
  id: string;
  area: string;
  risk: 'Low' | 'Medium' | 'High' | 'Critical';
  status: 'Designed' | 'In Progress' | 'Needs Owner';
  owner: string;
  control: string;
  evidence: string;
  nextAction: string;
};
