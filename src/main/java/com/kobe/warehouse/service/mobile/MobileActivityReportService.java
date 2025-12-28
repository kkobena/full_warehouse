package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.activity_summary.ActivitySummaryService;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.mobile.*;
import com.kobe.warehouse.service.dto.projection.*;
import com.kobe.warehouse.service.dto.records.ChiffreAffaireRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for mobile activity report (Rapport d'Activité).
 * Consolidates data from multiple sources into a single mobile-friendly response.
 */
@Service
@Transactional(readOnly = true)
public class MobileActivityReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ActivitySummaryService activitySummaryService;

    public MobileActivityReportService(ActivitySummaryService activitySummaryService) {
        this.activitySummaryService = activitySummaryService;
    }

    /**
     * Get activity report for the given date range.
     *
     * @param fromDate Start date
     * @param toDate   End date (defaults to fromDate if null)
     * @return Mobile activity report DTO
     */
    public MobileActivityReportDTO getActivityReport(LocalDate fromDate, LocalDate toDate) {
        if (toDate == null) {
            toDate = fromDate;
        }

        // Get chiffre d'affaires (includes recettes and mouvements)
        ChiffreAffaireDTO caDTO = activitySummaryService.getChiffreAffaire(fromDate, toDate);

        // Get recettes
        List<Recette> recettes = activitySummaryService.findRecettes(fromDate, toDate);

        // Get mouvements caisse
        List<MouvementCaisse> mouvements = activitySummaryService.findMouvementsCaisse(fromDate, toDate);

        // Get achats fournisseurs (first 20)
        Page<GroupeFournisseurAchat> achatsPage = activitySummaryService.fetchAchats(
            fromDate, toDate, PageRequest.of(0, 20)
        );

        // Get tiers payants (first 20 each)
        Page<ReglementTiersPayants> reglementsPage = activitySummaryService.findReglementTierspayant(
            fromDate, toDate, null, PageRequest.of(0, 20)
        );
        Page<AchatTiersPayant> achatsTpPage = activitySummaryService.fetchAchatTiersPayant(
            fromDate, toDate, null, PageRequest.of(0, 20)
        );

        return buildActivityReport(
            fromDate, toDate,
            caDTO, recettes, mouvements,
            achatsPage.getContent(),
            reglementsPage.getContent(),
            achatsTpPage.getContent()
        );
    }

    private MobileActivityReportDTO buildActivityReport(
            LocalDate fromDate,
            LocalDate toDate,
            ChiffreAffaireDTO caDTO,
            List<Recette> recettes,
            List<MouvementCaisse> mouvements,
            List<GroupeFournisseurAchat> achats,
            List<ReglementTiersPayants> reglements,
            List<AchatTiersPayant> achatsTp) {

        // Build chiffre d'affaires
        ChiffreAffaireMobileDTO chiffreAffaire = buildChiffreAffaire(caDTO);

        // Build recettes
        List<RecetteMobileDTO> recettesMobile = buildRecettes(recettes);
        long totalRecettes = recettesMobile.stream().mapToLong(RecetteMobileDTO::montant).sum();

        // Build mouvements caisse
        List<MouvementCaisseMobileDTO> mouvementsMobile = buildMouvements(mouvements);
        AtomicLong totalEntrees = new AtomicLong(0);
        AtomicLong totalSorties = new AtomicLong(0);
        mouvementsMobile.forEach(m -> {
            if (m.isEntree()) {
                totalEntrees.addAndGet(m.montant());
            } else {
                totalSorties.addAndGet(m.montant());
            }
        });

        // Build achats fournisseurs
        List<GroupeFournisseurAchatMobileDTO> achatsMobile = buildAchats(achats);
        long totalAchats = achatsMobile.stream().mapToLong(GroupeFournisseurAchatMobileDTO::montantTtc).sum();

        // Build tiers payants
        TiersPayantSummaryMobileDTO tiersPayants = buildTiersPayants(reglements, achatsTp);

        // Build period label
        String periodLabel = buildPeriodLabel(fromDate, toDate);

        return MobileActivityReportDTO.builder()
            .fromDate(fromDate)
            .toDate(toDate)
            .periodLabel(periodLabel)
            .chiffreAffaire(chiffreAffaire)
            .recettes(recettesMobile)
            .totalRecettes(totalRecettes)
            .mouvementsCaisse(mouvementsMobile)
            .totalEntrees(totalEntrees.get())
            .totalSorties(totalSorties.get())
            .achatsFournisseurs(achatsMobile)
            .totalAchats(totalAchats)
            .tiersPayants(tiersPayants)
            .build();
    }

    private ChiffreAffaireMobileDTO buildChiffreAffaire(ChiffreAffaireDTO caDTO) {
        if (caDTO == null || caDTO.chiffreAffaire() == null) {
            return ChiffreAffaireMobileDTO.empty();
        }

        ChiffreAffaireRecord ca = caDTO.chiffreAffaire();
        long montantTtc = toLong(ca.montantTtc());
        long marge = toLong(ca.marge());
        double margePercent = montantTtc > 0 ? (marge * 100.0 / montantTtc) : 0.0;

        return new ChiffreAffaireMobileDTO(
            montantTtc,
            toLong(ca.montantTva()),
            toLong(ca.montantHt()),
            toLong(ca.montantRemise()),
            toLong(ca.montantNet()),
            toLong(ca.montantEspece()),
            toLong(ca.montantAutreModePaiement()),
            toLong(ca.montantCredit()),
            toLong(ca.montantRegle()),
            marge,
            Math.round(margePercent * 10.0) / 10.0
        );
    }

    private List<RecetteMobileDTO> buildRecettes(List<Recette> recettes) {
        if (recettes == null || recettes.isEmpty()) {
            return List.of();
        }

        long total = recettes.stream()
            .mapToLong(Recette::realAmount)
            .sum();

        return recettes.stream()
            .map(r -> {
                long montant = r.realAmount();
                double percent = total > 0 ? (montant * 100.0 / total) : 0.0;
                return new RecetteMobileDTO(
                    r.code(),
                    r.libelle(),
                    montant,
                    Math.round(percent * 10.0) / 10.0,
                    RecetteMobileDTO.getColorForCode(r.code())
                );
            })
            .toList();
    }

    private List<MouvementCaisseMobileDTO> buildMouvements(List<MouvementCaisse> mouvements) {
        if (mouvements == null || mouvements.isEmpty()) {
            return List.of();
        }

        return mouvements.stream()
            .map(m -> {
                String type = determineMovementType(m.getType());
                return new MouvementCaisseMobileDTO(
                    m.getLibelle(),
                    toLong(m.getMontant()),
                    type
                );
            })
            .toList();
    }

    private String determineMovementType(TypeFinancialTransaction type) {
        if (type == null) {
            return MouvementCaisseMobileDTO.TYPE_ENTREE;
        }
        // Determine if it's an entry or exit based on the category
        return switch (type.getCategorieTransaction()) {
            case SORTIE_CAISSE -> MouvementCaisseMobileDTO.TYPE_SORTIE;
            case ENTREE, VENTES -> MouvementCaisseMobileDTO.TYPE_ENTREE;
        };
    }

    private List<GroupeFournisseurAchatMobileDTO> buildAchats(List<GroupeFournisseurAchat> achats) {
        if (achats == null || achats.isEmpty()) {
            return List.of();
        }

        long total = achats.stream()
            .mapToLong(a -> toLong(a.getMontantTtc()))
            .sum();

        return achats.stream()
            .map(a -> {
                long montantTtc = toLong(a.getMontantTtc());
                double percent = total > 0 ? (montantTtc * 100.0 / total) : 0.0;
                return new GroupeFournisseurAchatMobileDTO(
                    a.getLibelle(),
                    montantTtc,
                    toLong(a.getMontantTva()),
                    toLong(a.getMontantHt()),
                    Math.round(percent * 10.0) / 10.0
                );
            })
            .toList();
    }

    private TiersPayantSummaryMobileDTO buildTiersPayants(
            List<ReglementTiersPayants> reglements,
            List<AchatTiersPayant> achats) {

        // Build reglements
        List<ReglementTiersPayantMobileDTO> reglementsMobile = new ArrayList<>();
        long totalFacture = 0;
        long totalRegle = 0;

        if (reglements != null) {
            for (ReglementTiersPayants r : reglements) {
                long facture = r.montantFacture() != null ? r.montantFacture() : 0;
                long regle = r.montantReglement() != null ? r.montantReglement() : 0;
                long restant = facture - regle;

                reglementsMobile.add(new ReglementTiersPayantMobileDTO(
                    r.libelle(),
                    r.type() != null ? r.type().name() : "",
                    r.numFacture(),
                    facture,
                    regle,
                    restant
                ));

                totalFacture += facture;
                totalRegle += regle;
            }
        }

        // Build achats
        List<AchatTiersPayantMobileDTO> achatsMobile = new ArrayList<>();
        int totalBons = 0;
        long totalMontant = 0;
        int totalClients = 0;

        if (achats != null) {
            for (AchatTiersPayant a : achats) {
                int bons = a.bonsCount() != null ? a.bonsCount().intValue() : 0;
                long montant = a.montant() != null ? a.montant().longValue() : 0;
                int clients = a.clientCount() != null ? a.clientCount().intValue() : 0;

                achatsMobile.add(new AchatTiersPayantMobileDTO(
                    a.libelle(),
                    a.categorie() != null ? a.categorie().name() : "",
                    bons,
                    montant,
                    clients
                ));

                totalBons += bons;
                totalMontant += montant;
                totalClients += clients;
            }
        }

        return new TiersPayantSummaryMobileDTO(
            reglementsMobile,
            totalFacture,
            totalRegle,
            totalFacture - totalRegle,
            achatsMobile,
            totalBons,
            totalMontant,
            totalClients
        );
    }

    private String buildPeriodLabel(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();

        if (fromDate.equals(toDate)) {
            if (fromDate.equals(today)) {
                return "Aujourd'hui";
            } else if (fromDate.equals(today.minusDays(1))) {
                return "Hier";
            } else {
                return fromDate.format(DATE_FORMATTER);
            }
        } else {
            return fromDate.format(DATE_FORMATTER) + " - " + toDate.format(DATE_FORMATTER);
        }
    }

    private long toLong(BigDecimal value) {
        return value != null ? value.longValue() : 0;
    }
}
