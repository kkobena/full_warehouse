# Phase 2 Implementation Plan - Optimisation Opérationnelle
## Pharma-Smart Reports Implementation

**Date:** 2025-01-26
**Status:** 🚀 IN PROGRESS
**Priority:** Important

---

## 📋 Phase 2 Objectives

Add reports to optimize stock management, sales analysis, supplier performance, and customer segmentation.

**Estimated Duration:** 3-4 sprints
**Priority Level:** Important (P1)

---

## 📊 Reports to Implement

### 1. Stock Valuation Report 💰
**Priority:** P1 - Critical
**Complexity:** Medium

**Features:**
- Total stock value (purchase price vs sales price)
- Value by category
- Value by storage location
- Margin potential calculation
- Stock aging analysis

**Backend Endpoints:**
```
GET /api/stock-valuation/summary
GET /api/stock-valuation/by-category
GET /api/stock-valuation/by-storage
GET /api/stock-valuation/export (PDF)
```

**Database:**
- May use materialized view for performance
- Join: produit, stock_produit, fournisseur_produit, categorie, storage

**Frontend:**
- Route: `/reports/stock-valuation`
- Summary cards: Total Value (purchase), Total Value (sales), Potential Margin
- Table with sorting/filtering by category
- Chart visualization (optional)

---

### 2. Stock Rotation Analysis 🔄
**Priority:** P1 - Important
**Complexity:** High

**Features:**
- Rotation rate by product and category
- Slow-moving products (<5 sales/month)
- Fast-moving products
- Overstock detection (stock > max threshold)
- Average days in stock

**Backend Endpoints:**
```
GET /api/stock-rotation/by-category
GET /api/stock-rotation/slow-moving
GET /api/stock-rotation/fast-moving
GET /api/stock-rotation/overstock
GET /api/stock-rotation/export (PDF)
```

**KPI Calculation:**
```
Rotation Rate = (CA over period) / (Average Stock Value)
Average Days in Stock = 365 / Rotation Rate
```

**Database:**
- Materialized view: mv_stock_rotation
- Complex queries with sales history and stock levels

**Frontend:**
- Route: `/reports/stock-rotation`
- Category-based rotation table
- Slow-moving products alert section
- Rotation rate charts

---

### 3. Supplier Performance Report 🚚
**Priority:** P1 - Important
**Complexity:** Medium

**Features:**
- Purchase volume by supplier (30 days, 12 months)
- Average delivery time
- Delivery conformity rate (received vs ordered)
- Price history and evolution
- Top suppliers ranking

**Backend Endpoints:**
```
GET /api/supplier-performance/summary
GET /api/supplier-performance/{supplierId}/details
GET /api/supplier-performance/delivery-times
GET /api/supplier-performance/conformity
GET /api/supplier-performance/export (PDF)
```

**Database:**
- Join: fournisseur, commande, order_line, reception
- Calculate: AVG(reception_date - commande_date) as delivery_time

**Frontend:**
- Route: `/reports/supplier-performance`
- Supplier ranking table
- Delivery metrics cards
- Performance trends chart

---

### 4. Customer Segmentation (RFM Analysis) 👥
**Priority:** P2 - Desirable
**Complexity:** High

**Features:**
- RFM segmentation (Recency, Frequency, Monetary)
- Active customers (last purchase < 30 days)
- At-risk customers (inactive 31-90 days)
- Inactive customers (> 90 days)
- Loyal customers (>10 purchases/year)
- Average CA per customer

**Backend Endpoints:**
```
GET /api/customer-segmentation/rfm
GET /api/customer-segmentation/active
GET /api/customer-segmentation/at-risk
GET /api/customer-segmentation/loyal
GET /api/customer-segmentation/export (PDF)
```

**RFM Scoring:**
```sql
Recency Score (1-5): Days since last purchase
Frequency Score (1-5): Number of purchases
Monetary Score (1-5): Total spent
RFM Segment = R_score * 100 + F_score * 10 + M_score
```

**Database:**
- Materialized view: mv_customer_rfm
- Refresh: Daily

**Frontend:**
- Route: `/reports/customer-segmentation`
- Segmentation matrix visualization
- Customer lists by segment
- RFM score distribution charts

---

## 🗄️ Database Changes (V1.1.7__reports_phase_2.sql)

### Materialized Views to Create

#### 1. `mv_stock_valuation`
```sql
- produit_id, libelle, code_cip
- stock_quantity
- purchase_price (cost_amount)
- sales_price (regular_price)
- total_purchase_value
- total_sales_value
- potential_margin
- category
- storage_location
```

#### 2. `mv_stock_rotation`
```sql
- produit_id, libelle, category
- stock_quantity
- ca_last_30_days, ca_last_12_months
- nb_sales_last_30_days
- rotation_rate
- avg_days_in_stock
- classification (fast/normal/slow)
```

#### 3. `mv_customer_rfm`
```sql
- customer_id, name
- last_purchase_date
- days_since_last_purchase (Recency)
- nb_purchases_last_year (Frequency)
- total_spent_last_year (Monetary)
- recency_score, frequency_score, monetary_score
- rfm_segment
- customer_classification (active/at-risk/inactive/loyal)
```

### Performance Indexes
```sql
- idx_commande_supplier_date
- idx_commande_statut_date
- idx_reception_commande_date
- idx_stock_produit_category
- idx_sales_customer_date
```

---

## 📁 Files to Create

### Backend (Java)

**DTOs (8 files):**
- `StockValuationDTO.java`
- `StockValuationSummaryDTO.java`
- `StockRotationDTO.java`
- `SlowMovingProductDTO.java`
- `SupplierPerformanceDTO.java`
- `SupplierDeliveryMetricsDTO.java`
- `CustomerSegmentationDTO.java`
- `CustomerRFMDTO.java`

**Services (8 files):**
- `StockValuationService.java` + Impl
- `StockRotationService.java` + Impl
- `SupplierPerformanceService.java` + Impl
- `CustomerSegmentationService.java` + Impl

**REST Controllers (4 files):**
- `StockValuationResource.java`
- `StockRotationResource.java`
- `SupplierPerformanceResource.java`
- `CustomerSegmentationResource.java`

**Database:**
- `V1.1.7__reports_phase_2.sql`

**Total Backend:** ~24 files

### Frontend (Angular)

**Models (4 files):**
- `stock-valuation.model.ts`
- `stock-rotation.model.ts`
- `supplier-performance.model.ts`
- `customer-segmentation.model.ts`

**Services (4 files):**
- `stock-valuation.service.ts`
- `stock-rotation.service.ts`
- `supplier-performance.service.ts`
- `customer-segmentation.service.ts`

**Components (12 files):**
- `stock-valuation.component.ts/html/scss`
- `stock-rotation.component.ts/html/scss`
- `supplier-performance.component.ts/html/scss`
- `customer-segmentation.component.ts/html/scss`

**Routing:**
- Update `reports.route.ts` with 4 new routes

**Total Frontend:** ~20 files

---

## 🎯 Implementation Order

### Sprint 1: Stock Reports (Week 1-2)
1. ✅ Create database migration V1.1.7
2. ✅ Implement Stock Valuation Report (backend + frontend)
3. ✅ Implement Stock Rotation Analysis (backend + frontend)
4. ✅ Test and validate

### Sprint 2: Supplier & Customer Reports (Week 3-4)
1. ✅ Implement Supplier Performance Report (backend + frontend)
2. ✅ Implement Customer Segmentation Report (backend + frontend)
3. ✅ Add PDF export for all reports
4. ✅ Test and validate

### Sprint 3: Polish & Optimization (Week 5)
1. ✅ Performance tuning
2. ✅ UI/UX improvements
3. ✅ Documentation update
4. ✅ User acceptance testing

---

## 🔐 Security

All Phase 2 endpoints will be secured with:
- **Stock Valuation:** `ADMIN`, `ROLE_RESPONSABLE_COMMANDE`
- **Stock Rotation:** `ADMIN`, `ROLE_RESPONSABLE_COMMANDE`
- **Supplier Performance:** `ADMIN`, `ROLE_RESPONSABLE_COMMANDE`
- **Customer Segmentation:** `ADMIN`

---

## 📊 Success Metrics

- ✅ All 4 reports functional
- ✅ Query performance < 500ms
- ✅ Frontend build successful
- ✅ All tests passing
- ✅ Documentation complete

---

## 🚀 Next Actions

1. Create V1.1.7 database migration
2. Start with Stock Valuation Report implementation
3. Proceed sequentially through reports
4. Test each report before moving to next

---

**Plan Created:** 2025-01-26
**Status:** Ready to implement
**Next:** Start with Stock Valuation Report
