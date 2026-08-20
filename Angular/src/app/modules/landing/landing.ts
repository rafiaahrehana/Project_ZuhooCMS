import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { NavbarComponent } from './components/navbar/navbar';
import { HeroComponent } from './components/hero/hero';
import { WhyBusinessOsComponent } from './components/why-business-os/why-business-os';
import { ModulesComponent } from './components/modules/modules';
import { DashboardPreviewComponent } from './components/dashboard-preview/dashboard-preview';
import { PricingComponent } from './components/pricing/pricing';
import { FaqComponent } from './components/faq/faq';
import { CtaComponent } from './components/cta/cta';
import { FooterComponent } from './components/footer/footer';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    HeroComponent,
    WhyBusinessOsComponent,
    ModulesComponent,
    DashboardPreviewComponent,
    PricingComponent,
    FaqComponent,
    CtaComponent,
    FooterComponent
  ],
  // The accent-* class on each section sets the --accent channel that
  // global.scss's "LANDING ACCENT SYSTEM" reads. Custom properties inherit
  // through Angular's view-encapsulation boundary, so each section's own SCSS
  // picks the colour up with no specificity fight - recolouring the page is a
  // one-word edit here, and nowhere else.
  //
  // Rhythm: no two neighbours share a hue family. `lime` is deliberately
  // absent as a section wash (it reads sickly at full-section scale) and
  // appears only inside the Modules card grid. trusted-companies takes no
  // accent - its logos carry third-party brand colours that must stay literal.
  template: `
    <div class="landing-wrapper">
      <app-navbar class="accent-emerald"></app-navbar>

      <!-- Seven sections, one design language. The page was fourteen sections
           designed separately; everything that wasn't pulling weight was cut
           (fake logos, fabricated testimonials, four feature sections saying
           "modules" four ways) and what remains follows the hero's rhythm:
           dark product opening, light middle, dark close. -->
      <!-- Charcoal-emerald skin (the mockup): every band is dark, emerald
           and teal alternate as the accent channel. -->
      <main>
        <app-hero class="accent-emerald"></app-hero>
        <app-dashboard-preview class="accent-teal"></app-dashboard-preview>
        <app-why-business-os class="accent-emerald"></app-why-business-os>
        <app-modules class="accent-teal"></app-modules>
        <app-pricing class="accent-emerald"></app-pricing>
        <app-faq class="accent-teal"></app-faq>
        <app-cta class="accent-emerald"></app-cta>
      </main>

      <app-footer class="accent-emerald"></app-footer>
    </div>
  `,
  styles: [`
    .landing-wrapper {
      scroll-behavior: smooth;
      overflow-x: hidden;
    }
  `]
})
export class Landing {
}
