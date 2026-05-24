package com.kobe.warehouse.service.report;

import com.kobe.warehouse.repository.VieillissementCreancesRepository;
import com.kobe.warehouse.service.dto.report.DsoOrganismeDTO;
import com.kobe.warehouse.service.dto.report.EncoursMensuelDTO;
import com.kobe.warehouse.service.dto.report.VieillissementGlobalDTO;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VieillissementCreancesServiceImpl implements VieillissementCreancesService {

    private final VieillissementCreancesRepository repository;

    public VieillissementCreancesServiceImpl(VieillissementCreancesRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(value = "vieillissementCreances", key = "'global'")
    public VieillissementGlobalDTO getAgingGlobal() {
        Object[] row = repository.findAgingGlobal();
        return new VieillissementGlobalDTO(
            toLong(row[4]),   // total_encours
            toLong(row[0]),   // tranche_0_30
            toLong(row[1]),   // tranche_31_60
            toLong(row[2]),   // tranche_61_90
            toLong(row[3]),   // tranche_90_plus
            toLong(row[5]),   // nb_factures
            toLong(row[6])    // nb_en_retard
        );
    }

    @Override
    @Cacheable(value = "vieillissementCreances", key = "'dso_p' + #pageable.pageNumber + '_s' + #pageable.pageSize")
    public Page<DsoOrganismeDTO> getDsoByOrganisme(Pageable pageable) {
        long total = repository.countAgingByOrganisme();
        List<DsoOrganismeDTO> content = repository
            .findAgingByOrganisme((int) pageable.getOffset(), pageable.getPageSize())
            .stream()
            .map(row -> {
                String organisme = (String) row[0];
                int delai = toInt(row[1]);
                long encours = toLong(row[2]);
                long t030 = toLong(row[3]);
                long t3160 = toLong(row[4]);
                long t6190 = toLong(row[5]);
                long t90p = toLong(row[6]);
                long nbFact = toLong(row[7]);
                long nbRetard = toLong(row[8]);
                int dso = toInt(row[9]);
                String fiabilite = computeFiabilite(encours, t90p, dso, delai);
                return new DsoOrganismeDTO(organisme, encours, t030, t3160, t6190, t90p,
                    nbFact, nbRetard, dso, delai, fiabilite);
            })
            .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Cacheable(value = "vieillissementCreances", key = "'encoursMensuel'")
    public EncoursMensuelDTO getEncoursMensuelEvolution() {
        List<Object[]> rows = repository.findEncoursMensuelEvolution();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
        List<String> labels = new ArrayList<>();
        List<Long> montantFacture = new ArrayList<>();
        List<Long> encoursRestant = new ArrayList<>();
        for (Object[] row : rows) {
            int yr = toInt(row[0]);
            int mo = toInt(row[1]);
            labels.add(YearMonth.of(yr, mo).atDay(1).format(fmt));
            montantFacture.add(toLong(row[2]));
            encoursRestant.add(toLong(row[3]));
        }
        return new EncoursMensuelDTO(labels, montantFacture, encoursRestant);
    }

    private String computeFiabilite(long encours, long tranche90Plus, int dsoJours, int delaiReglement) {
        if (encours == 0) return "BON";
        double pct90Plus = (double) tranche90Plus / encours * 100;
        if (pct90Plus > 25 || dsoJours > 90) return "RISQUE";
        if (pct90Plus > 10 || dsoJours > Math.max(delaiReglement, 30) + 30) return "SURVEILLER";
        return "BON";
    }

    private static long toLong(Object o) {
        return o != null ? ((Number) o).longValue() : 0L;
    }

    private static int toInt(Object o) {
        return o != null ? ((Number) o).intValue() : 0;
    }
}
