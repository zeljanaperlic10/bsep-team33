import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CertificateService, CertificateResponse } from '../../../services/certificate.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-certificate-create',
  templateUrl: './certificate-create.component.html',
  styleUrls: ['./certificate-create.component.css']
})
export class CertificateCreateComponent implements OnInit {

  certForm!: FormGroup;
  isLoading = false;
  availableIssuers: CertificateResponse[] = [];

  // X.509 razlozi za KEY USAGE
  keyUsageOptions = [
    { value: 'keyCertSign', label: 'keyCertSign' },
    { value: 'cRLSign', label: 'cRLSign' },
    { value: 'digitalSignature', label: 'digitalSignature' },
    { value: 'keyEncipherment', label: 'keyEncipherment' },
    { value: 'dataEncipherment', label: 'dataEncipherment' },
    { value: 'nonRepudiation', label: 'nonRepudiation' }
  ];

  extendedKeyUsageOptions = [
    { value: 'serverAuth', label: 'serverAuth' },
    { value: 'clientAuth', label: 'clientAuth' },
    { value: 'codeSigning', label: 'codeSigning' },
    { value: 'emailProtection', label: 'emailProtection' }
  ];

  constructor(
    private fb: FormBuilder,
    private certificateService: CertificateService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadIssuers();

    // Kada se promeni tip sertifikata
    // ažuriramo validatore za issuer polje
    this.certForm.get('type')?.valueChanges.subscribe(type => {
      const issuerControl = this.certForm.get('issuerAlias');
      if (type === 'ROOT') {
        issuerControl?.clearValidators();
        issuerControl?.setValue('');
      } else {
        issuerControl?.setValidators([Validators.required]);
      }
      issuerControl?.updateValueAndValidity();
    });
  }

  initForm(): void {
    this.certForm = this.fb.group({
      // X500Name polja
      commonName: ['', [Validators.required]],
      organization: ['', [Validators.required]],
      organizationalUnit: [''],
      country: ['', [Validators.maxLength(2)]],
      state: [''],
      locality: [''],

      // Tip i issuer
      type: ['ROOT', [Validators.required]],
      issuerAlias: [''],

      // Datumi važenja
      validFrom: [new Date(), [Validators.required]],
      validTo: ['', [Validators.required]],

      // Ekstenzije
      keyUsages: [[]],
      extendedKeyUsages: [[]],
      isCA: [false]
    });
  }

  loadIssuers(): void {
    this.certificateService.getAvailableIssuers().subscribe({
      next: (issuers) => {
        this.availableIssuers = issuers;
      }
    });
  }

  onTypeChange(type: string): void {
    // Za ROOT i INTERMEDIATE automatski postavljamo isCA na true
    if (type === 'ROOT' || type === 'INTERMEDIATE') {
      this.certForm.patchValue({ isCA: true });
    } else {
      this.certForm.patchValue({ isCA: false });
    }
  }

  onSubmit(): void {
    if (this.certForm.invalid) return;

    this.isLoading = true;

    // Formatiramo datume za backend
    const formValue = this.certForm.value;
    const request = {
      ...formValue,
      validFrom: new Date(formValue.validFrom).toISOString().slice(0, 19),
      validTo: new Date(formValue.validTo).toISOString().slice(0, 19)
    };

    this.certificateService.generateCertificate(request).subscribe({
      next: (cert) => {
        this.isLoading = false;
        this.snackBar.open(
          `Sertifikat "${cert.commonName}" uspešno kreiran!`,
          'OK',
          { duration: 3000 }
        );
        this.router.navigate(['/certificates']);
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(
          'Greška: ' + (err.error?.message || 'Nepoznata greška'),
          'OK',
          { duration: 5000 }
        );
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/certificates']);
  }
}
