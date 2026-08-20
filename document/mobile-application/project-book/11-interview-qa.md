# অধ্যায় ১১ — ইন্টারভিউ প্রশ্নোত্তর (Interview Q&A)

> এই অধ্যায়টা ৫ ভাগে সাজানো: Java Core, Android Fundamentals, Architecture, Networking, আর সবশেষে এই প্রজেক্ট নিয়ে সরাসরি প্রশ্ন। প্রতিটা উত্তর ছোট, স্পষ্ট, আর যেখানে সম্ভব এই প্রজেক্টের আসল কোড থেকে উদাহরণ দেওয়া। **মুখস্থ না করে বোঝার চেষ্টা করো** — interviewer follow-up প্রশ্ন করবেই।

---

## ১১.১ Java Core

**প্রশ্ন: `volatile` কীওয়ার্ড কী করে?**
একটা variable-কে `volatile` ঘোষণা করলে — (১) প্রতিটা thread সেই variable-এর **সর্বশেষ মান সরাসরি main memory থেকে পড়ে** (নিজের CPU cache থেকে না), আর (২) compiler/CPU সেই variable-কে ঘিরে instruction reorder করতে পারে না (happens-before guarantee)। এই প্রজেক্টে `ApiClient.retrofit` field-টা `volatile`, কারণ double-checked locking pattern-এ এটা ছাড়া একটা thread আধা-তৈরি object দেখে ফেলতে পারে (অধ্যায় ৪.১ দেখো)।

**প্রশ্ন: Interface বনাম Abstract Class — কখন কোনটা?**
Interface — শুধু contract (কী করতে হবে), কোনো implementation না (Java 8-এর পর default method বাদে)। একাধিক interface implement করা যায়। Abstract class — কিছু implementation **সহ** থাকতে পারে, কিন্তু একটা ক্লাস শুধু একটাই extend করতে পারে। এই প্রজেক্টে `CachedListViewModel` একটা abstract class, কারণ এটার common logic (`start()`, `refresh()`, `onRefreshFailed()`) সব subclass-এর জন্য একই — শুধু `fetch()`, `idOf()`, `loadErrorRes()` টুকু আলাদা।

**প্রশ্ন: Generic Type (`<T>`) কেন ব্যবহার করা হয়?**
Type-safety আর code-reuse একসাথে পাওয়ার জন্য। `CachedListViewModel<T>` একটাই ক্লাস, কিন্তু `LeaveRequestResponse`, `ExpenseResponse` — যেকোনো টাইপের জন্য কাজ করে, প্রতিটার জন্য আলাদা ক্লাস না লিখেই, আর compile-time-এই ভুল টাইপ ধরা পড়ে (runtime-এ `ClassCastException` হওয়ার বদলে)।

**প্রশ্ন: `synchronized` ব্লক কী করে, কখন দরকার?**
একটা সময়ে **একটা মাত্র thread**-কে সেই ব্লকের ভেতরে ঢুকতে দেয়। যখন একাধিক thread একই shared resource (variable, object) বদলাতে পারে, race condition এড়াতে এটা লাগে। এই প্রজেক্টে `TokenAuthenticator`-এ `REFRESH_LOCK` ব্যবহার হয়েছে, যাতে একসাথে একাধিক API call token expire হলে **শুধু একটা** refresh request পাঠানো হয়, তিনটা না (অধ্যায় ৪.৩ দেখো)।

**প্রশ্ন: Checked বনাম Unchecked Exception?**
Checked exception (যেমন `IOException`) — compiler বাধ্য করে হয় `catch` করতে, নয়তো `throws` দিয়ে propagate করতে। Unchecked (`RuntimeException` ও তার subclass) — কম্পাইলার জোর করে না। এই প্রজেক্টে `TokenAuthenticator.requestNewTokens()` ইচ্ছাকৃতভাবে `IOException` catch **না করে** propagate করতে দেয় — কারণ network error আর "session সত্যিই শেষ" আলাদা জিনিস (অধ্যায় ৪.৩)।

**প্রশ্ন: `equals()` আর `hashCode()` একসাথে override করতে হয় কেন?**
যদি দুটো object `equals()`-এ সমান হয়, কিন্তু `hashCode()` আলাদা হয়, তাহলে `HashMap`/`HashSet`-এ তারা **আলাদা bucket-এ** পড়ে যাবে — সমান হওয়া সত্ত্বেও `contains()` মিথ্যা false ফেরত দিতে পারে। তাই এই দুটো সবসময় একসাথে override করতে হয় (এই নিয়মটাকে "hashCode-equals contract" বলে)।

---

## ১১.২ Android Fundamentals

**প্রশ্ন: Activity Lifecycle-এর প্রতিটা মেথড কখন কী করা উচিত?**
- `onCreate()` — one-time setup (view inflate, listener attach)।
- `onStart()`/`onStop()` — screen visible/invisible হওয়ার মুহূর্ত।
- `onResume()`/`onPause()` — screen foreground-এ active/inactive হওয়ার মুহূর্ত (এখানেই সাধারণত camera/sensor শুরু-বন্ধ করা হয়)।
- `onDestroy()` — cleanup (কিন্তু বেশিরভাগ resource lifecycle-aware component নিজেই সামলায়, ম্যানুয়াল cleanup কম দরকার হয় আজকাল)।

**প্রশ্ন: Configuration change (screen rotate)-এ কী হয়, কীভাবে ডেটা বাঁচানো যায়?**
Rotate করলে Activity **destroy হয়ে আবার নতুন করে তৈরি হয়** by default (যদি না `configChanges` manifest-এ handle করা থাকে, যেটা সাধারণত করা উচিত না)। এতে সাধারণ local variable হারিয়ে যায়। **সমাধান:** ViewModel ব্যবহার করা — ViewModel configuration change-এ বেঁচে থাকে (এটাই ViewModel-এর মূল উদ্দেশ্যগুলোর একটা)।

**প্রশ্ন: `Serializable` বনাম `Parcelable` — Intent-এ object পাঠানোর জন্য কোনটা ভালো?**
`Parcelable` — Android-specific, reflection ব্যবহার করে না, তাই **অনেক দ্রুত**। `Serializable` — Java standard, কিন্তু reflection-ভিত্তিক, ধীর, আর extra garbage তৈরি করে। Android-এ সবসময় `Parcelable` ভালো, বিশেষত যদি বড় বা ঘনঘন পাঠানো object হয়।

**প্রশ্ন: `Context` কী, কয় ধরনের আছে?**
`Context` হলো application environment-এর একটা handle — resource, file, database access করার জন্য দরকার। প্রধানত দুই ধরনের: **Application Context** (পুরো app-এর জীবনকাল জুড়ে বাঁচে) আর **Activity Context** (সেই Activity যতক্ষণ বাঁচে)। **ভুল Context ব্যবহার করলে memory leak হতে পারে** — যেমন, একটা long-lived object (singleton) যদি Activity Context ধরে রাখে, তাহলে সেই Activity destroy হয়ে গেলেও garbage collect হতে পারবে না। এই প্রজেক্টে `AppGraph`, `TokenManager` ইত্যাদি সবসময় `context.getApplicationContext()` ব্যবহার করে, এই কারণেই।

**প্রশ্ন: `RecyclerView` কেন `ListView`-এর চেয়ে ভালো?**
`RecyclerView`-তে **ViewHolder pattern বাধ্যতামূলক** (`ListView`-তে ঐচ্ছিক ছিল, অনেকে ভুলে যেত), যা `findViewById()` বারবার কল হওয়া আটকায়। `RecyclerView`-তে **LayoutManager আলাদা** (vertical/horizontal/grid বদলানো সহজ), আর **item animation** built-in সাপোর্ট আছে।

---

## ১১.৩ Architecture (MVVM, Repository, LiveData)

**প্রশ্ন: MVVM প্যাটার্নের ৩টা অংশ কী কী, প্রতিটার দায়িত্ব কী?**
- **Model** — ডেটা ও business logic (এই প্রজেক্টে: Repository + API/DTO layer)।
- **View** — UI (Activity/Layout XML) — শুধু দেখানো আর user input capture করা, কোনো logic না।
- **ViewModel** — View আর Model-এর মাঝে সেতু — UI state ধরে রাখে (LiveData দিয়ে), View-কে সরাসরি রেফারেন্স করে না (তাই rotate/destroy-তে সমস্যা হয় না)।

**প্রশ্ন: Repository Pattern কেন ব্যবহার করা হয়?**
Repository একটা **abstraction layer** — ViewModel/Activity-কে জানতে হয় না ডেটা কোথা থেকে আসছে (network, local cache, নাকি দুটোর মিশ্রণ)। এতে —
১. **Testability** বাড়ে (Repository mock করে ViewModel টেস্ট করা যায় আসল network call ছাড়াই)।
২. একই ডেটার উৎস বদলালে (যেমন cache যোগ করা) শুধু Repository বদলাতে হয়, বাকি কোড অক্ষত থাকে — এই প্রজেক্টে ঠিক এটাই হয়েছে `CachedListViewModel`-এ (Repository-র উপরে caching layer বসানো হয়েছে)।

**প্রশ্ন: LiveData lifecycle-aware মানে কী, এটা কীভাবে memory leak আটকায়?**
`liveData.observe(this, observer)` কল করলে LiveData জানে `this` (LifecycleOwner, যেমন Activity)-এর lifecycle state কী। Activity `DESTROYED` হয়ে গেলে LiveData **স্বয়ংক্রিয়ভাবে সেই observer সরিয়ে দেয়** — ম্যানুয়ালি `removeObserver()` কল করতে হয় না। এটা পুরনো observer pattern-এর তুলনায় বড় সুবিধা, কারণ পুরনো pattern-এ observer manually unregister না করলে Activity-র reference থেকে যেত, GC হতে পারত না — memory leak।

**প্রশ্ন: কেন সব screen-এ ViewModel ব্যবহার করা হয়নি এই প্রজেক্টে?**
সৎ উত্তর: প্রতিটা screen-এ ViewModel বাধ্যতামূলক করলে ছোট, simple screen (যেমন profile edit, যেখানে caching/complex-state কিছু নেই)-এও extra boilerplate তৈরি হয়। যেখানে caching+loading-state ম্যানেজমেন্ট জটিল (list screen), সেখানে ViewModel-এর সুবিধা স্পষ্ট। এটা একটা **pragmatic** সিদ্ধান্ত — pattern সব জায়গায় ধর্মের মতো মানা না, বরং যেখানে বাস্তব সুবিধা দেয় সেখানে ব্যবহার করা।

**প্রশ্ন: Dependency Injection (DI) কী, কেন দরকার?**
DI মানে — একটা ক্লাস নিজে তার dependency **তৈরি না করে**, বাইরে থেকে **পেয়ে যায় (injected হয়)**। সুবিধা: testability (mock dependency দেওয়া যায়), loose coupling। এই প্রজেক্টে Hilt/Dagger-এর বদলে **হাতে-লেখা DI** (`AppGraph`) ব্যবহার হয়েছে — ছোট অ্যাপের জন্য annotation processor-এর খরচ (build time) তার সুবিধার চেয়ে বেশি মনে হয়েছিল (অধ্যায় ২.৪)।

---

## ১১.৪ Networking (Retrofit/OkHttp)

**প্রশ্ন: Interceptor বনাম Authenticator (OkHttp) — পার্থক্য কী?**
**Interceptor** প্রতিটা request-এ (যাওয়ার পথে বা response আসার পরে, যেভাবে লেখা) কাজ করে — সবসময়। **Authenticator** শুধু response **401 (বা 407)** পেলে কাজ করে, আর একটা নতুন Request ফেরত দিতে পারে যেটা দিয়ে OkHttp **retry** করবে। এই প্রজেক্টে `AuthInterceptor` (header যোগ করে, সবসময়) আর `TokenAuthenticator` (token refresh করে, শুধু 401-এ) — এই দুটোর পার্থক্য অধ্যায় ৪-এ বিস্তারিত।

**প্রশ্ন: REST API-তে GET/POST/PUT/PATCH/DELETE-এর পার্থক্য কী?**
- **GET** — ডেটা পড়া, কোনো side-effect নেই, body থাকে না।
- **POST** — নতুন resource তৈরি করা।
- **PUT** — সম্পূর্ণ resource replace করা (পুরোটা পাঠাতে হয়)।
- **PATCH** — resource-এর **আংশিক** আপডেট (শুধু যা বদলাচ্ছে)।
- **DELETE** — মুছে ফেলা।

এই প্রজেক্টে একটা ভালো বাস্তব উদাহরণ: notification preferences আপডেট হয় `PUT`-এ (`PATCH` না) — as-built doc-এ স্পষ্ট লেখা আছে এটা ভুল করলে 405 (Method Not Allowed) আসবে।

**প্রশ্ন: HTTP Status Code 401 বনাম 403 — পার্থক্য?**
**401 Unauthorized** — তুমি কে **প্রমাণ করতে ব্যর্থ** হয়েছ (token নেই/expired/ভুল)। **403 Forbidden** — তোমাকে **চেনা গেছে**, কিন্তু তোমার এই কাজ করার অনুমতি নেই। এই পার্থক্যটা এই প্রজেক্টে token-refresh logic-এর মূল ভিত্তি — শুধু 401-এ refresh/retry করা হয়, 403-এ করলে infinite loop-এর ঝুঁকি (অধ্যায় ৪.৩)।

**প্রশ্ন: JWT (JSON Web Token) কী, কীভাবে কাজ করে?**
JWT তিনটা অংশের একটা string: `header.payload.signature` — Base64-এ এনকোড করা। Payload-এ থাকে claims (userId, role, companyId, expiry — এই প্রজেক্টে যেমন)। Server signature verify করে নিশ্চিত হয় token-টা বদলানো হয়নি। **Stateless** — server-কে session মনে রাখতে হয় না, প্রতিটা request নিজেই "প্রমাণ" বহন করে। এই প্রজেক্টে access token ১৫ মিনিট বাঁচে (কম সময়, ঝুঁকি কম), refresh token ৭ দিন (দীর্ঘ, কিন্তু revoke করা যায় server-এ)।

**প্রশ্ন: WebSocket বনাম REST API — কখন কোনটা?**
REST — request-response, client-ই সবসময় শুরু করে। **Server push করতে পারে না** নিজে থেকে। WebSocket — একটা persistent, দ্বিমুখী connection, **server যেকোনো সময় client-কে push করতে পারে**। Real-time chat-এর জন্য WebSocket দরকার (নতুন মেসেজ এলে সাথে সাথে জানা দরকার, client বারবার "নতুন মেসেজ আছে?" জিজ্ঞেস করতে থাকলে সেটা অদক্ষ — polling)।

---

## ১১.৫ এই প্রজেক্ট নিয়ে সরাসরি প্রশ্ন (STAR Method দিয়ে উত্তর দাও)

> **STAR method:** Situation (পরিস্থিতি) → Task (কাজ) → Action (তুমি কী করেছ) → Result (ফলাফল)। প্রতিটা "গল্প" প্রশ্নে এই কাঠামোয় উত্তর দাও।

**প্রশ্ন: "এমন একটা সময়ের কথা বলুন যখন আপনি একটা অগোছালো/legacy কোডবেস উন্নত করেছেন।"**
→ অধ্যায় ১০ (Design System case study) ব্যবহার করো পুরোটা। Situation: ৪৫-screen অ্যাপ functionally ঠিক কিন্তু UI অসংগত। Task: production-মানের UI/UX আনা, functionality না ভেঙে। Action: কেন্দ্রীয় design system, exemplar-first approach, ব্যাচে ব্যাচে redesign, প্রতি ধাপে build verify। Result: ৪৫টা screen redesign, ২টা নতুন shared component, ১৫০+ ফাইল বদলেছে, কোনো business logic না ভেঙে।

**প্রশ্ন: "একটা কঠিন bug-এর কথা বলুন যেটা আপনি ধরেছেন/ঠিক করেছেন।"**
→ `TokenAuthenticator`-এর race-condition সমাধান (অধ্যায় ৪.৩) — একাধিক thread একসাথে token refresh চাইলে কী হতো, আর `synchronized` লক দিয়ে কীভাবে সমাধান হলো। অথবা: `Widget.Zuhoo.StatusBadge` style-এর implicit parent bug (অধ্যায় ১০.৬) — ছোট কিন্তু শেখার মতো একটা Android-specific gotcha।

**প্রশ্ন: "একটা technical decision-এর কথা বলুন যেখানে আপনি একটা 'standard' সমাধানের বদলে অন্য কিছু বেছে নিয়েছেন, আর কেন।"**
→ Dagger/Hilt-এর বদলে হাতে-লেখা DI (অধ্যায় ২.৪), অথবা STOMP library-র বদলে হাতে-লেখা ~১২০ লাইনের codec (অধ্যায় ২.৮)। দুটোতেই মূল যুক্তি: "প্রজেক্টের আকার আর প্রকৃত প্রয়োজনের সাথে সমাধানের জটিলতা মেলানো, ট্রেন্ডি টুল ব্যবহার করার জন্য না।"

**প্রশ্ন: "কীভাবে নিশ্চিত করেন আপনার পরিবর্তন কিছু ভাঙছে না?"**
→ প্রতিটা redesign ব্যাচের পর `./gradlew assembleDevDebug` চালিয়ে compile-time verify, তারপর real emulator-এ চালিয়ে screenshot দিয়ে visual verify (অধ্যায় ১০.৬)। আর ViewBinding নিজেই একটা safety net — ভুল id রেফারেন্স করলে build fail হয়, runtime crash না।

**প্রশ্ন: "Multi-tenant অ্যাপে ডেটা isolation (একটা কোম্পানি আরেকটার ডেটা দেখতে না পারা) কীভাবে নিশ্চিত করা হয়?"**
→ এই অ্যাপে মূল isolation **backend-এ** হয় (Hibernate tenant filter + JWT-এর `companyId` claim)। Android app সবসময় `/my` বা `/me` endpoint ব্যবহার করে (`/api/service-requests/my`), কখনো সরাসরি id দিয়ে অন্য কারো ডেটা query করে না — client-side এটা "নিরাপদ" ডিজাইন প্যাটার্ন, কিন্তু আসল security enforcement সবসময় server-side হতে হয়, এটা বলা জরুরি।

**প্রশ্ন: "একটা payment/financial feature বানানোর সময় বিশেষ কী সতর্কতা নিতে হয়?"**
→ (১) টাকার জন্য কখনো `float`/`double` না, সবসময় `BigDecimal` (floating-point rounding error আসল টাকার হিসাবে বিপজ্জনক)। (২) Payment gateway-র redirect/callback-কে **কখনো সরাসরি বিশ্বাস না করা** — সবসময় backend থেকে আসল status আবার fetch করা (এই প্রজেক্টে exponential backoff দিয়ে, অধ্যায় ৭)। (৩) Money/personal ডেটার স্ক্রিনে `FLAG_SECURE` — screenshot/recent-apps thumbnail বন্ধ রাখা।

---

পরবর্তী ধাপ: Appendix-এ (অধ্যায় ১২) পুরো প্রজেক্টের ফাইল তালিকা এক জায়গায় পাবে — কোনো নির্দিষ্ট ফাইল দ্রুত খুঁজে পেতে সেটা ব্যবহার করো।
