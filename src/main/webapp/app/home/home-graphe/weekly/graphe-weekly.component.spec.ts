import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrapheWeeklyComponent } from './graphe-weekly.component';

describe('GrapheWeeklyComponent', () => {
  let component: GrapheWeeklyComponent;
  let fixture: ComponentFixture<GrapheWeeklyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GrapheWeeklyComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrapheWeeklyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
