package com.kobe.warehouse.service.dashboard.impl;

import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dashboard.mapper.DashboardDTOMapper;
import com.kobe.warehouse.service.dto.dashboard.*;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import com.kobe.warehouse.service.report.CashRegisterReportService;
import com.kobe.warehouse.service.report.TopProductsReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CaissierDashboardServiceImpl implements CaissierDashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CashRegisterReportService cashRegisterReportService;
    private final TopProductsReportService topProductsReportService;
    private final DashboardDTOMapper mapper;

    public CaissierDashboardServiceImpl(
        CashRegisterReportService cashRegisterReportService,
        TopProductsReportService topProductsReportService,
        DashboardDTOMapper mapper
    ) {
        this.cashRegisterReportService = cashRegisterReportService;
        this.topProductsReportService = topProductsReportService;
        this.mapper = mapper;
    }

    @Override
    public CaissierDashboardDTO getDashboardData() {
        return new CaissierDashboardDTO(
            getVentesJour(),
            getCaisseStatus(),
            getStatistiquesRapides(),
            getVentesRecentes(10),
            getTopProduits(10),
            getPerformanceVendeurs(),
            getAlertes()
        );
    }

    @Override
    public VentesJourDTO getVentesJour() {
        List<DailyCashRegisterReportDTO> cashRegisterReports =
            cashRegisterReportService.getDailyReport(LocalDate.now());
        return mapper.toVentesJourDTO(cashRegisterReports);
    }

    @Override
    public CaisseStatusDTO getCaisseStatus() {
        List<DailyCashRegisterReportDTO> cashRegisterReports =
            cashRegisterReportService.getDailyReport(LocalDate.now());
        return mapper.toCaisseStatusDTO(cashRegisterReports);
    }

    @Override
    public StatistiquesRapidesDTO getStatistiquesRapides() {
        String query = """
            SELECT
                COUNT(DISTINCT CASE WHEN s.statut = 'PROCESSING' THEN s.id END) as ventes_en_cours,
                COUNT(DISTINCT s.customer_id) as clients_servis,
                COALESCE(SUM(sl.quantity_sold), 0) as produits_vendus,
                COALESCE(AVG(EXTRACT(EPOCH FROM (s.updated_at - s.created_at)) / 60), 0) as temps_moyen
            FROM sales s
            LEFT JOIN sales_line sl ON sl.sales_id = s.id
            WHERE DATE(s.created_at) = CURRENT_DATE
        """;

        Query q = entityManager.createNativeQuery(query);
        Object[] result = (Object[]) q.getSingleResult();

        Integer ventesEnCours = toInt(result[0]);
        Integer clientsServis = toInt(result[1]);
        Integer produitsVendus = toInt(result[2]);
        Integer tempsMoyen = toInt(result[3]);

        return new StatistiquesRapidesDTO(ventesEnCours, clientsServis, produitsVendus, tempsMoyen);
    }

    @Override
    public List<VenteRecenteDTO> getVentesRecentes(Integer limit) {
        String query = """
            SELECT
                s.id,
                s.number_transaction,
                s.sales_amount,
                s.created_at,
                p.mode_paiement,
                u.first_name || ' ' || u.last_name as vendeur,
                COUNT(sl.id) as nombre_lignes,
                s.statut
            FROM sales s
            LEFT JOIN payment p ON p.sales_id = s.id
            LEFT JOIN app_user u ON u.id = s.seller_id
            LEFT JOIN sales_line sl ON sl.sales_id = s.id
            WHERE DATE(s.created_at) = CURRENT_DATE
            GROUP BY s.id, s.number_transaction, s.sales_amount, s.created_at, p.mode_paiement, u.first_name, u.last_name, s.statut
            ORDER BY s.created_at DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("limit", limit);

        List<?> results = q.getResultList();
        List<VenteRecenteDTO> ventes = new ArrayList<>();

        for (Object obj : results) {
            Object[] row = (Object[]) obj;
            ventes.add(new VenteRecenteDTO(
                toLong(row[0]),
                (String) row[1],
                toLong(row[2]),
                (LocalDateTime) row[3],
                row[4] != null ? (String) row[4] : "N/A",
                row[5] != null ? (String) row[5] : "N/A",
                toInt(row[6]),
                (String) row[7]
            ));
        }

        return ventes;
    }

    @Override
    public List<TopProduitDTO> getTopProduits(Integer limit) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        List<TopProductDTO> topProducts = topProductsReportService
            .getTopProductsByRevenue(currentMonth, limit != null ? limit : 10);
        return mapper.toTopProduitDTOList(topProducts);
    }

    @Override
    public List<PerformanceVendeurDTO> getPerformanceVendeurs() {
        String query = """
            SELECT
                u.id,
                u.first_name || ' ' || u.last_name as vendeur_nom,
                COUNT(DISTINCT s.id) as nombre_ventes,
                COALESCE(SUM(s.sales_amount), 0) as montant_total,
                CASE WHEN COUNT(DISTINCT s.id) > 0
                     THEN COALESCE(SUM(s.sales_amount), 0) / COUNT(DISTINCT s.id)
                     ELSE 0
                END as ticket_moyen,
                CASE WHEN SUM(s.sales_amount) > 0
                     THEN (SUM(s.discount_amount) * 100.0 / SUM(s.sales_amount))
                     ELSE 0
                END as taux_remise
            FROM sales s
            JOIN app_user u ON u.id = s.seller_id
            WHERE DATE(s.created_at) = CURRENT_DATE
            AND s.statut = 'CLOSED'
            GROUP BY u.id, u.first_name, u.last_name
            ORDER BY montant_total DESC
            LIMIT 5
        """;

        Query q = entityManager.createNativeQuery(query);
        List<?> results = q.getResultList();
        List<PerformanceVendeurDTO> vendeurs = new ArrayList<>();

        for (Object obj : results) {
            Object[] row = (Object[]) obj;
            vendeurs.add(new PerformanceVendeurDTO(
                toLong(row[0]),
                (String) row[1],
                toInt(row[2]),
                toLong(row[3]),
                toLong(row[4]),
                toDouble(row[5])
            ));
        }

        return vendeurs;
    }

    @Override
    public List<AlerteCaisseDTO> getAlertes() {
        List<AlerteCaisseDTO> alertes = new ArrayList<>();

        // Check if cash register is open
        CaisseStatusDTO caisse = getCaisseStatus();
        if ("FERMEE".equals(caisse.etat())) {
            alertes.add(new AlerteCaisseDTO(
                "ATTENTION",
                "Caisse Fermée",
                "La caisse n'est pas encore ouverte. Veuillez l'ouvrir pour commencer les ventes.",
                LocalDateTime.now()
            ));
        }

        // Check for cash register discrepancy
        if (Math.abs(caisse.ecart()) > 5000) { // More than 5000 XOF difference
            alertes.add(new AlerteCaisseDTO(
                "URGENT",
                "Écart de Caisse Détecté",
                String.format("Un écart de %d XOF a été détecté. Veuillez vérifier.", caisse.ecart()),
                LocalDateTime.now()
            ));
        }

        // Check sales target
        VentesJourDTO ventes = getVentesJour();
        if (ventes.tauxAtteinte() != null && ventes.tauxAtteinte() < 50 && LocalDateTime.now().getHour() > 12) {
            alertes.add(new AlerteCaisseDTO(
                "ATTENTION",
                "Objectif Non Atteint",
                String.format("Objectif réalisé à %.1f%%. Intensifiez les ventes pour atteindre l'objectif.", ventes.tauxAtteinte()),
                LocalDateTime.now()
            ));
        }

        // Check for pending sales
        StatistiquesRapidesDTO stats = getStatistiquesRapides();
        if (stats.ventesEnCours() > 5) {
            alertes.add(new AlerteCaisseDTO(
                "INFO",
                "Ventes en Attente",
                String.format("%d vente(s) en cours de traitement.", stats.ventesEnCours()),
                LocalDateTime.now()
            ));
        }

        // If no alerts, add success message
        if (alertes.isEmpty()) {
            alertes.add(new AlerteCaisseDTO(
                "OK",
                "Tout est en Ordre",
                "Aucune alerte à signaler. Continuez le bon travail !",
                LocalDateTime.now()
            ));
        }

        return alertes;
    }



    private static int toInt(Object value) {
        if (value == null) return 0;
        return ((Number) value).intValue();
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        return ((Number) value).doubleValue();
    }

    @Override
    @Transactional
    public void ouvrirCaisse(Long montantOuverture) {
        // TODO: Implement cash register opening logic
        // This would interact with CashRegister entity
        throw new UnsupportedOperationException("To be implemented");
    }

    @Override
    @Transactional
    public void fermerCaisse() {
        // TODO: Implement cash register closing logic
        // This would interact with CashRegister entity
        throw new UnsupportedOperationException("To be implemented");
    }
}
