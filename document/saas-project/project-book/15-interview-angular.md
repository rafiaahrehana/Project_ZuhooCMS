# অধ্যায় ১৫ — Angular: Interview Q&A

## Components ও Architecture

**প্রশ্ন: Standalone Components কী, NgModule-এর চেয়ে সুবিধা কী?**
Standalone কম্পোনেন্ট নিজের dependency (`imports: [...]`) সরাসরি ঘোষণা করে, কোনো `NgModule`-এ রেজিস্টার হওয়ার দরকার নেই। সুবিধা: কম boilerplate, বেশি explicit dependency (একটা কম্পোনেন্ট খুললেই বোঝা যায় সে কী কী ব্যবহার করছে), আর lazy-loading সহজ (`loadComponent`)। এই প্রজেক্টে **শূন্য NgModule** — সব রুট `loadComponent`/`loadChildren` দিয়ে lazy-loaded।

**প্রশ্ন: `@Input()`/`@Output()` কী?**
Parent থেকে child-এ ডেটা পাঠানো `@Input()` (property binding, `[value]="x"`), child থেকে parent-এ ইভেন্ট পাঠানো `@Output()` (`EventEmitter`, `(event)="handler($event)"`)। উদাহরণ: `DuplicateWarningModal`-এর `@Input() match` (কোন ডুপ্লিকেট পাওয়া গেছে) আর `@Output() dismissed` (ইউজার মডাল বন্ধ করলে)।

## Change Detection

**প্রশ্ন: `OnPush` স্ট্র্যাটেজি কী, কেন ব্যবহার করা হয়?**
ডিফল্ট change detection প্রতিটা ব্রাউজার ইভেন্টে পুরো কম্পোনেন্ট ট্রি চেক করে — বড় অ্যাপে এটা ধীর হয়ে যেতে পারে। `OnPush` একটা কম্পোনেন্টকে বলে: শুধু তখনই re-check করো যখন (১) একটা `@Input()` রেফারেন্স বদলায়, (২) এই কম্পোনেন্টের নিজের template-এ কোনো ইভেন্ট ফায়ার হয়, বা (৩) ম্যানুয়ালি `markForCheck()` কল করা হয়। এই প্রজেক্টে ১৪৮টা কম্পোনেন্ট `OnPush` ব্যবহার করে — পারফরম্যান্সের জন্য দাঁড়ানো একটা কনভেনশন।

**প্রশ্ন: `markForCheck()` কখন লাগে?**
যখন কোনো state Angular-এর নিজের ইভেন্ট-হ্যান্ডলিং সিস্টেমের বাইরে থেকে বদলায় — RxJS `subscribe()` কলব্যাক, `setTimeout`, WebSocket মেসেজ। উদাহরণ: `NotificationBell`-এ `unreadCount$.subscribe(n => { this.unread = n; this.cdr.markForCheck(); })` — সাবস্ক্রিপশন কলব্যাক Angular-এর zone-এর "স্বাভাবিক" ট্রিগার পাথের বাইরে, তাই ম্যানুয়ালি বলতে হয় "এখন আবার UI চেক করো"।

## RxJS

**প্রশ্ন: `Observable` বনাম `Promise`?**
`Promise` একবারই resolve হয়, cancel করা যায় না। `Observable` একাধিকবার ভ্যালু emit করতে পারে (stream), আর `unsubscribe()` করে cancel করা যায় — যেমন একটা HTTP রিকোয়েস্ট মাঝপথে বাতিল করা, বা একটা live WebSocket স্ট্রিম শোনা।

**প্রশ্ন: `BehaviorSubject` কী, কেন `Subject`-এর বদলে এটা?**
`BehaviorSubject` সবসময় একটা current ভ্যালু ধরে রাখে, আর নতুন সাবস্ক্রাইবার সাথে সাথেই সেই সর্বশেষ ভ্যালু পায় (subscribe করার আগে emit হওয়া থাকলেও)। প্লেইন `Subject` শুধু ভবিষ্যতের emission পায়। এই প্রজেক্টে `AuthService.currentUser$`, `PermissionService.permissions$` — সবই `BehaviorSubject`, কারণ একটা নতুন কম্পোনেন্ট (দেরিতে সাবস্ক্রাইব করলেও) সাথে সাথেই জানতে চায় "এখন কে লগইন করা আছে", পরের বদলের জন্য অপেক্ষা করতে চায় না।

**প্রশ্ন: `switchMap` বনাম `mergeMap`?**
`switchMap` — নতুন একটা source emit হলে, আগের inner observable **cancel** করে দেয় (শুধু সর্বশেষটার ফলাফল দরকার — যেমন সার্চ-বক্সের autosuggest, প্রতিটা কিস্ট্রোকে আগের রিকোয়েস্ট বাতিল করে নতুনটা পাঠানো)। `mergeMap` — সবগুলো inner observable সমান্তরালে চালায়, কোনোটাই cancel হয় না।

## Routing ও Guards

**প্রশ্ন: `CanActivate` বনাম `CanActivateChild` — পার্থক্য কী, কেন দুটোই দরকার হতে পারে?**
`CanActivate` একটা নির্দিষ্ট রুট অ্যাক্টিভেট হওয়ার আগে চেক করে। `CanActivateChild` parent রুটে বসিয়ে দিলে, সেই parent-এর **প্রতিটা child রুট** activate হওয়ার আগেও একই গার্ড চলে। এই প্রজেক্টে `RoleGuard`-এ `canActivateChild` ইচ্ছাকৃতভাবে parent রুটে (যেমন `crm`, `hrm`) বসানো — কারণ এটা না থাকলে, একই parent-এর দুই sibling child-এর মধ্যে navigation (`/crm/leads → /crm/pipeline`) guard re-evaluation সম্পূর্ণ স্কিপ করে যেত (Angular ধরে নেয় parent যেহেতু আগেই অ্যাক্টিভেট হয়েছে, আবার চেক করার দরকার নেই — যদি না `canActivateChild` স্পষ্টভাবে বলা থাকে)।

## Forms

**প্রশ্ন: Template-driven বনাম Reactive Forms?**
Template-driven (`[(ngModel)]`) — সরল, দ্রুত লেখা যায়, ছোট/মাঝারি ফর্মের জন্য উপযুক্ত, ভ্যালিডেশন টেমপ্লেটে ছড়িয়ে থাকে। Reactive Forms (`FormGroup`/`FormControl`) — TypeScript-এ programmatically সংজ্ঞায়িত, জটিল/dynamic/cross-field ভ্যালিডেশনে ভালো, ইউনিট টেস্ট করা সহজ। এই প্রজেক্টে বেশিরভাগ CRUD ফর্ম template-driven, কিন্তু auth ফ্লো (login, register — cross-field validation দরকার) আর reusable `location` picker reactive forms ব্যবহার করে।

## HTTP ও Interceptors

**প্রশ্ন: HTTP Interceptor কী, কীভাবে কাজ করে?**
প্রতিটা আউটগোয়িং HTTP রিকোয়েস্ট (এবং ইনকামিং রেসপন্স) একটা ইন্টারসেপ্টর চেইনের মধ্য দিয়ে যায় — যেমন middleware। এই প্রজেক্টে `authInterceptor` প্রতিটা রিকোয়েস্টে `Authorization` হেডার যোগ করে, `errorInterceptor` প্রতিটা রেসপন্স এরর গ্লোবালি টোস্ট হিসেবে দেখায়।

**প্রশ্ন: 401 এরর হলে Silent Token Refresh কীভাবে ইমপ্লিমেন্ট করা হয়?**
একটা রিকোয়েস্ট 401 পেলে, ইন্টারসেপ্টর সরাসরি ইউজারকে লগআউট না করে প্রথমে `/auth/refresh` কল করে নতুন টোকেন নেয়, তারপর মূল রিকোয়েস্টটা নতুন টোকেন দিয়ে **retry** করে। যদি একাধিক রিকোয়েস্ট একই সাথে 401 পায়, একটা `isRefreshing` ফ্ল্যাগ + `BehaviorSubject` দিয়ে নিশ্চিত করা হয় শুধু **একটাই** refresh কল হবে, বাকিগুলো সেই একটা refresh-এর ফলাফলের জন্য অপেক্ষা করবে (একাধিক সমান্তরাল refresh কল এড়াতে)।

## State Management

**প্রশ্ন: এই প্রজেক্ট NgRx ব্যবহার করেনি কেন?**
স্কেল অনুযায়ী প্রয়োজনীয়তা — NgRx-এর মতো ভারী স্টেট লাইব্রেরি তখন মূল্যবান যখন অনেক কম্পোনেন্ট জুড়ে জটিল, ইন্টারকানেক্টেড state আছে যেখানে predictable/traceable mutation দরকার (time-travel debugging)। এই প্রজেক্টে বেশিরভাগ ডেটা per-page fetch-and-display (কোনো local cache), শুধু genuinely cross-cutting state (auth, permissions, unread count) একটা সরল `BehaviorSubject`-backed service প্যাটার্নে শেয়ার করা হয়েছে — যথেষ্ট এবং সরল, অতিরিক্ত ইঞ্জিনিয়ারিং এড়ানো।

## Signals (নতুন Angular ফিচার)

**প্রশ্ন: Angular `signal()` কী, `BehaviorSubject`-এর থেকে কীভাবে ভিন্ন?**
`signal()` একটা reactive primitive যা সরাসরি Angular-এর change detection-এর সাথে ইন্টিগ্রেটেড — একটা signal বদলালে, সেটার উপর নির্ভরশীল template/computed value অটোমেটিক আপডেট হয়ে যায়, কোনো `subscribe()`/`markForCheck()` ম্যানুয়ালি করতে হয় না। `BehaviorSubject` একটা RxJS প্রিমিটিভ, যেটাতে সাবস্ক্রাইব করতে হয় আর OnPush-এ ম্যানুয়ালি `markForCheck()` কল করতে হয়। এই প্রজেক্টে দুটোই মিশে ব্যবহৃত হয়েছে — cross-cutting শেয়ার্ড state (auth, permissions) এখনো `BehaviorSubject`, কিন্তু simpler লোকাল UI state (toast লিস্ট, থিম, ড্র্যাগ পজিশন) `signal()` দিয়ে — একটা ট্রানজিশনাল কোডবেস যেখানে দুই প্যাটার্নই সহাবস্থান করছে।

**প্রশ্ন: `computed()` সিগন্যাল কী?**
একটা derived signal যা অন্য signal-এর উপর নির্ভর করে, নিজে থেকে কখনো সরাসরি set হয় না — নির্ভরশীল signal বদলালেই স্বয়ংক্রিয়ভাবে পুনরায় গণনা হয় (memoized — শুধু dependency বদলালেই recompute হয়, প্রতিবার read-এ না)। এটা RxJS-এর `combineLatest`/`map`-এর সিগন্যাল-ভিত্তিক সমতুল্য।

## Dependency Injection (Angular)

**প্রশ্ন: Angular-এ `inject()` ফাংশন কী, কনস্ট্রাক্টর ইনজেকশনের চেয়ে কেন ব্যবহার করা হয় কিছু জায়গায়?**
`inject()` একটা ফাংশন-ভিত্তিক DI মেকানিজম, ক্লাসের কনস্ট্রাক্টরের বাইরেও (যেমন একটা functional interceptor বা guard-এর ভেতরে, যেগুলো ক্লাস না, প্লেইন ফাংশন) ব্যবহার করা যায়। এই প্রজেক্টে `authInterceptor`/`errorInterceptor` (functional interceptor, `HttpInterceptorFn` টাইপ) এবং `AuthGuard`/`RoleGuard`-এর মতো functional guard-এ `inject(AuthService)`, `inject(Router)` ব্যবহার হয় — কারণ এগুলো ক্লাস-ভিত্তিক না, তাই কনস্ট্রাক্টর ইনজেকশনের সুযোগই নেই। Angular-এর নতুন functional API-গুলোর (guard, interceptor, resolver) এটাই standard প্যাটার্ন।

## Lazy Loading

**প্রশ্ন: `loadComponent` বনাম `loadChildren` — কখন কোনটা?**
`loadComponent` একটা সিঙ্গেল স্ট্যান্ডঅ্যালোন কম্পোনেন্ট lazy-load করে একটা নির্দিষ্ট রুটে (যেমন `/ai` → `AiAssistant`)। `loadChildren` একটা পুরো রুট-অ্যারে lazy-load করে একটা feature-এর জন্য (যেমন `/crm/*` → `CRM_ROUTES`, যেখানে আবার একাধিক সাব-রুট `loadComponent` দিয়ে নিজেদের ভেতরেই lazy-loaded)। এই প্রজেক্টে বড় মডিউল (CRM, HRM, Finance) `loadChildren` দিয়ে, ছোট স্ট্যান্ডঅ্যালোন পেজ (AI Settings) সরাসরি `loadComponent` দিয়ে — এভাবে initial bundle ছোট থাকে, ইউজার যে মডিউল খুলবে শুধু তার কোড-ই ডাউনলোড হবে।
