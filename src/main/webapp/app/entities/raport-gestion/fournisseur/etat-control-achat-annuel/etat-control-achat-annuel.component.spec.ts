import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtatControlAchatAnnuelComponent } from './etat-control-achat-annuel.component';

describe('EtatControlAchatAnnuelComponent', () => {
  let component: EtatControlAchatAnnuelComponent;
  let fixture: ComponentFixture<EtatControlAchatAnnuelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtatControlAchatAnnuelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EtatControlAchatAnnuelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
