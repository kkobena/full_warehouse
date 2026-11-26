package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.EtatPalierRfa;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.service.dto.AvoirFournisseurRfaDTO;
import com.kobe.warehouse.service.dto.RemiseRfaFournisseurDTO;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RemiseRfaServiceImpl implements RemiseRfaService {

    /** Au-delà, chaque commande orientée vers ce grossiste peut faire basculer le palier. */
    private static final double SEUIL_APPROCHE_PCT = 80.0;

    /** En deçà, l'échéance annuelle est trop lointaine pour peser sur une commande du jour. */
    private static final double SEUIL_ELOIGNEMENT_PCT = 50.0;

    private final CommandeRepository commandeRepository;
    private final AvoirFournisseurRepository avoirRepository;
    private final FournisseurRepository fournisseurRepository;

    public RemiseRfaServiceImpl(
            CommandeRepository commandeRepository,
            AvoirFournisseurRepository avoirRepository,
            FournisseurRepository fournisseurRepository) {
        this.commandeRepository = commandeRepository;
        this.avoirRepository = avoirRepository;
        this.fournisseurRepository = fournisseurRepository;
    }

    @Override
    public List<RemiseRfaFournisseurDTO> getRfaFournisseurs() {
        return lireAvancement(null);
    }

    @Override
    public RemiseRfaFournisseurDTO lireRfaDuGroupe(Integer fournisseurId) {
        Integer groupeId = fournisseurRepository.lireIdDuGroupe(fournisseurId).orElse(null);
        if (groupeId == null) {
            return null;
        }
        // Un groupe sans commande close cette année ne rend aucune ligne : il n'y a alors rien
        // a afficher, pas meme un zero, puisque l'accord lui-meme peut ne pas exister.
        return lireAvancement(groupeId).stream().findFirst().orElse(null);
    }

    @Override
    public List<AvoirFournisseurRfaDTO> getAvoirsFournisseurs() {
        return avoirRepository.findAllWithFournisseur().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * L'exercice court du premier janvier au premier janvier suivant : la remise de fin d'année
     * se calcule sur l'année civile, et seules les commandes closes y entrent.
     */
    private List<RemiseRfaFournisseurDTO> lireAvancement(Integer groupeId) {
        int year = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year + 1, 1, 1);

        List<Object[]> caRows = commandeRepository.sumCaByFournisseur(OrderStatut.CLOSED, start, end, groupeId);
        Map<Integer, Long> avoirsMap = buildAvoirsMap();

        return caRows.stream().map(row -> buildDto(row, avoirsMap)).collect(Collectors.toList());
    }

    private RemiseRfaFournisseurDTO buildDto(Object[] row, Map<Integer, Long> avoirsMap) {
        Integer fId = (Integer) row[0];
        String fName = (String) row[1];
        long ca = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Long palier = row[3] != null ? ((Number) row[3]).longValue() : null;
        Integer tauxPct = row[4] != null ? ((Number) row[4]).intValue() : null;
        long rfaRecue = avoirsMap.getOrDefault(fId, 0L);

        double pct = (palier != null && palier > 0) ? ca * 100.0 / palier : 0.0;
        long rfaEstimee = (tauxPct != null) ? ca * tauxPct / 100 : 0L;
        long reste = (palier != null) ? Math.max(0L, palier - ca) : 0L;

        return new RemiseRfaFournisseurDTO(
                fId, fName, palier, ca, pct, rfaEstimee, rfaRecue, reste, determinerEtat(palier, tauxPct, pct));
    }

    /**
     * Nomme l'avancement ; la formulation destinée au pharmacien se compose au front.
     *
     * <p>Les avoirs déjà remboursés n'entrent pas dans ce jugement : ils disent ce que le
     * grossiste a versé par le passé, pas où en est l'exercice en cours.
     */
    private EtatPalierRfa determinerEtat(Long palier, Integer tauxPct, double pct) {
        if (palier == null || palier <= 0) {
            return EtatPalierRfa.SANS_ACCORD;
        }
        if (pct >= 100) {
            return EtatPalierRfa.ATTEINT;
        }
        if (pct >= SEUIL_APPROCHE_PCT) {
            return EtatPalierRfa.PROCHE_DU_PALIER;
        }
        // Sans taux négocié, le retard n'a pas d'objet : il n'y a rien à gagner à combler
        // l'écart. Règle d'origine, conservée telle quelle — seule sa formulation a changé de
        // couche.
        if (pct < SEUIL_ELOIGNEMENT_PCT && tauxPct != null) {
            return EtatPalierRfa.LOIN_DU_PALIER;
        }
        return EtatPalierRfa.EN_COURS;
    }

    private Map<Integer, Long> buildAvoirsMap() {
        List<Object[]> rows = avoirRepository.sumParFournisseur(AvoirFournisseurStatut.REMBOURSE);
        Map<Integer, Long> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            map.put((Integer) row[0], ((Number) row[2]).longValue());
        }
        return map;
    }

    private AvoirFournisseurRfaDTO toDto(AvoirFournisseur a) {
        return new AvoirFournisseurRfaDTO(
                a.getId(),
                a.getFournisseur().getLibelle(),
                a.getReference(),
                a.getDateMtv().toLocalDate().toString(),
                a.getMontant(),
                a.getStatut().name());
    }
}
