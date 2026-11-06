package org.udaan.taxonomy.api;

import java.util.UUID;

public record CategoryRef(UUID id, String code, String nameEn, String nameHi) {
}
