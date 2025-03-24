package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;

import java.util.List;

public record ActivitySummaryRecord(ChiffreAffaireDTO chiffreAffaire, List<AchatTiersPayant> achatTiersPayants,
                                    List<ReglementTiersPayants> reglementTiersPayants,
                                    List<GroupeFournisseurAchat> groupeFournisseurAchats,String periode) {
}
