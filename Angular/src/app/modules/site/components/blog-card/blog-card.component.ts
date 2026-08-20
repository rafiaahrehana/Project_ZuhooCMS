import { Component, Input } from '@angular/core';
import { BlogPost } from '../../models/site.model';

@Component({
  selector: 'app-blog-card',
  standalone: true,
  template: `
    <div class="card h-100 border-0 shadow-sm blog-card overflow-hidden">
      <div class="blog-img-wrap">
        @if (post.coverImageUrl) {
          <img [src]="post.coverImageUrl" class="card-img-top" [alt]="post.title">
        } @else {
          <div class="blog-img-placeholder"><i class="bi bi-newspaper"></i></div>
        }
        <span class="badge-tag">{{ post.category }}</span>
      </div>
      <div class="card-body p-4">
        <div class="d-flex align-items-center gap-2 mb-2 text-muted small">
          <span><i class="bi bi-person me-1"></i>{{ post.author }}</span>
          <span>&middot;</span>
          <span>{{ post.readMinutes }} min read</span>
        </div>
        <h5 class="card-title fw-bold">{{ post.title }}</h5>
        <p class="card-text text-muted">{{ post.excerpt }}</p>
        <a [href]="'/blog/' + post.slug" class="stretched-link"><span class="visually-hidden">Read more</span></a>
      </div>
    </div>
  `,
  styles: [`
    .blog-card { border-radius: var(--site-radius); transition: all 0.3s; cursor: pointer; }
    .blog-card:hover { transform: translateY(-6px); box-shadow: 0 12px 30px rgba(0,0,0,0.1) !important; }
    .blog-img-wrap { position: relative; overflow: hidden; }
    .blog-img-wrap img { height: 200px; object-fit: cover; width: 100%; }
    .blog-img-placeholder { height: 200px; background: var(--site-gradient); opacity: 0.12; display: flex; align-items: center; justify-content: center; font-size: 3rem; color: var(--site-primary); }
    .badge-tag { position: absolute; top: 12px; left: 12px; background: var(--site-primary); color: #fff; padding: 4px 12px; border-radius: 999px; font-size: 0.75rem; font-weight: 600; }
  `]
})
export class BlogCardComponent {
  @Input({ required: true }) post!: BlogPost;
}
