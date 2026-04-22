package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.repository.AvoirTiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import com.kobe.warehouse.service.report.CommonReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Transactional(readOnly = true)
public class AvoirPdfService extends CommonReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AvoirTiersPayantRepository avoirRepository;

    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private String currentHtml;

    public AvoirPdfService(
        AvoirTiersPayantRepository avoirRepository,
        StorageService storageService,
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.avoirRepository = avoirRepository;
        this.templateEngine = templateEngine;
    }

    @Override
    protected List<?> getItems() {
        return List.of();
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
    }
    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }
    @Override
    protected String getTemplateAsHtml() {
        return currentHtml;
    }



    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
      //  return templateEngine.process("avoir/pdf/list-main", context);
        return templateEngine.process(currentHtml, context);
    }
    @Override
    protected String getGenerateFileName() {
        return "avoir";
    }

    public byte[] generatePdf(Long avoirId) {
        currentHtml="avoir/pdf/main";
        AvoirTiersPayant avoir = avoirRepository.findById(avoirId)
            .orElseThrow(() -> new IllegalArgumentException("Avoir non trouvé: " + avoirId));


        FactureTiersPayant facture = avoir.getFactureTiersPayant();
        TiersPayant tp = facture.getTiersPayant();
        GroupeTiersPayant gtp = Objects.isNull(tp) ? facture.getGroupeTiersPayant() : null;
        String tiersPayantName = tp != null ? tp.getFullName() : (gtp != null ? gtp.getName() : "—");


        getParameters().put("avoir", avoir);
        getParameters().put("tiersPayantName", tiersPayantName);
        getParameters().put("lignes", avoir.getLignes() != null ? avoir.getLignes() : List.of());

        super.getCommonParameters();
        return exportReportToPdf();
    }

    public byte[] generateListPdf(AvoirSearchParams params) {
        currentHtml="avoir/pdf/list-main";
        List<AvoirStatut> statuts = !CollectionUtils.isEmpty(params.statuts())
            ? params.statuts()
            : List.of(AvoirStatut.values());
        LocalDate start = params.startDate() != null ? params.startDate() : LocalDate.now().minusMonths(6);
        LocalDate end = params.endDate() != null ? params.endDate() : LocalDate.now();
        String numAvoir = (params.numAvoir() != null && !params.numAvoir().isBlank())
            ? "%" + params.numAvoir().toLowerCase() + "%"
            : null;

        List<AvoirTiersPayant> avoirs;
        if (params.tiersPayantId() != null) {
            avoirs = avoirRepository.searchByTiersPayant(params.tiersPayantId(), start, end, statuts, numAvoir, Pageable.unpaged()).getContent();
        } else {
            avoirs = avoirRepository.searchAll(start, end, statuts, numAvoir, Pageable.unpaged()).getContent();
        }

        BigDecimal totalMontant = avoirs.stream()
            .map(a -> a.getMontantAvoir() != null ? a.getMontantAvoir() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTva = avoirs.stream()
            .map(a -> a.getMontantTva() != null ? a.getMontantTva() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHt = avoirs.stream()
            .map(a -> a.getMontantHt() != null ? a.getMontantHt() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String periode = "du " + start.format(DATE_FMT) + " au " + end.format(DATE_FMT);

        List<AvoirListItem> items = avoirs.stream().map(a -> {
            FactureTiersPayant f = a.getFactureTiersPayant();
            TiersPayant tp = f.getTiersPayant();
            GroupeTiersPayant gtp = Objects.isNull(tp) ? f.getGroupeTiersPayant() : null;
            String tpName = tp != null ? tp.getFullName() : (gtp != null ? gtp.getName() : "—");
            return new AvoirListItem(a, tpName);
        }).toList();

        getParameters().put("avoirs", items);
        getParameters().put("periode", periode);
        getParameters().put("totalCount", avoirs.size());
        getParameters().put("totalMontant", totalMontant);
        getParameters().put("totalTva", totalTva);
        getParameters().put("totalHt", totalHt);
        super.getCommonParameters();
        return exportReportToPdf();
    }

    public record AvoirListItem(AvoirTiersPayant avoir, String tiersPayantName) {
        public String getNumAvoir() { return avoir.getNumAvoir(); }
        public FactureTiersPayant getFactureTiersPayant() { return avoir.getFactureTiersPayant(); }
        public LocalDate getAvoirDate() { return avoir.getAvoirDate(); }
        public BigDecimal getMontantAvoir() { return avoir.getMontantAvoir(); }
        public BigDecimal getMontantTva() { return avoir.getMontantTva(); }
        public BigDecimal getMontantHt() { return avoir.getMontantHt(); }
        public AvoirStatut getStatut() { return avoir.getStatut(); }
        public String getMotif() { return avoir.getMotif(); }
    }
}
