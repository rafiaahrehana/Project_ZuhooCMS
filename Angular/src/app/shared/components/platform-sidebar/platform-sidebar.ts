import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface NavGroup { label: string; icon: string; items: NavItem[]; roles?: string[]; }
interface NavItem { label: string; link: string; icon: string; roles?: string[]; }

@Component({
  selector: 'app-platform-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './platform-sidebar.html',
  styleUrl: './platform-sidebar.scss',
})
export class PlatformSidebar {
  constructor(private auth: AuthService) {}

  get visibleGroups(): NavGroup[] {
    return this.groups
      .filter(g => !g.roles || this.auth.hasAnyRole(g.roles))
      .map(g => ({
        ...g,
        items: g.items.filter(item => !item.roles || this.auth.hasAnyRole(item.roles))
      }))
      .filter(g => g.items.length > 0);
  }

  groups: NavGroup[] = [
    {
      label: 'Companies',
      icon: 'bi-buildings',
      roles: ['SUPER_ADMIN', 'SYSTEM_ADMIN', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER'],
      items: [
        { label: 'Companies', link: '/platform/companies', icon: 'bi-buildings' },
        { label: 'Subscriptions', link: '/platform/subscription-management', icon: 'bi-credit-card' },
      ]
    },
    {
      label: 'Support Desk Triage',
      icon: 'bi-life-preserver',
      roles: ['SUPER_ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER'],
      items: [
        { label: 'Support Tickets Desk', link: '/support/tickets', icon: 'bi-chat-left-dots' },
        { label: 'Direct Messages', link: '/support/messages', icon: 'bi-envelope' },
        { label: 'SLA Policies', link: '/support/sla-policies', icon: 'bi-stopwatch' },
        { label: 'Support Team Roster', link: '/support/agents', icon: 'bi-person-badge', roles: ['SUPER_ADMIN', 'SUPPORT_MANAGER'] },
      ]
    },
    {
      label: 'Platform Administration',
      icon: 'bi-shield-lock',
      roles: ['SUPER_ADMIN', 'SYSTEM_ADMIN', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER'],
      items: [
        { label: 'Platform Users', link: '/platform/platform-users', icon: 'bi-person-vcard' },
        { label: 'Feature Flags', link: '/platform/feature-flags', icon: 'bi-flag' },
        { label: 'AI Settings', link: '/ai/settings', icon: 'bi-robot' },
      ]
    },
    {
      label: 'Finance',
      icon: 'bi-cash-coin',
      roles: ['SUPER_ADMIN', 'PLATFORM_ACCOUNTANT'],
      items: [
        { label: 'Revenue', link: '/platform/revenue', icon: 'bi-graph-up-arrow' },
        { label: 'Subscriptions', link: '/platform/subscriptions', icon: 'bi-arrow-repeat' },
        { label: 'Platform Expenses', link: '/platform/platform-expenses', icon: 'bi-cash-stack' },
      ]
    },
    {
      label: 'AI',
      icon: 'bi-stars',
      roles: ['SUPER_ADMIN', 'SYSTEM_ADMIN'],
      items: [
        // Platform-wide default provider/model - used as a fallback by companies that
        // haven't configured their own AI provider. See AiProviderResolver.resolve().
        { label: 'AI Settings', link: '/ai/settings', icon: 'bi-gear' },
      ]
    },
  ];
}
