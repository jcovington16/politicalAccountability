export type Politician = {
  id: string;
  firstName: string;
  lastName: string;
  party: string;
  state: string;
  office: string;
  biography?: string;
  profileImageUrl?: string;
  startDate: string;
  endDate?: string;
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
  introducedDate: string;
  lastActionDate?: string;
  billUrl?: string;
};

export type VotingRecord = {
  id: string;
  politicianId: string;
  billId: string;
  billNumber?: string;
  billTitle?: string;
  voteType: 'YEA' | 'NAY' | 'ABSTAIN';
  voteDate: string;
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
