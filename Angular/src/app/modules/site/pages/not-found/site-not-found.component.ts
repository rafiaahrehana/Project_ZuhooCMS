import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteService } from '../../services/site.service';

@Component({
  selector: 'app-site-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="not-found d-flex align-items-center justify-content-center text-center" style="min-height: 80vh">
      <div>
        <h1 class="display-1 fw-bold" style="color: var(--site-primary); opacity: 0.2">404</h1>
        <h3 class="fw-bold mb-3">Page Not Found</h3>
        <p class="text-muted mb-4">The page you're looking for doesn't exist or has been moved.</p>
        <a [routerLink]="basePath || '/'" class="btn btn-primary px-4" style="border-radius: var(--site-btn-radius)">
          <i class="bi bi-house me-2"></i>Go Home
        </a>
      </div>
    </div>
  `,
  styles: [`.btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }`]
})
export class SiteNotFoundComponent {
  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
}
