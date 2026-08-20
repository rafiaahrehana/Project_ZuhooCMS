# অধ্যায় ৪ — Data Layer গভীরভাবে (File-by-File)

এই অধ্যায়ে আমরা `data/`, `di/`, আর `push/` প্যাকেজের গুরুত্বপূর্ণ ফাইলগুলো লাইন ধরে ধরে বুঝব। এটাই এই অ্যাপের **backbone** — বাকি সব UI module এই layer-এর উপর দাঁড়িয়ে আছে।

## ৪.১ `data/api/ApiClient.java` — Retrofit Singleton তৈরি

```java
public class ApiClient {
    private static volatile Retrofit retrofit;

    public static ApiService getClient(Context context) {
        Retrofit local = retrofit;
        if (local == null) {
            synchronized (ApiClient.class) {
                local = retrofit;
                if (local == null) {
                    local = build(context.getApplicationContext());
                    retrofit = local;
                }
            }
        }
        return local.create(ApiService.class);
    }
    ...
}
```

**কী হচ্ছে এখানে?** এটা classic **Double-Checked Locking (DCL)** singleton pattern — Java-তে thread-safe singleton বানানোর একটা প্রমাণিত উপায়।

> **Interview প্রশ্ন: "এখানে `volatile` কীওয়ার্ড কেন লাগানো হয়েছে?"**
> এটা একটা **খুব common ও গুরুত্বপূর্ণ** Java interview প্রশ্ন। `volatile` ছাড়া double-checked locking **ব্যর্থ হতে পারে** — কারণ `new Retrofit.Builder()...build()` আসলে ৩টা ধাপে হয়: (১) মেমরি allocate করা, (২) object construct করা, (৩) reference variable-এ assign করা। Compiler/CPU **reorder** করতে পারে ধাপ (২) আর (৩)-কে — মানে অন্য একটা thread `retrofit` variable-কে non-null দেখতে পারে **construction সম্পূর্ণ হওয়ার আগেই**, ফলে একটা আধা-তৈরি object ব্যবহার করে ফেলতে পারে। `volatile` এই reordering আটকায় (happens-before guarantee দেয়), তাই এটা নিরাপদ।

**`build()` মেথডে যা সেট করা হয়:**
- Timeout — connect/read/write প্রতিটা ৩০ সেকেন্ড।
- `AuthInterceptor` — প্রতিটা request-এ token জোড়ে।
- `.authenticator(new TokenAuthenticator(...))` — শুধু 401-এ কাজ করে।
- Debug build হলে (`BuildConfig.DEBUG`) `HttpLoggingInterceptor` যোগ হয় — **release build-এ এটা যোগ হয় না**, কারণ production-এ request/response body log করা মানে token, personal data লিক হওয়ার ঝুঁকি।
- `Gson().setLenient()` — সামান্য malformed JSON-ও parse করতে দেয় (কিছু server slightly non-standard JSON পাঠাতে পারে)।

## ৪.২ `data/api/AuthInterceptor.java` — প্রতিটা Request-এ Token জোড়া

```java
@Override
public Response intercept(Chain chain) throws IOException {
    Request original = chain.request();
    String token = tokenManager.getAccessToken();

    if (token == null || token.isEmpty()) {
        return chain.proceed(original);   // token নেই, header ছাড়াই পাঠাও (login/register-এর জন্য)
    }

    Request authorized = original.newBuilder()
            .header("Authorization", "Bearer " + token)
            .build();
    return chain.proceed(authorized);
}
```

সহজ, কিন্তু গুরুত্বপূর্ণ: `Request` object **immutable** (OkHttp-তে), তাই নতুন header যোগ করতে `newBuilder()` দিয়ে **নতুন** Request বানাতে হয়, পুরনোটা modify করা যায় না।

## ৪.৩ `data/api/TokenAuthenticator.java` — সবচেয়ে জটিল ও গুরুত্বপূর্ণ ফাইল

এই ফাইলটা silent token refresh করে। ধাপে ধাপে বুঝি:

**ধাপ ১ — এই path-এ কি refresh করা উচিত?**
```java
private static final String[] NO_REFRESH_PATHS = {
    "api/auth/login", "api/auth/refresh", "api/auth/register", ...
};
```
Login/register/forgot-password path-এ 401 মানে "ভুল password" — refresh করার কিছু নেই। `isUnauthenticatedPath()` মেথডটা **package-private ও static** রাখা হয়েছে যাতে **Android Context ছাড়াই unit test করা যায়** — এটা খুব গুরুত্বপূর্ণ কারণ এখানে ভুল হলে সবে ভুল password টাইপ করা একজন ব্যবহারকারীর আসল, valid session-ও মুছে যেতে পারে।

**ধাপ ২ — infinite loop আটকানো:**
```java
if (priorResponseCount(response) >= 1) {
    return null;   // ইতিমধ্যে একবার retry হয়েছে, আর না
}
```

**ধাপ ৩ — Race condition সামলানো (এইটা advanced, ভালো করে বোঝো):**
```java
synchronized (REFRESH_LOCK) {
    String current = tokenManager.getAccessToken();
    if (current != null && !current.equals(failedToken)) {
        return retryWith(response.request(), current);   // অন্য thread ইতিমধ্যে refresh করে ফেলেছে!
    }
    ...
}
```
ধরো ৩টা API call **একসাথে** 401 পেল (একই expired token দিয়ে)। তিনটাই `TokenAuthenticator.authenticate()`-এ ঢুকবে। `synchronized` ব্লক নিশ্চিত করে **একটা মাত্র thread** আসলে refresh API call করবে; বাকিরা lock-এর জন্য অপেক্ষা করবে, আর lock পাওয়ার পর দেখবে token **ইতিমধ্যে বদলে গেছে** (`!current.equals(failedToken)`) — তখন তারা **নতুন refresh call না করে** নতুন token দিয়েই retry করবে। এটা না থাকলে ৩টা সমান্তরাল refresh call হতো, যেখানে ১টাই যথেষ্ট।

> **Interview প্রশ্ন: "একসাথে একাধিক request token expire হলে কীভাবে সামলাবেন?"** — এই উত্তরটাই দাও। এটা senior-level thinking দেখায়।

**ধাপ ৪ — Network error বনাম আসল logout, পার্থক্য করা:**
```java
private JwtResponse requestNewTokens(String refreshToken) throws IOException {
    // ব্যর্থ হলে IOException throw হতে দেওয়া হচ্ছে (catch করা হচ্ছে না)
    retrofit2.Response<JwtResponse> result = TokenRefreshClient.get()
            .refresh(new RefreshTokenRequest(refreshToken)).execute();
    return result.isSuccessful() ? result.body() : null;
}
```
এখানে ইচ্ছাকৃতভাবে `IOException` **catch করা হয়নি** — যদি ইন্টারনেট সংযোগ সাময়িকভাবে বিচ্ছিন্ন থাকে (network glitch), সেটা ছড়িয়ে যাবে, যার ফলে **শুধু এই একটা API call ব্যর্থ হবে**, কিন্তু session অক্ষত থাকবে। কিন্তু refresh token **সত্যিই invalid/expired** হলে (`refreshed == null`), তখন `SessionExpiry.onSessionExpired()` কল হয় — এটা আসল logout।

> এই পার্থক্যটা (temporary network problem vs. genuine session expiry) **খুব সহজেই ভুল হতে পারে** — অনেক অ্যাপ ভুল করে যেকোনো refresh-ব্যর্থতাতেই logout করিয়ে দেয়, যেটা একটা খারাপ ব্যবহারিক অভিজ্ঞতা (একবার ওয়াইফাই কাটলেই লগ-আউট!)।

## ৪.৪ `data/local/TokenManager.java` ও `PushTokenStore.java`

`TokenManager` — access token, refresh token, role, companyId — সব **`EncryptedSharedPreferences`**-এ save করে (দেখো অধ্যায় ২.৫)। `PushTokenStore` আলাদা কারণ FCM push token-টা logout-এর পরেও দরকার হয় (unregister করার জন্য)।

## ৪.৫ `ui/common/CachedListViewModel.java` — সবচেয়ে গুরুত্বপূর্ণ Architecture ফাইল

এই abstract ক্লাসটা **প্রতিটা list screen-এর ViewModel-এর ভিত্তি**। এটা "cache-first, network-refresh" pattern implement করে:

```java
public void start() {
    if (!loadedOnce) {
        loadedOnce = true;
        readCache();   // প্রথমে যা আছে সেটা তাৎক্ষণিক দেখাও
    }
    refresh();         // তারপর নেটওয়ার্ক থেকে তাজা ডেটা আনো
}
```

**৪টা abstract মেথড**, প্রতিটা subclass-কে implement করতে হয়:
- `fetch(callback)` — কোন API call হবে (leave requests? expenses?)
- `idOf(item)` — cache-এ save করার জন্য প্রতিটা item-এর unique key কী
- `loadErrorRes()` — network ব্যর্থ হলে, আর কোনো cache-ও না থাকলে, কোন error message দেখাবে

**সবচেয়ে গুরুত্বপূর্ণ অংশ — `onRefreshFailed()`:**
```java
private void onRefreshFailed() {
    if (items.getValue() != null && !items.getValue().isEmpty()) {
        showingCached.setValue(true);   // পুরনো ডেটা থাক, শুধু "cached" লেবেল দেখাও
        return;
    }
    error.setValue(new Event<>(loadErrorRes()));   // কিছুই নেই, তখনই আসল error দেখাও
}
```

> **এইটা এই পুরো প্রজেক্টের অন্যতম সেরা design decision।** ধরো তুমি একটা list দেখছো (ইতিমধ্যে ডেটা load হয়ে গেছে), আর হঠাৎ wifi কেটে গেল, background refresh ব্যর্থ হলো — তখন কী হওয়া উচিত? স্ক্রিন খালি করে "Something went wrong" দেখানো **ভুল** — কারণ user-এর কাছে **valid, useful ডেটা এখনো স্ক্রিনে আছে**! তাই এই কোড শুধু "cached" stamp দেখায়, error card দেখায় না। Error card **শুধুই** তখন দেখানো হয় যখন সত্যিই কিছু দেখানোর নেই।
>
> **Interview প্রশ্ন এখান থেকে বানাতে পারো:** "আপনি কীভাবে সিদ্ধান্ত নেন কখন error state দেখাবেন, কখন দেখাবেন না?" — উত্তর: "যদি user-এর সামনে ইতিমধ্যে valid content থাকে, একটা silent refresh ব্যর্থতা সেটাকে ঢেকে দেওয়া উচিত না। Error UI শুধু তখনই দেখানো উচিত যখন user-এর কাছে সত্যিই কোনো তথ্য নেই।"

**`error` LiveData-র টাইপ `Event<Integer>` কেন `Integer` না?** — `Event` wrapper ব্যবহার হয়েছে (§৪.৭ দ্রষ্টব্য) — screen rotate করলে LiveData তার **শেষ মান আবার পাঠায়** (এটাই স্বাভাবিক আচরণ)। কিন্তু error message-এর ক্ষেত্রে এটা সমস্যা — rotate করলেই যদি আবার সেই একই toast/error দেখা যায়, সেটা bug-এর মতো লাগবে। `Event` wrapper একবার "consume" হয়ে গেলে সেটা আর দ্বিতীয়বার trigger করে না।

## ৪.৬ `data/local/db/` — Room Cache

| ফাইল | কাজ |
|---|---|
| `ZuhooDatabase.java` | Room `@Database` ক্লাস — DB instance তৈরি করে |
| `CacheDao.java` | `@Dao` interface — insert/query SQL-এর জায়গায় annotation |
| `CachedItem.java` | `@Entity` — একটা row-এর structure (`collection`, `itemId`, `json`, `updatedAt`) |
| `CacheMeta.java` | Metadata (কখন শেষ sync হয়েছিল) |
| `ListCache.java` | উপরের সবকিছু wrap করে একটা সহজ `read()`/`write()` API দেয়, যেটা `CachedListViewModel` ব্যবহার করে |

মনে রাখবে (অধ্যায় ২.৬ দ্রষ্টব্য) — এখানে **একটাই generic table**, প্রতিটা DTO-র জন্য আলাদা Entity **না**, কারণ ডেটা কখনো field-ভিত্তিক query হয় না, শুধু "এই collection-এর সব item দাও" এভাবে পড়া হয়।

## ৪.৭ `ui/common/Event.java` — One-Shot LiveData Wrapper

```java
public class Event<T> {
    private final T content;
    private boolean handled = false;

    public T consume() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
```

খুব ছোট কিন্তু গুরুত্বপূর্ণ ক্লাস। LiveData স্বভাবতই "sticky" — নতুন observer যোগ হলে বা configuration change (screen rotate) হলে **শেষ মান আবার পাঠায়**। এটা সাধারণ ডেটার জন্য ঠিক আছে (যেমন loading state — rotate করার পরেও জানা দরকার loading চলছে কিনা), কিন্তু **one-time event** (toast message, navigation trigger, error popup)-এর জন্য এটা bug তৈরি করে — rotate করলেই আগের toast আবার দেখা যাবে। `Event<T>` wrapper সমাধান করে: `consume()` একবারই আসল content ফেরত দেয়, তারপর সবসময় `null`।

## ৪.৮ `di/AppGraph.java` — হাতে-লেখা Dependency Container

```java
public class AppGraph {
    private final TokenManager tokenManager;
    private final ChatSocket chatSocket;      // পুরো app-এ একটাই instance
    private final ListCache listCache;
    private final NotificationCenter notificationCenter;

    public AppGraph(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
        chatSocket = new ChatSocket(appContext);
        listCache = new ListCache(ZuhooDatabase.create(appContext));
        notificationCenter = new NotificationCenter(appContext, chatSocket, tokenManager);
    }
    // getter methods...
}
```

`ZuhooApplication` ক্লাস (Application subclass, app শুরু হওয়ার সাথে সাথেই তৈরি হয়) একটা `AppGraph` instance রাখে (`ZuhooApplication.graph()` static access)। যেসব জিনিস **সত্যিই সারা app জুড়ে একটাই** হওয়া দরকার (socket connection, cache, token) — সেগুলোই এখানে। Repository-গুলো এত ভারী না, প্রতিটা screen যখন দরকার নিজেই `new XyzRepository(context)` বানিয়ে নেয়।

## ৪.৯ `data/chat/` — Real-time Chat (বিস্তারিত অধ্যায় ২.৮-এ)

| ফাইল | কাজ |
|---|---|
| `StompFrame.java` | STOMP protocol-এর frame গুলো (CONNECT, SUBSCRIBE, MESSAGE...) parse ও build করে, ~১২০ লাইন |
| `ChatSocket.java` | OkHttp WebSocket-এর উপর subscribe/reconnect/backoff logic |

## ৪.১০ `data/notification/NotificationCenter.java`

Notification badge count আর in-app notification list manage করে। WebSocket-এর `/user/queue/notifications` destination subscribe করে (chat socket-এরই একটা ব্যবহার, শুধু ভিন্ন channel-এ) — নতুন notification এলে count বাড়ে, কোনো polling ছাড়াই।

## ৪.১১ `push/` প্যাকেজ — Firebase Cloud Messaging

| ফাইল | কাজ |
|---|---|
| `ZuhooMessagingService.java` | `FirebaseMessagingService` extend করে — নতুন push token তৈরি হলে বা message এলে callback পায় |
| `PushRouter.java` | Notification-এর data payload দেখে কোন Activity খুলতে হবে ঠিক করে (deep link-এর মতো) |
| `PushChannels.java` | ৩টা Notification Channel সেটআপ করে (requests/billing/general) |

## ৪.১২ `data/model/` — DTO Layer (সম্পূর্ণ তালিকা)

`request/` (৩০+ ফাইল) আর `response/` (৪৫+ ফাইল) — প্রতিটা একটা plain Java class, backend API-র JSON structure-এর সাথে হুবহু মেলানো (field নাম, type)। এগুলোর প্রতিটাতে আলাদা করে "কী করে" ব্যাখ্যা করার দরকার নেই — এগুলো ডেটা ধারণ করে মাত্র (POJO — Plain Old Java Object), কোনো logic নেই। গুরুত্বপূর্ণ যেগুলো জানা দরকার:

- **`PageResponse<T>`** — backend-এর সব paginated list response wrap করে (`content`, `totalElements`, `totalPages` ইত্যাদি) — generic type, তাই একটাই ক্লাস সব list-এর জন্য কাজ করে।
- **enum-এর মতো status ক্লাস** (`ServiceRequestStatus`, `InvoiceStatus`, `TicketStatus`, ...) — আসলে এগুলো `public static final String[] VALUES` ধরে রাখা ক্লাস, Java `enum` না। কেন? কারণ backend থেকে নতুন status value এলে (enum-এ যোগ না করেই), string-based approach সেটা crash ছাড়াই handle করতে পারে — অচেনা value থাকলে raw constant-ই দেখানো হয়, app ভাঙে না।

## ৪.১৩ `data/repository/` — সম্পূর্ণ ফাইল তালিকা

প্রতিটা Repository একটা নির্দিষ্ট feature-এর জন্য `ApiService`-কে wrap করে সহজ মেথড দেয় (যেমন `getMyLeaveRequests(callback)`)। প্রতিটা মডিউল অধ্যায়ে (৫-৯) এগুলো বিস্তারিত আলোচনা হবে। এখানে শুধু তালিকা:

| Repository | কোন মডিউলের জন্য |
|---|---|
| `AuthRepository` | Login, register, password reset |
| `AttendanceRepository` | Check-in/out, location settings |
| `CatalogRepository` | Service catalog, categories |
| `ClientProfileRepository`, `ClientRepository` | Client profile ও তথ্য |
| `CompanyOverviewRepository`, `CompanyRepository` | Company overview stats |
| `DashboardRepository` | Role-based dashboard summary |
| `DeviceTokenRepository` | FCM token register/unregister |
| `EmployeeRepository` | Directory-তে employee তালিকা |
| `ExpenseRepository` | Expense claims ও approvals |
| `InvoiceRepository` | Invoice list/detail/PDF |
| `KbArticleRepository` | Knowledge Base article |
| `LeadRepository` | CRM lead তালিকা |
| `LeaveRequestRepository` | Leave request submit/approve |
| `NoticeBoardRepository` | Company announcement, holiday |
| `NotificationRepository` | Notification centre, preferences |
| `PackageRepository`, `SubscriptionRepository` | Service package subscription |
| `PaymentReceiptRepository`, `PaymentRepository` | Payment receipt, SSLCommerz initiate |
| `PayrollRepository` | Payslip তালিকা |
| `ReviewRepository` | Service review submit |
| `SearchRepository` | Global search |
| `ServiceRequestRepository` | মূল Service Request CRUD |
| `SupportTicketRepository` | Support ticket ও chat |
| `TimesheetRepository` | Timesheet log |
| `UploadRepository` | File/image upload |
| `UserProfileRepository` | User account profile |
| `WalletRepository` | Wallet balance, transaction |

---

পরবর্তী অধ্যায়গুলোতে (৫-৯) আমরা প্রতিটা UI module — screen, adapter, আর উপরের repository গুলো কীভাবে একসাথে কাজ করে — সেটা বিস্তারিতভাবে দেখব।
