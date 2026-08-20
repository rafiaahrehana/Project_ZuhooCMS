import { Routes } from '@angular/router';
import { Employees } from './components/employees/employees';
import { EmployeeDetail } from './components/employee-detail/employee-detail';
import { Departments } from './components/departments/departments';
import { Designations } from './components/designations/designations';
import { PayrollPage } from './components/payroll/payroll';
import { SalaryStructures } from './components/salary-structures/salary-structures';
import { Announcements } from './components/announcements/announcements';
import { Holidays } from './components/holidays/holidays';
import { LeavePolicies } from './components/leave-policies/leave-policies';
import { Shifts } from './components/shifts/shifts';
import { PerformanceReviews } from './components/performance-reviews/performance-reviews';
import { JobPostings } from './components/job-postings/job-postings';
import { OfferLetters } from './components/offer-letters/offer-letters';
import { Leaves } from './components/leaves/leaves';
import { LeaveBalances } from './components/leave-balances/leave-balances';
import { Applications } from './components/applications/applications';
import { Expenses } from './components/expenses/expenses';
import { Candidates } from './components/candidates/candidates';

export const HRM_ROUTES: Routes = [
  // HR overview. EMPLOYEE_VIEW is the marker for "may see the workforce", so an
  // ordinary employee (who has their own dashboard) never reaches this one.
  // Lazily loaded like the rest of the app's routes.
  { path: 'dashboard',
    loadComponent: () => import('./components/hr-dashboard/hr-dashboard').then(m => m.HrDashboard),
    data: { requiredPermission: 'EMPLOYEE_VIEW' } },
  { path: 'employees', component: Employees, data: { requiredPermission: 'EMPLOYEE_VIEW' } },
  { path: 'employees/:id', component: EmployeeDetail, data: { requiredPermission: 'EMPLOYEE_VIEW' } },
  { path: 'departments', component: Departments, data: { requiredPermission: 'DEPARTMENT_VIEW' } },
  { path: 'designations', component: Designations, data: { requiredPermission: 'DESIGNATION_VIEW' } },
  { path: 'payroll', component: PayrollPage, data: { requiredPermission: 'PAYROLL_VIEW' } },
  {
    path: 'salary-sheet',
    loadComponent: () => import('./components/salary-sheet/salary-sheet').then(m => m.SalarySheet),
    data: { requiredPermission: 'PAYROLL_VIEW' },
  },
  { path: 'leaves', component: Leaves, data: { requiredPermission: 'LEAVE_VIEW' } },
  { path: 'leave-balances', component: LeaveBalances, data: { requiredPermission: 'LEAVE_BALANCE_VIEW' } },
  { path: 'expenses', component: Expenses, data: { requiredPermission: 'EXPENSE_VIEW' } },
  { path: 'salary-structures', component: SalaryStructures, data: { requiredPermission: 'SALARY_STRUCTURE_VIEW' } },
  { path: 'announcements', component: Announcements, data: { requiredPermission: 'ANNOUNCEMENT_VIEW' } },
  { path: 'holidays', component: Holidays, data: { requiredPermission: 'HOLIDAY_VIEW' } },
  { path: 'leave-policies', component: LeavePolicies, data: { requiredPermission: 'LEAVE_POLICY_VIEW' } },
  { path: 'shifts', component: Shifts, data: { requiredPermission: 'SHIFT_VIEW' } },
  { path: 'performance', component: PerformanceReviews, data: { requiredPermission: 'PERFORMANCE_VIEW' } },
  { path: 'job-postings', component: JobPostings, data: { requiredPermission: 'JOB_POSTING_VIEW' } },
  { path: 'letters', component: OfferLetters, data: { requiredPermission: 'LETTER_VIEW' } },
  { path: 'applications', component: Applications, data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'candidates', component: Candidates, data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'pipeline', loadComponent: () => import('./components/pipeline/pipeline').then(m => m.Pipeline), data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'career-page', loadComponent: () => import('./components/career-page/career-page').then(m => m.CareerPage), data: { requiredPermission: 'JOB_POSTING_VIEW' } },
  { path: 'interviews', loadComponent: () => import('./components/interviews/interviews').then(m => m.Interviews), data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'offers', loadComponent: () => import('./components/offers/offers').then(m => m.Offers), data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'talent-pool', loadComponent: () => import('./components/talent-pool/talent-pool').then(m => m.TalentPool), data: { requiredPermission: 'APPLICATION_VIEW' } },
  { path: 'recruitment-reports', loadComponent: () => import('./components/recruitment-reports/recruitment-reports').then(m => m.RecruitmentReports), data: { requiredPermission: 'RECRUITMENT_REPORT_VIEW' } },
  { path: 'payroll-dashboard', loadComponent: () => import('./components/payroll-dashboard/payroll-dashboard').then(m => m.PayrollDashboard), data: { requiredPermission: 'PAYROLL_VIEW' } },
  { path: 'employee-salaries', loadComponent: () => import('./components/employee-salaries/employee-salaries').then(m => m.EmployeeSalaries), data: { requiredPermission: 'SALARY_STRUCTURE_VIEW' } },
  { path: 'loans-advances', loadComponent: () => import('./components/loans-advances/loans-advances').then(m => m.LoansAdvances), data: { requiredPermission: 'PAYROLL_VIEW' } },
  { path: '', redirectTo: 'employees', pathMatch: 'full' }
];
