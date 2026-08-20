import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { map } from 'rxjs/operators';
import { SalaryStructure, SalaryStructureRequest, Employee } from '../../models/hrm.model';
import { SalaryStructureService } from '../../services/salary-structure.service';
import { EmployeeService } from '../../services/employee.service';
import {
  SalaryComponentsService, SalaryComponent, SalaryStructureTemplate, StructureExtra,
} from '../../services/salary-components.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-salary-structures',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './salary-structures.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './salary-structures.scss',
})
export class SalaryStructures implements OnInit {
  employees: Employee[] = [];
  selectedEmployeeId: number | null = null;
  structures: SalaryStructure[] = [];
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: SalaryStructureRequest = this.emptyForm();

  deleteTarget: SalaryStructure | null = null;

  // ── Server-side templates & component catalog ─────────────
  templates: SalaryStructureTemplate[] = [];
  selectedTemplateId: number | null = null;
  applyingTemplate = false;

  catalog: SalaryComponent[] = [];

  // Extra components on the structure being edited (loan EMI, internet, ...).
  extras: { componentId: number | null; amount: number | null }[] = [];

  // Template manager modal
  showTemplates = false;
  editingTemplateId: number | null = null;
  templateForm: SalaryStructureTemplate = this.emptyTemplate();
  savingTemplate = false;

  // Catalog manager modal
  showCatalog = false;
  newComponent: Partial<SalaryComponent> = { type: 'EARNING', calculationType: 'FIXED', taxable: true };
  savingComponent = false;

  // ── Auto-breakup presets (Bangladesh-typical component ratios of Gross) ──
  selectedPreset = '';
  breakupPresets = [
    { key: 'BASIC_50', label: 'Standard — Basic 50%', basic: 0.50, houseRent: 0.25, medical: 0.10, transport: 0.05, food: 0.05 },
    { key: 'BASIC_60', label: 'Basic 60%',            basic: 0.60, houseRent: 0.20, medical: 0.10, transport: 0.05, food: 0.05 },
  ];
  pfRate = 0.10; // Provident Fund = 10% of Basic (editable after auto-fill)
  taxRatePct: number | null = null; // Tax as a % of Gross → auto-fills Tax Deduction

  private round(n: number): number {
    return Math.round(n * 100) / 100;
  }

  // Placeholder hint showing each component's % of Gross for the active preset
  // (falls back to the Standard preset so the guidance is always meaningful).
  pctHint(field: 'basic' | 'houseRent' | 'medical' | 'transport' | 'food'): string {
    const p = this.breakupPresets.find((x) => x.key === this.selectedPreset) || this.breakupPresets[0];
    return Math.round(p[field] * 100) + '% of gross';
  }

  /**
   * Real-IT-company behaviour: Gross is the contract figure, and Special
   * Allowance is the balancing component that absorbs whatever of Gross the
   * fixed components don't cover. While auto-balance is on (the default),
   * editing Gross or any fixed component recomputes Special, so the
   * structure can never stop reconciling. Editing Special by hand opts out.
   */
  autoBalanceSpecial = true;

  syncGross(): void {
    if (this.autoBalanceSpecial && Number(this.form.grossSalary) > 0) {
      const allocated =
        (Number(this.form.basicSalary) || 0) + (Number(this.form.houseRent) || 0) +
        (Number(this.form.medicalAllowance) || 0) + (Number(this.form.transportAllowance) || 0) +
        (Number(this.form.foodAllowance) || 0);
      this.form.specialAllowance = this.round(Math.max(0, Number(this.form.grossSalary) - allocated)) as any;
    } else {
      // Opted out: Gross follows the components instead.
      this.form.grossSalary = this.round(this.totalEarnings) as any;
    }
    if (this.taxRatePct) {
      this.form.taxDeduction = this.round(Number(this.form.grossSalary) * Number(this.taxRatePct) / 100) as any;
    }
    this.cdr.markForCheck();
  }

  /** A hand-typed Special Allowance means the user wants manual control. */
  onSpecialEdited(): void {
    this.autoBalanceSpecial = false;
    this.form.grossSalary = this.round(this.totalEarnings) as any;
    this.cdr.markForCheck();
  }

  /** Gross changed: rebalance, and if a template drives the form, re-apply it. */
  onGrossChanged(): void {
    if (this.selectedTemplateId) { this.applyTemplate(); return; }
    if (this.selectedPreset) { this.applyBreakup(); return; }
    this.syncGross();
  }

  // Tax deduction from a % of Gross (editable amount afterward).
  applyTax(): void {
    const gross = Number(this.form.grossSalary) || 0;
    const rate = Number(this.taxRatePct) || 0;
    this.form.taxDeduction = this.round(gross * rate / 100) as any;
    this.cdr.markForCheck();
  }

  // Fill the component fields from Gross using the chosen preset. Special Allowance
  // takes the remainder so earnings always reconcile to Gross. Everything stays editable.
  applyBreakup(): void {
    const gross = Number(this.form.grossSalary) || 0;
    const p = this.breakupPresets.find((x) => x.key === this.selectedPreset);
    if (!gross || !p) return;

    this.form.basicSalary = this.round(gross * p.basic) as any;
    this.form.houseRent = this.round(gross * p.houseRent) as any;
    this.form.medicalAllowance = this.round(gross * p.medical) as any;
    this.form.transportAllowance = this.round(gross * p.transport) as any;
    this.form.foodAllowance = this.round(gross * p.food) as any;

    const allocated =
      Number(this.form.basicSalary) + Number(this.form.houseRent) +
      Number(this.form.medicalAllowance) + Number(this.form.transportAllowance) +
      Number(this.form.foodAllowance);
    this.form.specialAllowance = this.round(Math.max(0, gross - allocated)) as any;

    this.form.providentFund = this.round(Number(this.form.basicSalary) * this.pfRate) as any;
    if (this.taxRatePct) this.form.taxDeduction = this.round(gross * Number(this.taxRatePct) / 100) as any;
    this.cdr.markForCheck();
  }

  get totalEarnings(): number {
    const f = this.form;
    return (Number(f.basicSalary) || 0) + (Number(f.houseRent) || 0) +
      (Number(f.medicalAllowance) || 0) + (Number(f.transportAllowance) || 0) +
      (Number(f.foodAllowance) || 0) + (Number(f.specialAllowance) || 0);
  }

  get netSalary(): number {
    return (Number(this.form.grossSalary) || 0) - (Number(this.form.providentFund) || 0)
      - (Number(this.form.taxDeduction) || 0)
      + this.extrasTotal('EARNING') - this.extrasTotal('DEDUCTION');
  }

  /** Sum of staged extra components of one type (employer contributions excluded from net). */
  extrasTotal(type: 'EARNING' | 'DEDUCTION'): number {
    return this.extras.reduce((sum, row) => {
      const comp = this.catalog.find((c) => c.id === row.componentId);
      if (!comp || comp.type !== type) return sum;
      return sum + (Number(row.amount) || 0);
    }, 0);
  }

  componentLabel(c: SalaryComponent): string {
    const tag = c.type === 'EARNING' ? '+' : c.type === 'DEDUCTION' ? '−' : '(employer)';
    return `${c.name} ${tag}`;
  }

  // True when component earnings add up to Gross (within a rounding cent).
  get earningsReconcile(): boolean {
    const gross = Number(this.form.grossSalary) || 0;
    return gross > 0 && Math.abs(this.totalEarnings - gross) < 0.01;
  }

  constructor(
    private salaryService: SalaryStructureService,
    private employeeService: EmployeeService,
    private componentsService: SalaryComponentsService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.employeeService.list(0, 100).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
    this.loadTemplates();
    this.componentsService.catalog().subscribe({ next: (c) => { this.catalog = c; this.cdr.markForCheck(); } });
    this.load();
  }

  loadTemplates(): void {
    this.componentsService.templates().subscribe({ next: (t) => { this.templates = t; this.cdr.markForCheck(); } });
  }

  // ── Template apply ─────────────────────────────────────────
  // Server-computed split: basic % of gross, HRA % of basic, fixed amounts,
  // special takes the remainder. Internet/mobile have no structure column, so
  // they arrive as staged extra components instead.
  applyTemplate(): void {
    const gross = Number(this.form.grossSalary) || 0;
    if (!this.selectedTemplateId || !gross) return;
    this.applyingTemplate = true;
    this.componentsService.breakdown(this.selectedTemplateId, gross).subscribe({
      next: (b) => {
        this.form.basicSalary = b.basicSalary as any;
        this.form.houseRent = b.houseRent as any;
        this.form.medicalAllowance = b.medicalAllowance as any;
        this.form.transportAllowance = b.transportAllowance as any;
        this.form.foodAllowance = b.foodAllowance as any;
        this.form.specialAllowance = b.specialAllowance as any;
        // The template fills EVERYTHING: PF defaults to the standard rate of
        // basic, and tax refreshes if a rate is set. Both stay editable.
        this.form.providentFund = this.round(b.basicSalary * this.pfRate) as any;
        if (this.taxRatePct) {
          this.form.taxDeduction = this.round(gross * Number(this.taxRatePct) / 100) as any;
        }
        this.stageExtra('Internet Allowance', b.internetAllowance);
        this.stageExtra('Mobile Allowance', b.mobileAllowance);
        this.autoBalanceSpecial = true;
        this.applyingTemplate = false;
        this.cdr.markForCheck();
      },
      error: () => { this.applyingTemplate = false; this.cdr.markForCheck(); },
    });
  }

  /**
   * Selecting a template fills the whole form immediately. With no Gross
   * typed yet, the grade's default package supplies it - selecting
   * "Engineer Grade A" IS choosing the salary.
   */
  onTemplateSelected(): void {
    if (!this.selectedTemplateId) return;
    if (!(Number(this.form.grossSalary) > 0)) {
      const t = this.templates.find((x) => x.id === this.selectedTemplateId);
      if (t?.defaultGross && Number(t.defaultGross) > 0) {
        this.form.grossSalary = Number(t.defaultGross) as any;
      }
    }
    if (Number(this.form.grossSalary) > 0) this.applyTemplate();
  }

  /** One-off bonuses belong on the payroll month, not the monthly structure. */
  isOneOffComponent(componentId: number | null): boolean {
    const c = this.catalog.find((x) => x.id === componentId);
    return !!c && /festival bonus|performance bonus/i.test(c.name);
  }

  get hasOneOffExtras(): boolean {
    return this.extras.some((e) => this.isOneOffComponent(e.componentId));
  }

  private stageExtra(componentName: string, amount: number): void {
    if (!amount || amount <= 0) return;
    const comp = this.catalog.find((c) => c.name === componentName);
    if (!comp) return;
    const existing = this.extras.find((e) => e.componentId === comp.id);
    if (existing) existing.amount = amount;
    else this.extras.push({ componentId: comp.id, amount });
  }

  addExtraRow(): void {
    this.extras.push({ componentId: null, amount: null });
  }

  removeExtraRow(i: number): void {
    this.extras.splice(i, 1);
    this.cdr.markForCheck();
  }

  // ── Template manager ───────────────────────────────────────
  openTemplateEditor(t?: SalaryStructureTemplate): void {
    this.editingTemplateId = t?.id ?? null;
    this.templateForm = t ? { ...t } : this.emptyTemplate();
    this.showTemplates = true;
  }

  saveTemplate(): void {
    this.savingTemplate = true;
    const op = this.editingTemplateId
      ? this.componentsService.updateTemplate(this.editingTemplateId, this.templateForm)
      : this.componentsService.createTemplate(this.templateForm);
    op.subscribe({
      next: () => {
        this.savingTemplate = false;
        this.editingTemplateId = null;
        this.templateForm = this.emptyTemplate();
        this.loadTemplates();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.savingTemplate = false;
        this.error = err?.error?.message || 'Failed to save template';
        this.cdr.markForCheck();
      },
    });
  }

  deleteTemplate(t: SalaryStructureTemplate): void {
    if (!t.id) return;
    this.componentsService.deleteTemplate(t.id).subscribe({ next: () => this.loadTemplates() });
  }

  private emptyTemplate(): SalaryStructureTemplate {
    return {
      structureName: '', defaultGross: null, basicPercentage: 50, hraPercentage: 40,
      medicalAmount: 0, transportAmount: 0, internetAmount: 0, mobileAmount: 0, mealAmount: 0,
    };
  }

  // ── Catalog manager ────────────────────────────────────────
  addComponent(): void {
    if (!this.newComponent.name?.trim()) return;
    this.savingComponent = true;
    this.componentsService.createComponent(this.newComponent).subscribe({
      next: (c) => {
        this.catalog = [...this.catalog, c];
        this.newComponent = { type: 'EARNING', calculationType: 'FIXED', taxable: true };
        this.savingComponent = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.savingComponent = false;
        this.error = err?.error?.message || 'Failed to add component';
        this.cdr.markForCheck();
      },
    });
  }

  toggleComponent(c: SalaryComponent): void {
    this.componentsService.updateComponent(c.id, { ...c, active: !c.active }).subscribe({
      next: (updated) => {
        this.catalog = this.catalog.map((x) => (x.id === updated.id ? updated : x));
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * No employee selected shows every structure in the company; picking one
   * narrows to that person's full history. The two come from different
   * endpoints - the all-view is paged, the per-employee history is not.
   */
  load(): void {
    this.loading = true;
    this.error = '';

    const request = this.selectedEmployeeId
      ? this.salaryService.history(this.selectedEmployeeId)
      : this.salaryService.listAll(0, 200).pipe(map((res) => res.content));

    request.subscribe({
      next: (res) => {
        this.structures = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load salary structures';
        this.structures = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** True when the table is showing everyone — drives the Employee column. */
  get showingAll(): boolean {
    return !this.selectedEmployeeId;
  }

  clearEmployeeFilter(): void {
    this.selectedEmployeeId = null;
    this.load();
  }

  openCreate(): void {
    if (!this.selectedEmployeeId) return;
    this.editingId = null;
    this.form = this.emptyForm();
    this.form.employeeId = this.selectedEmployeeId;
    this.selectedPreset = '';
    this.selectedTemplateId = null;
    this.taxRatePct = null;
    this.extras = [];
    this.showForm = true;
  }

  // Only the current (non-superseded) structure is editable.
  openEdit(s: SalaryStructure): void {
    this.editingId = s.id;
    this.selectedPreset = '';
    this.selectedTemplateId = null;
    this.taxRatePct = null;
    this.extras = [];
    this.componentsService.extras(s.id).subscribe({
      next: (rows: StructureExtra[]) => {
        this.extras = rows.map((r) => ({ componentId: r.componentId, amount: r.amount }));
        this.cdr.markForCheck();
      },
    });
    this.form = {
      employeeId: s.employeeId,
      effectiveFrom: s.effectiveFrom,
      grossSalary: s.grossSalary,
      basicSalary: s.basicSalary,
      houseRent: s.houseRent,
      medicalAllowance: s.medicalAllowance,
      transportAllowance: s.transportAllowance,
      foodAllowance: s.foodAllowance,
      specialAllowance: s.specialAllowance,
      providentFund: s.providentFund,
      taxDeduction: s.taxDeduction,
      notes: s.notes,
    } as SalaryStructureRequest;
    this.error = '';
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload: any = { ...this.form };
    Object.keys(payload).forEach((k) => {
      if (payload[k] === '' || payload[k] === null || payload[k] === undefined) delete payload[k];
    });
    const op = this.editingId
      ? this.salaryService.update(this.editingId, payload)
      : this.salaryService.create(payload);
    op.subscribe({
      next: (saved: SalaryStructure) => {
        // Persist the staged extra components against the saved structure.
        const structureId = this.editingId ?? saved?.id;
        const lines = this.extras
          .filter((e) => e.componentId && Number(e.amount) > 0)
          .map((e) => ({ componentId: e.componentId as number, amount: Number(e.amount) }));
        if (structureId) {
          this.componentsService.setExtras(structureId, lines).subscribe({ error: () => {} });
        }
        this.saving = false;
        this.showForm = false;
        this.success = this.editingId ? 'Salary structure updated' : 'Salary structure created';
        this.editingId = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save salary structure';
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.salaryService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Salary structure deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  isCurrent(s: SalaryStructure): boolean {
    return !s.effectiveTo;
  }

  private emptyForm(): SalaryStructureRequest {
    return {
      employeeId: undefined as any,
      effectiveFrom: '',
      grossSalary: undefined as any,
      basicSalary: undefined as any,
    };
  }
}
