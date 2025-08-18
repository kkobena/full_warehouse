import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionDifferesComponent } from './gestion-differes.component';

describe('GestionDifferesComponent', () => {
  let component: GestionDifferesComponent;
  let fixture: ComponentFixture<GestionDifferesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionDifferesComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GestionDifferesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
