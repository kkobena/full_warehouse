import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AchatDepotComponent } from './achat-depot.component';

describe('AchatDepotComponent', () => {
  let component: AchatDepotComponent;
  let fixture: ComponentFixture<AchatDepotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AchatDepotComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AchatDepotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
