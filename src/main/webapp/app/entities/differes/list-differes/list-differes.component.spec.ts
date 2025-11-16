import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListDifferesComponent } from './list-differes.component';

describe('ListDifferesComponent', () => {
  let component: ListDifferesComponent;
  let fixture: ComponentFixture<ListDifferesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListDifferesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ListDifferesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
