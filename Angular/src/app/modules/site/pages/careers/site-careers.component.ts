import { Component, OnInit, inject } from '@angular/core';
import { SiteService } from '../../services/site.service';
import { CmsPage, SiteSettings } from '../../models/site.model';
import { DEFAULT_SITE, DEFAULT_PAGES } from '../../services/default-site.config';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-careers',
  standalone: true,
  imports: [BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Careers' }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container" style="max-width: 800px">
        <h1 class="fw-bold mb-4">Careers at <span style="color: var(--site-primary)">{{ settings?.companyName }}</span></h1>
        @if (page) {
          <div class="cms-body mb-5" [innerHTML]="page.body"></div>
        }
        <div class="text-center py-4">
          <p class="text-muted">Interested? Send your resume to <a [href]="'mailto:' + settings?.email" style="color: var(--site-primary)">{{ settings?.email }}</a></p>
        </div>
      </div>
    </section>
  `,
  styles: [`.cms-body ::ng-deep p { line-height: 1.8; color: #475569; }`]
})
export class SiteCareersPage implements OnInit {
  private siteService = inject(SiteService);
  settings: SiteSettings = DEFAULT_SITE;
  page: CmsPage | null = null;

  ngOnInit(): void {
    this.siteService.getSettings().subscribe(s => this.settings = s);
    this.siteService.getPage('careers').subscribe(p => this.page = p);
  }
}
