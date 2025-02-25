import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaportActiviteGlobalComponent } from './raport-activite-global.component';

describe('RaportActiviteGlobalComponent', () => {
  let component: RaportActiviteGlobalComponent;
  let fixture: ComponentFixture<RaportActiviteGlobalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RaportActiviteGlobalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RaportActiviteGlobalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
