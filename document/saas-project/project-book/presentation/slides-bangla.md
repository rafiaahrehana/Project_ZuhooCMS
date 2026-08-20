---
marp: true
paginate: true
---

# ZuhooCMS (BusinessOS)
### একটা Multi-Tenant B2B SaaS প্ল্যাটফর্ম
Spring Boot + Angular

---

## প্রজেক্ট কী

- ছোট-মাঝারি ব্যবসার জন্য **all-in-one** প্ল্যাটফর্ম
- CRM + HR/Recruitment + Finance + Service Desk + IT Assets — একই জায়গায়
- **Multi-tenant:** একই কোড, একই ডেটাবেজ — প্রতিটা কোম্পানির ডেটা সম্পূর্ণ আলাদা

---

## Tech Stack

| স্তর | প্রযুক্তি |
|---|---|
| Backend | Java 21, Spring Boot, PostgreSQL |
| Frontend | Angular 22 (standalone components) |
| Auth | JWT (stateless) |
| AI | Gemini / Claude / OpenAI / Groq |
| Payment | SSLCommerz |

---

## ১২+ মডিউল

CRM · Service Desk · HR Core · **Recruitment/ATS** · Attendance & Leave · Payroll · **Finance** (৩ ভাগে) · IT Assets · Administration · **AI** · Platform Admin

---

## মূল স্থাপত্য নীতি ১ — Multi-Tenancy

```java
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
```

- ৮০টা entity-তে declared
- প্রতি-রিকোয়েস্টে enable হয় (`TenantFilterInterceptor`)
- Platform staff (support agent) এই filter বাইপাস করে — একাধিক কোম্পানি দেখতে পারে

---

## মূল স্থাপত্য নীতি ২ — Permission System

- দুই স্তর: `User.role` (coarse) + `CustomRole` (fine-grained)
- **কোনো `@PreAuthorize` না** — কারণ impersonation/owner-bypass লজিক complex
- `AuthorizationService.checkPermission()` — ম্যানুয়াল, explicit, ডিবাগযোগ্য

---

## Recruitment/ATS — সবচেয়ে জটিল মডিউল

- ১৩-ধাপের status pipeline
- **CV Parsing + ATS Match Scoring** — PDFBox/POI দিয়ে extract, weighted category scoring
- **কখনো auto-reject না** — শুধু signal, recruiter-এর judgment-কে সাহায্য করে

---

## একটা রিয়েল বাগ — Claude Model ID

```java
CLAUDE_SONNET("claude-sonnet-4-6")  // ❌ এই মডেলটাই নেই!
```

- দুটো কোম্পানি Claude configure করেছিল, **একটাও সফল conversation নেই**
- Live database data দিয়ে ধরা পড়েছে, শুধু code review-তে না
- Fix: সঠিক model ID-তে বদলানো

---

## একটা রিয়েল বাগ — Invisible CSS

```css
.sidebar-nav .nav-link.active {
  background: #ffffff !important;  /* সাদার উপর সাদা! */
}
```

- `!important` জিতে যাচ্ছিল component-এর সঠিক স্টাইলের বিরুদ্ধে
- দুটো contradictory comment একসাথে — dead code-এর ক্লাসিক উদাহরণ

---

## Wallet বনাম Payment Receipt

| | Payment Receipt | Wallet Top-up |
|---|---|---|
| GL-এ যায়? | ✅ হ্যাঁ | ❌ না |
| কেন | ক্লায়েন্টের টাকা, অফিসিয়াল হিসাব | প্রি-পেইড ইন-অ্যাপ ব্যালেন্স |

---

## AI Module — Agent না, Augmented Generation

- কোনো `tools`/`function_call` parameter নেই কোনো provider client-এ
- ৩টা ফিচার (Announcement/Holiday/Leave-Policy) সত্যিই real ডেটা টেনে prompt-এ বসায়
- বাকি সব: ইউজার নিজেই প্রম্পটে context লেখে

---

## Frontend — একটা চমক

- **শূন্য NgModule** — সব standalone
- Angular 22-এ `OnPush` এখন numerically **default**
- কোনো NgRx না — `BehaviorSubject` + plain service যথেষ্ট এই স্কেলে

---

## ধন্যবাদ

সম্পূর্ণ বই: `docs/project-book/`
প্রতিটা অধ্যায়ে real code, real bug, real interview Q&A
