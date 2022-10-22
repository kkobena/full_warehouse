import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormeProduitComponent } from './forme-produit.component';

describe('FormeProduitComponent', () => {
  let component: FormeProduitComponent;
  let fixture: ComponentFixture<FormeProduitComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [FormeProduitComponent],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormeProduitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
