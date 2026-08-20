# অধ্যায় ৫ — Authentication, Dashboard, Account ও Shared Components

এই অধ্যায়ে আমরা চারটা প্যাকেজ দেখব: `ui/auth/`, `ui/dashboard/`, `ui/account/`, আর `ui/common/`। এগুলো প্রতিটা ব্যবহারকারী app খোলার পর প্রথম যা দেখে (login), যেখানে সবার আগে ঢোকে (dashboard), আর যেখান থেকে নিজের তথ্য/সেশন সামলায় (account) — আর `ui/common/` হলো সেই ছোট ছোট বিল্ডিং ব্লক যেগুলো বাকি ২৮টা Activity বারবার পুনর্ব্যবহার করে। অধ্যায় ৪-এ যে concept গুলো (CachedListViewModel, Event wrapper, MVVM, ViewBinding) বিস্তারিত হয়েছে, এখানে শুধু reference করা হবে, পুনরাবৃত্তি না করে।

## ৫.১ Authentication মডিউল (ui/auth/)

**Business context:** Zuhoo একটা multi-tenant app — একটাই APK, কিন্তু ভিন্ন ভিন্ন company (tenant)-এর client, owner, employee সবাই এটা ব্যবহার করে। তাই "sign up" একটা single flow না — অধ্যায়ের প্ল্যান ডকুমেন্ট (`docs/android-client-app-plan.md` §২) অনুযায়ী **তিনটা সম্পূর্ণ আলাদা রেজিস্ট্রেশন পথ** আছে: CLIENT একটা existing company-তে যোগ দেয়, COMPANY_OWNER সম্পূর্ণ নতুন company তৈরি করে, আর EMPLOYEE কখনো নিজে register করে না (ওদের account owner/HR web থেকে বানায়)। `ui/auth/` প্যাকেজ এই পুরো পরিচয়-প্রমাণ ও অ্যাকাউন্ট-তৈরির স্তরটা সামলায়।

### ৫.১.১ `LoginActivity.java` — এন্ট্রি পয়েন্ট

এটাই app-এর "true" শুরু — session না থাকলে সবাই এখানে আসে। কয়েকটা গুরুত্বপূর্ণ জিনিস লক্ষ্য করার মতো:

**১. Session-check ও auto-forward `onCreate()`-এ:**
```java
if (getIntent().getBooleanExtra(SessionExpiry.EXTRA_SESSION_EXPIRED, false)) {
    Toast.makeText(this, R.string.error_session_expired, Toast.LENGTH_LONG).show();
    return;   // প্লেইন ফর্মেই থাকো, auto-forward/biometric পথে যেও না
}

if (tokenManager.isLoggedIn()) {
    if (tokenManager.isBiometricEnabled()) {
        showBiometricPrompt();
    } else {
        goToDashboard();
    }
}
```
এই মেথডটা একসাথে দুটো situation সামলায় যেগুলো সহজেই মিশে যেতে পারত: (ক) ব্যবহারকারী প্রথমবার app খুলেছে, session নেই — প্লেইন ফর্ম দেখাও; (খ) session **ছিল**, কিন্তু `TokenAuthenticator` (অধ্যায় ৪.৩ দ্রষ্টব্য) refresh token-ও invalid পেয়ে `SessionExpiry.onSessionExpired()` কল করে এখানে ফেরত পাঠিয়েছে — সেক্ষেত্রে auto-forward/biometric prompt আর দেখানো উচিত না, কারণ session টাই তো এইমাত্র মুছে গেছে। `SessionExpiry.reset()` কলটাও এখানেই হয় (নিচে ৫.১.৫ দেখো) — যাতে **ভবিষ্যতের** কোনো session expire হলে আবার এই একই route কাজ করে।

> **Interview প্রশ্ন: "Biometric login মানে কি পাসওয়ার্ড ছাড়াই লগইন?"**
> না — biometric এখানে **local re-entry convenience**, আসল authentication না (plan doc §৪.৫)। ইতিমধ্যে সংরক্ষিত refresh token-এর উপর একটা device-level gate বসানো হয়েছে মাত্র; ফিঙ্গারপ্রিন্ট backend-এ কিছু প্রমাণ করে না। `onAuthenticationError()`-এ ইচ্ছাকৃতভাবে **কোনো navigation হয় না** — cancel/error হলে নিচের প্লেইন লগইন ফর্মই fallback হিসেবে থেকে যায়, তাই ব্যবহারকারী কখনো আটকে যায় না।

**২. Login attempt:**
```java
authRepository.login(email, password, new Callback<LoginResponse>() {
    @Override
    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
        if (!response.isSuccessful() || response.body() == null) {
            showError(response);
            return;
        }
        onLoginSuccess(response.body());
    }
    ...
});
```
`onLoginSuccess()`-এ role চেক করা হয়:
```java
if (!Role.isSupported(login.getRole())) {
    Toast.makeText(this, R.string.error_unsupported_role, Toast.LENGTH_LONG).show();
    return;
}
```
এই app শুধু `CLIENT`/`COMPANY_OWNER`/`EMPLOYEE` সামলায় — `SUPER_ADMIN` বা `SUPPORT_AGENT`-এর মতো প্ল্যাটফর্ম-স্টাফ role এই app-এর scope-এর বাইরে (তাদের জন্য আলাদা admin console আছে)। Backend সেটা আটকায় না — client-সাইডে এই চেকটা না থাকলে একজন platform admin login করে একটা ভাঙা, অর্ধেক-কাজ-করা dashboard দেখতে পেত।

**৩. Google Sign-In-এর দুই-অংশের ফলাফল:**
```java
GoogleSignInResponse body = response.body();
if (body.isRegistered() && body.getLogin() != null) {
    onLoginSuccess(body.getLogin());
    return;
}
// পরিচিত কেউ না — company বাছাই করতে পাঠাও
Intent intent = new Intent(LoginActivity.this, CompleteGoogleSignupActivity.class);
intent.putExtra(CompleteGoogleSignupActivity.EXTRA_ID_TOKEN, firebaseIdToken);
```
Backend একটাই এন্ডপয়েন্টে **login বা signup** দুটোই সিদ্ধান্ত নেয় — Google account-টা যদি ইতিমধ্যে কোনো user-এর সাথে যুক্ত থাকে সেটা login, না থাকলে সেটা "genuine Google identity কিন্তু কোন company-র?" প্রশ্ন তোলে, যার উত্তর `CompleteGoogleSignupActivity`-তে নেওয়া হয়।

> **Interview প্রশ্ন: "একটা multi-tenant app-এ Google/social login implement করলে কী আলাদা challenge আসে?"**
> সাধারণ single-tenant app-এ Google token verify হলেই account তৈরি করা যায়। এখানে সেটা যথেষ্ট না — প্রতিটা user একটা নির্দিষ্ট company-র অধীনে থাকে, আর Google token সেই তথ্য দেয় না। তাই backend "account আছে কি না" আর "account থাকলে কোন company" — এই দুটো আলাদা প্রশ্নের উত্তর দেয়, আর app-টা সেই অনুযায়ী দুটো ভিন্ন পথে যায়।

### ৫.১.২ `GoogleSignInHelper.java` — দুই ধাপের Token exchange

এই ক্লাসের comment-টাই সবচেয়ে ভালো ব্যাখ্যা:
> ১. Credential Manager একটা **Google** ID token ফেরত দেয়।
> ২. Firebase সেটাকে exchange করে একটা **Firebase** ID token-এ।
> শুধু দ্বিতীয়টা backend-এ যায়, কারণ server-এর `FirebaseAuth.verifyIdToken()` সেটাই বোঝে।

```java
FirebaseAuth.getInstance()
        .signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null))
        .addOnCompleteListener(signIn -> {
            ...
            signIn.getResult().getUser().getIdToken(false)
                    .addOnCompleteListener(tokenTask -> {
                        if (tokenTask.isSuccessful() ...) {
                            callback.onToken(tokenTask.getResult().getToken());
                        }
                    });
        });
```

দুইটা ছোট কিন্তু গুরুত্বপূর্ণ detail:
- `setFilterByAuthorizedAccounts(false)` — নতুন install-এ filtered list খালি থাকে, যেটা দেখতে "বাটনটা কাজ করছে না"-এর মতো লাগে। তাই device-এর **সব** Google account দেখানো হয়।
- `onError()`-এ `USER_CANCELED`/`INTERRUPTED` টাইপ আলাদা করে `onCancelled()`-এ পাঠানো হয় — account chooser থেকে বেরিয়ে আসা কোনো error না, তাই সেটার জন্য কোনো error message দেখানো ঠিক না।

> **Interview প্রশ্ন: "একজন ব্যবহারকারী account-picker বন্ধ করে দিলে সেটাকে error হিসেবে দেখানো কেন খারাপ UX?"**
> কারণ user সচেতনভাবে একটা পছন্দ বাতিল করেছে, কিছু ভুল হয়নি। Error toast দেখালে user বিভ্রান্ত হবে ("কী ভুল হলো?")। সঠিক আচরণ হলো চুপচাপ form-এ ফিরিয়ে আনা — ঠিক যেভাবে `onCancelled()` করে।

### ৫.১.৩ `ClientRegisterActivity.java` ও `CompanyOwnerRegisterActivity.java` — দুই ভিন্ন signup

দুটো Activity গঠনে প্রায় একই রকম (form validate → repository call → toast → finish), কিন্তু **কী** পাঠায় তাতে সম্পূর্ণ আলাদা:

| | `ClientRegisterActivity` | `CompanyOwnerRegisterActivity` |
|---|---|---|
| Endpoint | `POST /api/clients/public/register` | `POST /api/auth/register` |
| আলাদা field | `companyId` (dropdown থেকে বাছাই) | `companyName`, `subdomain` |
| Repository | `ClientRepository` | `AuthRepository` |
| যোগ দেয় | একটা **existing** company-তে | একটা **নতুন** company তৈরি করে |

`ClientRegisterActivity`-তে company dropdown আসে `CompanyRepository.getPublicCompanies()` থেকে (login-এর আগে, তাই `public` endpoint):
```java
binding.companyDropdown.setAdapter(new ArrayAdapter<>(...));
binding.companyDropdown.setText(labels.get(0), false);
companyIndex = 0;
binding.companyDropdown.setOnItemClickListener((parent, view, position, id) -> companyIndex = position);
```
লক্ষ্য করো — `attemptRegister()` কখনো dropdown-এর **text** পড়ে না, শুধু `companyIndex` variable। কারণ `TextInputLayout.ExposedDropdownMenu` non-editable হলেও এটা টেকনিক্যালি একটা text field, আর label string থেকে ফিরে company object বের করা ভঙ্গুর (দুটো company-র নাম একই হতে পারে)। Index-ভিত্তিক ট্র্যাকিং এই ভুলটা এড়ায়।

`CompanyOwnerRegisterActivity`-তে subdomain validate হয় client-side-এই, backend-এর নিয়ম হুবহু মিলিয়ে:
```java
private static final Pattern SUBDOMAIN_PATTERN =
        Pattern.compile("^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$");
```

> **Interview প্রশ্ন: "Client-side validation থাকা সত্ত্বেও কেন backend-এও সব validate করতে হয়?"**
> Client-side validation UX-এর জন্য (তাৎক্ষণিক feedback, খারাপ request পাঠানো এড়ানো) — কিন্তু নিরাপত্তার জন্য না। যে কেউ Retrofit বাদ দিয়ে সরাসরি API-তে curl দিয়ে call করতে পারে; app-এর কোনো validation bypass করা তুচ্ছ। তাই backend-ই আসল সত্যের উৎস, client-side validation শুধু ভালো experience-এর জন্য।

উভয় Activity-ই `PasswordPolicy.isValid(password)` ব্যবহার করে — একটা shared static validator (`data/model/PasswordPolicy`) যেটা backend-এর নিয়ম (≥৮ অক্ষর, uppercase+lowercase+digit+special) client-side-এ mirror করে, যাতে ব্যবহারকারী সাবমিট করার পরেই backend থেকে "দুর্বল পাসওয়ার্ড" শুনতে না হয়।

### ৫.১.৪ `CompleteGoogleSignupActivity.java` — মিসিং লিংক পূরণ করা

এই Activity-র নিজের javadoc-ই যুক্তিটা স্পষ্ট করে:
> App multi-tenant, তাই শুধু Google token দিয়ে account বানানো যায় না — প্রতিটা user একটা company-র অধীনে। এখানেই সেই বাকি অংশটা নেওয়া হয়।

গঠন `ClientRegisterActivity`-র সাথে প্রায় একই (company dropdown, companyIndex ট্র্যাকিং) — পার্থক্য হলো এটা `authRepository.googleRegister(idToken, companyId, ...)` কল করে, password নয়, `idToken` পাঠায়:
```java
authRepository.googleRegister(
        idToken,
        companies.get(companyIndex).getId(),
        text(binding.phoneEditText.getText()),
        text(binding.clientCompanyEditText.getText()),
        text(binding.industryEditText.getText()),
        null,
        new Callback<LoginResponse>() { ... });
```
সফল হলে সরাসরি `LoginResponse` ফেরত আসে (session সহ) — অন্য দুটো register flow-র মতো "গিয়ে login করো" বলে না, কারণ Google দিয়ে identity ইতিমধ্যেই প্রমাণিত, তাই backend সাথে সাথেই token ইস্যু করে দেয়।

একটা ছোট কিন্তু ভালো practice — `text()` হেল্পার:
```java
/** Blank optional fields go as null, not "" — the API treats them as absent. */
private String text(CharSequence value) {
    ...
    return trimmed.isEmpty() ? null : trimmed;
}
```
খালি স্ট্রিং (`""`) আর `null` API-র কাছে ভিন্ন অর্থ বহন করতে পারে (`""` মানে "explicitly ফাঁকা করো", `null` মানে "touch করো না")। এই হেল্পার সেই পার্থক্যটা রক্ষা করে।

### ৫.১.৫ `SessionExpiry.java` — 401-চেইনের শেষ প্রান্ত

অধ্যায় ৪.৩-এ `TokenAuthenticator` দেখেছি — refresh token নিজেই invalid হলে এটা কল হয়:
```java
public static void onSessionExpired(Context context) {
    if (!routing.compareAndSet(false, true)) {
        return;
    }
    ...
    ZuhooApplication.graph().notificationCenter().stop();
    ZuhooApplication.graph().chatSocket().shutdown();
    ZuhooApplication.graph().listCache().wipe();
    new TokenManager(appContext).clearSession();

    Intent intent = new Intent(appContext, LoginActivity.class);
    intent.putExtra(EXTRA_SESSION_EXPIRED, true);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    appContext.startActivity(intent);
}
```

**`AtomicBoolean routing` কেন লাগলো?** এই মেথড OkHttp-র background thread থেকে কল হয় (`TokenAuthenticator` ভেতর থেকে), আর একই মুহূর্তে **একাধিক** in-flight request একসাথে 401 পেতে পারে (ঠিক যেভাবে অধ্যায় ৪.৩-এ race condition আলোচনা হয়েছে)। `compareAndSet(false, true)` নিশ্চিত করে শুধু **প্রথম** কলটাই আসলে কিছু করে — বাকিগুলো সাথে সাথে ফিরে যায়। এটা না থাকলে ৩-৪টা সমান্তরাল 401 মানে ৩-৪টা `startActivity(LoginActivity)` কল হতো, স্ট্যাকে একগাদা copy জমা হতো।

`reset()` মেথডটা `package-private` (শুধু `ui.auth` প্যাকেজের ভেতর visible) এবং `LoginActivity.onCreate()`-এ কল হয় — একবার ব্যবহারকারী লগইন স্ক্রিনে ফিরে এলে, `routing` flag আবার `false`-এ রিসেট হয়, যাতে **ভবিষ্যতের** কোনো session-ও একইভাবে expire হ্যান্ডল করতে পারে।

> **Interview প্রশ্ন: "কেন এই cleanup (socket bandh, cache wipe) session expire আর logout — দুই জায়গাতেই প্রায় হুবহু ডুপ্লিকেট করা হয়েছে, একটা shared মেথডে বের করা হয়নি কেন?"** — এটা একটা সৎ observation-ভিত্তিক প্রশ্ন। উত্তর দিতে পারো: "যুক্তিসঙ্গতভাবে এটা extract করা যেত (`SessionCleanup.perform()`-এর মতো একটা ইউটিলিটি), যেটা code duplication কমাতো। বর্তমান কোডে এটা দুই জায়গায় প্রায় হুবহু লেখা আছে — ছোট হলেও এটা একটা refactor সুযোগ।"

### ৫.১.৬ Forgot / Reset / Verify — `ResponseBody` কেন?

তিনটা Activity-ই (`ForgotPasswordActivity`, `ResetPasswordActivity`, `VerifyEmailActivity`) একই প্যাটার্ন মেনে চলে — আর তিনটাই `Call<ResponseBody>` ব্যবহার করে, `Call<SomeDto>` না:
```java
authRepository.forgotPassword(email, new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        if (!response.isSuccessful()) {
            UiErrors.show(ForgotPasswordActivity.this, response, ...);
            return;
        }
        ...
    }
```
কারণটা প্ল্যান ডকুমেন্টে ⚠-চিহ্নিত একটা গুরুত্বপূর্ণ সংশোধনী (§৪.৪, §১৬): **`/api/auth/` পাথের প্রায় সব এন্ডপয়েন্ট (login, refresh বাদে) একটা bare `String` ফেরত দেয়, JSON envelope না।** যদি Retrofit interface-এ `Call<SomeResponseDto>` লেখা হতো, Gson সেই plain string-কে DTO-তে parse করতে গিয়ে exception ছুঁড়ত — **success response-এই crash**, যেটা ধরা সবচেয়ে কঠিন bug-এর একটা (কারণ HTTP status 200, কিন্তু app crash করছে)। `Call<ResponseBody>` ব্যবহার করে Retrofit raw body deserialize করার চেষ্টাই করে না — app যা লাগে সেটাই বের করে নেয় (এখানে আসলে body পড়াও লাগে না, শুধু `isSuccessful()` চেক)।

> **Interview প্রশ্ন: "একটা REST API-র কিছু endpoint JSON envelope-এ সাড়া দেয়, কিছু plain text-এ — Android client-এ এটা কীভাবে সামলাবে?"**
> Retrofit-এর `Call<T>` টাইপ সরাসরি সেই endpoint-এর প্রকৃত response shape-এর সাথে মিলতে হবে। যেখানে shape অনিশ্চিত বা plain text, সেখানে `Call<ResponseBody>` ব্যবহার করে raw response নাও — deserialization কখনোই ব্যর্থ হবে না, কারণ কোনো deserialization চেষ্টাই হচ্ছে না।

`ForgotPasswordActivity`-তে একটা সূক্ষ্ম নিরাপত্তা সিদ্ধান্তও আছে:
```java
// The backend answers identically whether or not the address exists, so that a
// stranger can't use this screen to find out who has an account. Mirror that
// here — don't imply the email was found.
Toast.makeText(ForgotPasswordActivity.this, R.string.reset_link_sent, ...).show();
```
Backend ইচ্ছাকৃতভাবে ইমেইল থাকুক বা না থাকুক একই উত্তর দেয় (**user enumeration attack** ঠেকাতে) — app-এর টোস্ট মেসেজও সেই নীতিটা রক্ষা করে, কখনো "এই ইমেইল খুঁজে পাওয়া যায়নি"-এর মতো কিছু বলে না।

`ResetPasswordActivity`-তে একটা মন্তব্য এই chapter-এর একটা recurring theme তুলে ধরে:
```java
// The reset endpoint only enforces a minimum length, but hold the same complexity rule
// the registration screens use — a password that passes here should not be one the user
// would have been refused at signup.
```
Backend-এর reset নিয়ম signup নিয়মের চেয়ে শিথিল হলেও, app ইচ্ছাকৃতভাবে **কঠোরটাই** সব জায়গায় প্রয়োগ করে — যাতে reset করা পাসওয়ার্ড কখনো signup-এ প্রত্যাখ্যাত হতো এমন একটা না হয়।

### ৫.১.৭ ফাইল তালিকা (auth)

| ফাইল | কাজ |
|---|---|
| `LoginActivity.java` | মূল লগইন ফর্ম, session auto-forward, biometric prompt, Google Sign-In শুরু |
| `ClientRegisterActivity.java` | CLIENT self-registration — existing company-তে যোগদান |
| `CompanyOwnerRegisterActivity.java` | COMPANY_OWNER self-registration — নতুন company তৈরি |
| `CompleteGoogleSignupActivity.java` | নতুন Google identity-র জন্য company বাছাই ও signup সম্পূর্ণ করা |
| `GoogleSignInHelper.java` | Credential Manager → Firebase ID token exchange করার হেল্পার |
| `ForgotPasswordActivity.java` | ইমেইল দিয়ে reset link/code চাওয়া |
| `ResetPasswordActivity.java` | Token + নতুন পাসওয়ার্ড দিয়ে reset সম্পূর্ণ করা |
| `VerifyEmailActivity.java` | Verification code দিয়ে ইমেইল যাচাই, resend করার সুবিধাসহ |
| `SessionExpiry.java` | Refresh token invalid হলে session cleanup ও login-এ ফেরত পাঠানোর কেন্দ্রীয় জায়গা |

---

## ৫.২ Dashboard মডিউল (ui/dashboard/)

**Business context:** এটাই লগইনের পর প্রথম স্ক্রিন, আর প্ল্যান ডকুমেন্ট অনুযায়ী (§৯) — **একই Activity, `role` অনুযায়ী ভিন্ন চেহারা**। একজন client-এর জন্য এটা "আমার requests/invoices/subscriptions"-এর সারাংশ; একজন company owner/employee-র জন্য এটা "আজ কোন কাজ পেন্ডিং, কতগুলো ticket খোলা"-র summary। দুটো একদম আলাদা user persona-কে একটাই layout+Activity দিয়ে সামলানো হয়েছে — role-based visibility switching দিয়ে, আলাদা Activity লিখে না।

### ৫.২.১ `DashboardActivity.java` — Role-based branching কীভাবে কাজ করে

`DashboardActivity` `BottomNavActivity` (§৫.৪.১ দ্রষ্টব্য) extend করে, তাই bottom nav বার automatically পায়। মূল কাজ চারটা ধাপে ভাগ করা:

```java
viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
bindSession();       // ধাপ ১: টেক্সট/ব্যাজ বসানো + view visibility ঠিক করা
wireNavigation();     // ধাপ ২: সব বাটনের ক্লিক লিসেনার
observeViewModel();   // ধাপ ৩: LiveData observe করা
setUpPush();          // ধাপ ৪: FCM push token নিবন্ধন
viewModel.start();    // শেষে — data fetch শুরু
```

**Role branching `bindSession()`-এ:**
```java
if (viewModel.isStaff()) {
    binding.staffStatsGrid.setVisibility(View.VISIBLE);
    ...
    if (Role.EMPLOYEE.equals(tokenManager.getRole())) {
        binding.btnCheckIn.setVisibility(View.VISIBLE);
    }
} else {
    binding.statsGrid.setVisibility(View.VISIBLE);
    ...
}
```
সব view **default GONE** থাকে XML layout-এ, তারপর role অনুযায়ী দরকারিগুলো `VISIBLE` করা হয়। লক্ষ্য করো একটা সূক্ষ্ম তৃতীয় স্তরের ভাগ: `isStaff()` (`COMPANY_OWNER` অথবা `EMPLOYEE`) দুটোকেই কভার করে, কিন্তু "check-in" বাটন আরও সংকীর্ণভাবে শুধু `EMPLOYEE`-র জন্য — কারণ owner নিজে ঘড়ি-ইন করে না, সে company পরিচালনা করে।

> **Interview প্রশ্ন: "একটা app-এ role-based UI দেখানোর দুটো সাধারণ উপায় কী কী, আর এই প্রজেক্ট কোনটা বেছে নিয়েছে?"**
> এক, **আলাদা Activity/layout প্রতি role-এর জন্য** (বেশি duplication, কিন্তু প্রতিটা সরল)। দুই, **একই Activity/layout, visibility দিয়ে টগল করা** (কম duplication, কিন্তু একটাই XML-এ সব role-এর view একসাথে বসে থাকে)। এই প্রজেক্ট দ্বিতীয় পথ নিয়েছে — `DashboardActivity` একটাই, role-ভিত্তিক `View.VISIBLE`/`GONE`।

**LiveData observe করা — চারটা আলাদা stat উৎস:**
```java
viewModel.clientSummary().observe(this, summary -> {
    binding.statOpenRequestsValue.setText(String.valueOf(summary.getPendingRequests()));
    ...
});
viewModel.statsError().observe(this, event -> {
    if (event.consume() != null) {
        Toast.makeText(this, R.string.error_dashboard_stats_failed, Toast.LENGTH_SHORT).show();
    }
});
```
`statsError()`-এর টাইপ `Event<Boolean>` — অধ্যায় ৪.৭-এ আলোচিত one-shot wrapper। কারণ dashboard-এ একসাথে ৩-৪টা network call fan out হয় (client summary, subscriptions, বা staff-এর জন্য requests/assigned/tickets); যেকোনো একটা fail করলে একটাই toast যথেষ্ট, আর screen rotate করলে সেই toast আবার দেখানো ঠিক হবে না।

`onResume()`-এ notification center চালু হয়, `onCreate()`-এ না:
```java
@Override
protected void onResume() {
    super.onResume();
    ZuhooApplication.graph().notificationCenter().start();
}
```
এই `start()` idempotent — মানে বারবার কল করলেও সমস্যা নেই, সেটা `NotificationCenter`-এর দায়িত্ব। এটা `onResume()`-এ রাখার কারণ অধ্যায় ৩.১-এ আলোচিত সাধারণ প্যাটার্নটাই — dashboard-এ ফিরে এলেই (অন্য স্ক্রিন থেকে ব্যাক করে) badge count আবার তাজা হওয়া দরকার (যেমন notification screen থেকে সব read মার্ক করে ফিরে এলে)।

**Push token registration — কেন এখানে, login-এ না:**
```java
/**
 * Push registration is done here rather than on the login screen because it needs a live
 * session, and this is the first screen every signed-in path lands on — including a resumed
 * session that skipped the login form entirely.
 */
private void setUpPush() {
    ...
    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
        ...
        if (!token.equals(pushTokenStore.getToken())) {
            pushTokenStore.setToken(token);
        }
        deviceTokenRepository.registerIfNeeded();
    });
}
```
এটা একটা ভালো design সিদ্ধান্ত: যদি এই লজিক `LoginActivity`-তে থাকতো, তাহলে biometric দিয়ে auto-login করা user (যে login form-ই কখনো দেখে না) push token কখনো register করাতো না। `DashboardActivity` **প্রতিটা** সাইন-ইন পথের (fresh login, Google signup, biometric resume) কমন গন্তব্য, তাই এখানেই সবচেয়ে নির্ভরযোগ্যভাবে এটা করা যায়।

### ৫.২.২ `DashboardViewModel.java` — দুই সেট Repository call, একটা lifecycle

`DashboardViewModel` কনস্ট্রাক্টরেই role পড়ে নেয় (একবারই, ViewModel-এর গোটা lifetime জুড়ে অপরিবর্তনীয়):
```java
String role = ZuhooApplication.graph().tokenManager().getRole();
staff = Role.COMPANY_OWNER.equals(role) || Role.EMPLOYEE.equals(role);
```

`start()` মেথড দুটো path-এ ভাগ হয়:
```java
public void start() {
    if (started) return;
    started = true;
    if (staff) loadStaffStats(); else loadClientStats();
}
```
`started` flag একটা সাধারণ কিন্তু জরুরি guard — `DashboardActivity.onCreate()` থেকে `start()` কল হয়, আর screen rotate হলে Activity নতুন করে তৈরি হয়ে আবার `onCreate()` চালাবে, কিন্তু **ViewModel বেঁচে থাকে** (অধ্যায় ৩.৫)। এই guard না থাকলে প্রতিবার rotate করলেই সব stats আবার fetch হতো — অপ্রয়োজনীয় নেটওয়ার্ক কল।

**Client-এর জন্য: server-side summary বনাম client-side counting — একটা আসল bug-fix:**
```java
// Three of the four cards come straight from the server-side summary. Counting a page of
// /my instead — which is what this used to do — silently stops counting past the first
// 20 rows, so a client with more history than that saw numbers that were simply wrong.
dashboardRepository.getClientSummary(...);
```
এটা প্ল্যান ডকুমেন্টের §৫.১-এ ⚠-চিহ্নিত সংশোধনীর সাথে হুবহু মেলে: `GET /api/dashboard/client-summary` এন্ডপয়েন্ট backend-এ **ইতিমধ্যে ছিল**, কিন্তু আগের কোড সেটা ব্যবহার না করে `/my` থেকে একটা page (২০টা row) গুনে counter বানাতো — যা প্ল্যান ডকুমেন্টের §১৬-এ উল্লিখিত সাধারণ ফাঁদ ("Paged endpoints default to 20 rows")। একজন ব্যবহারকারীর ২০-এর বেশি request থাকলে dashboard-এর সংখ্যা ভুল দেখাত, কোনো error ছাড়াই — **silent bug**, সবচেয়ে বিপজ্জনক ধরনের।

Subscription-এর জন্য এখনও page গোনা হয় (`activeSubscriptions`), কারণ backend-এ subscription-এর জন্য কোনো summary field নেই — কিন্তু কমেন্টে সৎভাবে লেখা আছে কেন সেটা ঠিক আছে ("Subscriptions per client are few enough that a first page covers them in practice")।

**Staff-এর জন্য: এখনও approximation, কারণ backend-এ equivalent নেই:**
```java
// There's no staff equivalent of the client summary endpoint, so these two still count a
// page — the numbers are a first-page approximation for a busy company.
```
এটা প্ল্যান ডকুমেন্ট §৫a.১-এ flagged একটা known limitation — client dashboard ঠিক করা হয়েছে, staff dashboard এখনও প্রথম পেজের approximation। একজন senior developer হিসেবে এই ধরনের known-limitation সততার সাথে code comment-এ লিখে রাখাটাই ভালো practice।

**একই "count only OPEN status" প্যাটার্ন তিনবার:**
```java
private Callback<PageResponse<ServiceRequestSummary>> openRequestCounter(
        MutableLiveData<Integer> target) {
    return new Callback<PageResponse<ServiceRequestSummary>>() {
        @Override
        public void onResponse(...) {
            int open = 0;
            for (ServiceRequestSummary request : response.body().getContent()) {
                if (ServiceRequestStatus.isOpen(request.getStatus())) open++;
            }
            target.setValue(open);
        }
        ...
    };
}
```
এই হেল্পার একটা factory — `allOpenRequests` আর `assignedToMe` দুটো ভিন্ন `MutableLiveData` target-এর জন্য একই callback logic পুনর্ব্যবহার করে, শুধু কোন `LiveData`-তে ফলাফল যাবে সেটা প্যারামিটার হিসেবে নিয়ে।

**একাধিক network call, একটাই error toast:**
```java
private void reportStatsError() {
    if (errorReported) return;
    errorReported = true;
    statsError.setValue(new Event<>(true));
}
```
Client হোক বা staff — dashboard-এ সবসময় একাধিক (২-৩টা) কল একসাথে fan out হয়। যেকোনো একটা fail করলেও `errorReported` flag প্রথম failure-এই lock হয়ে যায়, বাকিগুলো silently ignore হয় — user একটাই toast দেখে, তিনটা না।

> **Interview প্রশ্ন: "Dashboard-এর মতো স্ক্রিনে যেখানে একসাথে একাধিক API call হয়, error handling কীভাবে ডিজাইন করবে?"**
> মূল principle: user-experience-এর দৃষ্টিকোণ থেকে চিন্তা করো, না যে কয়টা call fail হলো তার হিসাব রাখো। এই কোডে `errorReported` boolean flag একবার set হয়ে গেলে বাকি failure গুলো নীরবে গ্রহণ করা হয় — user একবারই জানে "কিছু stat load হয়নি", বারবার toast-এর ঝাঁকুনি খায় না।

### ফাইল তালিকা (dashboard)

| ফাইল | কাজ |
|---|---|
| `DashboardActivity.java` | Home tab — role অনুযায়ী UI দেখানো, navigation তারযুক্ত করা, push নিবন্ধন |
| `DashboardViewModel.java` | Client-summary বনাম staff-stats fetch করা, role অনুযায়ী কোন call হবে ঠিক করা |

---

## ৫.৩ Account মডিউল (ui/account/)

**Business context:** এটা app-এর "settings hub" — নিজের প্রোফাইল বদলানো, পাসওয়ার্ড বদলানো, ভাষা পাল্টানো, biometric চালু/বন্ধ করা, আর সবশেষে logout। Client-এর জন্য এখানে company profile-ও আছে (তাদের নিজের কোম্পানির তথ্য, যে company তাদের সেবা দিচ্ছে সেটা না — বিভ্রান্ত হওয়া সহজ, তাই নামটা লক্ষ্য করার মতো)।

### ৫.৩.১ `AccountActivity.java` — মেনু হাব, role-conditional আইটেম সহ

`DashboardActivity`-র মতোই এটাও `BottomNavActivity` extend করে, আর role অনুযায়ী নির্দিষ্ট মেনু আইটেম দেখায়:
```java
if (Role.CLIENT.equals(tokenManager.getRole())) {
    binding.btnCompanyProfile.setVisibility(View.VISIBLE);
}
if (Role.COMPANY_OWNER.equals(tokenManager.getRole())
        || Role.EMPLOYEE.equals(tokenManager.getRole())) {
    binding.btnWallet.setVisibility(View.VISIBLE);
    binding.btnDirectory.setVisibility(View.VISIBLE);
}
if (Role.COMPANY_OWNER.equals(tokenManager.getRole())) {
    binding.btnAttendanceLocationSettings.setVisibility(View.VISIBLE);
    binding.btnCompanyOverview.setVisibility(View.VISIBLE);
}
```
এখানে তিনটা আলাদা স্তরের গ্র্যান্যুলারিটি লক্ষণীয়: CLIENT-only আইটেম, staff-উভয় আইটেম, আর শুধু COMPANY_OWNER-only আইটেম (কারণ location settings আর company overview owner-level সিদ্ধান্ত, একজন সাধারণ employee-র সেটা বদলানোর অধিকার নেই)।

**Biometric toggle — device-capability চেক করে সিদ্ধান্ত:**
```java
private void setUpBiometricSwitch() {
    BiometricManager biometricManager = BiometricManager.from(this);
    boolean canUseBiometrics = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
            == BiometricManager.BIOMETRIC_SUCCESS;
    if (!canUseBiometrics) return;   // সুইচটাই দেখানো হয় না
    binding.switchBiometric.setVisibility(View.VISIBLE);
    binding.switchBiometric.setChecked(tokenManager.isBiometricEnabled());
    binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
            tokenManager.setBiometricEnabled(isChecked));
}
```
যেই ডিভাইসে fingerprint/face hardware নেই বা enroll করা নেই, সেখানে সুইচটাই render হয় না — একটা টগল দেখিয়ে ধরিয়ে দেওয়ার চেয়ে ভালো, যেটা toggle করার পর কিছুই ঘটে না।

**Logout — ক্রমটা গুরুত্বপূর্ণ:**
```java
private void logout() {
    String refreshToken = tokenManager.getRefreshToken();
    // Must go out before the session is cleared — the call needs the JWT to authenticate.
    new DeviceTokenRepository(this).unregister();
    ...
    authRepository.logout(refreshToken, new Callback<ResponseBody>() {
        @Override public void onResponse(...) { finishLogout(); }
        @Override public void onFailure(...) { finishLogout(); }  // network fail হলেও local session clear হয়
    });
}

private void finishLogout() {
    GoogleSignInHelper.signOut();
    ZuhooApplication.graph().notificationCenter().stop();
    ZuhooApplication.graph().chatSocket().shutdown();
    ZuhooApplication.graph().listCache().wipe();
    tokenManager.clearSession();
    ...
}
```
এখানে ক্রম সচেতনভাবে ঠিক করা: device token unregister call **আগে** যায়, কারণ সেটার নিজের JWT authentication দরকার — session clear করার পরে করলে সেই কলটাই authenticate হতে পারত না (401)। আর `onFailure()`-এও `finishLogout()` কল হয় — নেটওয়ার্ক ব্যর্থ হলেও local session সবসময় clear হবে, কারণ অফলাইন থাকার কারণে user-কে logged-in অবস্থায় আটকে রাখা আরও খারাপ। শেষে `GoogleSignInHelper.signOut()` — কারণ Firebase নিজের আলাদা session রাখে; সেটা সাফ না করলে পরবর্তী "Continue with Google" নীরবে আগের account-ই আবার ব্যবহার করে ফেলত, নতুন করে জিজ্ঞেস না করে।

> **Interview প্রশ্ন: "Logout করার সময় server call ব্যর্থ হলে কী করা উচিত — local session রেখে দেওয়া, নাকি যেভাবেই হোক clear করা?"**
> সবসময় local session clear করা উচিত। যুক্তি: server-side token revoke ব্যর্থ হলে সেই refresh token সার্ভারে valid-ই থেকে যাবে (এটা একটা ছোট security trade-off, network ফিরে এলে retry করা যেতে পারে) — কিন্তু app-কে "logged in" অবস্থায় local-এ আটকে রাখা মানে user মনে করছে সে বেরিয়েছে অথচ session তখনও device-এ সক্রিয়। এই কোড ঠিক এই সিদ্ধান্তটাই নেয়: `onFailure()`-এও `finishLogout()` কল হয়।

### ৫.৩.২ `EditProfileActivity.java` — দুই আলাদা ফর্ম, একই স্ক্রিনে

এই Activity আসলে দুটো স্বাধীন sub-form সামলায়: প্রোফাইল তথ্য (name/email/phone) আর password change — প্রতিটার নিজস্ব loading state ও button:
```java
private void setProfileLoading(boolean loading) {
    binding.profileProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.btnSaveProfile.setEnabled(!loading);
}
private void setPasswordLoading(boolean loading) {
    binding.passwordProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.btnChangePassword.setEnabled(!loading);
}
```

**Email বদলানোর শর্ত — একটা backend quirk যা client-এ handle করতে হয়েছে:**
```java
boolean emailChanged = !email.equalsIgnoreCase(currentEmail);
String currentPassword = textOf(binding.currentPasswordForEmailEditText);

if (emailChanged && TextUtils.isEmpty(currentPassword)) {
    binding.currentPasswordForEmailInputLayout.setError(
            getString(R.string.error_password_required));
    return;
}
...
userProfileRepository.updateProfile(firstName, lastName, email,
        TextUtils.isEmpty(phone) ? null : phone,
        emailChanged ? currentPassword : null,   // email না বদলালে password পাঠানোই হয় না
        ...);
```
প্ল্যান ডকুমেন্ট §৫.৮-এ এটা স্পষ্ট: **email বদলাতে হলে একই body-তে `currentPassword` দিতে হবে**, নাহলে 400 আসে — এটা মূল প্ল্যানে documented ছিল না। এই কোড শুধু সেই নিয়মটা মানছেই না, আরও একটা ভালো UX সিদ্ধান্ত নিচ্ছে: email অপরিবর্তিত থাকলে password field খালি থাকলেও কোনো error দেখায় না, শুধু email পরিবর্তনের সময়ই সেটা চাওয়া হয় — কারণ password re-enter করা প্রতিটা প্রোফাইল সেভে করানো একটা বিরক্তিকর বাধা হতো।

**পাসওয়ার্ড পরিবর্তন একটা সম্পূর্ণ আলাদা এন্ডপয়েন্ট:**
```java
authRepository.changePassword(currentPassword, newPassword, confirmPassword, ...);
```
প্ল্যান ডকুমেন্ট অনুযায়ী (§৫.৮) — এটা profile controller-এর অংশ না, বরং `POST /api/auth/change-password`-এ যায়, `AuthRepository`-র ভেতর দিয়ে, `UserProfileRepository`-র মাধ্যমে না। এই Activity দুটো ভিন্ন repository একসাথে ব্যবহার করছে সেই কারণেই।

**`SecureScreen.apply(this)` — কেন এই স্ক্রিনে:**
```java
// Shows money / personal details - keep it out of screenshots and recents.
SecureScreen.apply(this);
```
এটা প্ল্যান ডকুমেন্ট §৮.১-এর security checklist-এর সরাসরি বাস্তবায়ন — personal data (email, phone) থাকা যেকোনো স্ক্রিনেই `FLAG_SECURE` বসানো হয়েছে। বিস্তারিত §৫.৪.৬-এ।

### ৫.৩.৩ `ClientProfileActivity.java` — Company profile, read-only field সহ

শুধু `CLIENT` role-এর জন্য (AccountActivity-তে conditional visibility মনে করো)। এই Activity-র বিশেষত্ব হলো read-only আর editable field-এর স্পষ্ট বিভাজন:
```java
// শুধু দেখানো, edit করা যায় না
binding.detailStatus.setText(profile.getStatus());
binding.detailOnboardedAt.setText(profile.getOnboardedAt());
binding.detailAccountManager.setText(...);

// Editable — TextInputEditText-এ
binding.companyNameEditText.setText(profile.getClientCompanyName());
binding.industryEditText.setText(profile.getIndustry());
binding.websiteEditText.setText(profile.getWebsite());
```
এটা প্ল্যান ডকুমেন্টের §৬-এ স্পষ্ট তালিকা প্রতিফলিত করে: `status`, `onboardedAt`, `lifetimeValue`, `totalRequests`, `accountManager`, `taxId` — এগুলো **কখনো** client-editable না, কারণ এগুলো business-critical মেটাডেটা যা শুধু company staff/backend নির্ধারণ করে। `saveProfile()` কল করার সময় শুধু সেই পাঁচটা editable field-ই পাঠানো হয়:
```java
clientProfileRepository.updateMyProfile(
        companyName, industry, website, billingAddress, shippingAddress, ...);
```

> **Interview প্রশ্ন: "একটা প্রোফাইল স্ক্রিনে কিছু field read-only আর কিছু editable — এটা ডিজাইন করার সময় কোন নীতি অনুসরণ করবে?"**
> Read-only field গুলো plain `TextView`-তে রাখা ভালো, `EditText`-এ disabled অবস্থায় না — কারণ একটা disabled-looking input field ব্যবহারকারীকে বিভ্রান্ত করে ("এটা কি এডিট করা যায়?")। এখানে `detailStatus`/`detailOnboardedAt`/`detailAccountManager` plain TextView, আর সত্যিকারের editable field গুলো `TextInputEditText` — চেহারাতেই স্পষ্ট পার্থক্য।

### ফাইল তালিকা (account)

| ফাইল | কাজ |
|---|---|
| `AccountActivity.java` | Settings hub — role-conditional মেনু, biometric toggle, ভাষা পরিবর্তন, logout |
| `EditProfileActivity.java` | ব্যক্তিগত প্রোফাইল (নাম/ইমেইল/ফোন) ও পাসওয়ার্ড পরিবর্তন |
| `ClientProfileActivity.java` | Client-এর কোম্পানি প্রোফাইল দেখা/সম্পাদনা (শুধু CLIENT role) |

---

## ৫.৪ Shared Components (ui/common/)

**Business context:** এই ফাইলগুলো কোনো একটা নির্দিষ্ট ফিচারের জন্য না — এগুলো সেই সাধারণ সমস্যাগুলোর সমাধান যা **প্রতিটা** স্ক্রিনে বারবার আসে: loading/empty/error state কীভাবে দেখাবে, ফাইল/ছবি কীভাবে upload করাবে, নিচের navigation bar কীভাবে সব স্ক্রিনে একরকম রাখবে। এই প্যাকেজটাই এই codebase-এর "design system"-এর কোড-পক্ষ। ১১টা ফাইল আছে — `Event` আর `CachedListViewModel` অধ্যায় ৪-এ বিস্তারিত হয়েছে, এখানে শুধু সংক্ষেপে reference করা হবে।

### ৫.৪.১ `BottomNavActivity.java` — `setContentView` override করে shared UI ইনজেক্ট করা

এই ক্লাসের কৌশলটা চমৎকার — bottom nav bar প্রতিটা স্ক্রিনের XML-এ **যোগ না করে**, `setContentView()`-ই override করা হয়েছে:
```java
@Override
public void setContentView(View view, ViewGroup.LayoutParams params) {
    View shell = LayoutInflater.from(this).inflate(R.layout.shell_bottom_nav, null);
    FrameLayout content = shell.findViewById(R.id.contentFrame);
    content.addView(view, params);
    bottomNav = shell.findViewById(R.id.bottomNav);
    super.setContentView(shell);
    setUpNav();
}
```
যেকোনো subclass যখন সাধারণ `setContentView(R.layout.activity_dashboard)` কল করে, আসলে সেটা এই override-এ ধরা পড়ে — নিজের layout একটা `shell_bottom_nav.xml`-এর `contentFrame`-এর ভেতরে বসে যায়, তার উপরে/নিচে bottom nav bar যোগ হয়ে যায়। javadoc-এ এই সিদ্ধান্তের কারণ স্পষ্ট:
> Implemented by wrapping setContentView rather than editing six layouts: each screen keeps the root it already has ... and gets dropped into a frame above the nav bar. That also means a screen opts in purely by extending this class.

মানে ৬টা (বা তার বেশি) আলাদা layout XML হাতে না বদলে, শুধু Activity-র parent class বদলে দিলেই bottom nav চলে আসে।

**একটা Activity-based app-এ ট্যাব-সুইচিং কীভাবে কাজ করে (Fragment ছাড়া):**
```java
Intent intent = intentFor(item.getItemId(), staff);
intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
startActivity(intent);
overridePendingTransition(0, 0);   // কোনো transition animation নেই
```
অধ্যায় ৯.১ (প্ল্যান ডকুমেন্ট)-এ এই deviation-টা ব্যাখ্যা করা আছে: এই app **Fragment-based না, প্রতিটা স্ক্রিন নিজের Activity**। তাই ট্যাব সুইচ মানেই একটা নতুন Activity লঞ্চ। `FLAG_ACTIVITY_REORDER_TO_FRONT` না থাকলে বারবার একই ট্যাবে ফিরে গেলে স্ট্যাকে ডুপ্লিকেট Activity জমা হতো ("Home → Requests → Home → Requests..." করলে ৪টা Activity স্ট্যাকে)। এই flag পুরনো instance-টাকেই সামনে নিয়ে আসে, নতুন বানায় না। `overridePendingTransition(0, 0)` animation বন্ধ করে দেয় যাতে এটা "নতুন স্ক্রিনে যাওয়া" না লেগে "একই স্ক্রিন বদলে যাওয়া"-র মতো লাগে।

> **Interview প্রশ্ন: "Fragment-based bottom navigation বনাম Activity-per-screen বনাম — trade-off কী?"**
> Fragment-based (Jetpack Navigation দিয়ে) সাধারণত standard practice, কারণ একটাই Activity, ট্যাব সুইচ সস্তা, state সহজে বাঁচে। Activity-per-screen-এর সুবিধা হলো প্রতিটা স্ক্রিন সম্পূর্ণ স্বাধীন — সহজে বোঝা যায়, টেস্ট করা যায়, কিন্তু ট্যাব সুইচ ভারী (নতুন Activity lifecycle), আর multi-instance আটকাতে বাড়তি flag লাগে (এই `REORDER_TO_FRONT`-এর মতো)। এই প্রজেক্টে এটা upfront সিদ্ধান্ত ছিল না — ডকুমেন্টেশন অনুযায়ী স্ক্রিন এভাবেই তৈরি হয়ে গিয়েছিল, পরে ২৮টা Activity-কে Fragment-এ রূপান্তর করা একটা বড় refactor হতো যার কোনো ব্যবহারকারী-দৃশ্যমান লাভ নেই — তাই এটাই থেকে গেছে।

`selectedNavItemId()` abstract মেথড — প্রতিটা subclass (`DashboardActivity`, `AccountActivity`, ইত্যাদি) নিজের ট্যাব আইডি ফেরত দেয়, যাতে bottom bar জানে কোনটা highlighted দেখাবে, আর `onItemSelectedListener`-এ "already here" গার্ড কাজ করে:
```java
if (item.getItemId() == selectedNavItemId()) {
    return true;   // একই ট্যাবে থাকলে relaunch করো না
}
```

### ৫.৪.২ `Event.java` ও `CachedListViewModel.java` — সংক্ষিপ্ত reference

অধ্যায় ৪.৫ ও ৪.৭-এ এই দুটো বিস্তারিত আলোচনা হয়েছে। সংক্ষেপে মনে করিয়ে দেওয়া যাক:
- **`Event<T>`** — one-shot LiveData wrapper, `consume()` মেথড প্রথমবার আসল content দেয়, তারপর সবসময় `null` — screen rotate-এ toast/error পুনরাবৃত্তি আটকায়।
- **`CachedListViewModel<T>`** — সব list-screen ViewModel-এর ভিত্তি। `start()` প্রথমে cache দেখায়, তারপর network refresh করে; `onRefreshFailed()` cache-এ ডেটা থাকলে silent থাকে (শুধু "cached" stamp), শুধু cache-ও খালি থাকলে error দেখায়।

এই অধ্যায়ে দেখা `DashboardViewModel` অবশ্য `CachedListViewModel` extend **করে না** — কারণ dashboard কোনো paged list দেখায় না, বরং কয়েকটা count/summary। সেই কারণে dashboard নিজের `AndroidViewModel` সরাসরি extend করে, নিজস্ব সাধারণ (`Event<Boolean>` ভিত্তিক) error-reporting প্যাটার্ন বানিয়ে নিয়েছে।

### ৫.৪.৩ `StatusBadgeView.java` ও `StateView.java` — নতুন Shared Design System

এই সেশনের একটা বড় UI/UX redesign-এর অংশ হিসেবে এই দুটো component তৈরি হয়েছে, যেগুলো এখন প্রায় সব list/detail স্ক্রিন ব্যবহার করে। এগুলো তুলনামূলক নতুন এবং ডিজাইন-সিস্টেমের অংশ — বিস্তারিত আলাদা অধ্যায়ে থাকবে, কিন্তু এখানে এদের কাজ ও API বোঝা দরকার।

**`StatusBadgeView` — একটা একক "status pill" রেন্ডারার।**

এই প্রজেক্টে ইতিমধ্যে ৮টা ভিন্ন per-domain status-badge ক্লাস আছে (`LeaveRequestStatusBadge`, `ExpenseStatusBadge`, `ui.servicerequest.StatusBadge`, ইত্যাদি) — প্রতিটা backend status string থেকে একটা `@ColorInt` color আর একটা localized label বের করে (`colorFor(Context, String)` / `labelFor(Context, String)`)। `StatusBadgeView` এই ৮টা ক্লাসের কোনোটাকেই replace করে না — javadoc নিজেই এটা স্পষ্ট করে:
> Deliberately doesn't touch the 8 existing per-domain *StatusBadge classes ... This class is purely a shared renderer for that existing output.

মানে domain logic (কোন status-এর কোন রং, কোন label) আগের মতোই থেকে যায়; `StatusBadgeView` শুধু সেই **আউটপুট**-কে (color + label) স্ক্রিনে একইভাবে আঁকার দায়িত্ব নেয়:
```java
public static void bind(TextView badge, @ColorInt int color, String label) {
    badge.setText(label);
    badge.setTextColor(color);

    int softBackground = ColorUtils.setAlphaComponent(color, SOFT_ALPHA); // ~12%
    badge.setBackgroundTintList(ColorStateList.valueOf(softBackground));

    int iconRes = iconFor(context, color);
    ...
}
```
ব্যবহার এরকম দেখাবে (কোনো list adapter-এর `bind()`-এ):
```java
int color = LeaveRequestStatusBadge.colorFor(context, item.getStatus());
String label = LeaveRequestStatusBadge.labelFor(context, item.getStatus());
StatusBadgeView.bind(binding.itemStatusBadge, color, label);
```
`SOFT_ALPHA = 31` (~১২% অপাসিটি) দিয়ে ব্যাজের background টেক্সটের একই রং দিয়ে হালকা tint করা হয় — এতে সব status badge-এর একটা consistent visual language তৈরি হয় (soft pill + full-color text + icon), সব domain জুড়ে।

`iconFor()` মেথডটা color-কে আবার একটা icon-এ ম্যাপ করে — কিন্তু string-ভিত্তিক না, **resolved color int** তুলনা করে:
```java
if (color == ContextCompat.getColor(context, R.color.status_success)) {
    return R.drawable.ic_check_circle;
}
```
javadoc সৎভাবে ব্যাখ্যা করে কেন এটা নিরাপদ: "both sides come from the same 4 color resources via `ContextCompat.getColor()` in the same context" — মানে এই ৪টা রংই canonical tone family (success/danger/warning/info), তাই int তুলনা এখানে বিশ্বস্ত।

> **Interview প্রশ্ন: "স্ট্যাটাস UI-তে শুধু রং ব্যবহার করা কেন যথেষ্ট না?"**
> Color-blind ব্যবহারকারীর জন্য শুধু রং দিয়ে পার্থক্য বোঝা কঠিন/অসম্ভব হতে পারে (accessibility নীতি — "don't rely on color alone")। এই কারণেই `StatusBadgeView` প্রতিটা tone family-র (success/danger/warning) সাথে একটা আলাদা **icon**-ও যোগ করে (check/cancel/clock) — তথ্যটা রং **এবং** shape দুইভাবেই বহন হয়।

**`StateView` — এক সাধারণ loading/empty/error UI, সব list-এর জন্য।**

আগের প্যাটার্ন ছিল প্রতিটা স্ক্রিন নিজের একটা bare `emptyState` TextView + `ProgressBar` manage করত — `StateView` সেটাকে একটা single reusable custom View-তে একত্র করেছে। এটা `FrameLayout` extend করে, নিজের layout (`R.layout.view_state`) inflate করে init-এ:
```java
public class StateView extends FrameLayout {
    private CircularProgressIndicator loadingIndicator;
    private View messageGroup;
    private ImageView icon;
    private TextView title, subtitle;
    private MaterialButton action;

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_state, this, true);
        ...
        setVisibility(GONE);   // default: কিছুই দেখাও না
    }
```
চারটা পাবলিক state-method আছে, প্রতিটা `CachedListViewModel`-এর একটা LiveData-র সাথে সরাসরি মেলে:
```java
public void showContent()  // GONE — আসল RecyclerView দেখাও
public void showLoading()  // spinner
public void showEmpty(iconRes, titleRes, subtitleRes[, actionTextRes, onAction])
public void showError(subtitleRes, onRetry)  // "Try Again" বাটনসহ
```
`showError()` নিজে থেকেই একটা retry action যুক্ত করে দেয়:
```java
public void showError(@StringRes int subtitleRes, OnClickListener onRetry) {
    showMessage(R.drawable.ic_alert, getContext().getString(R.string.state_error_title),
            getContext().getString(subtitleRes), R.string.state_action_try_again, onRetry);
}
```
XML-এ ব্যবহার একটা RecyclerView-র sibling হিসেবে:
```xml
<com.raf.zuhoo.ui.common.StateView android:id="@+id/stateView" .../>
```
আর Activity-তে, javadoc-এর নিজের উদাহরণ অনুযায়ী:
```java
viewModel.items().observe(this, items -> {
    if (items.isEmpty()) {
        stateView.showEmpty(R.drawable.ic_inbox, R.string.empty_leave_title, R.string.empty_leave_subtitle);
    } else {
        stateView.showContent();
    }
    adapter.submitList(items);
});
viewModel.error().observe(this, event -> {
    Integer messageRes = event.consume();
    if (messageRes != null) {
        stateView.showError(messageRes, () -> viewModel.refresh());
    }
});
```
(লক্ষ্য করো — `Event<T>`-এর প্রকৃত মেথডের নাম `consume()`, এই javadoc-এর কমেন্টে ভিন্ন নাম লেখা থাকলেও বাস্তব ব্যবহারে সেটাই কল করতে হয়।)

`StateView`-এর সাথে `CachedListViewModel`-এর সম্পর্কটা গুরুত্বপূর্ণ — এটা **কীভাবে দেখানো হবে সেটা বদলায়, কীভাবে track করা হয় সেটা না** (javadoc-এর নিজের ভাষায়)। মানে অধ্যায় ৪.৫-এ আলোচিত "cached data থাকলে silent থাকো, শুধু empty হলে error দেখাও" — সেই সিদ্ধান্ত এখনো ViewModel-এই থাকে; `StateView` শুধু ViewModel যা বলে সেটাকেই সুন্দরভাবে আঁকে।

> **Interview প্রশ্ন: "একটা reusable UI component ডিজাইন করার সময় কোন জিনিসটা component-এ রাখবে, কোনটা রাখবে না?"**
> এই উদাহরণটা ভালো: presentation logic (কীভাবে loading/empty/error দেখতে হবে) `StateView`-তে, কিন্তু business logic (কখন loading দেখানো উচিত, কখন cached ডেটাকে error-মুক্ত রাখা উচিত) `CachedListViewModel`-এই থেকে যায়। একটা reusable View কখনো "কখন" সিদ্ধান্ত নেওয়া উচিত না, শুধু "কীভাবে দেখাবে" সেটা জানা উচিত — সেটাই এটাকে সত্যিকারের reusable রাখে, নাহলে প্রতিটা ব্যবহারকারী স্ক্রিনের জন্য আলাদা override লাগত।

### ৫.৪.৪ `AttachmentPicker.java` ও `SelfieCapture.java` — Pick/Capture-then-upload

দুটো ক্লাসই একই দুই-ধাপের প্যাটার্ন মেনে চলে (ফাইল বাছাই/তোলা → সাথে সাথে `/api/upload`-এ পাঠানো → URL string ফেরত), কিন্তু উদ্দেশ্য ভিন্ন।

**`AttachmentPicker`** — যেকোনো ফাইল বাছাইয়ের জন্য (comment/ticket-এর attachment):
```java
this.launcher = activity.registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), this::onPicked);
...
public void pick() {
    launcher.launch(new String[]{"*/*"});   // যেকোনো টাইপ
}
```

**`SelfieCapture`** — attendance check-in/out-এর জন্য, ইচ্ছাকৃতভাবে ক্যামেরা-only:
```java
this.launcher = activity.registerForActivityResult(
        new ActivityResultContracts.TakePicture(), this::onCaptured);
```
javadoc-এর ব্যাখ্যাটাই সবচেয়ে গুরুত্বপূর্ণ:
> Deliberately camera-only ... unlike AttachmentPicker's document picker — a gallery pick would let someone attach an old photo instead of proving they're here now.

এটা একটা business-logic সিদ্ধান্ত UI layer-এ প্রয়োগ করা হয়েছে — attendance-এর selfie যদি gallery থেকে বাছাই করা যেত, তাহলে কেউ পুরনো ছবি আপলোড করে "আমি এখানে আছি" প্রমাণ করে ফেলতে পারত, যা পুরো check-in ফিচারটার উদ্দেশ্যই নষ্ট করে দেয়।

`SelfieCapture`-এ camera permission হ্যান্ডলিং-ও `AttachmentPicker`-এর তুলনায় বাড়তি একটা ধাপ:
```java
public void capture() {
    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
        launchCamera();
    } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
}
```
কারণটাও comment-এ স্পষ্ট: "The system camera app enforces the caller's own CAMERA permission ... launching TakePicture without it throws a SecurityException rather than prompting" — মানে `OpenDocument`-এর মতো `TakePicture` নিজে থেকে permission চাইতে পারে না, আগে থেকেই থাকতে হবে, নাহলে crash।

দুটোই constructor-এ `registerForActivityResult()` কল করে, যা comment-এ বারবার সতর্ক করা হয়েছে:
```java
/** Must be constructed during onCreate — registerForActivityResult requires it. */
```
অধ্যায় ৩.২-এ আলোচিত Activity Result API-র একটা নিয়ম — launcher register শুধুমাত্র Activity/Fragment fully `STARTED` হওয়ার আগেই করা যায় (কার্যত মানে `onCreate()`-এ), তাই এই ক্লাসগুলো `onCreate()`-এর বাইরে (যেমন কোনো button click handler-এ) তৈরি করলে crash করবে।

### ৫.৪.৫ `UiErrors.java` — একটাই জায়গা যেখানে "error কীভাবে দেখাবো" সিদ্ধান্ত হয়

```java
public static void show(Activity activity, ApiErrors.ApiError error) {
    if (error.isSubscriptionExpired()) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.error_subscription_expired_title)
                .setMessage(error.getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .show();
        return;
    }
    Toast.makeText(activity, error.getMessage(), Toast.LENGTH_LONG).show();
}
```
বেশিরভাগ error toast-এই যথেষ্ট, কিন্তু একটা বিশেষ case-এ ব্যতিক্রম — trial expiry। প্ল্যান ডকুমেন্ট §১৬-এ আলোচিত `SubscriptionEnforcementFilter` bug/behavior: tenant-এর trial ফুরিয়ে গেলে **প্রতিটা non-GET request** 403 দেয়। এই অবস্থায় একটা toast (যেটা কয়েক সেকেন্ডে মিলিয়ে যায়) যথেষ্ট না, কারণ এটা একটা একবারের ভুল না — পুরো app-এর write ক্ষমতা বন্ধ। তাই এই একটা case-এ modal dialog দেখানো হয়, যেটা ব্যবহারকারীকে explicit acknowledge করতে হয়।

`UiErrors.show()`-এর দুটো overload — একটা `Response<?>` নেয় (সরাসরি Activity-তে callback-এর মধ্যে), আরেকটা আগে থেকে resolve করা `ApiError` নেয়:
```java
// Overload for ViewModels: the error body is a one-shot stream, so a VM resolves it to an
// ApiError at the point of failure and passes that on rather than a spent Response.
```
এই মন্তব্যটা একটা সূক্ষ্ম কিন্তু গুরুত্বপূর্ণ technical বিষয় স্পর্শ করে — Retrofit-এর `Response<T>` object-এর error body **একবারই পড়া যায়** (এটা একটা stream-ভিত্তিক resource)। কোনো ViewModel যদি error resolve করে UI-কে জানাতে চায়, ততক্ষণে সেই মূল `Response` object-এর body ইতিমধ্যে খরচ (consumed) হয়ে যেতে পারে — তাই ViewModel নিজেই তখনই `ApiError`-এ রূপান্তর করে রাখে, পরে Activity সেই readily-available object-টাই ব্যবহার করে।

### ৫.৪.৬ `SecureScreen.java` — `FLAG_SECURE`, এক লাইনে

```java
public static void apply(Activity activity) {
    activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE);
}
```
javadoc নিজেই ব্যাখ্যা করে কেন এটা শুধু "screenshot বন্ধ করা" না:
> The recents thumbnail is the part people forget: without this, an invoice or a profile stays rendered in the app switcher after the user has moved on, visible to anyone who picks up the phone.

প্ল্যান ডকুমেন্ট §৮.১-এর security checklist অনুযায়ী এটা প্রয়োগ হয়েছে payment, invoice detail, উভয় profile screen, receipts, আর subscriptions-এ — মূলত যেকোনো স্ক্রিন যেখানে টাকা বা ব্যক্তিগত তথ্য থাকে।

> **Interview প্রশ্ন: "`FLAG_SECURE` ঠিক কী কী আটকায়?"**
> দুটো জিনিস: (১) স্ক্রিনশট নেওয়া/স্ক্রিন রেকর্ড করা — চেষ্টা করলে কালো স্ক্রিন আসে, (২) Recent Apps (app switcher)-এ থাম্বনেইল — সেটাও কালো/ফাঁকা দেখায়। দ্বিতীয়টা প্রায়ই ভুলে যাওয়া হয় — ডেভেলপাররা প্রায়ই শুধু screenshot ব্লক করার কথা ভাবে, কিন্তু app switcher-এ রেন্ডার হয়ে থাকা sensitive content একইভাবে ঝুঁকিপূর্ণ, বিশেষ করে shared/family ডিভাইসে।

### ৫.৪.৭ `CacheStamp.java` — "showing saved data" লাইন

```java
public static void bind(TextView view, boolean showingCached, Long updatedAt) {
    if (!showingCached || updatedAt == null) {
        view.setVisibility(View.GONE);
        return;
    }
    CharSequence relative = DateUtils.getRelativeTimeSpanString(
            updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
    view.setText(context.getString(R.string.cache_last_updated, relative));
    view.setVisibility(View.VISIBLE);
}
```
`CachedListViewModel`-এর `showingCached` আর `lastUpdated` LiveData দুটো সরাসরি এই একটা static মেথডে ম্যাপ হয় — এই ছোট ক্লাসটাই অধ্যায় ৪.৫-এ আলোচিত "cache-first" pattern-এর UI-পক্ষ। `DateUtils.getRelativeTimeSpanString()` timestamp-কে "৫ মিনিট আগে"-এর মতো human-readable relative time-এ রূপান্তর করে।

### ৫.৪.৮ `PdfOpener.java` — Download + FileProvider + external viewer

```java
public static void writeAndOpen(Activity activity, ResponseBody body, String cacheFileName) {
    File file = new File(dir, cacheFileName);
    try (FileOutputStream out = new FileOutputStream(file)) {
        out.write(body.bytes());
    }
    Uri uri = FileProvider.getUriForFile(activity,
            activity.getPackageName() + ".fileprovider", file);
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(uri, "application/pdf");
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
        activity.startActivity(intent);
    } catch (ActivityNotFoundException e) {
        Toast.makeText(activity, R.string.error_pdf_no_viewer, Toast.LENGTH_LONG).show();
    }
}
```
প্ল্যান ডকুমেন্ট §৩ অনুযায়ী app-এ কোনো in-app PDF renderer নেই — এটা ডিভাইসের নিজের PDF viewer-এর কাছে হস্তান্তর করে। `FileProvider` ব্যবহার করার কারণ Android 7.0+ থেকে `file://` URI সরাসরি অন্য app-এর সাথে শেয়ার করা নিষিদ্ধ (`FileUriExposedException`); `content://` URI-ই একমাত্র নিরাপদ পথ, `FLAG_GRANT_READ_URI_PERMISSION`-সহ যাতে PDF viewer app-টা সাময়িকভাবে read করার অনুমতি পায়। `ActivityNotFoundException` ধরা হয় কারণ কিছু ডিভাইসে কোনো PDF app installed নাও থাকতে পারে — সেক্ষেত্রে crash না করে বার্তা দেখানো হয়। javadoc বলছে এটা `InvoiceDetailActivity`-তে প্রথম লেখা হয়েছিল, পরে payslip screen-এর জন্য এই কমন ক্লাসে বের করা হয়েছে — একটা ভালো DRY refactor উদাহরণ।

### ৫.৪.৯ ফাইল তালিকা (common)

| ফাইল | কাজ |
|---|---|
| `Event.java` | One-shot LiveData wrapper — `consume()` একবারই আসল মান দেয় (বিস্তারিত অধ্যায় ৪.৭) |
| `CachedListViewModel.java` | সব list-screen ViewModel-এর abstract ভিত্তি — cache-first, network-refresh (বিস্তারিত অধ্যায় ৪.৫) |
| `BottomNavActivity.java` | `setContentView` override করে shared bottom navigation bar ইনজেক্ট করে |
| `StatusBadgeView.java` | ৮টা per-domain status-badge ক্লাসের আউটপুট রেন্ডার করার একক shared "soft-tint pill" স্টাইল |
| `StateView.java` | Loading/empty/error-এর একক shared custom View, `CachedListViewModel`-এর LiveData থেকে সরাসরি চালিত |
| `AttachmentPicker.java` | যেকোনো ফাইল বাছাই → `/api/upload` → attachment URL (comment/ticket-এ ব্যবহৃত) |
| `SelfieCapture.java` | ইচ্ছাকৃতভাবে ক্যামেরা-only ছবি তোলা → upload — attendance check-in/out-এ ব্যবহৃত |
| `SecureScreen.java` | `FLAG_SECURE` বসিয়ে screenshot/recents-thumbnail বন্ধ করে money/personal স্ক্রিনে |
| `UiErrors.java` | সব ব্যর্থ API response দেখানোর একক সিদ্ধান্ত-কেন্দ্র (toast বনাম subscription-expired dialog) |
| `CacheStamp.java` | "showing saved data from …" রিলেটিভ-টাইম লাইন রেন্ডার করে |
| `PdfOpener.java` | PDF response ডাউনলোড করে cache-এ লিখে, `FileProvider` দিয়ে external viewer-এ খোলে |

---

## ৫.৫ এই অধ্যায়ের ইন্টারভিউ-প্রশ্ন সারাংশ

- **"Multi-tenant app-এ registration ডিজাইন কীভাবে করবে যখন তিন ধরনের user আছে?"** → §৫.১ — CLIENT/COMPANY_OWNER/EMPLOYEE-এর জন্য তিনটা সম্পূর্ণ আলাদা path, একটা ফর্মের variant বানানো হয়নি (`ClientRegisterActivity` বনাম `CompanyOwnerRegisterActivity`)।
- **"Session expire হলে multiple simultaneous 401 কীভাবে সামলাবে যাতে একাধিকবার login screen-এ না যায়?"** → §৫.১.৫ — `SessionExpiry`-র `AtomicBoolean routing`, `LoginActivity.onCreate()`-এ `reset()`।
- **"কেন কিছু Retrofit call `Call<ResponseBody>` নেয়, `Call<Dto>` না?"** → §৫.১.৬ — `/api/auth/` পাথের বেশিরভাগ endpoint plain `String` ফেরত দেয়, JSON envelope না; DTO parse করার চেষ্টা করলে success response-এই crash হতো।
- **"একটা dashboard যেখানে একসাথে একাধিক API call হয়, সেখানে error handling কীভাবে ডিজাইন করবে?"** → §৫.২.২ — `DashboardViewModel.reportStatsError()`-এর `errorReported` flag, একটাই toast, একাধিক না।
- **"Paged endpoint client-side গোনার ফাঁদ কী?"** → §৫.২.২ — client dashboard আগে `/my`-এর ২০-রো page গুনতো, `client-summary` endpoint থাকা সত্ত্বেও — বাস্তব bug যা ঠিক করা হয়েছে (প্ল্যান ডকুমেন্ট §৫.১)।
- **"একটা reusable UI component-এ কোন logic রাখা উচিত, কোনটা না?"** → §৫.৪.৩ — `StateView` শুধু "কীভাবে দেখাবে" জানে, "কখন" সিদ্ধান্তটা `CachedListViewModel`-এই থাকে।
- **"Status UI-তে শুধু রং যথেষ্ট না কেন?"** → §৫.৪.৩ — `StatusBadgeView` রং-এর সাথে icon-ও যোগ করে (accessibility, color-blind ব্যবহারকারীদের জন্য)।
- **"Logout-এর সময় server call fail করলে local session clear করবে কি না?"** → §৫.৩.১ — `AccountActivity.logout()`-এ `onFailure()`-এও `finishLogout()` কল হয়; local সবসময় clear হবে।
- **"`FLAG_SECURE` ঠিক কী আটকায়?"** → §৫.৪.৬ — শুধু screenshot না, recents/app-switcher থাম্বনেইলও।
