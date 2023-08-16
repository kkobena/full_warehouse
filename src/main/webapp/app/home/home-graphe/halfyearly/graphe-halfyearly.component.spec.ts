import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrapheHalfyearlyComponent } from './graphe-halfyearly.component';

describe('GrapheHalfyearlyComponent', () => {
  let component: GrapheHalfyearlyComponent;
  let fixture: ComponentFixture<GrapheHalfyearlyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GrapheHalfyearlyComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GrapheHalfyearlyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
