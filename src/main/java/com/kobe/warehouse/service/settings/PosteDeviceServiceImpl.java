package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.domain.PosteDevice;
import com.kobe.warehouse.domain.enumeration.DeviceType;
import com.kobe.warehouse.repository.PosteDeviceRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.service.settings.dto.PosteDeviceRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PosteDeviceServiceImpl implements PosteDeviceService {

    private final PosteDeviceRepository posteDeviceRepository;
    private final PosteRepository posteRepository;

    public PosteDeviceServiceImpl(PosteDeviceRepository posteDeviceRepository, PosteRepository posteRepository) {
        this.posteDeviceRepository = posteDeviceRepository;
        this.posteRepository = posteRepository;
    }

    @Override
    public List<PosteDeviceRecord> findByPoste(Integer posteId) {
        return posteDeviceRepository.findByPosteId(posteId).stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public List<PosteDeviceRecord> findByPosteAndType(Integer posteId, DeviceType deviceType) {
        return posteDeviceRepository.findByPosteIdAndDeviceType(posteId, deviceType).stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public Optional<PosteDeviceRecord> findActiveDevice(Integer posteId, DeviceType deviceType) {
        return posteDeviceRepository.findByPosteIdAndDeviceTypeAndActiveTrue(posteId, deviceType)
            .map(this::toRecord);
    }

    @Override
    @Transactional
    public PosteDeviceRecord save(PosteDeviceRecord record) {
        Poste poste = posteRepository.getReferenceById(record.posteId());

        PosteDevice entity = record.id() != null
            ? posteDeviceRepository.getReferenceById(record.id())
            : new PosteDevice();

        entity.setPoste(poste)
            .setDeviceType(record.deviceType())
            .setPortName(record.portName())
            .setLabel(record.label())
            .setBaudRate(record.baudRate() != null ? record.baudRate() : 9600)
            .setVid(record.vid())
            .setPid(record.pid())
            .setManufacturer(record.manufacturer())
            .setProductName(record.productName())
            .setSerialNumber(record.serialNumber())
            .setActive(record.active());

        // Si c'est le premier de son type pour ce poste, l'activer automatiquement
        if (record.id() == null) {
            List<PosteDevice> existing = posteDeviceRepository
                .findByPosteIdAndDeviceType(record.posteId(), record.deviceType());
            if (existing.isEmpty()) {
                entity.setActive(true);
            }
        }

        // Si activé, désactiver les autres du même type
        if (entity.isActive()) {
            posteDeviceRepository.deactivateAllByPosteAndType(record.posteId(), record.deviceType());
        }

        entity = posteDeviceRepository.save(entity);
        return toRecord(entity);
    }

    @Override
    @Transactional
    public void activate(Long deviceId) {
        PosteDevice device = posteDeviceRepository.getReferenceById(deviceId);
        posteDeviceRepository.deactivateAllByPosteAndType(device.getPoste().getId(), device.getDeviceType());
        device.setActive(true);
        posteDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void delete(Long deviceId) {
        posteDeviceRepository.deleteById(deviceId);
    }

    private PosteDeviceRecord toRecord(PosteDevice entity) {
        return new PosteDeviceRecord(
            entity.getId(),
            entity.getPoste().getId(),
            entity.getDeviceType(),
            entity.getPortName(),
            entity.getLabel(),
            entity.getBaudRate(),
            entity.getVid(),
            entity.getPid(),
            entity.getManufacturer(),
            entity.getProductName(),
            entity.getSerialNumber(),
            entity.isActive(),
            entity.getLastConnectedAt()
        );
    }
}

