package org.udaan.meta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.udaan.shared.UdaanProperties;

/**
 * Reports which build is running where. The deploy pipeline polls this to confirm
 * that test or production is serving the exact commit it just deployed.
 */
@RestController
@RequestMapping("/api/v1/meta")
class MetaController {

    private final UdaanProperties properties;

    MetaController(UdaanProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    MetaResponse meta() {
        return new MetaResponse("Project Udaan API", properties.environment(), properties.version());
    }

    record MetaResponse(String name, String environment, String version) {}
}
