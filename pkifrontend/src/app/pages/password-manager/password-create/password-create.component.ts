import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PasswordService } from '../../../services/password.service';
import { CryptoService } from '../../../services/crypto.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-password-create',
  templateUrl: './password-create.component.html',
  styleUrls: ['./password-create.component.css']
})
export class PasswordCreateComponent implements OnInit {

  passwordForm!: FormGroup;
  isLoading = false;
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private cryptoService: CryptoService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.passwordForm = this.fb.group({
      siteName: ['', [Validators.required]],
      username: ['', [Validators.required]],
      // Plain-text lozinka — enkriptujemo je pre slanja na backend
      plainPassword: ['', [Validators.required, Validators.minLength(1)]]
    });
  }

  async onSubmit(): Promise<void> {
    if (this.passwordForm.invalid) return;

    this.isLoading = true;

    try {
      // Korak 1: Dohvatamo javni ključ sa servera
      const publicKeyResponse = await this.passwordService.getPublicKey().toPromise();
      if (!publicKeyResponse?.publicKey) {
        throw new Error('Javni ključ nije dostupan');
      }

      // Korak 2: Enkriptujemo lozinku javnim ključem
      // Ovo se dešava lokalno u browseru — plain-text lozinka
      // nikad ne napušta browser
      const encryptedPassword = await this.cryptoService.encryptPassword(
        this.passwordForm.value.plainPassword,
        publicKeyResponse.publicKey
      );

      // Korak 3: Šaljemo enkriptovanu lozinku na backend
      const request = {
        siteName: this.passwordForm.value.siteName,
        username: this.passwordForm.value.username,
        encryptedPassword: encryptedPassword
      };

      this.passwordService.savePassword(request).subscribe({
        next: () => {
          this.isLoading = false;
          this.snackBar.open(
            'Lozinka uspešno sačuvana!',
            'OK',
            { duration: 3000 }
          );
          this.router.navigate(['/passwords']);
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.open(
            'Greška pri čuvanju: ' + (err.error?.message || 'Nepoznata greška'),
            'OK',
            { duration: 3000 }
          );
        }
      });

    } catch (err: any) {
      this.isLoading = false;
      this.snackBar.open(
        'Greška pri enkriptovanju: ' + err.message,
        'OK',
        { duration: 3000 }
      );
    }
  }

  cancel(): void {
    this.router.navigate(['/passwords']);
  }
}