-- Tracer l'envoi d'une commande au grossiste.


ALTER TABLE commande
  ADD COLUMN IF NOT EXISTS date_envoi    timestamp,
  ADD COLUMN IF NOT EXISTS canal_envoi   varchar(20),
  ADD COLUMN IF NOT EXISTS envoi_user_id integer;

COMMENT ON COLUMN commande.date_envoi IS
  'Horodatage de l''envoi au grossiste. NULL = brouillon, jamais transmis.';
COMMENT ON COLUMN commande.canal_envoi IS
  'Canal d''envoi (énumération CanalEnvoi) : EDI, MAIL, PDF, TELEPHONE, AUTRE.';
COMMENT ON COLUMN commande.envoi_user_id IS
  'Utilisateur qui a transmis la commande.';

ALTER TABLE commande
  ADD CONSTRAINT commande_envoi_user_fk FOREIGN KEY (envoi_user_id) REFERENCES app_user (id);

-- Le prédicat partiel sert la liste des brouillons et le compteur du tableau de bord, qui
-- interrogent tous deux `date_envoi IS NULL`. L'index est propagé aux partitions.
CREATE INDEX IF NOT EXISTS commande_non_envoyee_idx
  ON commande (fournisseur_id, order_date)
  WHERE date_envoi IS NULL;


UPDATE commande
   SET date_envoi  = order_date::timestamp,
       canal_envoi = 'AUTRE'
 WHERE date_envoi IS NULL
   AND receipt_type = 'ORDER'
   AND motif_bed IS NULL;
