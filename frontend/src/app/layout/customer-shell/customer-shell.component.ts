import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CustomerAuthService } from '../../core/customer-auth/customer-auth.service';
import { LucideCar, LucideLogOut } from '@lucide/angular';

@Component({
  selector: 'acr-customer-shell',
  imports: [MatButtonModule, MatIconModule, RouterLink, RouterLinkActive, RouterOutlet, LucideCar, LucideLogOut],
  template: `
    <div class="customer-shell">
      <header>
        <a class="brand" routerLink="/rent"><span class="brand-icon"><svg lucideCar [size]="21"></svg></span><span>AI Car Rental</span></a>
        <nav>
          <a
            routerLink="/rent"
            routerLinkActive="active"
            [routerLinkActiveOptions]="findCarActiveOptions"
          >Find a car</a>
          <a routerLink="/rent/track" routerLinkActive="active">Track reservation</a>
          @if (auth.isAuthenticated()) {
            <a routerLink="/customer/account/reservations" routerLinkActive="active">My bookings</a>
            <button mat-stroked-button type="button" (click)="logout()"><svg lucideLogOut [size]="17"></svg>Sign out</button>
          } @else {
            <a class="signin" routerLink="/customer/login">Sign in</a>
          }
        </nav>
      </header>
      <main><router-outlet /></main>
      <footer><span>AI Car Rental Marketplace</span><span>Secure tenant-aware booking</span></footer>
    </div>
  `,
  styles: [`
    .customer-shell{min-height:100vh;background:#f4f6f8;color:#162033;display:flex;flex-direction:column;overflow-x:hidden}
    header{height:72px;background:#fff;border-bottom:1px solid #dce2e9;display:flex;align-items:center;justify-content:space-between;padding:0 clamp(18px,5vw,72px);position:sticky;top:0;z-index:20}
    .brand{display:flex;align-items:center;gap:11px;font-size:18px;font-weight:750}.brand-icon{display:grid;place-items:center;width:38px;height:38px;background:#182438;color:#fff;border-radius:7px}.brand-icon svg,nav button svg{flex:0 0 auto}
    nav{display:flex;align-items:center;gap:24px;font-size:14px;font-weight:550}nav a{color:#526176;padding:26px 0 22px;border-bottom:3px solid transparent}nav a.active{color:#1261b5;border-color:#1261b5}.signin{color:#1261b5!important}
    main{width:100%;max-width:1316px;margin:0 auto;flex:1;padding:28px 18px 48px}footer{border-top:1px solid #dce2e9;background:#fff;padding:20px clamp(18px,5vw,72px);display:flex;justify-content:space-between;color:#657287;font-size:13px}
    @media(max-width:760px){header{height:auto;align-items:flex-start;flex-direction:column;padding:14px 18px;gap:10px}nav{width:100%;gap:14px;overflow-x:auto}nav a{padding:8px 0 10px;white-space:nowrap}nav button{white-space:nowrap}main{width:100%;padding:18px 12px 40px}footer{flex-direction:column;gap:5px}}
  `]
})
export class CustomerShellComponent {
  readonly auth = inject(CustomerAuthService);
  readonly findCarActiveOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored'
  } as const;
  private readonly router = inject(Router);
  logout(): void { this.auth.logout(); void this.router.navigate(['/rent']); }
}
