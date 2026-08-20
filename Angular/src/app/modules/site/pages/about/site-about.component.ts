import { Component, OnInit, inject } from '@angular/core';
import { SiteService } from '../../services/site.service';
import { SiteSettings, TeamMember } from '../../models/site.model';
import { DEFAULT_SITE } from '../../services/default-site.config';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-about',
  standalone: true,
  imports: [BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'About Us' }]"></app-breadcrumb>
      </div>
    </div>

    <section class="py-5">
      <div class="container">
        <div class="row align-items-center g-5">
          <div class="col-lg-7">
            <h1 class="fw-bold mb-4">About <span style="color: var(--site-primary)">{{ settings?.companyName }}</span></h1>
            <p class="lead text-muted mb-4">{{ settings?.aboutText }}</p>
            <div class="row g-4">
              <div class="col-sm-6">
                <div class="p-4 rounded shadow-sm" style="background: rgba(var(--site-primary-rgb), 0.04)">
                  <h5 class="fw-bold" style="color: var(--site-primary)">Our Mission</h5>
                  <p class="text-muted mb-0">{{ settings?.mission }}</p>
                </div>
              </div>
              <div class="col-sm-6">
                <div class="p-4 rounded shadow-sm" style="background: rgba(var(--site-primary-rgb), 0.04)">
                  <h5 class="fw-bold" style="color: var(--site-primary)">Our Vision</h5>
                  <p class="text-muted mb-0">{{ settings?.vision }}</p>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-5 text-center">
            @if (settings?.heroImageUrl) {
              <img [src]="settings.heroImageUrl" class="img-fluid rounded shadow" alt="About">
            } @else {
              <div class="about-placeholder rounded"><i class="bi bi-building"></i></div>
            }
          </div>
        </div>
      </div>
    </section>

    @if (team.length) {
      <section class="py-5" style="background: #f8fafc">
        <div class="container">
          <h2 class="fw-bold text-center mb-5">Meet Our <span style="color: var(--site-primary)">Team</span></h2>
          <div class="row g-4 justify-content-center">
            @for (m of team; track m.id) {
              <div class="col-md-6 col-lg-3">
                <div class="card h-100 border-0 shadow-sm text-center p-4" style="border-radius: var(--site-radius)">
                  <div class="mx-auto mb-3">
                    @if (m.photoUrl) {
                      <img [src]="m.photoUrl" [alt]="m.name" class="rounded-circle" width="100" height="100" style="object-fit: cover">
                    } @else {
                      <div class="team-avatar">{{ m.name.charAt(0) }}</div>
                    }
                  </div>
                  <h6 class="fw-bold mb-1">{{ m.name }}</h6>
                  <small class="text-muted d-block mb-2">{{ m.role }}</small>
                  <p class="small text-muted mb-0">{{ m.bio }}</p>
                </div>
              </div>
            }
          </div>
        </div>
      </section>
    }
  `,
  styles: [`
    .about-placeholder { width: 100%; height: 300px; background: var(--site-gradient); opacity: 0.1; display: flex; align-items: center; justify-content: center; font-size: 4rem; color: var(--site-primary); }
    .team-avatar { width: 100px; height: 100px; border-radius: 50%; background: var(--site-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 2rem; font-weight: 700; margin: 0 auto; }
  `]
})
export class SiteAboutPage implements OnInit {
  private siteService = inject(SiteService);
  settings: SiteSettings = DEFAULT_SITE;
  team: TeamMember[] = [];

  ngOnInit(): void {
    this.siteService.getSettings().subscribe(s => this.settings = s);
    this.siteService.getTeam().subscribe(t => this.team = t);
  }
}
