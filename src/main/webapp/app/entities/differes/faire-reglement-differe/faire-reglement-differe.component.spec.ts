import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FaireReglementDiffereComponent } from './faire-reglement-differe.component';

describe('FaireReglementDiffereComponent', () => {
  let component: FaireReglementDiffereComponent;
  let fixture: ComponentFixture<FaireReglementDiffereComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FaireReglementDiffereComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(FaireReglementDiffereComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
