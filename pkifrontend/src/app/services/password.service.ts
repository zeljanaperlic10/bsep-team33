import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PasswordEntryRequest {
  siteName: string;
  username: string;
  encryptedPassword: string;
}

export interface PasswordEntryResponse {
  id: number;
  siteName: string;
  username: string;
  encryptedPassword: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordService {

  private apiUrl = 'https://localhost:8443/api/passwords';
  private keyPairUrl = 'https://localhost:8443/api/keypair';

  constructor(private http: HttpClient) {}

  // Dohvata sve lozinke
  getAllPasswords(): Observable<PasswordEntryResponse[]> {
    return this.http.get<PasswordEntryResponse[]>(this.apiUrl);
  }

  // Dohvata jednu lozinku po ID-u
  getPasswordById(id: number): Observable<PasswordEntryResponse> {
    return this.http.get<PasswordEntryResponse>(`${this.apiUrl}/${id}`);
  }

  // Čuva novu enkriptovanu lozinku
  savePassword(request: PasswordEntryRequest): Observable<PasswordEntryResponse> {
    return this.http.post<PasswordEntryResponse>(this.apiUrl, request);
  }

  // Ažurira lozinku
  updatePassword(id: number, request: PasswordEntryRequest): Observable<PasswordEntryResponse> {
    return this.http.put<PasswordEntryResponse>(`${this.apiUrl}/${id}`, request);
  }

  // Briše lozinku
  deletePassword(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Generiše par ključeva — privatni ključ se vraća JEDNOM
  generateKeyPair(): Observable<{publicKey: string, privateKey: string}> {
    return this.http.post<{publicKey: string, privateKey: string}>(
      `${this.keyPairUrl}/generate`, {}
    );
  }

  // Dohvata javni ključ admina
  getPublicKey(): Observable<{publicKey: string}> {
    return this.http.get<{publicKey: string}>(`${this.keyPairUrl}/public-key`);
  }

  // Proverava da li admin ima generisan par ključeva
  getKeyPairStatus(): Observable<{hasKeyPair: boolean}> {
    return this.http.get<{hasKeyPair: boolean}>(`${this.keyPairUrl}/status`);
  }
}