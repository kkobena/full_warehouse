package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.AuthorityService;
import com.kobe.warehouse.service.dto.AuthorityDTO;
import com.kobe.warehouse.service.dto.PrivillegesDTO;
import com.kobe.warehouse.service.dto.PrivillegesWrapperDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class AuthorityResource {

    private final AuthorityService authorityService;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public AuthorityResource(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @GetMapping(value = "/privilleges")
    public ResponseEntity<List<PrivillegesDTO>> getAllPrivilleges(
        @RequestParam(value = "search", required = false, defaultValue = "") String search
    ) {
        return ResponseEntity.ok(authorityService.fetchPrivilleges(search));
    }

    @GetMapping(value = "/authorities/all")
    public ResponseEntity<List<AuthorityDTO>> getAuthorities(
        @RequestParam(value = "search", required = false, defaultValue = "") String search
    ) {
        return ResponseEntity.ok(authorityService.fetch(search));
    }

    @GetMapping(value = "/privilleges/{role}")
    public ResponseEntity<PrivillegesWrapperDTO> getAllPrivillegesByRole(@PathVariable("role") String roleName) {
        return ResponseEntity.ok(authorityService.fetchPrivillegesByRole(roleName));
    }

    @PostMapping("/authorities/save")
    public ResponseEntity<Void> save(@Valid @RequestBody AuthorityDTO authorityDTO) {
        authorityService.save(authorityDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/authorities/delete/{name}")
    public ResponseEntity<Void> delete(@PathVariable("name") String name) {
        authorityService.delete(name);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "menu", name)).build();
    }

    @GetMapping(value = "/authorities/{role}")
    public ResponseEntity<AuthorityDTO> getOneRole(@PathVariable("role") String roleName) {
        return ResponseEntity.ok(authorityService.fetchOne(roleName));
    }

    @PutMapping("/authorities/associe")
    public ResponseEntity<Void> setPrivilleges(@Valid @RequestBody AuthorityDTO authorityDTO) {
        authorityService.setPrivilleges(authorityDTO);
        return ResponseEntity.ok().build();
    }
}
