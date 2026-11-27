import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { INavNode } from 'app/shared/model/nav-item.model';
import { PromotionLayoutComponent } from './promotion-layout.component';

const PERMISSIONS = {
  canDisplay: true,
  canAccess: true,
  canCreate: true,
  canEdit: true,
  canDelete: true,
  canExport: true,
  canExecute: true,
};

const SECTIONS = [
  { code: 'promotions.liste', libelle: 'Promotions', icon: 'pi pi-tags' },
  { code: 'promotions.journal', libelle: 'Journal', icon: 'pi pi-list' },
];

describe('Promotions — onglets', () => {
  let fixture: ComponentFixture<PromotionLayoutComponent>;

  function creer(sections: typeof SECTIONS): HTMLElement {
    const nav: INavNode[] = [
      {
        id: 1,
        code: 'promotions',
        libelle: 'Promotions',
        targetType: 'ROUTE',
        ordre: 7,
        children: sections.map((s, i) => ({
          id: i + 2,
          ...s,
          targetType: 'SECTION' as const,
          ordre: (i + 1) * 10,
          permissions: PERMISSIONS,
        })),
        permissions: PERMISSIONS,
      },
    ];
    TestBed.inject(AbilityService).setFromNavTree(nav);
    TestBed.inject(NavStore).navTree.set(nav);
    fixture = TestBed.createComponent(PromotionLayoutComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromotionLayoutComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateService(), AbilityService],
    }).compileComponents();
  });

  it('ouvre sur la liste, suivie du journal', () => {
    const element = creer(SECTIONS);
    const liens = Array.from(element.querySelectorAll('.pharma-nav-vertical-link')).map(a => a.textContent?.replace('›', '').trim());
    expect(liens).toEqual(['Promotions', 'Journal']);
    expect(element.querySelector('app-promotion-home')).not.toBeNull();
  });

  it("n'affiche pas le journal sans le droit", () => {
    const element = creer(SECTIONS.slice(0, 1));
    expect(element.querySelectorAll('.pharma-nav-vertical-link').length).toBe(1);
  });
});
