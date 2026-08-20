import { ThemeSettings } from '../models/site.model';

// Five professional, pre-built themes. Company owners can switch instantly.
// Branding colors from SiteSettings override primary/secondary at runtime.

export const THEMES: Record<string, ThemeSettings> = {
  'purple-enterprise': {
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
  'corporate-blue': {
    primary: '#1D4ED8',
    secondary: '#0EA5E9',
    gradient: 'linear-gradient(135deg, #1D4ED8 0%, #0EA5E9 100%)',
    font: "'Manrope', 'Inter', sans-serif",
    radius: 10,
    buttonStyle: 'rounded',
    darkMode: false,
    navbarStyle: 'solid',
    footerStyle: 'dark',
    animations: true,
    spacing: 1,
  },
  'modern-green': {
    primary: '#047857',
    secondary: '#14B8A6',
    gradient: 'linear-gradient(135deg, #047857 0%, #14B8A6 100%)',
    font: "'Plus Jakarta Sans', 'Inter', sans-serif",
    radius: 16,
    buttonStyle: 'pill',
    darkMode: false,
    navbarStyle: 'solid',
    footerStyle: 'light',
    animations: true,
    spacing: 1,
  },
  'dark-executive': {
    primary: '#7C3AED',
    secondary: '#38BDF8',
    gradient: 'linear-gradient(135deg, #7C3AED 0%, #38BDF8 100%)',
    font: "'Inter', sans-serif",
    radius: 12,
    buttonStyle: 'rounded',
    darkMode: true,
    navbarStyle: 'glass',
    footerStyle: 'dark',
    animations: true,
    spacing: 1,
  },
  'minimal-white': {
    primary: '#2563EB',
    secondary: '#0F172A',
    gradient: 'linear-gradient(135deg, #2563EB 0%, #0F172A 100%)',
    font: "'Inter', sans-serif",
    radius: 8,
    buttonStyle: 'square',
    darkMode: false,
    navbarStyle: 'solid',
    footerStyle: 'light',
    animations: false,
    spacing: 1,
  },
};

export const DEFAULT_THEME_KEY = 'purple-enterprise';
