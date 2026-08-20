import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { NotificationBell } from '../notification-bell/notification-bell';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, FormsModule, NotificationBell, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  searchQuery = '';
  breadcrumb: string[] = [];
  isFullscreen = false;

  constructor(
    public auth: AuthService,
    public theme: ThemeService,
    private router: Router,
  ) {
    this.router.events.subscribe(() => this.buildBreadcrumb());
    this.buildBreadcrumb();

    // Esc leaves fullscreen without going through the button, so the icon has to
    // follow the document rather than a local toggle, or it goes stale.
    document.addEventListener('fullscreenchange', () => {
      this.isFullscreen = !!document.fullscreenElement;
    });

    this.restoreSidebarState();
  }

  /** Reapply the remembered desktop collapse, but never on a narrow viewport. */
  private restoreSidebarState(): void {
    try {
      if (localStorage.getItem('sidebar-collapsed') === 'true' && window.innerWidth >= 992) {
        document.body.classList.add('sidebar-collapsed');
      }
    } catch {
      // Storage unavailable - start expanded.
    }
  }

  /** Second line of the workspace label in the header. */
  get workspaceName(): string {
    if (this.auth.hasRole('CLIENT')) return 'Client Portal';
    if (this.auth.isPlatformUser()) return 'Platform Admin';
    return 'Command Center';
  }

  toggleFullscreen(): void {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {
        // Browsers reject this outside a user gesture or when the page is
        // embedded with fullscreen disallowed; nothing useful to do but ignore.
      });
    } else {
      document.exitFullscreen();
    }
  }

  private buildBreadcrumb(): void {
    const segments = this.router.url.split('?')[0].split('/').filter(Boolean);
    this.breadcrumb = segments.map((s) =>
      s.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
    );
  }

  /**
   * One button, two behaviours - matching how a desktop rail and a phone drawer
   * differ: on a wide viewport the rail collapses to icons in place (and the
   * choice is remembered), on a narrow one it slides in over the content (and
   * the choice is not remembered, because a drawer left open across navigations
   * would be in the way).
   */
  toggleSidebar(): void {
    if (window.innerWidth >= 992) {
      const collapsed = document.body.classList.toggle('sidebar-collapsed');
      try {
        localStorage.setItem('sidebar-collapsed', String(collapsed));
      } catch {
        // Private-mode browsers throw on write; collapsing still works, it just
        // will not survive a reload.
      }
    } else {
      document.body.classList.toggle('sidebar-open');
    }
  }

  goSearch(): void {
    if (this.searchQuery && this.searchQuery.trim().length > 0) {
      this.router.navigate(['/search'], { queryParams: { q: this.searchQuery.trim() } });
      this.searchQuery = '';
    } else {
      this.router.navigate(['/search']);
    }
  }

  goSearchAi(): void {
    if (this.searchQuery && this.searchQuery.trim().length > 0) {
      this.router.navigate(['/search'], { queryParams: { q: this.searchQuery.trim(), ai: 'true' } });
      this.searchQuery = '';
    } else {
      this.router.navigate(['/ai']);
    }
  }

  roleLabel(roles: string[] | undefined | null): string {
    const role = roles?.[0];
    if (!role) return '';
    return role
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }

  // The client portal has its own profile page (sidebar "Profile Settings" ->
  // /client/profile) - sending clients to the generic /profile
  // page instead would drop them outside the client portal shell entirely.
  // Platform staff (SUPER_ADMIN etc.) have no Employee HR record, so /my-profile
  // (which reads GET /api/employees/me) would just error for them - keep them on
  // the generic /profile page. Tenant users (COMPANY_OWNER/EMPLOYEE) go to
  // /my-profile instead, which now covers everything /profile did plus their HR
  // details (address, education) and stays consistent with the Welcome landing page.
  settingsLink(): string {
    if (this.auth.hasRole('CLIENT')) return '/client/profile';
    if (this.auth.isPlatformUser()) return '/profile';
    return '/my-profile';
  }

  // Account Settings (notification preferences + security) - clients already
  // manage those from within /client/profile, so only tenant/platform users
  // get a distinct destination here.
  accountSettingsLink(): string {
    if (this.auth.hasRole('CLIENT')) return '/client/profile';
    return '/profile';
  }
}
