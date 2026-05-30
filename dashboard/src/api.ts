import type { Bill, Politician, TrustScore, VotingRecord } from './types';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
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
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export async function searchPoliticians(name: string): Promise<Politician[]> {
  return getJson<Politician[]>(`/politicians/search/name?name=${encodeURIComponent(name)}`);
}

export async function getPolitician(id: string): Promise<Politician> {
  return getJson<Politician>(`/politicians/${id}`);
}

export async function getVotes(politicianId: string): Promise<VotingRecord[]> {
  return getJson<VotingRecord[]>(`/politicians/${politicianId}/votes`);
}

export async function searchBills(query: string): Promise<Bill[]> {
  return getJson<Bill[]>(`/bills/search?query=${encodeURIComponent(query)}`);
}

export async function scoreTrust(payload: {
  informationType: string;
  sourceQuality: string;
  citationCount: number;
  publishedDate?: string;
}): Promise<TrustScore> {
  return postJson<TrustScore>('/trust/score', payload);
}
