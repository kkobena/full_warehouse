-- ===============================================
-- Add unique index for concurrent refresh of mv_abc_pareto_analysis
-- Version: 1.1.21
-- Description: Add missing unique index to mv_abc_pareto_analysis
--              to enable REFRESH MATERIALIZED VIEW CONCURRENTLY
--
-- Note: Single-row aggregate views (mv_pareto_summary, mv_profitability_summary)
--       are handled with non-concurrent refresh in MaterializedViewRefreshService
-- ===============================================

-- Add unique index to mv_abc_pareto_analysis
-- This allows concurrent refresh without locking the view
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_abc_pareto_unique
ON mv_abc_pareto_analysis(produit_id);

-- Add comment
COMMENT ON INDEX idx_mv_abc_pareto_unique IS 'Unique index required for concurrent refresh of mv_abc_pareto_analysis';
