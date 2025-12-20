package com.kobe.warehouse.service.dashboard.impl;

import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dashboard.VendeurDashboardService;
import com.kobe.warehouse.service.dto.vendeur.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VendeurDashboardServiceImpl implements VendeurDashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public VendeurDashboardDTO getDashboardData() {
        return new VendeurDashboardDTO(
            getMesPerformances(),
            getMesClients(),
            getVentesParType(),
            getCommission(),
            getTopProduits(10),
            getVentesRecentes(10),
            getOpportunites(),
            getObjectifsMensuels(),
            getClientsFideles(10)
        );
    }

    @Override
    public MesPerformancesDTO getMesPerformances() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            WITH vendeur_stats AS (
                SELECT
                    u.id,
                    u.login,
                    COALESCE(SUM(CASE WHEN DATE(s.created_at) = CURRENT_DATE THEN s.sales_amount ELSE 0 END), 0) as ca_jour,
                    COALESCE(SUM(CASE WHEN DATE(s.created_at) = CURRENT_DATE THEN s.sales_amount ELSE 0 END), 0) as total_ventes
                FROM app_user u
                LEFT JOIN sales s ON s.seller_id = u.id AND s.statut = 'CLOSED' AND s.to_ignore = false
                WHERE u.login = :userLogin
                GROUP BY u.id, u.login
            ),
            objectif AS (
                SELECT 100000 as objectif_jour
            ),
            ranking AS (
                SELECT
                    u.id,
                    u.login,
                    COALESCE(SUM(CASE WHEN DATE(s.created_at) = CURRENT_DATE THEN s.sales_amount ELSE 0 END), 0) as ca_jour,
                    RANK() OVER (ORDER BY COALESCE(SUM(CASE WHEN DATE(s.created_at) = CURRENT_DATE THEN s.sales_amount ELSE 0 END), 0) DESC) as rang
                FROM app_user u
                LEFT JOIN sales s ON s.seller_id = u.id AND s.statut = 'CLOSED' AND s.to_ignore = false
                INNER JOIN user_authority ua ON ua.user_id = u.id
                WHERE ua.authority_name = 'ROLE_VENDEUR'
                GROUP BY u.id, u.login
            )
            SELECT
                vs.ca_jour,
                o.objectif_jour,
                CASE WHEN o.objectif_jour > 0 THEN (vs.ca_jour * 100.0 / o.objectif_jour) ELSE 0 END as taux_atteinte,
                COALESCE(r.rang, 0) as rang,
                (SELECT COUNT(DISTINCT id) FROM ranking) as total_vendeurs,
                CASE
                    WHEN vs.ca_jour >= 500000 THEN 'PLATINE'
                    WHEN vs.ca_jour >= 300000 THEN 'OR'
                    WHEN vs.ca_jour >= 150000 THEN 'ARGENT'
                    ELSE 'BRONZE'
                END as badge,
                CASE
                    WHEN o.objectif_jour > 0 THEN ((vs.ca_jour - o.objectif_jour) * 100.0 / o.objectif_jour)
                    ELSE 0
                END as progression
            FROM vendeur_stats vs
            CROSS JOIN objectif o
            LEFT JOIN ranking r ON r.login = vs.login
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        Object[] result = (Object[]) q.getSingleResult();

        double caJour = ((BigDecimal) result[0]).doubleValue();
        double caObjectif = ((BigDecimal) result[1]).doubleValue();
        double tauxAtteinte = ((BigDecimal) result[2]).doubleValue();
        int rang = ((BigDecimal) result[3]).intValue();
        int totalVendeurs = ((Long) result[4]).intValue();
        String badge = (String) result[5];
        double progression = ((BigDecimal) result[6]).doubleValue();

        return new MesPerformancesDTO(caJour, caObjectif, tauxAtteinte, rang, totalVendeurs, badge, progression);
    }

    @Override
    public MesClientsDTO getMesClients() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            WITH date_ranges AS (
                SELECT
                    CURRENT_DATE as today,
                    DATE_TRUNC('month', CURRENT_DATE) as debut_mois
            ),
            client_stats AS (
                SELECT
                    COUNT(DISTINCT s.customer_id) FILTER (WHERE DATE(s.created_at) = dr.today) as clients_servis,
                    COUNT(DISTINCT c.id) FILTER (
                        WHERE DATE(c.created_at) >= dr.debut_mois
                        AND DATE(c.created_at) <= dr.today
                    ) as nouveaux_clients,
                    COUNT(DISTINCT s.customer_id) FILTER (
                        WHERE s.customer_id IN (
                            SELECT customer_id
                            FROM sales
                            WHERE seller_id = u.id
                            AND statut = 'CLOSED'
                            AND to_ignore = false
                            GROUP BY customer_id
                            HAVING COUNT(*) >= 3
                        )
                    ) as clients_fideles
                FROM app_user u
                LEFT JOIN sales s ON s.seller_id = u.id AND s.statut = 'CLOSED' AND s.to_ignore = false
                LEFT JOIN customer c ON c.id = s.customer_id
                CROSS JOIN date_ranges dr
                WHERE u.login = :userLogin
            )
            SELECT
                COALESCE(clients_servis, 0),
                COALESCE(nouveaux_clients, 0),
                COALESCE(clients_fideles, 0),
                CASE
                    WHEN clients_servis > 0
                    THEN (clients_fideles * 100.0 / clients_servis)
                    ELSE 0
                END as taux_fidelisation
            FROM client_stats
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        Object[] result = (Object[]) q.getSingleResult();

        int clientsServis = ((BigDecimal) result[0]).intValue();
        int nouveauxClients = ((BigDecimal) result[1]).intValue();
        int clientsFideles = ((BigDecimal) result[2]).intValue();
        double tauxFidelisation = ((BigDecimal) result[3]).doubleValue();

        return new MesClientsDTO(clientsServis, nouveauxClients, clientsFideles, tauxFidelisation);
    }

    @Override
    public VentesParTypeDTO getVentesParType() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            SELECT
                COALESCE(SUM(CASE WHEN s.type_prescription = 'PRESCRIPTION' THEN s.sales_amount ELSE 0 END), 0) as ordonnance,
                COALESCE(SUM(CASE WHEN s.type_prescription = 'CONSEIL' THEN s.sales_amount ELSE 0 END), 0) as conseil,
                COALESCE(SUM(CASE WHEN s.nature_vente = 'ASSURANCE' THEN s.sales_amount ELSE 0 END), 0) as parapharmacie,
                COALESCE(SUM(s.sales_amount), 0) as total
            FROM sales s
            INNER JOIN app_user u ON u.id = s.seller_id
            WHERE u.login = :userLogin
            AND DATE(s.created_at) = CURRENT_DATE
            AND s.statut = 'CLOSED'
            AND s.to_ignore = false
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        Object[] result = (Object[]) q.getSingleResult();

        double ordonnance = ((BigDecimal) result[0]).doubleValue();
        double conseil = ((BigDecimal) result[1]).doubleValue();
        double parapharmacie = ((BigDecimal) result[2]).doubleValue();
        double total = ((BigDecimal) result[3]).doubleValue();

        return new VentesParTypeDTO(ordonnance, conseil, parapharmacie, total);
    }

    @Override
    public CommissionDTO getCommission() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            WITH commission_config AS (
                SELECT 2.5 as taux_commission
            )
            SELECT
                COALESCE(SUM(CASE WHEN DATE(s.created_at) = CURRENT_DATE THEN s.sales_amount ELSE 0 END), 0) * cc.taux_commission / 100 as montant_jour,
                COALESCE(SUM(CASE WHEN DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE) THEN s.sales_amount ELSE 0 END), 0) * cc.taux_commission / 100 as montant_mois,
                cc.taux_commission
            FROM sales s
            INNER JOIN app_user u ON u.id = s.seller_id
            CROSS JOIN commission_config cc
            WHERE u.login = :userLogin
            AND s.statut = 'CLOSED'
            AND s.to_ignore = false
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        Object[] result = (Object[]) q.getSingleResult();

        double montantJour = ((BigDecimal) result[0]).doubleValue();
        double montantMois = ((BigDecimal) result[1]).doubleValue();
        double tauxCommission = ((BigDecimal) result[2]).doubleValue();

        return new CommissionDTO(montantJour, montantMois, tauxCommission);
    }

    @Override
    public List<TopProduitVendeurDTO> getTopProduits(Integer limit) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            SELECT
                p.id,
                p.libelle,
                p.code_cip,
                SUM(sl.quantity_sold) as quantite_vendue,
                SUM(sl.sales_amount) as montant_total,
                SUM(sl.sales_amount - sl.cost_amount) as marge
            FROM sales_line sl
            INNER JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sale_date
            INNER JOIN app_user u ON u.id = s.seller_id
            INNER JOIN produit p ON p.id = sl.produit_id
            WHERE u.login = :userLogin
            AND DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE)
            AND s.statut = 'CLOSED'
            AND s.to_ignore = false
            GROUP BY p.id, p.libelle, p.code_cip
            ORDER BY montant_total DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        q.setParameter("limit", limit != null ? limit : 10);

        List<Object[]> results = q.getResultList();
        List<TopProduitVendeurDTO> topProduits = new ArrayList<>();

        for (Object[] row : results) {
            Long produitId = ((BigInteger) row[0]).longValue();
            String produitLibelle = (String) row[1];
            String codeCip = (String) row[2];
            int quantiteVendue = ((BigDecimal) row[3]).intValue();
            double montantTotal = ((BigDecimal) row[4]).doubleValue();
            double marge = ((BigDecimal) row[5]).doubleValue();

            topProduits.add(new TopProduitVendeurDTO(produitId, produitLibelle, codeCip, quantiteVendue, montantTotal, marge));
        }

        return topProduits;
    }

    @Override
    public List<VenteRecenteVendeurDTO> getVentesRecentes(Integer limit) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            SELECT
                s.id,
                s.created_at,
                COALESCE(c.first_name || ' ' || c.last_name, 'Anonyme') as client_nom,
                s.sales_amount,
                s.type_prescription
            FROM sales s
            INNER JOIN app_user u ON u.id = s.seller_id
            LEFT JOIN customer c ON c.id = s.customer_id
            WHERE u.login = :userLogin
            AND DATE(s.created_at) = CURRENT_DATE
            AND s.statut = 'CLOSED'
            AND s.to_ignore = false
            ORDER BY s.created_at DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        q.setParameter("limit", limit != null ? limit : 10);

        List<Object[]> results = q.getResultList();
        List<VenteRecenteVendeurDTO> ventesRecentes = new ArrayList<>();

        for (Object[] row : results) {
            Long saleId = ((BigInteger) row[0]).longValue();
            LocalDateTime dateVente = ((java.sql.Timestamp) row[1]).toLocalDateTime();
            String clientNom = (String) row[2];
            double montant = ((BigDecimal) row[3]).doubleValue();
            String typeVente = (String) row[4];

            ventesRecentes.add(new VenteRecenteVendeurDTO(saleId, dateVente, clientNom, montant, typeVente));
        }

        return ventesRecentes;
    }

    @Override
    public List<OpportuniteVenteDTO> getOpportunites() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            WITH client_abonnement AS (
                SELECT
                    'ABONNEMENT' as type,
                    'Clients à abonnement' as titre,
                    'Clients ayant un traitement régulier' as description,
                    COUNT(DISTINCT c.id) as nombre_clients,
                    SUM(s.sales_amount) * 1.2 as potentiel_ca
                FROM customer c
                INNER JOIN sales s ON s.customer_id = c.id
                INNER JOIN app_user u ON u.id = s.seller_id
                WHERE u.login = :userLogin
                AND s.statut = 'CLOSED'
                AND s.to_ignore = false
                AND DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE)
                GROUP BY c.id
                HAVING COUNT(*) >= 2
            ),
            vente_complementaire AS (
                SELECT
                    'COMPLEMENTAIRE' as type,
                    'Ventes complémentaires' as titre,
                    'Produits souvent achetés ensemble' as description,
                    5 as nombre_clients,
                    25000 as potentiel_ca
            ),
            fort_potentiel AS (
                SELECT
                    'FORT_POTENTIEL' as type,
                    'Clients à fort potentiel' as titre,
                    'Clients avec un panier moyen élevé' as description,
                    COUNT(DISTINCT c.id) as nombre_clients,
                    SUM(s.sales_amount) * 0.3 as potentiel_ca
                FROM customer c
                INNER JOIN sales s ON s.customer_id = c.id
                INNER JOIN app_user u ON u.id = s.seller_id
                WHERE u.login = :userLogin
                AND s.statut = 'CLOSED'
                AND s.to_ignore = false
                AND DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE)
                GROUP BY c.id
                HAVING AVG(s.sales_amount) > 50000
            )
            SELECT type, titre, description, nombre_clients, potentiel_ca FROM client_abonnement
            UNION ALL
            SELECT type, titre, description, nombre_clients, potentiel_ca FROM vente_complementaire
            UNION ALL
            SELECT type, titre, description, nombre_clients, potentiel_ca FROM fort_potentiel
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);

        List<Object[]> results = q.getResultList();
        List<OpportuniteVenteDTO> opportunites = new ArrayList<>();

        for (Object[] row : results) {
            String type = (String) row[0];
            String titre = (String) row[1];
            String description = (String) row[2];
            int nombreClients = ((BigDecimal) row[3]).intValue();
            double potentielCA = ((BigDecimal) row[4]).doubleValue();

            opportunites.add(new OpportuniteVenteDTO(type, titre, description, nombreClients, potentielCA));
        }

        return opportunites;
    }

    @Override
    public List<ObjectifMensuelDTO> getObjectifsMensuels() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            WITH ventes_mois AS (
                SELECT
                    COALESCE(SUM(s.sales_amount), 0) as ca_mois,
                    COUNT(DISTINCT s.customer_id) as clients_mois,
                    COUNT(DISTINCT s.id) as nombre_ventes
                FROM sales s
                INNER JOIN app_user u ON u.id = s.seller_id
                WHERE u.login = :userLogin
                AND DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE)
                AND s.statut = 'CLOSED'
                AND s.to_ignore = false
            )
            SELECT
                'Chiffre d''affaires mensuel' as libelle,
                vm.ca_mois as valeur_actuelle,
                2000000 as valeur_cible,
                'FCFA' as unite,
                CASE WHEN 2000000 > 0 THEN (vm.ca_mois * 100.0 / 2000000) ELSE 0 END as taux_atteinte
            FROM ventes_mois vm
            UNION ALL
            SELECT
                'Nombre de clients' as libelle,
                vm.clients_mois as valeur_actuelle,
                100 as valeur_cible,
                'clients' as unite,
                CASE WHEN 100 > 0 THEN (vm.clients_mois * 100.0 / 100) ELSE 0 END as taux_atteinte
            FROM ventes_mois vm
            UNION ALL
            SELECT
                'Nombre de ventes' as libelle,
                vm.nombre_ventes as valeur_actuelle,
                150 as valeur_cible,
                'ventes' as unite,
                CASE WHEN 150 > 0 THEN (vm.nombre_ventes * 100.0 / 150) ELSE 0 END as taux_atteinte
            FROM ventes_mois vm
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);

        List<Object[]> results = q.getResultList();
        List<ObjectifMensuelDTO> objectifs = new ArrayList<>();

        for (Object[] row : results) {
            String libelle = (String) row[0];
            double valeurActuelle = ((BigDecimal) row[1]).doubleValue();
            double valeurCible = ((BigDecimal) row[2]).doubleValue();
            String unite = (String) row[3];
            double tauxAtteinte = ((BigDecimal) row[4]).doubleValue();

            objectifs.add(new ObjectifMensuelDTO(libelle, valeurActuelle, valeurCible, unite, tauxAtteinte));
        }

        return objectifs;
    }

    @Override
    public List<ClientFideleDTO> getClientsFideles(Integer limit) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();

        String query = """
            SELECT
                c.id,
                c.first_name || ' ' || c.last_name as nom,
                CASE
                    WHEN SUM(s.sales_amount) > 500000 THEN 'VIP'
                    WHEN COUNT(s.id) >= 5 THEN 'FIDELE'
                    ELSE 'POTENTIEL'
                END as categorie,
                SUM(s.sales_amount) as montant_total,
                COUNT(s.id) as nombre_visites,
                MAX(DATE(s.created_at)) as derniere_visite
            FROM customer c
            INNER JOIN sales s ON s.customer_id = c.id
            INNER JOIN app_user u ON u.id = s.seller_id
            WHERE u.login = :userLogin
            AND DATE_TRUNC('month', s.created_at) = DATE_TRUNC('month', CURRENT_DATE)
            AND s.statut = 'CLOSED'
            AND s.to_ignore = false
            GROUP BY c.id, c.first_name, c.last_name
            ORDER BY montant_total DESC
            LIMIT :limit
        """;

        Query q = entityManager.createNativeQuery(query);
        q.setParameter("userLogin", userLogin);
        q.setParameter("limit", limit != null ? limit : 10);

        List<Object[]> results = q.getResultList();
        List<ClientFideleDTO> clients = new ArrayList<>();

        for (Object[] row : results) {
            Long clientId = ((BigInteger) row[0]).longValue();
            String nom = (String) row[1];
            String categorie = (String) row[2];
            double montantTotal = ((BigDecimal) row[3]).doubleValue();
            int nombreVisites = ((BigDecimal) row[4]).intValue();
            LocalDate derniereVisite = ((java.sql.Date) row[5]).toLocalDate();

            clients.add(new ClientFideleDTO(clientId, nom, categorie, montantTotal, nombreVisites, derniereVisite));
        }

        return clients;
    }
}
