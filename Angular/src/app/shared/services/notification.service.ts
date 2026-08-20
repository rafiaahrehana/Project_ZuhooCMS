import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'success' | 'danger' | 'warning' | 'info';
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  success(message: string): void { this.push('success', message); }
  error(message: string): void { this.push('danger', message); }
  warning(message: string): void { this.push('warning', message); }
  info(message: string): void { this.push('info', message); }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(type: Toast['type'], message: string): void {
    const toast: Toast = { id: this.nextId++, type, message };
    this.toasts.update(list => [...list, toast]);
    setTimeout(() => this.dismiss(toast.id), 5000);
  }
}
