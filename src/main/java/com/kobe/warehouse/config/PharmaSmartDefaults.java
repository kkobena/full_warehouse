package com.kobe.warehouse.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PharmaSmartDefaults {

    public interface ApiDocs {
        String title = "Application API";
        String description = "API documentation";
        String version = "0.0.1";
        String termsOfServiceUrl = null;
        String contactName = null;
        String contactUrl = null;
        String contactEmail = null;
        String license = null;
        String licenseUrl = null;
        String[] defaultIncludePattern = new String[] { "/api/**" };
        String[] managementIncludePattern = new String[] { "/management/**" };
        String host = null;
        String[] protocols = new String[0];
        boolean useDefaultResponseMessages = true;
    }

    public interface Gateway {
        Map<String, List<String>> authorizedMicroservicesEndpoints = new LinkedHashMap<>();

        interface RateLimiting {
            boolean enabled = false;
            long limit = 100000L;
            int durationInSeconds = 3600;
        }
    }

    public interface Ribbon {
        String[] displayOnActiveProfiles = null;
    }

    public interface Registry {
        String password = null;
    }

    public interface Async {
        int corePoolSize = 2;
        int maxPoolSize = 50;
        int queueCapacity = 10000;
    }

    public interface AuditEvents {
        int retentionPeriod = 30;
    }

    public interface Cache {
        interface Redis {
            String[] server = new String[] { "redis://localhost:6379" };
            int expiration = 300;
            boolean cluster = false;
            int connectionPoolSize = 64;
            int connectionMinimumIdleSize = 24;
            int subscriptionConnectionPoolSize = 50;
            int subscriptionConnectionMinimumIdleSize = 1;
        }

        interface Caffeine {
            int timeToLiveSeconds = 3600;
            long maxEntries = 100L;
        }

        interface Ehcache {
            int timeToLiveSeconds = 3600;
            long maxEntries = 100L;
        }

        interface Hazelcast {
            int timeToLiveSeconds = 3600;
            int backupCount = 1;

            interface ManagementCenter {
                boolean enabled = false;
                int updateInterval = 3;
                String url = "";
            }
        }

        interface Infinispan {
            String configFile = "default-configs/default-jgroups-tcp.xml";
            boolean statsEnabled = false;

            interface Distributed {
                long timeToLiveSeconds = 60L;
                long maxEntries = 100L;
                int instanceCount = 1;
            }

            interface Local {
                long timeToLiveSeconds = 60L;
                long maxEntries = 100L;
            }

            interface Replicated {
                long timeToLiveSeconds = 60L;
                long maxEntries = 100L;
            }
        }

        interface Memcached {
            boolean enabled = false;
            String servers = "localhost:11211";
            int expiration = 300;
            boolean useBinaryProtocol = true;

            interface Authentication {
                boolean enabled = false;
            }
        }
    }

    public interface ClientApp {
        String name = "jhipsterApp";
    }

    public interface Http {
        interface Cache {
            int timeToLiveInDays = 1461;
        }
    }

    public interface Logging {
        boolean useJsonFormat = false;

        interface Logstash {
            boolean enabled = false;
            String host = "localhost";
            int port = 5000;
            int ringBufferSize = 512;
        }
    }

    public interface Mail {
        boolean enabled = false;
        String from = "";
        String baseUrl = "";
    }

    public interface Security {
        String contentSecurityPolicy =
            "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";

        interface ClientAuthorization {
            String accessTokenUri = null;
            String tokenServiceId = null;
            String clientId = null;
            String clientSecret = null;
        }

        interface RememberMe {
            String key = null;
        }

        interface Authentication {
            interface Jwt {
                String secret = null;
                String base64Secret = null;
                long tokenValidityInSeconds = 1800L;
                long tokenValidityInSecondsForRememberMe = 2592000L;
            }
        }
    }

    public interface Social {
        String redirectAfterSignIn = "/#/home";
    }
}
