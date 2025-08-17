package com.kobe.warehouse.config;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(
    prefix = "pharma-smart-log",
    ignoreUnknownFields = false
)
public class LogProperties {
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

    public Async getAsync() {
        return this.async;
    }

    public Http getHttp() {
        return this.http;
    }

    public Database getDatabase() {
        return this.database;
    }

    public Cache getCache() {
        return this.cache;
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

    public Logging getLogging() {
        return this.logging;
    }

    public CorsConfiguration getCors() {
        return this.cors;
    }

    public Social getSocial() {
        return this.social;
    }

    public Gateway getGateway() {
        return this.gateway;
    }

    public ClientApp getClientApp() {
        return this.clientApp;
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

    public static class Database {
        private final Database.Couchbase couchbase = new Database.Couchbase();

        public Database.Couchbase getCouchbase() {
            return this.couchbase;
        }

        public static class Couchbase {
            private String bucketName;
            private String scopeName;

            public String getBucketName() {
                return this.bucketName;
            }

            public Database.Couchbase setBucketName(String bucketName) {
                this.bucketName = bucketName;
                return this;
            }

            public String getScopeName() {
                return this.scopeName;
            }

            public Database.Couchbase setScopeName(String scopeName) {
                this.scopeName = scopeName;
                return this;
            }
        }
    }

    public static class Cache {
        private final Cache.Hazelcast hazelcast = new Cache.Hazelcast();
        private final Cache.Caffeine caffeine = new Cache.Caffeine();
        private final Cache.Ehcache ehcache = new Cache.Ehcache();
        private final Cache.Infinispan infinispan = new Cache.Infinispan();
        private final Cache.Memcached memcached = new Cache.Memcached();
        private final Cache.Redis redis = new Cache.Redis();

        public Cache.Hazelcast getHazelcast() {
            return this.hazelcast;
        }

        public Cache.Caffeine getCaffeine() {
            return this.caffeine;
        }

        public Cache.Ehcache getEhcache() {
            return this.ehcache;
        }

        public Cache.Infinispan getInfinispan() {
            return this.infinispan;
        }

        public Cache.Memcached getMemcached() {
            return this.memcached;
        }

        public Cache.Redis getRedis() {
            return this.redis;
        }

        public static class Hazelcast {
            private int timeToLiveSeconds = 3600;
            private int backupCount = 1;

            public int getTimeToLiveSeconds() {
                return this.timeToLiveSeconds;
            }

            public void setTimeToLiveSeconds(int timeToLiveSeconds) {
                this.timeToLiveSeconds = timeToLiveSeconds;
            }

            public int getBackupCount() {
                return this.backupCount;
            }

            public void setBackupCount(int backupCount) {
                this.backupCount = backupCount;
            }
        }

        public static class Caffeine {
            private int timeToLiveSeconds = 3600;
            private long maxEntries = 100L;

            public int getTimeToLiveSeconds() {
                return this.timeToLiveSeconds;
            }

            public void setTimeToLiveSeconds(int timeToLiveSeconds) {
                this.timeToLiveSeconds = timeToLiveSeconds;
            }

            public long getMaxEntries() {
                return this.maxEntries;
            }

            public void setMaxEntries(long maxEntries) {
                this.maxEntries = maxEntries;
            }
        }

        public static class Ehcache {
            private int timeToLiveSeconds = 3600;
            private long maxEntries = 100L;

            public int getTimeToLiveSeconds() {
                return this.timeToLiveSeconds;
            }

            public void setTimeToLiveSeconds(int timeToLiveSeconds) {
                this.timeToLiveSeconds = timeToLiveSeconds;
            }

            public long getMaxEntries() {
                return this.maxEntries;
            }

            public void setMaxEntries(long maxEntries) {
                this.maxEntries = maxEntries;
            }
        }

        public static class Infinispan {
            private final Cache.Infinispan.Local local = new Cache.Infinispan.Local();
            private final Cache.Infinispan.Distributed distributed = new Cache.Infinispan.Distributed();
            private final Cache.Infinispan.Replicated replicated = new Cache.Infinispan.Replicated();
            private String configFile = "default-configs/default-jgroups-tcp.xml";
            private boolean statsEnabled = false;

            public String getConfigFile() {
                return this.configFile;
            }

            public void setConfigFile(String configFile) {
                this.configFile = configFile;
            }

            public boolean isStatsEnabled() {
                return this.statsEnabled;
            }

            public void setStatsEnabled(boolean statsEnabled) {
                this.statsEnabled = statsEnabled;
            }

            public Cache.Infinispan.Local getLocal() {
                return this.local;
            }

            public Cache.Infinispan.Distributed getDistributed() {
                return this.distributed;
            }

            public Cache.Infinispan.Replicated getReplicated() {
                return this.replicated;
            }

            public static class Local {
                private long timeToLiveSeconds = 60L;
                private long maxEntries = 100L;

                public long getTimeToLiveSeconds() {
                    return this.timeToLiveSeconds;
                }

                public void setTimeToLiveSeconds(long timeToLiveSeconds) {
                    this.timeToLiveSeconds = timeToLiveSeconds;
                }

                public long getMaxEntries() {
                    return this.maxEntries;
                }

                public void setMaxEntries(long maxEntries) {
                    this.maxEntries = maxEntries;
                }
            }

            public static class Distributed {
                private long timeToLiveSeconds = 60L;
                private long maxEntries = 100L;
                private int instanceCount = 1;

                public long getTimeToLiveSeconds() {
                    return this.timeToLiveSeconds;
                }

                public void setTimeToLiveSeconds(long timeToLiveSeconds) {
                    this.timeToLiveSeconds = timeToLiveSeconds;
                }

                public long getMaxEntries() {
                    return this.maxEntries;
                }

                public void setMaxEntries(long maxEntries) {
                    this.maxEntries = maxEntries;
                }

                public int getInstanceCount() {
                    return this.instanceCount;
                }

                public void setInstanceCount(int instanceCount) {
                    this.instanceCount = instanceCount;
                }
            }

            public static class Replicated {
                private long timeToLiveSeconds = 60L;
                private long maxEntries = 100L;

                public long getTimeToLiveSeconds() {
                    return this.timeToLiveSeconds;
                }

                public void setTimeToLiveSeconds(long timeToLiveSeconds) {
                    this.timeToLiveSeconds = timeToLiveSeconds;
                }

                public long getMaxEntries() {
                    return this.maxEntries;
                }

                public void setMaxEntries(long maxEntries) {
                    this.maxEntries = maxEntries;
                }
            }
        }

        public static class Memcached {
            private final Cache.Memcached.Authentication authentication = new Cache.Memcached.Authentication();
            private boolean enabled = false;
            private String servers = "localhost:11211";
            private int expiration = 300;
            private boolean useBinaryProtocol = true;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getServers() {
                return this.servers;
            }

            public void setServers(String servers) {
                this.servers = servers;
            }

            public int getExpiration() {
                return this.expiration;
            }

            public void setExpiration(int expiration) {
                this.expiration = expiration;
            }

            public boolean isUseBinaryProtocol() {
                return this.useBinaryProtocol;
            }

            public void setUseBinaryProtocol(boolean useBinaryProtocol) {
                this.useBinaryProtocol = useBinaryProtocol;
            }

            public Cache.Memcached.Authentication getAuthentication() {
                return this.authentication;
            }

            public static class Authentication {
                private boolean enabled = false;
                private String username;
                private String password;

                public boolean isEnabled() {
                    return this.enabled;
                }

                public Cache.Memcached.Authentication setEnabled(boolean enabled) {
                    this.enabled = enabled;
                    return this;
                }

                public String getUsername() {
                    return this.username;
                }

                public Cache.Memcached.Authentication setUsername(String username) {
                    this.username = username;
                    return this;
                }

                public String getPassword() {
                    return this.password;
                }

                public Cache.Memcached.Authentication setPassword(String password) {
                    this.password = password;
                    return this;
                }
            }
        }

        public static class Redis {
            private String[] server;
            private int expiration;
            private boolean cluster;
            private int connectionPoolSize;
            private int connectionMinimumIdleSize;
            private int subscriptionConnectionPoolSize;
            private int subscriptionConnectionMinimumIdleSize;

            public Redis() {
                this.server = com.kobe.warehouse.config.PharmaSmartDefaults.Cache.Redis.server;
                this.expiration = 300;
                this.cluster = false;
                this.connectionPoolSize = 64;
                this.connectionMinimumIdleSize = 24;
                this.subscriptionConnectionPoolSize = 50;
                this.subscriptionConnectionMinimumIdleSize = 1;
            }

            public String[] getServer() {
                return this.server;
            }

            public void setServer(String[] server) {
                this.server = server;
            }

            public int getExpiration() {
                return this.expiration;
            }

            public void setExpiration(int expiration) {
                this.expiration = expiration;
            }

            public boolean isCluster() {
                return this.cluster;
            }

            public void setCluster(boolean cluster) {
                this.cluster = cluster;
            }

            public int getConnectionPoolSize() {
                return this.connectionPoolSize;
            }

            public Cache.Redis setConnectionPoolSize(int connectionPoolSize) {
                this.connectionPoolSize = connectionPoolSize;
                return this;
            }

            public int getConnectionMinimumIdleSize() {
                return this.connectionMinimumIdleSize;
            }

            public Cache.Redis setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
                this.connectionMinimumIdleSize = connectionMinimumIdleSize;
                return this;
            }

            public int getSubscriptionConnectionPoolSize() {
                return this.subscriptionConnectionPoolSize;
            }

            public Cache.Redis setSubscriptionConnectionPoolSize(int subscriptionConnectionPoolSize) {
                this.subscriptionConnectionPoolSize = subscriptionConnectionPoolSize;
                return this;
            }

            public int getSubscriptionConnectionMinimumIdleSize() {
                return this.subscriptionConnectionMinimumIdleSize;
            }

            public Cache.Redis setSubscriptionConnectionMinimumIdleSize(int subscriptionConnectionMinimumIdleSize) {
                this.subscriptionConnectionMinimumIdleSize = subscriptionConnectionMinimumIdleSize;
                return this;
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
        private String contentSecurityPolicy = "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";

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

    public static class Logging {
        private final Logging.Logstash logstash = new Logging.Logstash();
        private boolean useJsonFormat = false;

        public boolean isUseJsonFormat() {
            return this.useJsonFormat;
        }

        public void setUseJsonFormat(boolean useJsonFormat) {
            this.useJsonFormat = useJsonFormat;
        }

        public Logging.Logstash getLogstash() {
            return this.logstash;
        }

        public static class Logstash {
            private boolean enabled = false;
            private String host = "localhost";
            private int port = 5000;
            private int ringBufferSize = 512;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getHost() {
                return this.host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return this.port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public int getRingBufferSize() {
                return this.ringBufferSize;
            }

            public void setRingBufferSize(int ringBufferSize) {
                this.ringBufferSize = ringBufferSize;
            }
        }
    }

    public static class Social {
        private String redirectAfterSignIn = "/#/home";

        public String getRedirectAfterSignIn() {
            return this.redirectAfterSignIn;
        }

        public void setRedirectAfterSignIn(String redirectAfterSignIn) {
            this.redirectAfterSignIn = redirectAfterSignIn;
        }
    }

    public static class Gateway {
        private final Gateway.RateLimiting rateLimiting = new Gateway.RateLimiting();
        private Map<String, List<String>> authorizedMicroservicesEndpoints;

        public Gateway() {
            this.authorizedMicroservicesEndpoints = com.kobe.warehouse.config.PharmaSmartDefaults.Gateway.authorizedMicroservicesEndpoints;
        }

        public Gateway.RateLimiting getRateLimiting() {
            return this.rateLimiting;
        }

        public Map<String, List<String>> getAuthorizedMicroservicesEndpoints() {
            return this.authorizedMicroservicesEndpoints;
        }

        public void setAuthorizedMicroservicesEndpoints(Map<String, List<String>> authorizedMicroservicesEndpoints) {
            this.authorizedMicroservicesEndpoints = authorizedMicroservicesEndpoints;
        }

        public static class RateLimiting {
            private boolean enabled = false;
            private long limit = 100000L;
            private int durationInSeconds = 3600;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public long getLimit() {
                return this.limit;
            }

            public void setLimit(long limit) {
                this.limit = limit;
            }

            public int getDurationInSeconds() {
                return this.durationInSeconds;
            }

            public void setDurationInSeconds(int durationInSeconds) {
                this.durationInSeconds = durationInSeconds;
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

    public static class ClientApp {
        private String name = "warehouseApp";

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
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
