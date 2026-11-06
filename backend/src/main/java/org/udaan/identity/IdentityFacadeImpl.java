package org.udaan.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.identity.api.UserSummary;
import org.udaan.shared.web.NotFoundException;

@Service
class IdentityFacadeImpl implements IdentityFacade {

    private final UserRepository users;

    IdentityFacadeImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findUser(UUID userId) {
        return users.findById(userId).map(IdentityFacadeImpl::toSummary);
    }

    @Override
    @Transactional
    public UserSummary rename(UUID userId, String displayName) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        user.rename(displayName.trim());
        return toSummary(user);
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getDisplayName(), user.getEmail(), user.getPhoneE164(),
                user.getRoles().stream().map(Enum::name).sorted().toList(), user.getCreatedAt());
    }
}
