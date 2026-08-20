# অধ্যায় ৩ — Android বেসিক ধারণা (প্রয়োজনীয় Prerequisite)

> এই অধ্যায়টা "Android কী" শেখানোর জন্য না — বরং যে concept গুলো এই প্রজেক্ট জুড়ে বারবার ব্যবহৃত হয়েছে, সেগুলো ঝালিয়ে নেওয়ার জন্য, যাতে পরের অধ্যায়গুলোতে কোড দেখলে চিনতে পারো। প্রতিটা concept-এর সাথে interview প্রশ্ন জুড়ে দেওয়া হয়েছে।

## ৩.১ Activity Lifecycle

একটা Activity (একটা screen) তৈরি হওয়া থেকে destroy হওয়া পর্যন্ত কতগুলো ধাপ পার হয়:

```
onCreate() → onStart() → onResume() → [screen visible & active]
                                            │
                    ব্যবহারকারী অন্য screen-এ গেলে
                                            ▼
                onPause() → onStop() → [background]
                                            │
                        ফিরে এলে
                                            ▼
                        onRestart() → onStart() → onResume()
                                            │
                        Activity শেষ হলে
                                            ▼
                                       onDestroy()
```

**এই প্রজেক্টে কোথায় ব্যবহার হয়েছে:**
- `onCreate()` — সব Activity-তে এখানেই `ViewBinding.inflate()`, `setContentView()`, আর initial setup হয়।
- `onResume()` — প্রায় সব list screen (`LeaveRequestListActivity`, `ExpenseListActivity`, ইত্যাদি) এখানে **আবার data load করে**। কেন? কারণ ব্যবহারকারী একটা নতুন leave request submit করে "back" চাপলে, list screen-এ ফিরে এসে **আবার তাজা ডেটা** দেখা উচিত — `onCreate()`-এ একবারই load করলে সেটা হতো না।

> **Interview প্রশ্ন: "onCreate আর onResume-এ data load করার পার্থক্য কী?"**
> `onCreate()` শুধু **একবার** চলে (screen প্রথমবার তৈরি হওয়ার সময়)। `onResume()` **প্রতিবার** চলে যখনই screen-টা আবার সামনে আসে (অন্য screen থেকে ফিরে এলেও)। যে screen-এর ডেটা বাইরে থেকে বদলে যেতে পারে (অন্য screen থেকে ফিরে এসে), সেখানে `onResume()`-এ reload করাই সঠিক।

## ৩.২ Intent — Screen থেকে Screen-এ যাওয়া

যেহেতু এই প্রজেক্টে Fragment নেই (অধ্যায় ২ দ্রষ্টব্য), নেভিগেশন হয় সরাসরি **Intent** দিয়ে:

```java
Intent intent = new Intent(this, CreateLeaveRequestActivity.class);
startActivity(intent);
```

ডেটা পাঠাতে হলে **extra** ব্যবহার হয়:
```java
Intent intent = new Intent(this, InvoiceDetailActivity.class);
intent.putExtra(InvoiceDetailActivity.EXTRA_INVOICE_ID, invoiceId);
startActivity(intent);
```

**ফলাফল ফেরত পাওয়ার জন্য** (যেমন Payment screen থেকে ফিরে এসে জানা দরকার payment success হলো কি না) — পুরনো `startActivityForResult()` না, বরং নতুন **Activity Result API**:
```java
ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(), this::onPaymentResult);
```

> **Interview প্রশ্ন: "startActivityForResult() deprecated কেন, আর নতুন API কী সুবিধা দেয়?"**
> পুরনো `startActivityForResult()`-এ `requestCode` (একটা integer) দিয়ে বোঝাতে হতো কোন request-এর result এটা — একাধিক জায়গায় একই code ব্যবহার হলে conflict হতে পারত, আর Fragment-এর ভেতরে এটা আরও জটিল হতো। নতুন **Activity Result API** (`registerForActivityResult`) টাইপ-সেফ, requestCode লাগে না, আর lifecycle-aware — Activity destroy হয়ে আবার তৈরি হলেও (screen rotate করলে) callback ঠিকভাবে কাজ করে।

## ৩.৩ ViewBinding — বিস্তারিত

অধ্যায় ২-এ সংক্ষেপে বলা হয়েছে। এখানে একটু গভীরে:

```xml
<!-- activity_login.xml -->
<TextInputEditText android:id="@+id/emailInput" .../>
```

```java
// Gradle automatically বানায়: ActivityLoginBinding
binding = ActivityLoginBinding.inflate(getLayoutInflater());
setContentView(binding.getRoot());

String email = binding.emailInput.getText().toString();
```

> **Interview প্রশ্ন: "ViewBinding বনাম DataBinding — পার্থক্য কী?"**
> দুটোই `findViewById()`-এর বিকল্প, কিন্তু **DataBinding** আরও শক্তিশালী — XML-এর ভেতরেই expression লেখা যায় (`android:text="@{user.name}"`), two-way binding সম্ভব, কিন্তু build time বেশি লাগে আর শেখার curve বেশি। **ViewBinding** শুধু view reference generate করে, সহজ ও দ্রুত — এই প্রজেক্টে যেহেতু জটিল XML-expression দরকার হয়নি, ViewBinding-ই যথেষ্ট ছিল।

## ৩.৪ RecyclerView + Adapter Pattern

List দেখানোর (leave requests, invoices, expenses ইত্যাদি) মূল উপাদান। তিনটা অংশ বুঝতে হবে:

1. **RecyclerView** — যে view-টা scroll হয়, list ধারণ করে।
2. **Adapter** — কোন position-এ কোন ডেটা দেখাবে, সেটা ঠিক করে (`onCreateViewHolder`, `onBindViewHolder`, `getItemCount`)।
3. **ViewHolder** — একটা list item-এর ভেতরের view গুলো (title, badge, ইত্যাদি) ধরে রাখে, যাতে বারবার `findViewById` করতে না হয় (RecyclerView এগুলো recycle/পুনর্ব্যবহার করে)।

এই প্রজেক্টের সাধারণ Adapter প্যাটার্ন (`LeaveRequestAdapter` উদাহরণ):
```java
public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.ViewHolder> {
    private List<LeaveRequestResponse> items = new ArrayList<>();

    public void submitList(List<LeaveRequestResponse> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();   // পুরো list redraw করতে বলে
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemLeaveRequestBinding binding;   // ViewBinding এখানেও!
        void bind(LeaveRequestResponse item) {
            binding.itemTitle.setText(item.getLeaveType());
            StatusBadgeView.bind(binding.itemStatusBadge, ...);
        }
    }
}
```

> **Interview প্রশ্ন: "`notifyDataSetChanged()` বনাম `DiffUtil` — কোনটা ভালো, কেন?"**
> `notifyDataSetChanged()` **পুরো list-কে** আবার bind করতে বলে — এমনকি যে item গুলো বদলায়নি সেগুলোকেও। এতে unnecessary re-render হয় আর animation smooth হয় না। **`DiffUtil`** দুটো list (পুরনো আর নতুন) তুলনা করে শুধু **যেটুকু বদলেছে** সেটুকুই update করে — performance ভালো, আর add/remove animation স্বাভাবিকভাবে কাজ করে।
>
> **সততার সাথে বলি:** এই প্রজেক্টের adapter গুলো `notifyDataSetChanged()` ব্যবহার করে, `DiffUtil` না — এটা একটা known সীমাবদ্ধতা, বড় list-এ performance-এ প্রভাব ফেলতে পারে। Interview-তে এটা জিজ্ঞেস করলে সততার সাথে বলা ভালো: "এই প্রজেক্টে DiffUtil ব্যবহার হয়নি, কিন্তু আমি জানি এটা কেন ভালো অভ্যাস, আর কীভাবে যোগ করতে হয়" — এটা প্রমাণ করে তুমি শুধু কোড copy করোনি, ব্যাপারটা বোঝো।

## ৩.৫ LiveData ও ViewModel

**ViewModel** — একটা class যেখানে UI-related ডেটা রাখা হয়, আর এটা **screen rotation-এ বেঁচে থাকে** (Activity ধ্বংস হয়ে আবার তৈরি হলেও ViewModel-এর ডেটা হারায় না)।

**LiveData** — একটা **observable** ডেটা হোল্ডার — মানে ডেটা বদলালে যে UI সেটা "observe" করছে সে automatically আপডেট হয়ে যায়, আর এটা **lifecycle-aware** (Activity destroy হয়ে গেলে আর কোনো update পাঠায় না, তাই crash হয় না)।

```java
// ViewModel-এ:
private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
public LiveData<Boolean> loading() { return loading; }

// Activity-তে:
viewModel.loading().observe(this, isLoading -> {
    binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
});
```

এই প্রজেক্টের `CachedListViewModel` (সব list-ViewModel-এর ভিত্তি) চারটা LiveData expose করে: `items`, `loading`, `error`, `showingCached` — প্রতিটার একটা নির্দিষ্ট দায়িত্ব।

> **Interview প্রশ্ন: "LiveData বনাম Kotlin Flow — কেন LiveData এই প্রজেক্টে?"**
> Flow আসে Kotlin coroutine ecosystem থেকে — এই প্রজেক্ট **Java**-তে লেখা, তাই Flow স্বাভাবিক পছন্দ না। LiveData-র Java-friendly API আছে (`androidx.lifecycle:lifecycle-livedata`) আর lifecycle-awareness built-in — এই কারণেই এখানে LiveData ব্যবহার হয়েছে।

## ৩.৬ Material Components — মূল Widget গুলো

| Widget | কাজ | এই প্রজেক্টে |
|---|---|---|
| `MaterialButton` | বাটন | Primary/Secondary/Text — অধ্যায় ১০-এ বিস্তারিত |
| `TextInputLayout` + `TextInputEditText` | Floating-label input field | সব form screen-এ |
| `BottomNavigationView` | নিচের navigation bar | `BottomNavActivity`-তে |
| `CircularProgressIndicator` | Loading spinner (Material style) | সব loading state-এ |
| `MaterialAlertDialogBuilder` | Dialog | সব confirmation/info dialog-এ |
| `AutoCompleteTextView` (+ `TextInputLayout.ExposedDropdownMenu`) | Dropdown selection | Form-এর dropdown field |

## ৩.৭ Gradle — Build System বেসিক

- **`build.gradle` (project-level)** — পুরো প্রজেক্টের জন্য common সেটিংস।
- **`build.gradle` (app-level, `app/build.gradle`)** — এই মডিউলের dependency, SDK version, build type, flavor।
- **`gradle/libs.versions.toml`** — এই প্রজেক্টে **Version Catalog** ব্যবহার হয়েছে — সব library-র version এক জায়গায় (`libs.appcompat`, `libs.material` ইত্যাদি হিসেবে ব্যবহৃত)। পুরনো পদ্ধতিতে প্রতিটা `build.gradle`-এ hardcoded version string লেখা হতো, যেটা একাধিক module থাকলে version mismatch-এর ঝুঁকি তৈরি করত।

**Build Types বনাম Product Flavors — পার্থক্য:**
- **Build Type** (`debug` / `release`) — কীভাবে build হবে (minify, debug-able কিনা)।
- **Product Flavor** (`dev` / `prod`) — **কী** build হবে (কোন API URL, কোন applicationId)।

দুটো মিলে **Build Variant** তৈরি হয় — যেমন `devDebug`, `prodRelease`। এই প্রজেক্টে ৪টা variant সম্ভব: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`।

> **Interview প্রশ্ন: "Build Type আর Product Flavor-এর পার্থক্য কী?"**
> Build Type ঠিক করে **কীভাবে** কোড build/package হবে (obfuscation, debuggable flag)। Product Flavor ঠিক করে **কোন version** — অনেকটা আলাদা "app-এর ভ্যারিয়েন্ট" যেগুলোর হয়তো আলাদা backend URL, আলাদা feature set, আলাদা branding থাকতে পারে। দুটো independent dimension, তাই combine হয়ে বিভিন্ন variant তৈরি হয়।

## ৩.৮ R8 / ProGuard — Code Shrinking ও Obfuscation

Release build-এ `minifyEnabled true` — মানে **R8** (Android-এর নতুন code shrinker, পুরনো ProGuard-এর replacement) চলে:
1. **Shrinking** — যে কোড ব্যবহার হয় না সেটা বাদ দেয় (APK size কমায়)।
2. **Obfuscation** — class/method-এর নাম ছোট, অর্থহীন নামে বদলে দেয় (reverse-engineering কঠিন করে তোলে)।

**সমস্যা:** যদি একটা DTO ক্লাসের field-এর নাম obfuscation-এ বদলে যায় (যেমন `firstName` → `a`), কিন্তু JSON থেকে আসা ডেটাতে key-টা এখনো `"firstName"`, তাহলে Gson সেটা match করাতে পারবে না — data null হয়ে যাবে।

**সমাধান:** প্রতিটা DTO field-এ `@SerializedName("firstName")` annotation — এটা R8-কে বলে দেয় "এই field-টার আসল JSON key কী", নাম obfuscate হলেও। এই প্রজেক্টে **২৬৯টা field**-এ এই annotation যোগ করা হয়েছে।

> **Interview tip:** এটা একটা classic "release build-এ কাজ করে না, debug-এ করে" bug-এর কারণ — debug build-এ minify বন্ধ থাকে, তাই সমস্যা ধরাই পড়ে না যতক্ষণ না release build test করা হয়। এই প্রজেক্টের §১৭ (before-you-ship checklist)-এ স্পষ্ট লেখা আছে — "শুধু debug না, আসল R8 build স্মোক-টেস্ট করো"।

---

এই বেসিকগুলো মাথায় রেখে পরের অধ্যায়ে আমরা data layer-এর প্রতিটা ফাইল বিস্তারিতভাবে দেখব।
