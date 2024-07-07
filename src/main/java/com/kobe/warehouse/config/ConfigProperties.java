package com.kobe.warehouse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;
import tech.jhipster.config.JHipsterProperties.ApiDocs;
import tech.jhipster.config.JHipsterProperties.Async;
import tech.jhipster.config.JHipsterProperties.AuditEvents;
import tech.jhipster.config.JHipsterProperties.Cache;
import tech.jhipster.config.JHipsterProperties.ClientApp;
import tech.jhipster.config.JHipsterProperties.Database;
import tech.jhipster.config.JHipsterProperties.Gateway;
import tech.jhipster.config.JHipsterProperties.Http;
import tech.jhipster.config.JHipsterProperties.Logging;
import tech.jhipster.config.JHipsterProperties.Mail;
import tech.jhipster.config.JHipsterProperties.Registry;
import tech.jhipster.config.JHipsterProperties.Security;
import tech.jhipster.config.JHipsterProperties.Social;

@ConfigurationProperties(prefix = "jhipster", ignoreUnknownFields = false)
public class ConfigProperties {
  private final Async async = new Async();

  private final Http http = new Http();

  private final Database database = new Database();

  private final Cache cache = new Cache();

  private final Mail mail = new Mail();

  private final Security security = new Security();

  private final ApiDocs apiDocs = new ApiDocs();

  private final Logging logging = new Logging();

  private final CorsConfiguration cors = new CorsConfiguration();

  private final Social social = new Social();

  private final Gateway gateway = new Gateway();

  private final Registry registry = new Registry();

  private final ClientApp clientApp = new ClientApp();

  private final AuditEvents auditEvents = new AuditEvents();
}
