# অধ্যায় ৮ — Service Request, Support ও যোগাযোগ-সংক্রান্ত মডিউল

এই অধ্যায়ে আমরা প্রজেক্টের **সবচেয়ে জটিল অংশে** ঢুকছি — `ui/servicerequest/`, `ui/support/`, `ui/kb/`, `ui/noticeboard/`, `ui/notification/`। এই পাঁচটার মধ্যে প্রথম দুটো (service request, support ticket) হলো এই অ্যাপের আসল "product" — multi-role workflow (client vs staff একই স্ক্রিনে আলাদা আচরণ করে) আর real-time chat (STOMP protocol, অধ্যায় ২.৮-এ যেটার architecture-level পরিচয় হয়েছিল) একসাথে মিলেছে এখানে। বাকি তিনটা (KB, notice board, notification) তুলনামূলক সহজ, read-mostly স্ক্রিন — কিন্তু এদের মধ্যেও কিছু ভালো design decision আছে।

মোট **২৭টা Java ফাইল** কভার হচ্ছে এই অধ্যায়ে — servicerequest (১০), support (৭), kb (৩), noticeboard (৩), notification (৪)।

---

## ৮.১ Service Request মডিউল (`ui/servicerequest/`) — গভীর বিশ্লেষণ

এটা এই গোটা অ্যাপের **সবচেয়ে বড় module**। একটা service request-এর পুরো jীবনচক্র (create → assign → quotation → in-progress → completed) client আর staff দুই পক্ষই এখান থেকেই দেখে, একই detail screen থেকে — কিন্তু কে কী করতে পারবে সেটা runtime-এ role অনুযায়ী বদলায়।

### Dynamic Form Rendering (`DynamicFormRenderer.java`)

প্রতিটা service (যেমন "AC Repair", "Legal Consultation") admin panel থেকে নিজের কিছু **custom field** define করতে পারে — আর সেই field-set compile-time-এ জানা থাকে না, কারণ সেটা runtime-এ `GET /api/v1/services/{serviceId}/form-fields` কল করে জানতে হয়। তাই একটা fixed XML layout দিয়ে এটা বানানো অসম্ভব — এই কারণেই `DynamicFormRenderer` পুরো form-টা **কোড থেকে** (programmatically) বানায়, `LinearLayout` container-এর ভেতর view যোগ করে করে।

```java
private final Map<ServiceFormField, View> inputs = new LinkedHashMap<>();
```

`LinkedHashMap` ব্যবহার করা হয়েছে **insertion order** রাখার জন্য — কেন গুরুত্বপূর্ণ? কারণ `firstMissingRequiredLabel()` মেথডটা "প্রথম যে required field খালি আছে" সেটা রিপোর্ট করে, আর সেই "প্রথম"-টা form-এ যে ক্রমে field দেখানো হয়েছে সেই ক্রমেই হওয়া উচিত — user যেটা প্রথমে দেখবে, error-ও সেটার জন্যই আগে আসা উচিত। সাধারণ `HashMap` হলে iteration order অনির্দিষ্ট হতো।

**`fieldType` অনুযায়ী View তৈরি** (`inputFor()`) — একটা বড় `switch`:

| `fieldType` (backend constant) | কী View বানায় |
|---|---|
| `TEXT`, default | `TextInputLayout` + `TextInputEditText`, plain text |
| `TEXTAREA` | একই, কিন্তু multi-line (৩ লাইন), `Gravity.TOP` |
| `NUMBER` | `InputType.TYPE_CLASS_NUMBER \| TYPE_NUMBER_FLAG_DECIMAL` |
| `EMAIL` / `PHONE` | সংশ্লিষ্ট `InputType` variation |
| `DROPDOWN` | `AutoCompleteTextView` (exposed dropdown, পুরনো raw `Spinner` না) |
| `RADIO` | `RadioGroup` + কয়েকটা `RadioButton` |
| `CHECKBOX` | একটা `CheckBox` |
| `DATE` | `TextInputLayout` + `DatePickerDialog`, ISO-8601 ফরম্যাটে টেক্সট বসায় |
| `FILE_UPLOAD` | আপাতত একটা URL-input টেক্সট ফিল্ড (আগে থেকে আপলোড করা ফাইলের URL বসাতে হয় — সরাসরি file picker এখনো wire করা হয়নি) |
| `FORMULA` | **কিছুই না — পুরো field skip করা হয়** |

`FORMULA` কেন skip? — এটা backend অন্য উত্তর থেকে হিসাব করে বসায়; একটা input দেখালে user ভাববে সেটা টাইপ করা যায়, অথচ সেটা টাইপ করলেও backend সেটা overwrite করে দেবে। তাই দেখানোই না করা ভালো।

**DROPDOWN/RADIO-র options কোথা থেকে আসে?** — as-built doc-এর একটা গুরুত্বপূর্ণ ⚠ পয়েন্ট: admin form builder-এ আলাদা কোনো "options" column নেই। `DROPDOWN`/`RADIO` field-এর সম্ভাব্য মানগুলো `validationRules` ফিল্ডে **কমা দিয়ে আলাদা করা** একটা string হিসেবে থাকে:

```java
// ServiceFormField.java
public List<String> options() {
    List<String> options = new ArrayList<>();
    if (validationRules == null) return options;
    for (String option : validationRules.split(",")) {
        String trimmed = option.trim();
        if (!trimmed.isEmpty()) options.add(trimmed);
    }
    return options;
}
```

এটা মোটেও intuitive না — নাম দেখে মনে হয় `validationRules` মানে বুঝি min/max length টাইপ কিছু, কিন্তু আসলে dropdown-এর options এখানেই লুকানো। Web client-ও ঠিক একইভাবে পড়ে, তাই এটা কোনো bug না, একটা (আজব হলেও) স্থির contract।

> **Interview প্রশ্ন: "Dynamic form কীভাবে বানালেন — XML দিয়ে না কোড দিয়ে? কেন?"**
> উত্তর: "Field-set compile time-এ জানা থাকে না — এটা admin runtime-এ ঠিক করে, প্রতিটা service-এর নিজের field থাকতে পারে। তাই আমরা `DynamicFormRenderer`-এ প্রতিটা `ServiceFormField`-এর `fieldType` দেখে runtime-এ সংশ্লিষ্ট View (TextInputLayout/RadioGroup/CheckBox ইত্যাদি) বানিয়ে একটা container-এ যোগ করি। এই approach-এর trade-off হলো — এটা RecyclerView-based approach-এর চেয়ে সহজ (form সাধারণত ছোট, কয়েকটা field), কিন্তু বড় form হলে RecyclerView.Adapter দিয়ে করা ভালো হতো performance-এর জন্য।"

**Answer collection — `answers()`:**

```java
public Map<String, String> answers() {
    Map<String, String> answers = new LinkedHashMap<>();
    for (Map.Entry<ServiceFormField, View> entry : inputs.entrySet()) {
        String value = valueOf(entry.getKey(), entry.getValue());
        if (!value.isEmpty() && entry.getKey().getId() != null) {
            answers.put(String.valueOf(entry.getKey().getId()), value);
        }
    }
    return answers.isEmpty() ? null : answers;
}
```

লক্ষ্য করো — key হলো **field id-কে string বানিয়ে** (`"42"`), value হলো user-এর টাইপ করা উত্তর। এটাই `CreateServiceRequestRequest.formData` হিসেবে পাঠানো হয়। কয়েকটা সূক্ষ্ম কিন্তু গুরুত্বপূর্ণ নিয়ম `valueOf()`-এ আছে:
- **Checkbox**: টিক না থাকলে `""` — কারণ "false" পাঠালে backend ভাববে এটা একটা real answer (explicitly "না" বলা হয়েছে), অথচ আসলে user কিছুই বলেনি।
- **Dropdown-এর "no selection" placeholder**: একটা optional dropdown-এ একটা synthetic "কিছু নির্বাচন করা হয়নি" option যোগ করা হয় (`form_field_no_selection` string) যাতে user explicitly "কিছু না" বেছে নিতে পারে — কিন্তু সেটা নির্বাচিত থাকলে সেটাও `""` হিসেবে গণ্য হয়, answer map-এ যায় না।

### Detail Screen ও Multi-role Workflow

`ServiceRequestDetailActivity` + `ServiceRequestDetailViewModel` — এই জোড়া এই প্রজেক্টের সবচেয়ে জটিল screen। এতে আছে: request details card, quotation card, status timeline, comment thread (real-time chat), আর **৬টা dialog** (reject reason, review, assign employee, change status, submit quotation, cancel confirm)।

**`isStaff()` — role branching-এর কেন্দ্রবিন্দু:**

```java
// ServiceRequestDetailViewModel constructor
String role = ZuhooApplication.graph().tokenManager().getRole();
staff = Role.COMPANY_OWNER.equals(role) || Role.EMPLOYEE.equals(role);
```

Login response-এর সাথে আসা `role` টোকেন থেকে সরাসরি বের করা হয় user staff কিনা (COMPANY_OWNER বা EMPLOYEE)। ViewModel-এ এটা একবারই constructor-এ resolve হয়ে যায়, তারপর `isStaff()` getter দিয়ে Activity সেটা পড়ে।

`bindDetail()`-এ role অনুযায়ী UI কীভাবে আলাদা হয়:

```java
if (viewModel.isStaff()) {
    binding.btnCancelRequest.setVisibility(View.GONE);
    binding.btnLeaveReview.setVisibility(View.GONE);
    binding.staffActions.setVisibility(View.VISIBLE);   // assign/status/quotation বোতাম
} else {
    boolean canCancel = detail.getAssignedEmployeeName() == null
            && ServiceRequestStatus.isOpen(detail.getStatus());
    binding.btnCancelRequest.setVisibility(canCancel ? View.VISIBLE : View.GONE);

    boolean canReview = ServiceRequestStatus.COMPLETED.equals(detail.getStatus());
    binding.btnLeaveReview.setVisibility(canReview ? View.VISIBLE : View.GONE);
}
```

আর quotation accept/reject বোতাম শুধু client-দের জন্যই দেখানো হয় (`quotationPending && !viewModel.isStaff()`) — staff কখনো নিজের দেওয়া quotation নিজে accept/reject করবে না, সেটা তো client-এর কাজ।

এভাবেই **একটাই layout, একটাই Activity, একটাই ViewModel** — কিন্তু runtime-এ দুই সম্পূর্ণ ভিন্ন persona-র জন্য কাজ করে। এটাই as-built doc-এর §5a.2-এ বলা "Detail is the **same screen** as the client's, with staff-only actions shown conditionally"।

> **Interview প্রশ্ন: "একই স্ক্রিন দুই ভিন্ন role-এর জন্য কীভাবে সামলালেন — দুটো আলাদা Activity বানাননি কেন?"**
> উত্তর: "কারণ ৯৫% UI (title, status, description, comment thread, timeline) দুই role-এর জন্যই এক। যা আলাদা সেটা শুধু কিছু action button-এর visibility। দুটো Activity বানালে ওই বড় common অংশটা duplicate হতো, maintenance-এ দুই জায়গায় বদলাতে হতো। তাই একটাই Activity রেখে, role অনুযায়ী কিছু `View.GONE`/`View.VISIBLE` toggle করা হয়েছে — `isStaff()` একটা single source of truth হিসেবে কাজ করে।"

### "Add Comment"-এর ভয়ংকর বাগ — visibility না পাঠালে client কিছুই দেখে না

এটা as-built doc-এ (§6) স্পষ্ট করে লেখা "the most damaging bug the original spec left open" — একটা চমৎকার real-world debugging গল্প, interview-এ বলার মতো।

**সমস্যাটা কী ছিল:** Backend-এর `ServiceRequestServiceImpl.addComment()` — কেউ comment পাঠালে যদি `visibility` field null থাকে, backend নিজে থেকে একটা default বসিয়ে দেয়, কিন্তু **role অনুযায়ী আলাদা default**:
- Client পাঠালে default হয় `CLIENT` (ঠিক আছে)
- **Staff পাঠালে default হয় `INTERNAL`** (staff-দের নিজেদের মধ্যে নোট রাখার জন্য)

আর `getComments()` (client যখন থ্রেড লোড করে) `INTERNAL` visibility-র কমেন্ট **filter করে বাদ দিয়ে দেয়** — কারণ ওগুলো তো staff-only note। ফলে একজন staff member যদি visibility field পাঠাতে ভুলে যায় (যেটা করাই স্বাভাবিক, কারণ ফিল্ডটা `optional` চিহ্নিত), তার কমেন্ট backend-এ সেভ হয়ে যায়, ২০০ response-ও আসে — কিন্তু **client কখনো সেটা দেখতেই পায় না**। কোনো error নেই, কোনো crash নেই — শুধু নীরবে হারিয়ে যাওয়া একটা reply।

এটা এমন একটা bug যেটা QA-তেও ধরা কঠিন — কারণ staff-এর নিজের অ্যাপে সব ঠিকঠাক দেখাবে (staff নিজের কমেন্টও দেখতে পায়), শুধু client-এর দিক থেকে সমস্যাটা বোঝা যাবে।

**সমাধান — `AddCommentRequest.java`-তে দুটো আলাদা factory method:**

```java
// Staff comments MUST carry an explicit visibility — the backend defaults a staff
// comment to INTERNAL, which the client can never see.
public static AddCommentRequest fromStaff(String content, String attachmentUrl) {
    return new AddCommentRequest(content, VISIBILITY_CLIENT, attachmentUrl);
}

// Clients can leave visibility null and take the server-side default, which is already CLIENT.
public static AddCommentRequest fromClient(String content, String attachmentUrl) {
    return new AddCommentRequest(content, null, attachmentUrl);
}
```

আর `ViewModel.sendComment()`-এ:

```java
AddCommentRequest request = staff
        ? AddCommentRequest.fromStaff(content, attachmentUrl)
        : AddCommentRequest.fromClient(content, attachmentUrl);
```

**Design decision-টা লক্ষ্য করো:** client-এর ক্ষেত্রে `visibility` এখনো `null` পাঠানো হয় (কারণ default-ই ঠিক আছে), কিন্তু staff-এর ক্ষেত্রে **explicitly** `"CLIENT"` পাঠানো হয়। এটা "compiler-কে বাধ্য করা" ধরনের একটা pattern — দুটো আলাদা named factory method থাকায়, `new AddCommentRequest(...)` সরাসরি কল করার কোনো উপায় নেই (constructor `private`), তাই ভবিষ্যতে কেউ নতুন call-site লিখলেও ভুলে visibility বাদ দিতে পারবে না — কম্পাইলার তাকে `fromStaff`/`fromClient` বেছে নিতে বাধ্য করবে।

> **Interview প্রশ্ন: "একটা real bug-এর গল্প বলুন যেটা আপনি নিজে ধরেছেন এবং ঠিক করেছেন।"**
> এটাই সেই গল্প — ready-made উত্তর। মূল পয়েন্ট: (১) bug-টা silent ছিল, কোনো error দেখাতো না; (২) এটা শুধু এক পক্ষের (client) দৃষ্টিকোণ থেকেই দৃশ্যমান, তাই দুই দিক থেকেই test করা জরুরি ছিল; (৩) fix-টা শুধু "null-এর বদলে ভ্যালু বসানো" না, বরং একটা API design করা হয়েছে (দুটো named factory method) যাতে ভবিষ্যতে কেউ ভুলে আবার এই bug ফিরিয়ে না আনে।

### Status Timeline — আগে staff-only ছিল, client-দের জন্য খোলা হয়েছে

`GET /api/service-requests/{id}/history` — as-built doc §15 অনুযায়ী এটা আগে `@PreAuthorize`-এ শুধু staff-দের জন্য ছিল, client কল করলে 403 পেত। এই প্রজেক্টে `CLIENT` role যোগ করে খোলা হয়েছে — নিরাপদ, কারণ `guardAccess()` ইতিমধ্যে নিশ্চিত করে একজন client শুধু **নিজের** request-এর history-ই দেখতে পারবে, অন্য কারো না।

```java
private void bindTimeline(List<RequestStatusHistory> history) {
    ...
    for (RequestStatusHistory entry : history) {
        StringBuilder line = new StringBuilder()
                .append(entry.getChangedAt())
                .append("  •  ")
                .append(StatusBadge.labelFor(this, entry.getNewStatus()));
        if (!TextUtils.isEmpty(entry.getChangedByName())) {
            line.append("  — ").append(entry.getChangedByName());
        }
        if (!TextUtils.isEmpty(entry.getReason())) {
            line.append('\n').append(entry.getReason());
        }
        ...
    }
}
```

এখানে একটা product-decision worth noting: timeline-এ `reason` আর `changedByName` — মানে staff যে কারণ লিখে status বদলেছে, সেটাও client দেখতে পাচ্ছে। as-built doc নিজেই এটাকে "worth confirming as a product decision" বলেছে — সাধারণ helpdesk-এর জন্য এটা স্বাভাবিক (transparency ভালো), কিন্তু এটা আগে staff-only ছিল, তাই এই পরিবর্তনটা সচেতনভাবে নেওয়া সিদ্ধান্ত, দুর্ঘটনাক্রমে না।

### Chat/Comment Thread — Real-time সংযোগ

`ServiceRequestDetailViewModel.connectChat()`:

```java
private void connectChat() {
    connectionListener = chatLive::setValue;
    chatSocket.addConnectionListener(connectionListener);

    subscription = chatSocket.subscribe(
            "/user/queue/service-requests/" + requestId + "/messages",
            (destination, jsonBody) ->
                    appendComment(new Gson().fromJson(jsonBody, RequestComment.class)));
}
```

প্রতিটা request-এর নিজস্ব destination (`.../{requestId}/messages`) — মানে একই `ChatSocket` (app-wide, singleton) একসাথে একাধিক destination-এ subscribe থাকতে পারে, প্রতিটা আলাদা request/ticket-এর জন্য। ৮.২-এ এই socket-টার internal mechanics নিয়ে গভীরে যাওয়া হবে।

**Dedup logic** — কেন দরকার? কারণ REST দিয়ে (`loadComments()`) আর socket push দিয়ে একই comment দুইবার আসতে পারে (রিফ্রেশ + push একসাথে হলে):

```java
private void appendComment(RequestComment comment) {
    for (int i = 0; i < thread.size(); i++) {
        if (comment.getId() != null && comment.getId().equals(thread.get(i).getId())) {
            thread.set(i, comment);   // replace, duplicate না বানিয়ে
            comments.setValue(new ArrayList<>(thread));
            return;
        }
    }
    thread.add(comment);
    comments.setValue(new ArrayList<>(thread));
}
```

আর **নিজের পাঠানো কমেন্ট নিজে থেকে থ্রেডে যোগ করা হয়** REST response আসার পরে (`appendComment(response.body())`) — কারণ (২.৮ ও as-built §14.1 অনুযায়ী) backend socket push শুধু **অন্য পক্ষকে** পাঠায়, sender নিজের session-এ কখনো echo ফেরত পায় না। এটা ভুলে গেলে নিজের পাঠানো মেসেজ নিজের স্ক্রিনেই দেখা যেত না।

### `item_comment.xml` — Compact Card Redesign

এই সেশনের redesign-এ `item_comment.xml` (আর সমান্তরালভাবে `item_support_message.xml`) কে সাধারণ ফ্ল্যাট row থেকে একটা **compact card**-এ পরিণত করা হয়েছে:

```xml
<LinearLayout
    android:background="@drawable/bg_card"
    android:padding="@dimen/card_padding_compact"
    android:orientation="vertical">
    <LinearLayout android:orientation="horizontal">     <!-- author + date header row -->
        <TextView android:id="@+id/commentAuthor" .../>
        <TextView android:id="@+id/commentDate" .../>
    </LinearLayout>
    <TextView android:id="@+id/commentContent" .../>    <!-- body -->
</LinearLayout>
```

XML-এর কমেন্টেই লেখা আছে গুরুত্বপূর্ণ দুটো design সিদ্ধান্ত: (১) `bg_card` ব্যবহার হয়েছে `bg_card_clickable` না — কারণ একটা কমেন্ট row-এ কোনো tap action নেই; (২) `item_support_message.xml`-এ একটা মন্তব্য আছে যে এটা "sender-aligned chat bubble" (WhatsApp-স্টাইল, ডান-বাম আলাদা) না, বরং একটা flat log entry — কারণ adapter-এ "এই মেসেজ কি current user-এর?" এই তথ্যটাই নেই। এটা একটা সৎ, বাস্তবসম্মত সীমাবদ্ধতা — chat bubble UI বানাতে হলে backend response-এ sender-এর user id-র সাথে বর্তমান logged-in user id compare করার লজিক লাগত, যেটা এখনো নেই।

Color/icon সহ badge রেন্ডারিং সবসময় `StatusBadgeView.bind()` দিয়ে হয় — এটা একটা shared renderer, প্রতিটা domain-এর নিজস্ব `colorFor()`/`labelFor()` (এখানে `StatusBadge`, support-এ `TicketStatusBadge`) থেকে রঙ আর label নিয়ে soft-tint pill + tone icon বানায়। State (loading/empty/error) render করতে সব list screen `StateView` কম্পোনেন্ট ব্যবহার করে — এই দুটো (`StatusBadgeView`, `StateView`) হলো এই redesign-এর কেন্দ্রীয় reusable widget, `ui/common/`-এ থাকে।

### List Screen-এর Asymmetry — Client vs Staff

একটা লক্ষণীয় জিনিস: `ServiceRequestListActivity` (client) একটা `ServiceRequestListViewModel` ব্যবহার করে যেটা `CachedListViewModel`-এর subclass (অধ্যায় ৪.৫ দ্রষ্টব্য — cache-first, background refresh):

```java
public class ServiceRequestListViewModel extends CachedListViewModel<ServiceRequestSummary> {
    @Override
    protected void fetch(Callback<PageResponse<ServiceRequestSummary>> callback) {
        repository.getMyServiceRequests(callback);
    }
    ...
}
```

কিন্তু `StaffServiceRequestListActivity`-র **কোনো ViewModel নেই** — সরাসরি Activity থেকে `serviceRequestRepository.getAllServiceRequests(...)` বা `getAssignedToMe(...)` কল হয়, কোনো caching ছাড়াই, প্রতি `onResume()`-এ। এটা ইচ্ছাকৃত asymmetry — staff-এর list দুই ভিন্ন mode-এ (`MODE_ALL` / `MODE_ASSIGNED_TO_ME`) খোলে, mode-অনুযায়ী ভিন্ন endpoint, আর staff প্রায়ই এই স্ক্রিনে থেকে অন্য জায়গায় গিয়ে আবার ফিরে আসে (নতুন request assign হয়েছে কিনা দেখতে) — তাই "সবসময় fresh network call, cache না" এখানে বেশি প্রাসঙ্গিক আচরণ। ছোট trade-off: wifi না থাকলে staff-এর list খালি স্ক্রিন দেখাবে (client-এর মতো "showing saved data" stamp দেখাবে না)।

> **Interview প্রশ্ন: "কোন screen-এ ViewModel দরকার, কোনটাতে না — এই সিদ্ধান্ত কীভাবে নেন?"**
> উত্তর: "যেখানে caching/loading-state জটিল আর user বারবার আসা-যাওয়া করে (client-এর নিজের request list), সেখানে `CachedListViewModel` দরকার — offline হলেও কিছু দেখানো যায়। কিন্তু staff-এর all-requests list-এর মতো জায়গায়, যেখানে "সবসময় সর্বশেষ ডেটা" বেশি গুরুত্বপূর্ণ cache-এর চেয়ে, সরাসরি Repository কল simpler এবং ঠিক।"

### সম্পূর্ণ ফাইল তালিকা — `ui/servicerequest/`

| ফাইল | বর্ণনা |
|---|---|
| `DynamicFormRenderer.java` | প্রতিটা service-এর custom form field runtime-এ কোড দিয়ে বানায়, উত্তর collect করে `formData` map হিসেবে ফেরত দেয় |
| `CreateServiceRequestActivity.java` | নতুন request তৈরির ফর্ম — service dropdown বদলালে `DynamicFormRenderer` পুনরায় render হয়, priority `NORMAL` (না `MEDIUM`) |
| `ServiceRequestListActivity.java` | Client-এর নিজের request list — `BottomNavActivity` extend করে, `ServiceRequestListViewModel` (cached) ব্যবহার করে |
| `ServiceRequestListViewModel.java` | `CachedListViewModel<ServiceRequestSummary>` subclass — `getMyServiceRequests` fetch করে |
| `StaffServiceRequestListActivity.java` | Staff-এর request list — `MODE_ALL`/`MODE_ASSIGNED_TO_ME` দুই mode, ViewModel ছাড়া সরাসরি Repository কল |
| `ServiceRequestAdapter.java` | List item bind করে — title, hub service, priority, `StatusBadgeView` দিয়ে status badge |
| `ServiceRequestDetailActivity.java` | মূল detail screen — request info, quotation, timeline, comment thread, ৬টা action dialog |
| `ServiceRequestDetailViewModel.java` | Detail screen-এর সব state + chat subscription owner, `isStaff()` role-branching-এর উৎস |
| `CommentAdapter.java` | Comment thread-এর RecyclerView adapter — id দিয়ে dedupe করে |
| `StatusBadge.java` | `ServiceRequestStatus` স্ট্রিং থেকে রঙ (`colorFor`) ও localized label (`labelFor`) বের করে |

---

## ৮.২ Support Ticket মডিউল (`ui/support/`) — Real-time Chat

Company (owner/employee) যখন platform support-এর কাছে কোনো সমস্যা তুলতে চায়, সেটাই support ticket — service request-এর মতোই structure (list → create → detail-with-chat), কিন্তু ভিন্ন backend controller (`/api/v1/support/`) আর ভিন্ন status vocabulary।

`SupportTicketDetailViewModel`-এর কোডেই লেখা আছে সততার সাথে:

```java
// Mirrors ServiceRequestDetailViewModel — same reasons: the ticket, its thread and the chat
// subscription all have to outlive a rotation.
public class SupportTicketDetailViewModel extends AndroidViewModel {
```

গঠন প্রায় একই — `ticket()`, `messages()`, `chatLive()`, দুই `Event<>`-wrapped (`message`, `apiError`), REST + socket দুই দিক থেকে আসা message id দিয়ে dedupe, নিজের পাঠানো মেসেজ নিজে থেকে থ্রেডে যোগ করা। একটাই অতিরিক্ত জিনিস — `rated` LiveData, resolve হওয়া ticket-এর satisfaction rating দেওয়ার পরে বোতাম লুকানোর জন্য।

**`CreateSupportTicketActivity`-তে একটা লক্ষণীয় পার্থক্য:**

```java
private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
```

এখানে **`MEDIUM`** আছে, `ServiceRequestPriority`-র মতো `NORMAL` না — কারণ ticket priority আলাদা enum (`TicketPriority`), আর as-built doc-এর §16 (Gotchas) স্পষ্ট করে সতর্ক করেছে: "`ServiceRequestPriority` uses `NORMAL`; `TicketPriority` uses `MEDIUM`. Two enums, two vocabularies, one easy 400।" এই দুটো ভিন্ন screen (`CreateServiceRequestActivity` vs `CreateSupportTicketActivity`) এই দুই আলাদা constant array রাখাটাই সঠিক — একটা shared constant বানিয়ে দুই জায়গায় ব্যবহার করলে বরং একটা bug আসার সুযোগ তৈরি হতো।

**`priority` ticket-এ required** (`@NotNull`), অথচ service request-এ optional — কোডে দেখো `priorityIndex` ডিফল্ট `1` (`MEDIUM`) সেট করা আছে, একটা `null` অপশন নেই priority dropdown-এ (`ServiceRequestPriority`-র dropdown-এ যেমন `PRIORITY_VALUES[0] = null` আছে, এখানে নেই) — কারণ ticket priority বাদ দিলে backend সরাসরি 400 দেবে।

**Satisfaction rating** (`promptSatisfaction()` → `submitSatisfaction()`):

```java
// Both query params are required server-side — feedback has no required=false, so an empty
// string is sent rather than omitting it.
public void submitSatisfaction(int rating, String feedback) {
    supportTicketRepository.submitSatisfaction(ticketId, rating, feedback, ...);
}
```

`feedback` optional মনে হলেও query param হিসেবে **সবসময় পাঠানো হয়**, খালি string দিয়ে হলেও — কারণ backend-এ `feedback` param-এর কোনো `required = false` নেই, বাদ দিলে 400 আসবে।

### STOMP Chat Protocol — গভীর বিশ্লেষণ (`ChatSocket.java`, `StompFrame.java`)

অধ্যায় ২.৮-এ এই socket-টার উচ্চ-স্তরের পরিচয় হয়েছিল। এখানে সেটার আসল ব্যবহার-জায়গা — `SupportTicketDetailActivity`/`ServiceRequestDetailActivity`-র মেসেজ থ্রেড — নিয়ে কাজ করছি বলে internal mechanics-টা পুরোপুরি বোঝা দরকার।

**একটা connection, দুই ধরনের destination:**

```
/user/queue/service-requests/{requestId}/messages
/user/queue/support-tickets/{ticketId}/messages
/user/queue/notifications                          (অধ্যায় ৪.১০ — badge-এর জন্য)
```

`ChatSocket` একটাই instance, `AppGraph`-এ owned, পুরো app জুড়ে একটাই WebSocket connection — কিন্তু একসাথে একাধিক destination-এ subscribe থাকতে পারে (`Map<String, List<Subscription>> subscriptions`)। প্রতিটা destination একাধিক `Subscription`ও রাখতে পারে (theoretically একই request দুইবার screen-এ খোলা থাকলে)।

**`subscribe()` — reference-counted, lazy socket open:**

```java
public Subscription subscribe(String destination, Listener listener) {
    Subscription subscription = new Subscription(destination, listener);
    subscriptions.computeIfAbsent(destination, key -> new CopyOnWriteArrayList<>()).add(subscription);
    shuttingDown = false;

    if (webSocket == null) {
        openSocket();                       // প্রথম subscription — socket খোলো
    } else if (connected) {
        sendSubscribeFrame(destination);    // ইতিমধ্যে সংযুক্ত — এখনই SUBSCRIBE frame পাঠাও
    }
    // handshake চলমান থাকলে কিছু করার দরকার নেই — CONNECTED আসলে bulk re-subscribe হবে
    return subscription;
}
```

আর `Subscription.cancel()` চেইন করে `removeSubscription()`-এ যায়, যেটা শেষ subscriber চলে গেলে `UNSUBSCRIBE` পাঠায়, আর **সব destination খালি হয়ে গেলে পুরো socket বন্ধ করে দেয়** (`shutdown()`) — ব্যাটারি বাঁচানোর জন্য, খামোখা একটা idle socket খোলা রাখা হয় না।

**STOMP frame flow — একটা সম্পূর্ণ handshake:**

```
CLIENT → CONNECT\naccept-version:1.2\nhost:localhost\n\n\0
SERVER → CONNECTED\nversion:1.2\nheart-beat:0,0\nuser-name:27\n\n\0
CLIENT → SUBSCRIBE\nid:sub-0\ndestination:/user/queue/support-tickets/13/messages\n\n\0
SERVER → MESSAGE\ndestination:...\ncontent-type:application/json\n\n{"id":1,...}\0
```

`onOpen()`-এ socket খোলার সাথে সাথেই `CONNECT` frame পাঠানো হয়। Server `CONNECTED` ফেরত দিলে `handleFrame()`-এ **bulk re-subscribe** হয়:

```java
if ("CONNECTED".equals(frame.command)) {
    reconnectAttempt = 0;
    setConnected(true);
    for (String destination : subscriptions.keySet()) {
        sendSubscribeFrame(destination);
    }
    return;
}
```

এই `for` লুপ-টাই reconnect logic-এর প্রাণ — connection ভেঙে আবার জুড়লে broker-এর নিজের memory-তে থাকা পুরনো subscription হারিয়ে যায় (নতুন session), তাই socket আবার connect হলে **প্রতিটা এখনো-দরকারি destination-এ আবার নতুন করে SUBSCRIBE পাঠাতে হয়** — নাহলে socket "up" থাকবে কিন্তু কোনো message আসবে না।

**Reconnect backoff:**

```java
private static final long[] RECONNECT_BACKOFF_MS = {1000, 2000, 4000, 8000, 16000, 30000};
```

Exponential backoff, ১ সেকেন্ড থেকে শুরু করে ৩০ সেকেন্ডে গিয়ে থামে (array-এর শেষ index-এ clamp হয়ে যায়)। `onDisconnected()` (OkHttp callback thread থেকে আসে, কিন্তু `mainHandler.post` দিয়ে UI thread-এ hop করানো হয়) `scheduleReconnect()` কল করে যদি এখনো কোনো subscription বাকি থাকে আর ইচ্ছাকৃত shutdown না হয়ে থাকে।

**Duplicate-subscribe race রোধ — `sendSubscribeFrame()`:**

```java
private void sendSubscribeFrame(String destination) {
    ...
    String id = "sub-" + nextSubId.getAndIncrement();
    if (subscriptionIds.putIfAbsent(destination, id) != null) {
        return;    // ইতিমধ্যে subscribe করা আছে
    }
    socket.send(StompFrame.encodeSubscribe(id, destination));
}
```

এই মেথড দুই ভিন্ন thread থেকে আসতে পারে (UI thread থেকে নতুন `subscribe()` কল, আর OkHttp thread থেকে `CONNECTED`-এর পরে bulk re-subscribe) — `ConcurrentHashMap.putIfAbsent()` atomically নিশ্চিত করে একই destination দুইবার SUBSCRIBE না হয়। দুইবার হলে প্রতিটা message দুইবার আসত — যেটা `appendComment()`-এর id-based dedupe দিয়ে ঢাকা পড়ে যেত ঠিকই, কিন্তু এটা root-cause-এ ঠিক করা ভালো।

**Sending — REST, কখনো socket না:**

```java
// ChatSocket.java-র class-লেভেল কমেন্ট
// Receive-only by design: the backend has no @MessageMapping, so sending stays plain REST. Also
// note the server only pushes to the *other* party, never back to the sender's own sessions.
```

Backend-এ কোনো `@MessageMapping` নেই — মানে client socket দিয়ে কিছু পাঠাতেই পারে না, শুধু শুনতে পারে। মেসেজ পাঠানো সবসময় সাধারণ `POST` (comment/support-message API)। আর push শুধু **অন্য পক্ষের** session-এ যায় (`convertAndSendToUser`), sender নিজের কাছে echo ফেরত পায় না — এই কারণেই ViewModel-এর `sendComment()`/`sendMessage()` REST response আসার সাথে সাথেই `appendComment()`/`append()` কল করে নিজের মেসেজ স্থানীয়ভাবে থ্রেডে বসায়।

> **Interview প্রশ্ন: "WebSocket disconnect হয়ে আবার connect হলে আপনি কীভাবে নিশ্চিত করেন client আবার সব message পাবে?"**
> উত্তর: "দুই স্তরে — (১) socket-level: reconnect হলে `CONNECTED` frame পেলে `subscriptions` map-এর সব destination-এ আবার `SUBSCRIBE` পাঠানো হয়, কারণ broker session মরে গেলে পুরনো subscription ভুলে যায়; (২) message-level: প্রতিটা comment/message-এর একটা server-generated `id` থাকে, socket push আর REST refetch-এর মধ্যে duplicate এলে `id` মিলিয়ে replace করা হয়, নতুন করে append না করে। এই দুটো মিলিয়ে reconnect-এর সময় কোনো মেসেজ miss হয় না, আবার duplicate-ও দেখা যায় না।"

> **Interview প্রশ্ন: "আপনার chat-এ sender কি নিজের পাঠানো মেসেজ backend থেকে ফেরত পায়?"**
> উত্তর: "না — এটা এই backend-এর একটা সচেতন ডিজাইন সিদ্ধান্ত, `convertAndSendToUser()` শুধু receiver-কে পাঠায়। আমাদের client তাই REST call-এর success response থেকেই পাওয়া object সরাসরি local thread-এ যোগ করে দেয় — socket থেকে নিজের মেসেজ ফেরত আসার অপেক্ষা করে না।"

### সম্পূর্ণ ফাইল তালিকা — `ui/support/`

| ফাইল | বর্ণনা |
|---|---|
| `CreateSupportTicketActivity.java` | নতুন ticket তৈরি — title/description/category/priority, priority `MEDIUM` ডিফল্ট (৪ মান, `ServiceRequestPriority`-র থেকে ভিন্ন enum) |
| `SupportTicketListActivity.java` | Company/staff-এর নিজের ticket list, `BottomNavActivity` extend করে |
| `SupportTicketAdapter.java` | List item — title, ticket number, priority, `TicketStatusBadge` দিয়ে status |
| `SupportTicketDetailActivity.java` | Ticket detail + message thread + satisfaction rating dialog |
| `SupportTicketDetailViewModel.java` | Detail screen-এর state + chat subscription (`ServiceRequestDetailViewModel`-এর mirror) |
| `SupportMessageAdapter.java` | Message thread-এর RecyclerView adapter — id দিয়ে dedupe |
| `TicketStatusBadge.java` | `TicketStatus` স্ট্রিং থেকে রঙ ও localized label বের করে |

---

## ৮.৩ Knowledge Base (`ui/kb/`)

সবচেয়ে সহজ module — শুধু পড়ার জন্য, কোনো role branching নেই, কোনো ViewModel নেই (কোনো caching-এর দরকার নেই বলে সরাসরি Repository কল)।

`KbArticleListActivity` — search box-এ IME action (`EditorInfo.IME_ACTION_SEARCH` বা Enter কী চাপলে) সরাসরি `loadArticles()` আবার কল করে, keyword-সহ:

```java
binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
    if (actionId == EditorInfo.IME_ACTION_SEARCH
            || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
        loadArticles();
        return true;
    }
    return false;
});
```

`KbArticleDetailActivity`-তে একটা "helpful" বোতাম — `POST /api/kb/articles/{id}/helpful` কল করে, response body দরকার নেই (`ResponseBody` টাইপ, plain success/fail)।

### সম্পূর্ণ ফাইল তালিকা — `ui/kb/`

| ফাইল | বর্ণনা |
|---|---|
| `KbArticleListActivity.java` | Article list + keyword search, `StateView` দিয়ে empty/error handle করে |
| `KbArticleDetailActivity.java` | একটা article-এর পুরো content দেখায়, "helpful" মার্ক করার বোতাম |
| `KbArticleAdapter.java` | List item — title, summary, category |

---

## ৮.৪ Notice Board (`ui/noticeboard/`)

`NoticeBoardActivity` একটাই screen-এ দুটো **স্বাধীন** list দেখায় — company announcement (vertical RecyclerView) আর এই বছরের holiday calendar (horizontal RecyclerView, strip আকারে)। Activity-র নিজের কমেন্টেই ব্যাখ্যা আছে কেন ViewModel নেই: "both read-only, no ViewModel needed (same reasoning as `PayslipListActivity`)" — সহজ, non-critical, rarely-visited read-only স্ক্রিনে ViewModel বাধ্যতামূলক না করার সেই একই নীতি (অধ্যায় ২.৩)।

দুটো list **আলাদা আলাদা** load হয় (`loadAnnouncements()`, `loadHolidays()`), একটা fail করলে আরেকটা impact হয় না:

```java
private void loadHolidays() {
    repository.getCurrentYearHolidays(new Callback<List<HolidayResponse>>() {
        @Override
        public void onResponse(...) {
            if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                return;   // চুপচাপ — holiday strip শুধু secondary content
            }
            holidayAdapter.submitList(response.body());
            binding.holidaysLabel.setVisibility(View.VISIBLE);
            binding.holidaysRecyclerView.setVisibility(View.VISIBLE);
        }
        @Override
        public void onFailure(...) { /* strip hidden-ই থেকে যায় */ }
    });
}
```

Holiday list-এর label ও RecyclerView **ডিফল্টভাবে GONE** — শুধু ডেটা এলেই দেখানো হয় (empty বছরে holiday না থাকলে একটা খালি "Holidays" heading দেখানো এড়ানো)। এটা announcement-এর মূল feed-কে primary content হিসেবে রেখে, holiday strip-কে একটা optional bonus হিসেবে treat করার সিদ্ধান্ত।

একটা announcement-এ tap করলে পুরো body একটা simple `MaterialAlertDialogBuilder` dialog-এ দেখানো হয় — এখানে আলাদা detail screen বানানো হয়নি, কারণ announcement সাধারণত ছোট টেক্সট, dialog-ই যথেষ্ট।

### সম্পূর্ণ ফাইল তালিকা — `ui/noticeboard/`

| ফাইল | বর্ণনা |
|---|---|
| `NoticeBoardActivity.java` | Announcement feed + holiday strip, দুটো independent load, ViewModel ছাড়া |
| `AnnouncementAdapter.java` | List item — title, body, published date; tap করলে caller-এর dialog callback ফায়ার করে |
| `HolidayAdapter.java` | Horizontal strip item — তারিখ + নাম |

---

## ৮.৫ Notification (`ui/notification/`)

দুটো screen: notification centre (list) আর preferences (settings)।

`NotificationListViewModel` — `CachedListViewModel<NotificationResponse>`-এর subclass, ঠিক `ServiceRequestListViewModel`-এর প্যাটার্নে:

```java
public class NotificationListViewModel extends CachedListViewModel<NotificationResponse> {
    @Override
    protected void fetch(Callback<PageResponse<NotificationResponse>> callback) {
        repository.getNotifications(false, callback);   // false = unreadOnly না, সব দেখাও
    }
    ...
}
```

**Mark-as-read logic** — দুটো ভিন্ন strategy লক্ষ্য করার মতো:

```java
public void markRead(Long id) {
    repository.markRead(id, new Callback<ResponseBody>() {
        @Override
        public void onResponse(...) { refresh(); }
        @Override
        public void onFailure(...) {
            // The user has already been taken to the notification's content; a failed
            // read-receipt isn't worth interrupting them for.
        }
    });
}

public void markAllRead() {
    repository.markAllRead(new Callback<ResponseBody>() {
        @Override
        public void onResponse(...) { refresh(); }
        @Override
        public void onFailure(...) { refresh(); }   // ব্যর্থ হলেও refresh — server state যাই থাকুক দেখাও
    });
}
```

`markRead()` ব্যর্থ হলে চুপ থাকে (user ইতিমধ্যে notification-এর content-এ চলে গেছে, একটা silent read-receipt failure নিয়ে বিরক্ত করার দরকার নেই), কিন্তু `markAllRead()` ব্যর্থ হলেও `refresh()` কল করে — কারণ "সব read মার্ক করো" একটা bulk explicit action, user সরাসরি এটার ফলাফল দেখতে চায়, তাই server-এ যা অবস্থা আছে সেটাই আবার টেনে এনে দেখানো ভালো, চুপ থাকার চেয়ে।

**Preferences — ৯টা fixed boolean, per-type toggle নেই:**

as-built doc §5.7-এর ⚠ পয়েন্ট: verbs হলো `GET`/`PUT`/`DELETE` (`PATCH` না), আর `UpdateNotificationPreferenceRequest`-এ কোনো "প্রতিটা `NotificationType`-এর জন্য আলাদা toggle" নেই — মাত্র ৯টা fixed boolean field:

```java
binding.switchEmailServiceRequest.setChecked(prefs.isEmailOnServiceRequest());
binding.switchEmailStatusChange.setChecked(prefs.isEmailOnStatusChange());
binding.switchEmailInvoice.setChecked(prefs.isEmailOnInvoice());
binding.switchEmailPayment.setChecked(prefs.isEmailOnPayment());
binding.switchEmailTaskAssigned.setChecked(prefs.isEmailOnTaskAssigned());
binding.switchEmailLeaveUpdate.setChecked(prefs.isEmailOnLeaveUpdate());
binding.switchInAppServiceRequest.setChecked(prefs.isInAppOnServiceRequest());
binding.switchInAppStatusChange.setChecked(prefs.isInAppOnStatusChange());
binding.switchEmailMarketing.setChecked(prefs.isEmailMarketing());
```

`NotificationType`-এর ৩২টা মান আছে (অধ্যায় ৮-এর প্রেক্ষাপটে না হলেও as-built §8-এ উল্লেখ), কিন্তু preferences screen সেগুলোর প্রতিটার জন্য আলাদা toggle দেয় না — API-টাই এভাবে বানানো (কতগুলো coarse category-র উপর email/in-app on-off)। এটা এমন একটা জায়গা যেখানে UI-টা API-র সীমাবদ্ধতা অনুযায়ী বানাতে হয়েছে, "যেমনটা চাইতাম" তেমনটা না — একটা বাস্তব software-engineering trade-off।

### সম্পূর্ণ ফাইল তালিকা — `ui/notification/`

| ফাইল | বর্ণনা |
|---|---|
| `NotificationListActivity.java` | Notification centre — list + mark-all-read বোতাম |
| `NotificationListViewModel.java` | `CachedListViewModel<NotificationResponse>` subclass, markRead/markAllRead |
| `NotificationAdapter.java` | List item — title, message, date, unread dot indicator |
| `NotificationPreferencesActivity.java` | ৯টা fixed boolean toggle লোড/সেভ করে, ViewModel ছাড়া |

---

## ৮.৬ এই অধ্যায়ের ইন্টারভিউ-প্রশ্ন সারাংশ

এই অধ্যায়ে যা যা কভার হলো, সেখান থেকে সবচেয়ে গুরুত্বপূর্ণ প্রশ্নগুলো এক জায়গায়:

1. **"Dynamic form কীভাবে বানালেন, কেন কোড দিয়ে XML দিয়ে না?"** — `DynamicFormRenderer`, field-set runtime-এ জানা যায় বলে।
2. **"একই স্ক্রিন দুই role-এর জন্য কীভাবে সামলালেন?"** — `ServiceRequestDetailActivity` + `viewModel.isStaff()` branching, একটাই Activity/ViewModel, শুধু button visibility বদলায়।
3. **"একটা real bug বলুন যেটা আপনি ধরেছেন।"** — Add Comment visibility bug: staff comment default `INTERNAL`, client দেখতে পায় না; সমাধান `fromStaff()`/`fromClient()` দুটো named factory method দিয়ে compile-time-এ ভুল আটকানো।
4. **"WebSocket reconnect হলে message miss/duplicate কীভাবে আটকান?"** — Socket-level bulk re-subscribe on `CONNECTED` + message-level id-based dedupe।
5. **"Sender কি নিজের পাঠানো মেসেজ socket থেকে ফেরত পায়?"** — না, `convertAndSendToUser()` শুধু receiver-কে পাঠায়; sender REST response থেকে নিজে local thread-এ যোগ করে।
6. **"কোন screen-এ ViewModel দরকার, কোনটাতে না?"** — caching/repeat-visit জটিল হলে (client request list, notification list) ViewModel; simple/rarely-visited/always-fresh-দরকার (staff list, KB, notice board, preferences) হলে সরাসরি Repository।
7. **"দুটো ভিন্ন enum, priority-তে `NORMAL` বনাম `MEDIUM` — কীভাবে গুলিয়ে ফেলা এড়ালেন?"** — `ServiceRequestPriority` vs `TicketPriority`, দুই আলাদা constant array দুই আলাদা Create screen-এ, কোনো shared constant না বানিয়ে।
8. **"Status timeline-এ কী এক্সট্রা তথ্য এক্সপোজ হচ্ছে, আর সেটা কি ঠিক আছে?"** — `reason`/`changedByName` (staff-লিখিত free text) client-কে দেখানো হচ্ছে; আগে staff-only ছিল, এখন খোলা — একটা সচেতন product decision হিসেবে চিহ্নিত।

পরবর্তী অধ্যায়ে আমরা বাকি থাকা module-গুলো (invoice/payment, catalog/package, profile) নিয়ে আলোচনা করব।
