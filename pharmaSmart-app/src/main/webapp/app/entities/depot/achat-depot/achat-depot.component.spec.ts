import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AchatDepotComponent } from './achat-depot.component';

describe('AchatDepotComponent', () => {
  let component: AchatDepotComponent;
  let fixture: ComponentFixture<AchatDepotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AchatDepotComponent],
      // Dépôts, utilisateurs et ventes sont chargés à l'initialisation ; la route vient des
      // composants d'écran projetés.
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AchatDepotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
