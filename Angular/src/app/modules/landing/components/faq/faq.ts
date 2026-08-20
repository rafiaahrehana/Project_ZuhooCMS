import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './faq.html',
  styleUrls: ['./faq.scss']
})
export class FaqComponent {
  faqs = signal([
    {
      q: 'How long does implementation usually take?',
      a: 'Depending on the complexity of your current data and required modules, typical onboarding takes between 2 to 4 weeks. Our dedicated success team will guide you through data migration and setup.',
      open: false
    },
    {
      q: 'Can we integrate with our existing tools?',
      a: 'Yes, ZuhooCMS provides robust REST APIs and pre-built integrations for over 100 popular tools including Slack, Salesforce, Google Workspace, and Microsoft 365.',
      open: false
    },
    {
      q: 'Is our data secure and compliant?',
      a: 'Absolutely. We are SOC 2 Type II certified, GDPR compliant, and offer HIPAA compliant environments for healthcare customers. All data is encrypted at rest and in transit.',
      open: false
    },
    {
      q: 'What kind of support is included?',
      a: 'All plans include 24/5 email support. Professional and Enterprise tiers include priority 24/7 phone routing and dedicated Customer Success Managers.',
      open: false
    }
  ]);

  toggle(index: number) {
    this.faqs.update(items => {
      const newItems = [...items];
      newItems[index].open = !newItems[index].open;
      return newItems;
    });
  }
}
