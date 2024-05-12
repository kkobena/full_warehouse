package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.dto.RemiseDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/** REST controller for managing {@link com.kobe.warehouse.domain.Remise}. */
public class RemiseResourceProxy {

  private final Logger log = LoggerFactory.getLogger(RemiseResourceProxy.class);

  public RemiseResourceProxy() {}

  public ResponseEntity<List<RemiseDTO>> getAll() {

    return ResponseEntity.ok().body(List.of());
  }
}
