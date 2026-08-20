import { Component, Input, HostListener, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteSettings, NavItem } from '../../models/site.model';
import { SiteService } from '../../services/site.service';

@Component({
  selector: 'app-site-navbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav class="navbar navbar-expand-lg fixed-top"
         [class.scrolled]="scrolled"
         [class.navbar-dark]="isDark"
         [class.navbar-light]="!isDark">
      <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2" [routerLink]="basePath || '/'">
          @if (settings?.logoUrl) {
            <img [src]="settings!.logoUrl" [alt]="settings!.companyName" height="36" class="rounded">
          } @else {
            <div class="brand-icon">
              <i class="bi bi-grid-3x3-gap-fill"></i>
            </div>
          }
          <span class="fw-bold fs-5">{{ settings?.companyName || 'Company' }}</span>
        </a>
        <button class="navbar-toggler border-0" type="button" (click)="collapsed = !collapsed">
          <i class="bi" [class.bi-list]="collapsed" [class.bi-x-lg]="!collapsed"
             [style.font-size]="'1.4rem'"></i>
        </button>
        <div class="collapse navbar-collapse" [class.show]="!collapsed">
          <ul class="navbar-nav ms-auto gap-lg-1">
            @for (item of nav; track item.id || item.label) {
              @if (item.children?.length) {
                <li class="nav-item dropdown">
                  <a class="nav-link dropdown-toggle" href="javascript:void(0)" role="button" data-bs-toggle="dropdown">
                    {{ item.label }}
                  </a>
                  <ul class="dropdown-menu dropdown-menu-end">
                    @for (child of item.children; track child.id || child.label) {
                      <li>
                        @if (child.external) {
                          <a class="dropdown-item" [href]="child.url" target="_blank">{{ child.label }}</a>
                        } @else {
                          <a class="dropdown-item" [routerLink]="basePath + child.url">{{ child.label }}</a>
                        }
                      </li>
                    }
                  </ul>
                </li>
              } @else {
                <li class="nav-item">
                  @if (item.external) {
                    <a class="nav-link" [href]="item.url" target="_blank">{{ item.label }}</a>
                  } @else {
                    <a class="nav-link" [routerLink]="basePath + item.url">{{ item.label }}</a>
                  }
                </li>
              }
            }
            <li class="nav-item">
              <a class="btn btn-primary ms-lg-3 px-4" style="border-radius: var(--site-btn-radius)"
                 [routerLink]="basePath + '/request-service'">
                Get a Quote
              </a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar { padding: 0.8rem 0; transition: all 0.3s ease; }
    .navbar.scrolled { background: rgba(255,255,255,0.97) !important; box-shadow: 0 2px 20px rgba(0,0,0,0.08); padding: 0.5rem 0; }
    .theme-dark .navbar.scrolled { background: rgba(15,23,42,0.97) !important; }
    .brand-icon { width: 36px; height: 36px; border-radius: 10px; background: var(--site-gradient); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 1rem; }
    .navbar.scrolled .brand-icon { width: 32px; height: 32px; }
    .dropdown-menu { border: none; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border-radius: var(--site-radius); }
    .dropdown-item { padding: 0.5rem 1rem; border-radius: 8px; }
    .dropdown-item:hover { background: rgba(var(--site-primary-rgb), 0.1); }
  `]
})
export class SiteNavbarComponent {
  @Input() settings: SiteSettings | null = null;
  @Input() nav: NavItem[] = [];

  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();

  collapsed = true;
  scrolled = false;

  get isDark(): boolean {
    return this.settings?.theme?.darkMode || false;
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled = window.scrollY > 50;
  }
}
