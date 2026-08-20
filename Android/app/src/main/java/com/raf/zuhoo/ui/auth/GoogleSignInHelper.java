package com.raf.zuhoo.ui.auth;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.raf.zuhoo.R;

import java.util.concurrent.Executors;

/**
 * Gets a Firebase ID token for whichever Google account the user picks.
 *
 * Two hops, and it matters which token ends up where:
 *   1. Credential Manager returns a **Google** ID token for the chosen account.
 *   2. Firebase exchanges that for a **Firebase** ID token.
 * Only the second one goes to our backend, because that is what FirebaseAuth.verifyIdToken()
 * on the server understands.
 */
public class GoogleSignInHelper {

    public interface Callback {
        void onToken(String firebaseIdToken);
        void onError(String message);
        /** The user dismissed the account chooser — not an error, so say nothing. */
        void onCancelled();
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());

    public GoogleSignInHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void signIn(Context activityContext, Callback callback) {

        String webClientId = webClientId();

        if (webClientId == null) {
            // google-services.json has no oauth_client yet, which means Google sign-in hasn't
            // been switched on in the Firebase console (or the SHA-1 is missing). Say that
            // plainly rather than letting Credential Manager fail with something cryptic.
            callback.onError(context.getString(R.string.error_google_not_configured));
            return;
        }

        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                // false = show every Google account on the device, not only ones that have used
                // this app before. On a fresh install the filtered list is empty, which looks
                // like the button is broken.
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(context).getCredentialAsync(
                activityContext,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse response) {
                        exchangeForFirebaseToken(response, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        // Dismissing the sheet arrives here too; treat it as a cancel so the
                        // user isn't shown an error for changing their mind.
                        String type = e.getType() == null ? "" : e.getType();
                        if (type.contains("USER_CANCELED") || type.contains("INTERRUPTED")) {
                            main.post(callback::onCancelled);
                        } else {
                            main.post(() -> callback.onError(
                                    context.getString(R.string.error_google_signin_failed)));
                        }
                    }
                });
    }

    private void exchangeForFirebaseToken(GetCredentialResponse response, Callback callback) {

        String googleIdToken;

        try {
            GoogleIdTokenCredential credential =
                    GoogleIdTokenCredential.createFrom(response.getCredential().getData());
            googleIdToken = credential.getIdToken();
        } catch (Exception e) {
            main.post(() -> callback.onError(
                    context.getString(R.string.error_google_signin_failed)));
            return;
        }

        FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null))
                .addOnCompleteListener(signIn -> {

                    if (!signIn.isSuccessful() || signIn.getResult() == null
                            || signIn.getResult().getUser() == null) {
                        callback.onError(context.getString(R.string.error_google_signin_failed));
                        return;
                    }

                    // false = don't force a refresh; the token was just minted.
                    signIn.getResult().getUser().getIdToken(false)
                            .addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                                    callback.onToken(tokenTask.getResult().getToken());
                                } else {
                                    callback.onError(context.getString(
                                            R.string.error_google_signin_failed));
                                }
                            });
                });
    }

    /**
     * The Web OAuth client id, which the google-services plugin writes into
     * `default_web_client_id` — but only once Google sign-in is enabled in the Firebase console.
     *
     * Looked up by name rather than as R.string.default_web_client_id so the project still
     * compiles before that console step is done.
     */
    private String webClientId() {

        int id = context.getResources().getIdentifier(
                "default_web_client_id", "string", context.getPackageName());

        if (id == 0) {
            return null;
        }

        String value = context.getString(id);
        return value.isBlank() ? null : value;
    }

    /** Clears the Firebase session. Our own tokens are cleared separately by logout. */
    public static void signOut() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {
            // Firebase not configured — nothing to sign out of.
        }
    }
}
