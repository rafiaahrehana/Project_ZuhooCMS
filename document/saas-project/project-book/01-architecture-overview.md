# অধ্যায় ১ — Architecture Overview

## ১.১ প্রজেক্ট কী

**ZuhooCMS** (কোডনেম **BusinessOS**) — একটা multi-tenant B2B SaaS প্ল্যাটফর্ম, ছোট-মাঝারি ব্যবসার জন্য একসাথে CRM, HR, Recruitment, Finance, Service Desk, IT Asset Management — সব একই জায়গায়। "Multi-tenant" মানে একই কোড, একই ডেটাবেজ, একই সার্ভার — কিন্তু প্রতিটা কোম্পানি (tenant) নিজেদের ডেটা শুধু নিজেরাই দেখে, একে অপরের ডেটা কখনো দেখতে পায় না (`company_id` দিয়ে isolated)।

## ১.২ Tech Stack

| স্তর | প্রযুক্তি |
|---|---|
| Backend Language | Java 21 |
| Backend Framework | Spring Boot (Spring MVC, Spring Security, Spring Data JPA) |
| Build Tool | Maven |
| Database | PostgreSQL |
| ORM | Hibernate/JPA — `hibernate.ddl-auto=update` (কোনো Flyway/Liquibase migration নেই) |
| Frontend Framework | Angular 22 (standalone components, `@if`/`@for` নতুন control-flow syntax) |
| Frontend Build | esbuild (`@angular/build:application`) |
| CSS | Bootstrap 5 + Bootstrap Icons, কাস্টম `--bos-*` CSS variable টোকেন সিস্টেম |
| Auth | JWT (stateless), HMAC-signed |
| PDF Generation | openhtmltopdf, PDFBox (টেক্সট এক্সট্র্যাকশনের জন্য) |
| Payment Gateway | SSLCommerz (Stripe না — বাংলাদেশি গেটওয়ে) |
| AI Integration | Gemini (ডিফল্ট), Claude, OpenAI, Groq — কনফিগারেবল প্রোভাইডার |
| Chart | Chart.js / ng2-charts |

## ১.৩ কেন `ddl-auto=update`, কেন Flyway না

এই প্রজেক্টে কোনো ফরমাল migration টুল নেই — Hibernate নিজে থেকে entity ক্লাস দেখে টেবিল/কলাম তৈরি/আপডেট করে। এর একটা গুরুত্বপূর্ণ সীমাবদ্ধতা: **`ddl-auto=update` কখনো একটা বিদ্যমান, ডেটাযুক্ত টেবিলে `NOT NULL` কলাম যোগ করতে পারে না** (পুরনো রো-গুলোর জন্য কোনো ডিফল্ট ভ্যালু নেই)। এই কারণেই এই কোডবেসে নতুন যোগ করা প্রায় সব ফিল্ড **nullable** — Performance Review-এর `promotionRecommendation`, Recruitment-এর ATS ফিল্ডগুলো, সবকিছুতেই এই প্যাটার্ন দেখা যায়। কোনো বড় স্কিমা পরিবর্তন দরকার হলে (যেমন Candidate entity আলাদা করা), একটা `ApplicationRunner`-ভিত্তিক one-time data-migration ক্লাস লেখা হয়েছে, বুট-টাইমে idempotent-ভাবে চলে।

## ১.৪ প্রজেক্ট স্ট্রাকচার

```
backend/BusinessOS/src/main/java/com/businessos/
├── auth/           — ইউজার, রোল, JWT, ইম্পারসোনেশন
├── core/            — BaseEntity, স্কেজুলার, ইন্টারসেপ্টর
├── security/         — JwtService, JwtAuthFilter, SecurityUtil
├── shared/          — এক্সসেপশন, ফাইল স্টোরেজ, নোটিফিকেশন, সাবস্ক্রিপশন, ফিচার ফ্ল্যাগ
└── modules/
    ├── crm/            — Lead, Opportunity, Client, Contact
    ├── servicedesk/     — Service Catalog, Workflow, Request
    ├── hrm/
    │   ├── employee, department, designation, performance
    │   ├── recruitment/   — Candidate, JobApplication, ATS
    │   ├── attendance/    — Attendance, Leave, Shift, Biometric
    │   ├── salary, payroll
    ├── finance/          — Invoice, Expense, Journal, GL
    ├── itam/ (+ hrm/asset) — Hardware, Software License
    ├── company/          — Company (tenant), Subscription
    ├── ai/               — AI provider config, Chat/Generate
    └── support/          — Ticket, SLA, Agent

frontend/BusinessFlow/src/app/
├── core/         — services, guards, interceptors (cross-cutting)
├── shared/       — reusable components, directives, pipes
└── modules/      — ঠিক ব্যাকএন্ডের মতোই মডিউল-ভিত্তিক ভাঙা
```

## ১.৫ একটা রিকোয়েস্ট কীভাবে যাত্রা করে (End-to-End)

ধরুন একজন ইউজার "Employees" পেজে একটা নতুন employee তৈরি করছে:

1. **Frontend:** `Employees` কম্পোনেন্ট ফর্ম সাবমিট করে `EmployeeService.create()` কল করে, যেটা `ApiService.post()`-এর উপর বিল্ড
2. **HTTP Interceptor:** `authInterceptor` `Authorization: Bearer <token>` হেডার যোগ করে
3. **Spring Security Filter Chain:** `JwtAuthFilter` টোকেন ভ্যালিডেট করে, `SecurityContextHolder`-এ `User` + `companyId` বসায়
4. **`TenantFilterInterceptor`:** Hibernate `tenantFilter` চালু করে, `companyId` প্যারামিটার সেট করে
5. **Controller:** `EmployeeController.create()` — প্রথমেই `AuthorizationService.checkPermission(EMPLOYEE_CREATE)` চেক করে
6. **Service:** `EmployeeServiceImpl.create()` — বিজনেস লজিক (ইমেইল ইউনিকনেস, portal user তৈরি, ইত্যাদি)
7. **Repository → Hibernate:** `save()` কল হয়, Hibernate অটোমেটিক `company_id` বসিয়ে দেয় (entity-তে সেট করা থাকে) — আর `@Filter`/`@SQLRestriction` পরবর্তী যেকোনো query-তে স্বয়ংক্রিয়ভাবে যুক্ত হয়
8. **Response:** `ApiResponse<EmployeeResponse>` উইন্ডো — uniform envelope, `GlobalExceptionHandler` কোনো এরর হলে একই শেপে এরর রিটার্ন করবে
9. **Frontend:** `errorInterceptor` এরর হলে toast দেখায়; সফল হলে কম্পোনেন্ট `cdr.markForCheck()` কল করে UI আপডেট করে

## ১.৬ মডিউল-বিভাজনের দর্শন

সাইডবার-রিঅর্গানাইজেশনের একটা কমিট মেসেজে (যেটা এই বইয়ের রেফারেন্সও) স্পষ্ট লেখা আছে ক্রম কীভাবে ঠিক হয়েছে: **"যতবার খোলা হয়, তারপর কাজ যেভাবে আসলে ঘটে সেই ক্রমে"** — Dashboards, CRM (জেতো), Service Desk (ডেলিভার করো), People (staff করো), Time & Leave (রেকর্ড করো), Payroll (পে করো), তারপর Billing/Treasury/Accounting, IT Assets, Administration, Platform Support সবার শেষে।

Finance-কে তিনটা আলাদা গ্রুপে ভাঙা হয়েছিল কারণ ১৭টা আইটেমের একটা মনোলিথিক গ্রুপ "স্ক্রলের ভেতরে স্ক্রল" হয়ে গিয়েছিল — দৈনন্দিন ডকুমেন্ট (Invoice) আর ত্রৈমাসিক কনফিগারেশন (Fiscal Years) একসাথে পুঁতে গিয়েছিল। এই ধরনের ডিজাইন সিদ্ধান্তগুলো এই বইয়ে প্রতিটা মডিউলের শুরুতে ব্যাখ্যা করা আছে।

## ১.৭ পরের অধ্যায়গুলো কীভাবে সাজানো

প্রতিটা মডিউল-অধ্যায় একই কাঠামো অনুসরণ করে:
1. **এই মডিউল কী করে** — এক-দুই বাক্যে
2. **Data Model** — entity, গুরুত্বপূর্ণ ফিল্ড, সম্পর্ক
3. **Service Layer** — মূল business-logic মেথড, real code snippet সহ
4. **Frontend Components** — প্রতিটা কম্পোনেন্টের কাজ
5. **ইন্টারভিউ প্রশ্ন** — সেই মডিউল থেকে সম্ভাব্য প্রশ্নোত্তর

Cross-cutting architecture (Security, Multi-tenancy, State Management) আলাদা অধ্যায়ে (২, ৩) — কারণ এগুলো কোনো একটা মডিউলের অংশ না, সব মডিউলই এগুলোর উপর নির্ভর করে।
