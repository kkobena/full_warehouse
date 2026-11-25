-- Retire tout ce que `preparer.sql` et le parcours e2e d'ordonnance ont posé.
-- Les interactions suivent la version (ON DELETE CASCADE) ; les ordonnances emportent leurs lignes,
-- délivrances et rattachements de vente.
--
--   docker exec -i pharma_smart_postgres psql -U postgres -d pharma_smart -v ON_ERROR_STOP=1 < scripts/essai-ordonnance/nettoyer.sql
SET search_path = pharma_smart, public;

DELETE FROM vente_prescripteur WHERE prescripteur_id IN (SELECT id FROM prescripteur WHERE nom LIKE 'ESSAI E2E%');
DELETE FROM customer_traitement_chronique WHERE note = 'ESSAI E2E';
DELETE FROM alerte_sante_derogation WHERE alertes LIKE '%ESSAI-E2E%';
DELETE FROM ordonnance WHERE prescripteur_id IN (SELECT id FROM prescripteur WHERE nom LIKE 'ESSAI E2E%');
DELETE FROM prescripteur WHERE nom LIKE 'ESSAI E2E%';
DELETE FROM referentiel_interaction_version WHERE source = 'ESSAI-E2E';
