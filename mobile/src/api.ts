import Constants from 'expo-constants';
import type { Bill, BillVote, Citation, Politician, VoteRecord } from './types';

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
};

export type ElectionCandidate = {
  politicianId: string;
  fullName: string;
  party?: string;
  ballotStatus: 'FILED' | 'CERTIFIED' | 'WITHDRAWN' | 'DISQUALIFIED';
  resultStatus?: 'WON' | 'LOST' | 'RUNOFF' | 'PENDING';
};

export type ElectionHistoryItem = {
  electionId: string;
  officeId: string;
  electionDate: string;
  electionType: 'PRIMARY' | 'GENERAL' | 'SPECIAL' | 'RUNOFF';
  cycleYear: number;
  candidates: ElectionCandidate[];
};

export type PoliticianProfileResponse = {
  politician: Politician;
  offices: OfficeHistoryItem[];
  elections: ElectionHistoryItem[];
  citations: Citation[];
};

const expoConfig = Constants.expoConfig?.extra as { apiBaseUrl?: string } | undefined;
const apiBaseUrl = expoConfig?.apiBaseUrl ?? 'http://localhost:8080';

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status} ${response.statusText}`);
  }

  return response.json() as Promise<T>;
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

export function getBillVotes(billId: string): Promise<BillVote[]> {
  return getJson<BillVote[]>(`/bills/${billId}/votes`);
}
