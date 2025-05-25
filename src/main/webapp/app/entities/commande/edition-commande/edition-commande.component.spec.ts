import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditionCommandeComponent } from './edition-commande.component';

describe('EditionCommandeComponent', () => {
  let component: EditionCommandeComponent;
  let fixture: ComponentFixture<EditionCommandeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditionCommandeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditionCommandeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
