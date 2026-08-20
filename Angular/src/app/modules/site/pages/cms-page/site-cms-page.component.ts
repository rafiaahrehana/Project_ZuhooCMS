import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SiteService } from '../../services/site.service';
import { CmsPage } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-cms-page',
  standalone: true,
  imports: [BreadcrumbComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: page?.title || slug }]"></app-breadcrumb>
      </div>
    </div>
    <section class="py-5">
      <div class="container" style="max-width: 800px">
        @if (page) {
          <h1 class="fw-bold mb-4">{{ page.title }}</h1>
          <div class="cms-body" [innerHTML]="page.body"></div>
        }
      </div>
    </section>
  `,
  styles: [`.cms-body ::ng-deep p { line-height: 1.8; color: #475569; } .cms-body ::ng-deep h2, .cms-body ::ng-deep h3 { font-weight: 700; }`]
})
export class SiteCmsPageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private siteService = inject(SiteService);
  slug = '';
  page: CmsPage | null = null;

  ngOnInit(): void {
    this.route.params.subscribe(p => {
      this.slug = p['slug'];
      this.siteService.getPage(this.slug).subscribe(pg => this.page = pg);
    });
  }
}
