import { Injectable, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

export type Theme = 'light' | 'dark';

/** localStorage key. Shared with the pre-paint script in index.html. */
const STORAGE_KEY = 'bos-theme';

/**
 * Routes that are always light, whatever the user's theme is.
 *
 * The public marketing page is a designed composition with its own dark bands
 * and ~450 literal hexes in its component sheets; a token swap would not give
 * you a dark version of it, it would give you a broken one. It is also
 * unreachable from the toggle, which lives in the authenticated navbar.
 */
const LIGHT_ONLY_ROUTES = ['/home'];

/**
 * Light/dark theme for the authenticated app.
 *
 * Two attributes are written to <html>, not one:
 *
 *   data-theme     drives our own --bos-* palette (see styles/_dark.scss)
 *   data-bs-theme  drives Bootstrap 5.3's built-in dark mode
 *
 * Setting the Bootstrap attribute as well is what makes this tractable. Cards,
 * modals, dropdowns, tables, form controls, close buttons and pagination all
 * recolour themselves from Bootstrap's own dark palette, so we only have to
 * handle our tokens and the handful of utilities Bootstrap leaves literal
 * (.bg-white, .text-dark, .table-light) rather than restyling every component.
 *
 * The same two attributes are set by an inline script in index.html before
 * first paint. Without it the app renders light and then flips once Angular
 * boots, which is a visible white flash on every load in dark mode.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {

  readonly theme = signal<Theme>('light');

  /** True once the user has picked a side; until then we follow the OS. */
  private explicit = false;

  /** What the user actually wants, as opposed to what a light-only route forces. */
  private preferred: Theme = 'light';

  constructor(private router: Router) {
    const stored = this.read();
    this.explicit = stored !== null;
    this.preferred = stored ?? (this.systemPrefersDark() ? 'dark' : 'light');
    this.apply(this.preferred);
    this.watchSystem();
    this.watchRoutes();
  }

  toggle(): void {
    this.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  /** An explicit choice; from here on the OS setting no longer overrides it. */
  set(theme: Theme): void {
    this.explicit = true;
    this.preferred = theme;
    try { localStorage.setItem(STORAGE_KEY, theme); } catch { /* private mode */ }
    this.applyForRoute(this.router.url);
  }

  /** Forget the choice and follow the OS again. */
  clear(): void {
    this.explicit = false;
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* private mode */ }
    this.preferred = this.systemPrefersDark() ? 'dark' : 'light';
    this.applyForRoute(this.router.url);
  }

  private apply(theme: Theme): void {
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    root.setAttribute('data-bs-theme', theme);
    this.theme.set(theme);
  }

  private read(): Theme | null {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      return v === 'dark' || v === 'light' ? v : null;
    } catch {
      return null;
    }
  }

  private systemPrefersDark(): boolean {
    return typeof matchMedia === 'function'
      && matchMedia('(prefers-color-scheme: dark)').matches;
  }

  /**
   * Follow the OS only while the user has not chosen. Someone who explicitly
   * picked light does not want their app flipping at sunset.
   */
  private watchSystem(): void {
    if (typeof matchMedia !== 'function') return;
    matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
      if (this.explicit) return;
      this.preferred = e.matches ? 'dark' : 'light';
      this.applyForRoute(this.router.url);
    });
  }

  /**
   * Force light on the marketing page and restore the user's choice on the way
   * out. The stored preference is never touched, so visiting /home and coming
   * back does not silently reset someone to light.
   */
  private watchRoutes(): void {
    this.applyForRoute(this.router.url);
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.applyForRoute(e.urlAfterRedirects));
  }

  private applyForRoute(url: string): void {
    const lightOnly = LIGHT_ONLY_ROUTES.some(r => url === r || url.startsWith(r + '/') || url.startsWith(r + '?'));
    this.apply(lightOnly ? 'light' : this.preferred);
  }
}
