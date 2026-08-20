import { Directive, ElementRef, Input, OnChanges, Renderer2, SimpleChanges, inject } from '@angular/core';
import { ThemeSettings } from '../models/site.model';

// Applies a ThemeSettings object as CSS custom properties on the host element.
// Branding colors (primary/secondary) are passed separately so company owners
// can override a theme's defaults without changing the theme itself.
@Directive({
  selector: '[appTheme]',
  standalone: true,
})
export class ThemeDirective implements OnChanges {
  @Input('appTheme') theme?: ThemeSettings;
  @Input() primaryOverride?: string;
  @Input() secondaryOverride?: string;

  private el = inject(ElementRef<HTMLElement>);
  private renderer = inject(Renderer2);

  ngOnChanges(_: SimpleChanges): void {
    const t = this.theme;
    if (!t) return;
    const primary = this.primaryOverride || t.primary;
    const secondary = this.secondaryOverride || t.secondary;
    const set = (k: string, v: string) => this.renderer.setStyle(this.el.nativeElement, k, v);

    set('--site-primary', primary);
    set('--site-primary-rgb', hexToRgb(primary));
    set('--site-secondary', secondary);
    set('--site-secondary-rgb', hexToRgb(secondary));
    set('--site-gradient', t.gradient);
    set('--site-font', t.font);
    set('--site-radius', t.radius + 'px');
    set('--site-spacing', (t.spacing || 1) + '');
    set('--site-btn-radius', t.buttonStyle === 'pill' ? '999px' : t.buttonStyle === 'square' ? '4px' : (t.radius * 0.7) + 'px');
    this.renderer.setStyle(this.el.nativeElement, 'font-family', t.font);
    this.renderer.addClass(this.el.nativeElement, t.darkMode ? 'theme-dark' : 'theme-light');
    this.renderer.addClass(this.el.nativeElement, t.animations ? 'theme-animate' : 'theme-no-animate');
  }
}

function hexToRgb(hex: string): string {
  const h = hex.replace('#', '');
  const n = h.length === 3 ? h.split('').map((c) => c + c).join('') : h;
  const r = parseInt(n.slice(0, 2), 16);
  const g = parseInt(n.slice(2, 4), 16);
  const b = parseInt(n.slice(4, 6), 16);
  return `${r}, ${g}, ${b}`;
}
