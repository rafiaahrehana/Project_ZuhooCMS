// Domain models for the dynamic, multi-tenant company website.
// All data is fetched from the website REST API; nothing is hardcoded.

export interface ThemeSettings {
  primary: string;
  secondary: string;
  gradient: string;
  font: string;  
  radius: number;
  buttonStyle: 'rounded' | 'pill' | 'square';
  darkMode: boolean;
  navbarStyle: 'solid' | 'transparent' | 'glass';
  footerStyle: 'dark' | 'light';
  animations: boolean;
  spacing: number;
}

export interface SiteSettings {
  companyName: string;
  logoUrl: string;
  faviconUrl: string;
  tagline: string;
  primaryColor: string;
  secondaryColor: string;
  heroHeading: string;
  heroSubheading: string;
  heroImageUrl: string;
  heroImages: string[];
  aboutText: string;
  mission: string;
  vision: string;
  email: string;
  phone: string;
  address: string;
  mapEmbedUrl: string;
  whatsapp: string;
  socialLinks: SocialLink[];
  copyright: string;
  theme: ThemeSettings;
  seo: SeoSettings;
}

export interface SocialLink { platform: string; url: string; icon: string; }

export interface SeoSettings {
  title: string;
  description: string;
  keywords: string;
  ogImage: string;
}

export interface NavItem {
  id: number;
  label: string;
  url: string;
  external?: boolean;
  children?: NavItem[];
  mega?: boolean;
}

export interface ServiceCategory { id: number; name: string; slug: string; }

export interface Service {
  id: number;
  slug: string;
  title: string;
  summary: string;
  description: string;
  icon: string;
  imageUrl: string;
  categoryId?: number;
  categoryName?: string;
  startingPrice?: string;
  estimatedTime?: string;
  requirements?: string;
  features?: string[];
}

export interface BlogPost {
  id: number;
  slug: string;
  title: string;
  excerpt: string;
  body: string;
  coverImageUrl: string;
  author: string;
  publishedAt: string;
  category: string;
  readMinutes: number;
}

export interface Testimonial {
  id: number;
  name: string;
  role: string;
  company: string;
  quote: string;
  avatarUrl: string;
  rating: number;
}

export interface Faq { id: number; question: string; answer: string; category?: string; }

export interface TeamMember {
  id: number;
  name: string;
  role: string;
  bio: string;
  photoUrl: string;
  email: string;
  socialLinks?: SocialLink[];
}

export interface Project {
  id: number;
  title: string;
  summary: string;
  description: string;
  coverImageUrl: string;
  client: string;
  category: string;
  year: number;
  tags?: string[];
}

export interface PricingPlan {
  id: number;
  name: string;
  description: string;
  price: string;
  period: string;
  features: string[];
  cta: string;
  featured: boolean;
}

export interface Stat { id?: number; value: string; label: string; icon: string; }

export interface CmsPage {
  slug: string;
  title: string;
  body: string;
  updatedAt: string;
}

export interface ServiceRequestPayload {
  serviceId?: number;
  name: string;
  email: string;
  phone: string;
  message: string;
  documents?: string[];
}

export interface TrackedRequest {
  code: string;
  service: string;
  status: string;
  submittedAt: string;
  steps: { label: string; done: boolean; }[];
}
