package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.repository.PnlAnalytiqueRepository;
import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO;
import com.kobe.warehouse.service.dto.report.PnlEvolutionDTO.PnlEvolutionSerieDTO;
import com.kobe.warehouse.service.dto.report.PnlFamilleDTO;
import com.kobe.warehouse.service.dto.report.PnlSegmentDTO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedSet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PnlAnalytiqueServiceImpl implements PnlAnalytiqueService {

    private final PnlAnalytiqueRepository pnlAnalytiqueRepository;

    public PnlAnalytiqueServiceImpl(PnlAnalytiqueRepository pnlAnalytiqueRepository) {
        this.pnlAnalytiqueRepository = pnlAnalytiqueRepository;
    }

    @Override
    @Cacheable(value = "pnlAnalytique", key = "'segment_' + #year")
    public List<PnlSegmentDTO> getSnapshotBySegment(int year) {
        return pnlAnalytiqueRepository.findSnapshotBySegment(year)
            .stream()
            .map(row -> new PnlSegmentDTO(
                (String) row[0],
                segmentLabel((String) row[0]),
                row[1] != null ? ((Number) row[1]).longValue() : 0L,
                row[2] != null ? ((Number) row[2]).longValue() : 0L,
                row[3] != null ? ((Number) row[3]).longValue() : 0L,
                row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO,
                row[5] != null ? ((Number) row[5]).intValue() : 0
            ))
            .toList();
    }

    @Override
    @Cacheable(value = "pnlAnalytique", key = "'famille_' + #year")
    public List<PnlFamilleDTO> getSnapshotByFamille(int year) {
        return pnlAnalytiqueRepository.findSnapshotByFamille(year)
            .stream()
            .map(row -> new PnlFamilleDTO(
                (String) row[0],
                row[1] != null ? ((Number) row[1]).longValue() : 0L,
                row[2] != null ? ((Number) row[2]).longValue() : 0L,
                row[3] != null ? ((Number) row[3]).longValue() : 0L,
                row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO
            ))
            .toList();
    }

    @Override
    @Cacheable(value = "pnlAnalytique", key = "'evolution'")
    public PnlEvolutionDTO getEvolutionByFamille() {
        List<Object[]> rows = pnlAnalytiqueRepository.findEvolutionMonthlyByFamille();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        List<String> labels = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> tauxParMoisEtFamille = new LinkedHashMap<>();
        SequencedSet<String> familles = new LinkedHashSet<>();

        for (Object[] row : rows) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            String famille = (String) row[2];
            BigDecimal taux = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            String monthLabel = YearMonth.of(yr, mo).atDay(1).format(fmt);
            if (!labels.contains(monthLabel)) {
                labels.add(monthLabel);
            }
            tauxParMoisEtFamille.computeIfAbsent(monthLabel, k -> new LinkedHashMap<>()).put(famille, taux);
            familles.add(famille);
        }

        List<PnlEvolutionSerieDTO> series = familles
            .stream()
            .map(famille -> new PnlEvolutionSerieDTO(famille, null, valeursAlignees(labels, tauxParMoisEtFamille, famille)))
            .toList();

        return new PnlEvolutionDTO(labels, series);
    }

    @Override
    @Cacheable(value = "pnlAnalytique", key = "'evolution_segment'")
    public PnlEvolutionDTO getEvolutionBySegment() {
        List<Object[]> rows = pnlAnalytiqueRepository.findEvolutionMonthlyBySegment();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        List<String> labels = new ArrayList<>();
        Map<String, Map<String, BigDecimal>> tauxParMoisEtSegment = new LinkedHashMap<>();
        SequencedSet<String> segments = new LinkedHashSet<>();

        for (Object[] row : rows) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            String segment = segmentLabel((String) row[2]);
            BigDecimal taux = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            String monthLabel = YearMonth.of(yr, mo).atDay(1).format(fmt);
            if (!labels.contains(monthLabel)) {
                labels.add(monthLabel);
            }
            tauxParMoisEtSegment.computeIfAbsent(monthLabel, k -> new LinkedHashMap<>()).put(segment, taux);
            segments.add(segment);
        }

        List<PnlEvolutionSerieDTO> series = segments
            .stream()
            .map(segment -> new PnlEvolutionSerieDTO(null, segment, valeursAlignees(labels, tauxParMoisEtSegment, segment)))
            .toList();

        return new PnlEvolutionDTO(labels, series);
    }

    /**
     * Aligne une serie sur l'axe des mois.
     *
     * <p>Une famille qui n'a rien vendu un mois n'a pas de ligne ce mois-la. Empiler les valeurs
     * dans leur ordre d'arrivee decalait alors toute la fin de sa courbe d'un cran : le taux de mars
     * s'affichait sur fevrier, et ainsi de suite jusqu'au bout.
     */
    private List<BigDecimal> valeursAlignees(List<String> labels, Map<String, Map<String, BigDecimal>> parMois, String serie) {
        return labels.stream().map(label -> parMois.getOrDefault(label, Map.of()).getOrDefault(serie, BigDecimal.ZERO)).toList();
    }

    private String segmentLabel(String natureVente) {
        if (natureVente == null) {
            return "Autre";
        }
        try {
            return switch (NatureVente.valueOf(natureVente)) {
                case COMPTANT -> "Comptant";
                case ASSURANCE -> "Ordonnancée (tiers payant)";
                case CARNET -> "Carnet";
            };
        } catch (IllegalArgumentException e) {
            return natureVente;
        }
    }
}
