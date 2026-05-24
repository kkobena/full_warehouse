package com.kobe.warehouse.service.report;

import com.kobe.warehouse.repository.ConcentrationPayersRepository;
import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationEvolutionDTO.ConcentrationEvolutionSerieDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationOrganismeDTO;
import com.kobe.warehouse.service.dto.report.ConcentrationSummaryDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
public class ConcentrationPayersServiceImpl implements ConcentrationPayersService {

    private final ConcentrationPayersRepository repository;

    public ConcentrationPayersServiceImpl(ConcentrationPayersRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(value = "concentrationPayers", key = "#periode + '_' + #topN")
    public ConcentrationSummaryDTO getSummary(String periode, int topN) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = "year".equals(periode)
            ? toDate.withDayOfYear(1)
            : toDate.minusDays(89);
        long periodDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        List<Object[]> rows = repository.findConcentration(fromDate, toDate, topN);
        if (rows.isEmpty()) {
            return new ConcentrationSummaryDTO(List.of(), 0L, 0L, 0L, 0, "FAIBLE");
        }

        long totalCaTp = toLong(rows.getFirst()[5]);
        long totalRegle = toLong(rows.getFirst()[6]);
        long totalImpaye = toLong(rows.getFirst()[7]);

        List<ConcentrationOrganismeDTO> organismes = rows.stream()
            .map(row -> {
                String organisme = (String) row[0];
                long caTp = toLong(row[1]);
                int nbFact = toInt(row[2]);
                int delai = toInt(row[3]);
                double partPct = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
                long stress = (caTp > 0 && periodDays > 0) ? Math.round((double) caTp / periodDays * 30) : 0;
                return new ConcentrationOrganismeDTO(organisme, caTp, nbFact, partPct, delai, stress);
            })
            .toList();

        double hhi = organismes.stream()
            .mapToDouble(o -> Math.pow(o.partPct() / 100.0, 2))
            .sum() * 10000;
        int hhiIndex = (int) Math.round(hhi);

        String riskLevel = hhiIndex >= 2500 ? "ELEVE" : hhiIndex >= 1000 ? "MODERE" : "FAIBLE";

        return new ConcentrationSummaryDTO(organismes, totalCaTp, totalRegle, totalImpaye, hhiIndex, riskLevel);
    }

    @Override
    public List<ConcentrationOrganismeDTO> getOrganismes(String periode, int topN) {
        return getSummary(periode, topN).organismes();
    }

    @Override
    @Cacheable(value = "concentrationPayers", key = "'evolution_' + #topN")
    public ConcentrationEvolutionDTO getEvolution(int topN) {
        List<Object[]> rows = repository.findEvolution(topN);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        Map<String, Map<String, Long>> monthOrgMap = new LinkedHashMap<>();
        SequencedSet<String> allOrganismes = new LinkedHashSet<>();

        for (Object[] row : rows) {
            int yr = toInt(row[0]);
            int mo = toInt(row[1]);
            String org = (String) row[2];
            long ca = toLong(row[3]);
            String label = YearMonth.of(yr, mo).atDay(1).format(fmt);
            monthOrgMap.computeIfAbsent(label, k -> new LinkedHashMap<>()).put(org, ca);
            allOrganismes.add(org);
        }

        List<String> labels = new ArrayList<>(monthOrgMap.keySet());

        List<ConcentrationEvolutionSerieDTO> series = allOrganismes.stream()
            .map(org -> {
                List<Long> values = labels.stream()
                    .map(label -> monthOrgMap.getOrDefault(label, Map.of()).getOrDefault(org, 0L))
                    .toList();
                return new ConcentrationEvolutionSerieDTO(org, values);
            })
            .toList();

        return new ConcentrationEvolutionDTO(labels, series);
    }

    private static long toLong(Object o) {
        return o != null ? ((Number) o).longValue() : 0L;
    }

    private static int toInt(Object o) {
        return o != null ? ((Number) o).intValue() : 0;
    }
}
