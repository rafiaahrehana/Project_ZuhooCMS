import { Routes } from '@angular/router';
import { Companies } from './components/companies/companies';
import { PlatformUsers } from './components/platform-users/platform-users';
import { CustomRoles } from './components/custom-roles/custom-roles';
import { FeatureFlags } from './components/feature-flags/feature-flags';
import { Locations } from './components/locations/locations';
import { PlatformExpenses } from './components/platform-expenses/platform-expenses';
import { SubscriptionManagement } from './components/subscription-management/subscription-management';

// NOTE: 'platform-employees' route removed - no PlatformEmployeeController exists
// anywhere in the backend. See services/platform-employee.service.ts for details.
export const PLATFORM_ADMIN_ROUTES: Routes = [
  { path: 'companies', component: Companies },
  { path: 'platform-users', component: PlatformUsers },
  { path: 'custom-roles', component: CustomRoles },
  { path: 'feature-flags', component: FeatureFlags },
  { path: 'locations', component: Locations },
  { path: 'platform-expenses', component: PlatformExpenses },
  { path: 'subscription-management', component: SubscriptionManagement },
  { path: '', redirectTo: 'companies', pathMatch: 'full' }
];
