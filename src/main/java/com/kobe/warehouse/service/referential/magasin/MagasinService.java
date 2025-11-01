package com.kobe.warehouse.service.referential.magasin;

import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.dto.MagasinDTO;
import java.util.List;
import java.util.Set;

public interface MagasinService {
    MagasinDTO save(MagasinDTO magasin);

    MagasinDTO currentUserMagasin();

    MagasinDTO findById(Long id);

    void delete(Long id);

    List<MagasinDTO> findAll(Set<TypeMagasin> types);
}
