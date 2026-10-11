package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneRemiseDTO;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.RentabiliteRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.VariationRapportDTO;
import com.kobe.warehouse.service.pilotage.DemarquePilotageService;
import com.kobe.warehouse.service.pilotage.MargePilotageService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.RapportRentabiliteService;
import com.kobe.warehouse.service.pilotage.RemisesPilotageService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.FormatRapport;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import com.kobe.warehouse.service.pilotage.calcul.TableauxRapport;
import com.kobe.warehouse.service.pilotage.calcul.TuilesRapport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportRentabiliteServiceImpl implements RapportRentabiliteService {

    private static final int LIGNES_MAXIMUM = 30;
    private static final List<IndicateurPilotage> INDICATEURS_MARGE = List.of(
        IndicateurPilotage.MARGE_BRUTE,
        IndicateurPilotage.TAUX_MARGE,
        IndicateurPilotage.COEFFICIENT_MOYEN,
        IndicateurPilotage.MARGE_PAR_VENTE
    );
    /** L'axe par défaut de l'écran ; les autres ventilations sont celles de l'onglet Analyser. */
    private static final AxeAnalyse AXE_MARGE = AxeAnalyse.FAMILLE;
    private static final List<String> COLONNES_MARGE = List.of("CA HT", "Coût HT", "Marge", "Taux", "Effet volume", "Effet taux");
    private static final List<String> COLONNES_VENTES_MARGE = List.of("Vente", "Vendeur", "CA HT", "Coût HT", "Marge");
    private static final List<String> COLONNES_REMISES = List.of("Ventes", "CA TTC", "Remises", "Taux de remise");
    private static final List<String> COLONNES_FORTES_REMISES = List.of("Vente", "Vendeur", "Montant", "Remise", "Taux", "Autorisée par");
    private static final List<String> COLONNES_DEMARQUE = List.of("Quantité", "Valeur");

    private final SeriesPilotageService seriesPilotageService;
    private final MargePilotageService margePilotageService;
    private final RemisesPilotageService remisesPilotageService;
    private final DemarquePilotageService demarquePilotageService;
    private final RapportCourbesService rapportCourbesService;

    public RapportRentabiliteServiceImpl(
        SeriesPilotageService seriesPilotageService,
        MargePilotageService margePilotageService,
        RemisesPilotageService remisesPilotageService,
        DemarquePilotageService demarquePilotageService,
        RapportCourbesService rapportCourbesService
    ) {
        this.seriesPilotageService = seriesPilotageService;
        this.margePilotageService = margePilotageService;
        this.remisesPilotageService = remisesPilotageService;
        this.demarquePilotageService = demarquePilotageService;
        this.rapportCourbesService = rapportCourbesService;
    }

    @Override
    public RentabiliteRapportDTO construireRentabilite(RequetePilotageDTO requete) {
        List<SerieIndicateurDTO> seriesMarge = seriesPilotageService.calculerSeries(requete.lireAvecIndicateurs(INDICATEURS_MARGE)).series();
        MargeRentabiliteDTO marge = margePilotageService.analyserMarge(requete, AXE_MARGE);
        RemisesRentabiliteDTO remises = remisesPilotageService.analyserRemises(requete);
        DemarqueRentabiliteDTO demarque = demarquePilotageService.analyserDemarque(requete);
        String horizon = HorizonCourbe.libellerHorizon(requete.granularite());
        return new RentabiliteRapportDTO(
            seriesMarge.stream().map(TuilesRapport::lireTuile).toList(),
            lireLectureMix(marge),
            "Taux de marge — " + horizon,
            rapportCourbesService.dessinerHistorique(requete, IndicateurPilotage.TAUX_MARGE, false, FormatGraphe.MOITIE),
            "Faibles marges (sous " + marge.seuilFaibleMarge() + " %)",
            TableauxRapport.limiter(construireFaiblesMarges(marge.faiblesMarges()), LIGNES_MAXIMUM),
            TableauxRapport.limiter(construireMarges(marge), LIGNES_MAXIMUM),
            TableauxRapport.limiter(construireVentesAMargeNegative(marge.ventesAMargeNegative()), LIGNES_MAXIMUM),
            lireTuilesRemises(remises),
            "Remises accordées — " + horizon,
            rapportCourbesService.dessinerHistorique(requete, IndicateurPilotage.REMISES, false, FormatGraphe.MOITIE),
            construireRemises("Octroi", remises.octrois(), null),
            construireRemises("Tranche", remises.tranches(), null),
            remises.vendeurs() == null ? null : construireRemises("Vendeur", remises.vendeurs(), remises.multipleAlerte()),
            TableauxRapport.limiter(construirePlusFortesRemises(remises), LIGNES_MAXIMUM),
            lireTuilesDemarque(demarque),
            construireDemarqueParMotif(demarque.parMotif()),
            TableauxRapport.limiter(construireProduitsDemarques(demarque.produits()), LIGNES_MAXIMUM)
        );
    }

    /** « Le taux de marge recule de 0,6 pt : - 0,4 pt dû au mix (…), - 0,2 pt au taux de chacun. » */
    private static String lireLectureMix(MargeRentabiliteDTO marge) {
        if (marge.effetMix() == null || marge.effetTaux() == null) {
            return null;
        }
        double variation = marge.effetMix() + marge.effetTaux();
        return (
            "Le taux de marge " +
            (variation >= 0 ? "progresse" : "recule") +
            " de " +
            FormatRapport.formaterPoints(variation) +
            " pt : " +
            libellerPoints(marge.effetMix()) +
            " dû au mix (le poids de chaque " +
            marge.axe().libelle().toLowerCase() +
            " dans le CA), " +
            libellerPoints(marge.effetTaux()) +
            " au taux de chacun."
        );
    }

    private static String libellerPoints(double points) {
        return FormatRapport.formaterVariation(points, null, UniteIndicateur.POURCENTAGE, SensFavorable.NEUTRE).texte();
    }

    private static TableauRapportDTO construireFaiblesMarges(List<LigneMargeDTO> faiblesMarges) {
        List<LigneRapportDTO> lignes = new ArrayList<>(faiblesMarges.size());
        for (LigneMargeDTO ligne : faiblesMarges) {
            lignes.add(
                new LigneRapportDTO(ligne.libelle(), List.of(TableauxRapport.lireValeur(ligne.caHt(), UniteIndicateur.MONTANT), lireTaux(ligne.taux(), null)))
            );
        }
        return new TableauRapportDTO("Produit", List.of("CA HT", "Taux de marge"), lignes);
    }

    /** Taux et, s'il y a une référence, son écart en points ; effets volume et taux en montants. */
    private static TableauRapportDTO construireMarges(MargeRentabiliteDTO marge) {
        List<LigneRapportDTO> lignes = new ArrayList<>(marge.lignes().size());
        for (LigneMargeDTO ligne : marge.lignes()) {
            VariationRapportDTO ecartTaux = ligne.tauxReference() == null
                ? null
                : FormatRapport.formaterVariation(ligne.ecartTaux(), null, UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE);
            lignes.add(
                new LigneRapportDTO(
                    ligne.libelle(),
                    List.of(
                        TableauxRapport.lireValeur(ligne.caHt(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(ligne.coutHt(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(ligne.marge(), UniteIndicateur.MONTANT),
                        lireTaux(ligne.taux(), ecartTaux),
                        TableauxRapport.lireValeur(ligne.effetVolume(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(ligne.effetTaux(), UniteIndicateur.MONTANT)
                    )
                )
            );
        }
        return new TableauRapportDTO(marge.axe().libelle(), COLONNES_MARGE, lignes);
    }

    private static TableauRapportDTO construireVentesAMargeNegative(List<VenteMargeDTO> ventes) {
        List<LigneRapportDTO> lignes = new ArrayList<>(ventes.size());
        for (VenteMargeDTO vente : ventes) {
            lignes.add(
                new LigneRapportDTO(
                    FormatRapport.formaterDate(vente.saleDate()),
                    List.of(
                        CelluleRapportDTO.lireTexte(vente.numero()),
                        CelluleRapportDTO.lireTexte(vente.vendeur()),
                        TableauxRapport.lireValeur(vente.caHt(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(vente.coutHt(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(vente.marge(), UniteIndicateur.MONTANT)
                    )
                )
            );
        }
        return new TableauRapportDTO("Date", COLONNES_VENTES_MARGE, lignes);
    }

    private static List<TuileRapportDTO> lireTuilesRemises(RemisesRentabiliteDTO remises) {
        return List.of(
            TuilesRapport.lireTuile("Remises accordées", remises.remises(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Taux de remise", remises.tauxRemise(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Ventes remisées", remises.partVentesRemisees(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Remise moyenne", remises.remiseMoyenne(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("Poids dans la marge", remises.poidsRemisesMarge(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE)
        );
    }

    /**
     * Pendant d'{@code app-tableau-remises} : la colonne du taux de marge n'apparaît que si une ligne le connaît, l'alerte que
     * pour les vendeurs ({@code multipleAlerte} non nul).
     */
    private static TableauRapportDTO construireRemises(String entete, List<LigneRemiseDTO> remises, Double multipleAlerte) {
        boolean avecMarge = remises.stream().anyMatch(ligne -> ligne.tauxMarge() != null);
        List<String> colonnes = new ArrayList<>(COLONNES_REMISES);
        if (avecMarge) {
            colonnes.add("Taux de marge");
        }
        if (multipleAlerte != null) {
            colonnes.add("Alerte");
        }
        List<LigneRapportDTO> lignes = new ArrayList<>(remises.size());
        for (LigneRemiseDTO ligne : remises) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(colonnes.size());
            cellules.add(TableauxRapport.lireCellule(ligne.nbVentes(), UniteIndicateur.NOMBRE, SensFavorable.HAUSSE));
            cellules.add(TableauxRapport.lireCellule(ligne.caTtc(), UniteIndicateur.MONTANT, SensFavorable.HAUSSE));
            cellules.add(TableauxRapport.lireCellule(ligne.remises(), UniteIndicateur.MONTANT, SensFavorable.BAISSE));
            cellules.add(TableauxRapport.lireCellule(ligne.tauxRemise(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE));
            if (avecMarge) {
                cellules.add(TableauxRapport.lireCellule(ligne.tauxMarge(), UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE));
            }
            if (multipleAlerte != null) {
                cellules.add(CelluleRapportDTO.lireTexte(ligne.alerte() ? "Plus de " + libellerMultiple(multipleAlerte) + " fois le taux de l'équipe" : ""));
            }
            lignes.add(new LigneRapportDTO(ligne.libelle(), cellules));
        }
        return new TableauRapportDTO(entete, colonnes, lignes);
    }

    /** Un taux au-delà du multiple d'alerte du taux moyen des ventes remisées est signalé, comme à l'écran. */
    private static TableauRapportDTO construirePlusFortesRemises(RemisesRentabiliteDTO remises) {
        Double tauxMoyen = lireTauxDesVentesRemisees(remises.octrois());
        double multiple = remises.multipleAlerte();
        List<LigneRapportDTO> lignes = new ArrayList<>(remises.plusFortesRemises().size());
        for (VenteRemiseeDTO vente : remises.plusFortesRemises()) {
            boolean excessif = vente.taux() != null && tauxMoyen != null && tauxMoyen > 0 && vente.taux() > multiple * tauxMoyen;
            VariationRapportDTO signal = excessif ? new VariationRapportDTO("plus de " + libellerMultiple(multiple) + " × le taux moyen", "defavorable") : null;
            lignes.add(
                new LigneRapportDTO(
                    FormatRapport.formaterDate(vente.saleDate()),
                    List.of(
                        CelluleRapportDTO.lireTexte(vente.numero()),
                        CelluleRapportDTO.lireTexte(vente.vendeur()),
                        TableauxRapport.lireValeur(vente.montant(), UniteIndicateur.MONTANT),
                        TableauxRapport.lireValeur(vente.remise(), UniteIndicateur.MONTANT),
                        lireTaux(vente.taux(), signal),
                        CelluleRapportDTO.lireTexte(Objects.requireNonNullElse(vente.autorisePar(), "Privilège du vendeur"))
                    )
                )
            );
        }
        return new TableauRapportDTO("Date", COLONNES_FORTES_REMISES, lignes);
    }

    /** Remises / CA des modes d'octroi : le taux moyen des seules ventes remisées. */
    private static Double lireTauxDesVentesRemisees(List<LigneRemiseDTO> octrois) {
        double remises = 0;
        double ca = 0;
        for (LigneRemiseDTO octroi : octrois) {
            remises += Objects.requireNonNullElse(octroi.remises().valeur(), 0d);
            ca += Objects.requireNonNullElse(octroi.caTtc().valeur(), 0d);
        }
        return ca > 0 ? remises * 100 / ca : null;
    }

    private static List<TuileRapportDTO> lireTuilesDemarque(DemarqueRentabiliteDTO demarque) {
        return List.of(
            TuilesRapport.lireTuile("Valeur perdue", demarque.valeur(), UniteIndicateur.MONTANT, SensFavorable.BAISSE),
            TuilesRapport.lireTuile("En % du CA TTC", demarque.partDuCa(), UniteIndicateur.POURCENTAGE, SensFavorable.BAISSE)
        );
    }

    private static TableauRapportDTO construireDemarqueParMotif(List<LigneDemarqueDTO> motifs) {
        List<LigneRapportDTO> lignes = new ArrayList<>(motifs.size());
        for (LigneDemarqueDTO motif : motifs) {
            lignes.add(
                new LigneRapportDTO(
                    motif.libelle(),
                    List.of(
                        TableauxRapport.lireValeur(motif.quantite(), UniteIndicateur.NOMBRE),
                        TableauxRapport.lireCellule(motif.valeur(), UniteIndicateur.MONTANT, SensFavorable.BAISSE)
                    )
                )
            );
        }
        return new TableauRapportDTO("Motif", COLONNES_DEMARQUE, lignes);
    }

    private static TableauRapportDTO construireProduitsDemarques(List<MontantDemarqueDTO> produits) {
        List<LigneRapportDTO> lignes = new ArrayList<>(produits.size());
        for (MontantDemarqueDTO produit : produits) {
            List<CelluleRapportDTO> cellules = List.of(
                TableauxRapport.lireValeur(produit.quantite(), UniteIndicateur.NOMBRE),
                TableauxRapport.lireValeur(produit.valeur(), UniteIndicateur.MONTANT)
            );
            lignes.add(new LigneRapportDTO(produit.libelle(), cellules));
        }
        return new TableauRapportDTO("Produit", COLONNES_DEMARQUE, lignes);
    }

    private static CelluleRapportDTO lireTaux(Double taux, VariationRapportDTO variation) {
        return new CelluleRapportDTO(FormatRapport.formaterValeur(taux, UniteIndicateur.POURCENTAGE), variation);
    }

    private static String libellerMultiple(double multiple) {
        return FormatRapport.formaterValeur(multiple, UniteIndicateur.NOMBRE);
    }
}
