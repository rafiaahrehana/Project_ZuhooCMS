# অধ্যায় ৭ — Finance ও Billing: Invoice, Payment, Wallet, Catalog

এই অধ্যায়ে আমরা অ্যাপের **টাকা-পয়সা সংক্রান্ত** চারটা module দেখব — যেখানে ভুল হলে সরাসরি ব্যবহারকারীর ক্ষতি হয়, তাই এখানকার প্রতিটা design decision ইচ্ছাকৃত এবং গুরুত্বপূর্ণ। ইন্টারভিউয়ের জন্য এই অধ্যায়টাই সবচেয়ে বেশি "সিনিয়র thinking" দেখানোর জায়গা — বিশেষ করে `ui/payment/` (SSLCommerz)।

চারটা প্যাকেজ:
- `ui/invoice/` — client কী invoice পেয়েছে, কত বাকি, PDF নামানো, payment শুরু করা।
- `ui/payment/` — আসল টাকা লেনদেন (SSLCommerz gateway) + receipt/subscription তালিকা।
- `ui/wallet/` — client-এর wallet balance আর transaction history (শুধু view, top-up company-level, এই অ্যাপে না)।
- `ui/catalog/` — company কী কী service/package বিক্রি করে সেটা browse ও subscribe করা।

## ৭.১ Invoice মডিউল (ui/invoice/)

**বাস্তব সমস্যা:** client-কে দেখাতে হবে তার কোন invoice আছে, কতটা বাকি (balance), আর সে চাইলে সরাসরি অ্যাপ থেকে টাকা দিতে পারবে ও PDF নামাতে পারবে। কিন্তু "Pay Now" চাপার পর ব্রাউজার/WebView থেকে ফিরে এসে বলা "টাকা গেছে" — এটা বিশ্বাস করা যায় না (§৭.২ দেখো), তাই এই module-এই সেই "না-বিশ্বাস-করে-আবার-যাচাই" করার লজিক থাকে।

### `InvoiceDetailViewModel.java` — সবচেয়ে গুরুত্বপূর্ণ ফাইল

এই ViewModel দুটো জিনিস rotation-safe রাখে যেটা আগে (comment অনুযায়ী) Activity-তে থাকত — invoice data, আর payment-confirmation poll। rotation-এর মাঝে poll চলতে থাকলে আগে হয় result হারিয়ে যেত, নয়তো destroyed view binding-এ write হতো — crash।

```java
private static final long[] CONFIRM_BACKOFF_MS = {1000, 2000, 4000, 8000};
public enum ConfirmState { IDLE, CONFIRMING, CONFIRMED, UNCONFIRMED }
```

**`confirmPayment()` — payment redirect আসার পর যা হয়:**

```java
public void confirmPayment() {
    InvoiceSummary current = invoice.getValue();
    balanceBeforePayment = current != null ? current.getBalanceAmount() : null;
    confirmState.setValue(ConfirmState.CONFIRMING);
    poll(0);
}
```

লক্ষ করো — payment গেটওয়ে থেকে ফিরে এসেই invoice-কে "PAID" ধরে নেওয়া হচ্ছে না। বরং payment-এর **আগের balance** টা মনে রাখা হচ্ছে, তারপর `poll()` invoice-টা আবার আবার fetch করে দেখে balance বদলেছে কিনা:

```java
private boolean settled(InvoiceSummary latest) {
    return balanceBeforePayment == null
            || latest.getBalanceAmount() == null
            || balanceBeforePayment.compareTo(latest.getBalanceAmount()) != 0;
}
```

`BigDecimal.compareTo()` ব্যবহার হয়েছে, `equals()` না — কারণ `equals()` scale-ও মেলাতে চায় (`5.00` আর `5.0` কে অসমান বলবে), কিন্তু `compareTo()` শুধু মান তুলনা করে। টাকার তুলনায় এটাই সঠিক।

**`scheduleOrGiveUp()` — exponential backoff-এর বাস্তবায়ন:**

```java
private void scheduleOrGiveUp(int attempt) {
    if (attempt >= CONFIRM_BACKOFF_MS.length) {
        confirmState.setValue(ConfirmState.UNCONFIRMED);   // "failed" না!
        return;
    }
    pendingPoll = () -> poll(attempt + 1);
    handler.postDelayed(pendingPoll, CONFIRM_BACKOFF_MS[attempt]);
}
```

১s, ২s, ৪s, ৮s — মোট ~১৫ সেকেন্ড ধৈর্য ধরে balance বদলায় কিনা দেখে (কেন এত সময়, বিস্তারিত §৭.২-এ)। ১৫ সেকেন্ড পরেও settle না হলে **`UNCONFIRMED`** state — `FAILED` না। কারণ comment-এ স্পষ্ট লেখা: "Not a failed payment — an unconfirmed one... reporting 'failed' here would be a lie"।

**`onCleared()`-এ pending poll বাতিল করা:**
```java
@Override
protected void onCleared() {
    super.onCleared();
    if (pendingPoll != null) handler.removeCallbacks(pendingPoll);
}
```
ViewModel destroy হয়ে গেলে (screen পুরোপুরি বন্ধ) বাকি সময়ের জন্য schedule করা poll callback বাতিল করা দরকার — নাহলে destroyed ViewModel-এর data-তে write করার চেষ্টা হতো (memory leak-এর সম্ভাবনাও)।

> **Interview প্রশ্ন: "money-এর জন্য `BigDecimal` কেন, `double`/`float` কেন না?"**
> `double`/`float` binary floating-point — `0.1 + 0.2 == 0.30000000000000004` এই ধরনের rounding error দেয়, কারণ `0.1` বা `0.2`-এর মতো decimal fraction বাইনারিতে ঠিকঠাক represent করা যায় না। টাকার হিসাবে এই সামান্য error জমতে জমতে বড় গরমিল তৈরি করতে পারে (এবং audit-এ ধরাও পড়ে না সহজে)। `BigDecimal` arbitrary-precision decimal arithmetic করে, তাই `0.1 + 0.2` ঠিক `0.3` হয়। পুরো প্রজেক্টে `InvoiceSummary`, `WalletResponse`, `SubscriptionSummary` — সব amount field `BigDecimal`।

### `InvoiceStatusBadge.java`

Backend enum `InvoiceStatus` (DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED, VOIDED, REFUNDED)-কে রং আর localized লেবেলে ম্যাপ করে। এটা প্রজেক্টের অন্য সব `*StatusBadge` ক্লাসের (LeaveRequestStatusBadge, `SubscriptionStatusBadge` ইত্যাদি) মতোই একই প্যাটার্ন: `colorFor()` আর `labelFor()`, দুটোই static, কোনো instance নেই (private constructor)।

```java
public static String labelFor(Context context, String status) {
    if (status == null) return "";
    switch (status) {
        case InvoiceStatus.PAID: return context.getString(R.string.invoice_status_paid);
        ...
        default: return status;   // অজানা status raw constant-ই দেখায়, খালি না রেখে
    }
}
```

এই class-গুলো নিজে UI আঁকে না — `StatusBadgeView.bind()` (নতুন redesign-এ যোগ হওয়া `ui/common/` shared renderer) কে রং+লেবেল পাস করে দেয়, যেটা soft-tint pill + status অনুযায়ী icon বসায়। `iconFor()` মেথডে যেটা লক্ষণীয়: warning রং মানেই clock icon, কারণ প্রজেক্টে `status_warning` সবসময় "pending" গোত্রের অবস্থার জন্যই ব্যবহার হয়, সত্যিকারের সতর্কতার জন্য না।

### `InvoiceDetailActivity.java` — payment ফলাফল হ্যান্ডল করা

```java
paymentLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), this::onPaymentResult);
```

`PaymentActivity`-কে `startActivity()` দিয়ে না খুলে **`ActivityResultLauncher`** দিয়ে খোলা হয়, কারণ ফিরে আসার পর একটা result দরকার (RESULT_OK বা RESULT_CANCELED + gateway status)।

```java
private void onPaymentResult(ActivityResult result) {
    if (result.getResultCode() != RESULT_OK) {
        String status = ...getStringExtra(PaymentActivity.EXTRA_GATEWAY_STATUS);
        Toast.makeText(this, ...);
        // Still re-read the invoice — SSLCommerz's IPN is independent of the browser
        // redirect and may have settled it anyway.
        viewModel.reload();
        return;
    }
    viewModel.confirmPayment();
}
```

এখানেও লক্ষ করো — RESULT_CANCELED হলেও (মানে গেটওয়ে "success" বলেনি) invoice-টা আবার **reload** করা হচ্ছে, শুধু toast দেখিয়ে ছেড়ে দেওয়া হচ্ছে না। কারণ ব্যাখ্যা কমেন্টেই আছে: IPN (Instant Payment Notification) ব্রাউজার redirect থেকে independent — WebView "FAILED" দেখালেও backend-এ IPN পরে এসে পেমেন্ট আসলে settle করে দিতে পারে।

**Screenshot protection:**
```java
getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
```
টাকার অঙ্ক স্ক্রিনে থাকা অবস্থায় স্ক্রিনশট বা recent-apps thumbnail-এ যেন না দেখা যায়। এই একই কাজ `SecureScreen.apply(activity)` হেল্পার class দিয়েও করা হয় অন্য কয়েকটা জায়গায় (§৭.২, ৭.৪) — `InvoiceDetailActivity` নিজে raw flag সেট করে, বাকিরা shared helper ব্যবহার করে (সামান্য অসামঞ্জস্য, কিন্তু ফলাফল একই)।

**PDF ডাউনলোড ও খোলা (`savePdfAndOpen` → `writePdfToCache` → `openPdf`):** response body-র raw bytes cache dir-এ লেখা হয়, তারপর **`FileProvider`** দিয়ে একটা `content://` URI বানানো হয় (raw `file://` URI Android N+ থেকে `FileUriExposedException` ছোড়ে) এবং `ACTION_VIEW` intent দিয়ে বাইরের PDF viewer-এ পাঠানো হয়। কোনো PDF viewer না থাকলে `ActivityNotFoundException` catch করে বন্ধুত্বপূর্ণ error দেখানো হয়।

### Invoice প্যাকেজ — সম্পূর্ণ ফাইল তালিকা

| ফাইল | বর্ণনা |
|---|---|
| `InvoiceDetailViewModel.java` | Invoice load, PDF download, এবং payment-confirmation-এর exponential-backoff poll (rotation-safe)। |
| `InvoiceListViewModel.java` | `CachedListViewModel<InvoiceSummary>`-এর subclass — cache-first invoice তালিকা (অধ্যায় ৪.৫ দেখো)। |
| `InvoiceStatusBadge.java` | `InvoiceStatus` enum-কে রং ও localized লেবেলে ম্যাপ করে (PAID=সবুজ, OVERDUE/CANCELLED/VOIDED=লাল, DRAFT/ISSUED/PARTIALLY_PAID=কমলা)। |
| `InvoiceAdapter.java` | Invoice list-এর RecyclerView adapter — নম্বর, সার্ভিস টাইটেল, status badge, বাকি টাকা দেখায়। |
| `InvoiceListActivity.java` | Invoice list স্ক্রিন — `StateView` দিয়ে loading/empty/error, `CacheStamp` দিয়ে "cached" লেবেল, receipts-এ shortcut বাটন। |
| `InvoiceDetailActivity.java` | একটা invoice-এর বিস্তারিত (line items, subtotal/tax/total/paid/balance), Pay Now, PDF download+open, payment result হ্যান্ডলিং। |

## ৭.২ Payment মডিউল (ui/payment/) — SSLCommerz Gateway গভীরভাবে

**বাস্তব সমস্যা:** client টাকা দিতে চায় — invoice-এর বাকি বা package subscription-এর দাম — এবং সেটা একটা তৃতীয়-পক্ষ gateway (SSLCommerz, বাংলাদেশের জনপ্রিয় payment gateway) দিয়ে করতে হবে, যেটার ফলাফল app নিজে নিয়ন্ত্রণ করে না। এটাই এই গোটা প্রজেক্টের সবচেয়ে ঝুঁকিপূর্ণ অংশ, কারণ ভুল হলে সরাসরি টাকার হিসাবে গরমিল হতে পারে।

### `PaymentActivity.java` — লাইন ধরে ধরে

**ধাপ ১ — কেন WebView, Custom Tabs না:**

```java
private void setUpWebView() {
    WebSettings settings = binding.webView.getSettings();
    settings.setJavaScriptEnabled(true);       // গেটওয়ের UI JS দিয়ে চলে
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);         // sandbox — local file access বন্ধ
    settings.setAllowContentAccess(false);
    ...
}
```

as-built doc অনুযায়ী: **"Open `gatewayUrl` in an in-app WebView — not Custom Tabs, which can't intercept navigation before it loads."** Chrome Custom Tabs UX-এর দিক থেকে ভালো (browser-এর নিজের UI, ফিরে আসতে সহজ), কিন্তু Custom Tabs-এ navigation intercept করার কোনো hook নেই — payment gateway যখন `/payment-result?status=...` তে redirect করে, সেই URL-টা browser নিজে খুলে ফেলবে, app জানতেই পারবে না কী হলো। WebView-তে `WebViewClient.shouldOverrideUrlLoading()` override করে সেই redirect-টাকে **browser-এ পৌঁছানোর আগেই** ধরে ফেলা যায়। তাই এখানে UX-এর কিছুটা compromise (in-app WebView কম polished) করেই control-টা রাখা হয়েছে।

**ধাপ ২ — navigation intercept ও host allowlist:**

```java
@Override
public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    Uri uri = request.getUrl();
    if (uri.toString().contains("/payment-result")) {
        finishWithGatewayResult(uri);
        return true;   // WebView-কে এই URL লোড করতে না দাও, নিজে হ্যান্ডল করো
    }
    if (!isAllowedHost(uri.getHost())) {
        return true;   // অজানা host — লোড করতে দিও না
    }
    return false;       // চেনা host, WebView স্বাভাবিকভাবে লোড করুক
}
```

```java
private boolean isAllowedHost(@Nullable String host) {
    if (host == null) return false;
    return host.endsWith("sslcommerz.com") || host.equals(apiHost());
}
```

এখানে দুটো জিনিস গুরুত্বপূর্ণ: (১) gateway payment flow-এর মধ্যে bKash/Nagad/card-issuer-এর মতো ৩য়-পক্ষ redirect আসতে পারে সেটা `sslcommerz.com` allowlist-এ পড়ে যায় (SSLCommerz নিজেই সেসব সাব-ডোমেইনে redirect করায়); (২) backend নিজের API host-ও allow করা আছে কারণ ব্যাক-এন্ডের callback URL সেই হোস্টেই বানানো হয়। এর বাইরের যেকোনো host-এ navigation **আটকে দেওয়া হয়** — এটা একটা basic কিন্তু কার্যকর navigation-hijacking প্রতিরোধ।

**ধাপ ৩ — `status` param আসলে কী বোঝায় (সবচেয়ে সূক্ষ্ম অংশ):**

```java
private static final String STATUS_SUCCESS = "SUCCESS";

private void finishWithGatewayResult(Uri uri) {
    String status = uri.getQueryParameter("status");
    if (STATUS_SUCCESS.equals(status)) {
        setResult(RESULT_OK);   // এখনো শুধু একটা ইঙ্গিত মাত্র!
    } else {
        setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_GATEWAY_STATUS, status));
    }
    finish();
}
```

as-built doc-এর সবচেয়ে গুরুত্বপূর্ণ সতর্কতা: **`status` আসলে backend-এর `GatewayTransactionStatus` enum-এর নাম, "success" শব্দটা নয়।** এই query param-এ যা আসতে পারে তার একটা মান হলো `VALIDATION_FAILED` — আর এটা **"success" callback path দিয়েই আসতে পারে**! কারণটা টাইমিং-এ: SSLCommerz প্রথমে ব্যবহারকারীকে ব্রাউজারে ফেরত পাঠায় (redirect), আর তারপর backend-এর সাথে server-to-server validation চালায় — এই দুটো ঘটনার মধ্যে সময়ের ফাঁক থাকে। ব্রাউজার redirect-এর মুহূর্তে backend হয়তো এখনো transaction validate করেনি, কিন্তু frontend URL-টা ইতিমধ্যে "success-shaped" (যেমন `/payment-result?status=SUCCESS` তৈরিই হয় optimistically) হয়ে গেছে অথবা backend সেই মুহূর্তে যা জানে সেটাই পাঠিয়ে দেয় — validation পরে fail করলে সেটা IPN দিয়ে backend-এর নিজস্ব রেকর্ডে প্রতিফলিত হয়, WebView-এর URL-এ না। তাই কোডে explicit করে বলা আছে: **শুধু `SUCCESS` স্ট্রিং worth waiting on** — সেটাও শুধু "caller এবার poll শুরু করবে" এই ইঙ্গিত, নিশ্চিত প্রমাণ না।

> **Interview প্রশ্ন: "payment gateway redirect-কে কেন সরাসরি বিশ্বাস করা উচিত না?"**
> কারণ redirect URL client-side data — এটা (ক) network-এ intercept/tamper করা যেতে পারে, (খ) gateway নিজে optimistic ভাবে "success" পাঠাতে পারে যেটা পরে server-side validation-এ বাতিল হয় (এই প্রজেক্টেই `VALIDATION_FAILED` উদাহরণ), এবং (গ) IPN (server-to-server webhook) ব্রাউজার redirect-এর থেকে **স্বাধীন ও ভিন্ন সময়ে** আসে — কখনো redirect-এর আগে, কখনো পরে। তাই client-এর "সত্যের উৎস" (source of truth) হওয়া উচিত backend-এর নিজের record — অর্থাৎ invoice/subscription-কে আবার fetch করে দেখা, redirect-এর status field না।

**ধাপ ৪ — কেন re-fetch করা হয় exponential backoff দিয়ে (১s/২s/৪s/৮s):**

এই লজিকটা `PaymentActivity`-তে নেই — `PaymentActivity` শুধু `RESULT_OK`/`RESULT_CANCELED` ফেরত দেয়, actual polling হয় caller-এ (§৭.১-এ দেখা `InvoiceDetailViewModel.confirmPayment()`)। as-built doc বলছে: **"The original's 'retry once or twice' is not enough: the IPN commonly lands after the browser returns."** সোজা কথায় — একবার-দুবার তাড়াতাড়ি চেক করলে প্রায়ই দেখা যাবে ব্যালেন্স তখনো বদলায়নি (কারণ IPN তখনো backend-এ পৌঁছায়নি), এবং app ভুলভাবে "payment failed" দেখিয়ে দেবে যদিও টাকা আসলে কেটে গেছে এবং কিছুক্ষণ পরেই settle হবে। তাই fixed interval-এ না গিয়ে **exponential backoff** — প্রথমে দ্রুত চেক করা (১s, দ্রুত settle হলে তাড়াতাড়ি জানানো যায়), তারপর ধীরে ধীরে অপেক্ষার সময় বাড়ানো (২s, ৪s, ৮s — মোট ~১৫s), যাতে ধীর IPN-কেও যথেষ্ট সময় দেওয়া যায় অথচ দ্রুত settle হওয়া case-এ ব্যবহারকারীকে বেশিক্ষণ বসিয়ে না রাখা হয়। ১৫s পরেও settle না হলে `UNCONFIRMED` — "এখনো নিশ্চিত হচ্ছে, একটু পর আবার খুলে দেখুন" — কখনো "failed" বলা হয় না, কারণ সেটা মিথ্যা হতে পারে।

> **Interview প্রশ্ন: "fixed-interval polling-এর বদলে exponential backoff কেন বেছে নিলেন?"**
> Fixed interval (যেমন প্রতি ২s) একটা trade-off জোর করে বেছে নেয় — খুব ঘন ঘন হলে সার্ভারে অকারণ লোড, খুব দূরে দূরে হলে দ্রুত settle হওয়া payment-এও ব্যবহারকারীকে বেশিক্ষণ অপেক্ষা করাতে হয়। Exponential backoff দুটোই পায়: শুরুতে দ্রুত (common case দ্রুত resolve করে), তারপর ধীরে ধীরে ব্যবধান বাড়িয়ে (worst-case-এও সার্ভারে চাপ না বাড়িয়ে) মোট একটা যুক্তিসঙ্গত সময়ের (এখানে ~১৫s) মধ্যে হাল ছেড়ে একটা honest "এখনো নিশ্চিত হয়নি" state দেখায়।

**ধাপ ৫ — কেন ভবিষ্যতে custom URI scheme না, Android App Links:**

এই প্রজেক্টে এখন কোনো deep-link close-out নেই (WebView-এর মধ্যেই সব হ্যান্ডল হয়), কিন্তু as-built doc ভবিষ্যতের জন্য একটা সতর্কতা রেখে দিয়েছে: **"A custom scheme is not exclusive — another installed app can register it and race to intercept the payment callback with a spoofed status."** ব্যাখ্যা: যদি কখনো `zuhoo://payment-result?status=SUCCESS` স্টাইলের custom URI scheme দিয়ে deep-link করা হয়, তাহলে Android-এ **যেকোনো ইনস্টলড app** সেই একই scheme (`zuhoo://`) নিজের `AndroidManifest.xml`-এ intent-filter হিসেবে ঘোষণা করতে পারে — scheme registration exclusive না। একটা ম্যালিশাস app যদি একই scheme claim করে, তাহলে সত্যিকারের redirect আসার সময় Android একটা disambiguation dialog দেখাতে পারে (বা কিছু ক্ষেত্রে race করে ম্যালিশাস app-ই জিতে যেতে পারে), আর সেই app স্পুফড status URI নিজে খুলে ব্যবহারকারীকে ভুল বোঝাতে পারে অথবা sensitive query param চুরি করতে পারে।

**সমাধান — Android App Links:** `https://` scheme-এর সাথে ডোমেইনের মালিকানা প্রমাণ করতে হয় `assetlinks.json` ফাইল দিয়ে (ডোমেইনের `.well-known/` path-এ হোস্ট করা, digital signature verify করে)। যেহেতু কোনো অন্য app সেই ভেরিফায়েড ডোমেইন claim করতে পারবে না, App Link-টা exclusively শুধু এই app-এই খোলে (verified হলে Android কোনো disambiguation dialog-ই দেখায় না, সরাসরি app খোলে)।

> **Interview প্রশ্ন: "custom URI scheme (`myapp://...`) আর Android App Links-এর মধ্যে security-এর দিক থেকে পার্থক্য কী?"**
> Custom scheme registration non-exclusive — যেকোনো app manifest-এ একই scheme claim করতে পারে, ফলে intent hijacking/spoofing সম্ভব। App Links একটা verified HTTPS domain-এর সাথে bind থাকে (`assetlinks.json` দিয়ে ownership প্রমাণ করতে হয়, Google Play-ও install-time-এ এটা verify করে), তাই শুধু সেই domain-এর প্রকৃত মালিকই সেই link handle করতে পারে — অন্য কোনো app claim করলেও Android সেটাকে ignore করে। Payment callback-এর মতো sensitive flow-এ তাই App Links নিরাপদ পছন্দ।

**ধাপ ৬ — Screenshot protection ও initiate call:**

```java
getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
...
paymentRepository.initiate(purpose, targetId, amount, callback);
// সফল হলে: binding.webView.loadUrl(response.body().get("gatewayUrl"));
```

`PaymentRepository.initiate()` ব্যাক-এন্ডকে `POST /api/payments/sslcommerz/initiate` কল করে `{purpose, targetId, amount}` পাঠায় ও ফেরত পাওয়া `gatewayUrl`-টা WebView-তে লোড করে। `purpose` দুটো মান নেয় — `PURPOSE_INVOICE` আর `PURPOSE_PACKAGE_SUBSCRIPTION` (কোম্পানি-লেভেল `WALLET_TOPUP`/`PLATFORM_SUBSCRIPTION` এই client অ্যাপে ব্যবহৃত হয় না, সেগুলো ওয়েব পোর্টালের staff-side-এর জন্য)।

### বাকি payment ফাইলগুলো

- **`SubscriptionListActivity.java`** — client-এর সব package subscription দেখায়। লক্ষণীয়: `onFailure`/error path-এ `stateView.showContent()` কল করে (error দেখায় না) কারণ list ইতিমধ্যে populate থাকতে পারে (payment থেকে ফিরে এসে refresh ব্যর্থ হলেও পুরনো valid তালিকাটাই থাকুক) — অধ্যায় ৪.৫-এর `CachedListViewModel`-এর মূলনীতিরই একটা manual প্রতিফলন, যদিও এই স্ক্রিনটা `CachedListViewModel` extend করে না (repository সরাসরি Activity থেকে ডাকা হয়)।
- **`SubscriptionAdapter.java`** — pending-payment subscription-এ "Pay Now" বোতাম দেখায়, active/pending-এ "Cancel" বোতাম।
- **`PaymentReceiptListActivity.java`** / **`PaymentReceiptAdapter.java`** — সফল payment-এর রসিদ (receipt) তালিকা, শুধু read-only।

### Payment প্যাকেজ — সম্পূর্ণ ফাইল তালিকা

| ফাইল | বর্ণনা |
|---|---|
| `PaymentActivity.java` | SSLCommerz gateway WebView-তে খোলে, navigation intercept করে `/payment-result` ধরে, `status` param পড়ে `RESULT_OK`/`RESULT_CANCELED` ফেরত দেয়। এই প্যাকেজের সবচেয়ে জটিল ফাইল। |
| `PaymentReceiptAdapter.java` | Receipt list-এর RecyclerView adapter — রসিদ নম্বর, invoice নম্বর, টাকার অঙ্ক, method/date/status। |
| `SubscriptionStatusBadge.java` | `SubscriptionStatus` enum (ACTIVE, PENDING_PAYMENT, EXPIRED, SUSPENDED, CANCELLED)-কে রং ও localized লেবেলে ম্যাপ করে। |
| `PaymentReceiptListActivity.java` | ব্যবহারকারীর সব payment receipt-এর তালিকা, `StateView` দিয়ে loading/empty/error। |
| `SubscriptionAdapter.java` | Subscription list-এর RecyclerView adapter — package নাম, status badge, দাম, quota usage, Pay Now/Cancel বোতাম। |
| `SubscriptionListActivity.java` | Client-এর সব package subscription — Pay Now (→PaymentActivity), Cancel (confirmation dialog সহ)। |

## ৭.৩ Wallet মডিউল (ui/wallet/)

**বাস্তব সমস্যা:** এই client অ্যাপে wallet একটা **read-only view** — client নিজে টাকা top-up করতে পারে না (সেটা company/staff-side ফিচার, `WALLET_TOPUP` purpose ওয়েব পোর্টালে), কিন্তু client-কে জানতে হয় তার wallet balance আর credit balance কত এবং transaction history কী।

### `WalletActivity.java`

দুটো আলাদা API কল সমান্তরালে চলে — `loadWallet()` (balance summary) আর `loadTransactions()` (history)। এটা `ui/catalog/CatalogActivity`-র মতোই dual-fetch প্যাটার্ন, কিন্তু catalog-এ দুটো fetch-এর ফলাফল মিলিয়ে row বানাতে হয় (তাই `tryBuildRows()` guard লাগে); wallet-এ দুটো সম্পূর্ণ independent — একটা fail করলে অন্যটাকে থামাতে হয় না।

```java
WalletResponse wallet = response.body();
binding.totalAvailableText.setText(wallet.getCurrency() + " " + wallet.getTotalAvailable());
binding.balanceText.setText(wallet.getCurrency() + " " + wallet.getBalance());
binding.creditBalanceText.setText(wallet.getCurrency() + " " + wallet.getCreditBalance());
```

তিনটা আলাদা অঙ্ক দেখানো হয় — `totalAvailable` (মোট ব্যবহারযোগ্য), `balance` (নিজের জমা করা টাকা), `creditBalance` (company থেকে দেওয়া credit, সম্ভবত promotional/adjustment) — এই তিনটা যে আলাদা জিনিস সেটা backend model-এর ডিজাইন প্রতিফলিত করে, UI শুধু as-is দেখায়।

### `WalletTransactionAdapter.java`

```java
BigDecimal amount = transaction.getAmount();
boolean negative = amount != null && amount.signum() < 0;
binding.itemAmount.setText(String.valueOf(amount));
binding.itemAmount.setTextColor(ContextCompat.getColor(context,
        negative ? R.color.status_danger : R.color.status_success));
```

`BigDecimal.signum()` — `-1`/`0`/`1` ফেরত দেয় তুলনার (`compareTo(BigDecimal.ZERO)`) চেয়ে পরিষ্কার সিনট্যাক্সে। ঋণাত্মক transaction (টাকা কাটা) লাল, ধনাত্মক (টাকা জমা) সবুজ — visual cue হিসেবে টাকার প্রবাহের দিক এক নজরে বোঝা যায়। এই module-এ status badge নেই (transaction-এর কোনো status field নেই, শুধু type/reference/amount/date), তাই এখানে নতুন redesign-এর `StatusBadgeView` প্রযোজ্য না — শুধু text color দিয়েই ইঙ্গিত দেওয়া হয়েছে।

লক্ষণীয়: `WalletActivity`-তে `SecureScreen.apply()` বা `FLAG_SECURE` **নেই** — যদিও এটা টাকার তথ্য দেখায়। এটা invoice/payment/receipt/subscription স্ক্রিনগুলোর সাথে অসামঞ্জস্যপূর্ণ (সেগুলোর সবগুলোতেই screenshot protection আছে); সম্ভবত একটা oversight, ভবিষ্যতে ঠিক করার মতো একটা জায়গা।

### Wallet প্যাকেজ — সম্পূর্ণ ফাইল তালিকা

| ফাইল | বর্ণনা |
|---|---|
| `WalletTransactionAdapter.java` | Transaction history-এর RecyclerView adapter — type, reference, date, এবং amount (ঋণাত্মক হলে লাল, ধনাত্মক হলে সবুজ)। |
| `WalletActivity.java` | Wallet balance summary (total/balance/credit) + transaction history লোড করে দেখায়, `StateView` দিয়ে empty/error। |

## ৭.৪ Catalog মডিউল (ui/catalog/)

**বাস্তব সমস্যা:** client-কে জানতে হয় company কী কী individual service অফার করে (এক-বারের জন্য request করার মতো) আর কী কী subscription package আছে (recurring billing-এর মতো)। এই দুটো ধারণা আলাদা স্ক্রিনে থাকলেও একই "কেনাকাটা" journey-এর অংশ।

### `CatalogActivity.java` + `CatalogRow.java` — দুটো ভিন্ন লিস্ট, একটা মিলিত RecyclerView

```java
public static CatalogRow header(String name) { ... }
public static CatalogRow service(CompanyServiceResponse service) { ... }
```

`CatalogRow` একটা **sealed-ish union type** (Java-তে proper sealed class ছাড়াই) — static factory method দিয়ে হয় header row নয়তো service row বানায়, private constructor দিয়ে বাইরের কেউ ভুল combination বানাতে পারে না। এটা `CatalogAdapter`-কে দুই ধরনের ViewHolder (`TYPE_HEADER`, `TYPE_SERVICE`) দেখাতে দেয় একই RecyclerView-তে — category-wise গ্রুপ করা service list, প্রতিটা category-র উপরে একটা header।

**দুটো API কল সমান্তরালে, তারপর merge — `tryBuildRows()` guard:**

```java
private void tryBuildRows() {
    if (categories == null || services == null) {
        return;   // দুটোই না আসা পর্যন্ত অপেক্ষা করো
    }
    ...
}
```

`getServiceCategories()` আর `getActiveServices()` — দুটো independent async call, যেকোনো একটা আগে শেষ হতে পারে। দুটোরই `onResponse`/`onFailure` শেষে `tryBuildRows()` ডাকা হয়, কিন্তু ভেতরে null-check guard আছে — শুধু **দুটোই** এসে গেলে (successful বা empty-list fallback, কখনোই null না) row বানানোর কাজ শুরু হয়। এটা একটা ছোট কিন্তু common concurrency প্যাটার্ন — দুই async source-এর join পয়েন্ট, RxJava/coroutine ছাড়া হাতে-করে করা।

```java
Collections.sort(sortedCategories, Comparator.comparingInt(ServiceCategoryResponse::getSortOrder));
...
if (!uncategorized.isEmpty()) {
    rows.add(CatalogRow.header(getString(R.string.catalog_other_category)));
    ...
}
```

Category-গুলো backend-নির্ধারিত `sortOrder` অনুযায়ী সাজানো হয় (alphabetical না — business-এর ইচ্ছামতো priority)। যে service-এর category নেই বা category-টা list-এ ম্যাচ করেনি, সেগুলো "Other" নামের একটা catch-all header-এর নিচে যোগ হয় — কোনো service যেন silently হারিয়ে না যায়।

### `PackageListActivity.java` — subscribe flow

```java
private void subscribe(ServicePackageResponse servicePackage) {
    packageRepository.subscribe(servicePackage.getId(), null, new Callback<SubscriptionSummary>() {
        @Override
        public void onResponse(...) {
            ...
            Toast.makeText(..., R.string.subscribed_pending_payment, ...).show();
            startActivity(new Intent(PackageListActivity.this, SubscriptionListActivity.class));
        }
        ...
    });
}
```

Subscribe করলেই সরাসরি payment শুরু হয় না — backend প্রথমে একটা subscription **`PENDING_PAYMENT`** state-এ তৈরি করে দেয় (string resource-এর নামই সেটা বলে দিচ্ছে: `subscribed_pending_payment`), তারপর client-কে `SubscriptionListActivity`-তে পাঠানো হয় যেখানে সেই subscription-এর পাশে "Pay Now" বোতাম দেখা যাবে (§৭.২-এর `SubscriptionAdapter` দেখো)। subscribe আর pay — দুটো ইচ্ছাকৃতভাবে আলাদা ধাপ, একই call-এ merge করা হয়নি।

### Catalog প্যাকেজ — সম্পূর্ণ ফাইল তালিকা

| ফাইল | বর্ণনা |
|---|---|
| `CatalogRow.java` | header/service — দুই ধরনের row-এর জন্য একটা union-স্টাইল model, static factory + private constructor। |
| `CatalogAdapter.java` | Category-wise গ্রুপ করা service catalog-এর RecyclerView adapter, দুই ViewHolder টাইপ (header/service)। |
| `PackageAdapter.java` | Subscription package তালিকার RecyclerView adapter — নাম, বর্ণনা, effective price/billing cycle, Subscribe বোতাম। |
| `CatalogActivity.java` | Categories + active services দুটো API সমান্তরালে fetch করে, merge করে category-grouped list বানায়, "Other" catch-all সহ। |
| `PackageListActivity.java` | সব সক্রিয় subscription package দেখায়, confirmation dialog সহ subscribe (→ pending-payment subscription তৈরি হয়, পরে আলাদা ধাপে টাকা দেওয়া হয়)। |

## ৭.৫ এই অধ্যায়ের ইন্টারভিউ-প্রশ্ন সারাংশ

দ্রুত রিভিশনের জন্য, এই অধ্যায়ে যে প্রশ্নগুলো এসেছে:

1. **"money-এর জন্য `BigDecimal` কেন, `double`/`float` কেন না?"** — binary floating-point-এ decimal fraction ঠিকভাবে represent হয় না, ফলে rounding error জমে; `BigDecimal` arbitrary-precision decimal arithmetic দেয়। (§৭.১)
2. **"payment gateway redirect-কে কেন সরাসরি বিশ্বাস করা উচিত না?"** — redirect client-side, tamper/spoof করা যায়; gateway optimistic "success"-ও পরে server-side validation-এ fail করতে পারে (`VALIDATION_FAILED`); IPN redirect থেকে independent timing-এ আসে। সত্যের উৎস সবসময় backend-এর নিজের record — resource আবার fetch করে দেখা। (§৭.২)
3. **"fixed-interval polling-এর বদলে exponential backoff কেন?"** — দ্রুত settle হওয়া case-এ তাড়াতাড়ি resolve, ধীর case-এও (worst-case সার্ভার-লোড না বাড়িয়ে) যথেষ্ট সময় দেওয়া, তারপর honest "unconfirmed" state — মিথ্যা "failed" না। (§৭.২)
4. **"custom URI scheme আর Android App Links-এর মধ্যে security-এর পার্থক্য কী?"** — custom scheme non-exclusive (অন্য app claim করতে পারে, hijacking সম্ভব); App Links verified HTTPS domain-এর সাথে bind, `assetlinks.json` দিয়ে ownership প্রমাণিত, exclusive। (§৭.২)
5. **"কেন WebView, Chrome Custom Tabs না — payment flow-এর জন্য?"** — Custom Tabs navigation intercept করতে দেয় না; WebView-এর `shouldOverrideUrlLoading()` দিয়ে redirect browser-এ পৌঁছানোর আগেই ধরা যায়। (§৭.২)
6. **"দুটো independent async API call-এর ফলাফল merge করতে হলে কী করবেন (RxJava/coroutine ছাড়া)?"** — `CatalogActivity.tryBuildRows()`-এর মতো একটা join-guard: প্রতিটা callback শেষে একই merge মেথড ডাকা, ভেতরে null-check দিয়ে নিশ্চিত করা দুটোই এসেছে কিনা। (§৭.৪)
7. **"`equals()` না `compareTo()` — `BigDecimal` তুলনায় কোনটা ব্যবহার করবেন এবং কেন?"** — `equals()` scale-ও মেলাতে চায় (`5.00` ≠ `5.0`), `compareTo()` শুধু মান তুলনা করে; টাকা "বদলেছে কিনা" দেখতে `compareTo() != 0` সঠিক। (§৭.১, `InvoiceDetailViewModel.settled()`)

---

**এই অধ্যায়ে মোট কভার করা ফাইল: ১৯টা মূল `.java` ফাইল** (`ui/invoice/`-এ ৬টা, `ui/payment/`-এ ৬টা, `ui/wallet/`-এ ২টা, `ui/catalog/`-এ ৫টা) — প্রতিটা সম্পূর্ণ পড়ে বিশ্লেষণ করা হয়েছে, অনুমান করে না। এছাড়া সহায়ক প্রসঙ্গের জন্য পড়া হয়েছে: `PaymentRepository.java`, `InvoiceRepository.java`, `SecureScreen.java`, `StatusBadgeView.java`, `StateView.java`, এবং as-built doc-এর §১২ (Payments — SSLCommerz)।
