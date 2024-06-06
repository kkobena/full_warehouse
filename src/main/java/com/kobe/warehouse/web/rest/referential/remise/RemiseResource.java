package com.kobe.warehouse.web.rest.referential.remise;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.dto.RemiseDTO;
import com.kobe.warehouse.service.remise.RemiseService;
import com.kobe.warehouse.web.rest.proxy.RemiseResourceProxy;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing {@link PaymentMode}. */
@RestController
@RequestMapping("/api")
public class RemiseResource extends RemiseResourceProxy {

  public RemiseResource(RemiseService remiseService) {
    super(remiseService);
  }

  @GetMapping("/remises")
  public ResponseEntity<List<RemiseDTO>> getAll() {
    return super.getAll();
  }

  @PostMapping("/remises")
  public ResponseEntity<RemiseDTO> create(@Valid @RequestBody RemiseDTO remiseDTO)
      throws URISyntaxException {

    return super.create(remiseDTO);
  }

  @PutMapping("/remises")
  public ResponseEntity<RemiseDTO> update(@Valid @RequestBody RemiseDTO remiseDTO)
      throws URISyntaxException {
    return super.update(remiseDTO);
  }

  @GetMapping("/remises/{id}")
  public ResponseEntity<RemiseDTO> getOne(@PathVariable Long id) {
    return super.getOne(id);
  }

  @DeleteMapping("/remises/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    return super.delete(id);
  }

  @PutMapping("/remises/associer/{id}")
  public ResponseEntity<Void> associer(@PathVariable Long id, @RequestBody List<Long> produitIds) {
    return super.associer(id, produitIds);
  }

  @PutMapping("/remises/dissocier")
  public ResponseEntity<Void> dissocier(@RequestBody List<Long> produitIds) {
    return super.dissocier(produitIds);
  }

  @PutMapping("/remises/change-status")
  public ResponseEntity<RemiseDTO> changeStatus(@Valid @RequestBody RemiseDTO remiseDTO)
      throws URISyntaxException {
    return super.changeStatus(remiseDTO);
  }
}
