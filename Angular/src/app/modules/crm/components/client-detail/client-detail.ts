import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Client, ClientContact, CrmActivity, Opportunity } from '../../models/crm.model';
import { ClientService } from '../../services/client.service';
import { ContactService } from '../../services/contact.service';
import { ActivityService } from '../../services/activity.service';
import { OpportunityService } from '../../services/opportunity.service';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-client-detail',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, ConfirmDialog],
  templateUrl: './client-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './client-detail.scss',
})
export class ClientDetail implements OnInit {
  clientId!: number;
  client?: Client;
  contacts: ClientContact[] = [];
  activities: CrmActivity[] = [];
  opportunities: Opportunity[] = [];
  error = '';
  success = '';

  // Portal invite
  showInviteConfirm = false;
  inviting = false;

  showContactForm = false;
  editingContact: ClientContact | null = null;
  newContact: Partial<ClientContact> = {};
  deleteContactTarget: ClientContact | null = null;

  newActivity: Partial<CrmActivity> = { type: 'NOTE' };

  aiSummary = '';
  summarising = false;
  summaryError = '';

  constructor(
    private route: ActivatedRoute,
    private clientService: ClientService,
    private contactService: ContactService,
    private activityService: ActivityService,
    private opportunityService: OpportunityService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.clientId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  loadAll(): void {
    this.clientService.getById(this.clientId).subscribe({
      next: (c) => {
        this.client = c;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load client';
        this.cdr.markForCheck();
      },
    });
    this.loadContacts();
    this.loadTimeline();
    this.opportunityService.list(0, 50, { clientId: this.clientId }).subscribe({
      next: (res) => {
        this.opportunities = res.content;
        this.cdr.markForCheck();
      },
    });
  }

  loadContacts(): void {
    this.contactService.listByClient(this.clientId).subscribe({
      next: (list) => {
        this.contacts = list;
        this.cdr.markForCheck();
      },
    });
  }

  loadTimeline(): void {
    this.activityService.timeline({ clientId: this.clientId }, 0, 30).subscribe({
      next: (res) => {
        this.activities = res.content;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Fresh add. The template used to toggle showContactForm directly, which
   * left editingContact from a cancelled edit in place - the next "Add"
   * would silently PATCH that previous contact instead of creating one.
   */
  openAddContact(): void {
    this.editingContact = null;
    this.newContact = {};
    this.showContactForm = true;
    this.cdr.markForCheck();
  }

  closeContactForm(): void {
    this.showContactForm = false;
    this.editingContact = null;
    this.newContact = {};
    this.cdr.markForCheck();
  }

  editContact(contact: ClientContact): void {
    this.editingContact = contact;
    this.newContact = { ...contact };
    this.showContactForm = true;
    this.cdr.markForCheck();
  }

  saveContact(): void {
    if (!this.newContact.fullName?.trim()) return;
    const obs = this.editingContact
      ? this.contactService.update(this.clientId, this.editingContact.id, this.newContact)
      : this.contactService.create(this.clientId, this.newContact);
    obs.subscribe({
      next: () => {
        this.newContact = {};
        this.editingContact = null;
        this.showContactForm = false;
        this.loadContacts();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save contact';
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Emails the client a one-time set-password link, creating their portal login
   * if they don't have one. Staff never choose or see the password.
   */
  confirmInvite(): void {
    this.showInviteConfirm = false;
    this.inviting = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.clientService.inviteToPortal(this.clientId).subscribe({
      next: (updated) => {
        // Reflect the new portal-access state without a full page reload, so the
        // button flips to "Resend" straight away.
        this.client = updated;
        this.inviting = false;

        // The login is created even when the email fails, so these are two
        // genuinely different outcomes and must not both read as success.
        if (updated.inviteEmailSent === false) {
          this.error = (updated.inviteEmailError || 'The invite email could not be delivered.')
            + ' The login is ready — check the mail server settings, then click Resend.';
        } else {
          this.success = 'Portal invite sent. The client sets their own password from the emailed link.';
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Usually "no contact email" - the backend says so precisely, so keep it.
        this.error = err?.error?.message || 'Failed to send the portal invite';
        this.inviting = false;
        this.cdr.markForCheck();
      },
    });
  }

  confirmDeleteContact(): void {
    if (!this.deleteContactTarget) return;
    this.contactService.delete(this.clientId, this.deleteContactTarget.id).subscribe({
      next: () => {
        this.deleteContactTarget = null;
        this.loadContacts();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete contact';
        this.deleteContactTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  completeActivity(activity: CrmActivity): void {
    this.activityService.markCompleted(activity.id).subscribe({
      next: () => {
        this.loadTimeline();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to complete activity';
        this.cdr.markForCheck();
      },
    });
  }

  deleteActivity(activity: CrmActivity): void {
    this.activityService.delete(activity.id).subscribe({
      next: () => {
        this.loadTimeline();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete activity';
        this.cdr.markForCheck();
      },
    });
  }

  makePrimary(contact: ClientContact): void {
    this.contactService.markPrimary(this.clientId, contact.id).subscribe({
      next: () => {
        this.loadContacts();
        this.cdr.markForCheck();
      },
    });
  }

  logActivity(): void {
    if (!this.newActivity.subject?.trim()) return;
    this.activityService.log({ ...this.newActivity, clientId: this.clientId }).subscribe({
      next: () => {
        this.newActivity = { type: 'NOTE' };
        this.loadTimeline();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to log activity';
        this.cdr.markForCheck();
      },
    });
  }

  summariseActivity(): void {
    if (this.summarising) return;
    this.summarising = true;
    this.summaryError = '';
    this.cdr.markForCheck();
    this.activityService.summarise({ clientId: this.clientId }).subscribe({
      next: (res) => {
        this.aiSummary = res.summary;
        this.summarising = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.summaryError = err?.error?.message || 'Failed to generate summary';
        this.summarising = false;
        this.cdr.markForCheck();
      },
    });
  }

  get openOpportunitiesCount(): number {
    return this.opportunities.filter((o) => o.stage !== 'WON' && o.stage !== 'LOST').length;
  }

  get completedOpportunitiesCount(): number {
    return this.opportunities.filter((o) => o.stage === 'WON').length;
  }

  get lastActivityDate(): string | undefined {
    return this.activities[0]?.activityDate;
  }

  activityIcon(type: string): string {
    const icons: Record<string, string> = {
      CALL: 'bi-telephone',
      MEETING: 'bi-calendar-event',
      EMAIL: 'bi-envelope',
      NOTE: 'bi-journal-text',
      TASK: 'bi-check2-square',
      FOLLOW_UP: 'bi-bell',
      STAGE_CHANGE: 'bi-graph-up-arrow',
      STATUS_CHANGE: 'bi-arrow-repeat',
      DOCUMENT: 'bi-file-earmark',
    };
    return icons[type] || 'bi-dot';
  }
}
