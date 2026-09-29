import {TestBed} from '@angular/core/testing';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {HasAuthorityService} from '../../../../entities/sales/service/has-authority.service';
import {Authority} from '../../../../shared/constants/authority.constants';
import {AuthorizationService} from './authorization.service';

/** Privilège de régularisation du stock à la vente — lot 3 de PLAN-VENTE-SUR-STOCK-ERRONE. */
describe('AuthorizationService.canRegulariserStock', () => {
  let detenus: string[];
  let service: AuthorizationService;

  beforeEach(() => {
    detenus = [];
    TestBed.configureTestingModule({
      providers: [
        {provide: NgbModal, useValue: {}},
        {provide: HasAuthorityService, useValue: {hasAuthorities: (a: string) => detenus.includes(a)}},
      ],
    });
    service = TestBed.inject(AuthorizationService);
  });

  it('porte sur le code du nav_item créé par la migration V2.1.4', () => {
    expect(Authority.PR_REGULARISER_STOCK_VENTE).toBe('pr-regulariser-stock-vente');
  });

  it('est accordé par le privilège dédié', () => {
    detenus = [Authority.PR_REGULARISER_STOCK_VENTE];
    expect(service.canRegulariserStock()).toBe(true);
  });

  it("est accordé à l'administrateur", () => {
    detenus = [Authority.ADMIN];
    expect(service.canRegulariserStock()).toBe(true);
  });

  it("n'est pas accordé par le seul forçage de stock", () => {
    detenus = [Authority.PR_FORCE_STOCK];
    expect(service.canRegulariserStock()).toBe(false);
    expect(service.canForceStock()).toBe(true);
  });
});
