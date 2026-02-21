import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CertificateRequest {
  commonName: string;
  organization: string;
  organizationalUnit?: string;
  country?: string;
  state?: string;
  locality?: string;
  type: string;
  validFrom: string;
  validTo: string;
  issuerAlias?: string;
  keyUsages?: string[];
  extendedKeyUsages?: string[];
  isCA: boolean;
}

export interface CertificateResponse {
  id: number;
  alias: string;
  commonName: string;
  organization: string;
  organizationalUnit: string;
  country: string;
  serialNumber: string;
  type: string;
  validFrom: string;
  validTo: string;
  issuerAlias: string;
  revoked: boolean;
  revocationReason: string;
  revokedAt: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CertificateService {

  private apiUrl = 'https://localhost:8443/api/certificates';

  constructor(private http: HttpClient) {}

  // Dohvata sve sertifikate
  getAllCertificates(): Observable<CertificateResponse[]> {
    return this.http.get<CertificateResponse[]>(this.apiUrl);
  }

  // Dohvata jedan sertifikat po ID-u
  getCertificateById(id: number): Observable<CertificateResponse> {
    return this.http.get<CertificateResponse>(`${this.apiUrl}/${id}`);
  }

  // Dohvata dostupne issuere za dropdown
  getAvailableIssuers(): Observable<CertificateResponse[]> {
    return this.http.get<CertificateResponse[]>(`${this.apiUrl}/issuers`);
  }

  // Kreira novi sertifikat
  generateCertificate(request: CertificateRequest): Observable<CertificateResponse> {
    return this.http.post<CertificateResponse>(`${this.apiUrl}/generate`, request);
  }

  // Povlači sertifikat sa razlogom
  revokeCertificate(id: number, reason: string): Observable<CertificateResponse> {
    return this.http.put<CertificateResponse>(
      `${this.apiUrl}/${id}/revoke`,
      { reason }
    );
  }
}