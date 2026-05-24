package com.kobe.warehouse.config;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "pharma-smart-log", ignoreUnknownFields = false)
public class LogProperties {

    private final Async async = new Async();
    private final Http http = new Http();
    private final Mail mail = new Mail();
    private final Security security = new Security();
    private final ApiDocs apiDocs = new ApiDocs();
    private final CorsConfiguration cors = new CorsConfiguration();
    private final Registry registry = new Registry();

    private final AuditEvents auditEvents = new AuditEvents();

    public Async getAsync() {
        return this.async;
    }

    public Http getHttp() {
        return this.http;
    }

    public Mail getMail() {
        return this.mail;
    }

    public Registry getRegistry() {
        return this.registry;
    }

    public Security getSecurity() {
        return this.security;
    }

    public ApiDocs getApiDocs() {
        return this.apiDocs;
    }

    public CorsConfiguration getCors() {
        return this.cors;
    }

    public AuditEvents getAuditEvents() {
        return this.auditEvents;
    }

    public static class Async {

        private int corePoolSize = 2;
        private int maxPoolSize = 50;
        private int queueCapacity = 10000;

        public int getCorePoolSize() {
            return this.corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return this.maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return this.queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Http {

        private final Http.Cache cache = new Http.Cache();

        public Http.Cache getCache() {
            return this.cache;
        }

        public static class Cache {

            public int timeToLiveInDays = 1461;

            public int getTimeToLiveInDays() {
                return this.timeToLiveInDays;
            }

            public void setTimeToLiveInDays(int timeToLiveInDays) {
                this.timeToLiveInDays = timeToLiveInDays;
            }
        }
    }

    public static class Mail {

        private boolean enabled = false;
        private String from = "";
        private String baseUrl = "";

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return this.from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getBaseUrl() {
            return this.baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Security {

        private final Security.ClientAuthorization clientAuthorization = new Security.ClientAuthorization();
        private final Security.Authentication authentication = new Security.Authentication();
        private final Security.RememberMe rememberMe = new Security.RememberMe();
        private final Security.OAuth2 oauth2 = new Security.OAuth2();
        private String contentSecurityPolicy =
            "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; img-src 'self' data:; font-src 'self' https://fonts.gstatic.com data:; connect-src 'self' http://localhost:* http://127.0.0.1:* ws://localhost:* ws://127.0.0.1:* ";

        public Security.ClientAuthorization getClientAuthorization() {
            return this.clientAuthorization;
        }

        public Security.Authentication getAuthentication() {
            return this.authentication;
        }

        public Security.RememberMe getRememberMe() {
            return this.rememberMe;
        }

        public Security.OAuth2 getOauth2() {
            return this.oauth2;
        }

        public String getContentSecurityPolicy() {
            return this.contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public static class ClientAuthorization {

            private String accessTokenUri;
            private String tokenServiceId;
            private String clientId;
            private String clientSecret;

            public ClientAuthorization() {
                this.accessTokenUri = com.kobe.warehouse.config.PharmaSmartDefaults.Security.ClientAuthorization.accessTokenUri;
                this.tokenServiceId = com.kobe.warehouse.config.PharmaSmartDefaults.Security.ClientAuthorization.tokenServiceId;
                this.clientId = com.kobe.warehouse.config.PharmaSmartDefaults.Security.ClientAuthorization.clientId;
                this.clientSecret = com.kobe.warehouse.config.PharmaSmartDefaults.Security.ClientAuthorization.clientSecret;
            }

            public String getAccessTokenUri() {
                return this.accessTokenUri;
            }

            public void setAccessTokenUri(String accessTokenUri) {
                this.accessTokenUri = accessTokenUri;
            }

            public String getTokenServiceId() {
                return this.tokenServiceId;
            }

            public void setTokenServiceId(String tokenServiceId) {
                this.tokenServiceId = tokenServiceId;
            }

            public String getClientId() {
                return this.clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getClientSecret() {
                return this.clientSecret;
            }

            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }
        }

        public static class Authentication {

            private final Security.Authentication.Jwt jwt = new Security.Authentication.Jwt();

            public Security.Authentication.Jwt getJwt() {
                return this.jwt;
            }

            public static class Jwt {

                private String secret;
                private String base64Secret;
                private long tokenValidityInSeconds;
                private long tokenValidityInSecondsForRememberMe;

                public Jwt() {
                    this.secret = com.kobe.warehouse.config.PharmaSmartDefaults.Security.Authentication.Jwt.secret;
                    this.base64Secret = com.kobe.warehouse.config.PharmaSmartDefaults.Security.Authentication.Jwt.base64Secret;
                    this.tokenValidityInSeconds = 1800L;
                    this.tokenValidityInSecondsForRememberMe = 2592000L;
                }

                public String getSecret() {
                    return this.secret;
                }

                public void setSecret(String secret) {
                    this.secret = secret;
                }

                public String getBase64Secret() {
                    return this.base64Secret;
                }

                public void setBase64Secret(String base64Secret) {
                    this.base64Secret = base64Secret;
                }

                public long getTokenValidityInSeconds() {
                    return this.tokenValidityInSeconds;
                }

                public void setTokenValidityInSeconds(long tokenValidityInSeconds) {
                    this.tokenValidityInSeconds = tokenValidityInSeconds;
                }

                public long getTokenValidityInSecondsForRememberMe() {
                    return this.tokenValidityInSecondsForRememberMe;
                }

                public void setTokenValidityInSecondsForRememberMe(long tokenValidityInSecondsForRememberMe) {
                    this.tokenValidityInSecondsForRememberMe = tokenValidityInSecondsForRememberMe;
                }
            }
        }

        public static class RememberMe {

            private @NotNull String key;

            public RememberMe() {
                this.key = com.kobe.warehouse.config.PharmaSmartDefaults.Security.RememberMe.key;
            }

            public String getKey() {
                return this.key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }

        public static class OAuth2 {

            private final List<String> audience = new ArrayList();

            public List<String> getAudience() {
                return Collections.unmodifiableList(this.audience);
            }

            public void setAudience(@NotNull List<String> audience) {
                this.audience.addAll(audience);
            }
        }
    }

    public static class ApiDocs {

        private String title = "Application API";
        private String description = "API documentation";
        private String version = "0.0.1";
        private String termsOfServiceUrl;
        private String contactName;
        private String contactUrl;
        private String contactEmail;
        private String license;
        private String licenseUrl;
        private String[] defaultIncludePattern;
        private String[] managementIncludePattern;
        private ApiDocs.Server[] servers;

        public ApiDocs() {
            this.termsOfServiceUrl = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.termsOfServiceUrl;
            this.contactName = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.contactName;
            this.contactUrl = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.contactUrl;
            this.contactEmail = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.contactEmail;
            this.license = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.license;
            this.licenseUrl = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.licenseUrl;
            this.defaultIncludePattern = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.defaultIncludePattern;
            this.managementIncludePattern = com.kobe.warehouse.config.PharmaSmartDefaults.ApiDocs.managementIncludePattern;
            this.servers = new ApiDocs.Server[0];
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getTermsOfServiceUrl() {
            return this.termsOfServiceUrl;
        }

        public void setTermsOfServiceUrl(String termsOfServiceUrl) {
            this.termsOfServiceUrl = termsOfServiceUrl;
        }

        public String getContactName() {
            return this.contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactUrl() {
            return this.contactUrl;
        }

        public void setContactUrl(String contactUrl) {
            this.contactUrl = contactUrl;
        }

        public String getContactEmail() {
            return this.contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public String getLicense() {
            return this.license;
        }

        public void setLicense(String license) {
            this.license = license;
        }

        public String getLicenseUrl() {
            return this.licenseUrl;
        }

        public void setLicenseUrl(String licenseUrl) {
            this.licenseUrl = licenseUrl;
        }

        public String[] getDefaultIncludePattern() {
            return this.defaultIncludePattern;
        }

        public void setDefaultIncludePattern(String[] defaultIncludePattern) {
            this.defaultIncludePattern = defaultIncludePattern;
        }

        public String[] getManagementIncludePattern() {
            return this.managementIncludePattern;
        }

        public void setManagementIncludePattern(String[] managementIncludePattern) {
            this.managementIncludePattern = managementIncludePattern;
        }

        public ApiDocs.Server[] getServers() {
            return this.servers;
        }

        public void setServers(ApiDocs.Server[] servers) {
            this.servers = servers;
        }

        public static class Server {

            private String url;
            private String description;

            public String getUrl() {
                return this.url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getDescription() {
                return this.description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }
    }

    public static class Registry {

        private String password;

        public Registry() {
            this.password = com.kobe.warehouse.config.PharmaSmartDefaults.Registry.password;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AuditEvents {

        private int retentionPeriod = 30;

        public int getRetentionPeriod() {
            return this.retentionPeriod;
        }

        public void setRetentionPeriod(int retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }
    }
}
