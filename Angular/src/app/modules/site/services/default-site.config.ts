import { SiteSettings, Service, BlogPost, Testimonial, Faq, TeamMember, Project, PricingPlan, Stat, CmsPage } from '../models/site.model';

// Used only when the API is unreachable (dev / no backend configured).
// In production every value comes from the REST API.
export const DEFAULT_SITE: SiteSettings = {
  companyName: 'Your Company',
  logoUrl: '',
  faviconUrl: '',
  tagline: 'Professional services, delivered with care.',
  primaryColor: '#6D28D9',
  secondaryColor: '#2563EB',
  heroHeading: 'Welcome to Your Company',
  heroSubheading: 'We help businesses grow with trusted, professional services tailored to your goals.',
  heroImageUrl: '',
  heroImages: [],
  aboutText: 'We are a team of professionals dedicated to helping businesses succeed through innovation, strategy and execution.',
  mission: 'To empower businesses with world-class solutions that drive sustainable growth.',
  vision: 'To be the most trusted partner for enterprises seeking operational excellence.',
  email: 'info@yourcompany.com',
  phone: '+1 555 0100',
  address: '123 Business Ave, Suite 100',
  mapEmbedUrl: '',
  whatsapp: '',
  socialLinks: [],
  copyright: '2026 Your Company. All rights reserved.',
  theme: {
    primary: '#6D28D9',
    secondary: '#2563EB',
    gradient: 'linear-gradient(135deg, #6D28D9 0%, #2563EB 100%)',
    font: "'Inter', 'Plus Jakarta Sans', sans-serif",
    radius: 14,
    buttonStyle: 'rounded',
    darkMode: false,
    navbarStyle: 'transparent',
    footerStyle: 'dark',
    animations: true,
    spacing: 1,
  },
  seo: { title: 'Your Company', description: 'Professional business services.', keywords: '', ogImage: '' },
};

export const DEFAULT_SERVICES: Service[] = [
  { id: 1, slug: 'consulting', title: 'Business Consulting', summary: 'Strategic advice to optimise operations and grow revenue.', description: 'Full description here.', icon: 'bi-lightbulb', imageUrl: '', startingPrice: '$199/hr', estimatedTime: '1-4 weeks', features: ['Strategy', 'Analysis', 'Roadmap'] },
  { id: 2, slug: 'accounting', title: 'Accounting & Tax', summary: 'Accurate bookkeeping, tax filing and financial reporting.', description: '', icon: 'bi-calculator', imageUrl: '', startingPrice: '$299/mo', features: ['Bookkeeping', 'Tax Returns'] },
  { id: 3, slug: 'legal', title: 'Legal Services', summary: 'Contracts, compliance and corporate law advisory.', description: '', icon: 'bi-briefcase', imageUrl: '', features: ['Contracts', 'Compliance'] },
  { id: 4, slug: 'hr', title: 'HR & Payroll', summary: 'End-to-end people operations and payroll management.', description: '', icon: 'bi-people', imageUrl: '', features: ['Payroll', 'Onboarding'] },
];

export const DEFAULT_BLOGS: BlogPost[] = [
  { id: 1, slug: 'how-to-scale', title: 'How to Scale Your Business in 2026', excerpt: 'A practical guide to scaling operations.', body: '', coverImageUrl: '', author: 'Jane Doe', publishedAt: '2026-06-15T10:00:00', category: 'Growth', readMinutes: 5 },
  { id: 2, slug: 'tax-planning', title: 'Essential Tax Planning Strategies', excerpt: 'Minimise your tax burden legally.', body: '', coverImageUrl: '', author: 'John Smith', publishedAt: '2026-05-20T10:00:00', category: 'Finance', readMinutes: 4 },
];

export const DEFAULT_TESTIMONIALS: Testimonial[] = [
  { id: 1, name: 'Sarah M.', role: 'CEO', company: 'Northwind Group', quote: 'Outstanding service. Our operations improved dramatically.', avatarUrl: '', rating: 5 },
  { id: 2, name: 'David O.', role: 'CFO', company: 'Brightlogistics', quote: 'Professional, reliable and truly partner-level work.', avatarUrl: '', rating: 5 },
];

export const DEFAULT_FAQS: Faq[] = [
  { id: 1, question: 'How do I get started?', answer: 'Contact us or use our online booking form. We will set up an initial consultation.', category: 'General' },
  { id: 2, question: 'Do you offer remote services?', answer: 'Yes, all our services can be delivered remotely via secure video and collaboration tools.', category: 'General' },
];

export const DEFAULT_TEAM: TeamMember[] = [
  { id: 1, name: 'Jane Doe', role: 'CEO & Founder', bio: '20 years in business strategy.', photoUrl: '', email: 'jane@company.com' },
  { id: 2, name: 'John Smith', role: 'Head of Finance', bio: 'Chartered accountant and tax advisor.', photoUrl: '', email: 'john@company.com' },
];

export const DEFAULT_PROJECTS: Project[] = [
  { id: 1, title: 'Enterprise Migration', summary: 'Cloud migration for a 500-person company.', description: '', coverImageUrl: '', client: 'Acme Inc.', category: 'Technology', year: 2025, tags: ['cloud', 'migration'] },
];

export const DEFAULT_PRICING: PricingPlan[] = [
  { id: 1, name: 'Starter', description: 'For small teams.', price: '$99', period: '/mo', cta: 'Get Started', featured: false, features: ['5 users', 'Core modules', 'Email support'] },
  { id: 2, name: 'Professional', description: 'For growing companies.', price: '$299', period: '/mo', cta: 'Get Started', featured: true, features: ['25 users', 'All modules', 'Priority support', 'AI assistant'] },
  { id: 3, name: 'Enterprise', description: 'For large organisations.', price: 'Custom', period: '', cta: 'Contact Sales', featured: false, features: ['Unlimited users', 'Dedicated instance', 'SSO', 'SLA'] },
];

export const DEFAULT_STATS: Stat[] = [
  { id: 1, value: '500+', label: 'Clients Served', icon: 'bi-people' },
  { id: 2, value: '10+', label: 'Years Experience', icon: 'bi-calendar-check' },
  { id: 3, value: '99%', label: 'Client Satisfaction', icon: 'bi-star' },
  { id: 4, value: '24/7', label: 'Support Available', icon: 'bi-headset' },
];

export const DEFAULT_PAGES: Record<string, CmsPage> = {
  'about': { slug: 'about', title: 'About Us', body: '<p>We are a trusted professional services firm helping businesses succeed since 2015.</p>', updatedAt: '2026-01-01T00:00:00' },
  'privacy': { slug: 'privacy', title: 'Privacy Policy', body: '<p>Your privacy matters to us. We collect only what is necessary to deliver our services.</p>', updatedAt: '2026-01-01T00:00:00' },
  'terms': { slug: 'terms', title: 'Terms of Service', body: '<p>By using our services you agree to these terms.</p>', updatedAt: '2026-01-01T00:00:00' },
  'careers': { slug: 'careers', title: 'Careers', body: '<p>We are always looking for talented people. Check our open positions below.</p>', updatedAt: '2026-01-01T00:00:00' },
};
