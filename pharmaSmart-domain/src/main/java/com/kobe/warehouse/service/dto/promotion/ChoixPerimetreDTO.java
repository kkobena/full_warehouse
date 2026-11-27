package com.kobe.warehouse.service.dto.promotion;

import java.util.List;

/**
 * Les rayons du magasin et les familles qui portent au moins un article actif : de quoi remplir d'un
 * geste le périmètre d'une promotion.
 */
public record ChoixPerimetreDTO(List<Option> rayons, List<Option> familles) {

    /** @param articles le nombre d'articles actifs, affiché à côté du libellé */
    public record Option(Integer id, String libelle, long articles) {}
}
