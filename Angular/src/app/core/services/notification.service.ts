import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { ApiService, PagedResponse } from './api.service';
import {
  Notification,
  NotificationCount,
  NotificationPreference,
  UpdateNotificationPreferenceRequest,
} from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly endpoint = '/notifications';
  private readonly preferencesEndpoint = '/notification-preferences';

  // POLLING INTERVAL FOR UNREAD BADGE (60 seconds)
  private readonly pollIntervalMs = 60000;

  // REACTIVE UNREAD COUNT USED BY THE NAVBAR BELL
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$: Observable<number> = this.unreadCountSubject.asObservable();

  private polling = false;

  constructor(private api: ApiService) {}

  // NOTIFICATIONS
  list(unreadOnly = false, page = 0, size = 20): Observable<PagedResponse<Notification>> {
    return this.api.getPaged<Notification>(this.endpoint, page, size, { unreadOnly });
  }

  getUnreadCount(): Observable<NotificationCount> {
    return this.api.get<NotificationCount>(`${this.endpoint}/count`);
  }

  markAsRead(id: number): Observable<string> {
    return this.api.patch<string>(`${this.endpoint}/${id}/read`, {});
  }

  markAllAsRead(): Observable<string> {
    return this.api.patch<string>(`${this.endpoint}/read-all`, {});
  }

  // PREFERENCES
  getPreferences(): Observable<NotificationPreference> {
    return this.api.get<NotificationPreference>(this.preferencesEndpoint);
  }

  updatePreferences(payload: UpdateNotificationPreferenceRequest): Observable<NotificationPreference> {
    return this.api.put<NotificationPreference>(this.preferencesEndpoint, payload);
  }

  resetPreferences(): Observable<NotificationPreference> {
    return this.api.delete<NotificationPreference>(this.preferencesEndpoint);
  }

  // START POLLING (SAFE TO CALL MULTIPLE TIMES)
  startPolling(): void {
    if (this.polling) return;
    this.polling = true;
    timer(0, this.pollIntervalMs)
      .pipe(
        switchMap(() => this.getUnreadCount()),
        tap((res) => this.unreadCountSubject.next(res.unreadCount ?? 0))
      )
      .subscribe({
        error: () => { /* keep polling; ignore transient errors */ }
      });
  }

  // MANUAL REFRESH USED AFTER MARK-AS-READ ACTIONS
  refreshCount(): void {
    this.getUnreadCount().subscribe({
      next: (res) => this.unreadCountSubject.next(res.unreadCount ?? 0),
      error: () => { /* no-op */ }
    });
  }
}
