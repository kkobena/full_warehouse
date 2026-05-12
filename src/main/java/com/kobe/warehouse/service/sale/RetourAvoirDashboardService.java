package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO.ClientAlerteDTO;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO.MotifStatDTO;
import com.kobe.warehouse.service.sale.dto.RetourAvoirStatsDTO.ProduitAlerteDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RetourAvoirDashboardService {

    private static final int SEUIL_RETOURS_PRODUIT = 5;
    private static final int SEUIL_RETOURS_CLIENT = 3;
    private static final int JOURS_ALERTE_EXPIRATION = 7;

    private final RetourClientRepository retourClientRepository;
    private final AvoirClientRepository avoirClientRepository;

    public RetourAvoirDashboardService(
        RetourClientRepository retourClientRepository,
        AvoirClientRepository avoirClientRepository
    ) {
        this.retourClientRepository = retourClientRepository;
        this.avoirClientRepository = avoirClientRepository;
    }

    public RetourAvoirStatsDTO getStats(YearMonth mois) {
        YearMonth periode = Objects.requireNonNullElse(mois, YearMonth.now());
        LocalDateTime debut = periode.atDay(1).atStartOfDay();
        LocalDateTime fin = periode.atEndOfMonth().plusDays(1).atStartOfDay();

        // ── Retours ────────────────────────────────────────────────────────────
        Object[] globaux = retourClientRepository.statsGlobales(debut, fin);
        int nbRetours = globaux[0] != null ? ((Number) globaux[0]).intValue() : 0;
        int montantRetours = globaux[1] != null ? ((Number) globaux[1]).intValue() : 0;

        List<MotifStatDTO> statsMotifs = retourClientRepository.statsParMotif(debut, fin).stream()
            .map(row -> new MotifStatDTO((MotifRetourClient) row[0], ((Number) row[1]).longValue()))
            .toList();

        List<ProduitAlerteDTO> produitsAlerte = retourClientRepository
            .produitsEnAlerte(debut, fin, SEUIL_RETOURS_PRODUIT).stream()
            .map(row -> new ProduitAlerteDTO(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue()))
            .toList();

        List<ClientAlerteDTO> clientsAlerte = retourClientRepository
            .clientsEnAlerte(debut, fin, SEUIL_RETOURS_CLIENT).stream()
            .map(row -> new ClientAlerteDTO(
                ((Number) row[0]).intValue(),
                row[1] + " " + row[2],
                ((Number) row[3]).longValue()))
            .toList();

        // ── Avoirs ─────────────────────────────────────────────────────────────
        Object[] avoirsStats = avoirClientRepository.statsAvoirsOuverts();
        int nbAvoirsOuverts = avoirsStats[0] != null ? ((Number) avoirsStats[0]).intValue() : 0;
        int montantAvoirsOuverts = avoirsStats[1] != null ? ((Number) avoirsStats[1]).intValue() : 0;

        LocalDate seuilExpiration = LocalDate.now().plusDays(JOURS_ALERTE_EXPIRATION);
        int nbProchesExpiration = (int) avoirClientRepository.countAvoirsProchesExpiration(seuilExpiration);

        return new RetourAvoirStatsDTO(
            nbRetours,
            montantRetours,
            statsMotifs,
            produitsAlerte,
            clientsAlerte,
            nbAvoirsOuverts,
            montantAvoirsOuverts,
            nbProchesExpiration
        );
    }
}
