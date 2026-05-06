package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.CommandeRepository;
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

    private final CommandeRepository commandeRepository;
    private final AvoirFournisseurRepository avoirRepository;

    public RemiseRfaServiceImpl(
        CommandeRepository commandeRepository,
        AvoirFournisseurRepository avoirRepository
    ) {
        this.commandeRepository = commandeRepository;
        this.avoirRepository = avoirRepository;
    }

    @Override
    public List<RemiseRfaFournisseurDTO> getRfaFournisseurs() {
        int year = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year + 1, 1, 1);

        List<Object[]> caRows = commandeRepository.sumCaByFournisseur(OrderStatut.CLOSED, start, end);
        Map<Integer, Long> avoirsMap = buildAvoirsMap();

        return caRows.stream()
            .map(row -> buildDto(row, avoirsMap))
            .collect(Collectors.toList());
    }

    @Override
    public List<AvoirFournisseurRfaDTO> getAvoirsFournisseurs() {
        return avoirRepository.findAllWithFournisseur().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private RemiseRfaFournisseurDTO buildDto(Object[] row, Map<Integer, Long> avoirsMap) {
        Integer fId       = (Integer) row[0];
        String  fName     = (String)  row[1];
        long    ca        = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Long    palier    = row[3] != null ? ((Number) row[3]).longValue() : null;
        Integer tauxPct   = row[4] != null ? ((Number) row[4]).intValue()  : null;
        long    rfaRecue  = avoirsMap.getOrDefault(fId, 0L);

        double pct = (palier != null && palier > 0) ? ca * 100.0 / palier : 0.0;
        long rfaEstimee   = (tauxPct != null) ? ca * tauxPct / 100 : 0L;

        String alerte = buildAlerte(palier, pct, tauxPct);

        return new RemiseRfaFournisseurDTO(fId, fName, palier, ca, pct, rfaEstimee, rfaRecue, alerte);
    }

    private String buildAlerte(Long palier, double pct, Integer tauxPct) {
        if (palier == null) return null;
        if (pct >= 100) return null;
        if (pct >= 80) return String.format("À %.0f%% du palier RFA — accélérer les commandes !", pct);
        if (tauxPct != null && pct < 50) return String.format("Seulement %.0f%% du palier RFA atteint.", pct);
        return null;
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
            a.getStatut().name()
        );
    }
}
