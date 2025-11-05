import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VenteDepotTableComponent } from './vente-depot-table.component';

describe('VenteDepotTableComponent', () => {
  let component: VenteDepotTableComponent;
  let fixture: ComponentFixture<VenteDepotTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VenteDepotTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VenteDepotTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
