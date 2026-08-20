package com.businessos.shared.notification.device;

import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository repository;
    private final SecurityUtil securityUtil;

    /**
     * Upsert rather than insert. FCM reissues tokens, and the same token can end up belonging to
     * a different account after a reinstall or account switch on the same handset — so an
     * existing row is reassigned to the current user instead of being duplicated or left
     * pointing at the previous owner.
     */
    @Transactional
    public void register(RegisterDeviceTokenRequest request) {

        User user = securityUtil.getCurrentUser();
        Long companyId = securityUtil.getCurrentCompanyId();

        DeviceToken deviceToken = repository.findByToken(request.getToken())
                .orElseGet(DeviceToken::new);

        deviceToken.setToken(request.getToken());
        deviceToken.setPlatform(request.getPlatform());
        deviceToken.setUser(user);
        deviceToken.setCompanyId(companyId);
        deviceToken.setLastSeenAt(LocalDateTime.now());

        repository.save(deviceToken);
    }

    /**
     * Called on sign-out. Deliberately does not check ownership: the caller is handing back a
     * token they hold, and refusing to unregister it would be worse than the alternative —
     * a device that keeps receiving a signed-out user's notifications.
     */
    @Transactional
    public void unregister(String token) {
        repository.deleteByToken(token);
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> tokensFor(Long userId) {
        return repository.findByUserId(userId);
    }

    /** Drops tokens FCM has told us are dead — see FcmPushService. */
    @Transactional
    public void prune(List<String> tokens) {
        for (String token : tokens) {
            repository.deleteByToken(token);
        }
        log.debug("Pruned {} dead device token(s)", tokens.size());
    }
}
