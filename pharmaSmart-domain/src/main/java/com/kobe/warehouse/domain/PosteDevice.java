package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

/**
 * Représente un périphérique (douchette, afficheur, imprimante) configuré sur un poste.
 * Un poste peut avoir plusieurs périphériques du même type (ex: plusieurs douchettes testées),
 * mais un seul est actif à la fois par type.
 */
@Entity
@Table(
    name = "poste_device",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"poste_id", "device_type", "port_name"})
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PosteDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @NotNull
    @Column(name = "port_name", nullable = false, length = 20)
    private String portName;

    @Column(name = "label", length = 100)
    private String label;

    @ColumnDefault("9600")
    @Column(name = "baud_rate")
    private Integer baudRate = 9600;

    @Column(name = "vid")
    private Integer vid;

    @Column(name = "pid")
    private Integer pid;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @ColumnDefault("false")
    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters & Setters (fluent) ---

    public Long getId() {
        return id;
    }

    public PosteDevice setId(Long id) {
        this.id = id;
        return this;
    }

    public Poste getPoste() {
        return poste;
    }

    public PosteDevice setPoste(Poste poste) {
        this.poste = poste;
        return this;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public PosteDevice setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getPortName() {
        return portName;
    }

    public PosteDevice setPortName(String portName) {
        this.portName = portName;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public PosteDevice setLabel(String label) {
        this.label = label;
        return this;
    }

    public Integer getBaudRate() {
        return baudRate;
    }

    public PosteDevice setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
        return this;
    }

    public Integer getVid() {
        return vid;
    }

    public PosteDevice setVid(Integer vid) {
        this.vid = vid;
        return this;
    }

    public Integer getPid() {
        return pid;
    }

    public PosteDevice setPid(Integer pid) {
        this.pid = pid;
        return this;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public PosteDevice setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }

    public String getProductName() {
        return productName;
    }

    public PosteDevice setProductName(String productName) {
        this.productName = productName;
        return this;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public PosteDevice setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public PosteDevice setActive(boolean active) {
        this.active = active;
        return this;
    }

    public LocalDateTime getLastConnectedAt() {
        return lastConnectedAt;
    }

    public PosteDevice setLastConnectedAt(LocalDateTime lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PosteDevice setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosteDevice that = (PosteDevice) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PosteDevice{" +
            "id=" + id +
            ", deviceType=" + deviceType +
            ", portName='" + portName + '\'' +
            ", active=" + active +
            '}';
    }
}

