package org.udaan.shared;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings that differ between local, test and production.
 *
 * @param environment   name shown by /api/v1/meta, e.g. "local", "test", "production"
 * @param version       build version (git commit) baked into the Docker image
 * @param corsOrigins   web origins allowed to call the API
 */
@ConfigurationProperties(prefix = "udaan")
public record UdaanProperties(String environment, String version, List<String> corsOrigins) {

    public UdaanProperties {
        environment = environment == null || environment.isBlank() ? "local" : environment;
        version = version == null || version.isBlank() ? "dev" : version;
        corsOrigins = corsOrigins == null ? List.of() : List.copyOf(corsOrigins);
    }
}
