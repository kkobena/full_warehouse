package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.DashboardCASummaryDTO;
import com.kobe.warehouse.service.dto.report.PaymentMethodCADTO;
import com.kobe.warehouse.service.dto.report.ProductFamilyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.DashboardCAService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardCAPdfExportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private final DashboardCAService dashboardCAService;

    public DashboardCAPdfExportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, DashboardCAService dashboardCAService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.dashboardCAService = dashboardCAService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/dashboard-ca/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/dashboard-ca/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_comparatif_ventes";
    }

    public byte[] export(LocalDate startDate, LocalDate endDate) {

        DashboardCASummaryDTO summary = dashboardCAService.getOverallSummary();
        List<PaymentMethodCADTO> paymentMethods = dashboardCAService.getPaymentMethodDistribution(startDate, endDate);
        List<ProductFamilyCADTO> productFamilies = dashboardCAService.getProductFamilyDistribution(startDate, endDate);
        List<TopProductDTO> topProducts = dashboardCAService.getTopProducts(startDate, endDate, 10);

        // Aggregate payment methods by method name
        Map<String, PaymentMethodSummary> paymentMap = new HashMap<>();
        paymentMethods.forEach(pm -> {
            String key = pm.paymentMethod();
            PaymentMethodSummary summary1 = paymentMap.getOrDefault(
                key,
                new PaymentMethodSummary(pm.paymentMethod(), pm.paymentCode(), 0L, 0)
            );
            paymentMap.put(
                key,
                new PaymentMethodSummary(
                    pm.paymentMethod(),
                    pm.paymentCode(),
                    summary1.montantTotal + pm.montantTotal(),
                    summary1.nbPayments + pm.nbPayments()
                )
            );
        });
        List<PaymentMethodSummary> aggregatedPayments = new ArrayList<>(paymentMap.values());
        aggregatedPayments.sort((a, b) -> Long.compare(b.montantTotal, a.montantTotal));

        // Aggregate product families
        Map<String, ProductFamilySummary> familyMap = new HashMap<>();
        productFamilies.forEach(pf -> {
            String key = pf.famille();
            ProductFamilySummary summary1 = familyMap.getOrDefault(
                key,
                new ProductFamilySummary(pf.famille(), 0L, 0L, BigDecimal.ZERO)
            );
            familyMap.put(
                key,
                new ProductFamilySummary(
                    pf.famille(),
                    summary1.caTotal + pf.caTotal(),
                    summary1.margeBrute + pf.margeBrute(),
                    summary1.tauxMargePct
                )
            );
        });
        List<ProductFamilySummary> aggregatedFamilies = new ArrayList<>(familyMap.values());
        // Recalculate margin percentage
        aggregatedFamilies.forEach(f -> {
            if (f.caTotal > 0) {
                BigDecimal newTauxMarge = BigDecimal
                    .valueOf(f.margeBrute * 100.0 / f.caTotal)
                    .setScale(2, RoundingMode.HALF_UP);
                familyMap.put(f.famille, new ProductFamilySummary(f.famille, f.caTotal, f.margeBrute, newTauxMarge));
            }
        });
        aggregatedFamilies = new ArrayList<>(familyMap.values());
        aggregatedFamilies.sort((a, b) -> Long.compare(b.caTotal, a.caTotal));

        // Calculate totals
        long totalFamilyCA = aggregatedFamilies.stream().mapToLong(f -> f.caTotal).sum();
        long totalFamilyMarge = aggregatedFamilies.stream().mapToLong(f -> f.margeBrute).sum();
        long totalTopProductsCA = topProducts.stream().mapToLong(TopProductDTO::caGenere).sum();

        this.getParameters().put("summary", summary);
        this.getParameters().put("paymentMethods", aggregatedPayments);
        this.getParameters().put("productFamilies", aggregatedFamilies);
        this.getParameters().put("topProducts", topProducts);
        this.getParameters().put("totalFamilyCA", totalFamilyCA);
        this.getParameters().put("totalFamilyMarge", totalFamilyMarge);
        this.getParameters().put("totalTopProductsCA", totalTopProductsCA);
        this.getParameters().put("startDate", startDate);
        this.getParameters().put("endDate", endDate);
        this.getParameters().put("reportTitle", "Dashboard CA et Performance des Ventes");

        super.getCommonParameters();
        return super.exportReportToPdf();
    }

    private record PaymentMethodSummary(String paymentMethod, String paymentCode, long montantTotal, int nbPayments) {
    }

    private record ProductFamilySummary(String famille, long caTotal, long margeBrute, BigDecimal tauxMargePct) {
    }
}
