package org.udaan.identity;

/** Global roles stored on a user (section 8.2). Owner/contributor rights are computed per resource. */
public enum Role {
    INDIVIDUAL,
    MODERATOR,
    PLATFORM_ADMIN
}
