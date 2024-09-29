package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.TiersPayantMapper;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;

public interface TiersPayantService extends TiersPayantMapper {
    TiersPayantDto createFromDto(TiersPayantDto dto) throws GenericError;

    TiersPayantDto updateFromDto(TiersPayantDto dto) throws GenericError;

    void desable(Long id);

    void delete(Long id) throws GenericError;

    List<Pair> getModelFacture();

    List<Pair> getOrdreTrisFacture();
}
