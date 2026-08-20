import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface SearchResultItem {
  type: string;
  id: number;
  title: string;
  subtitle: string;
  link: string;
}

export interface GlobalSearchResponse {
  query: string;
  results: SearchResultItem[];
  totalMatches: number;
}

export interface AskResponse {
  question: string;
  answer: string;
  sources: SearchResultItem[];
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  constructor(private api: ApiService) {}

  search(q: string): Observable<GlobalSearchResponse> {
    return this.api.get<GlobalSearchResponse>('/search', { q });
  }

  ask(question: string): Observable<AskResponse> {
    return this.api.post<AskResponse>('/search/ask', { question });
  }
}
