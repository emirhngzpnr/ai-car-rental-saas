import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { formatTryAmount } from '../../core/format/currency.util';
import { MarketplaceVehicle, VehicleSearchCriteria } from './marketplace.models';
import { MarketplaceService } from './marketplace.service';
import { LucideCar, LucideCircleAlert, LucideClock, LucideSearch } from '@lucide/angular';

@Component({
  selector: 'acr-marketplace-search',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, MatDatepickerModule, MatNativeDateModule, LucideCar, LucideCircleAlert, LucideClock, LucideSearch],
  template: `
    <section class="marketplace-page">
      <div class="page-heading"><div><h1>Find an available car</h1><p>Compare live availability and rental terms across verified rental companies.</p></div><a mat-stroked-button routerLink="/rent/track"><svg lucideSearch [size]="17"></svg>Track booking</a></div>
      <form class="search-panel" [formGroup]="form" (ngSubmit)="search(0)">
        <div class="primary-search">
          <div class="date-time-group">
            <mat-form-field appearance="outline"><mat-label>Pickup date</mat-label><input matInput [matDatepicker]="pickupPicker" [min]="today" formControlName="pickupDate" readonly><mat-datepicker-toggle matIconSuffix [for]="pickupPicker"></mat-datepicker-toggle><mat-datepicker #pickupPicker></mat-datepicker></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Time</mat-label><mat-select formControlName="pickupTime">@for(time of timeOptions;track time){<mat-option [value]="time">{{time}}</mat-option>}</mat-select></mat-form-field>
          </div>
          <div class="date-time-group">
            <mat-form-field appearance="outline"><mat-label>Return date</mat-label><input matInput [matDatepicker]="returnPicker" [min]="form.controls.pickupDate.value || today" formControlName="returnDate" readonly><mat-datepicker-toggle matIconSuffix [for]="returnPicker"></mat-datepicker-toggle><mat-datepicker #returnPicker></mat-datepicker></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Time</mat-label><mat-select formControlName="returnTime">@for(time of timeOptions;track time){<mat-option [value]="time">{{time}}</mat-option>}</mat-select></mat-form-field>
          </div>
          <mat-form-field appearance="outline"><mat-label>Location</mat-label><input matInput formControlName="location" placeholder="City or branch"></mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="loading()"><svg lucideSearch [size]="18"></svg>Search</button>
        </div>
        <div class="filters">
          <mat-form-field appearance="outline"><mat-label>Min price</mat-label><input matInput type="number" formControlName="minDailyPrice"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Max price</mat-label><input matInput type="number" formControlName="maxDailyPrice"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Min daily km</mat-label><input matInput type="number" formControlName="minDailyKmLimit"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Brand</mat-label><input matInput formControlName="brand"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Category</mat-label><mat-select formControlName="category"><mat-option value="">All</mat-option>@for(v of categories;track v){<mat-option [value]="v">{{v}}</mat-option>}</mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Transmission</mat-label><mat-select formControlName="transmission"><mat-option value="">All</mat-option><mat-option value="AUTOMATIC">Automatic</mat-option><mat-option value="MANUAL">Manual</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Fuel</mat-label><mat-select formControlName="fuelType"><mat-option value="">All</mat-option>@for(v of fuels;track v){<mat-option [value]="v">{{v}}</mat-option>}</mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Min seats</mat-label><input matInput type="number" formControlName="minSeats"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Sort</mat-label><mat-select formControlName="sort"><mat-option value="recommended">Recommended</mat-option><mat-option value="priceAsc">Price low to high</mat-option><mat-option value="priceDesc">Price high to low</mat-option><mat-option value="kmLimitDesc">Highest km limit</mat-option></mat-select></mat-form-field>
        </div>
      </form>
      @if(error()){<div class="state error"><svg lucideCircleAlert [size]="22"></svg><div><strong>Search could not be completed</strong><span>{{error()}}</span></div></div>}
      @if(loading()){<div class="state"><svg lucideClock [size]="22"></svg><span>Checking live vehicle availability...</span></div>}
      @else if(!error()){
        <div class="results-heading"><div><strong>{{total()}} vehicles available</strong><span>Prices shown per day</span></div></div>
        @if(vehicles().length===0){<div class="state"><svg lucideCar [size]="25"></svg><div><strong>No matching vehicles</strong><span>Try a wider date range or fewer filters.</span></div></div>}
        <div class="vehicle-grid">
          @for(vehicle of vehicles();track vehicle.vehicleId){
            <article class="vehicle-card">
              <div class="vehicle-media">@if(vehicle.imageUrl){<img [src]="vehicle.imageUrl" [alt]="vehicle.brand+' '+vehicle.model">}@else{<svg lucideCar [size]="50" [strokeWidth]="1.5"></svg>}</div>
              <div class="vehicle-body"><div class="tenant">{{vehicle.tenantName}}</div><h2>{{vehicle.brand}} {{vehicle.model}}</h2><div class="meta"><span>{{vehicle.category||'Vehicle'}}</span><span>{{vehicle.transmission||'Not specified'}}</span><span>{{vehicle.fuelType||'Not specified'}}</span><span>{{vehicle.dailyKmLimit}} km/day</span></div></div>
              <div class="vehicle-footer"><div><strong>{{money(vehicle.dailyPrice)}}</strong><span>per day</span></div><a mat-flat-button color="primary" [routerLink]="['/rent/vehicles',vehicle.vehicleId]" [queryParams]="detailParams()">View details</a></div>
            </article>
          }
        </div>
        @if(totalPages()>1){<div class="pager"><button mat-stroked-button (click)="search(page()-1)" [disabled]="page()===0">Previous</button><span>Page {{page()+1}} of {{totalPages()}}</span><button mat-stroked-button (click)="search(page()+1)" [disabled]="page()+1>=totalPages()">Next</button></div>}
      }
    </section>
  `,
  styles: [`
    .marketplace-page{display:flex;flex-direction:column;gap:18px;font-family:Roboto,Arial,sans-serif;min-width:0;max-width:100%;overflow-x:hidden}.page-heading,.results-heading{display:flex;justify-content:space-between;align-items:flex-start}.page-heading h1{font-size:28px;margin:0 0 5px}.page-heading p,.results-heading span{margin:0;color:#68758a}.page-heading a,.primary-search button{gap:7px}.page-heading a svg,.primary-search button svg{flex:0 0 auto}.search-panel{background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:18px;min-width:0;max-width:100%;overflow:hidden}.primary-search{display:grid;grid-template-columns:minmax(360px,1.35fr) minmax(360px,1.35fr) minmax(190px,1fr) auto;gap:12px;align-items:start;min-width:0;max-width:100%}.primary-search>*{min-width:0;max-width:100%}.date-time-group{display:grid;grid-template-columns:minmax(210px,1fr) minmax(120px,.55fr);gap:8px;min-width:0;max-width:100%}.primary-search button{height:56px;min-width:132px}.filters{display:grid;grid-template-columns:repeat(5,minmax(130px,1fr));gap:10px;border-top:1px solid #e7ebf0;padding-top:14px;min-width:0;max-width:100%}.filters>*{min-width:0;max-width:100%}.filters mat-form-field,.primary-search mat-form-field{width:100%;min-width:0;max-width:100%}.results-heading div{display:flex;flex-direction:column;gap:3px}.vehicle-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}.vehicle-card{background:#fff;border:1px solid #dce2e9;border-radius:8px;overflow:hidden;display:flex;flex-direction:column;min-width:0}.vehicle-media{aspect-ratio:16/8;background:#eef2f6;display:grid;place-items:center;color:#8290a4}.vehicle-media img{width:100%;height:100%;object-fit:cover}.vehicle-body{padding:16px}.tenant{font-size:12px;color:#1261b5;font-weight:700;text-transform:uppercase}.vehicle-body h2{font-size:19px;margin:5px 0 12px}.meta{display:flex;gap:6px;flex-wrap:wrap}.meta span{font-size:12px;background:#f2f5f8;padding:5px 7px;border-radius:4px;color:#536174}.vehicle-footer{border-top:1px solid #e7ebf0;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;margin-top:auto}.vehicle-footer div{display:flex;flex-direction:column}.vehicle-footer strong{font-size:18px}.vehicle-footer span{font-size:11px;color:#68758a}.state{min-height:150px;background:#fff;border:1px solid #dce2e9;border-radius:8px;display:flex;align-items:center;justify-content:center;gap:12px;color:#5c6879}.state>svg{flex:0 0 auto}.state div{display:flex;flex-direction:column}.state.error{border-color:#efb7b7;background:#fff8f8;color:#a11c1c}.pager{display:flex;justify-content:center;align-items:center;gap:18px;padding-top:8px}
    @media(max-width:1180px){.primary-search{grid-template-columns:1fr 1fr}.primary-search button{width:100%}.filters{grid-template-columns:repeat(3,1fr)}.vehicle-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:700px){.page-heading{flex-direction:column;gap:12px}.primary-search,.filters,.vehicle-grid,.date-time-group{grid-template-columns:1fr}.search-panel{padding:12px}}
  `]
})
export class MarketplaceSearchComponent implements OnInit {
  private readonly fb=inject(FormBuilder); private readonly service=inject(MarketplaceService); private readonly router=inject(Router); private readonly route=inject(ActivatedRoute);
  readonly categories=['ECONOMY','COMPACT','SEDAN','SUV','LUXURY','VAN']; readonly fuels=['GASOLINE','DIESEL','HYBRID','ELECTRIC','LPG']; readonly timeOptions=Array.from({length:96},(_,index)=>`${String(Math.floor(index/4)).padStart(2,'0')}:${String((index%4)*15).padStart(2,'0')}`); readonly today=this.startOfDay(new Date());
  readonly vehicles=signal<MarketplaceVehicle[]>([]); readonly loading=signal(false); readonly error=signal(''); readonly page=signal(0); readonly total=signal(0); readonly totalPages=signal(0);
  readonly form=this.fb.group({pickupDate:[null as Date|null,Validators.required],pickupTime:['10:00',Validators.required],returnDate:[null as Date|null,Validators.required],returnTime:['10:00',Validators.required],location:[''],minDailyPrice:[null as number|null],maxDailyPrice:[null as number|null],minDailyKmLimit:[null as number|null],brand:[''],category:[''],transmission:[''],fuelType:[''],minSeats:[null as number|null],sort:['recommended']});
  ngOnInit():void{const q=this.route.snapshot.queryParamMap;const pickup=this.parseDateTime(q.get('pickupDateTime'),1);const returned=this.parseDateTime(q.get('returnDateTime'),3);this.form.patchValue({pickupDate:pickup.date,pickupTime:pickup.time,returnDate:returned.date,returnTime:returned.time,location:q.get('location')||'',minDailyPrice:this.num(q.get('minDailyPrice')),maxDailyPrice:this.num(q.get('maxDailyPrice')),minDailyKmLimit:this.num(q.get('minDailyKmLimit')),brand:q.get('brand')||'',category:q.get('category')||'',transmission:q.get('transmission')||'',fuelType:q.get('fuelType')||'',minSeats:this.num(q.get('minSeats')),sort:q.get('sort')||'recommended'});this.search(Number(q.get('page')||0));}
  search(page:number):void{if(this.form.invalid){this.form.markAllAsTouched();return;}const criteria=this.buildCriteria(page);if(new Date(criteria.returnDateTime)<=new Date(criteria.pickupDateTime)){this.error.set('Return date and time must be after pickup.');return;}this.loading.set(true);this.error.set('');void this.router.navigate([], {relativeTo:this.route,queryParams:criteria,replaceUrl:true});this.service.search(criteria).subscribe({next:r=>{this.vehicles.set(r.content);this.page.set(r.page);this.total.set(r.totalElements);this.totalPages.set(r.totalPages)},error:e=>{this.error.set(e.error?.message||'Please verify the dates and filters.');this.loading.set(false)},complete:()=>this.loading.set(false)});}
  detailParams():object{return this.buildCriteria(this.page());} money(v:number):string{return formatTryAmount(v)} private buildCriteria(page:number):VehicleSearchCriteria{const value=this.form.getRawValue();return{pickupDateTime:this.combine(value.pickupDate!,value.pickupTime!),returnDateTime:this.combine(value.returnDate!,value.returnTime!),location:value.location||'',minDailyPrice:value.minDailyPrice,maxDailyPrice:value.maxDailyPrice,minDailyKmLimit:value.minDailyKmLimit,brand:value.brand||'',category:value.category||'',transmission:value.transmission||'',fuelType:value.fuelType||'',minSeats:value.minSeats,sort:value.sort||'recommended',page:Math.max(0,page),size:12}}private combine(date:Date,time:string):string{const [hours,minutes]=time.split(':').map(Number);const value=new Date(date);value.setHours(hours,minutes,0,0);const local=new Date(value.getTime()-value.getTimezoneOffset()*60000);return local.toISOString().slice(0,19)}private parseDateTime(value:string|null,days:number):{date:Date;time:string}{const date=value?new Date(value):this.defaultDate(days);return{date:this.startOfDay(date),time:`${String(date.getHours()).padStart(2,'0')}:${String(Math.floor(date.getMinutes()/15)*15).padStart(2,'0')}`}}private defaultDate(days:number):Date{const date=new Date();date.setDate(date.getDate()+days);date.setHours(10,0,0,0);return date}private startOfDay(date:Date):Date{const value=new Date(date);value.setHours(0,0,0,0);return value}private num(v:string|null):number|null{return v?Number(v):null}
}
