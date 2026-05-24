package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.service.settings.PosteService;
import com.kobe.warehouse.service.settings.dto.PosteRecord;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postes")
public class PosteResource {

    private final PosteService posteService;

    public PosteResource(PosteService posteService) {
        this.posteService = posteService;
    }

    @GetMapping("/current")
    public ResponseEntity<PosteRecord> getCurrentPoste(HttpServletRequest request) {
        // En mode Tauri, le backend tourne en local (127.0.0.1) : utiliser les headers
        // X-Poste-Ip / X-Poste-Hostname qui portent la vraie IP LAN et le hostname de la machine.
        // Sinon, fallback sur les valeurs réseau de la requête (mode navigateur distant).
        var address = resolveHeader(request, "X-Poste-Ip", request.getRemoteAddr());
        var name = resolveHeader(request, "X-Poste-Hostname", request.getRemoteHost());
        return ResponseUtil.wrapOrNotFound(posteService.findFirstByAddressOrName(address, name));
    }

    private static String resolveHeader(HttpServletRequest request, String header, String fallback) {
        var value = request.getHeader(header);
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    @GetMapping
    public ResponseEntity<List<PosteRecord>> fetchAll() {
        return ResponseEntity.ok().body(posteService.findAll());
    }

    @PostMapping
    public ResponseEntity<PosteRecord> create(@Valid @RequestBody PosteRecord posteRecord) {
        return ResponseEntity.ok().body(posteService.create(posteRecord));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        posteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
