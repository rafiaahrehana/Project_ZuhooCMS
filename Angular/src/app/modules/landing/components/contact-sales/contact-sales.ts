import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../../core/services/api.service';

/**
 * The public "contact sales" form. Posts to the anonymous lead-capture
 * endpoint, so a submission becomes a Lead in the platform company's CRM
 * instead of vanishing - the previous version showed a thank-you alert and
 * threw the data away.
 */
@Component({
  selector: 'app-contact-sales',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './contact-sales.html',
  styleUrls: ['./contact-sales.scss']
})
export class ContactSales {
  form = {
    name: '',
    email: '',
    phone: '',
    companyName: '',
    message: '',
    // Honeypot - stays empty for humans; the server drops filled ones.
    website: '',
  };

  sending = false;
  sent = false;
  error = '';

  constructor(private api: ApiService) {}

  submitForm(event: Event): void {
    event.preventDefault();
    if (this.sending || !this.form.name.trim() || !this.form.email.trim()) return;

    this.sending = true;
    this.error = '';

    this.api.post('/public/crm/leads', this.form).subscribe({
      next: () => {
        this.sending = false;
        this.sent = true;
      },
      error: (err) => {
        this.sending = false;
        this.error = err?.error?.message || 'Something went wrong - please try again.';
      },
    });
  }
}
