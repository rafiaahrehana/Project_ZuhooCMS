import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteService } from '../../services/site.service';
import { SiteSettings, Stat, Service, Testimonial, BlogPost, PricingPlan, Faq } from '../../models/site.model';
import { DEFAULT_SITE } from '../../services/default-site.config';
import { SiteHeroComponent } from '../../components/hero/hero.component';
import { StatCardComponent } from '../../components/stat-card/stat-card.component';
import { ServiceCardComponent } from '../../components/service-card/service-card.component';
import { TestimonialCardComponent } from '../../components/testimonial-card/testimonial-card.component';
import { BlogCardComponent } from '../../components/blog-card/blog-card.component';
import { FaqItemComponent } from '../../components/faq-item/faq-item.component';
import { PricingCardComponent } from '../../components/pricing-card/pricing-card.component';

@Component({
  selector: 'app-site-home',
  standalone: true,
  imports: [RouterLink, SiteHeroComponent, StatCardComponent, ServiceCardComponent, TestimonialCardComponent, BlogCardComponent, FaqItemComponent, PricingCardComponent],
  template: `
    <app-site-hero [settings]="settings"></app-site-hero>

    @if (stats.length) {
      <section class="py-5 stats-strip">
        <div class="container">
          <div class="row g-4">
            @for (s of stats; track $index) {
              <div class="col-6 col-md-3"><app-stat-card [stat]="s"></app-stat-card></div>
            }
          </div>
        </div>
      </section>
    }

    <section class="py-5 section-tint position-relative overflow-hidden">
      <div class="blob blob-a"></div>
      <div class="container position-relative">
        <div class="row align-items-center g-5">
          <div class="col-lg-6">
            <span class="eyebrow">About Us</span>
            <h2 class="fw-bold mb-3 mt-2">Built on <span class="text-gradient">trust</span> and results</h2>
            <p class="text-muted mb-4">{{ settings?.aboutText }}</p>
            <div class="row g-3">
              <div class="col-6">
                <div class="p-3 info-tile">
                  <div class="info-tile-icon mb-2"><i class="bi bi-bullseye"></i></div>
                  <h6 class="fw-bold mb-1">Our Mission</h6>
                  <small class="text-muted">{{ settings?.mission }}</small>
                </div>
              </div>
              <div class="col-6">
                <div class="p-3 info-tile">
                  <div class="info-tile-icon mb-2"><i class="bi bi-eye"></i></div>
                  <h6 class="fw-bold mb-1">Our Vision</h6>
                  <small class="text-muted">{{ settings?.vision }}</small>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-6 text-center">
            @if (settings?.heroImageUrl) {
              <img [src]="settings.heroImageUrl" class="img-fluid rounded shadow-lg" alt="About" style="max-height: 360px; border-radius: var(--site-radius)">
            } @else {
              <div class="about-placeholder rounded"><i class="bi bi-building"></i></div>
            }
          </div>
        </div>
      </div>
    </section>

    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <span class="eyebrow">Why Choose Us</span>
          <h2 class="fw-bold mt-2">The <span class="text-gradient">advantage</span> we bring</h2>
          <p class="text-muted">Reasons businesses trust us to deliver, again and again.</p>
        </div>
        <div class="row g-4">
          @for (f of whyChooseUs; track f.title) {
            <div class="col-md-6 col-lg-4">
              <div class="feature-card h-100 p-4">
                <div class="feature-icon mb-3"><i class="bi" [class]="f.icon"></i></div>
                <h5 class="fw-bold">{{ f.title }}</h5>
                <p class="text-muted mb-0 small">{{ f.text }}</p>
              </div>
            </div>
          }
        </div>
      </div>
    </section>

    @if (services.length) {
      <section class="py-5 section-tint">
        <div class="container">
          <div class="text-center mb-5">
            <span class="eyebrow">What We Offer</span>
            <h2 class="fw-bold mt-2">Our <span class="text-gradient">Services</span></h2>
            <p class="text-muted">Comprehensive solutions tailored to your business needs.</p>
          </div>
          <div class="row g-4">
            @for (s of services.slice(0, 4); track s.id) {
              <div class="col-md-6 col-lg-3"><app-service-card [service]="s"></app-service-card></div>
            }
          </div>
          <div class="text-center mt-4">
            <a [routerLink]="basePath + '/services'" class="btn btn-brand-outline px-4" style="border-radius: var(--site-btn-radius)">View All Services</a>
          </div>
        </div>
      </section>
    }

    <section class="py-5">
      <div class="container">
        <div class="text-center mb-5">
          <span class="eyebrow">Our Process</span>
          <h2 class="fw-bold mt-2">How We <span class="text-gradient">Work</span></h2>
          <p class="text-muted">A simple, transparent process from first contact to delivery.</p>
        </div>
        <div class="row g-4">
          @for (step of howWeWork; track step.title; let i = $index) {
            <div class="col-md-6 col-lg-3">
              <div class="step-card h-100 p-4 text-center position-relative">
                <div class="step-number mb-3">{{ i + 1 }}</div>
                <h6 class="fw-bold">{{ step.title }}</h6>
                <p class="text-muted small mb-0">{{ step.text }}</p>
              </div>
            </div>
          }
        </div>
      </div>
    </section>

    @if (projects.length) {
      <section class="py-5 section-tint">
        <div class="container">
          <div class="text-center mb-5">
            <span class="eyebrow">Portfolio</span>
            <h2 class="fw-bold mt-2">Our <span class="text-gradient">Work</span></h2>
            <p class="text-muted">Recent projects that showcase our expertise.</p>
          </div>
          <div class="row g-4">
            @for (p of projects.slice(0, 3); track p.id) {
              <div class="col-md-4">
                <div class="card h-100 border-0 shadow-sm overflow-hidden project-card" style="border-radius: var(--site-radius)">
                  @if (p.coverImageUrl) {
                    <img [src]="p.coverImageUrl" class="card-img-top" [alt]="p.title" style="height: 200px; object-fit: cover">
                  } @else {
                    <div class="project-placeholder"><i class="bi bi-folder2-open"></i></div>
                  }
                  <div class="card-body p-4">
                    <span class="badge mb-2 badge-brand">{{ p.category }}</span>
                    <h5 class="fw-bold">{{ p.title }}</h5>
                    <p class="text-muted small">{{ p.summary }}</p>
                  </div>
                </div>
              </div>
            }
          </div>
          <div class="text-center mt-4">
            <a [routerLink]="basePath + '/portfolio'" class="btn btn-brand-outline px-4" style="border-radius: var(--site-btn-radius)">View Portfolio</a>
          </div>
        </div>
      </section>
    }

    @if (pricing.length) {
      <section class="py-5">
        <div class="container">
          <div class="text-center mb-5">
            <span class="eyebrow">Pricing</span>
            <h2 class="fw-bold mt-2">Simple <span class="text-gradient">Plans</span></h2>
            <p class="text-muted">Simple, transparent pricing for every business.</p>
          </div>
          <div class="row g-4 justify-content-center">
            @for (p of pricing.slice(0, 3); track p.id) {
              <div class="col-md-6 col-lg-4"><app-pricing-card [plan]="p"></app-pricing-card></div>
            }
          </div>
        </div>
      </section>
    }

    @if (testimonials.length) {
      <section class="py-5 section-tint">
        <div class="container">
          <div class="text-center mb-5">
            <span class="eyebrow">Testimonials</span>
            <h2 class="fw-bold mt-2">What Clients <span class="text-gradient">Say</span></h2>
          </div>
          <div class="row g-4">
            @for (t of testimonials; track t.id) {
              <div class="col-md-6 col-lg-4"><app-testimonial-card [t]="t"></app-testimonial-card></div>
            }
          </div>
        </div>
      </section>
    }

    @if (blogs.length) {
      <section class="py-5">
        <div class="container">
          <div class="text-center mb-5">
            <span class="eyebrow">Insights</span>
            <h2 class="fw-bold mt-2">Latest <span class="text-gradient">Blog</span></h2>
            <p class="text-muted">Insights, tips and industry news.</p>
          </div>
          <div class="row g-4">
            @for (b of blogs.slice(0, 3); track b.id) {
              <div class="col-md-6 col-lg-4"><app-blog-card [post]="b"></app-blog-card></div>
            }
          </div>
          <div class="text-center mt-4">
            <a [routerLink]="basePath + '/blog'" class="btn btn-brand-outline px-4" style="border-radius: var(--site-btn-radius)">View All Posts</a>
          </div>
        </div>
      </section>
    }

    @if (faqs.length) {
      <section class="py-5 section-tint">
        <div class="container">
          <div class="row justify-content-center">
            <div class="col-lg-8">
              <div class="text-center mb-5">
                <span class="eyebrow">FAQ</span>
                <h2 class="fw-bold mt-2">Frequently Asked <span class="text-gradient">Questions</span></h2>
              </div>
              <div class="accordion" id="faqAccordion">
                @for (f of faqs.slice(0, 5); track f.id) {
                  <app-faq-item [faq]="f"></app-faq-item>
                }
              </div>
            </div>
          </div>
        </div>
      </section>
    }

    <section class="py-5">
      <div class="container">
        <div class="cta-box text-center text-white p-5 position-relative overflow-hidden" style="border-radius: var(--site-radius)">
          <div class="cta-pattern"></div>
          <div class="position-relative">
            <h2 class="fw-bold mb-3">Ready to Get Started?</h2>
            <p class="mb-4 opacity-75">Let us help you take your business to the next level.</p>
            <div class="d-flex justify-content-center gap-3 flex-wrap">
              <a [routerLink]="basePath + '/request-service'" class="btn btn-light btn-lg px-5" style="border-radius: var(--site-btn-radius)">
                Get a Free Quote <i class="bi bi-arrow-right ms-2"></i>
              </a>
              <a [routerLink]="basePath + '/contact'" class="btn btn-outline-light btn-lg px-5" style="border-radius: var(--site-btn-radius)">
                Contact Us
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .eyebrow { display: inline-block; font-size: 0.78rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--site-secondary); background: rgba(var(--site-primary-rgb), 0.08); padding: 4px 12px; border-radius: 999px; }
    .text-gradient { background: var(--site-gradient); -webkit-background-clip: text; background-clip: text; color: transparent; }

    .section-tint { background: linear-gradient(180deg, rgba(var(--site-primary-rgb), 0.05), rgba(var(--site-primary-rgb), 0.015)); position: relative; }
    .stats-strip { background: rgba(var(--site-primary-rgb), 0.03); }

    .blob { position: absolute; border-radius: 50%; filter: blur(70px); opacity: 0.12; z-index: 0; pointer-events: none; }
    .blob-a { width: 420px; height: 420px; background: var(--site-gradient); top: -120px; right: -120px; }

    .about-placeholder { width: 100%; height: 320px; background: var(--site-gradient); opacity: 0.1; display: flex; align-items: center; justify-content: center; font-size: 5rem; color: var(--site-primary); border-radius: var(--site-radius); }
    .project-placeholder { height: 200px; background: var(--site-gradient); opacity: 0.1; display: flex; align-items: center; justify-content: center; font-size: 3rem; color: var(--site-primary); }
    .project-card { transition: transform 0.3s, box-shadow 0.3s; }
    .project-card:hover { transform: translateY(-6px); box-shadow: 0 16px 32px rgba(var(--site-primary-rgb), 0.15) !important; }

    .info-tile { background: #fff; border: 1px solid rgba(var(--site-primary-rgb), 0.1); border-radius: var(--site-radius); transition: box-shadow 0.3s, transform 0.3s; }
    .info-tile:hover { box-shadow: 0 10px 24px rgba(var(--site-primary-rgb), 0.12); transform: translateY(-3px); }
    .info-tile-icon { width: 36px; height: 36px; border-radius: 10px; background: var(--site-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 0.95rem; }

    .feature-card { background: #fff; border: 1px solid rgba(var(--site-primary-rgb), 0.1); border-radius: var(--site-radius); transition: transform 0.3s, box-shadow 0.3s, border-color 0.3s; }
    .feature-card:hover { transform: translateY(-6px); box-shadow: 0 16px 34px rgba(var(--site-primary-rgb), 0.14); border-color: rgba(var(--site-primary-rgb), 0.25); }
    .feature-icon { width: 52px; height: 52px; border-radius: 14px; background: var(--site-gradient); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; }

    .step-card { background: rgba(var(--site-primary-rgb), 0.03); border-radius: var(--site-radius); transition: background 0.3s, transform 0.3s; }
    .step-card:hover { background: rgba(var(--site-primary-rgb), 0.07); transform: translateY(-4px); }
    .step-number { width: 44px; height: 44px; margin: 0 auto; border-radius: 50%; background: var(--site-gradient); color: #fff; font-weight: 700; display: flex; align-items: center; justify-content: center; }

    .badge-brand { background: var(--site-primary); color: #fff; }
    .btn-brand-outline { border: 1px solid var(--site-primary); color: var(--site-primary); background: transparent; }
    .btn-brand-outline:hover { background: var(--site-primary); color: #fff; }

    .cta-box { background: var(--site-gradient); }
    .cta-pattern { position: absolute; inset: 0; background-image: radial-gradient(circle, rgba(255,255,255,0.18) 1.5px, transparent 1.5px); background-size: 22px 22px; opacity: 0.5; }
    .btn-outline-light { border: 1px solid rgba(255,255,255,0.7); }
    .btn-outline-light:hover { background: rgba(255,255,255,0.15); color: #fff; }
  `]
})
export class SiteHomePage implements OnInit {
  private siteService = inject(SiteService);
  basePath = this.siteService.getBasePath();

  settings: SiteSettings = DEFAULT_SITE;
  stats: Stat[] = [];
  services: Service[] = [];
  testimonials: Testimonial[] = [];
  blogs: BlogPost[] = [];
  pricing: PricingPlan[] = [];
  faqs: Faq[] = [];
  projects: any[] = [];

  whyChooseUs = [
    { icon: 'bi-award', title: 'Expert Team', text: 'Seasoned professionals with deep, hands-on industry experience.' },
    { icon: 'bi-lightning-charge', title: 'Fast Turnaround', text: 'Streamlined delivery so you get results without the wait.' },
    { icon: 'bi-currency-dollar', title: 'Transparent Pricing', text: 'No hidden fees — clear quotes before any work begins.' },
    { icon: 'bi-headset', title: '24/7 Support', text: 'Real people ready to help, whenever you need us.' },
    { icon: 'bi-shield-check', title: 'Secure & Confidential', text: 'Your data and business information stay fully protected.' },
    { icon: 'bi-graph-up-arrow', title: 'Proven Results', text: 'A track record of measurable outcomes for our clients.' },
  ];

  howWeWork = [
    { title: 'Consultation', text: 'Share your goals — we listen and understand what you need.' },
    { title: 'Proposal', text: 'We craft a tailored plan with clear scope and pricing.' },
    { title: 'Execution', text: 'Our experts get to work, keeping you updated at every step.' },
    { title: 'Delivery & Support', text: 'We deliver on time and stay on hand for ongoing support.' },
  ];

  ngOnInit(): void {
    this.siteService.getSettings().subscribe(s => this.settings = s);
    this.siteService.getStats().subscribe(s => this.stats = s);
    this.siteService.getServices().subscribe(s => this.services = s);
    this.siteService.getTestimonials().subscribe(t => this.testimonials = t);
    this.siteService.getBlogs().subscribe(b => this.blogs = b);
    this.siteService.getPricing().subscribe(p => this.pricing = p);
    this.siteService.getFaqs().subscribe(f => this.faqs = f);
    this.siteService.getProjects().subscribe(p => this.projects = p);
  }
}
