import { Injectable, inject, signal } from '@angular/core';
import { Auth } from './auth';
/** Only a local development data selector, not an identity/ownership security control. */
@Injectable({providedIn: 'root'})
export class AccountContext {
  private readonly auth = inject(Auth);
  readonly id = signal<number | null>(null);
  constructor() {
    const stored = localStorage.getItem(this.key());
    const value = Number(stored);
    if (stored && Number.isSafeInteger(value) && value > 0) this.id.set(value);
  }
  private key() { return `kido-demo-numeric-id:${this.auth.subject}`; }
  setId(value: number) {
    if (!Number.isSafeInteger(value) || value <= 0) return false;
    this.id.set(value);
    localStorage.setItem(this.key(), String(value));
    return true;
  }
  clear() {
    this.id.set(null);
    localStorage.removeItem(this.key());
  }
}
