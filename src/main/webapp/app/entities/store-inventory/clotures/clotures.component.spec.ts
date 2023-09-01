import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CloturesComponent } from './clotures.component';

describe('CloturesComponent', () => {
  let component: CloturesComponent;
  let fixture: ComponentFixture<CloturesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CloturesComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CloturesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
