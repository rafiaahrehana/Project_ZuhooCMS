package com.businessos.config;

import java.security.Principal;

/**
 * Minimal Principal for STOMP/WebSocket sessions. name() must be the user's
 * numeric id as a string - NotificationServiceImpl and ServiceRequestServiceImpl
 * address live pushes via convertAndSendToUser(userId.toString(), ...), which
 * Spring matches against Principal.getName(), not the authenticated username/email.
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
