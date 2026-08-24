package com.kobe.warehouse.service.declaration_ca.dto;

import java.util.List;

/**
 * Résultat du contrôle d'un invariant du chiffre d'affaires déclaré.
 *
 * <p>Le nombre seul ne suffit pas : « 12 lignes incohérentes » n'aide personne à corriger. Chaque
 * anomalie porte donc quelques exemples, assez pour retrouver les ventes concernées.
 *
 * @param code identifiant stable de l'invariant (V1, V2, …), cité dans le plan
 * @param libelle ce que l'invariant garantit, formulé pour un lecteur non technique
 * @param consequence ce qui se passe si l'invariant est rompu — sans quoi le contrôle n'est qu'un
 *     nombre de plus
 * @param nombreAnomalies violations détectées ; {@code 0} = invariant tenu
 * @param exemples quelques cas concrets, au plus dix
 */
public record AnomalieDTO(String code, String libelle, String consequence, long nombreAnomalies, List<String> exemples) {
    public boolean estSain() {
        return nombreAnomalies == 0;
    }
}
