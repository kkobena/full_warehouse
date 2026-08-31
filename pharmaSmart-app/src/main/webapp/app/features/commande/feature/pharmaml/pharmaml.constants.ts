/**
 * Interrogation de disponibilité chez le grossiste : désactivée.
 *
 * La norme PharmaML prévoit une consultation de disponibilité, mais son schéma n'est pas
 * public (le CSRP en est le seul dépositaire) et l'implémentation en face — GESCOM 3.41.06
 * chez Laborex-CI — ne la connaît pas :
 *
 * - nature d'action `REQ_INFORMATION` → HTTP 400,
 *   « Value 'REQ_INFORMATION' is not facet-valid with respect to enumeration
 *   [REQ_EMISSION, REQ_RECEPTION, REP_EMISSION, REP_RECEPTION] » ;
 * - corps `REQ_INFOS` → `<ERREUR Statut="2">`, contenu invalide.
 *
 * Le seul message qui aboutit est une COMMANDE : chez ce répartiteur, « vérifier la
 * disponibilité » revient à **passer commande** et à lire les `Quantite_livree` de la
 * réponse. Un écran qui promet une consultation ne peut pas faire ça dans le dos de
 * l'utilisateur : les points d'entrée sont donc masqués tant qu'aucun grossiste ne
 * répond à une vraie demande d'information.
 *
 * Le code de la comparaison (`DispoComparaisonComponent`, `PharmaMlService.demanderDisponibiliteMulti`)
 * reste en place : passer ce drapeau à `true` le remet à l'écran.
 */
export const COMPARAISON_DISPONIBILITE_ACTIVE = false;
