package com.kobe.warehouse.web.rest.settings;

import com.kobe.warehouse.domain.enumeration.DeviceType;
import com.kobe.warehouse.service.settings.PosteDeviceService;
import com.kobe.warehouse.service.settings.dto.PosteDeviceRecord;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/postes/{posteId}/devices")
public class PosteDeviceResource {

    private final PosteDeviceService posteDeviceService;

    public PosteDeviceResource(PosteDeviceService posteDeviceService) {
        this.posteDeviceService = posteDeviceService;
    }

    @GetMapping
    public ResponseEntity<List<PosteDeviceRecord>> getAll(
        @PathVariable Integer posteId,
        @RequestParam(required = false) DeviceType type
    ) {
        List<PosteDeviceRecord> devices = type != null
            ? posteDeviceService.findByPosteAndType(posteId, type)
            : posteDeviceService.findByPoste(posteId);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/active")
    public ResponseEntity<PosteDeviceRecord> getActive(
        @PathVariable Integer posteId,
        @RequestParam DeviceType type
    ) {
        return ResponseUtil.wrapOrNotFound(posteDeviceService.findActiveDevice(posteId, type));
    }

    @PostMapping
    public ResponseEntity<PosteDeviceRecord> create(
        @PathVariable Integer posteId,
        @Valid @RequestBody PosteDeviceRecord record
    ) {
        return ResponseEntity.ok(posteDeviceService.save(record.withPosteId(posteId)));
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<PosteDeviceRecord> update(
        @PathVariable Integer posteId,
        @PathVariable Long deviceId,
        @Valid @RequestBody PosteDeviceRecord record
    ) {
        return ResponseEntity.ok(posteDeviceService.save(record.withIdAndPosteId(deviceId, posteId)));
    }

    @PutMapping("/{deviceId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Integer posteId, @PathVariable Long deviceId) {
        posteDeviceService.activate(deviceId);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> delete(@PathVariable Integer posteId, @PathVariable Long deviceId) {
        posteDeviceService.delete(deviceId);
        return ResponseEntity.noContent().build();
    }
}

