package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PosteDevice;
import com.kobe.warehouse.domain.enumeration.DeviceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PosteDeviceRepository extends JpaRepository<PosteDevice, Long> {

    List<PosteDevice> findByPosteId(Integer posteId);

    List<PosteDevice> findByPosteIdAndDeviceType(Integer posteId, DeviceType deviceType);

    Optional<PosteDevice> findByPosteIdAndDeviceTypeAndActiveTrue(Integer posteId, DeviceType deviceType);

    Optional<PosteDevice> findByPosteIdAndPortName(Integer posteId, String portName);

    /**
     * Désactive tous les périphériques d'un type donné pour un poste,
     * afin de n'en activer qu'un seul ensuite.
     */
    @Modifying
    @Query("UPDATE PosteDevice d SET d.active = false WHERE d.poste.id = :posteId AND d.deviceType = :deviceType")
    void deactivateAllByPosteAndType(@Param("posteId") Integer posteId, @Param("deviceType") DeviceType deviceType);
}

