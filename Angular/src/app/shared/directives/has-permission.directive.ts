import { Directive, Input, OnDestroy, OnInit, TemplateRef, ViewContainerRef, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { PermissionCode, PermissionService } from '../../core/services/permission.service';

@Directive({
  selector: '[appHasPermission]',
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  private templateRef = inject(TemplateRef<unknown>);
  private viewContainer = inject(ViewContainerRef);
  private permissionService = inject(PermissionService);
  private sub?: Subscription;
  private codes: PermissionCode[] = [];
  private hasView = false;

  @Input()
  set appHasPermission(value: PermissionCode | PermissionCode[] | null | undefined) {
    this.codes = value == null ? [] : Array.isArray(value) ? value : [value];
    this.updateView();
  }

  ngOnInit(): void {
    this.sub = this.permissionService.permissions$.subscribe(() => this.updateView());
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private updateView(): void {
    const allowed = this.permissionService.hasAnyPermission(this.codes);
    if (allowed && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!allowed && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
