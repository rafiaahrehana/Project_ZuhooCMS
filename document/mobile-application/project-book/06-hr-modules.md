# অধ্যায় ৬ — HR সম্পর্কিত মডিউল: Leave, Expense, Timesheet, Attendance, Payroll

এই অধ্যায়ে আমরা `ui/leave/`, `ui/expense/`, `ui/timesheet/`, `ui/attendance/`, আর `ui/payroll/` — এই ৫টা প্যাকেজ দেখব। এগুলো এমন ফিচার যা প্রায় প্রতিটা company-management বা HR অ্যাপে থাকে (Zoho People, Keka, BambooHR-এর মতো): একজন employee ছুটি চায়, খরচের বিল জমা দেয়, ঘণ্টা লগ করে, অফিসে ঢুকে/বেরিয়ে চেক-ইন করে, আর মাস শেষে salary slip দেখে।

architecture-এর দিক থেকে এই অধ্যায়টা গুরুত্বপূর্ণ একটা কারণে — **এই ৫টা মডিউল একই সমস্যার সমাধানে একরকম consistent না**। কোনোটা (`leave`) পুরোপুরি `CachedListViewModel` ব্যবহার করে (অধ্যায় ৪.৫ দ্রষ্টব্য), কোনোটা (`expense`, `timesheet`, `payroll`) সরাসরি Activity থেকে Repository কল করে, কোনো ViewModel-ই নেই। এই asymmetry দুর্ঘটনাবশত না — প্রতিটা জায়গায় কেন এই সিদ্ধান্ত নেওয়া হয়েছে তার একটা comment আছে কোডে, আর সেগুলো interview-তে "আপনি কখন ViewModel ব্যবহার করবেন, কখন করবেন না" প্রশ্নের ভালো উত্তর।

---

## ৬.১ Leave মডিউল (`ui/leave/`)

**বাস্তব সমস্যা:** একজন employee ছুটি চাইতে চায় (annual, sick, casual...), তার বর্তমান ছুটির ব্যালেন্স দেখতে চায়, আর manager/HR-এর pending ছুটির request approve/reject করতে হয়।

### `LeaveRequestStatusBadge.java` — স্ট্যাটাস থেকে রঙ ও লেবেল

```java
public static int colorFor(Context context, String status) {
    int colorRes;
    if (LeaveRequestStatus.APPROVED.equals(status)) {
        colorRes = R.color.status_success;
    } else if (LeaveRequestStatus.REJECTED.equals(status)
            || LeaveRequestStatus.CANCELLED.equals(status)) {
        colorRes = R.color.status_danger;
    } else {
        colorRes = R.color.status_warning;   // PENDING সহ, যেকোনো অচেনা মানও
    }
    return ContextCompat.getColor(context, colorRes);
}
```

এটা এই প্রজেক্টের একটা repeating pattern — প্রতিটা ডোমেইনের (leave, expense, payroll, service-request) নিজস্ব `XyzStatusBadge` ক্লাস, দুটো static method: `colorFor()` আর `labelFor()`। কোডের কমেন্টেই লেখা আছে কেন একটা shared generic badge ক্লাস বানানো হয়নি:

> "Same two-static-method shape as `ui.servicerequest.StatusBadge` — each domain gets its own sibling rather than a shared generic badge, since the status sets and colors don't line up."

মানে প্রতিটা ডোমেইনের status set আলাদা (leave-এর `PENDING/APPROVED/REJECTED/CANCELLED`, expense-এর সাথে আবার `PAID` যোগ হয়), তাই একটা generic enum→color mapper জোর করে বানালে সেটা আরও জটিল হতো।

> **Interview প্রশ্ন: "৫টা মডিউলেই যদি প্রায় একই রকম StatusBadge ক্লাস থাকে, সেটা duplicate code না?"**
> সামান্য duplicate দেখতে লাগলেও, এটা ইচ্ছাকৃত একটা trade-off। প্রতিটা ডোমেইনের status values ভিন্ন, রঙের mapping ভিন্ন (leave-এ CANCELLED লাল, কিন্তু অন্য কোথাও হয়তো ধূসর হতে পারত) — একটা shared class বানালে `if-else` বা `Map<String, Map<String,Integer>>` দিয়ে সব ডোমেইনের rule এক জায়গায় গুঁজতে হতো, যেটা পড়া কঠিন ও নতুন ডোমেইন যোগ করা risky করে তোলে। ছোট, independent classes এখানে **coupling কমানোর জন্য ইচ্ছাকৃত choice**, অজান্তে হওয়া duplication না। যা shared করা হয়েছে সেটা হলো *rendering* — `StatusBadgeView.bind()` (নিচে দেখো) — যেটা সত্যিই সব ডোমেইনে অভিন্ন।

### `LeaveTypeLabels.java`

`LeaveType` (ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID, COMPENSATORY, EMERGENCY, OTHER — অধ্যায় ৪-এ বর্ণিত "string-constants class, না enum" প্যাটার্নে) থেকে localized label বের করে। এখানে একটা দ্বিতীয় মেথড আছে যেটা অন্য Labels ক্লাসগুলোতেও থাকে:

```java
public static String[] allLabels(Context context) {
    String[] labels = new String[LeaveType.VALUES.length];
    for (int i = 0; i < LeaveType.VALUES.length; i++) {
        labels[i] = labelFor(context, LeaveType.VALUES[i]);
    }
    return labels;
}
```

`allLabels()` `CreateLeaveRequestActivity`-র dropdown populate করতে ব্যবহার হয় — `VALUES[]` আর `labels[]` একই index-এ align থাকে বলেই, dropdown-এ position `i` select হলে `LeaveType.VALUES[i]`-ই আসল wire value হিসেবে পাঠানো যায় (নিচে দেখো)।

### `LeaveBalanceAdapter.java` — ছোট, horizontal RecyclerView

```java
binding.balanceDays.setText(context.getString(R.string.leave_balance_format,
        balance.getRemainingDays(), balance.getEntitledDays()));
```

সাধারণ RecyclerView+Adapter প্যাটার্ন (অধ্যায় ৩.৪), কিন্তু `LeaveRequestListActivity`-তে এটা **horizontal** `LinearLayoutManager` দিয়ে বসানো হয় (নিচে দেখবে) — একটা scrollable card-strip, "Annual: 8/12", "Sick: 3/5" এভাবে।

### `LeaveRequestListViewModel.java` — `CachedListViewModel` extend করার আসল উদাহরণ

```java
public class LeaveRequestListViewModel extends CachedListViewModel<LeaveRequestResponse> {

    private final MutableLiveData<List<LeaveBalanceResponse>> balances = new MutableLiveData<>();

    public LeaveRequestListViewModel(@NonNull Application application) {
        super(application, ListCache.LEAVE_REQUESTS, LeaveRequestResponse.class);
        repository = new LeaveRequestRepository(application);
    }

    @Override protected void fetch(Callback<PageResponse<LeaveRequestResponse>> callback) {
        repository.getMyLeaveRequests(callback);
    }
    @Override protected Long idOf(LeaveRequestResponse item) { return item.getId(); }
    @Override protected int loadErrorRes() { return R.string.error_leave_requests_load_failed; }

    @Override
    public void start() {
        super.start();      // items/loading/error/showingCached — সব CachedListViewModel-এর কাজ
        loadBalances();      // + এই স্ক্রিনের নিজস্ব extra ডেটা
    }
}
```

এখানে `balances` LiveData টা **cache/refresh pattern-এর বাইরে রাখা হয়েছে**, ইচ্ছাকৃতভাবে — কোডের কমেন্ট অনুযায়ী:

> "Balances aren't paged/cached like the request list — a plain live fetch alongside it, same reasoning as `AttendanceLocationSettingsActivity` not using `CachedListViewModel` for a single small settings object."

মানে `CachedListViewModel<T>` শুধু **paginated list**-এর জন্য বানানো (`PageResponse<T>` আশা করে) — balances একটা ছোট `List<LeaveBalanceResponse>`, page না, তাই সেটা আলাদা সরল fetch হিসেবেই থাকে, জোর করে abstraction-এর মধ্যে গোঁজা হয়নি।

`refreshAll()` মেথডটাও লক্ষ করার মতো — cancel-এর পরে দুটো জিনিসই (request list + balance, কারণ ছুটি বাতিল হলে balance-ও ফিরে আসে) refresh করতে হয়:

```java
public void refreshAll() {
    refresh();       // CachedListViewModel-এর মেথড
    loadBalances();
}
```

### `LeaveApprovalListViewModel.java` — একই abstract class, ভিন্ন `fetch()`

```java
@Override
protected void fetch(Callback<PageResponse<LeaveRequestResponse>> callback) {
    repository.getPendingLeaveRequests(callback);
}
```

এটাই `CachedListViewModel`-এর আসল সুবিধা প্রমাণ করে — **My Leave** (নিজের requests) আর **Leave Approvals** (কোম্পানির pending requests) দুটোই একই `LeaveRequestResponse` টাইপ handle করে, শুধু `fetch()` মেথডের ভেতরে ভিন্ন API endpoint কল হয় (`getMyLeaveRequests()` বনাম `getPendingLeaveRequests()`)। Cache collection key-ও আলাদা (`ListCache.LEAVE_REQUESTS` বনাম `ListCache.LEAVE_APPROVALS`) যাতে দুটো screen একে অপরের cache overwrite না করে।

### `LeaveRequestListActivity.java` — "My Leave" স্ক্রিন

```java
@Override
protected void onResume() {
    super.onResume();
    // Re-entering after submitting or cancelling a request should show the new state.
    viewModel.start();
}
```

`onResume()`-এ `start()` (`onCreate()`-এ না) — অধ্যায় ৩.১-এ ব্যাখ্যা করা pattern-ই এখানে বাস্তবে প্রয়োগ হয়েছে। `start()` নিজেই idempotent-এর মতো আচরণ করে (`loadedOnce` flag দিয়ে cache শুধু একবারই পড়ে), কিন্তু `refresh()` প্রতিবারই চলে — তাই একটা নতুন request submit করে ফিরে এলে তালিকা তাজা হয়।

**StateView আর CacheStamp একসাথে কীভাবে কাজ করে** — এটা এই session-এর নতুন UI pattern, ভালো করে বোঝা দরকার:

```java
viewModel.items().observe(this, requests -> {
    adapter.submitList(requests);
    if (requests.isEmpty()) {
        binding.stateView.showEmpty(R.drawable.ic_calendar,
                R.string.empty_leave_requests, R.string.empty_leave_requests_subtitle);
    } else {
        binding.stateView.showContent();
    }
});

viewModel.loading().observe(this, loading -> {
    if (loading && adapter.getItemCount() == 0) {
        binding.stateView.showLoading();
    }
});

viewModel.showingCached().observe(this, cached ->
        CacheStamp.bind(binding.cacheStamp, cached, viewModel.lastUpdated().getValue()));

viewModel.error().observe(this, event -> {
    Integer messageRes = event.consume();
    if (messageRes != null) {
        binding.stateView.showError(messageRes, v -> viewModel.refresh());
    }
});
```

`StateView` (`ui/common/StateView.java`) একটা custom `FrameLayout` যেটা list screen-এর RecyclerView-এর **sibling** হিসেবে বসে (একই parent FrameLayout-এ), আর ৪টা mode-এ থাকতে পারে: `showLoading()`, `showEmpty(icon, title, subtitle)`, `showError(subtitleRes, onRetry)`, বা `showContent()` (নিজেকে `GONE` করে, real content-কে visible থাকতে দেয়)। এর আগে প্রতিটা screen নিজে নিজে bare `emptyStateTextView` + `ProgressBar` manage করত — এখন এই একটা component পুরো state machine handle করে।

`loading && adapter.getItemCount() == 0` — এই condition-টা গুরুত্বপূর্ণ। যদি ইতিমধ্যে rows দেখানো থাকে (cache থেকে), আর background-এ `refresh()` চলছে, তাহলে **পুরো spinner দেখিয়ে content ঢেকে দেওয়া হয় না** — এটা অধ্যায় ৪.৫-এর "cache থাকলে UI ভাঙা যাবে না" নীতিরই আরেকটা প্রয়োগ।

`CacheStamp.bind()` (`ui/common/CacheStamp.java`) `showingCached=true` হলে "শেষ আপডেট হয়েছিল ৫ মিনিট আগে" ধরনের একটা relative-time লাইন দেখায় (`DateUtils.getRelativeTimeSpanString`), আর একদম নিচে চলে যায় (`GONE`) যদি live ডেটা দেখানো হচ্ছে। এটা `CachedListViewModel`-এর `showingCached` LiveData-র সরাসরি UI representation।

**Cancel-এর জন্য ViewModel এড়িয়ে সরাসরি Repository:**

```java
private void cancel(LeaveRequestResponse request) {
    repository.cancelLeaveRequest(request.getId(), new Callback<ResponseBody>() {
        @Override public void onResponse(...) {
            ...
            viewModel.refreshAll();
        }
        ...
    });
}
```

`CachedListViewModel`-এর ক্লাস-লেভেল কমেন্টেই লেখা: **"Read-only by design — nothing is ever queued for later upload. Writes go straight to the API."** অর্থাৎ এই ViewModel শুধু **পড়া**র (list দেখানো) দায়িত্ব নেয়, **লেখা**র (cancel, approve, reject) দায়িত্ব না — সেটা Activity সরাসরি Repository দিয়ে করে, আর সফল হলে `viewModel.refreshAll()` কল করে তালিকা নতুন করে টেনে আনে।

> **Interview প্রশ্ন: "Write অপারেশনও কি ViewModel-এর ভেতর দিয়ে যাওয়া উচিত না, MVVM-এর নিয়ম অনুযায়ী?"**
> কড়া MVVM মানলে হ্যাঁ। কিন্তু এখানে ইচ্ছাকৃত সরলীকরণ — `CachedListViewModel`-এর দায়িত্ব সংকীর্ণভাবে "cache + refresh" এ সীমাবদ্ধ রাখা হয়েছে, যাতে সব subclass-এর জন্য এই abstract class-টা predictable থাকে। Write action (cancel/approve/reject) একবারই ঘটে, retry বা offline-queue দরকার নেই, তাই Activity সরাসরি Repository কল করাটা অপ্রয়োজনীয় জটিলতা এড়ায়। এটা একটা conscious architectural boundary, না যে ভুলে করা হয়েছে।

### `LeaveRequestAdapter.java` — দুই স্ক্রিনে একটাই Adapter

```java
public LeaveRequestAdapter(List<LeaveRequestResponse> requests, boolean showEmployeeName,
                           OnItemClickListener clickListener, OnCancelClickListener cancelListener) {
```

`showEmployeeName` flag + nullable `cancelListener` দিয়ে একই adapter দুটো ভিন্ন context-এ কাজ করে:
- **My Leave**: `showEmployeeName=false`, cancel button PENDING request-এ visible, ট্যাপ করলে detail dialog।
- **Leave Approvals**: `showEmployeeName=true`, `cancelListener=null` (তাই cancel button কখনো দেখায় না), ট্যাপ করলে approve/reject dialog।

```java
boolean canCancel = cancelListener != null && LeaveRequestStatus.PENDING.equals(request.getStatus());
binding.itemBtnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
```

Double condition — cancel শুধু তখনই সম্ভব যখন (ক) এই স্ক্রিনে cancel action আছে, আর (খ) request-টা এখনো PENDING (APPROVED/REJECTED হয়ে যাওয়া request cancel করা যায় না)।

> **Interview প্রশ্ন: "একটা Adapter দুই জায়গায় reuse করার সুবিধা-অসুবিধা কী?"**
> সুবিধা: `bind()` লজিক (status badge, date format) একবারই লেখা লাগে, দুই স্ক্রিন sync-এ থাকে (একটাতে বদল আনলে অন্যটাও পায়)। অসুবিধা: constructor-এ boolean flag আর nullable listener চলে আসা মানে adapter-টা কম self-explanatory — নতুন কেউ কোড পড়লে বুঝতে সময় লাগে কোন mode-এ কী দেখাবে। একটা rule of thumb: দুই ব্যবহারের পার্থক্য যদি ২-৩টা ছোট flag-এই ধরা যায় (যেমন এখানে), reuse ঠিক আছে; পার্থক্য বেশি হলে আলাদা adapter-ই পরিষ্কার।

### `LeaveApprovalListActivity.java` — Approve/Reject Dialog

```java
new MaterialAlertDialogBuilder(this)
        .setTitle(LeaveTypeLabels.labelFor(this, request.getLeaveType()))
        .setMessage(message)
        .setPositiveButton(R.string.action_approve, (dialog, which) -> approve(request))
        .setNegativeButton(R.string.action_reject, (dialog, which) -> promptRejectReason(request))
        .setNeutralButton(android.R.string.cancel, null)
        .show();
```

তিনটা বাটনের dialog (`MaterialAlertDialogBuilder`-এ positive/negative/neutral) — approve সরাসরি হয়, কিন্তু reject করলে একটা দ্বিতীয় dialog খোলে যেখানে reason চাওয়া হয় (`EditText` দিয়ে dynamic-ভাবে বানানো, XML layout ছাড়াই):

```java
EditText reasonInput = new EditText(this);
reasonInput.setHint(getString(R.string.hint_rejection_reason));
new MaterialAlertDialogBuilder(this)
        .setView(reasonInput)
        .setPositiveButton(R.string.action_reject, (dialog, which) -> {
            String reason = ...;
            if (TextUtils.isEmpty(reason)) {
                Toast.makeText(this, R.string.error_rejection_reason_required, Toast.LENGTH_LONG).show();
                return;
            }
            reject(request, reason);
        })
```

Reason ফাঁকা থাকলে reject block হয়ে যায় — server-এর কাছে কারণ ছাড়া reject পাঠানো অর্থহীন হবে (employee জানতেই পারবে না কেন বাতিল হলো)।

### `CreateLeaveRequestActivity.java` — নতুন Form Pattern

**Spinner-এর বদলে exposed dropdown:**
```java
binding.leaveTypeDropdown.setAdapter(new ArrayAdapter<>(this,
        android.R.layout.simple_list_item_1, leaveTypeLabels));
binding.leaveTypeDropdown.setText(leaveTypeLabels[0], false);
binding.leaveTypeDropdown.setOnItemClickListener((parent, view, position, id) -> leaveTypeIndex = position);
```

XML-এ এই field একটা `TextInputLayout` (`style="@style/Widget.Zuhoo.TextInputLayout.Dropdown"`, যেটার parent হলো Material-এর `...OutlinedBox.ExposedDropdownMenu`) এর ভেতরে বসানো `AutoCompleteTextView` (`android:inputType="none"` — টাইপ করা যায় না, শুধু list থেকে বাছা যায়)। পুরনো `Spinner` widget-এর জায়গায় এটাই এখন app-wide standard — কারণ `Spinner` Material Design-এর floating-label look দেয় না, `TextInputLayout`-এর ভেতরে বসে না।

`setText(labels[0], false)` — দ্বিতীয় প্যারামিটার `false` মানে এটা `TextWatcher`/filter-কে trigger করবে না, শুধু default দেখানোর জন্য টেক্সট বসিয়ে দেওয়া। State track করা হয় field-এর টেক্সট থেকে না, বরং আলাদা `leaveTypeIndex` int variable দিয়ে — `attemptSubmit()`-এ `LeaveType.VALUES[leaveTypeIndex]` পড়া হয়, field-এর `.getText()` কখনো না, কারণ label localized (`"বার্ষিক ছুটি"`) কিন্তু API-তে raw constant (`"ANNUAL"`) পাঠাতে হয়।

**বেয়ার TextView-এর বদলে TextInputLayout date field:**
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.Zuhoo.TextInputLayout.Date" ...>
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/startDateText"
        android:clickable="true"
        android:cursorVisible="false"
        android:focusable="false"
        android:inputType="none" />
</com.google.android.material.textfield.TextInputLayout>
```

`focusable="false"` + `cursorVisible="false"` + `inputType="none"` — এই তিনটার মিলিত effect হলো: field-টা দেখতে normal Material text field-এর মতো (floating hint, outline, ক্যালেন্ডার আইকন — `Widget.Zuhoo.TextInputLayout.Date` স্টাইল থেকে `endIconDrawable="@drawable/ic_calendar"`), কিন্তু keyboard খোলে না, কারসর দেখা যায় না — শুধু ট্যাপ করলে `DatePickerDialog` খোলে:

```java
private void pickDate(TextView target, OnDatePicked onPicked) {
    Calendar now = Calendar.getInstance();
    new DatePickerDialog(this, (picker, year, month, day) -> {
        String iso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
        target.setText(iso);
        onPicked.onPicked(iso);
    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
}
```

আগে এই field একটা raw `TextView` ছিল যেটাতে click listener লাগানো — এখন একই ফাংশনালিটি একটা `TextInputEditText` দিয়ে হচ্ছে যাতে বাকি form-এর সাথে visually consistent (floating label, outline box) দেখায়। কোডের comment বলছে এই date-picker approach `DynamicFormRenderer.datePicker()`-এর সাথে identical — app জুড়ে একটাই date-picker pattern আছে, দুইবার আবিষ্কার করা হয়নি।

`OnDatePicked` — একটা ছোট local functional interface, lambda দিয়ে caller-নির্দিষ্ট callback (`picked -> startDate = picked` বনাম `picked -> endDate = picked`) পাস করার জন্য, যাতে `pickDate()` মেথড দুইবার আলাদা করে লিখতে না হয়।

**Validation:**
```java
if (endDate.compareTo(startDate) < 0) {
    Toast.makeText(this, R.string.error_end_date_before_start, Toast.LENGTH_LONG).show();
    return;
}
```

তারিখগুলো ISO-8601 ফরম্যাটে (`yyyy-MM-dd`) স্ট্রিং হিসেবে রাখা হয় বলে, `String.compareTo()` দিয়েই lexicographic ordering সরাসরি chronological ordering-এর সমান হয়ে যায় — আলাদা করে `Date`/`LocalDate` parse করার দরকার পড়ে না।

> **Interview প্রশ্ন: "ISO-8601 date স্ট্রিং কেন `compareTo()` দিয়ে সরাসরি তুলনা করা যায়?"**
> কারণ `yyyy-MM-dd` ফরম্যাটে বড় unit (বছর) সবার আগে, তারপর মাস, তারপর দিন — fixed-width সংখ্যায় (`04`, `12` — leading zero সহ)। এই property-টাকে বলে **lexicographically sortable** — string হিসেবে sort করলেই chronological order মেলে। এটা ISO-8601 ডিজাইন করারই একটা উদ্দেশ্য ছিল।

### Leave মডিউল — সম্পূর্ণ ফাইল তালিকা

| ফাইল | কাজ |
|---|---|
| `LeaveRequestStatusBadge.java` | Status → রঙ (`colorFor`) ও localized লেবেল (`labelFor`) |
| `LeaveTypeLabels.java` | Leave type constant → localized লেবেল, আর dropdown-এর জন্য `allLabels()` |
| `LeaveBalanceAdapter.java` | Horizontal RecyclerView adapter — leave balance card strip |
| `LeaveRequestListViewModel.java` | `CachedListViewModel` subclass — নিজের requests + balances |
| `LeaveApprovalListViewModel.java` | `CachedListViewModel` subclass — pending approvals (company-wide) |
| `LeaveRequestListActivity.java` | "My Leave" স্ক্রিন — list + balance strip + cancel |
| `LeaveRequestAdapter.java` | দুই স্ক্রিনে shared adapter (mode flag দিয়ে) |
| `LeaveApprovalListActivity.java` | "Leave Approvals" স্ক্রিন — approve/reject dialog |
| `CreateLeaveRequestActivity.java` | নতুন ছুটির আবেদন ফর্ম — dropdown + দুইটা date picker |

---

## ৬.২ Expense মডিউল (`ui/expense/`)

**বাস্তব সমস্যা:** Employee অফিসের কাজে খরচ করলে (travel, meals, office supplies...) রিসিট আপলোড করে claim জমা দেয়, manager সেটা approve/reject করে।

Leave মডিউলের সাথে expense-এর গঠন প্রায় অভিন্ন (status badge, category labels, shared adapter, approve/reject dialog) — কিন্তু একটা বড় স্থাপত্যগত পার্থক্য আছে: **এখানে কোনো ViewModel নেই**।

### `ExpenseStatusBadge.java` ও `ExpenseCategoryLabels.java`

Leave-এর সাথে একই প্যাটার্ন, পার্থক্য শুধু status set-এ — expense-এ `APPROVED`-এর পাশাপাশি `PAID`-ও সবুজ:
```java
if (ExpenseStatus.APPROVED.equals(status) || ExpenseStatus.PAID.equals(status)) {
    colorRes = R.color.status_success;
}
```
`PAID` মানে টাকা ইতিমধ্যে দেওয়া হয়ে গেছে (approve-এর পরের ধাপ), তাই এটাও "ভালো" স্ট্যাটাস — সবুজ রঙেই দেখানো হয়, কিন্তু আলাদা লেবেল টেক্সট (`expense_status_paid`)।

### `ExpenseAdapter.java`

`LeaveRequestAdapter`-এর মতোই `showSubmittedBy` boolean flag দিয়ে "My Expenses" আর "Expense Approvals" — দুই স্ক্রিনে reuse হয়।

### `ExpenseListActivity.java` — ViewModel ছাড়া list screen

```java
@Override
protected void onResume() {
    super.onResume();
    load();
}

private void load() {
    binding.stateView.showLoading();
    repository.getMyExpenses(new Callback<PageResponse<ExpenseResponse>>() {
        @Override
        public void onResponse(...) {
            if (!response.isSuccessful() || response.body() == null) {
                // A refresh here may be replacing already-loaded content, so this can't
                // safely take over the whole screen the way an empty-state error can.
                binding.stateView.showContent();
                Toast.makeText(...).show();
                return;
            }
            ...
        }
    });
}
```

এখানে সরাসরি `ExpenseRepository` কল হচ্ছে Activity থেকে, `CachedListViewModel` নেই — মানে **কোনো offline cache নেই, screen rotate করলে ডেটা আবার fetch হয়**। ভুলবশত না — কমেন্টটা লক্ষ করো: `onResume()`-এ প্রতিবার load() চলে বলে, error হলে সেটা "প্রথমবার লোড ব্যর্থ" নাকি "already-shown ডেটার উপর একটা refresh ব্যর্থ" তা নিশ্চিতভাবে বলা যায় না — তাই safe পছন্দ হলো error-কে Toast হিসেবে দেখানো, `StateView`-কে পুরো screen দখল করতে না দেওয়া (`showContent()` কল করে রাখা হয়, আগের rows যা ছিল তাই থাকে)।

> **Interview প্রশ্ন: "Leave module CachedListViewModel ব্যবহার করে, Expense কেন করে না? এটা কি inconsistency, নাকি ইচ্ছাকৃত?"**
> সততার সাথে বলা ভালো — কোডে এর কোনো ব্যাখ্যা কমেন্ট নেই, এটা একটা বাস্তব inconsistency এই কোডবেসে। প্রযুক্তিগতভাবে `ExpenseListActivity`-কেও অনায়াসে `CachedListViewModel<ExpenseResponse>` দিয়ে rewrite করা যেত — এটা একই ধরনের paginated list। সম্ভবত সময়ের সাথে ফিচারগুলো আলাদা আলাদা সময়ে লেখা হয়েছে, আর CachedListViewModel প্যাটার্নটা Leave-এর পরে এসেছে কিন্তু Expense-এ retrofit করা হয়নি। Interview-তে এই প্রশ্ন এলে সেরা উত্তর: "আমি প্যাটার্নটা চিনি এবং জানি কীভাবে apply করতে হয় — এই নির্দিষ্ট জায়গায় সেটা এখনো apply করা হয়নি, এটা একটা known inconsistency" — এটা দেখায় তুমি কোড copy করোনি, বুঝে পড়েছ।

### `ExpenseApprovalListActivity.java` — একটা সূক্ষ্ম Retrofit gotcha

```java
// The backend's notes @RequestParam is required with no default — Retrofit drops a null
// @Query value from the URL entirely, which the server then sees as a missing parameter.
repository.approveExpense(expense.getId(), "", new Callback<ResponseBody>() {
```

Reject করার সময় user reason টাইপ করে, কিন্তু approve করার সময় কোনো notes লাগে না — তাহলে `null` না পাঠিয়ে খালি স্ট্রিং (`""`) পাঠানো হচ্ছে কেন? কমেন্টেই কারণ লেখা আছে: Retrofit-এ `@Query` প্যারামিটার `null` হলে সেটা URL থেকে **সম্পূর্ণ বাদ** পড়ে যায় (query string-এই আসে না), backend-এর `@RequestParam` (default value ছাড়া, required) তখন "প্যারামিটার missing" ধরে নিয়ে ৪০০ error দেয়। খালি স্ট্রিং পাঠালে প্যারামিটারটা URL-এ থাকে (`?notes=`), backend খুশি।

> **Interview প্রশ্ন: "Retrofit-এ null query parameter পাঠালে কী হয়?"**
> `@Query("key") String value` যদি `null` হয়, Retrofit সেই key-value pair-টাই request URL থেকে বাদ দিয়ে দেয় — এটা `@Query` প্যারামিটারের ডিফল্ট আচরণ (optional query param হিসেবে ট্রিট করে)। backend যদি ওই প্যারামিটারকে required ধরে (কোনো default value declare না করে), তাহলে null পাঠানো crash-এর মতো ৪০০ Bad Request-এ শেষ হয়। সমাধান: হয় backend-এ default value declare করা (`@RequestParam(defaultValue = "")`), অথবা client-এ কখনো null না পাঠিয়ে খালি string পাঠানো — এই প্রজেক্ট দ্বিতীয়টা বেছেছে।

### `CreateExpenseActivity.java` — Validation-এর ভিন্ন স্টাইল

`CreateLeaveRequestActivity` Toast দিয়ে validation error দেখায়, কিন্তু এখানে field-level error ব্যবহার হয়েছে:

```java
binding.descriptionInputLayout.setError(null);
binding.amountInputLayout.setError(null);

if (TextUtils.isEmpty(description)) {
    binding.descriptionInputLayout.setError(getString(R.string.error_description_required));
    return;
}

try {
    amount = new BigDecimal(amountText);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        binding.amountInputLayout.setError(getString(R.string.error_price_invalid));
        return;
    }
} catch (NumberFormatException e) {
    binding.amountInputLayout.setError(getString(R.string.error_price_invalid));
    return;
}
```

`TextInputLayout.setError()` — Material Design-এর built-in error UI (field-এর নিচে লাল টেক্সট, outline লাল হয়ে যায়)। প্রতিবার submit করার আগে `setError(null)` দিয়ে আগের error মুছে ফেলা হয়, যাতে পুরনো error নতুন attempt-এও লেগে না থাকে।

টাকার amount `BigDecimal` দিয়ে parse করা হয় (`double`/`float` না) — মানি ভ্যালুর জন্য এটা standard practice, কারণ `double` binary floating-point-এ ছোট রাউন্ডিং error আনতে পারে (যেমন `0.1 + 0.2 != 0.3`), যেটা টাকার হিসেবে গ্রহণযোগ্য না।

> **Interview প্রশ্ন: "টাকার amount-এর জন্য double/float-এর বদলে BigDecimal কেন?"**
> `double`/`float` IEEE 754 binary floating-point স্ট্যান্ডার্ড মেনে চলে, যেখানে `0.1` বা `0.2`-এর মতো decimal fraction **সঠিকভাবে represent করা যায় না** (বাইনারিতে এগুলো repeating fraction)। ফলে ছোট ছোট রাউন্ডিং error জমা হতে থাকে — টাকার হিসেবে এমনকি এক পয়সার পার্থক্যও গ্রহণযোগ্য না। `BigDecimal` arbitrary-precision decimal arithmetic করে, প্রতিটা decimal digit নির্ভুলভাবে রাখে — তাই এটাই standard choice যেকোনো financial calculation-এ (Java-তে, বা যেকোনো ভাষায়)।

আর একটা লক্ষণীয় জিনিস: `AttachmentPicker` (নিচে দেখো) দিয়ে রিসিট ছবি সংযুক্ত করা যায় — এটা `SelfieCapture`-এর মতোই দেখতে (upload flow), কিন্তু camera-only না, **gallery/file picker** (`OpenDocument` contract) — কারণ পুরনো রিসিট গ্যালারি থেকে বাছাই করাটা এখানে বৈধ (attendance selfie-র উল্টো, যেখানে জোর করে live camera লাগে)।

### Expense মডিউল — সম্পূর্ণ ফাইল তালিকা

| ফাইল | কাজ |
|---|---|
| `ExpenseStatusBadge.java` | Status → রঙ ও লেবেল (PAID-ও সবুজ) |
| `ExpenseCategoryLabels.java` | Category constant → localized লেবেল, dropdown-এর জন্য `allLabels()` |
| `ExpenseAdapter.java` | দুই স্ক্রিনে shared adapter (`showSubmittedBy` flag) |
| `ExpenseListActivity.java` | "My Expenses" — ViewModel ছাড়া, সরাসরি Repository কল |
| `ExpenseApprovalListActivity.java` | "Expense Approvals" — approve/reject, null-query gotcha handle |
| `CreateExpenseActivity.java` | নতুন claim ফর্ম — BigDecimal amount, receipt attachment, field-level error |

---

## ৬.৩ Timesheet মডিউল (`ui/timesheet/`)

**বাস্তব সমস্যা:** Employee কোন দিনে কোন প্রজেক্টে কত ঘণ্টা কাজ করেছে তার লগ রাখা — payroll ও billing-এর ইনপুট হিসেবে ব্যবহার হয়।

এই মডিউলটা সবচেয়ে ছোট (মাত্র ২টা ফাইল) আর সবচেয়ে কম "পালিশড" — এটা লক্ষ করার মতো, কারণ এটা দেখায় UI redesign সব জায়গায় সমানভাবে পৌঁছায়নি।

### `TimesheetAdapter.java` — Status raw টেক্সট, badge না

```java
void bind(TimesheetResponse entry) {
    binding.itemWorkDate.setText(entry.getWorkDate());
    binding.itemHours.setText(String.format(Locale.US, "%.1fh", entry.getHoursWorked()));
    binding.itemProjectName.setText(entry.getProjectName());
    binding.itemStatus.setText(entry.getStatus());   // ← raw string, StatusBadgeView নেই
}
```

Leave, Expense, Payroll — সবগুলোতে `StatusBadgeView.bind()` দিয়ে রঙিন pill badge দেখানো হয়, কিন্তু Timesheet-এ status সরাসরি `TextView.setText()` দিয়ে raw স্ট্রিং হিসেবে বসানো — না রঙ, না localization, না icon। এটা এই session-এর UI redesign যে জায়গাগুলোতে এখনো পৌঁছায়নি তার একটা উদাহরণ।

> **Interview প্রশ্ন: "যদি তোমাকে এই মডিউল consistent করতে বলা হয়, কী করবে?"**
> `TimesheetStatusBadge` নামে (বা যদি timesheet status set ছোট হয়, existing কোনো shared badge reuse করে) একটা নতুন ক্লাস বানিয়ে `colorFor()`/`labelFor()` যোগ করা, তারপর `StatusBadgeView.bind(binding.itemStatus, ...)` কল করা — ঠিক অন্য চারটা মডিউলে যেভাবে হয়েছে। এটাই দেখায় প্যাটার্ন pattern-matching করে বাকি কোডে সামঞ্জস্য আনার ক্ষমতা।

### `TimesheetListActivity.java` — Create ফর্ম Activity না, dynamic dialog

Leave আর Expense-এর নিজস্ব `Create...Activity` আছে (XML layout সহ, TextInputLayout দিয়ে সাজানো), কিন্তু Timesheet-এ "নতুন এন্ট্রি" পুরোপুরি একটা **programmatically-built dialog**:

```java
private void promptLogTime() {

    TextView dateText = new TextView(this);
    dateText.setText(R.string.form_field_pick_date);
    dateText.setOnClickListener(v -> { /* DatePickerDialog খোলে */ });

    EditText hoursInput = new EditText(this);
    hoursInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

    EditText projectInput = new EditText(this);
    EditText descriptionInput = new EditText(this);

    LinearLayout container = verticalDialogContainer();
    container.addView(dateText);
    container.addView(hoursInput);
    container.addView(projectInput);
    container.addView(descriptionInput);

    new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_log_time)
            .setView(container)
            .setPositiveButton(R.string.action_save, (dialog, which) -> { ... })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
}
```

কোনো XML layout ফাইল নেই এই dialog-এর জন্য — `new TextView(this)`, `new EditText(this)`, `new LinearLayout(this)` দিয়ে পুরো ফর্ম Java কোডেই বানানো হচ্ছে, তারপর `MaterialAlertDialogBuilder.setView()`-এ বসানো। `dp()` helper মেথডটা dp→px রূপান্তর করে (`density` multiply করে), কারণ programmatic `setPadding()` pixel নেয়, dp না — XML-এ `android:padding="16dp"` লিখলে Android automatically এই conversion করে দেয়, কিন্তু Java কোডে manually করতে হয়।

> **Interview প্রশ্ন: "Programmatic View তৈরি করা বনাম XML layout inflate করা — কখন কোনটা?"**
> XML layout-এর সুবিধা: preview পাওয়া যায় (Android Studio Layout Editor), ViewBinding দিয়ে type-safe reference, ডিজাইনার/developer আলাদাভাবে layout নিয়ে কাজ করতে পারে, জটিল layout পড়া সহজ। Programmatic View-এর সুবিধা: খুব ছোট, one-off UI (এই ৪-field dialog-এর মতো) হলে আলাদা XML ফাইল বানানোর overhead এড়ানো যায়, dynamic সংখ্যক View লাগলে (যেমন runtime-এ ঠিক হওয়া field count) loop দিয়ে সহজে বানানো যায়। এই কোডবেসের বাকি সব form (Leave, Expense) XML+ViewBinding ব্যবহার করে — Timesheet ব্যতিক্রম, সম্ভবত এটা ছোট ও কম গুরুত্বপূর্ণ ফিচার বলে দ্রুত লেখা হয়েছে।

Load logic Expense-এর মতোই — ViewModel ছাড়া, `onResume()`-এ সরাসরি Repository কল, `StateView` দিয়ে loading/empty/content।

### Timesheet মডিউল — সম্পূর্ণ ফাইল তালিকা

| ফাইল | কাজ |
|---|---|
| `TimesheetAdapter.java` | List item bind — status raw টেক্সট হিসেবে (badge নেই) |
| `TimesheetListActivity.java` | List + "Log Time" — dynamically-built dialog দিয়ে নতুন এন্ট্রি |

---

## ৬.৪ Attendance মডিউল (`ui/attendance/`) — Selfie + GPS Check-in

**বাস্তব সমস্যা:** Employee অফিসে এসে "চেক-ইন" করবে, বেরোনোর সময় "চেক-আউট" — কিন্তু কীভাবে নিশ্চিত করা যায় সে সত্যিই অফিসে আছে, বাসায় বসে বন্ধুকে দিয়ে চেক-ইন করাচ্ছে না? এই মডিউলের সমাধান: **প্রতিটা check-in/out-এর সাথে একটা live selfie + GPS coordinate লাগবে**।

এটা এই প্রজেক্টের একটা distinctive ফিচার — শুধু ফর্ম সাবমিট না, বরং **প্রমাণ সংগ্রহ** (evidence collection)।

### `LocationHelper.java` — Play Services ছাড়া GPS Fix

```java
/**
 * Runtime location permission + a single GPS fix, built on plain LocationManager rather than
 * play-services-location — this app has no Play Services dependency anywhere else, and one
 * screen isn't reason enough to add one.
 */
public class LocationHelper {
```

সাধারণত Android অ্যাপে location পেতে Google-এর `FusedLocationProviderClient` (play-services-location library) ব্যবহার হয় — বেশি accurate, ব্যাটারি-efficient। কিন্তু এই অ্যাপের আর কোথাও Play Services dependency নেই, তাই শুধুমাত্র এই একটা স্ক্রিনের জন্য পুরো library যোগ করা (APK size বাড়ায়, নতুন dependency risk আনে) worth মনে হয়নি — সাদামাটা platform `LocationManager` দিয়েই কাজ চলছে।

**Best provider বাছাই:**
```java
private String bestProvider() {
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        return LocationManager.GPS_PROVIDER;
    }
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        return LocationManager.NETWORK_PROVIDER;
    }
    return null;
}
```
GPS বেশি accurate কিন্তু বন্ধ ঘরে/underground দুর্বল; NETWORK_PROVIDER (WiFi/cell tower triangulation) কম accurate কিন্তু দ্রুত ও indoor-এও কাজ করে। GPS available থাকলে সেটাই আগে try হয়, না থাকলে network fallback।

**Stale last-known location এড়ানো:**
```java
Location last = locationManager.getLastKnownLocation(provider);
if (last != null && System.currentTimeMillis() - last.getTime() < 60_000) {
    finish(last);
    return;
}
```
`getLastKnownLocation()` তাৎক্ষণিক রিটার্ন করে (device-এ আগে থেকেই cache করা থাকে), কিন্তু সেটা পুরনো হতে পারে (কেউ ঘণ্টাখানেক আগে অন্য অ্যাপে GPS ব্যবহার করেছিল বলে)। ৬০ সেকেন্ডের বেশি পুরনো হলে সেটা বাতিল করে একটা **fresh single update** চাওয়া হয়:

```java
locationManager.requestSingleUpdate(provider, activeLocationListener, Looper.getMainLooper());
timeoutHandler.postDelayed(() -> finish(null), FIX_TIMEOUT_MS);   // 15 সেকেন্ড
```

**Race-safe একবার-মাত্র delivery:**
```java
private void finish(Location location) {
    if (pendingListener == null) {
        return;   // ইতিমধ্যে একবার finish() হয়ে গেছে
    }
    timeoutHandler.removeCallbacksAndMessages(null);
    if (activeLocationListener != null) {
        locationManager.removeUpdates(activeLocationListener);
        activeLocationListener = null;
    }
    Listener listener = pendingListener;
    pendingListener = null;
    ...
}
```
`requestSingleUpdate()` আর ১৫-সেকেন্ড timeout — দুটোই `finish()` কল করতে পারে, race condition। `pendingListener`-কে null করে দেওয়া guard হিসেবে কাজ করে — যেটাই আগে আসুক (আসল fix বা timeout), সেটাই জেতে, দ্বিতীয়টা কিছু করে না (early return)। এটা অনেকটা অধ্যায় ৪.৩-এ দেখা `TokenAuthenticator`-এর race-condition handling-এর ছোট সংস্করণ — সমস্যার প্যাটার্নটা একই: "দুটো asynchronous event একই callback-কে দুইবার trigger করতে চাইছে, একবারই হওয়া উচিত"।

> **Interview প্রশ্ন: "GPS fix না পেলে (indoor, permission denied, timeout) UI কী করে?"**
> `Listener.onUnavailable()` কল হয় — `CheckInActivity` সেটা ধরে location status টেক্সট আপডেট করে (`status_location_unavailable`) আর check-in/out বাটন disabled-ই থেকে যায় (নিচে `updateActionButtonState()` দেখো)। App crash করে না, silently ব্যর্থও হয় না — ব্যবহারকারীকে স্পষ্ট জানানো হয় কেন বাটন কাজ করছে না।

### `SelfieCapture.java` — Camera-only Upload Helper

```java
/**
 * Deliberately camera-only (ActivityResultContracts.TakePicture), unlike AttachmentPicker's
 * document picker — a gallery pick would let someone attach an old photo instead of proving
 * they're here now.
 */
```

`AttachmentPicker` (expense receipt-এর জন্য) `OpenDocument` contract ব্যবহার করে — ফাইল সিস্টেম/গ্যালারি থেকে যেকোনো ফাইল বাছা যায়। `SelfieCapture` ইচ্ছাকৃতভাবে `ActivityResultContracts.TakePicture()` ব্যবহার করে — এটা device-এর camera app খুলে, শুধু **এখনই তোলা** ছবিই গ্রহণ করে। এই পার্থক্যটাই পুরো check-in ফিচারের নিরাপত্তার ভিত্তি — যদি গ্যালারি থেকে বাছার সুযোগ থাকত, কেউ বছর আগের একটা selfie বারবার আপলোড করে "আজ অফিসে আছি" দাবি করতে পারত।

**দুটো ভিন্ন permission launcher:**
```java
this.launcher = activity.registerForActivityResult(
        new ActivityResultContracts.TakePicture(), this::onCaptured);

// The system camera app enforces the caller's own CAMERA permission, not just the
// FileProvider grant — launching TakePicture without it throws a SecurityException
// rather than prompting, so this has to be requested first
this.cameraPermissionLauncher = activity.registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) launchCamera(); else Toast.makeText(...).show();
        });
```
`TakePicture` contract নিজে থেকে CAMERA runtime permission চায় না — এটা শুধু camera intent launch করে, permission না থাকলে সরাসরি crash (`SecurityException`) করে। তাই `capture()` মেথড আগে manually check করে:
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

**FileProvider দিয়ে ফাইল destination তৈরি:**
```java
File dir = new File(activity.getCacheDir(), "selfies");
File file = new File(dir, "checkin_" + System.currentTimeMillis() + ".jpg");
pendingCaptureUri = FileProvider.getUriForFile(
        activity, activity.getPackageName() + ".fileprovider", file);
launcher.launch(pendingCaptureUri);
```
`TakePicture` contract-কে একটা ইতিমধ্যে-তৈরি `Uri` দিতে হয় যেখানে camera app ছবি সেভ করবে (নিজে থেকে uri রিটার্ন করে না, `TakePicture` এর মতো `TakePicturePreview`-এর মতো bitmap রিটার্ন করে না)। Android 7.0+ থেকে raw `file://` Uri অন্য অ্যাপে (camera app-এ) শেয়ার করা নিষিদ্ধ (`FileUriExposedException`) — `FileProvider` একটা secure `content://` Uri দেয় যেটা সাময়িক read/write permission grant করে শুধু সেই নির্দিষ্ট camera app-কে।

Capture সফল হলে সরাসরি upload flow শুরু হয় (`UploadRepository.upload()`, `AttachmentPicker`-এর মতোই `/api/upload` টু-স্টেপ ফ্লো), আর `url()` getter দিয়ে caller-এর কাছে ফলাফল (uploaded file-এর URL) পাওয়া যায়।

### `CheckInActivity.java` — সব একসাথে জোড়া লাগানো

**দুটো prerequisite যতক্ষণ না পূরণ হয়, বাটন disabled:**
```java
private void updateActionButtonState() {
    boolean ready = selfieCapture.url() != null && latitude != null && longitude != null;
    binding.btnCheckIn.setEnabled(ready && !checkedInToday);
    binding.btnCheckOut.setEnabled(ready && checkedInToday && !checkedOutToday);
}
```
এই একটা মেথডই **single source of truth** — location fix পেলে (`requestLocation()`-এর callback), selfie upload হলে (`SelfieCapture.Listener.onUploaded()`), অথবা selfie clear হলে — সব ক্ষেত্রেই `updateActionButtonState()` আবার কল হয়ে বাটনের enabled state recompute করে। কোথাও অন্য কোনো জায়গায় manually `setEnabled(true)` লেখা নেই।

**আজকের attendance-এর ৩টা সম্ভাব্য অবস্থা:**
```java
private void bindToday(AttendanceResponse today) {
    if (today == null) {
        // এখনো আজ চেক-ইন করা হয়নি
        showCheckInButton();
    } else if (today.isCheckedOut()) {
        // চেক-ইন এবং চেক-আউট দুটোই হয়ে গেছে — দুটো বাটনই GONE
        binding.btnCheckIn.setVisibility(View.GONE);
        binding.btnCheckOut.setVisibility(View.GONE);
    } else if (checkedInToday) {
        // চেক-ইন হয়েছে, চেক-আউট বাকি
        showCheckOutButton();
    } else {
        showCheckInButton();
    }
    ...
}
```

**Server-side geofence flag দেখানো (client block করে না):**
```java
if (today.isLocationFlagged()) {
    binding.flaggedBanner.setVisibility(View.VISIBLE);
    binding.flaggedBanner.setText(
            getString(R.string.status_location_flagged, today.getLocationFlagReason()));
}
```
এটা খুব গুরুত্বপূর্ণ design decision — Activity-র জাভাডক কমেন্টে স্পষ্ট লেখা:
> "The server is the actual authority on whether the location is close enough to the office — this screen just won't submit without both pieces of evidence attached."

মানে **client কখনো নিজে থেকে সিদ্ধান্ত নেয় না** যে GPS coordinate অফিসের কাছাকাছি কিনা — সেটা `AttendanceLocationSettingsActivity`-তে সেট করা office lat/lng/radius-এর সাথে তুলনা করে **backend** সিদ্ধান্ত নেয়, আর flag করে (`isLocationFlagged`, `locationFlagReason`) যদি বাইরে হয়। App শুধু coordinate + selfie জমা দেয়, আর ফলাফল যা-ই আসুক (flagged বা না) সেটা দেখায় — চেক-ইনটা block করে না, কারণ হয়তো ভ্যালিড কারণেই (সাইট ভিজিট, WFH অনুমোদন) কেউ অফিসের বাইরে থেকে চেক-ইন করছে; flag শুধু পরে admin review-এর জন্য একটা সংকেত।

> **Interview প্রশ্ন: "GPS spoofing (mock location app দিয়ে GPS coordinate নকল করা) কীভাবে ঠেকানো হয়?"**
> সৎ উত্তর: client-side থেকে GPS spoofing পুরোপুরি ঠেকানো প্রায় অসম্ভব — rooted ডিভাইস বা mock-location developer option দিয়ে যেকোনো coordinate পাঠানো যায়। এই প্রজেক্টের approach তাই **prevention** না, **evidence + detection**: (১) selfie বাধ্যতামূলক করে অন্তত ছবি জাল করাটা কঠিন করে তোলে (live camera capture, গ্যালারি pick না), (২) GPS coordinate সংগ্রহ করে ও `locationFlagged` হিসেবে server-এ চিহ্নিত রাখে, যাতে পরে admin/HR review করতে পারে অস্বাভাবিক প্যাটার্ন খুঁজে (যেমন কেউ রোজ "flagged" location থেকে চেক-ইন করছে)। এটা "hard block" না, "audit trail" মডেল — বাস্তব HR সিস্টেমে এটাই common approach, কারণ hard block false-positive-এ (ভ্যালিড remote worker, নতুন অফিস ব্রাঞ্চ) legitimate ব্যবহারকারীকেও আটকে দিতে পারে।

### `AttendanceLocationSettingsActivity.java` — Office Location কনফিগার করা

```java
/**
 * COMPANY_OWNER-only screen (see AccountActivity) for the office location that attendance
 * check-ins are measured against. Enforcement defaults off server-side, so leaving this unset
 * is a valid, safe state.
 */
```

এই স্ক্রিনেই সেই office lat/lng/radius/enforcement-toggle সেট হয়, যেটার বিপরীতে `CheckInActivity`-র submitted coordinate compare হয়। একই `LocationHelper` এখানেও পুনর্ব্যবহৃত হয় — "Use Current Location" বাটন চাপলে admin নিজে অফিসে দাঁড়িয়ে থাকলে বর্তমান GPS fix-ই office coordinate হিসেবে বসিয়ে দিতে পারে:
```java
locationHelper.requestLocation(new LocationHelper.Listener() {
    @Override public void onLocation(double latitude, double longitude) {
        binding.latitudeEditText.setText(String.valueOf(latitude));
        binding.longitudeEditText.setText(String.valueOf(longitude));
    }
    ...
});
```

`enforcementEnabled` একটা switch — চালু না থাকলে location flag আদৌ হয় না (soft-launch friendly, "আগে সবাইকে normal check-in করতে দাও, পরে কোম্পানি রেডি হলে enforce করো")।

### Attendance মডিউল — সম্পূর্ণ ফাইল তালিকা

| ফাইল | কাজ |
|---|---|
| `LocationHelper.java` | Runtime permission + একবারের GPS fix — plain `LocationManager`, race-safe delivery |
| `CheckInActivity.java` | Selfie + GPS ready হলেই check-in/out বাটন সক্রিয়; আজকের status ৩ অবস্থায় render করে |
| `AttendanceLocationSettingsActivity.java` | Owner-only: office lat/lng/radius/enforcement কনফিগার |

(`SelfieCapture.java` টা `ui/common/` প্যাকেজে থাকলেও এই মডিউলেরই মূল অংশ — উপরে বিস্তারিত আলোচনা করা হয়েছে।)

---

## ৬.৫ Payroll মডিউল (`ui/payroll/`)

**বাস্তব সমস্যা:** Employee প্রতি মাসের salary slip (payslip) দেখতে ও PDF ডাউনলোড করতে চায়। এই মডিউল read-only — কোনো create/submit ফর্ম নেই (payroll admin panel থেকে জেনারেট হয়, employee শুধু দেখে)।

### `PayrollStatusBadge.java` — এই একটাতেই raw string literal

```java
public static int colorFor(Context context, String status) {
    int colorRes;
    if ("PAID".equals(status)) {
        colorRes = R.color.status_success;
    } else if ("CANCELLED".equals(status)) {
        colorRes = R.color.status_danger;
    } else {
        colorRes = R.color.status_warning;
    }
    return ContextCompat.getColor(context, colorRes);
}
```

লক্ষ করো — `LeaveRequestStatusBadge`/`ExpenseStatusBadge` একটা shared constants class (`LeaveRequestStatus.APPROVED`, `ExpenseStatus.PAID`) রেফার করে, কিন্তু এখানে সরাসরি `"PAID"`, `"CANCELLED"` raw string literal হিসেবে লেখা — মানে এই প্রজেক্টে `PayrollStatus` নামের কোনো constants class নেই (comment-এ বলা আছে "DRAFT, APPROVED, PAID, CANCELLED" কিন্তু ক্লাসটা বানানো হয়নি)। ছোট কিন্তু আরেকটা consistency gap।

> **Interview প্রশ্ন: "raw string literal দিয়ে status compare করার ঝুঁকি কী?"**
> Typo হলে compile-time-এ ধরা পড়ে না — `"PIAD"` লিখলে (অক্ষর উল্টো) compiler কিছু বলবে না, শুধু runtime-এ status সবসময় "unmatched" (default warning রঙ) দেখাবে, silently ভুল দেখাবে। `LeaveRequestStatus.APPROVED`-এর মতো একটা `public static final String` constant ব্যবহার করলে অন্তত IDE-তে autocomplete পাওয়া যায়, আর ভুল বানান হলে সেটা একটা compile error (undefined symbol) হয়ে যেত, বরং silent bug না হয়ে।

### `PayslipAdapter.java` — মাসের নাম বের করা

```java
String monthName = new DateFormatSymbols().getMonths()[payslip.getPayMonth() - 1];
binding.itemPayPeriod.setText(monthName + " " + payslip.getPayYear());
```
`payMonth` server থেকে `1-12` (January=1) হিসেবে আসে, কিন্তু `DateFormatSymbols().getMonths()` array **0-indexed** (`[0]` = January) — তাই `- 1` অফসেট। `DateFormatSymbols` locale-aware (device-এর ভাষা অনুযায়ী "January" বা "জানুয়ারি" দেখাতে পারে, যদি resource bundle থাকে), তাই hardcoded ইংরেজি মাসের নামের array বানানোর চেয়ে ভালো।

### `PayslipListActivity.java` — দুই-ধাপের fetch

```java
/**
 * "My Payslips" — payroll has no bare "/my" endpoint (unlike leave/attendance), so this screen
 * first resolves the caller's own employeeId via /api/employees/me, then lists payroll by that
 * id. No ViewModel: a two-step, rarely-changing list like this doesn't carry its weight.
 */
```

```java
private void load() {
    binding.stateView.showLoading();
    repository.getMyEmployeeProfile(new Callback<EmployeeResponse>() {
        @Override public void onResponse(...) {
            loadPayslips(response.body().getId());   // ধাপ ১-এর ফলাফল ধাপ ২-এর ইনপুট
        }
        ...
    });
}
```

Leave/Attendance-এ backend একটা সরাসরি "/my..." endpoint দেয় (JWT থেকেই caller কে চিনে ফেলে), কিন্তু payroll API `employeeId` দিয়ে query করে — তাই client-কে আগে নিজের `employeeId` জানতে হয় (`/api/employees/me` কল করে), তারপর সেটা দিয়ে দ্বিতীয় কল। এটা একটা callback-এর-ভেতরে-callback (nested) প্যাটার্ন — RxJava/Coroutine হলে `flatMap`/`suspend` চেইন দিয়ে সরলভাবে লেখা যেত, কিন্তু plain Retrofit `Callback` দিয়ে nested `onResponse()`-ই স্বাভাবিক উপায়।

**PDF ডাউনলোড ও খোলা:**
```java
private void downloadPdf(PayrollResponse payslip) {
    repository.downloadPayslipPdf(payslip.getId(), new Callback<ResponseBody>() {
        @Override public void onResponse(...) {
            PdfOpener.writeAndOpen(PayslipListActivity.this, response.body(),
                    "payslip-" + payslip.getId() + ".pdf");
        }
    });
}
```
`PdfOpener.writeAndOpen()` (`ui/common/`) সাধারণ একটা পুনর্ব্যবহারযোগ্য utility — response body-র raw bytes cache dir-এ লিখে, `FileProvider` দিয়ে secure Uri বানিয়ে, `Intent.ACTION_VIEW` দিয়ে device-এর PDF viewer খুলে দেয় (একই `FileProvider` mechanism যেটা `SelfieCapture`-এও ব্যবহার হয়েছে — camera app-কে file access দিতে, আর এখানে PDF viewer app-কে)। `ActivityNotFoundException` catch করা হয় যদি ডিভাইসে কোনো PDF viewer install করা না থাকে।

**কখন পুরো-স্ক্রিন error, কখন Toast — এই মডিউলের নিজস্ব ন্যায্যতা:**
```java
// load() only ever runs once per activity instance (from onCreate, no onResume refresh), so
// a failure here can never be replacing content already on screen — safe to hand the whole
// screen over to the error state rather than just toasting.
private void showLoadError() {
    binding.stateView.showError(R.string.error_payslips_load_failed, v -> load());
}
```
এখানে `load()` শুধু `onCreate()`-এ একবারই কল হয় (`onResume()`-এ refresh নেই, কারণ payslip মাসে একবার তৈরি হয়, ঘন ঘন বদলায় না) — তাই "failure মানে already-shown content ঢেকে ফেলা" এই ঝুঁকিটা এখানে নেই, `ExpenseListActivity`-র উল্টো পরিস্থিতি। তাই এখানে নিরাপদে পুরো-স্ক্রিন `StateView.showError()` ব্যবহার করা যায়, retry বাটন সহ।

> **Interview প্রশ্ন: "কখন full-screen error state দেখাবে, কখন Toast?"**
> এই প্রশ্নটা অধ্যায় ৪.৫-এর `CachedListViewModel`-এর `onRefreshFailed()` নীতিরই সাধারণীকরণ: **যদি user-এর সামনে ইতিমধ্যে ডেটা থাকতে পারে এমন সম্ভাবনা থাকে (যেমন `onResume()`-এ বারবার reload হয়), তাহলে ব্যর্থতা Toast হওয়া উচিত — আগের content ঢেকে দেওয়া যাবে না। যদি load() নিশ্চিতভাবে একবারই চলে (`onCreate()`, কোনো re-entry reload নেই), তাহলে ব্যর্থতা মানেই স্ক্রিনে দেখানোর মতো কিছু নেই — তখন full-screen error + retry ঠিক আছে।** `PayslipListActivity` আর `ExpenseListActivity` — দুটো ভিন্ন সিদ্ধান্তের উদাহরণ, আর দুটোই যুক্তিসঙ্গত কারণ তাদের reload-behavior ভিন্ন।

### Payroll মডিউল — সম্পূর্ণ ফাইল তালিকা

| ফাইল | কাজ |
|---|---|
| `PayrollStatusBadge.java` | Status → রঙ ও লেবেল (raw string literal দিয়ে, shared constants class ছাড়াই) |
| `PayslipAdapter.java` | List item bind — pay period, net salary, status badge, download বাটন |
| `PayslipListActivity.java` | দুই-ধাপে fetch (employeeId রিজলভ → payslips), PDF ডাউনলোড ও ওপেন |

---

## ৬.৬ এই অধ্যায়ের ইন্টারভিউ-প্রশ্ন সারাংশ

| প্রশ্ন | সংক্ষিপ্ত উত্তরের সারমর্ম |
|---|---|
| ৫টা মডিউলে আলাদা আলাদা `StatusBadge` ক্লাস কেন, একটা shared class না কেন? | প্রতিটা ডোমেইনের status set ও রঙ-মাপিং আলাদা; শুধু *rendering* (`StatusBadgeView.bind()`) shared, decision-logic আলাদা — coupling কমানোর জন্য ইচ্ছাকৃত |
| Leave module `CachedListViewModel` ব্যবহার করে, Expense/Timesheet/Payroll করে না কেন? | এটা inconsistency — প্রযুক্তিগতভাবে সবগুলোতেই apply করা যেত; সততার সাথে "known gap" বলাই ভালো |
| Write অপারেশন (cancel/approve/reject) ViewModel-এর বদলে সরাসরি Repository দিয়ে কেন? | `CachedListViewModel` ইচ্ছাকৃতভাবে read-only ("Writes go straight to the API") — scope সংকীর্ণ রাখা |
| Retrofit-এ null `@Query` parameter পাঠালে কী হয়? | পুরো key-value pair URL থেকে বাদ পড়ে যায়; backend required param হলে ৪০০ error — তাই empty string পাঠানো নিরাপদ |
| টাকার amount-এ `double` না `BigDecimal` কেন? | Binary floating-point-এ decimal fraction নির্ভুল represent হয় না; financial calculation-এ সবসময় arbitrary-precision decimal দরকার |
| GPS/selfie দিয়ে কি spoofing পুরোপুরি আটকানো যায়? | না — এটা "prevention" না "evidence + server-side flag" মডেল; client block করে না, শুধু প্রমাণ সংগ্রহ ও চিহ্নিত করে |
| `play-services-location`-এর বদলে plain `LocationManager` কেন? | অ্যাপের আর কোথাও Play Services dependency নেই; এক স্ক্রিনের জন্য নতুন library যোগ করাটা worth না |
| `LocationHelper.finish()`-এ race condition কীভাবে সামলানো হয়? | fresh fix আর timeout দুটোই `finish()` ডাকতে পারে; `pendingListener = null` guard নিশ্চিত করে যেটাই আগে আসুক, একবারই ফলাফল যায় |
| কখন full-screen error state দেখাবে, কখন Toast? | Reload নিশ্চিতভাবে একবারই চলে (payslips) → full-screen error নিরাপদ। বারবার (`onResume()`) reload হয় (expenses) → Toast, আগের content ঢেকে দেওয়া যাবে না |
| Spinner-এর বদলে `AutoCompleteTextView` + `TextInputLayout.ExposedDropdownMenu` কেন? | Material Design floating-label look দেয়, `TextInputLayout`-এর সাথে visually consistent, form-এর বাকি field-এর সাথে একই language |
| একটা Adapter দুই স্ক্রিনে reuse করার trade-off কী? | Sync থাকা ও কম duplicate কোডের সুবিধা, কিন্তু flag/nullable-listener বেশি হলে readability কমে — ছোট পার্থক্যেই reuse worth it |

---

**কভারেজ:** এই অধ্যায়ে মোট **২৩টা `.java` ফাইল** সম্পূর্ণ পড়ে ব্যাখ্যা করা হয়েছে — Leave (৯টা), Expense (৬টা), Timesheet (২টা), Attendance (৩টা, প্লাস `ui/common/SelfieCapture.java`), Payroll (৩টা) — সাথে সহায়ক `ui/common/` ফাইল (`StatusBadgeView`, `StateView`, `CacheStamp`, `AttachmentPicker`, `UiErrors`, `PdfOpener`) ও সংশ্লিষ্ট Repository (`LeaveRequestRepository`, `ExpenseRepository`, `TimesheetRepository`, `AttendanceRepository`, `PayrollRepository`) এবং form layout XML (`activity_create_leave_request.xml`, `activity_create_expense.xml`) রেফারেন্স হিসেবে দেখা হয়েছে।
