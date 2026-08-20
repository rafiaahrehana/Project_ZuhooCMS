import { Component, Input, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SiteSettings } from '../../models/site.model';
import { SiteService } from '../../services/site.service';

@Component({
  selector: 'app-site-footer',
  standalone: true,
  imports: [NgClass, RouterLink],
  template: `
    <footer class="site-footer" [class.footer-dark]="settings?.theme?.footerStyle === 'dark'"
            [class.footer-light]="settings?.theme?.footerStyle === 'light'">
      <div class="container py-5">
        <div class="row g-4">
          <div class="col-lg-4 col-md-6">
            <div class="d-flex align-items-center gap-2 mb-3">
              @if (settings?.logoUrl) {
                <img [src]="settings!.logoUrl" alt="" height="32" class="rounded">
              } @else {
                <div class="brand-icon-sm"><i class="bi bi-grid-3x3-gap-fill"></i></div>
              }
              <h5 class="mb-0 fw-bold">{{ settings?.companyName }}</h5>
            </div>
            <p class="mb-3 opacity-75">{{ settings?.tagline }}</p>
            <div class="d-flex gap-2">
              @for (link of settings?.socialLinks || []; track link.platform) {
                <a [href]="link.url" target="_blank" class="social-link" [attr.aria-label]="link.platform">
                  <i class="bi" [ngClass]="link.icon || 'bi-link-45deg'"></i>
                </a>
              }
            </div>
          </div>
          <div class="col-lg-2 col-md-6">
            <h6 class="fw-bold mb-3 text-uppercase small">Company</h6>
            <ul class="list-unstyled">
              <li><a [routerLink]="basePath + '/about'">About</a></li>
              <li><a [routerLink]="basePath + '/services'">Services</a></li>
              <li><a [routerLink]="basePath + '/portfolio'">Portfolio</a></li>
              <li><a [routerLink]="basePath + '/careers'">Careers</a></li>
              <li><a [routerLink]="basePath + '/contact'">Contact</a></li>
            </ul>
          </div>
          <div class="col-lg-2 col-md-6">
            <h6 class="fw-bold mb-3 text-uppercase small">Resources</h6>
            <ul class="list-unstyled">
              <li><a [routerLink]="basePath + '/blog'">Blog</a></li>
              <li><a [routerLink]="basePath + '/pricing'">Pricing</a></li>
              <li><a [routerLink]="basePath + '/faq'">FAQ</a></li>
              <li><a [routerLink]="basePath + '/team'">Team</a></li>
              <li><a [routerLink]="basePath + '/book-consultation'">Book Consultation</a></li>
            </ul>
          </div>
          <div class="col-lg-4 col-md-6">
            <h6 class="fw-bold mb-3 text-uppercase small">Contact</h6>
            <ul class="list-unstyled">
              @if (settings?.address) { <li class="mb-2"><i class="bi bi-geo-alt me-2"></i>{{ settings.address }}</li> }
              @if (settings?.phone) { <li class="mb-2"><i class="bi bi-telephone me-2"></i>{{ settings.phone }}</li> }
              @if (settings?.email) { <li class="mb-2"><i class="bi bi-envelope me-2"></i>{{ settings.email }}</li> }
            </ul>
          </div>
        </div>
        <hr class="my-4" [style.opacity]="'0.15'">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <small class="opacity-75">&copy; {{ settings?.copyright || currentYear + ' All rights reserved.' }}</small>
          <div class="d-flex gap-3">
            <a [routerLink]="basePath + '/privacy'" class="small">Privacy</a>
            <a [routerLink]="basePath + '/terms'" class="small">Terms</a>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .site-footer { padding-top: 0; }
    .footer-dark { background: #0f172a; color: #e2e8f0; }
    .footer-light { background: #f8fafc; color: #334155; }
    .site-footer a { color: inherit; text-decoration: none; opacity: 0.75; transition: opacity 0.2s; }
    .site-footer a:hover { opacity: 1; }
    .social-link { width: 36px; height: 36px; border-radius: 10px; background: rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; font-size: 0.9rem; }
    .footer-light .social-link { background: rgba(0,0,0,0.05); }
    .brand-icon-sm { width: 32px; height: 32px; border-radius: 8px; background: var(--site-gradient); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 0.85rem; }
    .list-unstyled li { margin-bottom: 0.4rem; }
  `]
})
export class SiteFooterComponent {
  @Input() settings: SiteSettings | null = null;
  currentYear = new Date().getFullYear();

  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
}
