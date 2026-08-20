# ZuhooCMS (BusinessOS) — সম্পূর্ণ প্রজেক্ট বই

**উদ্দেশ্য:** এই বইটি লেখা হয়েছে ইন্টারভিউ প্রস্তুতি, ভিডিও প্রেজেন্টেশন, এবং প্রজেক্টটি নিজে থেকে গভীরভাবে বোঝার জন্য। ভাষা মূলত বাংলা, কিন্তু টেকনিক্যাল টার্ম, ক্লাস নাম, মেথড নাম, কোড — এগুলো ইংরেজিতেই রাখা হয়েছে, কারণ ইন্টারভিউতে এভাবেই বলতে হবে।

**স্কোপ সম্পর্কে সৎ কথা:** এই প্রজেক্টে ১২টির বেশি মডিউল, শত শত ব্যাকএন্ড ক্লাস, আর শতাধিক ফ্রন্টএন্ড কম্পোনেন্ট আছে। প্রতিটা getter/setter আলাদা করে ব্যাখ্যা করা বই-আকারে অর্থহীন হতো — তাই এই বই প্রতিটা মডিউলের **architecture, প্রতিটা গুরুত্বপূর্ণ entity, প্রতিটা business-logic method, প্রতিটা controller endpoint, আর প্রতিটা ফ্রন্টএন্ড কম্পোনেন্টের আসল কাজ** — এগুলো নিয়ে বিস্তারিত আলোচনা করে। যেসব জায়গায় কোড সত্যিই ইন্টারেস্টিং (validation, business rule, bug fix, design decision) — সেখানে মেথড বডি পর্যন্ত দেখানো হয়েছে।

## কীভাবে পড়বেন

- **নতুন হলে:** অধ্যায় ১ থেকে শুরু করুন — পুরো architecture-এর একটা ম্যাপ পাবেন।
- **নির্দিষ্ট মডিউল জানতে চাইলে:** সরাসরি সেই অধ্যায়ে যান।
- **ইন্টারভিউর ঠিক আগে:** অধ্যায় ১৩-১৬ (Interview Q&A) থেকে রিভিশন দিন।
- **প্রেজেন্টেশন/ভিডিওর জন্য:** `presentation/` ফোল্ডারে বাংলা ও ইংরেজি — দুটো স্লাইড ডেক আছে।

## সূচিপত্র (Table of Contents)

| অধ্যায় | বিষয় | ফাইল |
|---|---|---|
| ১ | Architecture Overview — টেক স্ট্যাক ও কেন এভাবে বানানো হলো | [01-architecture-overview.md](01-architecture-overview.md) |
| ২ | Backend Core Architecture — Security, Multi-tenancy, Base Patterns | [02-backend-core.md](02-backend-core.md) |
| ৩ | Frontend Core Architecture — Routing, Services, State | [03-frontend-core.md](03-frontend-core.md) |
| ৪ | CRM মডিউল | [04-crm-module.md](04-crm-module.md) |
| ৫ | Company Services / Service Desk মডিউল | [05-servicedesk-module.md](05-servicedesk-module.md) |
| ৬ | Human Resources (Core) মডিউল | [06-hrm-core-module.md](06-hrm-core-module.md) |
| ৭ | Recruitment / ATS মডিউল | [07-recruitment-module.md](07-recruitment-module.md) |
| ৮ | Time & Leave + Payroll মডিউল | [08-attendance-payroll-module.md](08-attendance-payroll-module.md) |
| ৯ | Finance মডিউল (Billing, Accounting, Banking) | [09-finance-module.md](09-finance-module.md) |
| ১০ | IT Assets মডিউল | [10-it-assets-module.md](10-it-assets-module.md) |
| ১১ | Administration + AI মডিউল | [11-administration-ai-module.md](11-administration-ai-module.md) |
| ১২ | Platform Admin / Super Admin মডিউল | [12-platform-admin-module.md](12-platform-admin-module.md) |
| ১৩ | Java Core — Interview Q&A | [13-interview-java.md](13-interview-java.md) |
| ১৪ | Spring Boot — Interview Q&A | [14-interview-spring-boot.md](14-interview-spring-boot.md) |
| ১৫ | Angular — Interview Q&A | [15-interview-angular.md](15-interview-angular.md) |
| ১৬ | এই প্রজেক্ট নিয়ে সরাসরি প্রশ্ন — Interview Q&A | [16-interview-project.md](16-interview-project.md) |

## প্রজেক্ট এক নজরে

- **নাম:** ZuhooCMS (কোডনেম BusinessOS)
- **ধরন:** Multi-tenant B2B SaaS — একই অ্যাপ, আলাদা আলাদা কোম্পানি (tenant) নিজেদের ডেটা নিয়ে ব্যবহার করে
- **Backend:** Java 21, Spring Boot, Maven, PostgreSQL, Hibernate/JPA (`ddl-auto=update`, কোনো Flyway migration নেই)
- **Frontend:** Angular (standalone components, `@if`/`@for` নতুন control-flow syntax), Bootstrap 5 + Bootstrap Icons
- **মডিউল সংখ্যা:** ১২+ (CRM, Service Desk, HRM, Recruitment, Attendance/Leave, Payroll, Finance x৩, IT Assets, Administration, AI, Platform Admin)
