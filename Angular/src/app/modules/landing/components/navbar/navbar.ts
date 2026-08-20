import { Component, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent {
  isScrolled = signal(false);
  isMobileMenuOpen = signal(false);

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled.set(window.scrollY > 50);
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.update(v => !v);
  }

  scrollTo(id: string, event: Event) {
    event.preventDefault();
    const element = document.getElementById(id);
    if (element) {
      // Measure the navbar rather than assuming 80px - its padding is fluid,
      // so a hardcoded offset leaves the target under the bar at one end of
      // the range and floating below it at the other.
      const nav = document.querySelector('nav.navbar') as HTMLElement | null;
      const offset = (nav?.offsetHeight ?? 72) + 12;
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      const y = element.getBoundingClientRect().top + window.scrollY - offset;
      window.scrollTo({ top: y, behavior: reduce ? 'auto' : 'smooth' });
    }
    this.isMobileMenuOpen.set(false);
  }
}
