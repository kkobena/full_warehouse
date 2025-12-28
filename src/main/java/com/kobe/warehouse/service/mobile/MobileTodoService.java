package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO.TodoItemDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO.TodoPriority;
import com.kobe.warehouse.service.dto.mobile.TodoType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile todo/priority actions list.
 */
@Service
@Transactional(readOnly = true)
public class MobileTodoService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileTodoService.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get prioritized todo list for mobile.
     *
     * @return Todo list with urgent, important, and normal items
     */
    public MobileTodoDTO getTodoList() {
        LOG.debug("Getting mobile todo list");

        List<TodoItemDTO> urgent = new ArrayList<>();

        // 1. Products in stock rupture (Urgent)
        urgent.addAll(getStockRuptureTodos());

        // 2. Overdue invoices > 90 days (Urgent)
        urgent.addAll(getOverdueInvoiceTodos(90));

        // 3. Products expiring < 90 days (Important)
        List<TodoItemDTO> important = new ArrayList<>(getExpiringProductTodos(90));

        // 4. Low stock products (Normal)
        List<TodoItemDTO> normal = new ArrayList<>(getLowStockTodos());

        return MobileTodoDTO.builder().urgent(urgent).important(important).normal(normal).build();
    }

    /**
     * Get all todo items as a flat list (without pagination).
     * Items are ordered by priority: URGENT first, then IMPORTANT, then NORMAL.
     *
     * @return Flat list of all todo items
     */
    public List<TodoItemDTO> getAllTodoItems() {
        return getAllTodoItems(0, Integer.MAX_VALUE);
    }

    /**
     * Get all todo items as a flat list with pagination.
     * Items are ordered by priority: URGENT first, then IMPORTANT, then NORMAL.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of todo items
     */
    public List<TodoItemDTO> getAllTodoItems(int page, int size) {
        LOG.debug("Getting all todo items with pagination: page={}, size={}", page, size);

        List<TodoItemDTO> allItems = new ArrayList<>();

        // Collect all items in priority order
        allItems.addAll(getStockRuptureTodos());
        allItems.addAll(getOverdueInvoiceTodos(90));
        allItems.addAll(getExpiringProductTodos(90));
        allItems.addAll(getLowStockTodos());

        // Apply pagination
        int fromIndex = page * size;
        if (fromIndex >= allItems.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + size, allItems.size());
        return new ArrayList<>(allItems.subList(fromIndex, toIndex));
    }

    /**
     * Get total count of all todo items.
     *
     * @return Total number of todo items
     */
    public int getTodoItemsCount() {
        int count = 0;
        count += getStockRuptureCount();
        count += getOverdueInvoicesCount(90);
        count += getExpiringProductsCount(90);
        count += getLowStockCount();
        return count;
    }

    /**
     * Get counts by priority.
     *
     * @return Map with counts for each priority
     */
    public TodoCountsDTO getTodoCounts() {
        int urgentCount = getStockRuptureCount() + getOverdueInvoicesCount(90);
        int importantCount = getExpiringProductsCount(90);
        int normalCount = getLowStockCount();
        return new TodoCountsDTO(urgentCount, importantCount, normalCount);
    }

    /**
     * DTO for todo counts by priority.
     */
    public record TodoCountsDTO(int urgent, int important, int normal) {
        public int total() {
            return urgent + important + normal;
        }
    }

    // =========================================================================
    // COUNT METHODS
    // =========================================================================

    private int getStockRuptureCount() {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                INNER JOIN stock_produit sp ON sp.produit_id = p.id
                WHERE p.status = :status
                GROUP BY p.id
                HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) = 0
            ) ruptures
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());
        return ((Number) query.getSingleResult()).intValue();
    }

    private int getOverdueInvoicesCount(int days) {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT ftp.id
                FROM facture_tiers_payant ftp
                INNER JOIN third_party_sale_line tpsl ON tpsl.facture_tiers_payant_id = ftp.id
                    AND tpsl.invoice_date = ftp.invoice_date
                WHERE ftp.statut IN (:notPaid, :partiallyPaid)
                  AND ftp.created < CURRENT_DATE - :days
                GROUP BY ftp.id, ftp.invoice_date, ftp.montant_regle
                HAVING COALESCE(SUM(tpsl.montant), 0) > COALESCE(ftp.montant_regle, 0)
            ) overdue
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("notPaid", InvoiceStatut.NOT_PAID.name());
        query.setParameter("partiallyPaid", InvoiceStatut.PARTIALLY_PAID.name());
        query.setParameter("days", days);
        return ((Number) query.getSingleResult()).intValue();
    }

    private int getExpiringProductsCount(int days) {
        String sql = """
            SELECT COUNT(DISTINCT l.id)
            FROM lot l
            INNER JOIN produit p ON l.produit_id = p.id
            WHERE l.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + :days
              AND l.current_quantity > 0
              AND p.status = :status
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("days", days);
        query.setParameter("status", Status.ENABLE.name());
        return ((Number) query.getSingleResult()).intValue();
    }

    private int getLowStockCount() {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                INNER JOIN stock_produit sp ON sp.produit_id = p.id
                WHERE p.status = :status
                  AND p.qty_seuil_mini > 0
                GROUP BY p.id, p.qty_seuil_mini
                HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) > 0
                   AND COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) <= p.qty_seuil_mini
            ) low_stock
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());
        return ((Number) query.getSingleResult()).intValue();
    }

    /**
     * Get todo items for products in stock rupture.
     * A product is in rupture when total stock across all storages is zero.
     */
    private List<TodoItemDTO> getStockRuptureTodos() {
        String sql = """
            SELECT
                p.id,
                p.libelle,
                fp.code_cip,
                f.id as fournisseur_id,
                f.libelle as fournisseur_name
            FROM produit p
            INNER JOIN stock_produit sp ON sp.produit_id = p.id
            LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            LEFT JOIN fournisseur f ON fp.fournisseur_id = f.id
            WHERE p.status = :status
            GROUP BY p.id, p.libelle, fp.code_cip, f.id, f.libelle
            HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) = 0
            ORDER BY p.libelle
            LIMIT 20
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TodoItemDTO> todos = new ArrayList<>();
        long idCounter = 1;
        TodoType todoType = TodoType.REORDER;

        for (Object[] row : results) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long supplierId = row[3] != null ? ((Number) row[3]).longValue() : null;

            todos.add(
                MobileTodoDTO.itemBuilder()
                    .id(idCounter++)
                    .type(todoType.getCode())
                    .title("Commander " + truncate(productName, 25))
                    .description("Rupture de stock")
                    .priority(TodoPriority.URGENT)
                    .icon(todoType.getIcon())
                    .actionLabel("Commander")
                    .actionType(todoType.getActionType())
                    .actionData(Map.of("productId", productId, "supplierId", supplierId != null ? supplierId : 0))
                    .relatedEntityId(productId)
                    .relatedEntityType("PRODUCT")
                    .relatedEntityName(productName)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        return todos;
    }

    /**
     * Get todo items for overdue invoices.
     * Uses third_party_sale_line to calculate invoice amounts.
     */
    private List<TodoItemDTO> getOverdueInvoiceTodos(int days) {
        String sql = """
            SELECT
                ftp.id,
                gtp.name as tiers_payant_name,
                gtp.telephone,
                COALESCE(SUM(tpsl.montant), 0) - COALESCE(ftp.montant_regle, 0) as montant_restant,
                (CURRENT_DATE - DATE(ftp.created)) as days_overdue
            FROM facture_tiers_payant ftp
            INNER JOIN groupe_tiers_payant gtp ON ftp.groupe_tiers_payant_id = gtp.id
            INNER JOIN third_party_sale_line tpsl ON tpsl.facture_tiers_payant_id = ftp.id
                AND tpsl.invoice_date = ftp.invoice_date
            WHERE ftp.statut IN (:notPaid, :partiallyPaid)
              AND ftp.created < CURRENT_DATE - :days
            GROUP BY ftp.id, ftp.invoice_date, gtp.name, gtp.telephone, ftp.montant_regle, ftp.created
            HAVING COALESCE(SUM(tpsl.montant), 0) > COALESCE(ftp.montant_regle, 0)
            ORDER BY days_overdue DESC
            LIMIT 10
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("notPaid", InvoiceStatut.NOT_PAID.name());
        query.setParameter("partiallyPaid", InvoiceStatut.PARTIALLY_PAID.name());
        query.setParameter("days", days);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TodoItemDTO> todos = new ArrayList<>();
        long idCounter = 1000;
        TodoType todoType = TodoType.CALL_CLIENT;

        for (Object[] row : results) {
            Long invoiceId = ((Number) row[0]).longValue();
            String tiersPayantName = (String) row[1];
            String phone = (String) row[2];
            long montantRestant = ((Number) row[3]).longValue();
            int daysOverdue = ((Number) row[4]).intValue();

            todos.add(
                MobileTodoDTO.itemBuilder()
                    .id(idCounter++)
                    .type(todoType.getCode())
                    .title("Relancer " + truncate(tiersPayantName, 20))
                    .description("Facture impayee depuis " + daysOverdue + " jours (" + formatAmount(montantRestant) + " F)")
                    .priority(TodoPriority.URGENT)
                    .icon(todoType.getIcon())
                    .actionLabel("Appeler")
                    .actionType(todoType.getActionType())
                    .actionData(Map.of("invoiceId", invoiceId, "phone", phone != null ? phone : "", "tiersPayantName", tiersPayantName))
                    .relatedEntityId(invoiceId)
                    .relatedEntityType("INVOICE")
                    .relatedEntityName(tiersPayantName)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        return todos;
    }

    /**
     * Get todo items for expiring products.
     * Uses current_quantity column from lot table.
     */
    private List<TodoItemDTO> getExpiringProductTodos(int days) {
        String sql = """
            SELECT
                l.id as lot_id,
                p.id as produit_id,
                p.libelle,
                l.num_lot,
                l.expiry_date,
                l.current_quantity,
                (l.expiry_date - CURRENT_DATE) as days_until_expiry
            FROM lot l
            INNER JOIN produit p ON l.produit_id = p.id
            WHERE l.expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + :days
              AND l.current_quantity > 0
              AND p.status = :status
            ORDER BY l.expiry_date
            LIMIT 15
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("days", days);
        query.setParameter("status", Status.ENABLE.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TodoItemDTO> todos = new ArrayList<>();
        long idCounter = 2000;
        TodoType todoType = TodoType.CREATE_DISCOUNT;

        for (Object[] row : results) {
            Long lotId = ((Number) row[0]).longValue();
            Long productId = ((Number) row[1]).longValue();
            String productName = (String) row[2];
            int quantity = ((Number) row[5]).intValue();
            int daysUntilExpiry = ((Number) row[6]).intValue();

            TodoPriority priority = daysUntilExpiry <= 30 ? TodoPriority.URGENT : TodoPriority.IMPORTANT;

            todos.add(
                MobileTodoDTO.itemBuilder()
                    .id(idCounter++)
                    .type(todoType.getCode())
                    .title("Demarquer " + truncate(productName, 20))
                    .description("Expire dans " + daysUntilExpiry + " jours (" + quantity + " unites)")
                    .priority(priority)
                    .icon(todoType.getIcon())
                    .actionLabel("Creer promotion")
                    .actionType(todoType.getActionType())
                    .actionData(Map.of("productId", productId, "lotId", lotId, "quantity", quantity))
                    .relatedEntityId(productId)
                    .relatedEntityType("PRODUCT")
                    .relatedEntityName(productName)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        return todos;
    }

    /**
     * Get todo items for low stock products.
     * A product has low stock when total stock is below min threshold.
     */
    private List<TodoItemDTO> getLowStockTodos() {
        String sql = """
            SELECT
                p.id,
                p.libelle,
                COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) as total_quantity,
                p.qty_seuil_mini,
                f.id as fournisseur_id,
                f.libelle as fournisseur_name
            FROM produit p
            INNER JOIN stock_produit sp ON sp.produit_id = p.id
            LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            LEFT JOIN fournisseur f ON fp.fournisseur_id = f.id
            WHERE p.status = :status
              AND p.qty_seuil_mini > 0
            GROUP BY p.id, p.libelle, p.qty_seuil_mini, f.id, f.libelle
            HAVING COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) > 0
               AND COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0) <= p.qty_seuil_mini
            ORDER BY (COALESCE(SUM(sp.qty_stock), 0) + COALESCE(SUM(sp.qty_ug), 0))::float / NULLIF(p.qty_seuil_mini, 0)
            LIMIT 15
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("status", Status.ENABLE.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TodoItemDTO> todos = new ArrayList<>();
        long idCounter = 3000;
        TodoType todoType = TodoType.REORDER;

        for (Object[] row : results) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            int currentStock = ((Number) row[2]).intValue();
            int minThreshold = ((Number) row[3]).intValue();
            Long supplierId = row[4] != null ? ((Number) row[4]).longValue() : null;

            todos.add(
                MobileTodoDTO.itemBuilder()
                    .id(idCounter++)
                    .type(todoType.getCode())
                    .title("Reapprovisionner " + truncate(productName, 18))
                    .description("Stock: " + currentStock + "/" + minThreshold + " (seuil mini)")
                    .priority(TodoPriority.NORMAL)
                    .icon("package-variant-closed")
                    .actionLabel("Commander")
                    .actionType(todoType.getActionType())
                    .actionData(
                        Map.of(
                            "productId",
                            productId,
                            "supplierId",
                            supplierId != null ? supplierId : 0,
                            "suggestedQuantity",
                            Math.max(minThreshold * 2 - currentStock, minThreshold)
                        )
                    )
                    .relatedEntityId(productId)
                    .relatedEntityType("PRODUCT")
                    .relatedEntityName(productName)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        return todos;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatAmount(long amount) {
        return String.format("%,d", amount).replace(",", " ");
    }
}
