import { Component, OnDestroy, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Notification } from '../../../core/models/notification.model';

@Component({
  selector: 'app-notification-bell',
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationBell implements OnInit, OnDestroy {
  // VARIABLES
  unread = 0;
  open = false;
  loading = false;
  recent: Notification[] = [];

  private countSub?: Subscription;

  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    this.notificationService.startPolling();
    this.countSub = this.notificationService.unreadCount$.subscribe(n => {
      this.unread = n;
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.countSub?.unsubscribe();
  }

  // TOGGLE DROPDOWN
  toggle(): void {
    this.open = !this.open;
    if (this.open) this.loadRecent();
  }

  close(): void {
    this.open = false;
  }

  // LOAD RECENT NOTIFICATIONS
  loadRecent(): void {
    this.loading = true;
    this.notificationService.list(false, 0, 5).subscribe({
      next: (res) => {
        this.recent = res.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  // MARK ONE AS READ AND OPTIONALLY NAVIGATE
  openNotification(n: Notification): void {
    if (!n.read) {
      this.notificationService.markAsRead(n.id).subscribe({
        next: () => {
          n.read = true;
          this.notificationService.refreshCount();
          this.cdr.markForCheck();
        }
      });
    }
    this.close();
    if (n.actionUrl) {
      let targetUrl = n.actionUrl;
      if (targetUrl.startsWith('/announcements/') || targetUrl.startsWith('/hrm/announcements/')) {
        targetUrl = '/hrm/announcements';
      }
      this.router.navigateByUrl(targetUrl);
    }
  }

  // MARK EVERYTHING AS READ
  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.recent.forEach(n => n.read = true);
        this.notificationService.refreshCount();
        this.cdr.markForCheck();
      }
    });
  }
}
