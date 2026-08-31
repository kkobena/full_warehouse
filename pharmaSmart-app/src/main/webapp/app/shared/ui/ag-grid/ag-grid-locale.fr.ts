/**
 * Libellés français d'AG Grid.
 *
 * AG Grid embarque ses messages en anglais et n'offre aucune traduction par défaut : sans ce
 * dictionnaire, une application entièrement française affiche « No Rows To Show » au milieu
 * d'un tableau vide, « Page 1 of 3 » sous la pagination et des menus de filtre en anglais.
 * Le défaut se voit d'autant plus qu'il est ponctuel — quelques mots anglais au milieu du
 * français passent pour un écran inachevé.
 *
 * Seules les clés réellement rencontrées dans l'application sont traduites : la liste
 * complète compte plusieurs centaines d'entrées dont l'immense majorité concerne des
 * fonctions non utilisées ici (regroupement, tableaux croisés, graphiques intégrés).
 *
 * Usage : `<ag-grid-angular [localeText]="AG_GRID_LOCALE_FR" …>`.
 */
export const AG_GRID_LOCALE_FR: Record<string, string> = {
  // État vide et chargement
  noRowsToShow: 'Aucune ligne à afficher',
  loadingOoo: 'Chargement…',

  // Pagination
  page: 'Page',
  to: 'à',
  of: 'sur',
  more: 'plus',
  nextPage: 'Page suivante',
  lastPage: 'Dernière page',
  firstPage: 'Première page',
  previousPage: 'Page précédente',
  pageSizeSelectorLabel: 'Lignes par page :',
  paginationPageSize: 'Lignes par page',

  // En-têtes de colonne et menu
  columns: 'Colonnes',
  filters: 'Filtres',
  pinColumn: 'Épingler la colonne',
  pinLeft: 'Épingler à gauche',
  pinRight: 'Épingler à droite',
  noPin: 'Ne pas épingler',
  autosizeThisColumn: 'Ajuster cette colonne',
  autosizeAllColumns: 'Ajuster toutes les colonnes',
  resetColumns: 'Réinitialiser les colonnes',
  sortAscending: 'Trier par ordre croissant',
  sortDescending: 'Trier par ordre décroissant',
  sortUnSort: 'Annuler le tri',

  // Filtres
  searchOoo: 'Rechercher…',
  blanks: '(Vides)',
  filterOoo: 'Filtrer…',
  applyFilter: 'Appliquer',
  resetFilter: 'Réinitialiser',
  clearFilter: 'Effacer',
  cancelFilter: 'Annuler',
  equals: 'Égal à',
  notEqual: 'Différent de',
  lessThan: 'Inférieur à',
  greaterThan: 'Supérieur à',
  lessThanOrEqual: 'Inférieur ou égal à',
  greaterThanOrEqual: 'Supérieur ou égal à',
  inRange: 'Compris entre',
  inRangeStart: 'De',
  inRangeEnd: 'À',
  contains: 'Contient',
  notContains: 'Ne contient pas',
  startsWith: 'Commence par',
  endsWith: 'Se termine par',
  andCondition: 'ET',
  orCondition: 'OU',
  selectAll: '(Tout sélectionner)',

  // Sélection et copie
  copy: 'Copier',
  copyWithHeaders: 'Copier avec les en-têtes',
  paste: 'Coller',
  export: 'Exporter',
  csvExport: 'Export CSV',
  excelExport: 'Export Excel',
};
