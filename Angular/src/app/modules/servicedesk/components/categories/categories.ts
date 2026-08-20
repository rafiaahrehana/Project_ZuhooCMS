import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ServiceCategory, ServiceCategoryRequest } from '../../models/servicedesk.model';
import { ServiceCategoryService } from '../../services/service-category.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-service-categories',
  imports: [CommonModule, FormsModule, Loader, EmptyState, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './categories.html',
  styleUrls: ['./categories.scss']
})
export class Categories implements OnInit {
  // VARIABLES
  categories: ServiceCategory[] = [];
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: ServiceCategoryRequest = { name: '' };
  searchTerm = '';

  constructor(private categoryService: ServiceCategoryService, private cdr: ChangeDetectorRef) {}

  get filteredCategories(): ServiceCategory[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.categories;
    return this.categories.filter((c) =>
      c.name.toLowerCase().includes(term) || (c.description ?? '').toLowerCase().includes(term)
    );
  }

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // LOAD CATEGORIES (management listing - includes inactive, bare list, not paged)
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.categoryService.listAll().subscribe({
      next: (res) => { this.categories = res; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load categories'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // OPEN CREATE / EDIT FORM
  openCreate(): void {
    this.editingId = null;
    this.form = { name: '' };
    this.showForm = true;
  }

  openEdit(c: ServiceCategory): void {
    this.editingId = c.id;
    this.form = {
      name: c.name,
      nameBn: c.nameBn,
      description: c.description,
      iconUrl: c.iconUrl,
      sortOrder: c.sortOrder,
    };
    this.showForm = true;
  }

  // SAVE CATEGORY
  save(): void {
    const op = this.editingId
      ? this.categoryService.update(this.editingId, this.form)
      : this.categoryService.create(this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Category updated' : 'Category created';
        this.showForm = false; this.editingId = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save category'; this.cdr.markForCheck(); }
    });
  }

  // TOGGLE ACTIVE
  toggle(c: ServiceCategory): void {
    this.categoryService.toggle(c.id).subscribe({
      next: () => this.load(),
      error: (err) => { this.error = err?.error?.message || 'Failed to toggle category'; this.cdr.markForCheck(); }
    });
  }

  // COLOR PALETTES FOR SERVICE CATEGORIES
  private colorPalettes = [
    { border: 'rgba(99, 102, 241, 0.2)', hover: 'rgba(99, 102, 241, 0.8)', bg: 'rgba(99, 102, 241, 0.08)', text: '#6366f1' }, // Indigo
    { border: 'rgba(59, 130, 246, 0.2)', hover: 'rgba(59, 130, 246, 0.8)', bg: 'rgba(59, 130, 246, 0.08)', text: '#3b82f6' }, // Blue
    { border: 'rgba(16, 185, 129, 0.2)', hover: 'rgba(16, 185, 129, 0.8)', bg: 'rgba(16, 185, 129, 0.08)', text: '#10b981' }, // Green
    { border: 'rgba(139, 92, 246, 0.2)', hover: 'rgba(139, 92, 246, 0.8)', bg: 'rgba(139, 92, 246, 0.08)', text: '#8b5cf6' }, // Purple
    { border: 'rgba(244, 63, 94, 0.2)', hover: 'rgba(244, 63, 94, 0.8)', bg: 'rgba(244, 63, 94, 0.08)', text: '#f43f5e' }, // Rose
    { border: 'rgba(245, 158, 11, 0.2)', hover: 'rgba(245, 158, 11, 0.8)', bg: 'rgba(245, 158, 11, 0.08)', text: '#f59e0b' }, // Amber
    { border: 'rgba(6, 182, 212, 0.2)', hover: 'rgba(6, 182, 212, 0.8)', bg: 'rgba(6, 182, 212, 0.08)', text: '#06b6d4' }, // Cyan
  ];

  getPalette(c: ServiceCategory, index: number) {
    const cat = (c.name || '').toLowerCase().trim();
    if (cat.includes('web') || cat.includes('software') || cat.includes('commerce') || cat.includes('app')) {
      return { border: 'rgba(139, 92, 246, 0.25)', hover: 'rgba(139, 92, 246, 0.8)', bg: 'rgba(139, 92, 246, 0.08)', text: '#8b5cf6' }; // Purple
    }
    if (cat.includes('it') || cat.includes('tech') || cat.includes('support') || cat.includes('stationery') || cat.includes('supplies')) {
      return { border: 'rgba(59, 130, 246, 0.25)', hover: 'rgba(59, 130, 246, 0.8)', bg: 'rgba(59, 130, 246, 0.08)', text: '#3b82f6' }; // Blue
    }
    if (cat.includes('consult') || cat.includes('architect')) {
      return { border: 'rgba(16, 185, 129, 0.25)', hover: 'rgba(16, 185, 129, 0.8)', bg: 'rgba(16, 185, 129, 0.08)', text: '#10b981' }; // Green
    }
    if (cat.includes('legal') || cat.includes('complian') || cat.includes('incorporat')) {
      return { border: 'rgba(217, 119, 6, 0.25)', hover: 'rgba(217, 119, 6, 0.8)', bg: 'rgba(217, 119, 6, 0.08)', text: '#c2410c' }; // Orange/Brown
    }
    if (cat.includes('office') || cat.includes('space') || cat.includes('room')) {
      return { border: 'rgba(245, 158, 11, 0.25)', hover: 'rgba(245, 158, 11, 0.8)', bg: 'rgba(245, 158, 11, 0.08)', text: '#d97706' }; // Amber/Orange
    }
    if (cat.includes('design') || cat.includes('graphic')) {
      return { border: 'rgba(6, 182, 212, 0.25)', hover: 'rgba(6, 182, 212, 0.8)', bg: 'rgba(6, 182, 212, 0.08)', text: '#06b6d4' }; // Cyan/Teal
    }
    if (cat.includes('market') || cat.includes('digital') || cat.includes('seo')) {
      return { border: 'rgba(236, 72, 153, 0.25)', hover: 'rgba(236, 72, 153, 0.8)', bg: 'rgba(236, 72, 153, 0.08)', text: '#ec4899' }; // Pink
    }
    return this.colorPalettes[index % this.colorPalettes.length];
  }

  getCategoryBorderColor(c: ServiceCategory, index: number): string {
    return this.getPalette(c, index).border;
  }

  getCategoryHoverBorderColor(c: ServiceCategory, index: number): string {
    return this.getPalette(c, index).hover;
  }

  getIconBg(c: ServiceCategory, index: number): string {
    return this.getPalette(c, index).bg;
  }

  getIconColor(c: ServiceCategory, index: number): string {
    return this.getPalette(c, index).text;
  }

  getCategoryIcon(c: ServiceCategory): string {
    if (c.iconUrl) return c.iconUrl;
    const n = (c.name || '').toLowerCase();
    if (n.includes('android') || n.includes('app')) return 'bi-code-slash';
    if (n.includes('email') || n.includes('mail') || n.includes('g-suite') || n.includes('it & technology') || n.includes('it ')) return 'bi-envelope';
    if (n.includes('cloud') || n.includes('architecture') || n.includes('server') || n.includes('hosting')) return 'bi-cloud';
    if (n.includes('incorporation') || n.includes('rjsc') || n.includes('company') || n.includes('legal') || n.includes('compliance')) return 'bi-bank';
    if (n.includes('room') || n.includes('space') || n.includes('conference') || n.includes('office')) return 'bi-door-open';
    if (n.includes('website') || n.includes('web') || n.includes('portal')) return 'bi-globe';
    if (n.includes('e-commerce') || n.includes('shop') || n.includes('cart') || n.includes('sales')) return 'bi-cart';
    if (n.includes('design') || n.includes('graphic') || n.includes('art') || n.includes('logo')) return 'bi-brush';
    if (n.includes('marketing') || n.includes('digital') || n.includes('seo') || n.includes('ads')) return 'bi-megaphone';
    if (n.includes('support') || n.includes('maintenance') || n.includes('help')) return 'bi-headset';
    if (n.includes('stationery') || n.includes('supplies') || n.includes('paper') || n.includes('pen')) return 'bi-journal-text';
    return 'bi-tag';
  }
}
