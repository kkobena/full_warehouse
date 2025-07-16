import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AjoutPerimesComponent } from './ajout-perimes.component';

describe('AjoutPerimesComponent', () => {
  let component: AjoutPerimesComponent;
  let fixture: ComponentFixture<AjoutPerimesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AjoutPerimesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AjoutPerimesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
