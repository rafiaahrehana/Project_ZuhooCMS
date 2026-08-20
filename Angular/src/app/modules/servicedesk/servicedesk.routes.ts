import { Routes } from '@angular/router';
import { Requests } from './components/requests/requests';
import { RequestDetail } from './components/request-detail/request-detail';
import { Approvals } from './components/approvals/approvals';
import { KnowledgeBase } from './components/knowledge-base/knowledge-base';
import { Categories } from './components/categories/categories';
import { Services } from './components/services/services';
import { Packages } from './components/packages/packages';
import { Workflows } from './components/workflows/workflows';
import { Templates } from './components/templates/templates';
import { Reviews } from './components/reviews/reviews';
import { ServiceFormFields } from './components/service-form-fields/service-form-fields';
import { ServiceRequiredDocuments } from './components/service-required-documents/service-required-documents';
import { ServicePrerequisites } from './components/service-prerequisites/service-prerequisites';

// NOTE: 'quotations' route/files were deleted outright (see below) - the standalone
// QuotationController was removed server-side, there is no independent list/CRUD
// endpoint anymore. Quotation data now lives as embedded fields on ServiceRequestResponse,
// accessed via /service-requests/{id}/quotation(/accept|/reject). Quotation submit/
// accept/reject is now available directly on the Request Detail page via
// ServiceRequestService. The old Quotations page/service/model
// (components/quotations, quotation.service.ts, Quotation/QuotationRequest interfaces)
// called dead endpoints and have been deleted.
export const SERVICEDESK_ROUTES: Routes = [
  { path: 'requests', component: Requests, data: { requiredPermission: 'SERVICE_REQUEST_VIEW' } },
  { path: 'requests/:id', component: RequestDetail, data: { requiredPermission: 'SERVICE_REQUEST_VIEW' } },
  { path: 'approvals', component: Approvals, data: { requiredPermission: 'SERVICE_REQUEST_APPROVE' } },
  { path: 'kb', component: KnowledgeBase, data: { requiredPermission: 'KNOWLEDGE_BASE_VIEW' } },
  { path: 'categories', component: Categories, data: { requiredPermission: 'SERVICE_CATEGORY_VIEW' } },
  { path: 'services', component: Services, data: { requiredPermission: 'SERVICE_CATALOG_VIEW' } },
  { path: 'services/:id/form-fields', component: ServiceFormFields, data: { requiredPermission: 'SERVICE_CATALOG_VIEW' } },
  { path: 'services/:id/required-documents', component: ServiceRequiredDocuments, data: { requiredPermission: 'SERVICE_CATALOG_VIEW' } },
  { path: 'services/:id/prerequisites', component: ServicePrerequisites, data: { requiredPermission: 'SERVICE_CATALOG_VIEW' } },
  { path: 'packages', component: Packages, data: { requiredPermission: 'SERVICE_PACKAGE_VIEW' } },
  { path: 'workflows', component: Workflows, data: { requiredPermission: 'WORKFLOW_VIEW' } },
  { path: 'templates', component: Templates, data: { requiredPermission: 'SERVICE_TEMPLATE_VIEW' } },
  { path: 'reviews', component: Reviews, data: { requiredPermission: 'REVIEW_VIEW' } },
  { path: '', redirectTo: 'requests', pathMatch: 'full' }
];
