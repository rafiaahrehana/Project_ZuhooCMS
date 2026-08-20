import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-faq-item',
  standalone: true,
  template: `
    <div class="accordion-item border-0 mb-3" style="border-radius: var(--site-radius); overflow: hidden;">
      <h2 class="accordion-header">
        <button class="accordion-button fw-semibold collapsed" [attr.data-bs-toggle]="'collapse'"
                [attr.data-bs-target]="'#faq' + faq.id" type="button"
                style="border-radius: var(--site-radius);">
          {{ faq.question }}
        </button>
      </h2>
      <div [id]="'faq' + faq.id" class="accordion-collapse collapse" data-bs-parent="#faqAccordion">
        <div class="accordion-body text-muted">{{ faq.answer }}</div>
      </div>
    </div>
  `,
  styles: [`
    .accordion-button { background: #f8fafc; }
    .accordion-button:not(.collapsed) { background: rgba(var(--site-primary-rgb), 0.08); color: var(--site-primary); }
    .accordion-button::after { background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%23334155'%3e%3cpath d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e"); }
  `]
})
export class FaqItemComponent {
  @Input({ required: true }) faq!: { id: number; question: string; answer: string };
}
