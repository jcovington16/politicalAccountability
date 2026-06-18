import Constants from 'expo-constants';
import type { Bill, BillSponsor, BillVote, ClaimRecord, Politician, PublicStatement, SourceCitationRecord, TimelineAggregate, TrustScore, VoteRecord } from './types';

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

export type ProfileTrustSummary = {
  averageScore: number;
  citationCount: number;
  openRiskCount: number;
  records: TrustScore[];
};

export type PoliticianProfileResponse = {
  politician: Politician;
  offices: OfficeHistoryItem[];
  elections: ElectionHistoryItem[];
  trustSummary: ProfileTrustSummary;
  votingRecords: VoteRecord[];
  billsSupported: Bill[];
  billsOpposed: Bill[];
  billsSponsored: Bill[];
  citations: SourceCitationRecord[];
  contentItems: Array<{
    id: string;
    title: string;
    contentType: string;
    textBody?: string;
    sourceUrl?: string;
    publishedAt: string | [number, number, number];
  }>;
};

export type BillDetailResponse = {
  bill: Bill;
  sponsors: BillSponsor[];
  cosponsors: BillSponsor[];
  actions: Array<{ id: string; actionDate: string; actionText: string }>;
  citations: SourceCitationRecord[];
  votes: BillVote[];
};

const expoConfig = Constants.expoConfig?.extra as { apiBaseUrl?: string } | undefined;
const apiBaseUrl = expoConfig?.apiBaseUrl ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly statusText: string,
  ) {
    super(`API request failed: ${status} ${statusText}`);
  }
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    recordApiError(path, response.status, response.statusText);
    throw new ApiError(response.status, response.statusText);
  }

  return response.json() as Promise<T>;
}

function recordApiError(path: string, status: number, statusText: string) {
  console.warn('Public Record API error', {
    path,
    status,
    statusText,
    observedAt: new Date().toISOString(),
  });
}

export function searchPoliticians(query: string): Promise<Politician[]> {
  return getJson<Politician[]>(`/politicians/search/name?name=${encodeURIComponent(query)}`);
}

export function getPoliticianProfile(politicianId: string): Promise<PoliticianProfileResponse> {
  return getJson<PoliticianProfileResponse>(`/politicians/${politicianId}/profile`);
}

export function getVotingRecord(politicianId: string): Promise<VoteRecord[]> {
  return getJson<VoteRecord[]>(`/politicians/${politicianId}/votes`);
}

export function searchBills(query: string): Promise<Bill[]> {
  return getJson<Bill[]>(`/bills/search?query=${encodeURIComponent(query)}`);
}

export function getBill(billId: string): Promise<Bill> {
  return getJson<Bill>(`/bills/${billId}`);
}

export function getBillDetail(billId: string): Promise<BillDetailResponse> {
  return getJson<BillDetailResponse>(`/bills/${billId}`);
}

export function getBillVotes(billId: string): Promise<BillVote[]> {
  return getJson<BillVote[]>(`/bills/${billId}/votes`);
}

export function getPublicStatements(politicianId: string): Promise<PublicStatement[]> {
  return getJson<PublicStatement[]>(`/politicians/${politicianId}/statements`);
}

export function getPoliticianClaims(politicianId: string): Promise<ClaimRecord[]> {
  return getJson<ClaimRecord[]>(`/politicians/${politicianId}/claims?limit=100`);
}

export function getPoliticianTimeline(politicianId: string): Promise<TimelineAggregate> {
  return getJson<TimelineAggregate>(`/politicians/${politicianId}/timeline?limit=100`);
}
