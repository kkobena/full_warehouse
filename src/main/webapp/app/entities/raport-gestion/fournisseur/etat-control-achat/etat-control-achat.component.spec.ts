import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtatControlAchatComponent } from './etat-control-achat.component';

describe('EtatControlAchatComponent', () => {
  let component: EtatControlAchatComponent;
  let fixture: ComponentFixture<EtatControlAchatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtatControlAchatComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(EtatControlAchatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
