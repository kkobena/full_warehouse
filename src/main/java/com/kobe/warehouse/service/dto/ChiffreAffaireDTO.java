package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.Recette;
import com.kobe.warehouse.service.dto.records.ChiffreAffaireRecord;
import java.util.List;

public record ChiffreAffaireDTO(
    List<Recette> recettes,
    ChiffreAffaireRecord chiffreAffaire,
    ChiffreAffaireAchat achats,
    List<MouvementCaisse> mouvementCaisses
) {}
