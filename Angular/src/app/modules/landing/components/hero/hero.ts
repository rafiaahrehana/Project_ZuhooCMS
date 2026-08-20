import { Component, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * The hero used to switch between a three.js particle field on desktop and a
 * CSS composite everywhere else, which is why this class carried viewport and
 * reduced-motion signals. The WebGL scene was removed, so the composite in
 * hero.html is now unconditional.
 *
 * What remains is the "See Demo" flow: one click drops the visitor into the
 * seeded read-only demo tenant, logged in, on the real dashboard.
 */
@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './hero.html',
  styleUrls: ['./hero.scss']
})
export class HeroComponent {
  startingDemo = false;
  demoError = '';

  constructor(
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  seeDemo(): void {
    if (this.startingDemo) return;
    this.startingDemo = true;
    this.demoError = '';

    this.auth.startDemo().subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.startingDemo = false;
        this.demoError = 'The demo is not available right now - please try again later.';
        this.cdr.markForCheck();
      },
    });
  }
}
