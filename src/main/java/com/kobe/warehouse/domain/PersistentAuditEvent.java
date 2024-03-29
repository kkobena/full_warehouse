package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Persist AuditEvent managed by the Spring Boot actuator.
 *
 * @see org.springframework.boot.actuate.audit.AuditEvent
 */
@Getter
@Entity
@Table(name = "persistent_audit_event")
public class PersistentAuditEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "event_id")
  private Long id;

  @NotNull
  @Column(nullable = false)
  private String principal;

  @Column(name = "event_date")
  private Instant auditEventDate;

  @Column(name = "event_type")
  private String auditEventType;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "value")
  @CollectionTable(name = "persistent_audit_evt_data", joinColumns = @JoinColumn(name = "event_id"))
  private Map<String, String> data = new HashMap<>();

  public void setId(Long id) {
    this.id = id;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public void setAuditEventDate(Instant auditEventDate) {
    this.auditEventDate = auditEventDate;
  }

  public void setAuditEventType(String auditEventType) {
    this.auditEventType = auditEventType;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PersistentAuditEvent)) {
      return false;
    }
    return id != null && id.equals(((PersistentAuditEvent) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  // prettier-ignore
  @Override
  public String toString() {
    return "PersistentAuditEvent{"
        + "principal='"
        + principal
        + '\''
        + ", auditEventDate="
        + auditEventDate
        + ", auditEventType='"
        + auditEventType
        + '\''
        + '}';
  }
}
