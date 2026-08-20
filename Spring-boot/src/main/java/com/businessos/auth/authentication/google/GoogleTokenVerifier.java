package com.businessos.auth.authentication.google;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.UnauthorizedException;
import com.businessos.shared.firebase.FirebaseInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Turns a Firebase ID token from the Android app into a verified identity.
 *
 * This is the security boundary of the whole Google sign-in feature. The token is signed by
 * Google; verifying it proves the caller really did authenticate as that Google account. Every
 * value used afterwards — above all the email — is read from the *verified* token and never from
 * anything else the client sent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTokenVerifier {

    private final FirebaseInitializer firebase;

    /** Verified identity. Nothing here came from the request body. */
    public record GoogleIdentity(String email, String firstName, String lastName) {
    }

    public GoogleIdentity verify(String idToken) {

        if (!firebase.isAvailable()) {
            // Configuration problem, not the user's fault — say so plainly rather than
            // pretending the sign-in failed.
            throw new BadRequestException(
                    "Google sign-in is not configured on this server. Contact the administrator.");
        }

        FirebaseToken token;

        try {
            token = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            log.debug("Google ID token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Could not verify your Google sign-in. Please try again.");
        }

        String email = token.getEmail();

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Your Google account has no email address attached.");
        }

        // A Google account whose email Google itself has not verified must not be trusted to
        // claim an existing account with the same address.
        if (!token.isEmailVerified()) {
            throw new UnauthorizedException("Your Google account's email is not verified.");
        }

        String[] names = splitName(token.getName(), email);

        return new GoogleIdentity(email.toLowerCase().trim(), names[0], names[1]);
    }

    /**
     * Google gives one display name; the User entity wants first and last separately. A single
     * word becomes the first name, and a missing name falls back to the email's local part so the
     * account is never created nameless.
     */
    private String[] splitName(String displayName, String email) {

        if (displayName == null || displayName.isBlank()) {
            return new String[]{email.substring(0, email.indexOf('@')), ""};
        }

        String trimmed = displayName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');

        return lastSpace < 0
                ? new String[]{trimmed, ""}
                : new String[]{trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1)};
    }
}
