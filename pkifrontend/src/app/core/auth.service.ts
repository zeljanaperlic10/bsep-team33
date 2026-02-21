import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'https://localhost:8443/api/auth';

  constructor(private http: HttpClient, private router: Router) {}

  // Šalje login request na backend
  // Čuva token i podatke o korisniku u localStorage
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('email', response.email);
        localStorage.setItem('firstName', response.firstName);
        localStorage.setItem('lastName', response.lastName);
        localStorage.setItem('role', response.role);
      })
    );
  }

  // Briše sve podatke iz localStorage i redirektuje na login
  logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  // Proverava da li postoji token u localStorage
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  // Vraća JWT token
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // Vraća podatke o ulogovanom adminu
  getCurrentUser() {
    return {
      email: localStorage.getItem('email'),
      firstName: localStorage.getItem('firstName'),
      lastName: localStorage.getItem('lastName'),
      role: localStorage.getItem('role')
    };
  }
}