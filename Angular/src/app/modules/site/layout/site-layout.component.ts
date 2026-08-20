import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SiteService } from '../services/site.service';
import { ThemeDirective } from '../theme/theme.directive';
import { SiteNavbarComponent } from '../components/site-navbar/site-navbar.component';
import { SiteFooterComponent } from '../components/site-footer/site-footer.component';
import { SiteSettings } from '../models/site.model';
import { DEFAULT_SITE } from '../services/default-site.config';

@Component({
  selector: 'app-site-layout',
  standalone: true,
  imports: [RouterOutlet, ThemeDirective, SiteNavbarComponent, SiteFooterComponent],
  template: `
    <div class="site-wrapper"
         [appTheme]="settings?.theme"
         [primaryOverride]="settings?.primaryColor"
         [secondaryOverride]="settings?.secondaryColor">
      <app-site-navbar [settings]="settings" [nav]="nav"></app-site-navbar>
      <main>
        <router-outlet></router-outlet>
      </main>
      <app-site-footer [settings]="settings"></app-site-footer>
    </div>
  `,
  styles: [`
    .site-wrapper {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      font-family: var(--site-font);
    }
    main { flex: 1; }
  `]
})
export class SiteLayoutComponent implements OnInit {
  private siteService = inject(SiteService);

  settings: SiteSettings = DEFAULT_SITE;
  nav: any[] = [];

  ngOnInit(): void {
    this.siteService.getSettings().subscribe(s => this.settings = s);
    this.siteService.getNav().subscribe(n => this.nav = n);
  }
}
