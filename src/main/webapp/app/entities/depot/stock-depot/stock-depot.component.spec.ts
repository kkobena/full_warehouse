import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockDepotComponent } from './stock-depot.component';

describe('StockDepotComponent', () => {
  let component: StockDepotComponent;
  let fixture: ComponentFixture<StockDepotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockDepotComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StockDepotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
