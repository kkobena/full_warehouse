## Phase 3 : Intelligence Décisionnelle
**Durée estimée : 4-5 sprints | Priorité : Optimisation**

### 🎯 Objectif
Ajouter des analyses avancées pour la prise de décision stratégique.

### Rapports à Implémenter

#### 3.1 Analyse de Rentabilité
**Priorité : P1 - Important**

**Rapports :**
- **Marge brute globale et par produit**
- **Top 20 produits les plus rentables**
- **Produits à faible marge** (< 10%)
- **Ratio marge/volume** (matrice BCG)

**Calcul de marge :**
```sql
SELECT
    p.libelle,
    SUM(sl.quantity_sold) as qte_vendue,
    SUM(sl.sales_amount) as ca,
    SUM(sl.cost_amount) as cout_achat,
    SUM(sl.sales_amount - sl.cost_amount) as marge_brute,
    ROUND(
        (SUM(sl.sales_amount - sl.cost_amount) / NULLIF(SUM(sl.sales_amount), 0)) * 100,
        2
    ) as taux_marge_pct
FROM produit p
INNER JOIN sales_line sl ON p.id = sl.produit_id
INNER JOIN sales s ON sl.sale_id = s.id
WHERE s.sale_date BETWEEN :startDate AND :endDate
  AND s.statut = 'CLOSED'
GROUP BY p.id, p.libelle
HAVING SUM(sl.sales_amount) > 0
ORDER BY marge_brute DESC
LIMIT 20;
```

**Matrice BCG (Boston Consulting Group) :**
Classifier les produits en 4 quadrants :
- **Stars** : Forte marge + Forte rotation
- **Cash Cows** : Forte marge + Faible rotation
- **Question Marks** : Faible marge + Forte rotation
- **Dogs** : Faible marge + Faible rotation

**Visualisation Chart.js :**
```typescript
const bcgChartConfig = {
  type: 'scatter',
  data: {
    datasets: [{
      label: 'Stars',
      data: products.filter(p => p.margin > 20 && p.rotation > 6),
      backgroundColor: 'green'
    }, {
      label: 'Cash Cows',
      data: products.filter(p => p.margin > 20 && p.rotation <= 6),
      backgroundColor: 'blue'
    }, {
      label: 'Question Marks',
      data: products.filter(p => p.margin <= 20 && p.rotation > 6),
      backgroundColor: 'orange'
    }, {
      label: 'Dogs',
      data: products.filter(p => p.margin <= 20 && p.rotation <= 6),
      backgroundColor: 'red'
    }]
  },
  options: {
    scales: {
      x: { title: { display: true, text: 'Taux de rotation' } },
      y: { title: { display: true, text: 'Marge (%)' } }
    }
  }
};
```

---

#### 3.2 Analyse ABC (Pareto)
**Priorité : P1 - Important**

**Objectif :** Classifier les produits selon la loi 80/20
- **Classe A** : 80% du CA (produits stratégiques)
- **Classe B** : 15% du CA (produits importants)
- **Classe C** : 5% du CA (produits secondaires)

**Algorithme :**
```java
@Service
public class ABCAnalysisService {

    public List<ProductABCClassification> performABCAnalysis(
        LocalDate startDate,
        LocalDate endDate
    ) {
        // 1. Récupérer tous les produits avec leur CA
        List<ProductSalesDTO> products = salesRepository
            .findProductSalesByPeriod(startDate, endDate);

        // 2. Trier par CA décroissant
        products.sort(Comparator.comparing(
            ProductSalesDTO::getSalesAmount).reversed()
        );

        // 3. Calculer le CA total
        double totalCA = products.stream()
            .mapToDouble(ProductSalesDTO::getSalesAmount)
            .sum();

        // 4. Classifier les produits
        double cumulativeCA = 0;
        List<ProductABCClassification> result = new ArrayList<>();

        for (ProductSalesDTO product : products) {
            cumulativeCA += product.getSalesAmount();
            double cumulativePercent = (cumulativeCA / totalCA) * 100;

            String classe;
            if (cumulativePercent <= 80) {
                classe = "A";
            } else if (cumulativePercent <= 95) {
                classe = "B";
            } else {
                classe = "C";
            }

            result.add(new ProductABCClassification(
                product.getProductId(),
                product.getLibelle(),
                product.getSalesAmount(),
                cumulativePercent,
                classe
            ));
        }

        return result;
    }
}
```

**Endpoint REST :**
```java
@GetMapping("/api/reports/abc-analysis")
public ResponseEntity<List<ProductABCClassification>> getABCAnalysis(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate
) {
    return ResponseEntity.ok(abcAnalysisService.performABCAnalysis(startDate, endDate));
}
```

---

#### 3.3 Tableaux Comparatifs
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Comparaison CA année N vs N-1**
- **Comparaison mois vs mois** (évolution mensuelle)
- **Comparaison par type de vente** (VNO, VO, VE)

**Requête SQL optimisée :**
```sql
WITH current_year AS (
    SELECT
        EXTRACT(MONTH FROM sale_date) as mois,
        SUM(sales_amount) as ca
    FROM sales
    WHERE EXTRACT(YEAR FROM sale_date) = EXTRACT(YEAR FROM CURRENT_DATE)
      AND statut = 'CLOSED'
    GROUP BY EXTRACT(MONTH FROM sale_date)
),
previous_year AS (
    SELECT
        EXTRACT(MONTH FROM sale_date) as mois,
        SUM(sales_amount) as ca
    FROM sales
    WHERE EXTRACT(YEAR FROM sale_date) = EXTRACT(YEAR FROM CURRENT_DATE) - 1
      AND statut = 'CLOSED'
    GROUP BY EXTRACT(MONTH FROM sale_date)
)
SELECT
    cy.mois,
    cy.ca as ca_annee_courante,
    py.ca as ca_annee_precedente,
    cy.ca - py.ca as difference,
    ROUND(((cy.ca - py.ca) / NULLIF(py.ca, 0)) * 100, 2) as evolution_pct
FROM current_year cy
LEFT JOIN previous_year py ON cy.mois = py.mois
ORDER BY cy.mois;
```

---

#### 3.4 Rapports de Conformité
**Priorité : P2 - Souhaitable**

**Rapports réglementaires :**
- **Ventes de stupéfiants** (traçabilité obligatoire)
- **Ventes à prescription obligatoire**
- **Traçabilité des lots vendus**
- **Produits sous surveillance** (psychotropes)

**Service de conformité :**
```java
@Service
public class ComplianceReportService {

    public List<ControlledSubstanceSaleDTO> getControlledSubstanceSales(
        LocalDate startDate,
        LocalDate endDate
    ) {
        return salesRepository.findControlledSubstanceSales(
            startDate,
            endDate,
            List.of(
                TypePrescription.STUPEFIANT,
                TypePrescription.PSYCHOTROPE
            )
        );
    }

    // Export PDF avec Thymeleaf pour autorités
    public byte[] generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        List<ControlledSubstanceSaleDTO> sales =
            getControlledSubstanceSales(startDate, endDate);

        Context context = new Context();
        context.setVariable("sales", sales);
        context.setVariable("startDate", startDate);
        context.setVariable("endDate", endDate);

        String html = templateEngine.process("reports/compliance-report", context);
        return pdfService.generatePdf(html);
    }
}
```

---

### 📦 Livrables Phase 3

- ✅ Dashboard rentabilité avec matrice BCG
- ✅ Analyse ABC automatisée
- ✅ Tableaux comparatifs année N vs N-1
- ✅ Module de conformité réglementaire
- ✅ Exports PDF certifiés pour autorités

---
