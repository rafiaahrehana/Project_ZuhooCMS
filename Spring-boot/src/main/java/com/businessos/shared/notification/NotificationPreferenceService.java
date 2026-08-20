package com.businessos.shared.notification;

public interface NotificationPreferenceService {

    /** ALL: get preferences for the authenticated platformuser — auto-creates if missing */
    NotificationPreferenceResponse getForCurrentUser();

    /** ALL: full replacement update of notification preferences */
    NotificationPreferenceResponse update(UpdateNotificationPreferenceRequest request);

    /** ALL: reset all preferences to platform defaults */
    NotificationPreferenceResponse resetToDefaults();

    /** INTERNAL: create default preferences on platformuser activation */
    void createDefaultsForUser(Long userId);

}
