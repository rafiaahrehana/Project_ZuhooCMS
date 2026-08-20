import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { SiteService } from '../../services/site.service';
import { BlogPost } from '../../models/site.model';
import { BreadcrumbComponent } from '../../components/breadcrumb/breadcrumb.component';

@Component({
  selector: 'app-site-blog-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BreadcrumbComponent, DatePipe, RouterLink],
  template: `
    <div class="pt-5" style="margin-top: 72px">
      <div class="container">
        <app-breadcrumb [items]="[{ label: 'Blog', url: '/blog' }, { label: post?.title || 'Post' }]"></app-breadcrumb>
      </div>
    </div>

    @if (loading) {
      <div class="text-center py-5"><div class="spinner-border" style="color: var(--site-primary)"></div></div>
    } @else if (post) {
      <article class="py-5">
        <div class="container" style="max-width: 800px">
          @if (post.coverImageUrl) {
            <img [src]="post.coverImageUrl" class="w-100 rounded shadow mb-4" style="max-height: 400px; object-fit: cover" [alt]="post.title">
          }
          <span class="badge mb-3" style="background: var(--site-primary); color: #fff">{{ post.category }}</span>
          <h1 class="fw-bold mb-3">{{ post.title }}</h1>
          <div class="d-flex align-items-center gap-3 text-muted mb-4">
            <span><i class="bi bi-person me-1"></i>{{ post.author }}</span>
            <span>&middot;</span>
            <span><i class="bi bi-calendar me-1"></i>{{ post.publishedAt | date:'mediumDate' }}</span>
            <span>&middot;</span>
            <span>{{ post.readMinutes }} min read</span>
          </div>
          <div class="blog-body" [innerHTML]="post.body"></div>
          <hr class="my-5">
          <a [routerLink]="basePath + '/blog'" class="btn btn-outline-dark" style="border-radius: var(--site-btn-radius)">
            <i class="bi bi-arrow-left me-2"></i>Back to Blog
          </a>
        </div>
      </article>
    }
  `,
  styles: [`
    .blog-body ::ng-deep p { line-height: 1.8; color: #475569; margin-bottom: 1.2rem; }
    .blog-body ::ng-deep h2, .blog-body ::ng-deep h3 { font-weight: 700; margin-top: 2rem; margin-bottom: 1rem; }
    .blog-body ::ng-deep img { border-radius: var(--site-radius); max-width: 100%; margin: 1.5rem 0; }
  `]
})
export class SiteBlogDetailPage implements OnInit {
  private route = inject(ActivatedRoute);
  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();
  constructor(private cdr: ChangeDetectorRef) {}
  post: BlogPost | null = null;
  loading = true;

  ngOnInit(): void {
    this.route.params.subscribe(p => {
      this.siteService.getBlog(p['slug']).subscribe(b => {
        this.post = b;
        this.loading = false;
        this.cdr.markForCheck();
      });
    });
  }
}
