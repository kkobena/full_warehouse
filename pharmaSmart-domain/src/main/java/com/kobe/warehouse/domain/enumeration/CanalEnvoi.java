package com.kobe.warehouse.domain.enumeration;

/**
 * Par quel moyen une commande est partie chez le grossiste.
 *
 * <p>Le canal n'est pas une statistique : c'est lui qui dit où retrouver la trace d'un envoi quand
 * le grossiste prétend n'avoir rien reçu. Une commande transmise par téléphone ne se prouve pas
 * comme un envoi EDI, et l'officine qui ne fait pas d'EDI doit pouvoir tracer les siennes.
 *
 * <p>Aucune contrainte en base ne restreint la colonne {@code canal_envoi} : cette énumération en
 * est le seul garde-fou, délibérément, pour qu'ajouter un canal ne demande pas de reconstruire une
 * contrainte sur une table partitionnée.
 */
public enum CanalEnvoi {
    /** Transmission électronique PharmaML. */
    EDI,
    /** Courrier électronique. */
    MAIL,
    /** Bon imprimé ou fichier remis en main propre. */
    PDF,
    /** Commande passée de vive voix. */
    TELEPHONE,
    /** Canal non précisé — notamment les commandes antérieures à la traçabilité de l'envoi. */
    AUTRE,
}
