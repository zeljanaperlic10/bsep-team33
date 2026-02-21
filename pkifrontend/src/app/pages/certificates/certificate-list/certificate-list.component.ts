import { Component, OnInit } from '@angular/core';
import { CertificateService, CertificateResponse } from '../../../services/certificate.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-certificate-list',
  templateUrl: './certificate-list.component.html',
  styleUrls: ['./certificate-list.component.css']
})
export class CertificateListComponent implements OnInit {

  certificates: CertificateResponse[] = [];
  filteredCertificates: CertificateResponse[] = [];
  isLoading = true;
  searchText = '';
  selectedType = '';

  displayedColumns = ['commonName', 'type', 'organization', 'validFrom', 'validTo', 'status', 'actions'];

  constructor(
    private certificateService: CertificateService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCertificates();
  }

  loadCertificates(): void {
    this.isLoading = true;
    this.certificateService.getAllCertificates().subscribe({
      next: (certs) => {
        this.certificates = certs;
        this.filteredCertificates = certs;
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Greška pri učitavanju sertifikata', 'OK', { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  // Filtriranje po tekstu i tipu
  applyFilter(): void {
    this.filteredCertificates = this.certificates.filter(cert => {
      const matchesText = !this.searchText ||
        cert.commonName.toLowerCase().includes(this.searchText.toLowerCase()) ||
        cert.organization.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesType = !this.selectedType || cert.type === this.selectedType;

      return matchesText && matchesType;
    });
  }

  // Povlačenje sertifikata
  revoke(cert: CertificateResponse): void {
    const reason = prompt(
      'Unesite razlog povlačenja:\n' +
      '(keyCompromise, cACompromise, affiliationChanged, superseded, cessationOfOperation)'
    );

    if (!reason) return;

    this.certificateService.revokeCertificate(cert.id, reason).subscribe({
      next: () => {
        this.snackBar.open('Sertifikat uspešno povučen', 'OK', { duration: 3000 });
        this.loadCertificates();
      },
      error: (err) => {
        this.snackBar.open('Greška pri povlačenju: ' + err.error?.message, 'OK', { duration: 3000 });
      }
    });
  }
}