import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ImpersonationSession } from '../../../core/models/auth.model';

@Component({
  selector: 'app-impersonation-banner',
  imports: [CommonModule],
  templateUrl: './impersonation-banner.html',
  styleUrl: './impersonation-banner.scss',
})
export class ImpersonationBanner implements OnInit, OnDestroy {
  session: ImpersonationSession | null = null;
  remaining = '00:00';
  private timer?: ReturnType<typeof setInterval>;

  constructor(public auth: AuthService) {}

  ngOnInit(): void {
    this.auth.impersonation$.subscribe(session => {
      this.session = session;
    });
    this.timer = setInterval(() => this.tick(), 1000);
    this.tick();
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private tick(): void {
    if (!this.session) return;
    const msLeft = this.session.expiresAt - Date.now();
    if (msLeft <= 0) {
      this.remaining = '00:00';
      this.endSession();
      return;
    }
    const totalSeconds = Math.floor(msLeft / 1000);
    const mm = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
    const ss = String(totalSeconds % 60).padStart(2, '0');
    this.remaining = `${mm}:${ss}`;
  }

  endSession(): void {
    this.auth.endImpersonation();
  }
}
