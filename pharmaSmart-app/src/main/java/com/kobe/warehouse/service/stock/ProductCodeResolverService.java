package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.dto.ProductResolveResult;
import com.kobe.warehouse.service.dto.ProduitCodeAliasDTO;
import java.util.List;

/**
 * Retrouve un produit à partir de n'importe quel code scanné, et mémorise les codes que le
 * catalogue ne connaissait pas.
 *
 * <p>En Côte d'Ivoire les grossistes n'intègrent pas les codes du DataMatrix : le GTIN du
 * fabricant ne correspond à rien en base. La résolution interroge donc les codes du catalogue
 * (CIP et EAN fournisseur, EAN laboratoire) puis les codes alternatifs appris aux réceptions
 * précédentes.
 */
public interface ProductCodeResolverService {
    /**
     * Codes candidats extraits d'un scan, du plus sûr au plus large : CIP-13, EAN-13, GTIN-14.
     * Les doublons sont retirés, un DataMatrix CIP-13 portant le même code dans deux champs ne
     * provoquant qu'une seule recherche.
     */
    List<String> listerCodesCandidats(DataMatrixInfo parsed);

    /**
     * Produits portant l'un des codes candidats.
     *
     * <p>Liste vide : résolution manuelle. Un seul élément : association directe. Plusieurs :
     * l'utilisateur tranche, un même code pouvant viser plusieurs produits.
     */
    List<ProductResolveResult> resoudre(DataMatrixInfo parsed);

    /** Produits portant ce code précis, mêmes règles de retour que {@link #resoudre}. */
    List<ProductResolveResult> resoudreCode(String code);

    /**
     * Mémorise un code alternatif pour un produit. Idempotent : un code déjà associé à ce
     * produit est renvoyé tel quel, sans doublon ni erreur.
     */
    ProduitCodeAliasDTO enregistrerAlias(Integer produitId, String code, CodeAliasType type, CodeAliasSource source);

    /** Supprime un code alternatif. Sans effet s'il n'existe plus. */
    void supprimerAlias(Integer aliasId);

    /** Codes alternatifs d'un produit, du plus récent au plus ancien. */
    List<ProduitCodeAliasDTO> listerAlias(Integer produitId);

    /** Type déduit de la forme du code, pour éviter de le faire choisir à l'utilisateur. */
    CodeAliasType deduireType(String code);
}
