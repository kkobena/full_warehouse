import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReglementDifferesComponent } from './reglement-differes.component';

describe('ReglementDifferesComponent', () => {
  let component: ReglementDifferesComponent;
  let fixture: ComponentFixture<ReglementDifferesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReglementDifferesComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ReglementDifferesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
