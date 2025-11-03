import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DepotHomeComponent } from './depot-home.component';

describe('DepotHomeComponent', () => {
  let component: DepotHomeComponent;
  let fixture: ComponentFixture<DepotHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DepotHomeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DepotHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
