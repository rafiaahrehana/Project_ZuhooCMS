import { Routes } from '@angular/router';
import { CrmDashboard } from './components/crm-dashboard/crm-dashboard';
import { Leads } from './components/leads/leads';
import { PipelineBoard } from './components/pipeline-board/pipeline-board';
import { PipelineReports } from './components/pipeline-reports/pipeline-reports';
import { Clients } from './components/clients/clients';
import { ClientDetail } from './components/client-detail/client-detail';
import { Contacts } from './components/contacts/contacts';
import { TagManager } from './components/tag-manager/tag-manager';

export const CRM_ROUTES: Routes = [
  { path: 'dashboard', component: CrmDashboard, data: { requiredPermission: 'OPPORTUNITY_VIEW' } },
  { path: 'leads', component: Leads, data: { requiredPermission: 'LEAD_VIEW' } },
  { path: 'pipeline', component: PipelineBoard, data: { requiredPermission: 'OPPORTUNITY_VIEW' } },
  { path: 'pipeline/reports', component: PipelineReports, data: { requiredPermission: 'OPPORTUNITY_VIEW' } },
  { path: 'clients', component: Clients, data: { requiredPermission: 'CLIENT_VIEW' } },
  { path: 'clients/:id', component: ClientDetail, data: { requiredPermission: 'CLIENT_VIEW' } },
  { path: 'contacts', component: Contacts, data: { requiredPermission: 'CONTACT_VIEW' } },
  { path: 'tags', component: TagManager, data: { requiredPermission: 'TAG_VIEW' } },
  // Opportunities is still the default landing page - that's where reps spend their day.
  { path: '', redirectTo: 'pipeline', pathMatch: 'full' }
];
