package com.kobe.warehouse.service.declaration_ca.dto;

import java.time.LocalDate;

/**
 * Une vente tiers-payant écartée du chiffre d'affaires à déclarer.
 *
 * <p>Le tiers-payant exclut la vente <strong>entière</strong>, pas certaines de ses lignes : le
 * journal se lit donc par vente, et le détail des lignes n'est chargé qu'à la demande. Afficher
 * d'emblée les lignes d'un mois de ventes assurance noierait le fait marquant — quelles ventes, et
 * pour quel montant.
 *
 * @param tiersPayants intitulés des tiers-payants de la vente, séparés par des virgules : une même
 *     vente peut en porter plusieurs, un seul exclu suffit à l'écarter. Renseigné dans un second
 *     temps, la requête d'agrégation ne pouvant pas concaténer plusieurs lignes filles
 */
public record JournalVenteDTO(
    Long saleId,
    LocalDate saleDate,
    String numeroTransaction,
    String tiersPayants,
    String client,
    Long valeurTtc,
    Long montantExclu,
    Long marge,
    Long nombreLignes
) {
    /**
     * La même vente, avec les intitulés de ses tiers-payants.
     *
     * <p>Une requête d'agrégation par vente ne peut pas concaténer ses lignes filles sans se
     * dupliquer : les intitulés sont donc rapportés par une seconde requête, puis posés ici.
     */
    public JournalVenteDTO avecTiersPayants(String intitules) {
        return new JournalVenteDTO(
            saleId,
            saleDate,
            numeroTransaction,
            intitules,
            client,
            valeurTtc,
            montantExclu,
            marge,
            nombreLignes
        );
    }
}
