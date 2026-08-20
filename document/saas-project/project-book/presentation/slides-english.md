---
marp: true
paginate: true
---

# ZuhooCMS (BusinessOS)
### A Multi-Tenant B2B SaaS Platform
Spring Boot + Angular

---

## What It Is

- An **all-in-one** platform for small-to-mid businesses
- CRM + HR/Recruitment + Finance + Service Desk + IT Assets — in one app
- **Multi-tenant:** one codebase, one database — every company's data fully isolated

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, PostgreSQL |
| Frontend | Angular 22 (standalone components) |
| Auth | JWT (stateless) |
| AI | Gemini / Claude / OpenAI / Groq |
| Payment | SSLCommerz |

---

## 12+ Modules

CRM · Service Desk · HR Core · **Recruitment/ATS** · Attendance & Leave · Payroll · **Finance** (split 3 ways) · IT Assets · Administration · **AI** · Platform Admin

---

## Core Architecture #1 — Multi-Tenancy

```java
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
```

- Declared on 80 entities
- Enabled per-request (`TenantFilterInterceptor`)
- Platform staff (support agents) bypass this filter — can see across companies

---

## Core Architecture #2 — Permission System

- Two tiers: `User.role` (coarse) + `CustomRole` (fine-grained)
- **No `@PreAuthorize`** — because impersonation/owner-bypass logic is complex
- `AuthorizationService.checkPermission()` — manual, explicit, debuggable

---

## Recruitment/ATS — The Most Complex Module

- 13-step status pipeline
- **CV Parsing + ATS Match Scoring** — PDFBox/POI extraction, weighted category scoring
- **Never auto-rejects** — just a signal, assists recruiter judgment

---

## A Real Bug — Claude Model ID

```java
CLAUDE_SONNET("claude-sonnet-4-6")  // ❌ this model doesn't exist!
```

- Two companies configured Claude, **zero successful conversations**
- Caught via live database evidence, not just code review
- Fix: corrected to the real model ID

---

## A Real Bug — Invisible CSS

```css
.sidebar-nav .nav-link.active {
  background: #ffffff !important;  /* white on white! */
}
```

- `!important` was winning over the component's correct style
- Two contradictory comments back to back — a classic dead-code trap

---

## Wallet vs Payment Receipt

| | Payment Receipt | Wallet Top-up |
|---|---|---|
| Posts to GL? | ✅ Yes | ❌ No |
| Why | Client's money, official books | Prepaid in-app balance |

---

## AI Module — Not an Agent, Augmented Generation

- No `tools`/`function_call` parameter in any provider client
- 3 features (Announcement/Holiday/Leave-Policy) genuinely pull real data into the prompt
- Everything else: the user types the context themselves

---

## Frontend — One Surprise

- **Zero NgModules** — everything standalone
- In Angular 22, `OnPush` is now numerically the **default**
- No NgRx — `BehaviorSubject` + plain services are enough at this scale

---

## Thank You

Full book: `docs/project-book/`
Every chapter has real code, real bugs, real interview Q&A
