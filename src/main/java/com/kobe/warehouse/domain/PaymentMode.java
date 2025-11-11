package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.internal.util.stereotypes.Lazy;

/**
 * A PaymentMode.
 */
@Entity
@Table(name = "payment_mode")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PaymentMode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String code;

    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;

    @NotNull
    @Column(name = "ordre_tri", nullable = false)
    private short order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_group", nullable = false,length = 15)
    private PaymentGroup group;

    @NotNull
    @Column(name = "enable", nullable = false)
    private boolean enable = true;
    @Column(name = "icon_url", length = 70)
    private String iconUrl;
    @Lazy
    @Lob
    @Column(name = "qr_code", columnDefinition = "BYTEA")
    private byte[] qrCode;

    public String getIconUrl() {
        return iconUrl;
    }

    public PaymentMode setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
        return this;
    }

    public short getOrder() {
        return order;
    }

    public PaymentMode setOrder(short order) {
        this.order = order;
        return this;
    }

    public byte[] getQrCode() {
        return qrCode;
    }

    public PaymentMode setQrCode(byte[] qrCode) {
        this.qrCode = qrCode;
        return this;
    }

    public boolean isEnable() {
        return enable;
    }

    public PaymentMode setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public PaymentMode libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PaymentMode code(String code) {
        this.code = code;
        return this;
    }

    public PaymentGroup getGroup() {
        return group;
    }

    public void setGroup(PaymentGroup group) {
        this.group = group;
    }

    public PaymentMode group(PaymentGroup group) {
        this.group = group;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentMode)) {
            return false;
        }
        return code != null && code.equals(((PaymentMode) o).code);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentMode{"
            + ", libelle='"
            + getLibelle()
            + "'"
            + ", code='"
            + getCode()
            + "'"
            + ", group='"
            + getGroup()
            + "'"
            + "}";
    }
}
