package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.LigneStockDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import com.kobe.warehouse.service.dto.pilotage.StockFamilleDTO;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stock, coût des ventes et dormants par famille, rapprochés en une ligne par famille. */
public record StockParFamille(List<StockFamilleDTO> stock, Map<String, Long> couts, Map<String, StockFamilleDTO> dormants, long nbDormants, long valeurDormante) {
    public static StockParFamille lire(List<StockFamilleDTO> stock, List<MontantsFamilleDTO> couts, List<StockFamilleDTO> dormantsLus) {
        Map<String, Long> coutParFamille = new HashMap<>();
        for (MontantsFamilleDTO cout : couts) {
            coutParFamille.put(cout.cle(), cout.montantTtc());
        }
        Map<String, StockFamilleDTO> dormants = new HashMap<>();
        long nombre = 0;
        long valeur = 0;
        for (StockFamilleDTO dormant : dormantsLus) {
            dormants.put(dormant.cle(), dormant);
            nombre += dormant.quantite();
            valeur += dormant.valeur();
        }
        return new StockParFamille(stock, coutParFamille, dormants, nombre, valeur);
    }

    public List<LigneStockDTO> lignes(long valeurTotale) {
        return stock
            .stream()
            .sorted(Comparator.comparingLong(StockFamilleDTO::valeur).reversed())
            .map(famille -> {
                long cout = couts.getOrDefault(famille.cle(), 0L);
                StockFamilleDTO dormant = dormants.get(famille.cle());
                return new LigneStockDTO(
                    famille.cle(),
                    famille.libelle(),
                    famille.valeur(),
                    Variations.part((double) famille.valeur(), (double) valeurTotale),
                    cout,
                    IndicateursStock.rotation(cout, famille.valeur()),
                    IndicateursStock.couverture(famille.valeur(), cout),
                    dormant == null ? 0 : dormant.valeur()
                );
            })
            .toList();
    }
}
