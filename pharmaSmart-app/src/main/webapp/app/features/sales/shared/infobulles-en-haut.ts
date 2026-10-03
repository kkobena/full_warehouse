import { Provider } from '@angular/core';
import { NgbTooltipConfig } from '@ng-bootstrap/ng-bootstrap';

/**
 * Position par défaut des `ngbTooltip` de l'espace de travail : au-dessus du bouton, sinon dessous, puis sur les côtés
 * quand la place manque (le bandeau est collé à la barre de navigation). Une infobulle qui fixe son `placement` garde
 * le sien. Attachées au `body` pour ne pas être rognées. Fourni au niveau du composant racine de l'écran : il vaut pour tous ses descendants, routés compris.
 */
export function fournirInfobullesEnHaut(): Provider {
  return {
    provide: NgbTooltipConfig,
    useFactory: (): NgbTooltipConfig => {
      const config = new NgbTooltipConfig();
      config.placement = ['top', 'bottom', 'start', 'end'];
      // Dans le `body` : insérée à côté de son bouton, l'infobulle est rognée par les cartes à `overflow: hidden`
      // (carte « Assuré », panneaux), surtout quand elle s'ouvre au-dessus.
      config.container = 'body';
      return config;
    },
  };
}
