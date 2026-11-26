import { computed, Signal } from '@angular/core';
import { isActive, IsActiveMatchOptions, Router } from '@angular/router';
import { NavItem } from '../navbar/navbar-item.model';

/**
 * Un lien est actif quand l'URL courante est sa page ou l'une de ses sous-pages (`/commande`
 * reste actif sur `/commande/42`). Les paramètres de requête sont ignorés : un menu reste
 * actif quel que soit le filtre ou l'onglet passé dans l'URL.
 */
const SOUS_ARBRE: IsActiveMatchOptions = { paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' };

/** La racine `/` contient toutes les URL : en sous-arbre, son entrée serait toujours active. */
const EXACT: IsActiveMatchOptions = { paths: 'exact', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' };

function liensDe(item: NavItem): string[] {
  const enfants = (item.children ?? []).flatMap(liensDe);
  return item.routerLink ? [item.routerLink, ...enfants] : enfants;
}

/**
 * Identifiants des entrées de premier niveau actives : celles dont la page, ou la page de
 * l'un des sous-menus, est la page courante. Sert à marquer le menu parent dans la barre
 * horizontale et dans la barre latérale, pour qu'on sache sous quel menu on se trouve.
 */
export function menusActifs(router: Router, items: Signal<NavItem[]>): Signal<ReadonlySet<string>> {
  const signaux = computed(() =>
    items().map(item => ({
      id: item.id,
      actifs: liensDe(item).map(lien => {
        const arbre = router.parseUrl(lien);
        return isActive(arbre, router, arbre.root.hasChildren() ? SOUS_ARBRE : EXACT);
      }),
    })),
  );
  return computed(
    () =>
      new Set(
        signaux()
          .filter(e => e.actifs.some(actif => actif()))
          .map(e => e.id),
      ),
  );
}
