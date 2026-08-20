package com.businessos.auth.authentication.google;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * The Firebase ID token the Android app obtained after the user picked a Google account.
 *
 * Deliberately carries nothing else — no email, no name. Those are read out of the verified
 * token server-side. Accepting an email from the client would let anyone sign in as anyone.
 */
@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
