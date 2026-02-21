import { Component, OnInit } from '@angular/core';
import { CertificateService, CertificateResponse } from '../../services/certificate.service';
import { PasswordService } from '../../services/password.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  totalCertificates = 0;
  activeCertificates = 0;
  revokedCertificates = 0;
  totalPasswords = 0;
  hasKeyPair = false;
  isLoading = true;

  recentCertificates: CertificateResponse[] = [];

  constructor(
    private certificateService: CertificateService,
    private passwordService: PasswordService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    // Učitavamo sertifikate
    this.certificateService.getAllCertificates().subscribe({
      next: (certs) => {
        this.totalCertificates = certs.length;
        this.activeCertificates = certs.filter(c => !c.revoked).length;
        this.revokedCertificates = certs.filter(c => c.revoked).length;
        // Prikazujemo poslednjih 5 sertifikata
        this.recentCertificates = certs.slice(-5).reverse();
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });

    // Učitavamo broj lozinki
    this.passwordService.getAllPasswords().subscribe({
      next: (passwords) => {
        this.totalPasswords = passwords.length;
      }
    });

    // Proveravamo da li admin ima par ključeva
    this.passwordService.getKeyPairStatus().subscribe({
      next: (status) => {
        this.hasKeyPair = status.hasKeyPair;
      }
    });
  }
}