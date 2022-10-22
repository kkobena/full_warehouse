import { TestBed } from '@angular/core/testing';

import { FormeProduitService } from './forme-produit.service';

describe('FormeProduitService', () => {
  let service: FormeProduitService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FormeProduitService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
