package com.kobe.warehouse.service.report;

import com.kobe.warehouse.repository.CashFlowBfrRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.report.BfrEvolutionDTO;
import com.kobe.warehouse.service.dto.report.BfrSnapshotDTO;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CashFlowBfrServiceImpl implements CashFlowBfrService {

    private final CashFlowBfrRepository repository;
    private final UserService userService;

    public CashFlowBfrServiceImpl(CashFlowBfrRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Override
    @Cacheable(value = "cashFlowBfr", key = "'snapshot_' + #root.target.currentMagasinId()")
    public BfrSnapshotDTO getSnapshot() {
        long stockValue = repository.getStockValue(currentMagasinId());
        long creancesTp = repository.getCreanceTp();
        long dettesFournisseurs = Math.max(repository.getDetteFournisseur(), 0L);
        long cogs12m = repository.getCogs12m();
        long caTp12m = repository.getCaTp12m();
        long achats12m = repository.getAchats12m();

        long bfr = stockValue + creancesTp - dettesFournisseurs;

        int dio = cogs12m > 0 ? (int) Math.round(stockValue / (cogs12m / 365.0)) : 0;
        int dso = caTp12m > 0 ? (int) Math.round(creancesTp / (caTp12m / 365.0)) : 0;
        int dpo = achats12m > 0 ? (int) Math.round(dettesFournisseurs / (achats12m / 365.0)) : 0;
        int ccc = dio + dso - dpo;

        return new BfrSnapshotDTO(stockValue, creancesTp, dettesFournisseurs, bfr, dio, dso, dpo, ccc);
    }

    @Override
    @Cacheable(value = "cashFlowBfr", key = "'evolution'")
    public BfrEvolutionDTO getEvolution() {
        List<Object[]> rows = repository.findEvolution();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        List<String> labels = new ArrayList<>();
        List<Long> creancesEmises = new ArrayList<>();
        List<Long> achatsRecus = new ArrayList<>();

        for (Object[] row : rows) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            labels.add(YearMonth.of(yr, mo).atDay(1).format(fmt));
            creancesEmises.add(row[2] != null ? ((Number) row[2]).longValue() : 0L);
            achatsRecus.add(row[3] != null ? ((Number) row[3]).longValue() : 0L);
        }

        return new BfrEvolutionDTO(labels, creancesEmises, achatsRecus);
    }

    public Integer currentMagasinId() {
        return userService.getUser().getMagasin().getId();
    }
}
