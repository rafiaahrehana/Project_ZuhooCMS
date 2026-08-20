# অধ্যায় ২ — টেক স্ট্যাক ও আর্কিটেকচার (Tech Stack & Architecture)

## ২.১ কেন Java, Kotlin না?

এই প্রজেক্টের ভাষা **Java**, Kotlin না। কারণ সহজ: backend-টা **Spring Boot + Java**-তে লেখা, তাই টিমের মধ্যে একটা consistency রাখা হয়েছে। আরেকটা বাস্তব কারণ — **Jetpack Compose Kotlin-only**, মানে Compose ব্যবহার করতে চাইলে Kotlin বাধ্যতামূলক। যেহেতু Java রাখা হয়েছে, UI-ও তাই **XML layout** দিয়ে বানানো হয়েছে, Compose দিয়ে না।

> **Interview tip:** "আপনারা Kotlin কেন ব্যবহার করলেন না?" — এই প্রশ্নে defensive হওয়ার দরকার নেই। উত্তর: এটা একটা conscious trade-off ছিল, team consistency (backend-ও Java) আর existing XML-based UI maintain করার সিদ্ধান্ত। Kotlin/Compose নতুন প্রজেক্টে ভালো, কিন্তু existing বড় Java codebase-কে migrate করার cost অনেক সময় benefit-এর চেয়ে বেশি হয়ে যায়।

## ২.২ UI Layer — XML + ViewBinding + Material Components

- **XML layouts** — প্রতিটা screen-এর জন্য একটা `.xml` ফাইল (`res/layout/activity_*.xml`)।
- **View Binding** (Compose না, findViewById-ও না) — `app.buildFeatures.viewBinding true` অন করা আছে। প্রতিটা layout XML থেকে Gradle automatically একটা `ActivityXxxBinding` ক্লাস generate করে। এতে করে —
  - **কোনো `findViewById()` লাগে না** — সরাসরি `binding.myButton` লিখলেই হয়।
  - **Compile-time null safety** — ভুল id লিখলে app crash হবে না, বরং **build-ই fail হবে**। এটা `findViewById()`-এর চেয়ে বড় সুবিধা, কারণ `findViewById()` ভুল id দিলে runtime-এ `NullPointerException` দেয়, যেটা ধরা কঠিন।
- **Material Components (Material 2, Material 3 না)** — থিম হলো `Theme.MaterialComponents.DayNight.DarkActionBar`। Material Design library-র version **`1.10.0`**।

> **Material 2 বনাম Material 3 — কেন Material 2?** এই প্রজেক্টে (এবং এই বইয়ের অধ্যায় ১০-এ যে বড় redesign নিয়ে আলোচনা হয়েছে সেখানে) ইচ্ছাকৃতভাবে Material 3-এ migrate করা হয়নি। কারণ — গোটা থিমিং framework বদলে ফেলার ঝুঁকি (৪৫টা screen-এ ripple effect পড়তে পারে) একটা visual redesign-এর তুলনায় অনেক বেশি। এটা একটা ভালো architecture decision-এর উদাহরণ: **"যেটা দরকার সেটাই বদলাও, পুরো framework বদলে ফেলো না"**।

## ২.৩ Architecture Pattern — MVVM (আংশিক)

এই প্রজেক্ট **পুরোপুরি MVVM না, একটা practical hybrid**। বুঝে নেওয়া যাক:

```
                    ┌─────────────┐
                    │   Activity   │  ← UI, user interaction
                    └──────┬───────┘
                           │ observes LiveData
                    ┌──────▼───────┐
                    │  ViewModel   │  ← শুধু ৮টা screen-এ আছে (list screen গুলোতে বেশি)
                    └──────┬───────┘
                           │ calls
                    ┌──────▼───────┐
                    │  Repository  │  ← প্রতিটা feature module-এর নিজস্ব Repository
                    └──────┬───────┘
                           │ calls
                    ┌──────▼───────┐
                    │  ApiService  │  ← Retrofit interface, backend-এর সাথে কথা বলে
                    └──────────────┘
```

**গুরুত্বপূর্ণ সত্যি কথা:** সব screen-এ ViewModel নেই। যেগুলোতে caching/loading-state ম্যানেজ করা জটিল (list screen — leave requests, expenses, ইত্যাদি), সেগুলো `CachedListViewModel`-এর subclass ব্যবহার করে। কিন্তু সাধারণ, simple screen (যেমন profile edit) সরাসরি Activity থেকে Repository কল করে, কোনো ViewModel ছাড়াই।

> **Interview tip:** এইখানে সৎ থাকা জরুরি। যদি জিজ্ঞেস করা হয় "আপনার পুরো অ্যাপ কি MVVM?" — উত্তর দাও: "মূলত MVVM pattern অনুসরণ করা হয়েছে, বিশেষ করে যেসব স্ক্রিনে caching আর loading state আছে। কিন্তু ছোট, simple screen-এ ViewModel ছাড়াই সরাসরি Repository ব্যবহার করা হয়েছে — কারণ প্রতিটা screen-এ ViewModel বাধ্যতামূলক করলে boilerplate code বাড়ে, বাস্তব সুবিধা ছাড়াই।" এটা দেখায় তুমি pattern-টা **understand** করো, শুধু **copy-paste** করোনি।

### কেন Fragment ব্যবহার হয়নি?

এই প্রজেক্টে **একটাও Fragment নেই** — প্রতিটা screen একটা আলাদা **Activity**। নেভিগেশন হয় সরাসরি `Intent` দিয়ে।

> এটা শুরু থেকে পরিকল্পিত সিদ্ধান্ত ছিল না — স্ক্রিনগুলো এভাবেই তৈরি হয়েছিল, আর পরে ৪৫টা Activity-কে Fragment-এ রূপান্তর করাটা অনেক বড় refactor হয়ে যেত, যার থেকে ব্যবহারকারী কোনো সুবিধা পেত না। তাই এটাই থেকে গেছে।
>
> এর একটা ফল হলো — প্রতিটা bottom-nav tab-এর state Activity instance-এ থাকে, আর Android system দরকার হলে ব্যাকগ্রাউন্ডে থাকা tab-টা reclaim (kill) করে দিতে পারে। এটার সমাধান: প্রতিটা screen `onResume()`-এ আবার data load করে, আর প্রথমে Room cache থেকে দেখায় — তাই ব্যবহারকারী কোনো glitch টের পায় না।

**Bottom Navigation** কীভাবে কাজ করে: `ui/common/BottomNavActivity` নামের একটা base class আছে যেটা `setContentView()`-কে override করে — একটা screen-এর নিজের layout-কে একটা shared frame-এর ভেতরে ঢুকিয়ে দেয়, তার নিচে bottom nav bar থাকে। কোনো Activity `BottomNavActivity` extend করলেই সে bottom nav পায় — layout ফাইলে কিছু বদলাতে হয় না। Tab পাল্টানোর সময় `FLAG_ACTIVITY_REORDER_TO_FRONT` ব্যবহার হয়, যাতে একই tab বারবার নতুন instance হিসেবে stack-এ না জমে।

## ২.৪ Dependency Injection — হাতে-তৈরি (Hilt না)

Standard practice হলো **Dagger/Hilt** ব্যবহার করা, কিন্তু এই প্রজেক্টে **hand-rolled (হাতে বানানো) DI** ব্যবহার হয়েছে — `ZuhooApplication` → `di/AppGraph`।

> **কেন Hilt না?** পরিকল্পনায় লেখা ছিল — "এই সাইজের অ্যাপের জন্য plain manual DI-ও যথেষ্ট"। ২৮টা Activity জুড়ে একটা annotation processor (Hilt) চালানোর যে খরচ (build time বাড়া, complexity), সেটা তার থেকে যা লাভ হতো তার চেয়ে বেশি হতো। `AppGraph` single class-এ থাকে: token store, chat socket, Room cache, notification centre।
>
> **Interview tip:** "Dagger/Hilt না ব্যবহার করে DI কীভাবে করলেন?" — এটা শোনায় যেন একটা দুর্বলতা, কিন্তু আসলে এটা একটা matured engineering decision-এর ভালো উদাহরণ: **প্রতিটা tool সব জায়গায় দরকার হয় না**। ছোট অ্যাপে ভারী framework আনলে সেটা "over-engineering"। এই উত্তরটা প্রস্তুত রাখো — এটা interviewer-কে impress করে, কারণ বেশিরভাগ ক্যান্ডিডেট শুধু "Hilt ব্যবহার করেছি" বলে, কেন করেছে সেটা বোঝে না।

## ২.৫ Networking Layer — Retrofit + OkHttp + Gson

| Library | Version | কাজ |
|---|---|---|
| Retrofit | 3.0.0 | REST API call-কে Java interface-এ রূপান্তর করে (`ApiService`) |
| Retrofit Gson Converter | 3.0.0 | JSON ↔ Java object রূপান্তর |
| OkHttp | 5.4.0 | আসল HTTP client, connection pooling, interceptor chain |
| OkHttp Logging Interceptor | 5.4.0 | Debug build-এ request/response log করে (release build-এ বাদ, security-র জন্য) |
| Gson | 2.14.0 | JSON parsing |

**কেন Retrofit + OkHttp, শুধু OkHttp না?** Retrofit একটা **type-safe** layer দেয় OkHttp-র উপর। মানে —

```java
// Retrofit ছাড়া (raw OkHttp) হলে এভাবে লিখতে হতো:
Request request = new Request.Builder().url(baseUrl + "/api/leave-requests").build();
// ... response parse করা, error handling, সব ম্যানুয়ালি

// Retrofit দিয়ে:
@GET("api/leave-requests/my")
Call<PageResponse<LeaveRequestResponse>> getMyLeaveRequests();
```

একটা interface লিখলেই Retrofit বাকি সব (URL বানানো, request পাঠানো, JSON parse করা) নিজে করে দেয়।

### দুইটা গুরুত্বপূর্ণ ক্লাস — `AuthInterceptor` আর `TokenAuthenticator`

এই দুটোর পার্থক্য বোঝা **খুব common একটা interview প্রশ্ন**:

- **`AuthInterceptor`** — **প্রতিটা** outgoing request-এর সাথে `Authorization: Bearer <token>` header জুড়ে দেয়, request পাঠানোর **আগে**।
- **`TokenAuthenticator`** — response **401 (Unauthorized)** পেলে **পরে** কাজ করে। OkHttp-র `Authenticator` interface implement করে — access token expire হয়ে গেলে **refresh token** দিয়ে নতুন access token আনে, তারপর **একবার** original request আবার পাঠায় (retry)।

> **কেন দুটো আলাদা ক্লাস, একটা না?** কারণ এই দুটোর কাজের **সময়** আলাদা — Interceptor request যাওয়ার পথে কাজ করে, Authenticator response আসার পরে, ব্যর্থ (401) হলে। এই আলাদা করাটা **Single Responsibility Principle**-এর একটা বাস্তব উদাহরণ।
>
> **401 বনাম 403 — এই পার্থক্যটা এই প্রজেক্টে গুরুত্বপূর্ণ:** Backend 401 দেয় token expire/invalid হলে (retry করা উচিত), আর 403 দেয় genuine permission নেই বললে (retry করে লাভ নেই)। শুধু 401-এ retry করা হয়। ভুলে 403-এও retry করলে infinite loop-এর ঝুঁকি থাকত।
>
> **Login/Register endpoint-এ token refresh চলে না কেন?** কারণ ওখানে 401 মানে "ভুল পাসওয়ার্ড" — সেটা একটা আসল উত্তর, token refresh করে "ঠিক করার" কিছু নেই। ভুল করে refresh করতে গেলে একজন সবে ভুল টাইপ করা ব্যবহারকারীর session মুছে যেতে পারত।

### Token সংরক্ষণ — `EncryptedSharedPreferences`

Access token আর refresh token সাধারণ `SharedPreferences`-এ না রেখে **`EncryptedSharedPreferences`** (AndroidX Security library, version `1.1.0`) ব্যবহার হয়েছে — ডেটা disk-এ **encrypted** থাকে। এটা `TokenManager` ক্লাস manage করে।

> **FCM push token আলাদা কেন রাখা হলো (`PushTokenStore`)?** কারণ logout-এর সময় session clear হয়ে যায়, কিন্তু push token unregister করার API call-টার জন্য token-টা তখনো দরকার। তাই push token আলাদা, সাধারণ (encrypted না) prefs-এ রাখা হয়েছে, যাতে সেটা `clearSession()`-এর পরেও বেঁচে থাকে।

## ২.৬ Local Cache — Room (Offline Read Support)

**Room** (version 2.6.1) ব্যবহার হয়েছে, কিন্তু **একটা অস্বাভাবিক ডিজাইন সিদ্ধান্ত** সহ: প্রতিটা data type-এর জন্য আলাদা table (Entity) বানানো হয়নি। বরং **একটাই generic JSON-backed table** (`ListCache`) — যেখানে যেকোনো list-এর ডেটা JSON string হিসেবে save হয়।

> **কেন এভাবে?** কারণ এই cache **শুধু list দেখানোর জন্য পড়া হয়**, কখনো field ধরে query (যেমন "amount > 500 এমন সব invoice") করা হয় না। যদি প্রতিটা DTO-র জন্য আলাদা Entity + TypeConverter বানানো হতো, সেটা হতো "structure যার কোনো বাস্তব প্রয়োজন নেই" — অপ্রয়োজনীয় জটিলতা। এটা আরেকটা ভালো উদাহরণ: **"তোমার ব্যবহারের ধরন অনুযায়ী ডিজাইন করো, কল্পিত ভবিষ্যতের জন্য না"**।

Room-এর ফাইলগুলো: `CacheDao` (queries), `CacheMeta` + `CachedItem` (Entity), `ZuhooDatabase` (Room database class)।

**Offline behavior:** Cache **read-only** — কোনো write (নতুন leave request submit করা ইত্যাদি) কখনো offline queue হয় না, সব mutation-এর জন্য internet লাগবেই। শুধু "শেষ যা লোড হয়েছিল সেটা দেখানো" — সাথে "শেষ কখন সেভ হয়েছিল" লেখা একটা label (`CacheStamp` component, "showing saved data from …")।

## ২.৭ Push Notifications — Firebase Cloud Messaging (FCM)

`firebase-messaging` (BOM version 33.7.0) দিয়ে push notification পাঠানো হয়। App background-এ থাকলে বা বন্ধ থাকলেও notification পৌঁছাতে পারে — এটা এমন কিছু যা শুধু WebSocket দিয়ে সম্ভব না (WebSocket connection app বন্ধ হয়ে গেলে বেঁচে থাকে না)।

- `ZuhooMessagingService` — FCM থেকে message receive করার service।
- `PushRouter` — কোন ধরনের notification এলে অ্যাপের কোন screen-এ নিয়ে যাবে, সেই routing logic।
- `PushChannels` — Android Notification Channel setup (৩টা category: requests / billing / general — যাতে ব্যবহারকারী চাইলে শুধু billing notification বন্ধ করতে পারে, request notification হারাবে না)।

## ২.৮ রিয়েল-টাইম চ্যাট — হাতে-লেখা STOMP (একটা লাইব্রেরির বদলে)

এটা এই প্রজেক্টের **সবচেয়ে interesting technical decision**গুলোর একটা।

Backend WebSocket-এর উপর **STOMP protocol** ব্যবহার করে (raw WebSocket, SockJS **না**)। Android-এ সাধারণত এর জন্য `StompProtocolAndroid`-এর মতো third-party library ব্যবহার করা হয়, কিন্তু এখানে **নিজে থেকে ~১২০ লাইনের STOMP frame codec লেখা হয়েছে** (`data/chat/StompFrame.java`), OkHttp-র নিজস্ব `WebSocket` ক্লাসের উপর।

> **কেন third-party library না?** কারণ এই প্রজেক্টে STOMP protocol-টা খুবই সীমিত ও সহজ (simple text framing) — একটা পুরো library আনার দরকার নেই যেটা হয়তো maintained-ও না। "নিজে লেখা ~১২০ লাইন কোড" vs "একটা unmaintained dependency" — এখানে নিজে লেখাটাই যুক্তিসঙ্গত ছিল, কারণ এতে unit test দিয়ে ঠিকভাবে verify করা যায় (framing bug হলে silent failure হয়, তাই test থাকা জরুরি)।

**STOMP frame গঠন** (protocol-টা বোঝা জরুরি):
```
CONNECT
accept-version:1.2
host:localhost

\0
```
প্রতিটা frame `\0` (NUL character) দিয়ে শেষ হয়। Header হয় `key:value` লাইন আকারে, প্রথম blank line header আর body-কে আলাদা করে।

**Authentication trick:** WebSocket handshake-এ `Authorization` header পাঠানো যায় না (browser/client limitation), তাই JWT token **query parameter** হিসেবে পাঠানো হয়: `ws://host/ws?token=<accessToken>`। `WebSocketAuthInterceptor` (backend-এ) সেটা যাচাই করে।

> ⚠️ **এটা একটা known trade-off**, দুর্বলতা না ধরে নিয়েই সমাধান করা হয়েছে — URL-এ token থাকলে সেটা server access log-এ থেকে যেতে পারে। Production-এ `wss://` (encrypted) ব্যবহার করে ঝুঁকি কমানো হয়েছে, কিন্তু আসল fix হতো একটা short-lived "handshake ticket" ব্যবহার করা। **এটা বলতে পারা একটা ভালো sign — তুমি জানো তোমার সমাধানের সীমাবদ্ধতা কী, এবং real fix কী হতো।**

**একটা কানেকশন, পুরো app জুড়ে:** Socket-টা `AppGraph`-এ owned হয় (singleton-এর মতো)। Screen-রা `subscribe(destination, listener)` কল করে আর ফেরত পাওয়া handle cancel করে — প্রথম subscription-এ socket খোলে, শেষটা চলে গেলে বন্ধ হয়।

**Reconnect ও re-subscribe:** Connection ছিঁড়ে গেলে ১ সেকেন্ড থেকে শুরু করে ৩০ সেকেন্ড পর্যন্ত exponential backoff দিয়ে আবার connect করার চেষ্টা হয়, আর automatic re-subscribe হয় — কারণ broker session মরে গেলে পুরনো subscription ভুলে যায়, শুধু reconnect করলে socket চালু থাকবে কিন্তু কিছুই শুনবে না।

## ২.৯ Build Configuration — Product Flavors

```groovy
flavorDimensions += "env"
productFlavors {
    dev {
        applicationIdSuffix ".dev"
        buildConfigField "String", "API_BASE_URL", '"http://10.0.2.2:8086/"'
    }
    prod {
        buildConfigField "String", "API_BASE_URL", '"https://api.zuhoo.app/"'
    }
}
```

দুটো **product flavor** — `dev` আর `prod`। `dev`-এর নিজস্ব `applicationIdSuffix ".dev"` আছে, তাই **একই ডিভাইসে দুটো ভার্সন একসাথে install করা যায়** (দুটো আলাদা app হিসেবে গণ্য হয়)।

`10.0.2.2` হলো **Android Emulator-এর একটা বিশেষ IP** — এটা emulator থেকে host machine-এর `localhost`-কে বোঝায়। Real device-এ টেস্ট করতে হলে host-এর LAN IP address লাগবে।

**WebSocket URL আলাদাভাবে declare করা হয়নি** — সেটা `API_BASE_URL` থেকেই derive হয় (`http` → `ws`, `https` → `wss`)। কারণ দুটো আলাদা constant রাখলে সেগুলো সময়ের সাথে **desync (একে অপরের থেকে আলাদা)** হয়ে যেতে পারে — একটা বদলালে আরেকটা ভুলে যাওয়া common bug।

## ২.১০ Security Checklist (as-built)

| আইটেম | অবস্থা |
|---|---|
| `android:allowBackup="false"` | ✅ — ADB backup দিয়ে ডেটা বের করা বন্ধ |
| Logout-এ token revoke + device unregister + cache clear | ✅ |
| `FLAG_SECURE` টাকা/personal screen-এ | ✅ — screenshot/recent-apps thumbnail বন্ধ (`ui/common/SecureScreen`) |
| Logging interceptor release build-এ বাদ | ✅ — `BuildConfig.DEBUG` দিয়ে গার্ড করা |
| R8 + resource shrinking | ✅ — release APK 9.8 MB থেকে 2.2 MB |
| Cleartext traffic (HTTP, non-HTTPS) | ✅ — শুধু dev flavor-এ emulator-এর জন্য অনুমোদিত, prod-এ সম্পূর্ণ বন্ধ |
| Certificate pinning | ⚠️ কাঠামো তৈরি আছে কিন্তু বন্ধ রাখা হয়েছে — ভুল pin দিলে পুরো অ্যাপ অচল হয়ে যাবে নতুন release ছাড়া কোনো উপায় নেই বলে |
| WebSocket URL-এ token | ⚠️ known limitation, উপরে ব্যাখ্যা করা হয়েছে |

> **Interview tip:** Security section-টা খুব ভালো করে পড়ো। "আপনার অ্যাপে security কীভাবে handle করেছেন?" — এটা প্রায় প্রতিটা interview-তে আসে। উপরের টেবিলটা একটা ready-made উত্তর।

## ২.১১ সারসংক্ষেপ ডায়াগ্রাম

```
┌─────────────────────────── Android App ───────────────────────────┐
│                                                                      │
│  Activity (৪৫টা) ──uses──> ViewBinding                             │
│       │                                                             │
│       ├──observes──> ViewModel (৮টা, শুধু জটিল list screen-এ)      │
│       │                    │                                        │
│       └────────calls───────┴──> Repository (feature-ভিত্তিক)        │
│                                       │                              │
│                                       ▼                              │
│                              ApiService (Retrofit interface)         │
│                                       │                              │
│                    ┌──────────────────┼──────────────────┐          │
│                    ▼                  ▼                  ▼          │
│            AuthInterceptor   TokenAuthenticator   OkHttp Client      │
│           (header যোগ করে)   (401 হলে refresh)                      │
│                                       │                              │
└───────────────────────────────────────┼──────────────────────────────┘
                                         ▼
                          backend/Zuhoo (Spring Boot, :8086)
                                         │
                                         ▼
                          PostgreSQL (businessflow database)
                          — Angular web app (BusinessOS)-এর সাথে শেয়ার্ড
```

পরের অধ্যায়ে আমরা Android-এর basic ধারণাগুলো (Activity lifecycle, Intent, RecyclerView ইত্যাদি) ঝালিয়ে নেব, যাতে module-ভিত্তিক অধ্যায়গুলো পড়তে সমস্যা না হয়।
