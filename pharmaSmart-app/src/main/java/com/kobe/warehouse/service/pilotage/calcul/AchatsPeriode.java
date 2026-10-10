package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.DelaiFournisseurDTO;
import com.kobe.warehouse.service.dto.pilotage.FournisseurAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** AchatsPeriode d'une période : par fournisseur (bons, quantités), délais par fournisseur, montants par famille, et leurs totaux. */
public record AchatsPeriode(
    List<FournisseurAchatsDTO> fournisseurs,
    Map<String, DelaiFournisseurDTO> delais,
    List<MontantsFamilleDTO> familles,
    long totalTtc,
    long totalHt,
    long nbBons,
    Double delaiMoyen,
    Double conformite
) {
    private static final double CENT = 100.0;

    /** Un passage par liste lue : les totaux s'accumulent en rangeant les lignes. */
    public static AchatsPeriode lire(List<FournisseurAchatsDTO> fournisseurs, List<DelaiFournisseurDTO> delaisLus, List<MontantsFamilleDTO> familles) {
        long nbBons = 0;
        long commandees = 0;
        long recues = 0;
        for (FournisseurAchatsDTO fournisseur : fournisseurs) {
            nbBons += fournisseur.nbBons();
            commandees += fournisseur.quantiteCommandee();
            recues += fournisseur.quantiteRecue();
        }
        Map<String, DelaiFournisseurDTO> delais = new LinkedHashMap<>();
        long commandes = 0;
        double jours = 0;
        for (DelaiFournisseurDTO delai : delaisLus) {
            delais.put(delai.cle(), delai);
            commandes += delai.nombre();
            jours += delai.delaiMoyen() == null ? 0 : delai.delaiMoyen() * delai.nombre();
        }
        long totalTtc = 0;
        long totalHt = 0;
        for (MontantsFamilleDTO famille : familles) {
            totalTtc += famille.montantTtc();
            totalHt += famille.montantHt();
        }
        return new AchatsPeriode(
            fournisseurs,
            delais,
            familles,
            totalTtc,
            totalHt,
            nbBons,
            commandes == 0 ? null : jours / commandes,
            commandees == 0 ? null : Math.min(CENT, recues * CENT / commandees)
        );
    }

    public List<LigneAchatsDTO> lignesFournisseurs(AchatsPeriode reference) {
        Map<String, Long> montantsReference = reference == null
            ? Map.of()
            : reference.fournisseurs.stream().collect(Collectors.toMap(FournisseurAchatsDTO::cle, FournisseurAchatsDTO::montantTtc));
        return fournisseurs
            .stream()
            .sorted(Comparator.comparingLong(FournisseurAchatsDTO::montantTtc).reversed())
            .map(fournisseur -> {
                DelaiFournisseurDTO delai = delais.get(fournisseur.cle());
                Long montantReference = reference == null ? null : montantsReference.getOrDefault(fournisseur.cle(), 0L);
                return new LigneAchatsDTO(
                    fournisseur.cle(),
                    fournisseur.libelle(),
                    Variations.cellule(UniteIndicateur.MONTANT, (double) fournisseur.montantTtc(), montantReference == null ? null : (double) montantReference),
                    Variations.part((double) fournisseur.montantTtc(), (double) totalTtc),
                    fournisseur.nbBons(),
                    delai == null ? null : delai.delaiMoyen(),
                    fournisseur.quantiteCommandee() == 0 ? null : Math.min(CENT, fournisseur.quantiteRecue() * CENT / fournisseur.quantiteCommandee())
                );
            })
            .toList();
    }

    public List<LigneAchatsDTO> lignesFamilles(AchatsPeriode reference) {
        Map<String, Long> montantsReference = reference == null
            ? Map.of()
            : reference.familles.stream().collect(Collectors.toMap(MontantsFamilleDTO::cle, MontantsFamilleDTO::montantTtc));
        return familles
            .stream()
            .sorted(Comparator.comparingLong(MontantsFamilleDTO::montantTtc).reversed())
            .map(famille -> {
                Long montantReference = reference == null ? null : montantsReference.getOrDefault(famille.cle(), 0L);
                return new LigneAchatsDTO(
                    famille.cle(),
                    famille.libelle(),
                    Variations.cellule(UniteIndicateur.MONTANT, (double) famille.montantTtc(), montantReference == null ? null : (double) montantReference),
                    Variations.part((double) famille.montantTtc(), (double) totalTtc),
                    null,
                    null,
                    null
                );
            })
            .toList();
    }
}
