import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { SiteService } from '../../services/site.service';
import { BlogPost } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';
import { BlogCardComponent } from '../../components/blog-card/blog-card.component';

@Component({
  selector: 'app-site-blog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BreadcrumbComponent, BlogCardComponent],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Blog' }]"></app-breadcrumb>
      </div>
    </div>

    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <h1 class="fw-bold">Our <span style="color: var(--site-primary)">Blog</span></h1>
          <p class="text-muted">Insights, tips and industry news.</p>
        </div>

        @if (loading) {
          <div class="text-center py-5"><div class="spinner-border" style="color: var(--site-primary)"></div></div>
        } @else if (posts.length) {
          <div class="row g-4">
            @for (p of posts; track p.id) {
              <div class="col-md-6 col-lg-4"><app-blog-card [post]="p"></app-blog-card></div>
            }
          </div>
        } @else {
          <div class="text-center py-5 text-muted"><i class="bi bi-newspaper" style="font-size: 3rem; opacity: 0.3"></i><p class="mt-3">No posts yet.</p></div>
        }
      </div>
    </section>
  `,
  styles: [`.btn-primary { background: var(--site-primary); border-color: var(--site-primary); color: #fff; }`]
})
export class SiteBlogPage implements OnInit {
  private siteService = inject(SiteService);
  constructor(private cdr: ChangeDetectorRef) {}
  posts: BlogPost[] = [];
  loading = true;

  ngOnInit(): void {
    this.siteService.getBlogs().subscribe(p => {
      this.posts = p;
      this.loading = false;
      this.cdr.markForCheck();
    });
  }
}
