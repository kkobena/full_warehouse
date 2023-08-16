import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrapheYearlyComponent } from './graphe-yearly.component';

describe('GrapheYearlyComponent', () => {
  let component: GrapheYearlyComponent;
  let fixture: ComponentFixture<GrapheYearlyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GrapheYearlyComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrapheYearlyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
