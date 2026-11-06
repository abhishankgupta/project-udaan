package org.udaan.identity.api;

import java.util.List;
import java.util.UUID;

/** The signed-in user's own account details. Contact fields are only ever shown to the user themself. */
public record CurrentUserDto(UUID id, String displayName, String email, String phone, List<String> roles) {

    public static CurrentUserDto from(UUID id, String displayName, String email, String phone, List<String> roles) {
        return new CurrentUserDto(id, displayName, email, phone, List.copyOf(roles));
    }
}
