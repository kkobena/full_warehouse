package com.kobe.warehouse.domain.enumeration;

/**
 * Où en est l'officine vis-à-vis du palier de remise de fin d'année d'un grossiste.
 *
 * <p>L'état est nommé ici, la phrase se compose au front. Le service calculait auparavant des
 * messages tels que « À 92 % du palier RFA — accélérer les commandes ! » : une formulation
 * destinée au pharmacien n'a pas sa place dans une couche métier, qui ne sait ni dans quelle
 * langue ni dans quel écran elle sera lue.
 *
 * <p>Les bornes ne sont pas décoratives : elles décident quand l'écran conseille d'agir. En
 * dessous du palier mais loin, rien à décider dans la journée ; proche, chaque commande compte ;
 * franchi, la remise est acquise et l'urgence tombe.
 */
public enum EtatPalierRfa {
    /** Aucun palier négocié avec ce grossiste : il n'y a rien à suivre. */
    SANS_ACCORD,
    /** Moins de la moitié du palier : l'échéance annuelle est encore lointaine. */
    LOIN_DU_PALIER,
    /** Entre la moitié et 80 % : l'avancement est normal, rien à signaler. */
    EN_COURS,
    /** À partir de 80 % : c'est là qu'orienter une commande vers ce grossiste peut faire basculer. */
    PROCHE_DU_PALIER,
    /** Palier franchi : la remise est acquise, il n'y a plus de décision à prendre. */
    ATTEINT,
}
