## Phase 2 : Optimisation Opérationnelle
**Durée estimée : 3-4 sprints | Priorité : Important**

### 🎯 Objectif
Ajouter des rapports pour optimiser la gestion des stocks, des ventes et des fournisseurs.

### Rapports à Implémenter

#### 2.1 Analyse des Ventes
**Priorité : P1 - Important**

**Rapports :**
- **Top produits vendus** (Top 20 par quantité et CA)
  - Filtres : période, type de vente, catégorie
- **Produits à rotation lente** (< 5 ventes/mois)
- **Panier moyen** (évolution par période)
- **Taux de remise moyen** par type de vente

**Vue matérialisée pour performance :**
```sql
CREATE MATERIALIZED VIEW mv_produits_top_ventes AS
SELECT
    p.id,
    p.libelle,
    p.code_cip,
    COUNT(DISTINCT sl.sale_id) as nb_ventes,
    SUM(sl.quantity_sold) as quantite_vendue,
    SUM(sl.sales_amount) as ca_genere,
    AVG(sl.sales_amount / sl.quantity_sold) as prix_moyen
FROM produit p
INNER JOIN sales_line sl ON p.id = sl.produit_id
INNER JOIN sales s ON sl.sale_id = s.id
WHERE s.sale_date >= CURRENT_DATE - INTERVAL '30 days'
  AND s.statut = 'CLOSED'
GROUP BY p.id, p.libelle, p.code_cip
ORDER BY ca_genere DESC;
```

**Composant Angular :**
```typescript
@Component({
  selector: 'jhi-top-products-report',
  template: `
    <p-table [value]="products" [paginator]="true" [rows]="20">
      <ng-template #header>
        <tr>
          <th>Produit</th>
          <th>Qté vendue</th>
          <th>CA généré</th>
          <th>Nb ventes</th>
        </tr>
      </ng-template>
      <ng-template #body let-product>
        <tr>
          <td>{{ product.libelle }}</td>
          <td>{{ product.quantiteVendue }}</td>
          <td>{{ product.caGenere | currency:'XOF' }}</td>
          <td>{{ product.nbVentes }}</td>
        </tr>
      </ng-template>
    </p-table>
  `
})
export class TopProductsReportComponent implements OnInit {
  products: ProductSalesDTO[] = [];

  ngOnInit() {
    this.reportService.getTopProducts({ period: 'MONTH', limit: 20 })
      .subscribe(data => this.products = data);
  }
}
```

---

#### 2.2 Gestion de Stock Avancée
**Priorité : P1 - Important**

**Rapports :**
- **Valeur totale du stock** (prix d'achat vs prix de vente)
- **Taux de rotation des stocks** par catégorie
- **Produits en surstockage** (stock > seuil maximum)
- **Historique des ajustements** (inventaires, casse, vol)

**KPIs à calculer :**
```sql
-- Taux de rotation stock (formule : CA / Stock moyen)
SELECT
    c.libelle as categorie,
    SUM(sp.stock_quantity * sp.cost_amount) as valeur_stock,
    SUM(ventes_30j.ca) as ca_30j,
    CASE
        WHEN SUM(sp.stock_quantity * sp.cost_amount) > 0
        THEN ROUND((SUM(ventes_30j.ca) / SUM(sp.stock_quantity * sp.cost_amount)) * 12, 2)
        ELSE 0
    END as taux_rotation_annuel
FROM categorie c
INNER JOIN produit p ON c.id = p.categorie_id
INNER JOIN stock_produit sp ON p.id = sp.produit_id
LEFT JOIN (
    SELECT
        sl.produit_id,
        SUM(sl.sales_amount) as ca
    FROM sales_line sl
    INNER JOIN sales s ON sl.sale_id = s.id
    WHERE s.sale_date >= CURRENT_DATE - 30
    GROUP BY sl.produit_id
) ventes_30j ON p.id = ventes_30j.produit_id
GROUP BY c.id, c.libelle
ORDER BY taux_rotation_annuel DESC;
```

---

#### 2.3 Performance Fournisseurs
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Volume d'achats par fournisseur** (30 derniers jours, 12 derniers mois)
- **Délai moyen de livraison**
- **Taux de conformité des livraisons**
- **Historique des prix** (évolution par produit)

**Service Java :**
```java
@Service
public class FournisseurReportService {

    public List<FournisseurPerformanceDTO> getPerformanceReport(
        LocalDate startDate,
        LocalDate endDate
    ) {
        return fournisseurRepository.findPerformanceMetrics(startDate, endDate);
    }

    // Calcul délai moyen de livraison
    public Map<Long, Double> getAverageDeliveryTime(LocalDate startDate, LocalDate endDate) {
        List<Commande> commandes = commandeRepository
            .findByCreatedDateBetween(startDate, endDate);

        return commandes.stream()
            .filter(c -> c.getStatut() == CommandeStatut.RECEIVED)
            .collect(Collectors.groupingBy(
                c -> c.getFournisseur().getId(),
                Collectors.averagingLong(c ->
                    ChronoUnit.DAYS.between(c.getCreatedDate(), c.getReceivedDate())
                )
            ));
    }
}
```

---

#### 2.4 Analyse Clients
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Segmentation clients** (actifs, inactifs, nouveaux)
- **Clients fidèles** (> 10 achats/an)
- **Clients à risque** (inactifs > 90 jours)
- **CA moyen par client**

**Segmentation RFM (Récence, Fréquence, Montant) :**
```sql
WITH customer_metrics AS (
    SELECT
        c.id,
        c.first_name || ' ' || c.last_name as nom_complet,
        COUNT(DISTINCT s.id) as nb_achats,
        SUM(s.sales_amount) as ca_total,
        MAX(s.sale_date) as derniere_vente,
        CURRENT_DATE - MAX(s.sale_date) as jours_depuis_derniere_vente
    FROM customer c
    LEFT JOIN sales s ON c.id = s.customer_id
    WHERE s.sale_date >= CURRENT_DATE - INTERVAL '1 year'
    GROUP BY c.id
)
SELECT
    *,
    CASE
        WHEN jours_depuis_derniere_vente <= 30 THEN 'Actif'
        WHEN jours_depuis_derniere_vente BETWEEN 31 AND 90 THEN 'Risque'
        ELSE 'Inactif'
    END as segment,
    CASE
        WHEN nb_achats >= 10 THEN 'Fidèle'
        WHEN nb_achats >= 5 THEN 'Régulier'
        ELSE 'Occasionnel'
    END as fidelite
FROM customer_metrics
ORDER BY ca_total DESC;
```

---

### 📦 Livrables Phase 2

- ✅ Rapport top produits avec filtres avancés
- ✅ Dashboard stock avec taux de rotation
- ✅ Tableau de bord performance fournisseurs
- ✅ Segmentation client RFM
- ✅ Exports Excel pour analyse approfondie

---
