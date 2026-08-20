# Zuhoo Android Client — Plan & As-Built Record

> **What this document is.** It started life on 2026-07-23 as a *pre-build* plan. The app has since
> been built, so it is now two things at once: the specification (field lists, endpoint maps, the
> chat protocol — still the reference you want when adding a screen) and the record of what was
> actually done, including where reality diverged from the plan.
>
> **Sections are numbered as they always were**, because source comments reference them (`§14.2`,
> `§5.2`, `§6`). Don't renumber.
>
> **Last revised:** 2026-07-31, after phases A–E. Corrections since the original draft are marked
> **⚠ Corrected** where the original was factually wrong about the API — several of those were
> guaranteed runtime failures.

**Scope:** Native Android app (Android Studio, **Java**), **multi-role and multi-tenant** — one app build that works for `CLIENT` (a tenant's customer), `COMPANY_OWNER`, and `EMPLOYEE` (tenant staff) across **any** company on the platform. **Not in scope:** platform-staff roles (`SUPER_ADMIN`, `SYSTEM_ADMIN`, `SUPPORT_AGENT`, `SUPPORT_MANAGER`, etc.) — those manage the SaaS itself and would be a separate app, not an extra role on this one.

**Backend:** ⚠ **Corrected.** The original draft said the app "reuses the existing Spring Boot REST API (`backend/BusinessOS`) as-is, **unchanged**". Both halves of that are now out of date:

- The app talks to **`backend/Zuhoo` in this repository, on port 8086**. That is a source-identical copy of `SAAS-PROJECT/backend/BusinessOS` (`com.businessos` → `com.zuhoo`), sharing the same `businessflow` Postgres database, so the Android app and the Angular app see the same tenants and the same data. BusinessOS keeps 8085 for Angular; both run at once.
- The backend was **not** left unchanged. See **§15** for the full list of what this project modified and added.

---

## 0. Status at a glance

| Area | State |
|---|---|
| Auth (login, both registrations, refresh, forgot/reset, verify) | ✅ Built |
| Service requests (list, create, detail, timeline, comments, quotation, cancel) | ✅ Built |
| Invoices, PDF, receipts, SSLCommerz payment | ✅ Built |
| Catalog, packages, subscriptions, reviews | ✅ Built |
| Live chat (client↔company, company↔platform) | ✅ Built |
| Staff mode (assign, status, quotation, support tickets) | ✅ Built |
| KB, notification preferences, profile, biometric | ✅ Built |
| Push notifications (FCM) | ✅ Built — **needs a Firebase project to activate**, see §17 |
| Dynamic per-service form fields | ✅ Built |
| Attachments on comments and support messages | ✅ Built |
| Bengali localisation | ✅ Built — needs a native-speaker review |
| R8, signing config, network security config | ✅ Built — **certificate pinning left off deliberately**, see §17 |
| Offline read cache, ViewModels, bottom nav | ✅ Built |

**Size as built:** 162 Java files, ~12,300 lines, 28 Activities, 8 ViewModels, 41 layouts, 63 API endpoints, 337 strings (×2 locales), 37 unit tests. Release APK 2.2 MB (9.8 MB unminified).

---

## 1. Why this app exists

The web app already has a `client-portal` with dashboard, categories, packages, payments, profile, and services screens, backed by a `CLIENT` Spring Security role — plus, on the staff side, service-request and platform-support-ticket handling for `COMPANY_OWNER`/`EMPLOYEE`. The Android app is a **mobile-first companion** to both sides of that, not a re-platform of the whole SaaS. It lets:

**A tenant's customer (`CLIENT`), on their phone:**
- Request a service and track it end-to-end without emailing/calling support.
- Get push notifications the moment a quotation, status change, or invoice needs their attention.
- Approve quotations and pay invoices from anywhere.
- Browse the service catalog / subscribe to packages.
- **Message the company handling their request in real time** (§14).

**A company's owner/staff (`COMPANY_OWNER`/`EMPLOYEE`), on their phone:**
- See and respond to client requests without being at a desk — assign, update status, quote, and chat with the client live.
- Raise and track a support ticket to platform support, and chat with the assigned agent in real time.

## 2. Target user & constraints

- **One app, any company, one company scope per logged-in session.** `LoginResponse` carries `role` + `companyId`; the app branches its whole UI on `role`. There is no "switch company" within a session — that means logging out and back in, matching the web app.
- Multi-tenant safety is enforced server-side via the `tenantFilter` Hibernate filter and `/me`/`/my` endpoints. The app prefers `.../my` and `.../me` over id-based endpoints so it never needs to know another tenant's data.
- **No tenant header is required.** Tenancy comes entirely from the JWT's `companyId` claim — there is no `X-Tenant-Id` or subdomain header anywhere. The app sends `Authorization` and nothing else.
- All money fields are `BigDecimal` server-side — format with the company's currency, never do float math on totals.
- **Three different account-creation paths, not one:**
  - **CLIENT** self-registers *into an existing company* — `GET /api/companies/public/list` for the picker, then `POST /api/clients/public/register` with the chosen `companyId`.
  - **COMPANY_OWNER** self-registers a *brand-new* company — `POST /api/auth/register` (`RegisterRequest`: name/email/password + `companyName` + `subdomain`). A completely different screen, not a variant of the client signup.
  - **EMPLOYEE** never self-registers. Those accounts are created by the owner/HR on the web; the app only needs a login screen for them.

## 3. Tech stack — as built

| Layer | Choice | Note |
|---|---|---|
| Language | **Java** | matches the Spring Boot backend; Compose is Kotlin-only, so UI is XML |
| UI | XML + View Binding + Material Components | Material **2** theme (`Theme.MaterialComponents.DayNight.DarkActionBar`) |
| Architecture | MVVM — `ViewModel` + `LiveData` | 8 ViewModels covering the detail and list screens |
| DI | ⚠ **Changed: hand-rolled, not Hilt** | `ZuhooApplication` → `di/AppGraph` owns the token store, chat socket, Room cache and notification centre. The plan allowed "plain manual DI is also fine for an app this size"; an annotation processor across 28 Activities would have cost more than it returned. |
| Networking | Retrofit + OkHttp + Gson | `AuthInterceptor` (bearer) + `TokenAuthenticator` (silent refresh) |
| Auth storage | `EncryptedSharedPreferences` | `TokenManager`; the FCM token lives separately in plain prefs (`PushTokenStore`) because it must outlive `clearSession()` in order to be unregistered |
| Local cache | Room | ⚠ **Changed: one generic JSON-backed table**, not an entity per DTO — the cache is only ever read back wholesale to draw a list, never queried by field, so per-type columns plus TypeConverters would be structure with no purpose. See `data/local/db/ListCache`. |
| Push | Firebase Cloud Messaging | §7, §17 |
| PDF | download to cache + `FileProvider` + `ACTION_VIEW` | no in-app renderer |
| STOMP | ⚠ **Hand-rolled**, not `StompProtocolAndroid` | ~120 lines over OkHttp's `WebSocket`. The protocol is simple text framing (§14.2) and this avoids a dependency on an unmaintained library. Covered by unit tests, since a framing bug here is silent. |
| Min SDK | 26 · Target 36 | |

## 4. Auth flow

1. **Login** — `POST /api/auth/login` `{email, password}` → `LoginResponse {userId, firstName, email, role, companyId, accessToken, refreshToken}`.
   - Any role outside `CLIENT`/`COMPANY_OWNER`/`EMPLOYEE` is refused with "use the web admin console".
   - The app branches its entire UI on `role`: `CLIENT` gets §5, staff get §5a. Same login screen, same token handling.
2. **Registration** — three distinct flows; see §2.
3. **Token refresh** — ⚠ **The endpoint is `POST /api/auth/refresh`** (`RefreshTokenRequest {refreshToken}` → `JwtResponse {accessToken, refreshToken}`). The access token lives **15 minutes**; the refresh token 7 days. `TokenAuthenticator` refreshes on 401 and retries once.
   - **401 vs 403 matters.** The backend returns 401 for an expired/invalid token and 403 for a genuine permission failure. Only 401 is retried.
   - Unauthenticated endpoints (login, register, forgot/reset/verify) are **excluded** from refresh: a 401 there is a real answer ("wrong password"), and refreshing would wipe the session of someone who just mistyped.
   - A network failure during refresh propagates as `IOException` rather than logging the user out — a blip must not end a valid session.
4. **Forgot / reset password, email verification, resend** — all built (`ForgotPasswordActivity`, `ResetPasswordActivity`, `VerifyEmailActivity`). ⚠ These endpoints return a **bare `String`**, not the JSON envelope — Retrofit must use `Call<ResponseBody>` or Gson throws.
5. **Biometric unlock** — a local re-entry convenience on top of the stored refresh token, not a replacement for real login.
6. **Logout** — revokes the refresh token server-side, unregisters the FCM device token (before clearing the session, since that call needs the JWT), disconnects the socket, and **wipes the Room cache** so the next account on the device inherits nothing.

## 5. Feature map for CLIENT — screen → backend endpoint

### 5.1 Dashboard (home tab)
⚠ **Corrected.** The original said no client dashboard endpoint existed and one should be added. **It already existed** — `GET /api/dashboard/client-summary` (`hasRole('CLIENT')`), added 2026-07-10, thirteen days *before* this document was first written.

Returns `{pendingRequests, inProgressRequests, completedRequests, unpaidInvoices, outstandingInvoiceAmount}`. Note the grouping: `pendingRequests` = PENDING + QUOTATION_PENDING, `inProgressRequests` = IN_PROGRESS + ASSIGNED. `WAITING_CLIENT`/`UNDER_REVIEW`/`RESUBMITTED` are in neither, so the two do **not** add up to "all open requests" — the cards are labelled for what they actually count.

Active subscriptions still come from `GET /api/packages/subscriptions/my` (no summary field for it).

### 5.2 Service Requests
- List: `GET /api/service-requests/my` (paged).
- Create: `POST /api/service-requests` — fields in §6, including the dynamic `formData` map.
- Detail: `GET /api/service-requests/{id}`.
- **Status timeline:** `GET /api/service-requests/{id}/history`. ⚠ This was **staff-only** and would have 403'd for every client. Opened to `CLIENT` by this project (§15) — `guardAccess()` already restricted a client to their own request, so nothing leaks.
- Comments/thread: `GET`/`POST /api/service-requests/{id}/comments`.
- Cancel: **`PATCH`** `/api/service-requests/{id}/cancel` (not POST); returns plain text.
- Quotation: `POST .../quotation/accept`, `POST .../quotation/reject` (`{reason}`).
- Statuses to render as a badge (`ServiceRequestStatus`): `PENDING → QUOTATION_PENDING → ASSIGNED → IN_PROGRESS → WAITING_CLIENT → UNDER_REVIEW → COMPLETED`, with `REJECTED` / `CANCELLED` / `RESUBMITTED` as side states. Never render the raw constant — map it (§11).

### 5.3 Service Catalog & Packages
⚠ **Resolved** — the original left this as "confirm the exact controller when building". Three options, all confirmed:
- `GET /api/service-categories` — **`CLIENT` explicitly allowed**, tenant-scoped, sorted by `sortOrder`.
- `GET /api/services/active` and `GET /api/services?categoryId=` — no `@PreAuthorize` at all, so any authenticated user.
- `GET /api/companies/public/{subdomain}/services` — truly public, usable **before login**.
- Packages: `GET /api/packages/active`, `POST /api/packages/subscribe`, `GET /api/packages/subscriptions/my`, `PATCH /api/packages/subscriptions/{id}/cancel` — ⚠ `reason` is a **query param**, not a body.

### 5.4 Invoices & Payments
- My invoices: `GET /api/company/finance/invoices/me`.
- **Invoice detail:** `GET /api/company/finance/invoices/{id}`. ⚠ This was **staff-only**, contradicting the original's "self only, enforced server-side" — it would have 403'd on the detail screen *and* on the post-payment re-fetch in §12. Opened to `CLIENT` by this project (§15), reusing the ownership check that `/pdf` already had.
- PDF: `GET /api/company/finance/invoices/{id}/pdf` → `application/pdf` bytes.
- Receipts: `GET /api/company/finance/payment-receipts/me`.
- Refunds are *requested* implicitly by cancelling a paid request; there is no separate button.
- **"Pay Now"** hands off to SSLCommerz — see §12.

### 5.5 Reviews
`POST /api/reviews` (`serviceRequestId`, `rating` 1–5, `comment`) once a request reaches `COMPLETED`.

### 5.6 Knowledge Base
`GET /api/kb/articles?keyword=`, `GET /api/kb/articles/{id}`, `POST /api/kb/articles/{id}/helpful`. Read paths are authenticated but unrestricted by role.

### 5.7 Notifications
- Centre: `GET /api/notifications?unreadOnly=`, count at `GET /api/notifications/count`, mark-read at **`PATCH`** `/api/notifications/{id}/read` and `/read-all`.
- **Preferences:** ⚠ **Corrected twice over.** The verbs are `GET` / **`PUT`** / `DELETE` — there is no `PATCH`, so the original's advice would have produced a 405. And there is **no per-`NotificationType` toggle**: `UpdateNotificationPreferenceRequest` is nine fixed booleans (`emailOnServiceRequest`, `emailOnStatusChange`, `emailOnInvoice`, `emailOnPayment`, `emailOnTaskAssigned`, `emailOnLeaveUpdate`, `inAppOnServiceRequest`, `inAppOnStatusChange`, `emailMarketing`). The original's "let the client choose which NotificationTypes" is not supported by the API.
- **Live badge:** the app subscribes to `/user/queue/notifications` (§14.4) and re-reads the count on each push. No polling.

### 5.8 Profile
- Client company profile: `GET`/`PATCH /api/clients/me` — only `clientCompanyName`, `industry`, `website`, `billingAddress`, `shippingAddress` are editable.
- User account: `GET`/`PATCH /api/users/profile`. ⚠ **Changing `email` requires `currentPassword` in the same body** or it 400s — undocumented in the original.
- Password change is on **`POST /api/auth/change-password`**, not the profile controller.

## 5a. Feature map for COMPANY_OWNER / EMPLOYEE ("staff mode")

Scoped to what is realistic on a phone. **Not** a mobile port of HRM/CRM/Finance/ITAM — those stay web-only.

### 5a.1 Staff Dashboard
Summary cards built from `/api/service-requests` and `/assigned-to-me`. ⚠ There is **no staff equivalent** of `client-summary`, so these count a page of 20 and are a first-page approximation for a busy company. Flagged in the code; worth a small backend addition if the numbers matter operationally.

### 5a.2 Service Request management
- `GET /api/service-requests` (staff only), `GET /api/service-requests/assigned-to-me`.
- Assign: `PATCH /api/service-requests/{id}/assign/{employeeId}` (no body).
- Change status: `PATCH /api/service-requests/{id}/status` (`{status, reason}`).
- Submit quotation: `POST /api/service-requests/{id}/quotation`.
- Detail is the **same screen** as the client's, with staff-only actions shown conditionally.

### 5a.3 Support Tickets — company ↔ platform
- Create: `POST /api/v1/support/tickets` (`COMPANY_OWNER`/`EMPLOYEE` only).
- Mine: `GET /api/v1/support/tickets/my-tickets`. Detail: `GET /api/v1/support/tickets/{id}`.
- Thread: `GET /api/v1/support/messages/ticket/{id}/external` + `POST /api/v1/support/messages`.
- Satisfaction after `RESOLVED`: `POST /api/v1/support/tickets/{id}/satisfaction?rating=&feedback=`.
- Categories for the dropdown: `GET /api/support/categories/active` (the unsuffixed path returns a `Page`, not a list).

### 5a.4 Shared with CLIENT
Notifications (§5.7) and profile (§5.8) work identically; the same screens are reused.

## 6. Exact form specs

Keep forms **field-for-field identical** to what the API accepts — anything extra is dropped or 400s.

**Login** — `email`, `password`.

**New Service Request** (`CreateServiceRequestRequest`)
- `title` *(required, ≤255)*, `description` *(optional)*, `hubServiceId` *(required, picked from catalog)*
- `priority` *(optional — `ServiceRequestPriority`)*. ⚠ The values are `LOW`/**`NORMAL`**/`HIGH`/`URGENT`. **Not `MEDIUM`** — that belongs to `TicketPriority`, a different enum. Sending the wrong one is a 400 that reads like a mystery.
- `agreedPrice` *(optional, ≥ 0)*, `slaDeadline` *(optional)*, `subscriptionId` *(optional)*
- `formData` *(dynamic map, keyed by field id as a string)* — see §6.1.
- `paymentChoice` / `paymentMethod` remain **reserved/unused**; no UI.

### 6.1 Dynamic form fields
`GET /api/v1/services/{serviceId}/form-fields` → `[{id, label, fieldType, required, validationRules, sortOrder}]`, no role restriction on the GET. Rendered at runtime by `DynamicFormRenderer`.
- `fieldType` is `TEXT, TEXTAREA, NUMBER, DROPDOWN, CHECKBOX, RADIO, DATE, FILE_UPLOAD, EMAIL, PHONE, FORMULA`.
- ⚠ **`DROPDOWN`/`RADIO` options live in `validationRules` as a comma-separated list** — the admin form builder has no options column. The web client reads them the same way.
- `FORMULA` fields are computed server-side; don't render an input for one.
- Answers go back as `formData["<fieldId>"] = "<answer>"`, blanks omitted.

**Add Comment** (`AddCommentRequest`)
- `content` *(required)*
- `visibility` — ⚠ **staff MUST send `"CLIENT"` explicitly.** `ServiceRequestServiceImpl.addComment()` defaults a null visibility per role: `CLIENT` for clients, but **`INTERNAL` for staff** — and `getComments()` filters `INTERNAL` out for the client. Omitting it makes every staff reply invisible to the client it was written for. This was the most damaging bug the original spec left open.
- `attachmentUrl` *(optional)* — upload first via `POST /api/upload`, then send the returned URL.

**Send Support Message** (`SupportMessageRequest`)
- `ticketId` *(required)*, `message` *(required)*, `attachmentUrl` / `attachmentFileName` *(optional)*
- `isInternal` — omit entirely; it's for platform-staff notes.
- `sentByUserId` — never send it; the backend derives the sender from the JWT.

**Create Support Ticket** (`SupportTicketRequest`)
- `title`, `description` *(required)*
- `priority` — ⚠ **required (`@NotNull`)**, contrary to the original's "optional, defaults MEDIUM". Omitting it is a 400. Values `CRITICAL`/`HIGH`/`MEDIUM`/`LOW`.
- `categoryId` *(optional)*

**Submit Satisfaction** — ⚠ **both** `rating` and `feedback` are required query params. Omitting `feedback` is a 400; send an empty string.

**Submit Quotation** — `amount`, `currency` *(optional)*, `notes` *(optional)*, `validUntil` *(optional)*. Note none are `@NotNull` server-side, so validate `amount` client-side.

**Change Request Status** — `status` *(required)*, `reason` *(optional)*.

**Company Registration** (`RegisterRequest`) — `firstName`, `lastName` *(2–50)*, `email`, `password` *(≥8, needs upper+lower+digit+special)*, `companyName` *(2–150)*, `subdomain` *(3–50, `^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$`)*, `companyEmail`/`companyPhone` *(optional)*.

**Client Registration** (`PublicClientRegisterRequest`) — same names, plus `phone`, `companyId` *(required, from the picker)*, `clientCompanyName`/`industry`/`website` *(optional)*. Its password rule is looser server-side (length only); the app applies the stricter rule anyway so a password accepted at reset isn't one that would have been refused at signup.

**Reset Password** — `token`, `newPassword`, `confirmPassword`.

**Read-only, never editable by a client:** `status`, `onboardedAt`, `lifetimeValue`, `totalRequests`, `portalAccessEnabled`, `accountManager`, `taxId`.

## 7. Backend additions — status

**1. Device token registration for FCM — ✅ BUILT** (was the one fully outstanding item). Added to both backends:
- `DeviceToken` entity + `DeviceTokenRepository` — unique on `token`, because FCM can reissue the same token to a different account after a reinstall, so registering must *reassign* the row rather than duplicate it.
- `POST /api/notifications/device-tokens` (upsert) and `DELETE /api/notifications/device-tokens/{token}`. The request DTO deliberately has **no `userId`** — the owner comes from the JWT. Accepting one would let any authenticated caller receive someone else's notifications.
- `FcmPushService` fans out from `NotificationServiceImpl` alongside the existing WebSocket push, pruning tokens FCM reports as `UNREGISTERED`/`INVALID_ARGUMENT`.
- **Firebase is optional at runtime.** With no service account configured the service logs once and every send is a no-op, so the backend runs fine in a checkout without credentials.

**2. Client dashboard summary — ✅ already existed.** See §5.1.

**3. Public catalog endpoint — ✅ already existed.** See §5.3.

## 8. Non-functional requirements

- **Security:** HTTPS only in prod (enforced by the network security config, §8.1). JWT never logged. Biometric gate is local-only.
- **Multi-tenancy:** always `/my` or `/me`; never let a user type another client's id.
- **Offline:** the Room cache renders last-known requests/invoices/notifications with a "showing saved data from …" stamp. **Read-only by design** — no write is ever queued; all mutations require connectivity.
- **Push categories:** three channels (requests / billing / general) so a user can silence billing without losing request updates. `NotificationType` has 32 values and has grown repeatedly — unknown values fall through to `general` rather than being dropped.
- **Currency:** format with the tenant's configured currency, never one guessed from the phone locale.

### 8.1 Security checklist — as built

| Item | State |
|---|---|
| `android:allowBackup="false"` | ✅ |
| Call the real logout/revoke endpoint | ✅ plus device-token unregister and cache wipe |
| `FLAG_SECURE` on money/personal screens | ✅ payment, invoice detail, both profile screens, receipts, subscriptions (`ui/common/SecureScreen`) |
| Logging interceptor excluded from release | ✅ `BuildConfig.DEBUG`-gated |
| R8 / resource shrinking on release | ✅ 9.8 MB → 2.2 MB, `mapping.txt` retained for crash deobfuscation |
| Cleartext traffic | ✅ app-wide `usesCleartextTraffic` **removed**; per-flavor network security config — dev permits plaintext only to `10.0.2.2`/`localhost`, prod permits none and trusts system CAs only |
| Payment WebView locked down | ✅ JS on (the gateway needs it), file/content access off, host allowlist derived from the flavor's API host |
| Certificate pinning | ⚠ **Structured but disabled.** A wrong pin bricks every request with no fix but a new release, so it ships off with the `openssl` recipe and a mandatory backup pin documented in `src/prod/res/xml/network_security_config.xml`. See §17. |
| Signing config | ✅ reads a gitignored `keystore.properties` or env vars; builds unsigned if absent rather than failing |
| Token in the WebSocket URL | ⚠ **Still true** — the handshake can't carry an `Authorization` header, so the JWT is a query param and can land in server access logs. Mitigated by `wss://` in prod. A short-lived handshake ticket would be the real fix. |

## 9. Navigation

Same shell app, different tab set built off `role`.

**CLIENT (5 tabs):** Home · Requests · Catalog · Billing · Account
**COMPANY_OWNER / EMPLOYEE (4 tabs):** Home · Requests · Support · Account

Notifications is a bell in the header with a live badge, not a tab.

### 9.1 How this was actually built

Both tab sets exist (`res/menu/bottom_nav_client.xml`, `bottom_nav_staff.xml`, chosen at runtime from `role`).

**Deviation:** the app is **one Activity per screen**, not a single Activity hosting Fragments. Navigation is explicit `Intent`s. This wasn't a considered choice up front — the screens were simply built that way — but converting 28 Activities to Fragments after the fact would be a large refactor with no user-visible payoff, so it stands.

The bottom bar is therefore applied by `ui/common/BottomNavActivity`, which wraps `setContentView` to drop each screen's existing layout into a frame above a shared nav bar. Screens opt in by extending it; no layout files changed. Tab switches use `FLAG_ACTIVITY_REORDER_TO_FRONT` so tabs don't stack as duplicates.

Consequence: tab state lives in the Activity instance, and the system may reclaim a backgrounded tab. Screens reload in `onResume` and render from the Room cache first, so this is invisible in practice.

## 10. Delivery record

The original six-phase plan was followed, then extended. What actually shipped, in order:

| Phase | Work |
|---|---|
| Original build | Auth, dashboard, requests, quotations, invoices, payments, catalog, packages, reviews, chat, staff mode, KB, preferences, profile, biometric |
| **A — correctness** | Token refresh + 401 handling · staff comment visibility · status timeline · invoice detail · payment backoff + `VALIDATION_FAILED` · trial-expiry banner · chat dedupe + ordering · `client-summary` · build flavors |
| **B — architecture** | `AppGraph` + Application class · 8 ViewModels · app-scoped socket with reconnect · Room read cache |
| **C — push & backend** | Device-token endpoint + `FcmPushService` · `backend/Zuhoo` rebuilt from BusinessOS on port 8086 · Android FCM, channels, deep links |
| **D — features** | Forgot/reset/verify screens · dynamic form fields · attachments · live notification badge · bottom nav |
| **E — release** | `@SerializedName` on 269 fields · Bengali (337 strings) + `localeConfig` · enum localisation · R8 + keep rules + signing · network security config · `FLAG_SECURE` sweep · 37 unit tests |

## 11. Multi-language (i18n) — as built

**Done:**
- `res/values/strings.xml` + `res/values-bn/strings.xml`, **337 strings each, verified 1:1** with no missing or extra keys.
- `res/xml/locales_config.xml` + `android:localeConfig` — ⚠ **required from API 33**; without it the in-app picker silently does nothing, which is exactly what it had been doing.
- **Every backend enum is mapped to a string resource** — `ServiceRequestStatus` (10), `InvoiceStatus` (8), `SubscriptionStatus` (5), `TicketStatus` (8), `ServiceRequestPriority` (4). Nothing renders a raw wire constant. Unknown values fall back to the constant rather than blanking out.
- In-app switcher via `AppCompatDelegate.setApplicationLocales`, persisted by the framework.

**Still true — be upfront about this:**
- Free text typed by staff (service titles, KB articles, comments, invoice line items) exists only in the language it was typed in. Real translation needs a backend content-translation layer.
- Push and email bodies are generated server-side in one fixed language. The app localises the *category* of a notification, not its text.
- The Bengali was translated by the implementer, not a native speaker — **worth a review**, particularly finance terms (উপমোট / বকেয়া).

## 12. Payments — SSLCommerz

The backend integration is sandboxed by default (`sslcommerz.sandbox=true`). No backend change was needed; the Android side is entirely about handling the redirect.

1. Client taps "Pay Now".
2. App calls `POST /api/payments/sslcommerz/initiate` `{purpose, targetId, amount}` → `{"gatewayUrl": "..."}`.
   - `purpose = "INVOICE"` with `targetId = invoiceId`, or `"PACKAGE_SUBSCRIPTION"` with the subscription id. (`WALLET_TOPUP`/`PLATFORM_SUBSCRIPTION` are company-level, not for this app.)
3. Open `gatewayUrl` in an **in-app WebView** — not Custom Tabs, which can't intercept navigation before it loads.
4. Intercept navigation to `/payment-result` and read `status` off the query string.
   - ⚠ **The `status` value is a `GatewayTransactionStatus` enum name, not "success".** The *success* callback can still carry **`VALIDATION_FAILED`**, because server-to-server validation runs after the browser has already been redirected. Only `SUCCESS` is worth waiting on.
5. **Don't trust the redirect.** Re-fetch the invoice — the real source of truth. ⚠ The original's "retry once or twice" is not enough: the IPN commonly lands *after* the browser returns. The app polls with **exponential backoff (1s/2s/4s/8s, ~15s total)** and then shows an explicit *"still confirming — reopen in a moment"* state. Reporting "payment failed" at that point would be a lie.
6. On confirmed success, refresh the list.

**If a deep-link close-out is wanted later, use Android App Links, not a custom URI scheme.** A custom scheme is not exclusive — another installed app can register it and race to intercept the payment callback with a spoofed status. App Links require a verified `https://` domain with `assetlinks.json`, which no other app can claim.

## 13. Project setup — as built

- Package `com.raf.zuhoo`, min SDK 26, target 36, View Binding + BuildConfig on.
- **Product flavors `dev` / `prod`** with `buildConfigField API_BASE_URL`. `dev` → `http://10.0.2.2:8086/` (the emulator's alias for the host loopback; use your LAN IP on a physical device). `prod` → `https://api.zuhoo.app/` — **placeholder, change before release**.
- ⚠ **The WebSocket URL is derived from `API_BASE_URL`** (`ApiConfig.webSocketUrl()`, http→ws / https→wss), never declared separately. Two hardcoded hosts is how they drift.
- `dev` gets `applicationIdSuffix ".dev"`, so both flavors can sit on one device — note this means **two Android apps must exist in the Firebase project** (§17).

## 14. Live Chat — protocol

Built 2026-07-23 and verified end-to-end on web. The Android client speaks the identical protocol.

### 14.1 What the backend does
- **Transport:** STOMP over raw WebSocket at `ws://<host>/ws` (`wss://` in prod) — **not** SockJS; `WebSocketConfig` deliberately doesn't call `.withSockJS()`.
- **Auth at the handshake.** A WebSocket client can't attach an `Authorization` header to the upgrade, so the JWT travels as a query param: `ws://<host>/ws?token=<accessToken>`. `WebSocketAuthInterceptor` validates it during the handshake and refuses the upgrade on failure. `/ws/**` is `permitAll` at the HTTP layer precisely because auth happens here.
- **Delivery is per-user.** The server uses `convertAndSendToUser(userId, …)`; only the session authenticated as that numeric user id receives it. There is nothing subscribable that leaks another user's or tenant's messages.
- **Sending is plain REST.** There is no `@MessageMapping` — the socket is receive-only. **The push reaches only the *other* party, never the sender's own sessions**, so append your own message locally after posting; it will never arrive over the socket.

### 14.2 STOMP frames
```
// Client sends, right after the socket opens:
CONNECT
accept-version:1.2
host:localhost

\0

// Server replies:
CONNECTED
version:1.2
heart-beat:0,0
user-name:27          // <- the numeric user id, confirms handshake auth worked

\0

// Client subscribes:
SUBSCRIBE
id:sub-0
destination:/user/queue/service-requests/13/messages

\0

// Server pushes:
MESSAGE
destination:/user/queue/service-requests/13/messages
content-type:application/json
subscription:sub-0

{"id":1,"authorId":28,"authorName":"...","content":"...","createdAt":"...","visibility":"CLIENT"}\0
```
Each frame is NUL-terminated (`\0`); headers are `key:value` lines; the **first** blank line separates headers from body (a body may contain further blank lines). The server advertises `heart-beat:0,0`, so neither side sends heartbeats.

### 14.3 Android implementation — as built
- **Hand-rolled frame codec** (`data/chat/StompFrame`) over OkHttp's `WebSocket`, ~120 lines, unit-tested.
- **One connection for the whole process**, owned by `AppGraph`. Screens `subscribe(destination, listener)` and cancel the returned handle; the socket opens on the first subscription and closes when the last goes away.
- **Reconnect with backoff** (1s→30s) and automatic re-subscription — the broker forgets subscriptions when the session dies, so a reconnect without re-subscribing gives a socket that is up but silent.
- Reads the access token fresh on every connect, so a silent refresh doesn't strand it.
- Sends `DISCONNECT` and `UNSUBSCRIBE` on teardown rather than dropping the socket.
- **The subscription lives in the ViewModel**, so rotating the device doesn't drop the thread or re-open a socket.
- Messages are deduplicated by server id — a socket push and a REST refetch can carry the same message.

### 14.4 Third destination — notifications
⚠ **Not in the original.** `/user/queue/notifications` pushes every in-app notification live (`NotificationServiceImpl.pushWebSocket`). The app subscribes app-wide and drives the header badge from it — no polling. This is the in-app counterpart to FCM: FCM reaches a backgrounded or killed app, this keeps the badge live while it's open.

## 15. Backend changes made by this project

The original said the backend would be used "as-is, unchanged". That is no longer true. Applied to **both** `SAAS-PROJECT/backend/BusinessOS` and this repo's `backend/Zuhoo`:

**Authorization opened up (2 endpoints).** Both reuse an ownership-check pattern that already existed elsewhere in the same class, so neither widens what a client can actually see:
- `GET /api/service-requests/{id}/history` — added `CLIENT`. `guardAccess()` already refused a client any request that wasn't theirs.
- `GET /api/company/finance/invoices/{id}` — added `CLIENT`, then narrowed in the service by `requireOwnInvoice()` when the caller lacks `INVOICE_VIEW` — exactly the shape `generatePdf()` already used.

> **Worth confirming as a product decision:** the timeline exposes `reason` and `changedByName` — staff-written free text on each status change — to clients. Normal for a helpdesk, but it was staff-only before.

**New module — `shared/notification/device/`:** `DeviceToken`, `DevicePlatform`, `DeviceTokenRepository`, `DeviceTokenService`, `DeviceTokenController`, `RegisterDeviceTokenRequest`, `FcmPushService`. Plus a `firebase-admin` dependency, an `fcm.credentials-path` property, and one call added to `NotificationServiceImpl`. Additive only — `ddl-auto=update` creates the `device_tokens` table, no destructive migration.

**`backend/Zuhoo` configuration:** `spring.application.name=Zuhoo`, artifactId `Zuhoo`, `server.port=8086`, `app.backend-url=http://localhost:8086` (SSLCommerz builds its callback URLs from it — a mismatch sends the gateway to the wrong process). Database is deliberately shared with BusinessOS.

Seed identities were **deliberately left alone** — `AdminInitializer` still creates `superadmin@businessos.com` and "BusinessOS HQ". Renaming them would create a second set of platform admins in the shared database.

## 16. Gotchas the original didn't know about

**`SubscriptionEnforcementFilter`.** When a tenant's trial expires, **every non-GET request returns 403** with `{"error":"Subscription Expired", ...}`. Worse, its allow-list regexes (`^/api/support/tickets.*`, `^/api/invoices.*`) **don't match the real paths** (`/api/v1/support/tickets`, `/api/company/finance/invoices`), so even the intended escape hatches are blocked. The app detects this specific body and shows a read-only dialog rather than a generic toast. **The broken regexes are a backend bug worth fixing.**

**Uploaded images don't cross between the two backends.** `LocalFileStorageService` builds the file URL from the request host, so a file uploaded through Angular is stored as `http://localhost:8085/uploads/…` — which an emulator can't resolve, since its `localhost` isn't the host machine's. The 18 seeded images work on both because each backend has its own copy; only *new* uploads diverge.

**Auth endpoints return plain text.** Everything under `/api/auth/` except login and refresh returns a bare `String`. Deserialising into a POJO throws.

**Paged endpoints default to 20 rows.** Anything counting a list client-side silently stops at 20. This is why the client dashboard uses `client-summary`, and why the staff dashboard's counts are still approximate (§5a.1).

**`ServiceRequestPriority` uses `NORMAL`; `TicketPriority` uses `MEDIUM`.** Two enums, two vocabularies, one easy 400.

## 17. Before you ship

1. **Firebase.** Create a project; add **two** Android apps (`com.raf.zuhoo` and `com.raf.zuhoo.dev` — the flavors have different ids, and a missing one fails the dev build with "No matching client found"). Replace the placeholder `app/google-services.json`. Generate a service-account key and point the backend at it via `FIREBASE_CREDENTIALS_PATH`. Full steps in `app/GOOGLE-SERVICES-README.md`.
2. **Production API host.** `prod` currently points at the placeholder `https://api.zuhoo.app/`.
3. **Certificate pinning.** Generate real pins (leaf *and* a CA-intermediate backup — without a backup, a routine cert rotation takes the app offline) and uncomment the block in `src/prod/res/xml/network_security_config.xml`.
4. **Signing.** Create a keystore, copy `keystore.properties.example` to `keystore.properties`, fill it in. Keep the `.jks` out of the repo — losing it means never shipping an update to the same listing again.
5. **Rotate the committed SSLCommerz credentials.** `sslcommerz.store-id` / `store-password` sit in `application.properties` in version control.
6. **Have a native Bengali speaker review `values-bn/strings.xml`.**
7. **Smoke-test the R8 build, not just debug.** Obfuscation breaking JSON is the classic release-only failure. It is guarded by `@SerializedName` on all 269 DTO fields plus keep rules for generic signatures, but verify the real thing: `./gradlew assembleProdRelease`, install, and run the full path — login → create request → chat → pay.
8. **Decide on the dashboard's duplicate navigation.** It now shows both the bottom nav and its own row of buttons to the same destinations.
