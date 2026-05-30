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
  billNumber: string;
  title: string;
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
