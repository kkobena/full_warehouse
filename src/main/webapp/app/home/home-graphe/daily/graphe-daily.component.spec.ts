import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrapheDailyComponent } from './graphe-daily.component';

describe('GrapheDailyComponent', () => {
  let component: GrapheDailyComponent;
  let fixture: ComponentFixture<GrapheDailyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GrapheDailyComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrapheDailyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
