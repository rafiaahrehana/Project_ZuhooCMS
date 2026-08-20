import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AssetImportResult } from '../models/itam.model';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssetImportService {
  private readonly endpoint = '/itam/asset-import';

  constructor(private api: ApiService, private http: HttpClient) {}

  // Bypasses ApiService here since it always expects a JSON body - this endpoint
  // returns a raw text/csv file.
  getTemplateCsv(): Observable<string> {
    return this.http.get(`${environment.apiUrl}${this.endpoint}/template`, { responseType: 'text' });
  }

  getTemplateXlsx(): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}${this.endpoint}/template.xlsx`, { responseType: 'blob' });
  }

  importCsv(file: File): Observable<AssetImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.post<AssetImportResult>(this.endpoint, formData);
  }
}
