import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LocationService } from '../../../../shared/services/location.service';
import { GeoNodeDto } from '../../../../shared/models/location.model';
import { Loader } from '../../../../shared/components/loader/loader';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-locations-management',
  standalone: true,
  imports: [CommonModule, FormsModule, Loader, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './locations.html',
  styleUrl: './locations.scss'
})
export class Locations implements OnInit {
  countries: GeoNodeDto[] = [];
  level1Nodes: GeoNodeDto[] = [];
  level2Nodes: GeoNodeDto[] = [];
  level3Nodes: GeoNodeDto[] = [];
  level4Nodes: GeoNodeDto[] = [];

  selectedCountry: GeoNodeDto | null = null;
  selectedL1: GeoNodeDto | null = null;
  selectedL2: GeoNodeDto | null = null;
  selectedL3: GeoNodeDto | null = null;
  selectedL4: GeoNodeDto | null = null;

  loading = false;
  saving = false;
  error = '';
  success = '';

  showFormModal = false;
  isEditMode = false;
  
  formNode: {
    id?: number;
    name: string;
    type: string;
    code: string;
    parentId?: number;
  } = { name: '', type: 'COUNTRY', code: '' };

  showDeleteDialog = false;
  deleteTarget: GeoNodeDto | null = null;

  constructor(private locationService: LocationService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadCountries();
  }

  loadCountries(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.locationService.getCountriesMaster().subscribe({
      next: (data) => {
        this.countries = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Failed to load countries';
        this.loading = false;
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }

  selectCountry(c: GeoNodeDto): void {
    this.selectedCountry = c;
    this.selectedL1 = null;
    this.selectedL2 = null;
    this.selectedL3 = null;
    this.selectedL4 = null;
    this.level1Nodes = [];
    this.level2Nodes = [];
    this.level3Nodes = [];
    this.level4Nodes = [];
    this.loadDivisions(c.id);
  }

  selectL1(node: GeoNodeDto): void {
    this.selectedL1 = node;
    this.selectedL2 = null;
    this.selectedL3 = null;
    this.selectedL4 = null;
    this.level2Nodes = [];
    this.level3Nodes = [];
    this.level4Nodes = [];
    this.loadChildren(node.id, 2);
  }

  selectL2(node: GeoNodeDto): void {
    this.selectedL2 = node;
    this.selectedL3 = null;
    this.selectedL4 = null;
    this.level3Nodes = [];
    this.level4Nodes = [];
    this.loadChildren(node.id, 3);
  }

  selectL3(node: GeoNodeDto): void {
    this.selectedL3 = node;
    this.selectedL4 = null;
    this.level4Nodes = [];
    this.loadChildren(node.id, 4);
  }

  selectL4(node: GeoNodeDto): void {
    this.selectedL4 = node;
  }

  loadDivisions(countryId: number): void {
    this.error = '';
    this.cdr.markForCheck();
    this.locationService.getDivisionsForCountry(countryId).subscribe({
      next: (data) => {
        this.level1Nodes = data;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Failed to load divisions';
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }

  loadChildren(parentId: number, targetLevel: number): void {
    this.error = '';
    this.cdr.markForCheck();
    this.locationService.getChildrenMaster(parentId).subscribe({
      next: (data) => {
        if (targetLevel === 2) this.level2Nodes = data;
        else if (targetLevel === 3) this.level3Nodes = data;
        else if (targetLevel === 4) this.level4Nodes = data;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = `Failed to load children for level ${targetLevel}`;
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }

  openAddModal(type: string, parentId?: number): void {
    this.isEditMode = false;
    this.formNode = {
      name: '',
      type: type,
      code: '',
      parentId: parentId
    };
    this.showFormModal = true;
    this.error = '';
    this.success = '';
  }

  openEditModal(node: GeoNodeDto): void {
    this.isEditMode = true;
    this.formNode = {
      id: node.id,
      name: node.name,
      type: node.type,
      code: node.code || ''
    };
    this.showFormModal = true;
    this.error = '';
    this.success = '';
  }

  closeFormModal(): void {
    this.showFormModal = false;
  }

  saveNode(): void {
    if (!this.formNode.name.trim()) {
      this.error = 'Name is required';
      return;
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();

    if (this.isEditMode && this.formNode.id) {
      const req = {
        name: this.formNode.name.trim(),
        code: this.formNode.code ? this.formNode.code.trim().toUpperCase() : null
      };
      this.locationService.updateNode(this.formNode.id, req).subscribe({
        next: (res) => {
          this.success = 'Node updated successfully';
          this.saving = false;
          this.showFormModal = false;
          this.cdr.markForCheck();
          this.refreshColumnAfterWrite(res.type, res);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to update node';
          this.saving = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      const req = {
        name: this.formNode.name.trim(),
        type: this.formNode.type,
        code: this.formNode.code ? this.formNode.code.trim().toUpperCase() : null,
        parentId: this.formNode.parentId || null
      };
      this.locationService.createNode(req).subscribe({
        next: (res) => {
          this.success = 'Node created successfully';
          this.saving = false;
          this.showFormModal = false;
          this.cdr.markForCheck();
          this.refreshColumnAfterWrite(res.type, res);
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to create node';
          this.saving = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  openDeleteDialog(node: GeoNodeDto): void {
    this.deleteTarget = node;
    this.showDeleteDialog = true;
  }

  closeDeleteDialog(): void {
    this.showDeleteDialog = false;
    this.deleteTarget = null;
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    const target = this.deleteTarget;
    this.locationService.deleteNode(target.id).subscribe({
      next: () => {
        this.success = 'Node deleted successfully';
        this.showDeleteDialog = false;
        this.deleteTarget = null;
        this.cdr.markForCheck();
        this.refreshColumnAfterDelete(target);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete node';
        this.showDeleteDialog = false;
        this.deleteTarget = null;
        this.cdr.markForCheck();
      }
    });
  }

  private refreshColumnAfterWrite(type: string, node: GeoNodeDto): void {
    if (type === 'COUNTRY') {
      this.loadCountries();
      if (this.selectedCountry && this.selectedCountry.id === node.id) {
        this.selectedCountry = node;
      }
    } else if (type === 'LEVEL1') {
      if (this.selectedCountry) this.loadDivisions(this.selectedCountry.id);
      if (this.selectedL1 && this.selectedL1.id === node.id) this.selectedL1 = node;
    } else if (type === 'LEVEL2') {
      if (this.selectedL1) this.loadChildren(this.selectedL1.id, 2);
      if (this.selectedL2 && this.selectedL2.id === node.id) this.selectedL2 = node;
    } else if (type === 'LEVEL3') {
      if (this.selectedL2) this.loadChildren(this.selectedL2.id, 3);
      if (this.selectedL3 && this.selectedL3.id === node.id) this.selectedL3 = node;
    } else if (type === 'LEVEL4') {
      if (this.selectedL3) this.loadChildren(this.selectedL3.id, 4);
      if (this.selectedL4 && this.selectedL4.id === node.id) this.selectedL4 = node;
    }
  }

  private refreshColumnAfterDelete(node: GeoNodeDto): void {
    const type = node.type;
    if (type === 'COUNTRY') {
      this.loadCountries();
      if (this.selectedCountry && this.selectedCountry.id === node.id) {
        this.selectedCountry = null;
        this.level1Nodes = [];
        this.selectedL1 = null;
        this.level2Nodes = [];
        this.selectedL2 = null;
        this.level3Nodes = [];
        this.selectedL3 = null;
        this.level4Nodes = [];
        this.selectedL4 = null;
      }
    } else if (type === 'LEVEL1') {
      if (this.selectedCountry) this.loadDivisions(this.selectedCountry.id);
      if (this.selectedL1 && this.selectedL1.id === node.id) {
        this.selectedL1 = null;
        this.level2Nodes = [];
        this.selectedL2 = null;
        this.level3Nodes = [];
        this.selectedL3 = null;
        this.level4Nodes = [];
        this.selectedL4 = null;
      }
    } else if (type === 'LEVEL2') {
      if (this.selectedL1) this.loadChildren(this.selectedL1.id, 2);
      if (this.selectedL2 && this.selectedL2.id === node.id) {
        this.selectedL2 = null;
        this.level3Nodes = [];
        this.selectedL3 = null;
        this.level4Nodes = [];
        this.selectedL4 = null;
      }
    } else if (type === 'LEVEL3') {
      if (this.selectedL2) this.loadChildren(this.selectedL2.id, 3);
      if (this.selectedL3 && this.selectedL3.id === node.id) {
        this.selectedL3 = null;
        this.level4Nodes = [];
        this.selectedL4 = null;
      }
    } else if (type === 'LEVEL4') {
      if (this.selectedL3) this.loadChildren(this.selectedL3.id, 4);
      if (this.selectedL4 && this.selectedL4.id === node.id) {
        this.selectedL4 = null;
      }
    }
  }
}
