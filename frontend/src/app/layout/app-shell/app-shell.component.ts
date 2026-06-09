import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';
import { NAVIGATION_ITEMS } from './navigation-item';

@Component({
  selector: 'acr-app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly mobileNavOpen = signal(false);
  readonly session = this.authService.session;
  readonly navItems = computed(() =>
    NAVIGATION_ITEMS.filter((item) =>
      !item.roles || this.authService.hasAnyRole(item.roles)
    )
  );

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }
}
