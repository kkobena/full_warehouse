package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.TiersPayantMapper;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TiersPayantService extends TiersPayantMapper {
    TiersPayantDto createFromDto(TiersPayantDto dto) throws GenericError;

    TiersPayantDto updateFromDto(TiersPayantDto dto) throws GenericError;

    void desable(Long id);

    void delete(Long id) throws GenericError;

    List<Pair> getModelFacture();

    List<Pair> getOrdreTrisFacture();

    Page<AchatTiersPayant> fetchAchatTiersPayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable);


    List<TiersPayantAchat> fetchAchatTiersPayant(VenteRecordParamDTO venteRecordParam);
}
