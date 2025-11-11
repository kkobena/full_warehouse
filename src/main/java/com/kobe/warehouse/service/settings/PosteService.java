package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.service.settings.dto.PosteRecord;

import java.util.List;
import java.util.Optional;

public interface PosteService {
    List<PosteRecord> findAll();

    Optional<PosteRecord> findFirstByAddressOrName(String address, String name);

    void create(PosteRecord posteRecord);



    void delete(Integer id);

}
