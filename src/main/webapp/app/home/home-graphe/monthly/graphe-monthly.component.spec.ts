import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrapheMonthlyComponent } from './graphe-monthly.component';

describe('GrapheMonthlyComponent', () => {
  let component: GrapheMonthlyComponent;
  let fixture: ComponentFixture<GrapheMonthlyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GrapheMonthlyComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrapheMonthlyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
