import { Component, OnInit } from '@angular/core';
import { PasswordService, PasswordEntryResponse } from '../../../services/password.service';
import { CryptoService } from '../../../services/crypto.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-password-list',
  templateUrl: './password-list.component.html',
  styleUrls: ['./password-list.component.css']
})
export class PasswordListComponent implements OnInit {

  passwords: PasswordEntryResponse[] = [];
  isLoading = true;
  hasKeyPair = false;
  isGeneratingKeys = false;

  // Mapa koja čuva dekriptovane lozinke privremeno u memoriji
  // Ključ je ID lozinke, vrednost je plain-text lozinka
  decryptedPasswords: Map<number, string> = new Map();
  decryptingId: number | null = null;

  // Privatni ključ koji admin učitava lokalno
  // Nikad se ne šalje na server
  privateKeyBase64: string | null = null;

  displayedColumns = ['siteName', 'username', 'password', 'createdAt', 'actions'];

  constructor(
    private passwordService: PasswordService,
    private cryptoService: CryptoService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkKeyPairStatus();
    this.loadPasswords();
  }

  checkKeyPairStatus(): void {
    this.passwordService.getKeyPairStatus().subscribe({
      next: (status) => {
        this.hasKeyPair = status.hasKeyPair;
      }
    });
  }

  loadPasswords(): void {
    this.isLoading = true;
    this.passwordService.getAllPasswords().subscribe({
      next: (passwords) => {
        this.passwords = passwords;
        this.isLoading = false;
      },
      error: () => {
        this.snackBar.open('Greška pri učitavanju lozinki', 'OK', { duration: 3000 });
        this.isLoading = false;
      }
    });
  }

  // Generiše novi par ključeva
  // Privatni ključ se preuzima kao .pem fajl — mora se čuvati lokalno
  generateKeyPair(): void {
    if (!confirm(
      'Da li ste sigurni?\n\n' +
      'Generisanjem novog para ključeva, sve postojeće lozinke neće moći biti dekriptovane starim ključem.\n\n' +
      'Privatni ključ će biti preuzet kao fajl — OBAVEZNO ga sačuvajte!'
    )) return;

    this.isGeneratingKeys = true;
    this.passwordService.generateKeyPair().subscribe({
      next: (keys) => {
        this.isGeneratingKeys = false;
        this.hasKeyPair = true;

        // Preuzimamo privatni ključ kao .pem fajl
        this.cryptoService.downloadPrivateKey(keys.privateKey);

        this.snackBar.open(
          'Par ključeva generisan! Privatni ključ je preuzet — sačuvajte ga na sigurno mesto!',
          'OK',
          { duration: 8000 }
        );
      },
      error: () => {
        this.isGeneratingKeys = false;
        this.snackBar.open('Greška pri generisanju ključeva', 'OK', { duration: 3000 });
      }
    });
  }

  // Admin učitava privatni ključ sa svog računara
  // Ključ ostaje samo u memoriji browsera
  onPrivateKeyFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    this.cryptoService.loadPrivateKeyFromFile(file).then(privateKey => {
      this.privateKeyBase64 = privateKey;
      this.snackBar.open(
        'Privatni ključ učitan! Možete dekriptovati lozinke.',
        'OK',
        { duration: 3000 }
      );
    }).catch(() => {
      this.snackBar.open('Greška pri učitavanju privatnog ključa', 'OK', { duration: 3000 });
    });
  }

  // Dekriptuje jednu lozinku privatnim ključem
  async decryptPassword(entry: PasswordEntryResponse): Promise<void> {
    if (!this.privateKeyBase64) {
      this.snackBar.open(
        'Molimo učitajte privatni ključ pre dekriptovanja',
        'OK',
        { duration: 3000 }
      );
      return;
    }

    this.decryptingId = entry.id;
    try {
      const decrypted = await this.cryptoService.decryptPassword(
        entry.encryptedPassword,
        this.privateKeyBase64
      );
      this.decryptedPasswords.set(entry.id, decrypted);
    } catch (err) {
      this.snackBar.open(
        'Greška pri dekriptovanju — proverite da li ste učitali ispravan privatni ključ',
        'OK',
        { duration: 4000 }
      );
    }
    this.decryptingId = null;
  }

  // Sakriva dekriptovanu lozinku
  hidePassword(id: number): void {
    this.decryptedPasswords.delete(id);
  }

  // Briše lozinku
  deletePassword(id: number): void {
    if (!confirm('Da li ste sigurni da želite da obrišete ovu lozinku?')) return;

    this.passwordService.deletePassword(id).subscribe({
      next: () => {
        this.snackBar.open('Lozinka obrisana', 'OK', { duration: 2000 });
        this.loadPasswords();
        this.decryptedPasswords.delete(id);
      },
      error: () => {
        this.snackBar.open('Greška pri brisanju', 'OK', { duration: 3000 });
      }
    });
  }

  isDecrypted(id: number): boolean {
    return this.decryptedPasswords.has(id);
  }

  getDecryptedPassword(id: number): string {
    return this.decryptedPasswords.get(id) || '';
  }
}