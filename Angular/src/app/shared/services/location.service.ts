import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { globalHierarchyLabels, countryNames, GeoNodeDto } from '../models/location.model';

export interface CountryInfo {
  code: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  private apiUrl = environment.apiUrl + '/locations';

  constructor(private http: HttpClient) { }

  getCountriesMaster(): Observable<GeoNodeDto[]> {
    return this.http.get<GeoNodeDto[]>(`${this.apiUrl}/countries`);
  }

  // Country.id and Location.id are independent sequences that can collide - always use
  // this (not getChildrenMaster) to fetch a country's top-level divisions.
  getDivisionsForCountry(countryId: number): Observable<GeoNodeDto[]> {
    return this.http.get<GeoNodeDto[]>(`${this.apiUrl}/countries/${countryId}/divisions`);
  }

  // Location-to-Location only (division -> district -> ... ). Do not pass a Country.id here.
  getChildrenMaster(parentId: number): Observable<GeoNodeDto[]> {
    return this.http.get<GeoNodeDto[]>(`${this.apiUrl}/children/${parentId}`);
  }

  getNode(id: number): Observable<GeoNodeDto> {
    return this.http.get<GeoNodeDto>(`${this.apiUrl}/${id}`);
  }

  createNode(request: any): Observable<GeoNodeDto> {
    return this.http.post<GeoNodeDto>(this.apiUrl, request);
  }

  updateNode(id: number, request: any): Observable<GeoNodeDto> {
    return this.http.put<GeoNodeDto>(`${this.apiUrl}/${id}`, request);
  }

  deleteNode(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Mock getting a list of countries based on the keys available in globalHierarchyLabels.
   * In a real enterprise app, this would call your backend API: GET /api/countries
   */
  getAvailableCountries(): Observable<CountryInfo[]> {
    // We are just extracting the country codes from the labels dictionary
    // For a real app, you would fetch name from an API or a larger constant list.
    const countries = Object.keys(globalHierarchyLabels).map(code => ({
      code,
      name: countryNames[code] || code
    }));
    return of(countries);
  }
}
