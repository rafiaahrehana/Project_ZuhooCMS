import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { SiteService } from '../../services/site.service';
import { TeamMember } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-team',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Our Team' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h1 class="fw-bold">Meet Our <span style="color: var(--site-primary)">Team</span></h1>
          <p class="text-muted">The people behind our success.</p>
        </div>
        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border" style="color: var(--site-primary)"></div></div>
        } @else {
          <div class="row g-4 justify-content-center">
            @for (m of members; track m.id) {
              <div class="col-md-6 col-lg-3">
                <div class="card h-100 border-0 shadow-sm text-center p-4" style="border-radius: var(--site-radius)">
                  <div class="mx-auto mb-3">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" [alt]="m.name" class="rounded-circle" width="120" height="120" style="object-fit: cover">
                    } @else {
                      <div class="avatar-lg">{{ m.name.charAt(0) }}</div>
                    }
                  </div>
                  <h6 class="fw-bold mb-1">{{ m.name }}</h6>
                  <small class="text-primary fw-semibold d-block mb-2">{{ m.role }}</small>
                  <p class="small text-muted mb-0">{{ m.bio }}</p>
                  @if (m.email) {
                    <a [href]="'mailto:' + m.email" class="mt-3 small"><i class="bi bi-envelope me-1"></i>{{ m.email }}</a>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>
    </section>
  `,
  styles: [`.avatar-lg { width: 120px; height: 120px; border-radius: 50%; background: var(--site-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; font-weight: 700; margin: 0 auto; }`]
})
export class SiteTeamPage implements OnInit {
  private siteService = inject(SiteService);
  constructor(private cdr: ChangeDetectorRef) {}
  members: TeamMember[] = [];
  loading = true;
  ngOnInit(): void {
    this.siteService.getTeam().subscribe(t => {
      this.members = t;
      this.loading = false;
      this.cdr.markForCheck();
    });
  }
}
