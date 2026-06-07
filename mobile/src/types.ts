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

export type VoteRecord = {
  id: string;
  billId: string;
  billNumber: string;
  billTitle: string;
  voteType: 'YEA' | 'NAY' | 'ABSTAIN';
  voteDate: string;
};

export type Bill = {
  id: string;
  billNumber: string;
  title: string;
  description?: string;
  introducedBy?: string;
  status: 'Pending' | 'Passed' | 'Failed' | 'Vetoed';
  introducedDate: string | [number, number, number];
  lastActionDate?: string | [number, number, number];
  sponsor?: string;
  chamber?: string;
  jurisdiction?: string;
  billUrl?: string;
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

export type BillSponsor = {
  id: string;
  billId: string;
  politicianId: string;
  politicianName: string;
  party?: string;
  state?: string;
  sponsorType: 'SPONSOR' | 'COSPONSOR';
  sponsorshipDate?: string | [number, number, number];
};

export type PublicStatement = {
  id: string;
  politicianId?: string;
  statementType: 'SPEECH' | 'INTERVIEW' | 'SOCIAL' | 'PRESS_RELEASE' | 'DEBATE' | 'HEARING' | 'OTHER';
  title: string;
  body?: string;
  quote?: string;
  venue?: string;
  statementDate: string | [number, number, number];
  confidence?: number;
  suspiciousContent: boolean;
};

export type ClaimRecord = {
  id: string;
  politicianId?: string;
  statementId?: string;
  claimText: string;
  claimType: string;
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
  sourceType?: string;
  citationType: string;
  targetId?: string;
  title?: string;
  url: string;
  archiveUrl?: string;
  publishedAt?: string | [number, number, number];
  retrievedAt: string | [number, number, number];
  quote?: string;
  sourceQuality: string;
  confidence?: number;
  manipulationWarnings: string[];
};

export type IssueStance = {
  issue: string;
  stance: string;
  confidence: 'High' | 'Medium' | 'Low';
  basis: string;
};

export type TimelineItem = {
  id: string;
  date: string;
  title: string;
  detail: string;
  category: string;
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

export type Citation = {
  id: string;
  source: string;
  quality: string;
  date: string;
  url: string;
};

export type TrustScore = {
  informationType: string;
  sourceQuality: string;
  citationCount: number;
  recencyDays?: number;
  confidenceLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  score: number;
  explanation: string;
};

export type Article = {
  id: string;
  headline: string;
  source: string;
  date: string;
  summary: string;
  tone: 'Positive' | 'Controversy' | 'Neutral';
};
