package com.kobe.warehouse.service.financiel_transaction.dto;

/**
 * Lequel des deux chiffres d'affaires un rapport doit renvoyer.
 *
 * <p>Les deux se lisent sur les mêmes écrans et sortent des mêmes fonctions SQL, distinguées par le
 * seul paramètre {@code p_mode} : dupliquer les fonctions, les services et les composants aurait
 * produit deux implémentations à maintenir en parallèle, qui divergeraient à la première évolution.
 */
public enum ModeChiffreAffaire {
    /**
     * Le chiffre d'affaires encaissé, sans aucun retraitement — unités gratuites comprises, puisque
     * c'est ce qui est réellement passé en caisse.
     */
    REEL,

    /**
     * Le chiffre d'affaires à déclarer à la comptabilité, une fois appliquées les exclusions et
     * l'éventuelle ponction.
     */
    DECLARE;

    /**
     * Mode retenu quand l'appelant n'en impose pas.
     *
     * <p><strong>{@code REEL}</strong>, parce que c'est l'état neutre : la donnée non retraitée. Un
     * appelant qui oublie de préciser le mode obtient le chiffre encaissé, jamais un chiffre
     * diminué — une sous-déclaration silencieuse ne se remarque pas, alors qu'un montant trop élevé
     * saute aux yeux. Le mode déclaré se demande explicitement, là où l'intention métier existe.
     */
    public static final ModeChiffreAffaire DEFAUT = REEL;
}
