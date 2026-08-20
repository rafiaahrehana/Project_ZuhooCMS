# অধ্যায় ১৪ — Spring Boot: Interview Q&A

## Core Concepts

**প্রশ্ন: Dependency Injection কী, এই প্রজেক্টে কীভাবে ব্যবহৃত?**
DI মানে একটা ক্লাস নিজে তার dependency তৈরি না করে, বাইরে থেকে (framework দিয়ে) পেয়ে যায়। এই প্রজেক্টে **constructor injection** প্রধান প্যাটার্ন — `@RequiredArgsConstructor` (Lombok) দিয়ে সব `final` ফিল্ড constructor-এ ইনজেক্ট হয়, যেমন `RecruitmentServiceImpl(CandidateService candidateService, JobApplicationRepository repo, ...)`। Constructor injection field injection (`@Autowired` সরাসরি ফিল্ডে) থেকে ভালো কারণ dependency immutable (`final`) হয় আর টেস্টে সহজে mock পাস করা যায়।

**প্রশ্ন: `@Component`, `@Service`, `@Repository`, `@Controller` — পার্থক্য কী?**
সবগুলোই আসলে `@Component`-এর স্টেরিওটাইপ (মেটা-অ্যানোটেশন) — Spring-কে বলে এই ক্লাসটা একটা bean হিসেবে স্ক্যান করো। `@Service` বিজনেস লজিক লেয়ারের জন্য (semantic, functionally `@Component`-এর মতোই)। `@Repository` — এক্সট্রা সুবিধা দেয়: ডেটাবেজ exception-কে Spring-এর `DataAccessException` হায়ারার্কিতে translate করে। `@Controller`/`@RestController` — web লেয়ার, `@RestController` = `@Controller` + `@ResponseBody` (প্রতিটা রেসপন্স JSON হিসেবে সিরিয়ালাইজ হয়)।

**প্রশ্ন: Spring Boot Auto-configuration কীভাবে কাজ করে?**
Classpath-এ কী কী লাইব্রেরি আছে দেখে, Spring Boot নিজে থেকে অনুমান করে কোন bean-গুলো দরকার। যেমন — PostgreSQL ড্রাইভার classpath-এ থাকলে, `DataSource` bean অটোমেটিক তৈরি হয়ে যায় `application.properties`-এর কানেকশন স্ট্রিং দিয়ে, কোনো ম্যানুয়াল `@Bean` কনফিগারেশন ছাড়াই।

## JPA / Hibernate

**প্রশ্ন: `@Filter` কীভাবে কাজ করে (এই প্রজেক্টের মাল্টি-টেন্যান্সি)?**
`@FilterDef` একটা named filter সংজ্ঞায়িত করে (একটা প্যারামিটার সহ), `@Filter` সেটা একটা entity-তে প্রয়োগ করে একটা SQL condition দিয়ে (`company_id = :companyId`)। Hibernate তখন প্রতিটা query-তে এই condition যোগ করে দেয়, **কিন্তু শুধু যদি filter enable করা থাকে সেই session-এ** — `session.enableFilter("tenantFilter").setParameter(...)` কল করতে হয়। এই প্রজেক্টে এটা `TenantFilterInterceptor.preHandle()`-এ প্রতিটা `/api/**` রিকোয়েস্টের শুরুতে হয়।

**প্রশ্ন: `@OneToMany`, `@ManyToOne`, `@ManyToMany` — এই প্রজেক্টে উদাহরণ।**
- `@ManyToOne` — `JobApplication.candidate` (একাধিক application একজন candidate-এর)
- `@OneToMany` — `Department.employees` (`mappedBy="department"`)
- `@ManyToMany` — `ServicePackage.services` (একটা প্যাকেজে একাধিক সার্ভিস, একটা সার্ভিস একাধিক প্যাকেজে থাকতে পারে) — join টেবিল `service_package_items` দিয়ে

**প্রশ্ন: Lazy বনাম Eager Loading?**
Lazy — সম্পর্কিত এন্টিটি তখনই লোড হয় যখন সত্যিই অ্যাক্সেস করা হয় (`getEmployees()` কল করার সময়)। Eager — সাথে সাথেই লোড হয়ে যায় parent এন্টিটির সাথে। ডিফল্ট: `@ManyToOne`/`@OneToOne` eager, `@OneToMany`/`@ManyToMany` lazy। এই প্রজেক্টে বেশিরভাগ `@OneToMany` কালেকশন explicitly lazy রাখা হয়েছে (যেমন `Employee.department` না, বরং `Department.employees`) — কারণ eager হলে প্রতিবার একটা Department fetch করলেই সব employee লোড হয়ে যেত, অপ্রয়োজনীয় N+1 বা বিশাল query।

**প্রশ্ন: `@SQLRestriction` কী?**
Hibernate 6-এ `@Where`-এর replacement — একটা static SQL condition যা সব সময় প্রতিটা query-তে যুক্ত হয়ে যায়, কোনো on/off toggle ছাড়াই। এই প্রজেক্টে `BaseEntity`-তে `@SQLRestriction("deleted = false")` — সব entity সব সময় soft-deleted row বাদ দিয়ে query করে, কোনো `.findByDeletedFalse()` লেখা ছাড়াই।

**প্রশ্ন: N+1 Query সমস্যা কী?**
একটা লিস্টের প্রতিটা আইটেমের জন্য আলাদা করে একটা রিলেটেড এন্টিটি lazy-load করলে, ১টা parent query + N টা child query = N+1 query চলে যায়, যেটা পারফরম্যান্স খারাপ করে। সমাধান: `JOIN FETCH` ব্যবহার করা JPQL-এ, বা `@EntityGraph`।

## Transactions

**প্রশ্ন: `@Transactional`-এর `propagation` কী, `REQUIRES_NEW` কখন লাগে?**
Propagation ঠিক করে যে একটা মেথড যখন আরেকটা transactional মেথড কল করে, কী হবে — একই transaction চালিয়ে যাবে নাকি নতুন শুরু করবে। `REQUIRES_NEW` মানে বাইরের transaction যাই থাকুক, এই মেথড একটা **সম্পূর্ণ নতুন, স্বাধীন** transaction শুরু করবে। এই প্রজেক্টে `ServiceRequestServiceImpl.ensurePendingStageApproval()`-এ ব্যবহৃত — কারণ approval তৈরি করার ঠিক পরেই বাইরের method একটা exception ছুঁড়ে দেয় (ইউজারকে জানাতে), যা normal propagation হলে পুরো transaction (approval-সহ) rollback করে দিত।

**প্রশ্ন: `TransactionSynchronizationManager.registerSynchronization()` কী করে?**
এটা একটা callback রেজিস্টার করে যা বর্তমান transaction commit/rollback হওয়ার **পরে** চলে। এই প্রজেক্টে `CvScoringService.scheduleAfterCommit()`-এ ব্যবহৃত — `apply()`-এর transaction commit হওয়ার আগে async CV-স্কোরিং শুরু হলে race condition হতে পারতো (async থ্রেড এখনো-persist-না-হওয়া row খুঁজে পেত না)।

## Spring Security

**প্রশ্ন: JWT-ভিত্তিক Stateless Authentication কীভাবে কাজ করে?**
সার্ভার কোনো session সেভ করে না (`SessionCreationPolicy.STATELESS`)। লগইনে একটা signed JWT দেওয়া হয়, যেটাতে ইউজারের পরিচয় (claims) এনকোড থাকে। প্রতিটা পরবর্তী রিকোয়েস্টে ক্লায়েন্ট সেই টোকেন `Authorization` হেডারে পাঠায়; একটা filter (`JwtAuthFilter`) টোকেনের signature ভেরিফাই করে (যা প্রমাণ করে এটা tampered হয়নি) আর claims থেকে ইউজার/রোল বের করে `SecurityContext`-এ বসায় — সব কিছু per-request, কোনো সার্ভার-সাইড session স্টেট ছাড়াই।

**প্রশ্ন: `@PreAuthorize` কেন এই প্রজেক্টে খুবই কম ব্যবহৃত হয়েছে?**
কারণ এই প্রজেক্টের permission মডেল runtime-নির্ভর জটিল লজিক বহন করে (impersonation bypass, owner universal-access, platform-role whitelist) — যেটা SpEL এক্সপ্রেশনে declaratively লেখা কঠিন/অস্বচ্ছ হতো, তার জন্য একটা কাস্টম `PermissionEvaluator` লাগতোই। তার বদলে একটা সাধারণ ইনজেক্টেবল `AuthorizationService` ব্যবহার করা হয়েছে, যা প্রতিটা controller method-এর ভেতরে explicit কল করে চেক করে — কম "magic", বেশি ডিবাগযোগ্য।

## Exception Handling

**প্রশ্ন: `@RestControllerAdvice` কী, `@ExceptionHandler`-এর সাথে সম্পর্ক?**
`@RestControllerAdvice` একটা গ্লোবাল ক্লাস যেখানে একাধিক `@ExceptionHandler(SomeException.class)` মেথড থাকতে পারে — যেকোনো controller-এ ছোঁড়া exception এই কেন্দ্রীয় জায়গায় ধরা পড়ে, HTTP status code + একটা uniform JSON response-এ রূপান্তরিত হয়। এতে প্রতিটা controller-এ try-catch লেখার দরকার হয় না।

## Testing (সাধারণ প্রশ্ন, প্রজেক্টের বাইরে)

**প্রশ্ন: `@SpringBootTest` বনাম `@WebMvcTest` বনাম `@DataJpaTest`?**
`@SpringBootTest` — পুরো application context লোড করে, integration test-এর জন্য (ধীর কিন্তু বাস্তবসম্মত)। `@WebMvcTest` — শুধু web লেয়ার (controller) লোড করে, service/repository mock করতে হয় (দ্রুত)। `@DataJpaTest` — শুধু JPA লেয়ার, in-memory ডেটাবেজ দিয়ে repository টেস্ট করার জন্য।

## Scheduling ও Interceptors

**প্রশ্ন: `@Scheduled` কীভাবে কাজ করে, cron expression বনাম fixedRate?**
`@Scheduled(cron = "0 */30 * * * *")` — cron সিনট্যাক্স দিয়ে নির্দিষ্ট সময়ে (প্রতি ৩০ মিনিটে, প্রতিদিন সকাল ৯টায়) মেথড চালানো যায় — এই প্রজেক্টে `SlaBreachScheduler`, `SubscriptionScheduler`, `AbsenteeMarkingService`-এর নাইট জব সবই এই প্যাটার্নে। `fixedRate`/`fixedDelay` — একটা নির্দিষ্ট ব্যবধানে বারবার চালানোর জন্য, শুরুর সময় নির্দিষ্ট না। এই প্রজেক্টে cron বেশি ব্যবহৃত কারণ বেশিরভাগ scheduled কাজ নির্দিষ্ট সময়ে (রাতে, সকালে) চালানো দরকার, শুধু "প্রতি X মিনিটে" যথেষ্ট না।

**প্রশ্ন: `HandlerInterceptor` বনাম `Filter` — পার্থক্য কী, এই প্রজেক্টে কোনটা কোথায়?**
`Filter` (servlet-লেভেল, `javax.servlet.Filter`) Spring-এর DispatcherServlet-এর **আগে** চলে — এখনো কোনো Spring MVC কনটেক্সট (handler resolution) নেই তখন। `HandlerInterceptor` (Spring MVC-লেভেল) DispatcherServlet-এর **ভেতরে**, নির্দিষ্ট handler resolve হওয়ার পরে চলে — তাই `@PathVariable`, handler method সম্পর্কে তথ্য অ্যাক্সেস করা যায়। এই প্রজেক্টে `JwtAuthFilter` (auth — servlet-লেভেলে দরকার, DispatcherServlet-এর আগেই authentication ঠিক করতে হবে) একটা `Filter`, কিন্তু `TenantFilterInterceptor` (tenant scoping — handler resolve হওয়ার পরে যথেষ্ট, কারণ ততক্ষণে auth ইতিমধ্যে সম্পন্ন) একটা `HandlerInterceptor`।

## Lombok

**প্রশ্ন: `@RequiredArgsConstructor` কী করে, কেন এই প্রজেক্টে এত ব্যবহৃত?**
সব `final` ফিল্ড নিয়ে একটা constructor অটো-জেনারেট করে দেয় — বয়লারপ্লেট কমায়। এই প্রজেক্টে প্রতিটা `@Service`/`@Component` ক্লাসে dependency-গুলো `private final` হিসেবে ঘোষণা করা হয় (constructor injection-এর জন্য), আর `@RequiredArgsConstructor` সেই constructor নিজে থেকেই বানিয়ে দেয় — ম্যানুয়ালি প্রতিটা dependency-র জন্য constructor প্যারামিটার লিখতে হয় না।

**প্রশ্ন: `@Getter`/`@Setter` ব্যবহার করলে encapsulation ভঙ্গ হয় না কি?**
আংশিকভাবে হ্যাঁ — প্রতিটা ফিল্ডের জন্য পাবলিক getter/setter দেওয়া মানে বাইরের কোড সরাসরি state পড়তে/বদলাতে পারে, যেটা "খাঁটি" encapsulation-এর বিপরীত। কিন্তু বাস্তবে JPA entity-র ক্ষেত্রে এটা প্রায় অনিবার্য — Hibernate নিজেই reflection দিয়ে getter/setter ব্যবহার করে entity hydrate করে। ট্রেডঅফ: `@Getter`/`@Setter` কোড কমায়, ঝুঁকি: কেউ ভুলভাবে সরাসরি setter কল করে বিজনেস-রুল বাইপাস করতে পারে (তাই গুরুত্বপূর্ণ mutation যেমন `softDelete()` একটা আলাদা named মেথড হিসেবে রাখা হয়েছে, শুধু `setDeleted(true)` কল করার বদলে)।
