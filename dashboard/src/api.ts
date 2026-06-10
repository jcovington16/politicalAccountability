import type { Bill, BillDetail, ClaimRecord, Politician, PoliticianProfile, PublicStatement, SearchResponse, SourceCitationRecord, TimelineAggregate, TrustScore, VotingRecord } from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly statusText: string,
    message?: string,
  ) {
    super(message ?? `${status} ${statusText}`);
  }
}

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    recordApiError(path, response.status, response.statusText);
    throw new ApiError(response.status, response.statusText);
  }
  return response.json() as Promise<T>;
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    recordApiError(path, response.status, response.statusText);
    throw new ApiError(response.status, response.statusText);
  }
  return response.json() as Promise<T>;
}

function recordApiError(path: string, status: number, statusText: string) {
  if (typeof window === 'undefined') return;

  window.dispatchEvent(new CustomEvent('publicrecord:api-error', {
    detail: {
      path,
      status,
      statusText,
      observedAt: new Date().toISOString(),
    },
  }));
}

export async function searchPoliticians(name: string): Promise<Politician[]> {
  return getJson<Politician[]>(`/politicians/search/name?name=${encodeURIComponent(name)}`);
}

export async function globalSearch(query: string): Promise<SearchResponse> {
  return getJson<SearchResponse>(`/search?query=${encodeURIComponent(query)}&limit=8`);
}

export async function getPolitician(id: string): Promise<Politician> {
  return getJson<Politician>(`/politicians/${id}`);
}

export async function getPoliticianProfile(id: string): Promise<PoliticianProfile> {
  return getJson<PoliticianProfile>(`/politicians/${id}/profile`);
}

export async function getVotes(politicianId: string): Promise<VotingRecord[]> {
  return getJson<VotingRecord[]>(`/politicians/${politicianId}/votes`);
}

export async function searchBills(query: string): Promise<Bill[]> {
  return getJson<Bill[]>(`/bills/search?query=${encodeURIComponent(query)}`);
}

export async function getBillDetail(id: string): Promise<BillDetail> {
  return getJson<BillDetail>(`/bills/${id}`);
}

export async function getPublicStatements(politicianId: string): Promise<PublicStatement[]> {
  return getJson<PublicStatement[]>(`/politicians/${politicianId}/statements`);
}

export async function getPoliticianClaims(politicianId: string): Promise<ClaimRecord[]> {
  return getJson<ClaimRecord[]>(`/politicians/${politicianId}/claims?limit=100`);
}

export async function getPoliticianTimeline(politicianId: string): Promise<TimelineAggregate> {
  return getJson<TimelineAggregate>(`/politicians/${politicianId}/timeline?limit=100`);
}

export async function searchCitations(query: string): Promise<SourceCitationRecord[]> {
  return getJson<SourceCitationRecord[]>(`/citations?query=${encodeURIComponent(query)}&limit=100`);
}

export async function scoreTrust(payload: {
  informationType: string;
  sourceQuality: string;
  citationCount: number;
  publishedDate?: string;
}): Promise<TrustScore> {
  return postJson<TrustScore>('/trust/score', payload);
}
