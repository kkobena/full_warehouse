import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecapitualtifCaisseComponent } from './recapitualtif-caisse.component';

describe('RecapitualtifCaisseComponent', () => {
  let component: RecapitualtifCaisseComponent;
  let fixture: ComponentFixture<RecapitualtifCaisseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecapitualtifCaisseComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RecapitualtifCaisseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
