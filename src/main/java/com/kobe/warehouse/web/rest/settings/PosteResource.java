package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.service.settings.PosteService;
import com.kobe.warehouse.service.settings.dto.PosteRecord;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/postes")
public class PosteResource {

    private final PosteService posteService;

    public PosteResource(PosteService posteService) {
        this.posteService = posteService;
    }

    @GetMapping("/current")
    public ResponseEntity<PosteRecord> getCurrentPoste(HttpServletRequest request) {
        var remoteAddr = request.getRemoteAddr();
        var remoteHost = request.getRemoteHost();
        return ResponseUtil.wrapOrNotFound(posteService.findFirstByAddressOrName(remoteAddr, remoteHost));
    }


    @GetMapping
    public ResponseEntity<List<PosteRecord>> fetchAll(
    ) {
        return ResponseEntity.ok().body(posteService.findAll());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody PosteRecord posteRecord) {
        posteService.create(posteRecord);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {

        posteService.delete(id);
        return ResponseEntity.noContent()
            .build();
    }
}
