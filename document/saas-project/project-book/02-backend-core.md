# অধ্যায় ২ — Backend Core Architecture

> এই অধ্যায়টা পুরো ব্যাকএন্ডের **infrastructure** নিয়ে — যেটা প্রতিটা মডিউল ব্যবহার করে কিন্তু কোনো একটা নির্দিষ্ট মডিউলের অংশ না। Spring Boot ইন্টারভিউর জন্য এই অধ্যায়টাই সবচেয়ে গুরুত্বপূর্ণ।

## ২.১ Multi-tenancy — Hibernate `@Filter`

**ডিক্লেয়ারেশন প্রতি-entity-তে, `BaseEntity`-তে একবারে না।** `BaseEntity`-তে `company_id` ফিল্ডই নেই — কারণ সব entity-র tenant scope থাকে না (যেমন `Permission`, প্ল্যাটফর্ম-লেভেল lookup টেবিল)। তাই প্রতিটা tenant-scoped entity নিজে থেকেই এই boilerplate লেখে:

```java
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
public class Employee extends BaseEntity { ... }
```

একই প্যাটার্ন **৮০টা entity-তে** independently declared।

**প্রতি-রিকোয়েস্টে enable করা** — `TenantFilterInterceptor implements HandlerInterceptor`:
```java
@Override
public boolean preHandle(...) {
    Long companyId = securityUtil.getCurrentCompanyId();
    User user = securityUtil.getCurrentUser();
    if (companyId != null && user != null && user.isTenantUser()) {
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("companyId", companyId);
    }
    return true;
}
```
`afterCompletion`-এ explicit disable করা হয় (try/catch দিয়ে, কারণ response commit হওয়ার পর session already বন্ধ থাকতে পারে)। রেজিস্টার করা আছে `/api/**`-এর জন্য।

**ডিজাইন যুক্তি:**
- Hibernate `@Filter` চলে *session* লেভেলে, তাই প্রতি-রিকোয়েস্টেই on/off করতে হয় — `HandlerInterceptor.preHandle` সঠিক জায়গা কারণ এটা Spring Security auth-এর পরে কিন্তু controller লজিকের আগে চলে
- **`user.isTenantUser()` গার্ড — সবচেয়ে গুরুত্বপূর্ণ security branch:** platform staff (SUPER_ADMIN, SUPPORT_AGENT ইত্যাদি) ইচ্ছাকৃতভাবে এই filter এড়িয়ে যায় — তাদের একাধিক কোম্পানির ডেটা দেখতে হয় (যেমন সাপোর্ট এজেন্ট যেকোনো টিকিট সমাধান করতে পারবে)
- `company_id = :companyId` শর্তটা প্রতিটা generated SQL query-তে বসে যায় — এটা tenant isolation-কে "fail-closed" করে তোলে: কোনো repository query-তে ভুলে companyId filter না বসালেও, entity-তে `@Filter` declared থাকলে ডেটা লিক হবে না

**নিশ্চিত হওয়া ফাঁক:** `JobOffer`, `Candidate`, `Interview`, `TalentPoolCandidate` — এই ৪টা entity-র `company_id` কলাম আছে কিন্তু **কোনো `@Filter` অ্যানোটেশনই নেই**। মানে এই ৪টার tenant isolation সম্পূর্ণ নির্ভর করে repository method-এ ম্যানুয়াল `WHERE company_id = ?` লেখার উপর — একটাও missed query cross-tenant data leak করতে পারে। এটা একটা চমৎকার "spot the vulnerability" ইন্টারভিউ প্রশ্ন।

## ২.২ Authentication / JWT

`JwtService` — HMAC signing (`Keys.hmacShaKeyFor`), claims: `role`, `companyId`, subject(=email), `iat`, `exp`।

**`JwtAuthFilter extends OncePerRequestFilter`** — ভ্যালিডেশন ফ্লো:
1. `Authorization: Bearer` হেডার এক্সট্র্যাক্ট, না থাকলে চেইন কন্টিনিউ (public endpoint যেন কাজ করে)
2. Token valid কিনা চেক (expiry — signature ইতিমধ্যেই parse-এর সময় ভেরিফাই হয়ে যায়, খারাপ হলে exception ছুঁড়ে)
3. ইমেইল দিয়ে `User` খোঁজা, `user.isEnabled()` চেক

**Impersonation branch — সবচেয়ে ইন্টারেস্টিং অংশ:**
```java
if (impersonatedBy != null) {
    String impersonatedRole = jwtService.extractRole(token);
    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + impersonatedRole));
    MDC.put("impersonatedBy", String.valueOf(impersonatedBy));
} else {
    authorities = user.getAuthorities();
}
```
কেন? impersonation token-কে **impersonated tenant role হিসেবে** access দিতে হবে, admin-এর নিজের ডেটাবেজ role হিসেবে না — নাহলে প্রতিটা tenant endpoint platform admin/support agent-কে 403 দিত।

`UsernamePasswordAuthenticationToken(user, companyId, authorities)` — লক্ষ্য করুন, **`companyId` credentials স্লটে বসানো হয়েছে** (আলাদা কাস্টম ফিল্ড না) — এটাই `SecurityUtil.getCurrentCompanyId()` পড়ে ফেরত দেয়।

**`SecurityUtil` ThreadLocal-based না** — এটা প্রতিবার `SecurityContextHolder.getContext().getAuthentication()` পড়ে। কেন এটা গুরুত্বপূর্ণ: `SecurityContextHolder`-এর security context `@Async` থ্রেডে propagate হয় না (ডিফল্ট কনফিগারেশনে) — এই কারণেই `CvScoringService.scoreApplication` (async) `companyId` কে **প্যারামিটার** হিসেবে নেয়, `SecurityUtil` কল করে না।

## ২.৩ Permission System

`PermissionCode` — একটা flat enum, প্রায় ২০০ কোড (`LEAD_VIEW`, `INVOICE_REFUND`, `PAYROLL_APPROVE`...)। ডেটা মডেল: `Permission` (স্ট্যাটিক ক্যাটালগ, বুট-টাইমে `PermissionInitializer` দিয়ে seed হয়), `CustomRole` (tenant-scoped), `RolePermission` (join টেবিল)।

**দুই-স্তরের role মডেল:**
- `User.role` (`Role` enum) — কোর্স-গ্রেইন Spring Security `GrantedAuthority`, কার্যত ব্যবহার হয় না (`@PreAuthorize` প্রায় কোথাও নেই)
- **ফাইন-গ্রেইন authorization — সম্পূর্ণ কাস্টম**, `AuthorizationServiceImpl`:
```java
public boolean hasPermission(PermissionCode permission) {
    if (securityUtil.isImpersonating()) return true;
    if (user.getRole() == SUPER_ADMIN || user.getRole() == SYSTEM_ADMIN)
        return isPlatformPermission(permission);   // হোয়াইটলিস্ট
    if (user.getRole() == COMPANY_OWNER) return true;
    CustomRole role = user.getCustomRole();
    return role != null && rolePermissionRepository.existsByCustomRoleIdAndPermission_Code(role.getId(), permission.name());
}
```

**কেন `@PreAuthorize`/`PermissionEvaluator` না, ম্যানুয়াল চেক?** কারণ permission মডেলে runtime context লাগে (impersonation, owner bypass, platform whitelist) — এগুলো SpEL দিয়ে express করতে হলেও একটা custom `PermissionEvaluator` বানাতেই হতো। তাই টিম একটা সাধারণ injectable service বেছে নিয়েছে — declarative annotation-এর বদলে debugging-এ সহজ, আর mid-method boolean চেকও করা যায় (যা `@PreAuthorize` পারে না, শুধু guard-clause 403)।

## ২.৪ BaseEntity / Auditing / Soft-delete

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted = false")
public abstract class BaseEntity {
    @Id @GeneratedValue Long id;
    @CreatedDate LocalDateTime createdAt;
    @LastModifiedDate LocalDateTime updatedAt;
    boolean deleted = false;
    LocalDateTime deletedAt;
}
```

- **Auditing = Spring Data JPA Auditing**, ম্যানুয়াল `@PrePersist`/`@PreUpdate` না — `@EnableJpaAuditing` + `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate` — সম্পূর্ণ framework-managed
- **Soft-delete = `@SQLRestriction("deleted = false")`** (Hibernate 6-এর deprecated `@Where`-এর replacement), `BaseEntity`-তে একবারই ঘোষিত, তাই সব subclass এটা automatic ইনহেরিট করে। **`@Filter`-এর সাথে মূল পার্থক্য:** `@SQLRestriction` কোনো runtime enable/disable লাগে না — এটা সবসময় সক্রিয়, সব query-তে বাধ্যতামূলক `AND deleted = false` যোগ হয়ে যায়। `@Filter` কেন conditional (request context-নির্ভর), আর soft-delete কেন unconditional — এই পার্থক্যই ব্যাখ্যা করে কেন `TenantFilterInterceptor`-এর মতো কিছু soft-delete-এর জন্য লাগে না।

## ২.৫ Exception Handling

`GlobalExceptionHandler` — `@RestControllerAdvice`। কাস্টম exception hierarchy: `ResourceNotFoundException` (404), `BadRequestException` (400), `DuplicateResourceException` (409), `ForbiddenException` (403 — `checkPermission` এটা ছোঁড়ে), `UnauthorizedException` (401)।

Framework exception-ও translate হয়: `AccessDeniedException`, `BadCredentialsException`, `DataIntegrityViolationException` (409 — soft-delete + unique index interaction-এর একটা বাস্তব ফলাফল: soft-deleted row-এর unique কলাম DB-তে এখনো index-এ থাকে, একই ভ্যালু দিয়ে নতুন রেকর্ড app-level চেক পাস করলেও DB constraint-এ ব্যর্থ হয়)।

সব রেসপন্স একটা uniform envelope শেয়ার করে (`ApiResponse<T>` — success/message/data/timestamp) — সফল হোক বা ব্যর্থ, একই শেপ, যা ফ্রন্টএন্ডে uniform error handling সম্ভব করে।

## ২.৬ Async Processing

`@EnableAsync` আছে, কিন্তু **কোনো কাস্টম ThreadPoolTaskExecutor বিন নেই** — Spring Boot-এর default `applicationTaskExecutor` ব্যবহার হয়। প্রোডাকশন-টিউনিং-এর একটা সম্ভাব্য গ্যাপ (queue capacity, rejection policy কনফিগার করা নেই)।

**`TransactionSynchronizationManager` প্যাটার্ন** (রিক্রুটমেন্ট অধ্যায়ে বিস্তারিত দেখুন) — `afterCommit()` callback দিয়ে transaction commit হওয়ার পরে async কাজ শুরু হয়, race condition এড়াতে।

**Self-injection পিটফল** — `@Autowired @Lazy private CvScoringService self;` — নিজের ক্লাসের ভেতর থেকে `this.method()` কল করলে Spring-এর AOP প্রক্সি বাইপাস হয়ে `@Async`/`@Transactional` দুটোই silently কাজ করা বন্ধ করে দেয়। `self.method()` কল প্রক্সির মধ্য দিয়ে যায়, তাই কাজ করে।

## ২.৭ File Storage

`LocalFileStorageService` — লেয়ার্ড ভ্যালিডেশন (defense-in-depth):
1. Extension whitelist
2. Size cap (avatar ৫MB, document ২০MB — আলাদা)
3. Extension ↔ Content-Type cross-check
4. **Real-image decode check** (`ImageIO.read()` — শুধু extension/MIME না, বাইট সত্যিই ছবি হিসেবে ডিকোড হয় কিনা)
5. Path-traversal গার্ড (`targetLocation.startsWith(uploadPath)`)

ফাইলের নাম সবসময় র‍্যান্ডম-জেনারেটেড (`নাম_userId_random8charUUID.ext`) — কখনো আসল ফাইলনেম না। এটাই `/uploads/**`-এর ১ বছরের aggressive HTTP caching-কে নিরাপদ করে তোলে — একটা re-upload সবসময় নতুন URL পায়, cache-invalidation সমস্যাই নেই।

## ২.৮ সারসংক্ষেপ টেবিল (ইন্টারভিউর আগে রিভিশন)

| বিষয় | মেকানিজম | কোথায় enforce হয় |
|---|---|---|
| Tenant isolation | `@Filter("tenantFilter")` (৮০ entity) | `TenantFilterInterceptor` |
| Tenant isolation gap | ৪টা entity-তে filter নেই | JobOffer, Candidate, Interview, TalentPoolCandidate |
| Auth | JWT HMAC, stateless | `JwtAuthFilter` |
| Fine-grained authz | ম্যানুয়াল `AuthorizationService`, কোনো `@PreAuthorize` না | প্রতিটা controller method-এ explicit কল |
| Auditing | JPA `@EnableJpaAuditing` | `BaseEntity.createdAt/updatedAt` |
| Soft delete | `@SQLRestriction` (সবসময় সক্রিয়) | প্রতিটা generated SQL query |
| Async | `@EnableAsync`, ডিফল্ট executor | `TransactionSynchronizationManager` দিয়ে post-commit dispatch |
| Files | Extension+MIME+decode validation, র‍্যান্ডম নাম | `LocalFileStorageService` |

## ২.৯ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: `@Filter` আর `@SQLRestriction` — দুটোই তো row হাইড করে, তফাত কী?**
উত্তর: `@SQLRestriction` **static** — কম্পাইল-টাইমে নির্দিষ্ট, প্রতিটা query-তে সবসময় প্রযোজ্য, কোনো on/off নেই (soft-delete-এর জন্য উপযুক্ত, কারণ "মুছে ফেলা row দেখানো উচিত না" এই নিয়মের কোনো ব্যতিক্রম নেই)। `@Filter` **dynamic** — runtime-এ session-প্রতি enable/disable করা যায়, parameter নিতে পারে (multi-tenancy-এর জন্য উপযুক্ত, কারণ কে কোন কোম্পানির ডেটা দেখবে তা request-ভিত্তিক পরিবর্তনশীল — এমনকি কিছু ইউজারের (platform staff) জন্য পুরোপুরি বন্ধও করে দিতে হয়)।

**প্রশ্ন: `SecurityContextHolder`-এর ডেটা `@Async` থ্রেডে কেন যায় না, আর এটার সমাধান কী?**
উত্তর: `SecurityContextHolder`-এর ডিফল্ট স্ট্র্যাটেজি (`MODE_THREADLOCAL`) থ্রেড-লোকাল — প্রতিটা থ্রেডের নিজস্ব কপি, প্যারেন্ট থ্রেড থেকে চাইল্ড থ্রেডে (যেমন `@Async` এক্সিকিউটর pool-এর থ্রেড) অটোমেটিক propagate হয় না, যদি না `MODE_INHERITABLETHREADLOCAL` কনফিগার করা হয় (যা এই প্রজেক্টে করা হয়নি)। সমাধান: security context-নির্ভর তথ্য (companyId, userId) async মেথডের **প্যারামিটার** হিসেবে আগেই পাস করে দেওয়া, মেথডের ভেতরে `SecurityUtil` কল না করে।

**প্রশ্ন: `@FilterDef`/`@Filter`-এর `parameters` অংশ কী কাজ করে, `type = Long.class` কেন দরকার?**
উত্তর: `@FilterDef`-এর `parameters = @ParamDef(name = "companyId", type = Long.class)` Hibernate-কে বলে দেয় যে এই filter একটা প্যারামিটার আশা করে, আর সেটার টাইপ কী। এটা ছাড়া Hibernate জানতে পারতো না কীভাবে `session.enableFilter("tenantFilter").setParameter("companyId", value)` কল-এর ভ্যালুটা SQL-এ bind করতে হবে (Long হিসেবে, নাকি String, নাকি অন্য কিছু)। এই টাইপ ইনফরমেশন compile-time-এ ঘোষিত, কিন্তু আসল ভ্যালুটা runtime-এ `TenantFilterInterceptor` সেট করে দেয়।

**প্রশ্ন: `PermissionInitializer implements CommandLineRunner`-এর মতো বুট-টাইম seeder কেন দরকার, কেন সরাসরি ডেটাবেজে insert করা হয় না?**
উত্তর: `PermissionCode` enum-এ নতুন একটা ভ্যালু যোগ করলে (নতুন ফিচারের জন্য), সেটা ডেটাবেজের `permissions` টেবিলেও থাকতে হবে, নাহলে `RolePermission` জয়েন কাজ করবে না। `CommandLineRunner` প্রতিবার অ্যাপ বুট হওয়ার সময় চেক করে — `existsByCode()` না থাকলে insert করে (idempotent — বারবার চালালেও ডুপ্লিকেট হয় না)। এতে ডেভেলপার একটা নতুন enum ভ্যালু যোগ করে ডিপ্লয় করলেই সেটা অটোমেটিক ডেটাবেজে চলে আসে, কোনো আলাদা migration script লেখার দরকার হয় না — `ddl-auto=update`-নির্ভর এই কোডবেসের সাথে সামঞ্জস্যপূর্ণ একটা প্যাটার্ন।

**প্রশ্ন: `GlobalExceptionHandler`-এ `HttpMessageNotReadableException` আলাদা করে হ্যান্ডল করা হয়েছে কেন?**
উত্তর: এই exception ঘটে Jackson deserialization-এর সময়, `@Valid` ভ্যালিডেশন চালু হওয়ারও **আগে** — যেমন একটা dropdown-এর ফাঁকা placeholder ভ্যালু (`""`) একটা enum ফিল্ডে পাঠানো হলে, Jackson সেটা enum-এ কনভার্ট করতেই ব্যর্থ হয়, রিকোয়েস্ট বডি পার্স হওয়ার আগেই। এই exception আলাদাভাবে ধরা না হলে, এটা generic `Exception.class` handler-এ গিয়ে একটা অস্বচ্ছ 500 এরর হয়ে যেত, ইউজার বুঝতেই পারতো না ফর্মে কী ভুল ছিল।
