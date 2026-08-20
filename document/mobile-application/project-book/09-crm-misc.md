# অধ্যায় ৯ — CRM, Directory, Search ও Company Overview

এই অধ্যায়ে আমরা চারটা ছোট কিন্তু গুরুত্বপূর্ণ প্যাকেজ দেখব — `ui/crm/`, `ui/directory/`, `ui/search/`, `ui/overview/`। এগুলো মূল `docs/android-client-app-plan.md`-এর মূল ফিচার-ম্যাপে (§৫, §৫a) সরাসরি ছিল না — মনে হচ্ছে পরে যোগ হয়েছে, "web app-এর যা আছে তার একটা lightweight mobile glance" হিসেবে। চারটাই আকারে ছোট (প্রতিটা ১-৩টা ফাইল), কিন্তু প্রতিটাতে একটা করে শেখার-মতো প্যাটার্ন আছে।

লক্ষ করার মতো একটা বড় স্থাপত্যগত পার্থক্য: অধ্যায় ৪-এ আমরা `CachedListViewModel`-ভিত্তিক pattern দেখেছিলাম (ViewModel + LiveData + Room cache)। এই চারটা স্ক্রিন তা ব্যবহার করে **না** — এরা সরাসরি Activity থেকে Repository কল করে, `Callback` দিয়ে UI আপডেট করে। এগুলো ছোট, staff/owner-only, কম-ব্যবহৃত utility স্ক্রিন — তাই পুরো ViewModel+cache স্থাপত্য বসানো এখানে ওভারকিল হতো। এটা একটা ভালো architectural judgment call: **সব স্ক্রিনে এক pattern জোর করে চাপাতে নেই**, স্ক্রিনের গুরুত্ব ও জটিলতা অনুযায়ী সিদ্ধান্ত নিতে হয়।

---

## ৯.১ CRM / Leads মডিউল (`ui/crm/`)

### `LeadListActivity.java` — "My Leads", পুরো CRM নয়

ক্লাসের উপরে থাকা comment-টাই এই স্ক্রিনের scope সবচেয়ে ভালো বলে দেয়:

```java
/**
 * "My Leads" — a lightweight glance, not the full CRM: view leads assigned to me and change
 * status. No pipeline board, no activity history, no contacts — those stay web-only.
 */
public class LeadListActivity extends AppCompatActivity {
```

এটা staff-only ফিচার — `DashboardActivity`-তে `binding.btnMyLeads` শুধু `isStaff()` (অর্থাৎ `EMPLOYEE` বা `COMPANY_OWNER`) হলেই visible হয়:

```java
binding.btnMyLeads.setVisibility(View.VISIBLE); // isStaff() ব্লকের ভিতরে
```

**Backend endpoint:** `GET api/crm/leads/my` — `id`-ভিত্তিক এন্ডপয়েন্ট নয়, `/my` — অর্থাৎ tenant isolation-এর পাশাপাশি "শুধু আমাকে assign করা lead" ফিল্টারও সার্ভার-সাইডে হয়। ক্লায়েন্ট কোনো id পাঠায় না, তাই ভুল করে অন্য কারো lead দেখার সুযোগই নেই।

**তিনটা ধাপের flow:**

1. **`load()`** — `getMyLeads()` কল করে, `PageResponse<LeadResponse>` থেকে `getContent()` বের করে adapter-এ বসায়। খালি লিস্ট হলে `StateView.showEmpty()`, নাহলে `showContent()`।
2. **`openDetailDialog(lead)`** — RecyclerView-তে item click করলে একটা `MaterialAlertDialogBuilder` দিয়ে detail popup — industry, phone, email, estimated value, notes — যেগুলো non-empty শুধু সেগুলোই `StringBuilder` দিয়ে জোড়া হয়। এখানে একটা আলাদা detail Activity/layout না বানিয়ে dialog ব্যবহার করাটা যুক্তিসঙ্গত — এই স্ক্রিন এমনিতেই "quick glance", পূর্ণাঙ্গ detail screen দরকার নেই।
3. **`promptChangeStatus(lead)`** — dialog-এর ভেতর একটা `Spinner` বসিয়ে status পরিবর্তনের UI। `LeadStatus.VALUES[]` অ্যারে থেকে `currentIndex` বের করে spinner-এ preselect করা হয়:

```java
int currentIndex = java.util.Arrays.asList(LeadStatus.VALUES).indexOf(lead.getStatus());
if (currentIndex >= 0) {
    statusSpinner.setSelection(currentIndex);
}
```

Save চাপলে `updateStatus()` কল হয়, যা `PATCH api/crm/leads/{id}` হিট করে (partial update — পুরো lead object নয়, শুধু status)। সফল হলে `load()` আবার কল করে লিস্ট রিফ্রেশ করে — এখানে কোনো optimistic UI নেই, সার্ভার confirm করার পরই UI বদলায়।

> **Interview প্রশ্ন: "PATCH কেন, PUT নয়?"**
> `PUT` পুরো resource replace করার সিমান্টিক্স বহন করে (পুরো object পাঠাতে হয়), `PATCH` partial update বোঝায়। এখানে ক্লায়েন্ট শুধু status বদলাচ্ছে — বাকি contactName, email, notes ইত্যাদি অপরিবর্তিত থাকা উচিত। `PATCH` দিয়ে `UpdateLeadStatusRequest` (শুধু status field ধারণ করে এমন একটা ছোট DTO) পাঠানো তাই বেশি সঠিক এবং নিরাপদ — accidentally অন্য field খালি (null) দিয়ে overwrite হওয়ার ঝুঁকি নেই।

### `LeadStatusBadge.java` — repeated pattern-এর একটা উদাহরণ

এই ফাইলের কোডের ভেতরের comment নিজেই বলে দেয়:

```java
// Same two-static-method shape as ui.servicerequest.StatusBadge.
public final class LeadStatusBadge {
```

`colorFor()` আর `labelFor()` — দুটোই static, `private` constructor সহ non-instantiable utility class (Java-তে "static factory" নয়, বরং pure "namespace" pattern)। এই একই shape এই প্রজেক্টে অন্তত ৮ বার repeat হয়েছে (`ExpenseStatusBadge`, `ui.servicerequest.StatusBadge`, ইত্যাদি) — ইচ্ছাকৃতভাবে একটা shared abstract base class বানানো হয়নি, কারণ প্রতিটা ডোমেইনের status enum আলাদা এবং mapping যুক্তিও আলাদা; জোর করে generalize করলে সুবিধার চেয়ে জটিলতা বেশি হতো।

`StatusBadgeView.bind()` (ui/common) এই রঙ+লেবেলকে আসল UI badge-এ রেন্ডার করে — soft-tint pill background (রঙের ~১২% অ্যালফা), full-color text, আর status tone অনুযায়ী icon (success→check, danger→cancel, warning→clock)। `LeadAdapter.ViewHolder.bind()`-এ কল হয়:

```java
StatusBadgeView.bind(binding.itemStatusBadge,
        LeadStatusBadge.colorFor(context, lead.getStatus()),
        LeadStatusBadge.labelFor(context, lead.getStatus()));
```

অর্থাৎ দুই স্তরের বিভাজন: **domain-specific status→color/label mapping** (`LeadStatusBadge`) বনাম **shared visual rendering** (`StatusBadgeView`)। এটা ভালো separation — নতুন domain যোগ করতে হলে শুধু একটা ছোট `XxxStatusBadge` ক্লাস লিখলেই হয়, রেন্ডারিং কোড ছোঁয়া লাগে না।

### `LeadAdapter.java`

সাধারণ RecyclerView adapter — `submitList()` পুরনো লিস্ট clear করে নতুনটা `addAll()` করে `notifyDataSetChanged()` কল করে (কোনো `DiffUtil` নেই — ছোট লিস্টের জন্য এটা যথেষ্ট, প্রজেক্টের অন্য অনেক adapter-ও একই প্যাটার্ন মেনে চলে)।

### `ui/crm/` ফাইল-তালিকা

| ফাইল | কাজ |
|---|---|
| `LeadStatusBadge.java` | `LeadStatus` → রঙ (status_success/danger/warning/info) ও লোকালাইজড লেবেল ম্যাপ করে। |
| `LeadAdapter.java` | Lead লিস্টের RecyclerView adapter — contact name, company, phone, status badge বাইন্ড করে, item click শোনে। |
| `LeadListActivity.java` | "My Leads" স্ক্রিন — assigned leads লোড করে, detail dialog দেখায়, status change করার UI দেয়। staff-only (EMPLOYEE/COMPANY_OWNER)। |

---

## ৯.২ Employee Directory (`ui/directory/`)

### `DirectoryActivity.java`

এটা `LeadListActivity`-র চেয়েও সরল — কোনো interaction নেই, শুধু company-র সব employee-র একটা read-only লিস্ট। `AccountActivity` থেকে খোলা হয়, এবং visibility rule:

```java
if (Role.COMPANY_OWNER.equals(tokenManager.getRole())
        || Role.EMPLOYEE.equals(tokenManager.getRole())) {
    binding.btnDirectory.setVisibility(View.VISIBLE);
}
```

অর্থাৎ `CLIENT` রোলে এই বাটনই দেখা যায় না — এটা staff-facing internal ফিচার, কোনো client-এর নিজের কোম্পানির কর্মী-তালিকা দেখার দরকার নেই।

`onCreate()`-এই সরাসরি `load()` কল হয় (অন্য স্ক্রিনের মতো `onResume()`-এ নয় — কারণ directory data কম পরিবর্তনশীল, প্রতিবার স্ক্রিনে ফিরে আসলে রিফ্রেশ করার দরকার নেই)। `GET api/employees` কল হয়, `PageResponse<EmployeeResponse>` থেকে content বের হয়, খালি হলে `showEmpty(ic_inbox, ...)`, ব্যর্থ হলে `showError()` — যেখানে retry action `load()`-কে আবার কল করে (`v -> load()`)।

### `EmployeeResponse.java` — tenant/security discipline model-level-এও প্রয়োগ করা হয়েছে

এই DTO-র উপরের comment-টা এই অধ্যায়ের সবচেয়ে গুরুত্বপূর্ণ security পয়েন্ট:

```java
// Directory-only fields. The backend's EmployeeResponse also carries salary, bank account,
// and national ID fields — deliberately not declared here so they're never parsed into
// memory on a screen that lists every employee in the company.
```

ব্যাকএন্ডের `EmployeeResponse` (পুরো object) বেতন, ব্যাংক অ্যাকাউন্ট, national ID-র মতো sensitive field-ও বহন করে (HR মডিউলের জন্য দরকার)। কিন্তু Android app-এর `EmployeeResponse` DTO **ইচ্ছাকৃতভাবে** সেই field-গুলো declare করে না — Gson শুধু যে field-গুলো ক্লাসে আছে সেগুলোই parse করে, বাকিগুলো response JSON-এ থাকলেও silently ignore হয়ে যায়।

> **Interview প্রশ্ন: "সার্ভার তো পুরো object পাঠাচ্ছে, তাহলে ক্লায়েন্ট-সাইডে field বাদ দেওয়ার লাভ কী — এটা তো real security না?"**
> সত্যি কথা: এটা **defense in depth**-এর একটা স্তর, network-level "real" security নয় — response এখনো wire-এ পুরোটাই যায়, HTTPS দিয়ে এনক্রিপ্টেড থাকলেও ডিভাইসে পৌঁছায়। আসল access control ব্যাকএন্ডে হওয়া উচিত (এই endpoint-এর জন্য salary/bank-এর মতো field আলাদা, permission-gated endpoint দিয়ে সার্ভ করা)। কিন্তু ক্লায়েন্ট-সাইডে declare না করাটাও মূল্যবান: (১) এই sensitive data কখনো app memory-তে deserialize হয়ে বসে থাকে না, (২) accidental log/crash-report/screenshot-এ leak হওয়ার সুযোগ কমে, (৩) ভবিষ্যতে কোনো developer ভুল করে `employee.getSalary()` লিখে ফেললেও কম্পাইল-টাইমেই ব্যর্থ হবে, রানটাইমে চুপচাপ sensitive data দেখিয়ে ফেলবে না। প্রকৃত fix হলো ব্যাকএন্ডে directory endpoint-এর জন্য একটা trimmed DTO বানানো (defense-in-depth-এর দুই স্তরই থাকা উচিত), কিন্তু client-side trimming একটা সস্তা, কার্যকর দ্বিতীয় স্তর।

### `DirectoryAdapter.java` — null-safe join pattern

`joinNonEmpty()` হেল্পার লক্ষ করার মতো:

```java
private String joinNonEmpty(String a, String b) {
    if (TextUtils.isEmpty(a)) return b == null ? "" : b;
    if (TextUtils.isEmpty(b)) return a;
    return a + " · " + b;
}
```

জব টাইটেল বা ডিপার্টমেন্ট — যেকোনো একটা missing থাকলেও (`" · "` দুইবার না দেখিয়ে) সঠিকভাবে জোড়া লাগায়, আর দুটোই খালি হলে পুরো row (`itemJobTitleAndDept`/`itemContact`) `GONE` করে দেয়, যাতে খালি bullet-separator নিয়ে একটা ফাঁকা লাইন না দেখা যায়। ছোট কিন্তু বাস্তব-জীবনের ডেটার এলোমেলোভাবে missing হওয়ার (কারো ফোন নম্বর নেই, কারো ডিপার্টমেন্ট নেই) সাথে ভালোভাবে মানিয়ে নেওয়া UI ডিটেইল।

### `ui/directory/` ফাইল-তালিকা

| ফাইল | কাজ |
|---|---|
| `DirectoryAdapter.java` | Employee লিস্টের RecyclerView adapter — নাম, job title+department, email+phone (null-safe জোড়া দিয়ে) দেখায়। |
| `DirectoryActivity.java` | Company-র সব employee-র read-only directory — `GET api/employees` লোড করে, staff-only (COMPANY_OWNER/EMPLOYEE)। |

---

## ৯.৩ Global Search (`ui/search/`)

### `SearchActivity.java` — একাধিক entity type জুড়ে flat search

ক্লাস-লেভেল comment স্কোপ স্পষ্ট করে:

```java
/**
 * Flat cross-module search — leads, clients, opportunities, service requests, tickets, invoices
 * all come back in one list, tagged by type. No deep-linking into each result type yet (the
 * backend returns a web route in `link`, unused here); this is a "find it, then go look it up in
 * the right screen yourself" tool for now.
 */
```

`GET api/search?q={query}` একটাই endpoint, `GlobalSearchResponse` ফেরত দেয় — `{ totalMatches, results: [SearchResultItem] }`। প্রতিটা `SearchResultItem`-এ `type` (LEAD/CLIENT/OPPORTUNITY/SERVICE_REQUEST/TICKET/INVOICE), `id`, `title`, `subtitle` থাকে। এখানে result-এ ক্লিক করলে সংশ্লিষ্ট detail screen-এ deep-link করা **হয়নি** — backend প্রতিটা result-এর সাথে একটা web `link` field পাঠায়, কিন্তু app সেটা ব্যবহার করে না। এটা একটা conscious scope-cut, over-engineering এড়ানোর উদাহরণ — "খুঁজে বের করো, তারপর নিজে গিয়ে সঠিক স্ক্রিনে দেখো" এই সীমিত কাজটাই এখন এই ফিচারের যথেষ্ট।

### দুই ধরনের empty state — search UX-এর সবচেয়ে গুরুত্বপূর্ণ প্যাটার্ন

`onCreate()`-এই, কোনো search করার আগে, একটা "pre-query" state দেখানো হয়:

```java
// Nothing searched yet — distinct from "searched, found nothing" below, same
// ic_search-vs-ic_inbox + copy split used on other search-like screens.
binding.stateView.showEmpty(R.drawable.ic_search,
        R.string.empty_search_prompt_title, R.string.empty_search_prompt_subtitle);
```

স্ট্রিং: *"Search for anything" / "Find leads, clients, invoices, tickets, and more."* — আইকন `ic_search`।

আর সার্চ চালানোর পরে ফলাফল খালি হলে, `runSearch()`-এর ভেতরে সম্পূর্ণ **আলাদা** state:

```java
if (results.isEmpty()) {
    binding.stateView.showEmpty(R.drawable.ic_inbox,
            R.string.empty_search_results, R.string.empty_search_results_subtitle);
}
```

স্ট্রিং: *"No results found" / "Try a different search term."* — আইকন `ic_inbox`।

দুটো state-ই `StateView.showEmpty()` কল করে, দুটোই ভিজুয়ালি "খালি স্ক্রিন" — কিন্তু আলাদা icon, আলাদা title, আলাদা subtitle। কেন?

> **Interview প্রশ্ন: "Empty vs no-results state কেন আলাদা রাখলেন — দুটোই তো একই রকম দেখতে (কিছুই নেই)?"**
> এই দুইটা state-এর **কারণ সম্পূর্ণ আলাদা**, তাই ব্যবহারকারীর জন্য দরকারি পরবর্তী পদক্ষেপও আলাদা:
> - **Pre-query state** ("Search for anything"): ব্যবহারকারী এখনো কিছু টাইপ করেননি। এখানে বার্তার কাজ হলো **guidance/onboarding** — "কী কী খোঁজা যায়" (leads, clients, invoices, tickets) বলে দেওয়া, যাতে ব্যবহারকারী বুঝতে পারেন এই টুলটা কী কী কভার করে। এখানে "কিছু পাওয়া যায়নি" বললে ভুল বার্তা যাবে — কিছু খোঁজাই হয়নি!
> - **No-results state** ("No results found"): ব্যবহারকারী ইতিমধ্যে একটা query চালিয়েছেন এবং তার ফলাফল শূন্য। এখানে বার্তার কাজ হলো **recovery guidance** — "অন্য শব্দ দিয়ে চেষ্টা করুন", কারণ হয়তো বানান ভুল, বা খুব specific/narrow query।
>
> যদি এই দুটো state-কে একই কপি দিয়ে merge করে দেওয়া হতো (ধরুন শুধু "No results"), তাহলে স্ক্রিন খোলার সাথে সাথেই ব্যবহারকারী একটা negative/confusing বার্তা দেখতেন, অথচ তিনি এখনো কিছুই খোঁজেননি — যেটা প্রথম ইম্প্রেশনে বিভ্রান্তিকর। আলাদা রাখাটা একটা সাধারণ, প্রমাণিত search-UX প্র্যাকটিস — Google, GitHub-এর মতো প্রায় সব ভালো search UI-তেই এই দুই state আলাদা।

কোডে এটা implement করার জায়গাগুলো দেখলে বোঝা যায় flow-টা কীভাবে কাজ করে:
- `onCreate()` → prompt state (একবারই, স্ক্রিন খোলার সময়)।
- `runSearch()` শুরুতে → `showLoading()`।
- সফল response, `results` খালি → no-results state।
- সফল response, `results` non-empty → `showContent()` (StateView আড়াল, RecyclerView দেখা যায়)।
- network/parse error → `showContent()` + `Toast` (এখানে prompt/no-results state-এর বদলে content আড়াল হয়ে যায়, এবং error শুধু toast দিয়ে জানানো হয় — স্ক্রিনটা ফাঁকা RecyclerView-তে ফিরে যায়, যা এই ছোট utility screen-এর জন্য যথেষ্ট, `StateView.showError()`-এর মতো dedicated retry card এখানে ব্যবহার করা হয়নি)।

### IME action হ্যান্ডলিং

```java
binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
    if (actionId == EditorInfo.IME_ACTION_SEARCH
            || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
        runSearch();
        return true;
    }
    return false;
});
```

দুটো শর্ত `||` দিয়ে চেক করা — কারণ soft keyboard-এর "Search" action button (`IME_ACTION_SEARCH`, layout-এ `imeOptions="actionSearch"` দিয়ে সেট করা) আর হার্ডওয়্যার/কিছু কিবোর্ডের plain Enter key — দুটোই একই কাজ করা উচিত। শুধু একটা চেক করলে কোনো কোনো ডিভাইস/কিবোর্ডে সার্চ ট্রিগার না-ও হতে পারত।

### `SearchResultTypeLabels.java` ও `SearchResultAdapter.java`

`SearchResultTypeLabels` ঠিক `LeadStatusBadge`-এর মতোই shape — একটা `switch` দিয়ে raw backend constant (`"LEAD"`, `"CLIENT"`, ...) কে লোকালাইজড স্ট্রিং-এ ম্যাপ করে, অজানা type হলে raw string-ই fallback হিসেবে ফেরত দেয় (crash না করে)। `SearchResultAdapter` সরল তিন-field bind: title, subtitle, আর type label (chip/tag হিসেবে)।

### Multi-tenant নিরাপত্তা — search-এও কি leak হতে পারে?

`api/search` কোনো `companyId` প্যারামিটার নেয় না — শুধু `q`। প্ল্যান ডকুমেন্টের §২ অনুযায়ী: *"Multi-tenant safety is enforced server-side via the `tenantFilter` Hibernate filter... The app sends `Authorization` and nothing else."* অর্থাৎ tenant scoping পুরোপুরি JWT-র `companyId` claim থেকে সার্ভার-সাইড Hibernate filter দিয়ে হয় — ক্লায়েন্ট কোনো tenant identifier পাঠায়ই না, তাই "ভুল tenant id পাঠিয়ে অন্য কোম্পানির ডেটা সার্চ করে ফেলা" — এই আক্রমণের ধরনটাই এই ডিজাইনে সম্ভব নয় (কোনো id পাঠানোর সুযোগই নেই)। একই কারণে `DirectoryActivity`-র `api/employees`ও tenant-scoped — কোনো companyId param নেই, JWT-ই একমাত্র সোর্স।

> **Interview প্রশ্ন: "Global search-এর মতো একটা ফিচারে multi-tenant data isolation কীভাবে নিশ্চিত করবেন?"**
> সবচেয়ে নিরাপদ ডিজাইন হলো: tenant scoping ক্লায়েন্ট-থেকে-পাঠানো কোনো প্যারামিটারের উপর নির্ভর না করা। এখানে যদি `api/search?companyId=X&q=...` এভাবে ডিজাইন হতো, তাহলে ক্লায়েন্ট (বা bug/tampered request) ভুল/অন্য companyId পাঠালে সেটা IDOR (Insecure Direct Object Reference)-জাতীয় ঝুঁকি তৈরি করত — সার্ভারকে প্রতিটা রিকোয়েস্টে verify করতে হতো যে দাবি করা companyId, JWT-র companyId-র সাথে মেলে কিনা। এই প্রজেক্ট সেই পুরো ক্লাস অফ বাগ এড়িয়ে গেছে: tenant id ক্লায়েন্ট কখনো পাঠায়ই না, সার্ভার JWT থেকে companyId বের করে Hibernate-এর `tenantFilter` দিয়ে **প্রতিটা query**-তে স্বয়ংক্রিয়ভাবে `WHERE company_id = ?` বসিয়ে দেয়। এটা "secure by construction" — developer প্রতিটা নতুন query-তে ম্যানুয়ালি tenant check যোগ করতে ভুলে গেলেও leak হয় না।

### `ui/search/` ফাইল-তালিকা

| ফাইল | কাজ |
|---|---|
| `SearchResultTypeLabels.java` | Raw result type constant (LEAD/CLIENT/...) → লোকালাইজড লেবেল ম্যাপ করে। |
| `SearchResultAdapter.java` | Search result লিস্টের RecyclerView adapter — title, subtitle, type label বাইন্ড করে। |
| `SearchActivity.java` | Cross-entity global search স্ক্রিন — `GET api/search?q=`; pre-query prompt vs post-query no-results — দুই আলাদা empty state; ফলাফলে ক্লিক করলে deep-link নেই (ইচ্ছাকৃত scope cut)। |

---

## ৯.৪ Company Overview (`ui/overview/`)

### `CompanyOverviewActivity.java` — দুই independent API call, একই স্ক্রিনে

ক্লাস-কমেন্ট:

```java
/**
 * COMPANY_OWNER-only quick glance at this month's finance and today's HR figures — plain numbers,
 * no charts, mirroring the web dashboard's stat-card style rather than its trend/budget charts
 * ...
 */
```

`AccountActivity`-তে শুধু `COMPANY_OWNER`-এর জন্যই visible (`EMPLOYEE`-এর জন্যও নয়) — এই একটাই জায়গায় role check `EMPLOYEE`-কে বাদ দেয়, যেখানে `DirectoryActivity`/`LeadListActivity` উভয় staff role-এর জন্যই খোলা। এর কারণ কমেন্টেই আছে: *"Not staff-wide like Leave/Expense approvals — this is company-level financial visibility, so it stays owner-only"* — আর্থিক তথ্য (revenue, expenses) শুধু owner-এর দেখার কথা, সাধারণ employee-র নয়।

**দুটো আলাদা, independent dashboard endpoint:**
- `GET api/finance/dashboard` → `FinanceDashboardResponse` (revenue, expenses, net profit, cash collected, outstanding, overdue)
- `GET api/hr/dashboard/summary` → `HrDashboardResponse` (total employees, present/on-leave/absent today, open positions, pending leave approvals)

দুটোই আলাদা permission-gated (`FINANCIAL_REPORT_VIEW`, `EMPLOYEE_VIEW` — কমেন্টে উল্লেখ) — মানে owner-এর custom role config অনুযায়ী একটা থাকতে পারে, আরেকটা না-ও থাকতে পারে। তাই দুটো section **independently** fail করতে পারা জরুরি:

```java
private boolean financeLoaded;
private boolean hrLoaded;
```

দুটো boolean flag দিয়ে ট্র্যাক করা হয় কোনটা লোড শেষ হলো, আর `updateProgress()` তখনই progress bar লুকায় যখন **উভয়েই** শেষ:

```java
private void updateProgress() {
    if (financeLoaded && hrLoaded) {
        binding.progressBar.setVisibility(View.GONE);
    }
}
```

প্রতিটা `loadXxx()`-এর `onResponse`/`onFailure` — দুটোতেই আগে `xxxLoaded = true; updateProgress();` কল হয়, তারপর success/failure-নির্দিষ্ট কাজ। এই দুই-স্বাধীন-request pattern-টা গুরুত্বপূর্ণ:

```java
// A 403 here (missing FINANCIAL_REPORT_VIEW/EMPLOYEE_VIEW permission) is expected for some
// owner accounts depending on their custom role config — each section fails independently
// rather than blanking the whole screen.
private void showFinanceUnavailable() {
    binding.statRevenue.getRoot().setVisibility(View.GONE);
    ... // প্রতিটা finance stat card GONE
    binding.labelFinance.setText(R.string.error_finance_dashboard_unavailable);
}
```

> **Interview প্রশ্ন: "একটা স্ক্রিনে দুটো independent API call থাকলে, একটা fail করলে পুরো স্ক্রিন খালি দেখানো উচিত কি?"**
> না — এখানে যেটা করা হয়েছে সেটাই সঠিক প্যাটার্ন: প্রতিটা section-এর ব্যর্থতা **শুধু সেই section**-কেই প্রভাবিত করা উচিত। Finance dashboard 403 দিলে (owner-এর permission নেই) HR অংশ এখনো দেখানো উচিত, আর উল্টোটাও। যদি যেকোনো একটা fail করলেই পুরো স্ক্রিন "Something went wrong" দেখাত, সেটা এমন ডেটাও লুকিয়ে ফেলত যেটা আসলে পাওয়া গিয়েছিল। দুটো independent boolean flag (`financeLoaded`, `hrLoaded`) দিয়ে progress bar-এর completion ট্র্যাক করা, আর প্রতিটা response নিজের অংশটুকু নিজে সামলানো — এটা Promise.all-এর "সব সফল না হলে সব fail" আচরণের বিপরীত, বরং প্রতিটা independent request-কে সত্যিকার independent রাখা।

**Stat card বাইন্ডিং প্যাটার্ন — dashboard-এর সাথে সামঞ্জস্যপূর্ণ:**

```java
private void bindStat(ItemOverviewStatBinding stat, int labelRes, int iconRes) {
    stat.statLabel.setText(labelRes);
    stat.statIcon.setImageResource(iconRes);
}
```

`item_overview_stat.xml` একটা reusable `<include>`-করা layout — icon + বড় বোল্ড নম্বর + ছোট মিউটেড লেবেল, ঠিক home dashboard-এর stat card-এর মতোই প্যাটার্ন (icon-value-label)। ১২টা stat card-ই একই layout reuse করে, শুধু `onCreate()`-এ প্রতিটার icon+label আলাদাভাবে সেট করা হয় (`bindStat()` কল ১২ বার), আর value পরে যখন network response আসে তখন সেট হয় (`statValue.setText(String.valueOf(...))`)।

লক্ষণীয়: `HrDashboardResponse.getPendingLeaveApprovals()` একটা nested static class (`LeaveSummary`) থেকে মান বের করে —

```java
public long getPendingLeaveApprovals() {
    return leaveSummary == null ? 0 : leaveSummary.pending;
}

public static class LeaveSummary {
    @SerializedName("pending")
    private long pending;
}
```

এটা নেস্টেড JSON object (`{ "leaveSummary": { "pending": 3 } }`) সরাসরি ম্যাপ করার Gson-friendly উপায় — আর null-check দিয়ে defensive-ভাবে wrap করা, যাতে backend কখনো `leaveSummary` না পাঠালে NPE না হয়ে চুপচাপ `0` ফেরত আসে।

`FinanceDashboardResponse`/`HrDashboardResponse` দুটোতেই সেই একই trimming discipline — comment বলছে ব্যাকএন্ড আসলে trend/budget/pipeline/upcoming-এর মতো chart-এর জন্য অতিরিক্ত list-ও পাঠায় (web dashboard-এর জন্য), কিন্তু এই DTO সেগুলো declare করেনি — Gson সেগুলো silently ignore করে।

### `ui/overview/` ফাইল-তালিকা

| ফাইল | কাজ |
|---|---|
| `CompanyOverviewActivity.java` | COMPANY_OWNER-only finance+HR stat dashboard — দুটো independent API call (`finance/dashboard`, `hr/dashboard/summary`), প্রতিটা section আলাদাভাবে সফল/ব্যর্থ হতে পারে, ১২টা reusable stat card `<include>` করে বাইন্ড হয়। |

---

## ৯.৫ এই অধ্যায়ের ইন্টারভিউ-প্রশ্ন সারাংশ

- **"PATCH কেন, PUT নয় — Lead status আপডেটে?"** → partial update সিমান্টিক্স; বাকি field অক্ষত রাখা।
- **"ক্লায়েন্ট-সাইড DTO থেকে salary/bank-account field বাদ দেওয়া কি real security?"** → না, defense-in-depth-এর একটা দুর্বল কিন্তু দরকারি স্তর; আসল control ব্যাকএন্ডে হতে হবে।
- **"Search-এ pre-query prompt আর post-query no-results — আলাদা কেন রাখলেন?"** → দুটোর কারণ আলাদা (কিছু খোঁজা হয়নি vs খুঁজে পাওয়া যায়নি), তাই guidance-ও আলাদা হওয়া উচিত; মিশিয়ে ফেললে ভুল first-impression তৈরি হয়।
- **"Global search-এ multi-tenant isolation কীভাবে guarantee করবেন?"** → tenant id ক্লায়েন্ট থেকে কখনো না পাঠানো; সার্ভার-সাইড JWT claim + Hibernate filter দিয়ে প্রতিটা query স্বয়ংক্রিয়ভাবে scope করা — "secure by construction"।
- **"একই স্ক্রিনে দুটো independent API call, একটা fail করলে কী করবেন?"** → প্রতিটা section আলাদাভাবে ব্যর্থ হতে দিন (independent boolean flags), পুরো স্ক্রিন ব্ল্যাংক করবেন না — যা আছে তা দেখান, যা নেই শুধু তার জন্যই error দেখান।
