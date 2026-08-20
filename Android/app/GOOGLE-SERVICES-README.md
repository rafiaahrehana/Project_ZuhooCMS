# Firebase setup (required before push notifications work)

`app/google-services.json` in this repo is a **placeholder**. It has the right shape so the
project builds and the Firebase SDK links, but its project id and API keys are fake — the app
will never obtain a real FCM token with it in place.

## To enable push

1. Create a Firebase project at <https://console.firebase.google.com>.
2. Add **two** Android apps to it — the flavors have different application ids:
   - `com.raf.zuhoo` (prod)
   - `com.raf.zuhoo.dev` (dev)
   Both must exist in the same Firebase project, or `assembleDevDebug` fails with
   *"No matching client found for package name"*.
3. Download the generated `google-services.json` and replace this file with it.
4. In the Firebase console: **Project settings → Service accounts → Generate new private key**.
   Save the JSON somewhere outside the repo.
5. Point the backend at it before starting `backend/Zuhoo`:

```bash
export FIREBASE_CREDENTIALS_PATH=/absolute/path/to/service-account.json
```

Without step 5 the backend logs `FCM push disabled` at startup and simply doesn't send —
everything else, including live in-app notifications over the STOMP socket, keeps working.

## Do not commit the real files

The real `google-services.json` is low-sensitivity but still identifies your project, and the
service-account key is a **full-access credential** — it must never be committed. Keep the
service account out of the repo entirely and pass it via the environment variable above.
