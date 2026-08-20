import { Component, ChangeDetectionStrategy, signal, computed, effect, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LocationRequest, globalHierarchyLabels, HierarchyLabel, GeoNodeDto } from '../../models/location.model';
import { LocationValidators } from '../../utils/location.validators';
import { LocationService } from '../../services/location.service';

@Component({
  selector: 'app-location',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './location.component.html',
  // Using Default change detection as 'Eager' was requested (Default is eager in Angular).
  changeDetection: ChangeDetectionStrategy.Default
})
export class LocationComponent implements OnInit {

  @Output() formSubmit = new EventEmitter<LocationRequest>();

  // Off by default so every other usage of this component (registration, employee
  // create/edit forms) keeps its current always-editable behavior. Callers that want
  // the view-then-edit pattern (e.g. the account settings page) opt in explicitly.
  @Input() readOnlyUntilEdit = false;
  @Input() showActions = true;
  editing = signal(true);

  locationForm!: FormGroup;
  
  countries = signal<GeoNodeDto[]>([]);
  level1Nodes = signal<GeoNodeDto[]>([]);
  level2Nodes = signal<GeoNodeDto[]>([]);
  level3Nodes = signal<GeoNodeDto[]>([]);
  level4Nodes = signal<GeoNodeDto[]>([]);

  // The selected country code to dynamically calculate labels
  selectedCountryCode = signal<string | null>(null);

  isLoading = signal<boolean>(false);
  hasError = signal<boolean>(false);
  errorMessage = signal<string>('');

  // Computed signals for dynamic labels
  labels = computed<HierarchyLabel>(() => {
    const code = this.selectedCountryCode();
    if (code && globalHierarchyLabels[code]) {
      return globalHierarchyLabels[code];
    }
    // Fallback default labels
    return {
      level1: 'State / Province',
      level2: 'District / County',
      level3: 'City / Municipality',
      level4: 'Police Station / Precinct'
    };
  });

  constructor(
    private fb: FormBuilder,
    private locationService: LocationService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.editing.set(!this.readOnlyUntilEdit);

    // Listen for Country changes
    this.locationForm.get('country')?.valueChanges.subscribe(val => {
      this.selectedCountryCode.set(val);
      // Find the country node to get its ID
      const countryNode = this.countries().find(c => c.code === val || c.name === val);
      
      this.level1Nodes.set([]);
      this.level2Nodes.set([]);
      this.level3Nodes.set([]);
      this.level4Nodes.set([]);
      this.locationForm.patchValue({ level1: null, level2: null, level3: null, level4: null }, { emitEvent: false });
      
      if (countryNode && countryNode.id) {
        this.fetchDivisions(countryNode.id);
      }
    });

    // Listen for Level 1 changes
    this.locationForm.get('level1')?.valueChanges.subscribe(val => {
      const node = this.level1Nodes().find(n => n.name === val);
      this.level2Nodes.set([]);
      this.level3Nodes.set([]);
      this.level4Nodes.set([]);
      this.locationForm.patchValue({ level2: null, level3: null, level4: null }, { emitEvent: false });
      if (node && node.id) this.fetchChildren(node.id, 2);
    });

    // Listen for Level 2 changes
    this.locationForm.get('level2')?.valueChanges.subscribe(val => {
      const node = this.level2Nodes().find(n => n.name === val);
      this.level3Nodes.set([]);
      this.level4Nodes.set([]);
      this.locationForm.patchValue({ level3: null, level4: null }, { emitEvent: false });
      if (node && node.id) this.fetchChildren(node.id, 3);
    });

    // Listen for Level 3 changes
    this.locationForm.get('level3')?.valueChanges.subscribe(val => {
      const node = this.level3Nodes().find(n => n.name === val);
      this.level4Nodes.set([]);
      this.locationForm.patchValue({ level4: null }, { emitEvent: false });
      if (node && node.id) this.fetchChildren(node.id, 4);
    });

    this.loadCountries();
  }

  private initForm(): void {
    this.locationForm = this.fb.group({
      country: [null, [Validators.required, LocationValidators.noWhitespaceValidator()]],
      level1: [null, [Validators.required, LocationValidators.noWhitespaceValidator()]],
      level2: [null, [Validators.required, LocationValidators.noWhitespaceValidator()]],
      level3: [null, [Validators.required, LocationValidators.noWhitespaceValidator()]],
      // Not required: Bangladesh's seeded hierarchy has no police-station/precinct
      // level, so this dropdown is legitimately empty for every BD address.
      level4: [null, [LocationValidators.noWhitespaceValidator()]],
      streetAddress: [null, [Validators.required, LocationValidators.noWhitespaceValidator()]],
      postalCode: [null], // Optional
      apartment: [null]
    });
  }

  loadCountries(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.locationService.getCountriesMaster().subscribe({
      next: (data) => {
        this.countries.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.hasError.set(true);
        this.errorMessage.set('Failed to load countries from master data');
        this.isLoading.set(false);
      }
    });
  }

  fetchDivisions(countryId: number): void {
    this.locationService.getDivisionsForCountry(countryId).subscribe({
      next: (data) => this.level1Nodes.set(data),
      error: (err) => console.error('Failed to load divisions', err)
    });
  }

  fetchChildren(parentId: number, level: number): void {
    this.locationService.getChildrenMaster(parentId).subscribe({
      next: (data) => {
        if (level === 2) this.level2Nodes.set(data);
        else if (level === 3) this.level3Nodes.set(data);
        else if (level === 4) this.level4Nodes.set(data);
      },
      error: (err) => console.error(`Failed to load level ${level} children`, err)
    });
  }

  // Pattern: patchValue()
  patchValue(data: Partial<LocationRequest>): void {
    this.locationForm.patchValue(data);
    if (data.country) {
      this.selectedCountryCode.set(data.country);
    }
  }

  // Resolves and loads all necessary levels for an existing location
  resolveExistingLocation(data: Partial<LocationRequest>): void {
    this.locationForm.patchValue(data, { emitEvent: false });
    if (data.country) {
      this.selectedCountryCode.set(data.country);
      
      this.locationService.getCountriesMaster().subscribe({
        next: (countries) => {
          this.countries.set(countries);
          const countryNode = countries.find(c => c.code === data.country || c.name === data.country);
          if (countryNode && countryNode.id) {
            // Load Level 1
            this.locationService.getDivisionsForCountry(countryNode.id).subscribe({
              next: (l1Nodes) => {
                this.level1Nodes.set(l1Nodes);
                const l1Node = l1Nodes.find(n => n.name === data.level1);
                if (l1Node && l1Node.id) {
                  // Load Level 2
                  this.locationService.getChildrenMaster(l1Node.id).subscribe({
                    next: (l2Nodes) => {
                      this.level2Nodes.set(l2Nodes);
                      const l2Node = l2Nodes.find(n => n.name === data.level2);
                      if (l2Node && l2Node.id) {
                        // Load Level 3
                        this.locationService.getChildrenMaster(l2Node.id).subscribe({
                          next: (l3Nodes) => {
                            this.level3Nodes.set(l3Nodes);
                            const l3Node = l3Nodes.find(n => n.name === data.level3);
                            if (l3Node && l3Node.id) {
                              // Load Level 4
                              this.locationService.getChildrenMaster(l3Node.id).subscribe({
                                next: (l4Nodes) => {
                                  this.level4Nodes.set(l4Nodes);
                                }
                              });
                            }
                          }
                        });
                      }
                    }
                  });
                }
              }
            });
          }
        }
      });
    }
  }

  // Pattern: reset()
  reset(): void {
    this.locationForm.reset();
    this.selectedCountryCode.set(null);
  }

  enterEditMode(): void {
    this.editing.set(true);
  }

  // Called by the parent after a successful save, or by Cancel/Reset in
  // read-only-until-edit mode - drops back to the disabled, view-only state.
  enterViewMode(): void {
    this.editing.set(false);
  }

  // Pattern: save()
  save(): void {
    if (this.locationForm.invalid) {
      this.locationForm.markAllAsTouched();
      return;
    }

    const formData: LocationRequest = this.locationForm.getRawValue();
    // Trim string values before saving
    Object.keys(formData).forEach(key => {
      const val = (formData as any)[key];
      if (typeof val === 'string') {
        (formData as any)[key] = val.trim();
      }
    });

    this.formSubmit.emit(formData);
  }

  // Utility to check form errors in template
  hasFieldError(fieldName: string): boolean {
    const control = this.locationForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
