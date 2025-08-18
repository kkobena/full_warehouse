import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivitySummaryComponent } from './activity-summary.component';

describe('ActivitySummaryComponent', () => {
  let component: ActivitySummaryComponent;
  let fixture: ComponentFixture<ActivitySummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivitySummaryComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ActivitySummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
