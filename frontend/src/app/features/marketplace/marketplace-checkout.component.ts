import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CustomerAuthService } from '../../core/customer-auth/customer-auth.service';
import { formatTryAmount } from '../../core/format/currency.util';
import { PHONE_VALIDATORS } from '../../core/validation/phone.validation';
import { MarketplaceVehicleDetail, ReservationResponse } from './marketplace.models';
import { MarketplaceService } from './marketplace.service';

@Component({
  selector:'acr-marketplace-checkout',imports:[ReactiveFormsModule,RouterLink,MatButtonModule,MatFormFieldModule,MatIconModule,MatInputModule,MatSelectModule],
  template:`
    <a class="back" [routerLink]="['/rent/vehicles',vehicleId]" [queryParams]="queryParams"><mat-icon>arrow_back</mat-icon>Vehicle details</a>
    <div class="checkout-grid">
      <section class="checkout-panel"><h1>Complete your reservation</h1><p class="lead">Review the rental period and provide the driver information.</p>
        @if(error()){<div class="error">{{error()}}</div>}
        @if(reservation();as booked){
          <div class="reserved"><mat-icon>check_circle</mat-icon><div><h2>Reservation created</h2><p>Reference: <strong>{{booked.reservationCode}}</strong></p><p>Your vehicle is held pending the demo deposit payment. No real card will be charged.</p></div></div>
          <button mat-flat-button color="primary" class="pay" (click)="payDeposit()" [disabled]="paying()">{{paying()?'Processing...':'Pay '+money(booked.depositAmount)+' deposit'}}</button>
        }@else{
          @if(auth.isAuthenticated()){<div class="account-note"><mat-icon>account_circle</mat-icon><span>Booking as <strong>{{auth.session()?.email}}</strong></span></div>}@else{<div class="guest-note"><span>Continue as guest, or</span><a routerLink="/customer/login" [queryParams]="{returnUrl:currentUrl}">sign in</a><span>to keep this booking in your account.</span></div>}
          <form [formGroup]="form" (ngSubmit)="reserve()">
            @if(!auth.isAuthenticated()){
              <div class="form-grid"><mat-form-field appearance="outline"><mat-label>Full name</mat-label><input matInput formControlName="customerFullName"></mat-form-field><mat-form-field appearance="outline"><mat-label>Email</mat-label><input matInput type="email" formControlName="customerEmail"></mat-form-field><mat-form-field appearance="outline"><mat-label>Phone</mat-label><input matInput type="tel" inputmode="tel" autocomplete="tel" maxlength="20" formControlName="customerPhone">@if(form.controls.customerPhone.hasError('required')){<mat-error>Phone number is required</mat-error>}@else if(form.controls.customerPhone.hasError('pattern')||form.controls.customerPhone.hasError('maxlength')){<mat-error>Enter 7 to 15 valid digits</mat-error>}</mat-form-field></div>
            }
            <mat-form-field appearance="outline" class="full"><mat-label>Identity number</mat-label><input matInput formControlName="customerIdentityNumber"><mat-hint>Required by the rental company at pickup</mat-hint></mat-form-field>
            <mat-form-field appearance="outline" class="full"><mat-label>Insurance package</mat-label><mat-select formControlName="insurancePackageId"><mat-option [value]="null">No optional package</mat-option>@for(p of vehicle()?.insurancePackages||[];track p.id){<mat-option [value]="p.id">{{p.name}} · {{money(p.dailyPrice)}} / day</mat-option>}</mat-select></mat-form-field>
            <label class="confirm"><input type="checkbox" formControlName="terms"> <span>I confirm the rental dates and customer information are correct.</span></label>
            <button mat-flat-button color="primary" class="reserve" type="submit" [disabled]="saving()">{{saving()?'Creating reservation...':'Create reservation'}}</button>
          </form>
        }
      </section>
      <aside>@if(vehicle();as v){<div class="vehicle"><div class="thumb">@if(v.imageUrl){<img [src]="v.imageUrl" [alt]="v.brand+' '+v.model">}@else{<mat-icon>directions_car</mat-icon>}</div><span>{{v.tenantName}}</span><h2>{{v.brand}} {{v.model}}</h2></div><dl><div><dt>Pickup</dt><dd>{{pickupLabel}}</dd></div><div><dt>Return</dt><dd>{{returnLabel}}</dd></div><div><dt>Daily rate</dt><dd>{{money(v.dailyPrice)}}</dd></div><div><dt>Estimated rental</dt><dd>{{money(estimatedRental())}}</dd></div><div class="total"><dt>Estimated total</dt><dd>{{money(estimatedTotal())}}</dd></div></dl>}</aside>
    </div>
  `,
  styles:[`
    .back{display:inline-flex;align-items:center;gap:6px;color:#315b86;margin-bottom:16px}.checkout-grid{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(300px,.7fr);gap:18px}.checkout-panel,aside{background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:24px}.checkout-panel h1{margin:0;font-size:27px}.lead{color:#68758a;margin:7px 0 22px}.form-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.full{width:100%}.account-note,.guest-note{display:flex;align-items:center;gap:7px;background:#f0f6fc;border:1px solid #cfe0f2;padding:12px;border-radius:6px;margin-bottom:18px}.guest-note a{color:#1261b5;font-weight:700}.confirm{display:flex;gap:9px;color:#536174;font-size:14px;margin:7px 0 20px}.reserve,.pay{width:100%;height:48px}.error{background:#fff1f1;border:1px solid #efb7b7;color:#a11c1c;padding:11px;border-radius:6px;margin-bottom:15px}.reserved{display:flex;gap:12px;padding:18px;background:#effaf2;border:1px solid #bfe2c7;border-radius:7px;margin:20px 0}.reserved mat-icon{color:#268342}.reserved h2,.reserved p{margin:0 0 5px}.vehicle .thumb{height:150px;background:#eef2f6;display:grid;place-items:center;border-radius:6px;overflow:hidden;margin-bottom:13px}.thumb img{width:100%;height:100%;object-fit:cover}.thumb mat-icon{font-size:45px;width:45px;height:45px}.vehicle span{font-size:12px;color:#1261b5;font-weight:700}.vehicle h2{margin:4px 0 18px}dl{margin:0}dl div{display:flex;justify-content:space-between;gap:12px;padding:11px 0;border-top:1px solid #e7ebf0}dt{color:#68758a}dd{margin:0;text-align:right;font-weight:650}.total{font-size:17px}@media(max-width:820px){.checkout-grid{grid-template-columns:1fr}.form-grid{grid-template-columns:1fr}} 
  `]
})
export class MarketplaceCheckoutComponent implements OnInit{
  private readonly fb=inject(FormBuilder);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);private readonly service=inject(MarketplaceService);readonly auth=inject(CustomerAuthService);
  readonly vehicle=signal<MarketplaceVehicleDetail|null>(null);readonly reservation=signal<ReservationResponse|null>(null);readonly saving=signal(false);readonly paying=signal(false);readonly error=signal('');readonly vehicleId=Number(this.route.snapshot.paramMap.get('vehicleId'));readonly queryParams={...this.route.snapshot.queryParams};readonly currentUrl=this.router.url;pickup='';returnDate='';pickupLabel='';returnLabel='';private idempotencyKey=this.uuid();
  readonly form=this.fb.group({customerFullName:['',Validators.required],customerEmail:['',[Validators.required,Validators.email]],customerPhone:['',PHONE_VALIDATORS],customerIdentityNumber:['',Validators.required],insurancePackageId:[null as number|null],terms:[false,Validators.requiredTrue]});
  readonly estimatedRental=computed(()=>{const v=this.vehicle();return v?v.dailyPrice*this.days():0});estimatedTotal():number{const v=this.vehicle();if(!v)return 0;const insurance=v.insurancePackages.find(p=>p.id===this.form.controls.insurancePackageId.value);return this.estimatedRental()*1.3+(insurance?.dailyPrice||0)*this.days()}
  ngOnInit():void{this.pickup=String(this.route.snapshot.queryParamMap.get('pickupDateTime')||'');this.returnDate=String(this.route.snapshot.queryParamMap.get('returnDateTime')||'');this.pickupLabel=this.label(this.pickup);this.returnLabel=this.label(this.returnDate);if(!this.pickup||!this.returnDate)this.error.set('Pickup and return dates are required.');this.service.getVehicle(this.vehicleId).subscribe({next:v=>this.vehicle.set(v),error:e=>this.error.set(e.error?.message||'Vehicle could not be loaded.')});if(this.auth.isAuthenticated()){this.form.controls.customerFullName.clearValidators();this.form.controls.customerEmail.clearValidators();this.form.controls.customerPhone.clearValidators();}}
  reserve():void{if(this.form.invalid||!this.vehicle()) {this.form.markAllAsTouched();return;}this.saving.set(true);this.error.set('');const value=this.form.getRawValue();const common={vehicleId:this.vehicleId,customerIdentityNumber:value.customerIdentityNumber,pickupDateTime:this.pickup,returnDateTime:this.returnDate,insurancePackageId:value.insurancePackageId};const call=this.auth.isAuthenticated()?this.service.createCustomerReservation({...common,tenantSlug:this.vehicle()!.tenantSlug}):this.service.createGuestReservation(this.vehicle()!.tenantSlug,{...common,customerFullName:value.customerFullName,customerEmail:value.customerEmail,customerPhone:value.customerPhone});call.subscribe({next:r=>this.reservation.set(r),error:e=>{this.error.set(e.error?.message||'Reservation could not be created.');this.saving.set(false)},complete:()=>this.saving.set(false)});}
  payDeposit():void{const r=this.reservation();const v=this.vehicle();if(!r||!v)return;this.paying.set(true);this.error.set('');const email=this.auth.session()?.email||r.customerEmail;const call=this.auth.isAuthenticated()?this.service.payCustomerDeposit(r.reservationCode,this.idempotencyKey):this.service.payGuestDeposit(v.tenantSlug,r.reservationCode,email,this.idempotencyKey);call.subscribe({next:()=>void this.router.navigate(['/rent/reservation/success'],{queryParams:{reservationCode:r.reservationCode,email,status:'DEPOSIT_PAID'}}),error:e=>{this.error.set(e.error?.message||'Deposit payment failed.');this.paying.set(false)}});}
  money(v:number):string{return formatTryAmount(v)}private days():number{return Math.max(1,Math.floor((new Date(this.returnDate).getTime()-new Date(this.pickup).getTime())/86400000))}private label(v:string):string{return v?new Date(v).toLocaleString('en-GB',{dateStyle:'medium',timeStyle:'short'}):'-'}private uuid():string{return globalThis.crypto?.randomUUID?.()||`${Date.now()}-${Math.random()}`}
}
