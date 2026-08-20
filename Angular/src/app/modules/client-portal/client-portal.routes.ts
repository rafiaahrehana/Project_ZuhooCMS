import { Routes } from '@angular/router';
import { ClientDashboard } from './components/client-dashboard/client-dashboard';
import { ClientProfile } from './components/client-profile/client-profile';
import { ClientPackages } from './components/client-packages/client-packages';
import { ClientPayments } from './components/client-payments/client-payments';
import { ClientCategories } from './components/client-categories/client-categories';
import { ClientServices } from './components/client-services/client-services';
import { ClientTickets } from './components/client-tickets/client-tickets';
import { ClientTicketDetail } from './components/client-ticket-detail/client-ticket-detail';
import { Requests } from '../servicedesk/components/requests/requests';
import { RequestDetail } from '../servicedesk/components/request-detail/request-detail';

// My Requests / Initialize Project reuse the existing servicedesk Requests &
// RequestDetail components - they already fully support CLIENT-role behavior
// (see the isClient branches in those components), so there's no need to
// duplicate that flow under the client-portal module.
export const CLIENT_PORTAL_ROUTES: Routes = [
  { path: 'dashboard', component: ClientDashboard },
  { path: 'profile', component: ClientProfile },
  { path: 'requests', component: Requests },
  { path: 'requests/:id', component: RequestDetail },
  { path: 'categories', component: ClientCategories },
  { path: 'categories/:id/services', component: ClientServices },
  { path: 'packages', component: ClientPackages },
  { path: 'payments', component: ClientPayments },
  { path: 'tickets', component: ClientTickets },
  { path: 'tickets/:id', component: ClientTicketDetail },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
