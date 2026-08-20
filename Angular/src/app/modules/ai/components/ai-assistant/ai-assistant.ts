import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService, AiGenerateResponse, AiThread } from '../../../../core/services/ai.service';
import { AnnouncementService } from '../../../hrm/services/announcement.service';
import { AnnouncementDraftResponse, HolidayDraftResponse } from '../../../hrm/models/hrm.model';
import { LeavePolicyService } from '../../../hrm/services/leave-policy.service';
import { HolidayService } from '../../../hrm/services/holiday.service';
import { extractErrorMessage } from '../../../../core/utils/http-error.util';
import { Loader } from '../../../../shared/components/loader/loader';
import { SpeechInputService } from '../../../../shared/services/speech-input.service';

/** One rendered bubble in the active thread - built from AiGenerateResponse
 * rows (which each carry one full user+assistant exchange) split into two. */
interface ChatBubble {
  role: 'user' | 'assistant';
  text: string;
  awaitingConfirmation?: boolean;
  meta?: string;
}

@Component({
  selector: 'app-ai-assistant',
  imports: [CommonModule, FormsModule, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ai-assistant.html',
})
export class AiAssistant implements OnInit {
  // Top-level: the redesigned chat/agent experience is the default landing
  // view; the three structured single-field drafts (which save straight
  // into a real Announcement/Holiday/Leave-Policy record) keep their own
  // non-chat form - forcing those into chat bubbles would just be a worse
  // UI for a one-shot structured output.
  view: 'chat' | 'drafts' = 'chat';

  // ── Chat / agent state ────────────────────────────────────
  threads: AiThread[] = [];
  loadingThreads = false;
  activeThreadId: number | null = null;
  bubbles: ChatBubble[] = [];
  loadingMessages = false;
  composerText = '';
  sending = false;
  chatError = '';

  // Proactive daily briefing - fetched once on open (cached server-side per
  // user/day), shown as a dismissible card above the thread rather than
  // injected into any one thread's history, which it isn't really part of.
  briefing: string | null = null;
  briefingDismissed = false;

  // ── Quick Drafts state (unchanged behaviour, existing feature) ─────
  features = [
    { value: 'GENERAL', label: 'General Assistant' },
    { value: 'CRM_LEAD_SUMMARY', label: 'CRM Lead Summary' },
    { value: 'CRM_ACTIVITY_SUMMARY', label: 'CRM Activity Summary' },
    { value: 'INVOICE_SUMMARY', label: 'Invoice Summary' },
    { value: 'SERVICE_REQUEST_SUMMARY', label: 'Service Request Summary' },
    { value: 'EMPLOYMENT_LETTER', label: 'Employment Letter' },
    { value: 'LEAVE_POLICY', label: 'Leave Policy Draft' },
    { value: 'PERFORMANCE_REVIEW', label: 'Performance Review Draft' },
    { value: 'ANNOUNCEMENT_DRAFT', label: 'Announcement Draft' },
    { value: 'HOLIDAY_DRAFT', label: 'Holiday Draft' },
    { value: 'WORKFLOW_SUGGESTION', label: 'Workflow Suggestion' },
  ];
  feature = 'GENERAL';
  prompt = '';
  result?: AiGenerateResponse;
  generating = false;
  error = '';

  announcementDraft?: AnnouncementDraftResponse;
  savingAnnouncement = false;
  announcementSaved = false;

  leavePolicyDraft?: { document: string };

  holidayDraft?: HolidayDraftResponse;
  savingHoliday = false;
  holidaySaved = false;

  constructor(
    private aiService: AiService,
    private announcementService: AnnouncementService,
    private leavePolicyService: LeavePolicyService,
    private holidayService: HolidayService,
    private cdr: ChangeDetectorRef,
    private speechInput: SpeechInputService,
  ) {}

  // ── Voice input ────────────────────────────────────────────
  get voiceSupported(): boolean {
    return this.speechInput.isSupported;
  }

  get listening(): boolean {
    return this.speechInput.isListening;
  }

  toggleVoiceInput(): void {
    if (this.speechInput.isListening) {
      this.speechInput.stop();
      return;
    }
    this.speechInput.start(
      (text) => { this.composerText = (this.composerText.trim() + ' ' + text).trim(); this.cdr.markForCheck(); },
      () => this.cdr.markForCheck(),
    );
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    this.loadThreads();
    this.loadDailyBriefing();
  }

  private loadDailyBriefing(): void {
    this.aiService.dailyBriefing().subscribe({
      next: (res) => {
        this.briefing = res.content;
        this.cdr.markForCheck();
      },
      // Not fatal - the assistant is fully usable without a briefing (e.g.
      // no AI provider configured yet), so fail silently here.
      error: () => {},
    });
  }

  dismissBriefing(): void {
    this.briefingDismissed = true;
  }

  // ── Chat / agent ───────────────────────────────────────────

  loadThreads(): void {
    this.loadingThreads = true;
    this.cdr.markForCheck();
    this.aiService.listThreads().subscribe({
      next: (res) => {
        this.threads = res.content;
        this.loadingThreads = false;
        // Land on the most recently active thread if one exists, rather
        // than an empty composer with no context.
        if (!this.activeThreadId && this.threads.length) {
          this.openThread(this.threads[0].id);
        } else {
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.loadingThreads = false;
        this.cdr.markForCheck();
      },
    });
  }

  startNewChat(): void {
    this.chatError = '';
    this.aiService.createThread('AGENT_TASK').subscribe({
      next: (thread) => {
        this.threads = [thread, ...this.threads];
        this.activeThreadId = thread.id;
        this.bubbles = [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.chatError = extractErrorMessage(err, 'Could not start a new chat');
        this.cdr.markForCheck();
      },
    });
  }

  openThread(threadId: number): void {
    this.activeThreadId = threadId;
    this.chatError = '';
    this.loadingMessages = true;
    this.cdr.markForCheck();
    this.aiService.threadMessages(threadId).subscribe({
      next: (res) => {
        // Each row is one full exchange (user message in requestPayload,
        // reply in result) - split into two bubbles, oldest first.
        this.bubbles = res.content.flatMap((row) => this.exchangeToBubbles(row));
        this.loadingMessages = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingMessages = false;
        this.cdr.markForCheck();
      },
    });
  }

  private exchangeToBubbles(row: AiGenerateResponse & { requestPayload?: string }): ChatBubble[] {
    // The thread-messages endpoint reuses AiGenerateResponse, whose `result`
    // is always the assistant's reply; the user's own message text isn't
    // separately exposed on that DTO today, so history replay shows the
    // assistant side only for now (still fully useful for resuming context -
    // the backend already re-feeds the real transcript on the next message).
    const meta = `${row.provider ?? ''} · ${row.model ?? ''} · ${row.executionTimeMs ?? 0}ms`;
    return [{ role: 'assistant', text: row.result, meta }];
  }

  sendMessage(): void {
    const text = this.composerText.trim();
    if (!text || this.sending) return;

    if (!this.activeThreadId) {
      this.aiService.createThread('AGENT_TASK').subscribe({
        next: (thread) => {
          this.threads = [thread, ...this.threads];
          this.activeThreadId = thread.id;
          this.doSend(text);
        },
        error: (err) => {
          this.chatError = extractErrorMessage(err, 'Could not start a new chat');
          this.cdr.markForCheck();
        },
      });
      return;
    }
    this.doSend(text);
  }

  private doSend(text: string): void {
    this.bubbles = [...this.bubbles, { role: 'user', text }];
    this.composerText = '';
    this.sending = true;
    this.chatError = '';
    this.cdr.markForCheck();

    this.aiService.agentTurn(this.activeThreadId!, text).subscribe({
      next: (res) => {
        this.bubbles = [
          ...this.bubbles,
          {
            role: 'assistant',
            text: res.result,
            awaitingConfirmation: res.awaitingConfirmation,
            meta: `${res.provider} · ${res.model} · ${res.executionTimeMs}ms`,
          },
        ];
        this.sending = false;
        this.loadThreads(); // refresh titles/ordering without switching the active thread away
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.chatError = extractErrorMessage(err, 'The assistant could not respond - check your AI provider configuration');
        this.sending = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** Confirm/cancel card buttons - just send the natural-language equivalent, same as typing it. */
  confirmPendingAction(): void {
    this.composerText = 'yes';
    this.sendMessage();
  }

  cancelPendingAction(): void {
    this.composerText = 'cancel';
    this.sendMessage();
  }

  deleteThread(threadId: number, event: Event): void {
    event.stopPropagation();
    this.aiService.deleteThread(threadId).subscribe({
      next: () => {
        this.threads = this.threads.filter((t) => t.id !== threadId);
        if (this.activeThreadId === threadId) {
          this.activeThreadId = null;
          this.bubbles = [];
        }
        this.cdr.markForCheck();
      },
    });
  }

  get lastBubbleAwaitingConfirmation(): boolean {
    const last = this.bubbles[this.bubbles.length - 1];
    return !!last && last.role === 'assistant' && !!last.awaitingConfirmation;
  }

  // ── Quick Drafts (unchanged) ──────────────────────────────

  generate(): void {
    if (!this.prompt.trim()) return;
    this.generating = true;
    this.error = '';
    this.result = undefined;
    this.announcementDraft = undefined;
    this.announcementSaved = false;
    this.leavePolicyDraft = undefined;
    this.holidayDraft = undefined;
    this.holidaySaved = false;
    this.cdr.markForCheck();

    if (this.feature === 'ANNOUNCEMENT_DRAFT') {
      this.announcementService.draftWithAi(this.prompt.trim()).subscribe({
        next: (draft) => {
          this.announcementDraft = draft;
          this.generating = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = extractErrorMessage(err, 'Generation failed — check your AI provider configuration');
          this.generating = false;
          this.cdr.markForCheck();
        },
      });
      return;
    }

    if (this.feature === 'HOLIDAY_DRAFT') {
      this.holidayService.draftWithAi(this.prompt.trim()).subscribe({
        next: (draft) => {
          this.holidayDraft = draft;
          this.generating = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = extractErrorMessage(err, 'Generation failed — check your AI provider configuration');
          this.generating = false;
          this.cdr.markForCheck();
        },
      });
      return;
    }

    if (this.feature === 'LEAVE_POLICY') {
      this.leavePolicyService.draftWithAi(false, this.prompt.trim()).subscribe({
        next: (draft) => {
          this.leavePolicyDraft = draft;
          this.generating = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = extractErrorMessage(err, 'Generation failed — configure an Annual leave policy for Full-time employees first');
          this.generating = false;
          this.cdr.markForCheck();
        },
      });
      return;
    }

    this.aiService.generate(this.feature, this.prompt.trim()).subscribe({
      next: (res) => {
        this.result = res;
        this.generating = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error =
          err?.error?.message || 'Generation failed — check your AI provider configuration';
        this.generating = false;
        this.cdr.markForCheck();
      },
    });
  }

  saveAsAnnouncement(): void {
    if (!this.announcementDraft || this.savingAnnouncement) return;
    this.savingAnnouncement = true;
    this.error = '';
    this.cdr.markForCheck();
    this.announcementService.create({
      title: this.announcementDraft.title,
      body: this.announcementDraft.body,
      audience: 'ALL',
      priority: 1,
    }).subscribe({
      next: () => {
        this.savingAnnouncement = false;
        this.announcementSaved = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = extractErrorMessage(err, 'Failed to save announcement');
        this.savingAnnouncement = false;
        this.cdr.markForCheck();
      },
    });
  }

  saveAsHoliday(): void {
    if (!this.holidayDraft || this.savingHoliday) return;
    this.savingHoliday = true;
    this.error = '';
    this.cdr.markForCheck();
    this.holidayService.create({
      name: this.holidayDraft.name,
      holidayDate: this.holidayDraft.date,
      holidayType: this.holidayDraft.type,
      description: this.holidayDraft.description,
    }).subscribe({
      next: () => {
        this.savingHoliday = false;
        this.holidaySaved = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = extractErrorMessage(err, 'Failed to save holiday');
        this.savingHoliday = false;
        this.cdr.markForCheck();
      },
    });
  }
}
