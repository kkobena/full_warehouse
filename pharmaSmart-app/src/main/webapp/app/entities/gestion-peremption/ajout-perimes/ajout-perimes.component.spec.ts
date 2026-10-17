import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { AjoutPerimesComponent } from './ajout-perimes.component';

describe('AjoutPerimesComponent', () => {
  let component: AjoutPerimesComponent;
  let fixture: ComponentFixture<AjoutPerimesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AjoutPerimesComponent],
      // ErrorService traduit ses messages : sans TranslateService, l'écran ne se construit pas.
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(AjoutPerimesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
