# অধ্যায় ৩ — Frontend Core Architecture

> Angular ২২, TypeScript ৬, `@angular/build:application` (esbuild) বিল্ডার। এই অধ্যায় Angular ইন্টারভিউর জন্য মূল ভিত্তি।

## ৩.১ App স্ট্রাকচার

**শূন্য NgModule — সব standalone।** Angular 19+ থেকে `standalone: true` ডিফল্ট, তাই বেশিরভাগ ফাইলে এটা লেখার দরকারই নেই। `main.ts`-এ `bootstrapApplication(App, appConfig)` — কোনো `AppModule` নেই।

**রাউটিং** — প্রতিটা রুট `loadComponent: () => import(...)` (lazy-loaded standalone component) অথবা `loadChildren` দিয়ে একটা feature-এর রুট অ্যারে (`CRM_ROUTES`, `HRM_ROUTES` ইত্যাদি)। কোনো eager-loaded feature module নেই।

**Guards:**
- **`AuthGuard`** — JWT valid কিনা চেক করে; expired হলে inline-এ refresh টোকেন ট্রাই করে, সফল হলে এগিয়ে যায়, নাহলে লগিন পেজে রিডাইরেক্ট
- **`RoleGuard`** — `route.data['roles']` আর `route.data['requiredPermission']` স্বাধীনভাবে চেক করে ("একটা রুট দুটোর একটা, দুটোই, বা কোনোটাই দিতে পারে")। **`canActivateChild`** ইচ্ছাকৃতভাবে parent রুটে সেট করা — কমেন্টে ব্যাখ্যা: এটা না থাকলে একই parent-এর দুই child-এর মধ্যে lateral navigation (`/crm/leads → /crm/pipeline`) guard re-evaluation সম্পূর্ণ স্কিপ করে যেত

## ৩.২ Core Services — State Management Pattern

**কোনো NgRx/NgXs নেই।** পুরো অ্যাপ-জোড়া প্যাটার্ন: **plain injectable service (`providedIn: 'root'`) যেখানে `BehaviorSubject`-backed public observable এক্সপোজ করা হয়।**

**`AuthService`:**
- `localStorage` কী: `TOKEN_KEY='access_token'`, `REFRESH_TOKEN_KEY='refresh_token'`, `USER_KEY='user'`
- `isAuthenticated$`/`currentUser$` — `BehaviorSubject`
- `PLATFORM_ROLES` অ্যারে — ব্যাকএন্ডের `User.isPlatformUser()`-এর সাথে হুবহু মিরর করা
- **Refresh-token interceptor মেকানিক্স** (`auth.interceptor.ts`, একটা functional interceptor, ক্লাস না): মডিউল-লেভেল `isRefreshing` ফ্ল্যাগ + `refreshedToken$` + `refreshFailed$` দিয়ে "একটাই in-flight refresh, বাকি concurrent 401-গুলো queue-তে রাখা" প্যাটার্ন — যদি ইতিমধ্যে একটা refresh চলছে, নতুন 401-প্রাপ্ত রিকোয়েস্টগুলো নতুন করে `/auth/refresh` কল না করে, বিদ্যমান refresh-এর ফলাফলের জন্য `race()` করে অপেক্ষা করে
- **`initializeAuthState()`-এ `setTimeout(…, 0)` দিয়ে প্রথম লোড deferred** — কেন? Angular-এর `NG0200` circular-dependency এরর এড়াতে: interceptor প্রতিটা HTTP রিকোয়েস্টে `inject(AuthService)` কল করে, `AuthService`-এর কনস্ট্রাক্টরের ভেতরেই synchronously একটা HTTP কল ফায়ার করলে (তখনও DI স্ট্যাক-এ) reentrant injection এরর হতো

**`PermissionService`:**
- `permissions$`/`catalog$` — একই `BehaviorSubject` + `localStorage` প্যাটার্ন
- `hasPermission(code)` — **synchronous**, সরাসরি `permissionsSubject.value` পড়ে (observable subscribe না করে) — যাতে guard/directive সিঙ্ক্রোনাসলি চেক করতে পারে
- `PermissionCode` — plain `string` টাইপ, TS enum না — কারণ সোর্স-অফ-ট্রুথ ব্যাকএন্ডের `GET /users/permissions` endpoint, দুই জায়গায় ডুপ্লিকেট মেইনটেইন করা ঝুঁকিপূর্ণ

**HTTP Interceptors** (দুটো functional interceptor, `app.config.ts`-এ রেজিস্টার্ড):
1. `authInterceptor` — `Authorization` হেডার সংযুক্ত করে, 401 হ্যান্ডল/রিফ্রেশ/রিট্রাই
2. `errorInterceptor` — গ্লোবাল error toast — যেকোনো non-401 এররে toast দেখায়, `SKIP_ERROR_TOAST` `HttpContext` টোকেন দিয়ে opt-out করা যায় (যেসব কলার নিজেই inline error দেখায়, তারা ডুপ্লিকেট toast চায় না)

## ৩.৩ Change Detection — Angular ২২-এর একটা চমক

**গুরুত্বপূর্ণ টেকনিক্যাল পয়েন্ট:** Angular ২২-এ `ChangeDetectionStrategy` enum-এ **তিনটা** মেম্বার, দুইটা না:
```
OnPush = 0   // এখন এটাই সংখ্যাগতভাবে ডিফল্ট মান!
Eager  = 1   // যেকোনো CD traversal-এ সবসময় চেক হয়
Default = 1  // deprecated alias for Eager, সরিয়ে ফেলা হবে
```
এই কোডবেসে `changeDetection` না দিলে এখন **OnPush** সেমান্টিক্স পায় (পুরনো Angular-এ যা CheckAlways ছিল, তার উল্টো!)। `ChangeDetectionStrategy.Eager` (root `App`, আর `stat-card`/`loader`/`empty-state`-এর মতো মূলত স্ট্যাটিক কম্পোনেন্টে ব্যবহৃত) হলো পুরনো "সবসময় চেক করো" স্ট্র্যাটেজির নতুন নাম।

**`markForCheck()` প্যাটার্ন** — ১৩৬টা ফাইলে `this.cdr.markForCheck()` — OnPush-এর সাথী, কোনো state mutation যেটা Angular-এর template-binding/event-handler পাথের বাইরে ঘটে (RxJS subscribe callback, `setTimeout`, WebSocket মেসেজ হ্যান্ডলার) তার পরে ম্যানুয়ালি CD ট্রিগার করতে ব্যবহৃত।

**জোনলেস কিনা — অস্পষ্ট:** `package.json`-এ `zone.js` নেই, `angular.json`-এ কোনো polyfills entry নেই, কিন্তু `app.config.ts`-এ explicit `provideZonelessChangeDetection()` কলও নেই। এটা একটা আকর্ষণীয় ওপেন প্রশ্ন — বইয়ে "এটা সত্যিই জোনলেস চলছে নাকি একটা required provider miss হয়ে গেছে" — এভাবে discussion পয়েন্ট রাখা হয়েছে, নিশ্চিত উত্তর না দিয়ে।

## ৩.৪ Forms — Reactive না, বেশিরভাগ Template-driven

একটা সাধারণ ধারণার বিপরীত — এই কোডবেস **প্রধানত template-driven** (`[(ngModel)]`):
- ১১৮টা ফাইল `FormsModule` ইমপোর্ট করে, ১০২টা টেমপ্লেট `[(ngModel)]` ব্যবহার করে — বেশিরভাগ CRUD স্ক্রিনে ডিফল্ট
- **Reactive Forms মাত্র ৮টা ফাইলে** — auth ফ্লো (login, register, forgot-password) আর `location` শেয়ারড কম্পোনেন্টে, যেখানে ভ্যালিডেশন জটিল বা cross-field

এটা একটা ভালো ইন্টারভিউ পয়েন্ট: heavy business-CRUD স্ক্রিনগুলো `ngModel`-এর সরলতা পছন্দ করে (বড় multi-field ফর্ম সরাসরি একটা plain object-এ bound), কিন্তু auth ফ্লো (`Validators.email`, password confirm) আর reusable picker (`location`) reactive forms ব্যবহার করে কারণ সেখানে structured validation দরকার।

## ৩.৫ HTTP/API Layer

**`ApiService`** (base wrapper) — `get/post/put/patch/delete<T>()`, `postFile()`/`uploadFile()` (multipart), `postText()`(টেক্সট রেসপন্স endpoint-এর জন্য), `getBlob()` (PDF ডাউনলোড), আর **`getPaged<T>()`** — Spring Data-র `Page<T>` JSON শেপ নরমালাইজ করে একটা কনসিস্টেন্ট `PagedResponse<T>`-এ।

৯৯টা feature-service ফাইল `ApiService`-এর উপর বিল্ড করে; মাত্র ৪টা সরাসরি `HttpClient` কল করে (public/unauthenticated বা low-level control দরকার এমন ক্ষেত্রে)।

## ৩.৬ Notification System — দুইটা আলাদা `NotificationService`, নাম গুলিয়ে যায়

**একটা গুরুত্বপূর্ণ নেমিং কোলিশন যা জানা দরকার:**
1. **`shared/services/notification.service.ts`** — UI **toast/snackbar** (success/error/warning/info, ৫ সেকেন্ড পর auto-dismiss)
2. **`core/services/notification.service.ts`** — ডোমেইন **notification-center** (bell dropdown, `/notifications` পেজ, `unreadCount$` — প্রতি ৬০ সেকেন্ডে polling)

দুটো সম্পূর্ণ আলাদা concern, শুধু নাম একই — কোড পড়ার সময় import path দেখে বুঝতে হয় কোনটা।

## ৩.৭ শেয়ারড কম্পোনেন্ট (গুরুত্বপূর্ণ কয়েকটা)

- **`skill-tag-input`** — Chip-স্টাইল ট্যাগ এডিটর, ২৫০ms debounced autosuggest
- **`file-upload`** — জেনেরিক uploader, `variant: 'file'|'avatar'` আলাদা endpoint বেছে নেয়, client-side pre-check (সার্ভার ভ্যালিডেশনের replacement না, শুধু আগেভাগে feedback)
- **`duplicate-warning-modal`** — CRM-এ ব্যবহৃত non-blocking নাজ, কখনো "undo" অপশন দেয় না
- **`has-permission` directive** — `*appHasPermission="'CODE'"` — `permissions$` সাবস্ক্রাইব করে `ViewContainerRef` দিয়ে ভিউ দেখায়/লুকায়

**একটা dead-code উদাহরণ:** `shared/services/api.service.ts` একটা ডুপ্লিকেট (`core/services/api.service.ts`-এর প্রায় হুবহু কপি, সামান্য ভিন্ন `PagedResponse` শেপ নিয়ে) — কিন্তু **কোথাও ইমপোর্ট হয় না** (৯৯টা ফাইল core-টা ইমপোর্ট করে, শেয়ারড-টা ০টা)। একটা রেখে-যাওয়া/পরিত্যক্ত ডুপ্লিকেট — কোড রিভিউতে "খুঁজে বের করো" ব্যায়ামের ভালো উদাহরণ।

## ৩.৮ "Ask AI" ফ্লোটিং বাটন — ড্র্যাগ মেকানিক্স

রুট `App` কম্পোনেন্টে সরাসরি লেখা (আলাদা directive/service না):
- `posX`/`posY` — `signal<number|null>(null)` (null = ডিফল্ট CSS পজিশন ব্যবহার করো)
- `onMouseDown`/`onTouchStart` — transient `window` লিসেনার সংযুক্ত করে, যেগুলো `mouseup`/`touchend`-এ সরিয়ে ফেলা হয় (স্থায়ী গ্লোবাল লিসেনার রাখা হয় না)
- **৪px থ্রেশহোল্ড** — ৪ পিক্সেলের কম নড়াচড়া হলে এটা "ক্লিক", তার বেশি হলে "ড্র্যাগ" — এভাবে click-vs-drag ডিসঅ্যাম্বিগুয়েশন করা হয়, যাতে একটা ড্র্যাগ শেষে accidentally `/ai`-তে নেভিগেট না হয়ে যায়

## ৩.৯ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: এই প্রজেক্ট NgRx ব্যবহার করেনি কেন, শুধু `BehaviorSubject` কেন যথেষ্ট?**
উত্তর: NgRx-এর মতো একটা ভারী স্টেট-ম্যানেজমেন্ট লাইব্রেরি বড় হয় যখন state অনেক জায়গা থেকে জটিলভাবে mutate হয় আর time-travel debugging/predictable-reducer দরকার হয়। এই অ্যাপে বেশিরভাগ ডেটা per-component fetch-and-forget (কোনো local caching layer নেই) — শুধু সত্যিকারের cross-cutting concern (auth state, permissions, unread count) `BehaviorSubject` দিয়ে শেয়ার করা হয়েছে, যা এই স্কেলে যথেষ্ট এবং সরল।

**প্রশ্ন: OnPush strategy-তে `markForCheck()` কখন লাগে, কখন লাগে না?**
উত্তর: OnPush কম্পোনেন্ট শুধু তখনই re-check হয় যখন: (১) একটা `@Input()` রেফারেন্স বদলায়, (২) কম্পোনেন্টের ভেতরে template-বাউন্ড কোনো ইভেন্ট (click, ইত্যাদি) ট্রিগার হয়, বা (৩) `markForCheck()` ম্যানুয়ালি কল হয়। RxJS `subscribe()` কলব্যাক, `setTimeout`, WebSocket মেসেজ — এগুলো Angular-এর নিজের ইভেন্ট-হ্যান্ডলিং সিস্টেমের বাইরে ঘটে, তাই এই ধরনের asynchronous callback-এর ভেতর state বদলালে `markForCheck()` ম্যানুয়ালি কল করতে হয়, নাহলে UI আপডেট হবে না যতক্ষণ না অন্য কোনো কারণে CD ট্রিগার হয়।

**প্রশ্ন: `authInterceptor` কীভাবে নিশ্চিত করে যে একই সাথে একাধিক 401-এ শুধু একটাই refresh কল হবে?**
উত্তর: একটা মডিউল-লেভেল `isRefreshing` বুলিয়ান ফ্ল্যাগ আর `refreshedToken$` (`BehaviorSubject`) ব্যবহার করা হয়। প্রথম 401 পেলে `isRefreshing = true` সেট করে refresh কল শুরু হয়। এর মধ্যে যদি আরও রিকোয়েস্ট 401 পায়, তারা দেখে `isRefreshing` আগে থেকেই `true` — তাই নতুন refresh কল না করে, `refreshedToken$`-এর পরবর্তী emission-এর জন্য `race()` করে অপেক্ষা করে (নতুন টোকেন পেলে নিজেদের রিকোয়েস্ট retry করে)। refresh শেষ হলে `isRefreshing = false` হয়ে যায়, `refreshedToken$` নতুন টোকেন emit করে সবাইকে জানিয়ে দেয়।

**প্রশ্ন: `RoleGuard`-এ `permissionService.ensureLoaded()` কেন কল করা হয়, `hasPermission()` সরাসরি কল করলে সমস্যা কী হতো?**
উত্তর: `hasPermission()` সিঙ্ক্রোনাসলি `permissionsSubject.value` পড়ে — কিন্তু একটা hard reload-এ (ব্রাউজার রিফ্রেশ করে সরাসরি একটা permission-গেটেড রুটে ল্যান্ড করা), `permissions$`-এর ভ্যালু তখনো লোড হয়নি (asynchronous `GET /users/permissions` কল এখনো চলছে)। যদি guard সরাসরি `hasPermission()` কল করতো, এটা stale/empty ডেটা দেখে ভুলভাবে 403 দিয়ে দিতে পারতো। `ensureLoaded()` guard-কে বলে "যদি এখনো লোড না হয়ে থাকে, প্রথমে অপেক্ষা করো real fetch-এর জন্য, তারপর চেক করো" — একটা race condition প্রতিরোধ।
