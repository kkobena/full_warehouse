package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ProduitCodeAlias;
import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import java.time.LocalDateTime;

/**
 * Code alternatif d'un produit, tel que présenté dans l'onglet « Codes alternatifs ».
 *
 * @param id        identifiant de l'alias, nécessaire pour le supprimer
 * @param produitId produit visé
 * @param code      code scanné mémorisé
 * @param codeType  nature du code
 * @param source    origine de la liaison
 * @param createdAt date de création, en UTC
 * @param createdBy login de l'auteur, null pour une reprise automatique
 */
public record ProduitCodeAliasDTO(
    Integer id,
    Integer produitId,
    String code,
    CodeAliasType codeType,
    CodeAliasSource source,
    LocalDateTime createdAt,
    String createdBy
) {
    public static ProduitCodeAliasDTO fromEntity(ProduitCodeAlias alias) {
        return new ProduitCodeAliasDTO(
            alias.getId(),
            alias.getProduit().getId(),
            alias.getCode(),
            alias.getCodeType(),
            alias.getSource(),
            alias.getCreatedAt(),
            alias.getCreatedBy()
        );
    }
}
