# অধ্যায় ১৩ — Java Core: Interview Q&A

> প্রতিটা উত্তরের সাথে এই প্রজেক্টের বাস্তব উদাহরণ যুক্ত করা হয়েছে, যাতে ইন্টারভিউতে শুধু থিওরি না বলে "আমার প্রজেক্টে এভাবে ব্যবহার করেছি" — এটাও বলা যায়।

## OOP বেসিকস

**প্রশ্ন: চারটা OOP পিলার কী কী, এই প্রজেক্টে উদাহরণ দাও।**
- **Encapsulation:** প্রতিটা Entity ক্লাস — ফিল্ড `private`, `@Getter`/`@Setter` (Lombok) দিয়ে কন্ট্রোলড অ্যাক্সেস
- **Inheritance:** `BaseEntity` — সব entity (`Employee`, `Client`, `Invoice`...) এটা `extends` করে `id`, `createdAt`, `deleted` পায়
- **Polymorphism:** `AiHttpClient` ইন্টারফেস — `ClaudeClient`, `OpenAiClient`, `GeminiClient`, `GroqClient`, `MockAiClient` — সবাই একই `call()` মেথড implement করে, `AiServiceImpl` কোনটা ব্যবহার হচ্ছে তা না জেনেই কল করে
- **Abstraction:** `BaseEntity` নিজেই abstract (`@MappedSuperclass`) — কখনো সরাসরি instantiate হয় না

**প্রশ্ন: Interface বনাম Abstract Class — কখন কোনটা?**
Interface যখন শুধু "কী করতে হবে" চুক্তি দরকার, একাধিক implementation সম্পূর্ণ ভিন্ন হতে পারে (`AiHttpClient` — প্রতিটা প্রোভাইডারের API সম্পূর্ণ আলাদা, শুধু method signature কমন)। Abstract class যখন কিছু কমন state/বিহেভিয়ার শেয়ার করতে হয় (`BaseEntity` — সব entity-র `id`/`createdAt`/`deleted` ফিল্ড আর `softDelete()` মেথড একই রকম, শুধু ইনহেরিট করলেই হয়)।

## Collections

**প্রশ্ন: `List` বনাম `Set` বনাম `Map` — এই প্রজেক্টে কোথায় কোনটা?**
- `List` — ordered ডেটা যেখানে ক্রম গুরুত্বপূর্ণ, যেমন `WorkflowStage`-এর ordered লিস্ট (`stageOrder` অনুযায়ী)
- `Set` — ইউনিক ভ্যালুর সংগ্রহ, যেমন role-এর `authorities`
- `Map` — key-value lookup, যেমন `AiModel` enum-এর `modelId` স্ট্রিং ম্যাপিং, বা `EXPECTED_CONTENT_TYPES` (extension → MIME type ম্যাপ ফাইল আপলোড ভ্যালিডেশনে)

**প্রশ্ন: `HashMap` বনাম `TreeMap` বনাম `LinkedHashMap`?**
`HashMap` — ক্রম গুরুত্বপূর্ণ না, দ্রুততম লুকআপ। `TreeMap` — key sorted অর্ডারে চাই। `LinkedHashMap` — insertion অর্ডার ধরে রাখতে হবে (যেমন CSV import-এর column-header ম্যাপিং যেখানে original ক্রম গুরুত্বপূর্ণ)।

## Streams ও Lambda

**প্রশ্ন: Stream API-র একটা বাস্তব উদাহরণ দাও।**
`CvScoringService`-এ `Arrays.stream(requiredSkills.split(","))` — কমা-দিয়ে-আলাদা স্কিল স্ট্রিং থেকে প্রতিটা স্কিল টোকেন বের করে, `map()` দিয়ে trim করে, `filter()` দিয়ে ফাঁকা বাদ দেয়, তারপর প্রতিটার জন্য regex ম্যাচ চেক করে।

**প্রশ্ন: `Optional` কেন ব্যবহার করা হয়?**
Null-safety — একটা মেথড যদি কখনো ভ্যালু না-ও পেতে পারে (যেমন `employee.getDesignation()` — একটা employee-র designation নাও থাকতে পারে), `Optional` ব্যবহার করে caller-কে বাধ্য করা হয় null-চেক করতে, নাহলে কম্পাইলার ওয়ার্নিং/সরাসরি `NullPointerException`-এর ঝুঁকি। যদিও এই প্রজেক্টে বেশিরভাগ জায়গায় সরাসরি null-চেক (`if (x != null)`) ব্যবহৃত হয়েছে, `Optional` মূলত repository query-তে দেখা যায় (`findById()` `Optional<T>` রিটার্ন করে)।

## Exception Handling

**প্রশ্ন: Checked বনাম Unchecked Exception?**
Checked exception (যেমন `IOException`) কম্পাইল-টাইমে handle করতে বাধ্য করে (`try-catch` বা `throws`)। Unchecked (`RuntimeException`-এর সাবক্লাস) কম্পাইল-টাইমে বাধ্যতামূলক না। এই প্রজেক্টের সব কাস্টম exception (`BadRequestException`, `ResourceNotFoundException`, `ForbiddenException`) **unchecked** — কারণ প্রতিটা service মেথডে `throws` ঘোষণা করা বিরক্তিকর হতো, আর `@RestControllerAdvice` (`GlobalExceptionHandler`) কেন্দ্রীয়ভাবে সবগুলো ধরে HTTP রেসপন্সে রূপান্তর করে।

**প্রশ্ন: `try-with-resources` কী, উদাহরণ দাও।**
একটা `AutoCloseable` রিসোর্স (যেমন `InputStream`) `try(...)` ব্লকের মধ্যে ঘোষণা করলে, ব্লক শেষে (এমনকি exception হলেও) সেটা অটোমেটিক `close()` হয়ে যায়। `CvTextExtractor`-এ PDF/DOCX ফাইল পড়ার সময় এই প্যাটার্ন ব্যবহার হয়, যাতে ফাইল হ্যান্ডেল লিক না হয়।

## Concurrency

**প্রশ্ন: `@Async` কীভাবে কাজ করে ভেতরে ভেতরে?**
Spring `@Async` মেথডকে একটা প্রক্সির পেছনে র‍্যাপ করে — যখন কল হয়, প্রক্সি আসল মেথডকে একটা আলাদা থ্রেড পুলে (`ThreadPoolTaskExecutor`) সাবমিট করে দেয়, আর caller সাথে সাথে রিটার্ন পায় (`void` বা `Future`)। **গুরুত্বপূর্ণ পিটফল:** একই ক্লাসের ভেতর থেকে `this.asyncMethod()` কল করলে প্রক্সি বাইপাস হয়ে যায়, মেথডটা synchronous-ই চলে। এই প্রজেক্টে `CvScoringService`-এ এই সমস্যা সমাধান করা হয়েছে `@Lazy` self-injection দিয়ে (`self.scoreApplication(...)`)।

**প্রশ্ন: Race condition কী, এই প্রজেক্টে কোথায় প্রতিরোধ করা হয়েছে?**
Race condition ঘটে যখন একাধিক থ্রেড একই শেয়ার্ড ডেটা একসাথে পড়ে/লেখে, ফলাফল timing-নির্ভর অনির্দিষ্ট হয়ে যায়। উদাহরণ: `AssetServiceImpl.assign()`-এ **pessimistic lock** (`findByIdAndCompanyIdForUpdate` — SQL `SELECT ... FOR UPDATE`) ব্যবহার করা হয়েছে, যাতে দুইজন অ্যাডমিন একই সাথে একই asset দুই আলাদা employee-কে assign করার race condition না ঘটে। একইভাবে `ServicePackageServiceImpl.consumeQuota()`-তেও pessimistic lock।

**প্রশ্ন: `ThreadLocal` কী, `SecurityContextHolder`-এর সাথে সম্পর্ক কী?**
`ThreadLocal` — প্রতিটা থ্রেডের নিজস্ব, আলাদা কপি রাখে একটা ভ্যারিয়েবলের, একই ভ্যারিয়েবল নামেও থ্রেডগুলো একে অপরের ডেটা দেখতে পায় না। Spring Security-র `SecurityContextHolder` ডিফল্টে `ThreadLocal`-ভিত্তিক (`MODE_THREADLOCAL`) — তাই লগইন করা ইউজারের তথ্য প্রতিটা রিকোয়েস্ট-থ্রেডে আলাদাভাবে "মনে থাকে"। কিন্তু এই কারণেই `@Async` মেথড (আলাদা থ্রেড পুলে চলে) সেই তথ্য দেখতে পায় না — `CvScoringService.scoreApplication()` তাই `companyId` প্যারামিটার হিসেবে নেয়, `SecurityUtil` কল করে না।

## Generics

**প্রশ্ন: Generic টাইপের একটা উদাহরণ দাও এই প্রজেক্টে।**
`ApiResponse<T>` (ব্যাকএন্ড) — একই ক্লাস `ApiResponse<EmployeeResponse>`, `ApiResponse<List<InvoiceResponse>>` ইত্যাদি হয়ে যায়, টাইপ-সেফটি বজায় রেখে একটা uniform envelope শেয়ার করে সব endpoint।

## JVM ও Memory (সাধারণ প্রশ্ন)

**প্রশ্ন: Stack বনাম Heap মেমরি?**
Stack — মেথড কল, লোকাল ভ্যারিয়েবল, প্রিমিটিভ টাইপ — দ্রুত, স্বয়ংক্রিয়ভাবে পরিষ্কার হয় মেথড রিটার্ন করলে। Heap — অবজেক্ট, ইনস্ট্যান্স ভ্যারিয়েবল — Garbage Collector পরিষ্কার করে যখন কোনো রেফারেন্স আর অবজেক্টটা ধরে রাখে না।

**প্রশ্ন: `equals()` আর `hashCode()` কেন একসাথে override করতে হয়?**
`HashMap`/`HashSet`-এর মতো কালেকশন hashCode দিয়ে প্রথমে bucket খুঁজে, তারপর equals দিয়ে নিশ্চিত করে। শুধু `equals()` override করলে (hashCode না করলে), দুটো "সমান" অবজেক্ট ভিন্ন bucket-এ চলে যেতে পারে — `Set`-এ ডুপ্লিকেট থেকে যায়, `Map`-এ lookup ব্যর্থ হয়।

## Enum

**প্রশ্ন: Java enum-এ প্রতিটা constant-এর সাথে একটা ফিল্ড কীভাবে জুড়ে দেওয়া যায়? এই প্রজেক্টে উদাহরণ দাও।**
Enum constructor-এর মাধ্যমে — যেমন `AiModel` এনামে প্রতিটা constant (`GEMINI_2_5_FLASH`, `CLAUDE_SONNET`...) একটা `modelId` স্ট্রিং বহন করে:
```java
public enum AiModel {
    GEMINI_2_5_FLASH("gemini-flash-latest"),
    CLAUDE_SONNET("claude-sonnet-5");
    private final String modelId;
    AiModel(String modelId) { this.modelId = modelId; }
    public String getModelId() { return modelId; }
}
```
এভাবে টাইপ-সেফ কনস্ট্যান্টের সাথে অতিরিক্ত মেটাডেটা (এখানে আসল API model ID) জুড়ে দেওয়া যায় — যদি এই ম্যাপিং একটা প্লেইন `Map<String,String>` হতো, ভুল key টাইপ করার ঝুঁকি থাকতো, কম্পাইলার ধরতে পারতো না।

**প্রশ্ন: Enum-এ মেথড override করা যায় কি, উদাহরণ কোথায় আছে?**
হ্যাঁ — `PerformanceStage` এনামের `next()` মেথড একটা সরল উদাহরণ (যদিও এখানে সাধারণ switch/if দিয়ে করা, প্রতি-constant override না) — একটা লিনিয়ার চেইন (`SELF_ASSESSMENT → MANAGER_REVIEW → ... → COMPLETED`) এনকোড করে। Java-তে চাইলে প্রতিটা enum constant-এর নিজস্ব method body-ও থাকতে পারে (anonymous class-এর মতো), কিন্তু এই প্রজেক্টে সেই প্যাটার্ন ব্যবহার হয়নি — সরল switch-based লজিক দিয়েই কাজ চলেছে।

## Immutability

**প্রশ্ন: Immutable ক্লাস কী, কেন গুরুত্বপূর্ণ?**
একবার তৈরি হলে যার state আর বদলানো যায় না (সব ফিল্ড `final`, কোনো setter নেই)। গুরুত্বপূর্ণ কারণ: থ্রেড-সেফ (একাধিক থ্রেড নিরাপদে শেয়ার করতে পারে, কোনো race condition নেই), আর predictable (একটা রেফারেন্স পাস করলে নিশ্চিত থাকা যায় সেটা কেউ পাল্টে দেবে না)। এই প্রজেক্টে `PermissionCode` enum values, DTO রেসপন্স ক্লাস (যেগুলো একবার তৈরি হয়ে সরাসরি JSON-এ সিরিয়ালাইজ হয়ে যায়) — এই ধরনের প্যাটার্নে immutability স্বাভাবিকভাবেই আসে।

## Access Modifiers

**প্রশ্ন: `private`, `protected`, `default` (package-private), `public` — কোনটা কখন?**
`private` — শুধু নিজের ক্লাসে অ্যাক্সেসযোগ্য (এই প্রজেক্টের সব entity ফিল্ড private, Lombok getter/setter দিয়ে কন্ট্রোলড অ্যাক্সেস)। `protected` — নিজের ক্লাস + সাবক্লাস + একই প্যাকেজ (inheritance hierarchy-তে ব্যবহৃত, যেমন `BaseEntity`-র ফিল্ড সাবক্লাস থেকে সরাসরি অ্যাক্সেসযোগ্য প্রয়োজনে)। `default` — শুধু একই প্যাকেজ (Spring bean-এর ইমপ্লিমেন্টেশন ক্লাস প্রায়ই package-private রাখা হয়, শুধু ইন্টারফেসটা public)। `public` — সব জায়গা থেকে অ্যাক্সেসযোগ্য (Controller, Service ইন্টারফেস)।
