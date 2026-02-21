import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CryptoService {

  // Enkriptuje lozinku javnim ključem admina
  // Koristi RSA-OAEP algoritam — standard za asimetričnu enkripciju
  async encryptPassword(plainPassword: string, publicKeyBase64: string): Promise<string> {
    // Korak 1: Dekodujemo Base64 javni ključ u bytes
    const publicKeyBytes = this.base64ToArrayBuffer(publicKeyBase64);

    // Korak 2: Uvozimo javni ključ u Web Crypto API format
    const publicKey = await window.crypto.subtle.importKey(
      'spki',           // Format javnog ključa
      publicKeyBytes,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256'
      },
      false,            // Ključ nije exportable
      ['encrypt']       // Možemo samo enkriptovati
    );

    // Korak 3: Enkriptujemo lozinku
    const encoder = new TextEncoder();
    const passwordBytes = encoder.encode(plainPassword);

    const encryptedBytes = await window.crypto.subtle.encrypt(
      { name: 'RSA-OAEP' },
      publicKey,
      passwordBytes
    );

    // Korak 4: Konvertujemo u Base64 za čuvanje
    return this.arrayBufferToBase64(encryptedBytes);
  }

  // Dekriptuje lozinku privatnim ključem admina
  // Privatni ključ admin učitava lokalno sa svog računara
  async decryptPassword(encryptedBase64: string, privateKeyBase64: string): Promise<string> {
    // Korak 1: Dekodujemo Base64 privatni ključ
    const privateKeyBytes = this.base64ToArrayBuffer(privateKeyBase64);

    // Korak 2: Uvozimo privatni ključ u Web Crypto API format
    const privateKey = await window.crypto.subtle.importKey(
      'pkcs8',          // Format privatnog ključa
      privateKeyBytes,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256'
      },
      false,
      ['decrypt']       // Možemo samo dekriptovati
    );

    // Korak 3: Dekriptujemo
    const encryptedBytes = this.base64ToArrayBuffer(encryptedBase64);

    const decryptedBytes = await window.crypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      privateKey,
      encryptedBytes
    );

    // Korak 4: Konvertujemo bytes u string
    const decoder = new TextDecoder();
    return decoder.decode(decryptedBytes);
  }

  // Preuzima privatni ključ kao .pem fajl
  // Admin mora sačuvati ovaj fajl lokalno
  downloadPrivateKey(privateKeyBase64: string): void {
    const pemContent = `-----BEGIN PRIVATE KEY-----\n${privateKeyBase64}\n-----END PRIVATE KEY-----`;
    const blob = new Blob([pemContent], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'private-key.pem';
    a.click();
    window.URL.revokeObjectURL(url);
  }

  // Učitava privatni ključ iz .pem fajla koji admin selektuje
  async loadPrivateKeyFromFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        // Uklanjamo PEM header i footer
        const base64 = content
          .replace('-----BEGIN PRIVATE KEY-----', '')
          .replace('-----END PRIVATE KEY-----', '')
          .replace(/\n/g, '')
          .trim();
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }

  // Helper — konvertuje Base64 string u ArrayBuffer
  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binaryString = window.atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  }

  // Helper — konvertuje ArrayBuffer u Base64 string
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  }
}