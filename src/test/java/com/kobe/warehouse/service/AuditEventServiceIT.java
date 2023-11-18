package com.kobe.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.PersistentAuditEvent;
import com.kobe.warehouse.repository.PersistenceAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.config.JHipsterProperties;

/** Integration tests for {@link AuditEventService}. */
@SpringBootTest(classes = WarehouseApp.class)
@Transactional
@Disabled
public class AuditEventServiceIT {

  @Autowired private AuditEventService auditEventService;

  @Autowired private PersistenceAuditEventRepository persistenceAuditEventRepository;

  @Autowired private JHipsterProperties jHipsterProperties;

  private PersistentAuditEvent auditEventOld;

  private PersistentAuditEvent auditEventWithinRetention;

  private PersistentAuditEvent auditEventNew;

  @BeforeEach
  public void init() {
    auditEventOld = new PersistentAuditEvent();
    /*auditEventOld.setAuditEventDate(Instant.now()
    .minusDays(jHipsterProperties.getAuditEvents().getRetentionPeriod() + 1));*/
    auditEventOld.setPrincipal("test-user-old");
    auditEventOld.setAuditEventType("test-type");

    auditEventWithinRetention = new PersistentAuditEvent();
    /* auditEventWithinRetention.setAuditEventDate(
        LocalDateTime.now()
            .minusDays(jHipsterProperties.getAuditEvents().getRetentionPeriod() - 1)
    );*/
    auditEventWithinRetention.setPrincipal("test-user-retention");
    auditEventWithinRetention.setAuditEventType("test-type");

    auditEventNew = new PersistentAuditEvent();
    //  auditEventNew.setAuditEventDate(LocalDateTime.now());
    auditEventNew.setPrincipal("test-user-new");
    auditEventNew.setAuditEventType("test-type");
  }

  @Test
  @Transactional
  public void verifyOldAuditEventsAreDeleted() {
    persistenceAuditEventRepository.deleteAll();
    persistenceAuditEventRepository.save(auditEventOld);
    persistenceAuditEventRepository.save(auditEventWithinRetention);
    persistenceAuditEventRepository.save(auditEventNew);

    persistenceAuditEventRepository.flush();
    auditEventService.removeOldAuditEvents();
    persistenceAuditEventRepository.flush();

    assertThat(persistenceAuditEventRepository.findAll().size()).isEqualTo(2);
    assertThat(persistenceAuditEventRepository.findByPrincipal("test-user-old")).isEmpty();
    assertThat(persistenceAuditEventRepository.findByPrincipal("test-user-retention")).isNotEmpty();
    assertThat(persistenceAuditEventRepository.findByPrincipal("test-user-new")).isNotEmpty();
  }
}
