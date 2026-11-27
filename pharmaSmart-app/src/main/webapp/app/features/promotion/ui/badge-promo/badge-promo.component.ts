import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { DevisePipe } from 'app/shared/utils/devise';
import { IPromotionArticle } from '../../models/promotion.model';
import { decrirePromotion } from '../../data-access/services/promotions-en-vigueur.service';

/** Pastille « PROMO » d'un article en promotion aujourd'hui ; l'infobulle dit laquelle, et à quel prix. */
@Component({
  selector: 'app-badge-promo',
  template: `<span [ngbTooltip]="description()" [attr.aria-label]="'En promotion : ' + description()" class="badge-promo" container="body"
    >PROMO</span
  >`,
  styles: `
    .badge-promo {
      font-size: 0.6rem;
      font-weight: 700;
      padding: 1px 4px;
      border-radius: 3px;
      background: #ffedd5;
      color: #c2410c;
      border: 1px solid #fed7aa;
      white-space: nowrap;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgbTooltip],
})
export class BadgePromoComponent {
  readonly article = input.required<IPromotionArticle>();

  protected readonly description = computed(() => {
    const devise = new DevisePipe();
    return decrirePromotion(this.article(), v => devise.transform(v));
  });
}
