package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.TiersPayantMapper;
import com.kobe.warehouse.web.rest.errors.GenericError;

public interface TiersPayantService extends TiersPayantMapper {
    TiersPayantDto createFromDto(TiersPayantDto dto) throws GenericError;

    TiersPayantDto updateFromDto(TiersPayantDto dto) throws GenericError;

    void desable(Long id);

    void delete(Long id) throws GenericError;


}
