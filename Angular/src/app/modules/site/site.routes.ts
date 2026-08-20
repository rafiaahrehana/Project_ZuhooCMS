import { Routes } from '@angular/router';
import { SiteLayoutComponent } from './layout/site-layout.component';

export const SITE_ROUTES: Routes = [
  {
    path: '',
    component: SiteLayoutComponent,
    children: [
      { path: '', loadComponent: () => import('./pages/home/site-home.component').then(m => m.SiteHomePage) },
      { path: 'about', loadComponent: () => import('./pages/about/site-about.component').then(m => m.SiteAboutPage) },
      { path: 'services', loadComponent: () => import('./pages/services/site-services.component').then(m => m.SiteServicesPage) },
      { path: 'services/:slug', loadComponent: () => import('./pages/service-detail/site-service-detail.component').then(m => m.SiteServiceDetailPage) },
      { path: 'pricing', loadComponent: () => import('./pages/pricing/site-pricing.component').then(m => m.SitePricingPage) },
      { path: 'portfolio', loadComponent: () => import('./pages/home/site-home.component').then(m => m.SiteHomePage) },
      { path: 'blog', loadComponent: () => import('./pages/blog/site-blog.component').then(m => m.SiteBlogPage) },
      { path: 'blog/:slug', loadComponent: () => import('./pages/blog-detail/site-blog-detail.component').then(m => m.SiteBlogDetailPage) },
      { path: 'team', loadComponent: () => import('./pages/team/site-team.component').then(m => m.SiteTeamPage) },
      { path: 'faq', loadComponent: () => import('./pages/faq/site-faq.component').then(m => m.SiteFaqPage) },
      { path: 'contact', loadComponent: () => import('./pages/contact/site-contact.component').then(m => m.SiteContactPage) },
      { path: 'request-service', loadComponent: () => import('./pages/request-service/site-request-service.component').then(m => m.SiteRequestServicePage) },
      { path: 'track-request', loadComponent: () => import('./pages/track-request/site-track-request.component').then(m => m.SiteTrackRequestPage) },
      { path: 'careers', loadComponent: () => import('./pages/careers/site-careers.component').then(m => m.SiteCareersPage) },
      { path: 'book-consultation', loadComponent: () => import('./pages/book-consultation/site-book-consultation.component').then(m => m.SiteBookConsultationPage) },
      { path: 'privacy', loadComponent: () => import('./pages/cms-page/site-cms-page.component').then(m => m.SiteCmsPageComponent) },
      { path: 'terms', loadComponent: () => import('./pages/cms-page/site-cms-page.component').then(m => m.SiteCmsPageComponent) },
      { path: ':slug', loadComponent: () => import('./pages/cms-page/site-cms-page.component').then(m => m.SiteCmsPageComponent) },
      { path: '**', loadComponent: () => import('./pages/not-found/site-not-found.component').then(m => m.SiteNotFoundComponent) }
    ]
  }
];
