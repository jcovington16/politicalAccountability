export type Politician = {
  id: string;
  firstName: string;
  lastName: string;
  party: string;
  state: string;
  office: string;
  biography: string;
  startDate: string;
};

export type VoteRecord = {
  id: string;
  billId: string;
  billNumber: string;
  title: string;
  vote: 'YEA' | 'NAY' | 'ABSTAIN';
  date: string;
};

export type Bill = {
  id: string;
  billNumber: string;
  title: string;
  summary: string;
  status: 'Pending' | 'Passed' | 'Failed' | 'Vetoed';
  introducedDate: string;
  lastActionDate?: string;
  sponsor: string;
  chamber: string;
  jurisdiction: string;
  sourceUrl: string;
};

export type BillVote = {
  id: string;
  politicianId: string;
  politicianName: string;
  party: string;
  state: string;
  vote: 'YEA' | 'NAY' | 'ABSTAIN';
  date: string;
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

export type Citation = {
  id: string;
  source: string;
  quality: string;
  date: string;
  url: string;
};

export type Article = {
  id: string;
  headline: string;
  source: string;
  date: string;
  summary: string;
  tone: 'Positive' | 'Controversy' | 'Neutral';
};
