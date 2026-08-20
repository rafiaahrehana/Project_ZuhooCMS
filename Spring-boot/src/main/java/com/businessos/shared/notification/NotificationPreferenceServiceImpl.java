package com.businessos.shared.notification;

import com.businessos.auth.user.User;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository                   userRepository;
    private final SecurityUtil                     securityUtil;

    @Override
    @Transactional
    public NotificationPreferenceResponse getForCurrentUser() {
        User user = securityUtil.getCurrentUser();
        NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
            .orElseGet(() -> createDefaults(user));
        return NotificationPreferenceMapper.toResponse(prefs);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse update(UpdateNotificationPreferenceRequest request) {
        User user = securityUtil.getCurrentUser();
        NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
            .orElseGet(() -> createDefaults(user));

        prefs.setEmailOnServiceRequest(request.isEmailOnServiceRequest());
        prefs.setEmailOnStatusChange(request.isEmailOnStatusChange());
        prefs.setEmailOnInvoice(request.isEmailOnInvoice());
        prefs.setEmailOnPayment(request.isEmailOnPayment());
        prefs.setEmailOnTaskAssigned(request.isEmailOnTaskAssigned());
        prefs.setEmailOnLeaveUpdate(request.isEmailOnLeaveUpdate());
        prefs.setInAppOnServiceRequest(request.isInAppOnServiceRequest());
        prefs.setInAppOnStatusChange(request.isInAppOnStatusChange());
        prefs.setEmailMarketing(request.isEmailMarketing());

        preferenceRepository.save(prefs);
        
        return NotificationPreferenceMapper.toResponse(prefs);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse resetToDefaults() {
        User user = securityUtil.getCurrentUser();

        // Fix from Phase 2 review: use builder() so @Builder.Default values apply.
        // new NotificationPreference() sets all booleans to false (Java default),
        // bypassing @Builder.Default which sets them to true.
        NotificationPreference prefs = preferenceRepository.findByUserId(user.getId())
            .orElseGet(() -> NotificationPreference.builder().user(user).build());

        prefs.setUser(user);
        prefs.setEmailOnServiceRequest(true);
        prefs.setEmailOnStatusChange(true);
        prefs.setEmailOnInvoice(true);
        prefs.setEmailOnPayment(true);
        prefs.setEmailOnTaskAssigned(true);
        prefs.setEmailOnLeaveUpdate(true);
        prefs.setInAppOnServiceRequest(true);
        prefs.setInAppOnStatusChange(true);
        prefs.setEmailMarketing(false);

        preferenceRepository.save(prefs);
        
        return NotificationPreferenceMapper.toResponse(prefs);
    }

    @Override
    @Transactional
    public void createDefaultsForUser(Long userId) {
        if (preferenceRepository.existsByUserId(userId)) {
            return;
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        createDefaults(user);
        
    }

    private NotificationPreference createDefaults(User user) {
        NotificationPreference prefs = NotificationPreference.builder()
            .user(user)
            .build();
        return preferenceRepository.save(prefs);
    }
}
