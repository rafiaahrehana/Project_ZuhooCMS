# অধ্যায় ১২ — Platform Admin (Super Admin) মডিউল

> **কোড লোকেশন:** Frontend: `frontend/BusinessFlow/src/app/modules/platform-admin/` + `.../support/` · Backend: ছড়ানো — `modules/company`, `shared/subscription`, `shared/feature`, `auth/platformuser`, `auth/impersonation`, `modules/support/*` (কোনো একক `modules/platform` প্যাকেজ নেই)

## ১২.১ Company Lifecycle

`CompanyStatus` — সঠিক ৫টা অবস্থা:
```
PENDING_VERIFICATION → TRIAL (১৪ দিন) → ACTIVE (পেইড) 
                                    ↘ SUSPENDED (মেয়াদ শেষ, read-only) → DEACTIVATED (স্থায়ীভাবে বন্ধ)
```

**`changeStatus()`** — শুধু status বদলায় না, active service request থাকলে block করে, আর একটা **audit log** লেখে। কোড কমেন্টে সরাসরি লেখা: পুরো একটা tenant suspend করা "সবচেয়ে বড় blast-radius অ্যাকশনগুলোর একটা" যা platform admin করতে পারে, আর আগে কোনো audit trail ছাড়াই এটা হতো।

## ১২.২ Subscription — Stripe না, SSLCommerz

**গুরুত্বপূর্ণ ভুল-ধারণা ভাঙা:** এই প্রজেক্টে কোথাও **Stripe integration নেই**। পেমেন্ট গেটওয়ে হলো **SSLCommerz** (বাংলাদেশি গেটওয়ে)। কোনো recurring auto-charge job নেই — বিলিং দুই ভাবে হয়:
1. **Admin-assigned:** প্ল্যাটফর্ম admin ম্যানুয়ালি `amountPaid`/`transactionRef` টাইপ করে একটা কোম্পানিকে প্ল্যানে বসায়
2. **Self-service checkout:** SSLCommerz দিয়ে পেমেন্ট সফল হলে `applyPaidPlanUpgrade()` কল হয়

**Expiry enforcement:**
- `SubscriptionScheduler` — প্রতিদিন `subscriptionEnd` পার হওয়া কোম্পানিদের `SUSPENDED` করে দেয়
- **`SubscriptionEnforcementFilter`** (একটা সাধারণ servlet filter, guard annotation না) — GET request-গুলো ছাড়া বাকি সব write request-এ, suspended/expired কোম্পানির জন্য 403 রিটার্ন করে। কিন্তু billing-lapse কোম্পানিদের জন্য একটা narrow allow-list আছে (`/api/support/tickets`, `/api/invoices`, `/api/payment`, `/api/wallet`) — যাতে তারা টাকা দিয়ে নিজেদের suspend থেকে বের হতে পারে। **Admin-suspended কোম্পানি এই ছাড় পায় না** — তারা payment endpoint দিয়েও পালাতে পারবে না।

## ১২.৩ Feature Flags — একটা আবিষ্কার-যোগ্য গ্যাপ

`FeatureFlag` entity-তে **কোনো `companyId` কলামই নেই** — মানে এগুলো per-tenant toggle না, পুরো প্ল্যাটফর্ম-জোড়া সুইচ (`ENABLE_BKASH`, `ENABLE_NAGAD`, `ENABLE_REFERRAL`, `ENABLE_AUTO_ASSIGN`, `ENABLE_PACKAGES`, `MAINTENANCE_MODE`)।

**একটা সত্যিকারের ফাঁক:** পুরো ব্যাকএন্ড গ্রেপ করে দেখা গেছে — `FeatureFlagRepository`/`flagKey` **কোথাও ব্যবহৃত হয় না** নিজের controller/entity ছাড়া। কোনো filter, interceptor, বা গার্ড কখনো flag-এর মান পড়ে কোনো আচরণ নিয়ন্ত্রণ করে না — এমনকি `MAINTENANCE_MODE`-ও না, যার description বলছে "সব রিকোয়েস্ট ব্লক করে"! এটা একটা admin-manageable catalog আছে, কিন্তু কোথাও enforce হয় না — একটা চমৎকার "কীভাবে wire করা উচিত ছিল বনাম বর্তমানে কী আছে" আলোচনার বিষয়।

## ১২.৪ Revenue Reporting

কোনো আলাদা "Revenue" মডিউল নেই — `DashboardServiceImpl.getPlatformSummary()` লাইভ কম্পিউট করে:
```java
totalRevenue = subscriptionHistoryRepository.sumTotalRevenue()
```
মানে revenue = `SubscriptionHistory.amountPaid`-এর যোগফল — ঠিক সেই রেকর্ডগুলো যা `changePlan()`/`applyPaidPlanUpgrade()` লেখে।

## ১২.৫ Platform Expenses — companyId = null

Platform-এর নিজের operating cost একই `Expense` entity ব্যবহার করে, কিন্তু **`companyId = null`** দিয়ে — কোড কমেন্টে স্পষ্ট: "তারা কোনো tenant-এর অংশ না"। **Expense approval GL-integrated:**
- `approveExpense()` → Dr Expense / Cr Accounts Payable
- `rejectExpense()` (approve-এর পরে) → বিপরীত এন্ট্রি (reverse)
- `markAsPaid()` → Dr AP / Cr Cash

## ১২.৬ Support Desk — Ticket Lifecycle

```
NEW → OPEN → IN_PROGRESS → WAITING/ON_HOLD → RESOLVED → CLOSED
                                          ↘ REOPENED
```

`TicketType`: `PLATFORM_SUPPORT` (tenant staff-এর নিজেদের BusinessOS নিয়ে সমস্যা) বনাম `CUSTOMER_SUPPORT` (একজন CRM Client তাদের নিজের কোম্পানির বিরুদ্ধে টিকিট তোলে)।

**Agent-এর concurrent-ticket cap সত্যিই enforce করা:** `assignToAgent()` চেক করে `countByAssignedToAgentIdAndStatusNotIn(...) >= agent.maxConcurrentTickets` — কোড কমেন্টে লেখা এই ক্যাপ আগে "একটা সাজানো ফিল্ড ছিল, কোনো backing logic ছাড়া" — এখন সত্যিই enforce হয়।

**SLA policy** priority অনুযায়ী keyed (CRITICAL/HIGH/MEDIUM/LOW), plan/tier অনুযায়ী না।

## ১২.৭ Impersonation ("Access Company")

`ImpersonationServiceImpl.startImpersonation()` — একটা বিশেষ JWT জেনারেট করে যেখানে platform admin **সেই কোম্পানির owner-এর মতো** টোকেন পায় (`generateImpersonationToken`)। প্রতিটা impersonation session আলাদা `ImpersonationAuditLog`-এ লগ হয়, যেখানে একটা **কারণ (reason) লেখা বাধ্যতামূলক**।

## ১২.৮ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: Feature Flag থাকা সত্ত্বেও এটা কোথাও enforce হয় না কেন — এটা কি বাগ?**
উত্তর: এটা সম্পূর্ণ হওয়া ফিচার না, বরং একটা আংশিকভাবে তৈরি ইনফ্রাস্ট্রাকচার — admin UI (toggle করা যায়) তৈরি হয়ে গেছে, কিন্তু কনজিউমার সাইড (কোথাও `if (featureFlagService.isEnabled("X"))` চেক করা) এখনো লেখা হয়নি। এটা "বাগ" না, বরং একটা incomplete feature — একটা ভালো ইন্টারভিউ উত্তর হলো: "আমি কীভাবে এটা wire করবো" ব্যাখ্যা করা — একটা Spring `@Aspect` বা filter দিয়ে, যেটা রিকোয়েস্টের আগে ফ্ল্যাগ চেক করে।

**প্রশ্ন: Impersonation token-এ কেন admin-এর নিজের role না দিয়ে impersonated কোম্পানির owner-এর role বসানো হয়?**
উত্তর: কারণ tenant-scoped সব endpoint `COMPANY_OWNER`-এর মতো permission আশা করে। যদি admin নিজের `SUPER_ADMIN` role নিয়েই impersonate করতো, তাহলে প্রতিটা tenant endpoint-এ 403 পেত (কারণ permission system tenant role-ভিত্তিক, platform role না)। impersonated role ব্যবহার করে সিস্টেম "as if logged in as the owner" আচরণ করে, কিন্তু `impersonatedBy`/`impersonationSessionId` claim দিয়ে এটা সবসময় ট্রেসেবল থাকে যে আসলে কে করছে।

## ১২.৯ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `modules/company/CompanyServiceImpl.java` | `changeStatus` (audit log সহ suspend/activate), `changePlan`, `deactivate` |
| `shared/subscription/SubscriptionScheduler.java` | Expiry enforcement, reminder ইমেইল |
| `shared/subscription/SubscriptionEnforcementFilter.java` | রানটাইমে suspended/expired কোম্পানি ব্লক করা |
| `shared/feature/FeatureFlagController.java` | Flag CRUD/toggle (enforcement কোথাও নেই — dead-code finding) |
| `auth/platformuser/PlatformUserService.java` | স্টাফ অ্যাকাউন্ট CRUD, `PLATFORM_ROLES` whitelist |
| `modules/support/ticket/SupportTicketServiceImpl.java` | Ticket lifecycle, `requireActiveAgent` (concurrent cap) |
| `modules/support/sla/SLAPolicy.java` | Priority-keyed SLA পলিসি |
| `auth/impersonation/ImpersonationServiceImpl.java` | `startImpersonation` — বিশেষ JWT + audit log |

### Frontend

| ফাইল | কাজ |
|---|---|
| `companies.ts` | Company list, status/plan change, `doImpersonate()` |
| `subscription-management.ts` | Plan catalog CRUD, revenue ড্যাশবোর্ড |
| `feature-flags.ts` | Toggle UI (কোনো consumer নেই ব্যাকএন্ডে) |
| `platform-users.ts` | স্টাফ অ্যাকাউন্ট ম্যানেজমেন্ট |
| `support/components/tickets/tickets.ts` | Role-aware ticket queue (manager/agent/admin ভিন্ন ভিউ) |

## ১২.১০ আরও ইন্টারভিউ প্রশ্ন

**প্রশ্ন: কেন Platform Expenses একই `Expense` entity পুনর্ব্যবহার করে, আলাদা entity বানানো হলো না কেন?**
উত্তর: Platform expense আর tenant expense-এর গঠন (title, amount, category, approval workflow, GL posting) হুবহু একই — শুধু "কার জন্য" এই প্রশ্নের উত্তর ভিন্ন (একটা tenant-এর, আরেকটা প্ল্যাটফর্মের নিজের)। `companyId = null` দিয়ে এই পার্থক্য প্রকাশ করাটা একটা DRY (Don't Repeat Yourself) সিদ্ধান্ত — একই CRUD লজিক, approval workflow, GL-posting কোড দুইবার লেখার দরকার নেই। ঝুঁকি: `companyId = null`-কে সবসময় সাবধানে হ্যান্ডল করতে হবে (tenant filter-এ যেন accidentally leak না হয়) — এটাই একমাত্র জায়গা যেখানে "no company" একটা বৈধ, ইচ্ছাকৃত অবস্থা।

**প্রশ্ন: Support ticket-এর permission model কেন বাকি সিস্টেমের মতো `AuthorizationService.checkPermission()` ব্যবহার করে না, শুধু Spring role-check ব্যবহার করে?**
উত্তর: Support ticket পুরোপুরি প্ল্যাটফর্ম-স্টাফ + কোম্পানি-owner-এর মধ্যে সীমাবদ্ধ একটা ফিচার — কোনো tenant custom-role এখানে involved না (একজন সাধারণ employee কখনো টিকিট assign/resolve করে না)। যেহেতু involved role-সেট ছোট এবং ফিক্সড (`SUPPORT_AGENT`, `SUPPORT_MANAGER`, `COMPANY_OWNER`, platform admin), সরাসরি `@PreAuthorize("hasRole(...)")` যথেষ্ট এবং সরল — পুরো `CustomRole`/`RolePermission` সিস্টেম টানার দরকার নেই এমন একটা সীমিত-scope ফিচারের জন্য।
