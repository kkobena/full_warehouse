import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { StockDepotComponent } from './stock-depot.component';

describe('StockDepotComponent', () => {
  let component: StockDepotComponent;
  let fixture: ComponentFixture<StockDepotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockDepotComponent],
      // L'écran lit la route (filtres, pagination), interroge les dépôts dès sa construction,
      // et traduit ses messages d'erreur.
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(StockDepotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
