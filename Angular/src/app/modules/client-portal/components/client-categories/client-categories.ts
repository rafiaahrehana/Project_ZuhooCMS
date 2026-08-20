import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ServiceCategory, CompanyService } from '../../../servicedesk/models/servicedesk.model';
import { ServiceCategoryService } from '../../../servicedesk/services/service-category.service';
import { CompanyServiceService } from '../../../servicedesk/services/company-service.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

interface Accent {
  bgFrom: string;
  bgTo: string;
  icon: string;
}

const ACCENTS: Record<string, Accent> = {
  web: { bgFrom: '#F5F0FF', bgTo: '#EDE3FF', icon: '#8B5CF6' },
  it: { bgFrom: '#EAF2FF', bgTo: '#DCEAFF', icon: '#3B82F6' },
  legal: { bgFrom: '#E9FBF3', bgTo: '#D7F7E7', icon: '#10B981' },
  marketing: { bgFrom: '#FFF3E6', bgTo: '#FFE7CC', icon: '#F59E0B' },
  hr: { bgFrom: '#EEF0FF', bgTo: '#E0E4FF', icon: '#6366F1' },
  finance: { bgFrom: '#E6FBF8', bgTo: '#CFF7F1', icon: '#14B8A6' },
};

const DEFAULT_ACCENT: Accent = { bgFrom: '#F8F5FF', bgTo: '#EEE6FF', icon: '#8B5CF6' };

interface CategoryCard extends ServiceCategory {
  serviceCount: number;
  previewNames: string[];
  accent: Accent;
}

@Component({
  selector: 'app-client-categories',
  imports: [CommonModule, FormsModule, Loader, EmptyState],
  templateUrl: './client-categories.html',
  styleUrl: './client-categories.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientCategories implements OnInit {
  categories: CategoryCard[] = [];
  loading = true;
  error = '';
  searchTerm = '';

  get filteredCategories(): CategoryCard[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.categories;
    return this.categories.filter((c) =>
      c.name.toLowerCase().includes(term) ||
      (c.description ?? '').toLowerCase().includes(term) ||
      c.previewNames.some((n) => n.toLowerCase().includes(term))
    );
  }

  constructor(
    private categoryService: ServiceCategoryService,
    private companyServiceService: CompanyServiceService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.categoryService.list().subscribe({
      next: (categories) => {
        this.companyServiceService.listActive().subscribe({
          next: (services) => {
            this.categories = categories.map((c) => this.toCard(c, services));
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            // Counts are a nice-to-have - still show the categories if the service list fails.
            this.categories = categories.map((c) => this.toCard(c, []));
            this.loading = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.error = 'Failed to load service categories.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private toCard(category: ServiceCategory, services: CompanyService[]): CategoryCard {
    const inCategory = services.filter((s) => s.categoryId === category.id);
    return {
      ...category,
      serviceCount: inCategory.length,
      previewNames: inCategory.slice(0, 4).map((s) => s.name),
      accent: this.accentFor(category.name),
    };
  }

  private accentFor(name: string): Accent {
    const n = name.toLowerCase();
    if (n.includes('web') || n.includes('software') || n.includes('app') || n.includes('commerce')) return ACCENTS['web'];
    if (n.includes('it') || n.includes('tech') || n.includes('cloud') || n.includes('hosting')) return ACCENTS['it'];
    if (n.includes('legal') || n.includes('complian') || n.includes('incorporat')) return ACCENTS['legal'];
    if (n.includes('market') || n.includes('digital') || n.includes('seo') || n.includes('ads')) return ACCENTS['marketing'];
    if (n.includes('hr') || n.includes('human resource') || n.includes('recruit')) return ACCENTS['hr'];
    if (n.includes('finance') || n.includes('accounting') || n.includes('tax')) return ACCENTS['finance'];
    return DEFAULT_ACCENT;
  }

  viewServices(category: ServiceCategory): void {
    this.router.navigate(['/client/categories', category.id, 'services']);
  }
}
