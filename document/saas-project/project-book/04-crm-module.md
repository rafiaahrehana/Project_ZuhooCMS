# অধ্যায় ৪ — CRM মডিউল

> **কোড লোকেশন:** Backend: `backend/BusinessOS/src/main/java/com/businessos/modules/crm/` (subpackages: `lead`, `opportunity`, `client`, `contact`, `activity`, `tag`, `duplicate`, `dashboard`, `capture`) · Frontend: `frontend/BusinessFlow/src/app/modules/crm/`

## ৪.১ Entity সম্পর্ক — কে কী

```
Lead  --(convert, only when QUALIFIED)-->  Opportunity  --(stage = WON)-->  Client (+ ClientContact)
```

চারটা মূল entity: **Lead** (কাঁচা prospect), **Opportunity** (deal/সুযোগ — কোডে "Opportunity", UI-তে "Deal" শব্দে দেখানো), **Client** (Account — যাদের সাথে ব্যবসায়িক সম্পর্ক পাকা হয়েছে), **ClientContact** (একটা client-এর ভেতরের একজন মানুষ)। সবগুলো Hibernate `@Filter`/`@FilterDef` দিয়ে multi-tenant scoped (`company_id`)।

**সবচেয়ে গুরুত্বপূর্ণ ডিজাইন সিদ্ধান্ত (CRM redesign Phase 1):** পুরনো ফ্লো ছিল Lead→Client→Deal — মানে deal শুরু করার আগেই একটা Client তৈরি করতে হতো। নতুন ফ্লো: **একটা Opportunity ক্লায়েন্ট ছাড়াই থাকতে পারে**, যতক্ষণ না সেটা সত্যিই "জেতা" (WON) হয়। এতে sales team deal নিয়ে কাজ করতে পারে "এটা সত্যিই ক্লায়েন্ট হওয়ার যোগ্য" — এই সিদ্ধান্ত না নিয়েই।

## ৪.২ Lead → Opportunity → Client — কনভার্শন ফ্লো বিস্তারিত

### ধাপ ১: Lead তৈরি ও qualify করা

`LeadServiceImpl.createLead()` — নতুন lead সেভ করে, ইমেইল **আর** ফোন — দুটো দিয়েই duplicate চেক করে (আগে শুধু ইমেইল চেক হতো — CSV import-এ দুটোই চেক হতো কিন্তু manual/API-তে শুধু ইমেইল, ফলে দুইজন sales rep একই walk-in customer-কে আলাদা lead হিসেবে ঢুকিয়ে ফেলতো — এই অসামঞ্জস্য বাগ হিসেবে নোট করা আছে কোড কমেন্টে)।

**Lead qualification সম্পূর্ণ manual** — কোনো automated scoring algorithm নেই। শুধু `LeadStatus` enum (`NEW → CONTACTED → QUALIFIED → DISQUALIFIED`) আর একটা `Priority` enum (LOW/NORMAL/HIGH/URGENT), sales rep নিজে সেট করে।

### ধাপ ২: Opportunity-তে কনভার্ট

`LeadServiceImpl.convertToOpportunity(id, request)` — **শুধু** `status == QUALIFIED` হলে কাজ করে, নাহলে `BadRequestException`। তারপর `OpportunityServiceImpl.createFromLead(leadId, request)` কল হয় — এখানেই clientless deal তৈরি হয়:

```java
Client client = lead.getConvertedClient() != null ? lead.getConvertedClient()
    : request.getClientId() != null ? clientRepository.findById(...)
    : null;  // <- ক্লায়েন্ট ছাড়াই Opportunity তৈরি হতে পারে
```

`sourceLead = lead` সেট হয়, আর lead-এর `converted = true`, `convertedAt = now` হয়। **এই মুহূর্তে কোনো Client বা portal user তৈরি হয় না।**

### ধাপ ৩: Pipeline-এ কাজ — `changeStage()`

এটাই মডিউলের সবচেয়ে জটিল মেথড। প্রতিটা stage move-এ:

- একই stage-এ move করলে no-op হিসেবে block হয়
- `LOST`-এ move করতে হলে `lostReasonCode` (picklist enum) বাধ্যতামূলক; কোড `OTHER` হলে free-text `lostReason`-ও লাগবে
- `probability` অটোমেটিক stage-এর default-এ সেট হয়ে যায় (`QUALIFICATION=15, PRESENTATION=35, PROPOSAL=55, NEGOTIATION=80, WON=100, LOST=0`) — মানে কেউ manually probability override করলেও, পরের কোনো stage-move-এ সেটা মুছে যাবে ডিফল্টে
- Stage বন্ধ (WON/LOST) হলে `actualCloseDate = today`
  - **LOST হলে:** reason সেভ, owner-কে নোটিফিকেশন
  - **WON হলে:** যদি `opportunity.client == null` হয়, `resolveClientForWonOpportunity()` কল হয় (নিচে); তারপর `client.lifetimeValue`-এ deal-এর amount যোগ হয় (`creditClientLifetimeValue`); `AutomationEventPublisher`-এ event পাবলিশ হয় (automation rule hook); owner-কে নোটিফিকেশন
- **একটা আগে-জেতা deal যদি আবার re-open করা হয়** (stage সরানো হয় WON থেকে), `debitClientLifetimeValue()` দিয়ে সেই credit-টা উল্টে দেওয়া হয় — যাতে পরে আবার win করলে double-count না হয়ে যায়

### ধাপ ৪: `resolveClientForWonOpportunity()` — ৩ ধাপের সিদ্ধান্ত

deal "জিতে" গেলে, client-less deal-এর জন্য কোন Client ব্যবহার হবে তা ঠিক করে, এই ক্রমে:

1. **Explicit link** — frontend যদি `linkToExistingClientId` পাঠায় (duplicate-modal-এ ইউজার "existing client ব্যবহার করো" সিলেক্ট করলে), সরাসরি সেই Client লিংক হয়
2. **Explicit force-create** — `forceCreateNewClient = true` হলে duplicate-check পুরোপুরি স্কিপ
3. **Duplicate detection চালানো** — না হলে `DuplicateDetectionService.findPossibleDuplicateClient(...)` চলে (lead-এর company name/email/phone দিয়ে); ম্যাচ পেলে সেটাতেই auto-link (এটা safety-net, সাধারণত frontend আগেই preview করে modal দেখায়)
4. **কোনো ম্যাচ না পেলে** — নতুন Client তৈরি হয় (status ACTIVE, `onboardedAt = today`, কোনো portal login ছাড়াই)
5. `lead.convertedClient` সেট হয় — Lead থেকে শেষ পর্যন্ত কোন Client হলো, সেই ট্রেসেবিলিটি (কমেন্টে লেখা: আগে এটা কোথাও লেখাই হতো না)
6. **`createPrimaryContactFromLead()`** — lead-এর নাম/ইমেইল/ফোন কপি করে নতুন Client-এর একটা `primaryContact=true` ClientContact তৈরি হয়। **কেন এটা জরুরি:** `DuplicateDetectionService` ম্যাচ করে `ClientContact`-এর ইমেইল/ফোন দিয়ে — এই স্টেপ স্কিপ করলে, একই কোম্পানির দ্বিতীয় deal শুধু কোম্পানির নাম টাইপো-সহ ম্যাচ করার চেষ্টা করতো ("Bengal Textiles" বনাম "Bengal Textiles Ltd.") — এবং চুপচাপ ইনভয়েস/সার্ভিস হিস্ট্রি দুই ভাগে ভাগ হয়ে যেত।

### Preview endpoint — মডাল দেখানোর আগেই চেক

`GET /api/crm/opportunities/{id}/won-duplicate-check` → `previewWonDuplicate(id)` — read-only, deal WON করার আগেই duplicate match আছে কিনা জানায় (`null` যদি ইতিমধ্যে client attached থাকে)। এভাবে frontend WON-এ ড্র্যাগ করার সাথে সাথেই, actual commit করার **আগেই** ইউজারকে জিজ্ঞেস করতে পারে।

## ৪.৩ Duplicate Detection — "nudge, not block"

`DuplicateDetectionService` — ইচ্ছাকৃতভাবে fuzzy/trigram matching না, শুধু normalized exact/LIKE matching:
1. কোম্পানির নাম — case-insensitive exact match
2. ইমেইল — exact match, না পেলে **domain fallback** (`@`-এর পরের অংশ দিয়ে `Client.website`-এ LIKE সার্চ)
3. ফোন — exact match

প্রথম যেটা মিলে যায় সেটাই রিটার্ন হয় (নাম → ইমেইল → domain → ফোন — এই priority-তে)। এটা তিন জায়গায় ব্যবহার হয়: Lead তৈরি করার সময়, নতুন Client তৈরি করার সময়, আর Opportunity WON হওয়ার সময়। **সবসময় শুধু warning** — কখনো তৈরি হওয়া আটকায় না, `DuplicateWarningModal` কম্পোনেন্টের নিজস্ব কমেন্টেই লেখা: "রেকর্ডটা ততক্ষণে তৈরি হয়েই গেছে... কখনো 'undo the creation' না।"

## ৪.৪ Frontend — Pipeline Board (drag-and-drop kanban)

`pipeline-board.ts` — Angular CDK `DragDropModule` ব্যবহার করে। ড্র্যাগ শেষ হলে:

- **একই কলামে** ড্রপ হলে শুধু visual reorder (`moveItemInArray`) — কোনো persisted per-stage-order ফিল্ড নেই, তাই এটা শুধু cosmetic
- **ভিন্ন কলামে** ড্রপ হলে `transferArrayItem` দিয়ে optimistic local move, তারপর `move()` কল হয়:
  - টার্গেট `LOST` হলে reason modal খোলে, সাথে সাথে কমিট হয় না
  - টার্গেট `WON` আর deal-এর এখনো clientId না থাকলে, প্রথমে `previewWonDuplicate()` কল হয়; ম্যাচ পেলে modal, না পেলে সরাসরি কমিট
  - preview-check ব্যর্থ হলে **"fail open"** — ব্লক না করে সরাসরি কমিট করে ফেলে (infrastructure hiccup-এর জন্য ইউজারকে আটকে রাখা হয় না)
- `commitChangeStage()` ব্যাকএন্ডে PATCH করে; সফল হোক বা ব্যর্থ, দুই ক্ষেত্রেই `load()` দিয়ে আবার সব ডেটা fetch করে — একটা ব্যর্থ ড্র্যাগ visually "ফিরে আসে" কারণ সার্ভার থেকে সত্যিকারের ডেটা আবার আসে, client-side undo logic দিয়ে না

## ৪.৫ বাকি Frontend Components

| Component | কাজ |
|---|---|
| `leads.ts` | ৬টা quick-view (All/My/Unassigned/High-Priority/Never-Contacted/Stale), CSV import, PDF export, convert-to-opportunity modal, activity timeline, AI summarization |
| `clients.ts` | CRUD, portal-login provisioning সহ create ফর্ম, duplicate-warning wiring |
| `client-detail.ts` | ৩৬০° ভিউ — profile, contacts, activity timeline, linked opportunities, AI summary |
| `tag-manager.ts` | Tag taxonomy-র inline CRUD |
| `pipeline-reports.ts` | Chart.js দিয়ে analytics — forecast (open deals-কে expected-close মাস অনুযায়ী bucket করা, undated deal-দের জন্য আলাদা "No close date" bucket), loss-reason breakdown, ৬-মাসের trend, per-owner sales performance (win rate) |
| `contacts.ts` | সব client-এর সব contact একসাথে দেখার directory (শুধু সার্চ/লিস্ট, নতুন তৈরি এখান থেকে হয় না) |

## ৪.৬ Notification Scheduler

`CrmFollowUpScheduler` — দুটো `@Scheduled` job:
- **প্রতি ৩০ মিনিটে:** যেসব follow-up due হয়ে গেছে তাদের নোটিফাই করে, `followUpNotifiedAt IS NULL` গার্ড দিয়ে একবারই নোটিফাই করে (এটা না থাকলে একটা overdue item দিনে ৪৮ বার নোটিফাই করতো!)
- **প্রতিদিন সকাল ৯টায়:** ৩০ দিন no-contact হলে Lead-কে, ১৪ দিন no-activity হলে Opportunity-কে "stale" মার্ক করে

## ৪.৭ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `lead/LeadServiceImpl.java` | `createLead` (dedup by email+phone), `updateLead` (converted/DISQUALIFIED লক), `filterLeads`, `findStalLeads`, `addActivity`, `summariseLead` (AI), `escapeLikeKeyword` |
| `lead/LeadController.java` | `/api/crm/leads` — CRUD, `/import`, `/pdf`, `/my`, `/search`, `/filter`, quick-view endpoints, `/{id}/convert-to-opportunity` |
| `opportunity/OpportunityServiceImpl.java` | `create`, `createFromLead`, `changeStage` (মূল state machine), `resolveClientForWonOpportunity`, `previewWonDuplicate`, `getPipelineSummary`, `creditClientLifetimeValue`/`debitClientLifetimeValue` |
| `opportunity/OpportunityStage.java` | Stage enum + hardcoded default probability |
| `client/ClientServiceImpl.java` | `create` (duplicate check + optional portal provisioning), `inviteToPortal` (random password, email link), `delete` (open-opportunity guard) |
| `client/ClientContactServiceImpl.java` | `clearPrimaryContact()` — single-primary-per-client এনফোর্স |
| `duplicate/DuplicateDetectionService.java` | নাম → ইমেইল → domain → ফোন — priority-ordered matching |
| `tag/TagServiceImpl.java` | Tag taxonomy CRUD |
| `activity/CrmActivityServiceImpl.java` | `log()`, `logSystemActivity()` (auto-generated timeline), `summarise()` (AI) |
| `dashboard/CrmDashboardServiceImpl.java` | pipeline value, conversion rate, funnel, lead-source breakdown |
| `capture/PublicLeadCaptureController.java` | `/api/public/crm/leads` — একমাত্র unauthenticated CRM endpoint, honeypot ফিল্ড |
| `core/scheduler/CrmFollowUpScheduler.java` | দুটো `@Scheduled` job — follow-up reminder, stale marking |

### Frontend

| ফাইল | কাজ |
|---|---|
| `pipeline-board.ts` | Kanban drag-and-drop, `move()`, `commitChangeStage()`, WON/LOST modal লজিক |
| `leads.ts` | ৬টা quick-view, CSV import, PDF export, convert-modal, activity timeline |
| `clients.ts` | CRUD, portal-provisioning ফর্ম, duplicate-warning wiring |
| `client-detail.ts` | ৩৬০° ভিউ, contacts, timeline, linked opportunities |
| `tag-manager.ts` | Tag inline CRUD |
| `pipeline-reports.ts` | Chart.js — forecast bucket, loss-reason breakdown, ৬-মাসের trend, per-owner performance |
| `contacts.ts` | ক্রস-ক্লায়েন্ট contact directory (শুধু সার্চ/লিস্ট) |

## ৪.৮ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: একটা Opportunity ক্লায়েন্ট ছাড়া থাকতে পারে কেন?**
উত্তর: sales team deal নিয়ে কাজ শুরু করতে চায় সিদ্ধান্ত নেওয়ার আগেই যে এটা পাকা ক্লায়েন্ট রিলেশনশিপ হবে কিনা। যদি প্রতিটা deal-এর জন্য আগে থেকেই একটা Client তৈরি করতে বাধ্য করা হতো, প্রচুর half-finished/lost deal-এর জন্য অপ্রয়োজনীয় Client রেকর্ড জমে যেত ডেটাবেজে।

**প্রশ্ন: Duplicate detection কেন block না করে শুধু warning দেয়?**
উত্তর: false positive-এর কারণে legitimate ব্যবহারকারীকে block করা খারাপ UX। "nudge, not block" — ইউজারকে জানানো হয় সম্ভাব্য duplicate-এর কথা, কিন্তু সিদ্ধান্ত ইউজারের — সে চাইলে সত্যিই নতুন একটা আলাদা রেকর্ড বানাতে পারবে (হয়তো একই কোম্পানির দুইটা ভিন্ন শাখা)।

**প্রশ্ন: drag-and-drop ব্যর্থ হলে UI কীভাবে "আগের অবস্থায়" ফিরে যায়?**
উত্তর: কোনো ক্লায়েন্ট-সাইড undo/rollback লজিক নেই। PATCH কল সফল হোক বা ব্যর্থ — দুই ক্ষেত্রেই `load()` কল হয়ে সার্ভার থেকে আসল ডেটা আবার fetch হয়। এটা simpler এবং bug-প্রুফ — কারণ "optimistic UI-কে কীভাবে সঠিকভাবে undo করবো" এই জটিলতা এড়ানো যায়, খরচ শুধু একটা এক্সট্রা নেটওয়ার্ক কল।

**প্রশ্ন: একই কোম্পানির দ্বিতীয় deal তৈরি হলে duplicate detection কীভাবে সেটা ধরে, যদি কোম্পানির নাম সামান্য টাইপো-সহ ভিন্ন লেখা হয় (যেমন "Bengal Textiles" বনাম "Bengal Textiles Ltd.")?**
উত্তর: এখানেই `createPrimaryContactFromLead()`-এর গুরুত্ব বোঝা যায় — `resolveClientForWonOpportunity()` একটা নতুন Client তৈরি করার সাথে সাথে lead-এর যোগাযোগের তথ্য (ইমেইল, ফোন) দিয়ে একটা primary `ClientContact`-ও তৈরি করে দেয়। `DuplicateDetectionService` মূলত `ClientContact`-এর ইমেইল/ফোনে ম্যাচ করে, শুধু কোম্পানির নামে (যেটা টাইপো-প্রবণ) না — তাই কোম্পানির নাম কিছুটা ভিন্ন লেখা হলেও, একই ইমেইল/ফোন দিয়ে দ্বিতীয় deal সঠিক Client-এর সাথে ম্যাচ হয়ে যায়।

**প্রশ্ন: Lead qualification-এ কোনো automated scoring না থাকা কি একটা দুর্বলতা?**
উত্তর: এটা একটা সচেতন সরলতা-সিদ্ধান্ত, দুর্বলতা না। Automated lead scoring (যেমন engagement-ভিত্তিক পয়েন্ট সিস্টেম) দরকার হয় যখন lead volume অনেক বেশি এবং sales rep-দের priority ঠিক করতে সাহায্য দরকার। এই প্রজেক্টের স্কেলে, sales rep নিজেই manual `LeadStatus`/`Priority` সেট করে যথেষ্ট নমনীয়তা দেয় — ভবিষ্যতে scale বাড়লে, এই manual ফিল্ডগুলোর উপর ভিত্তি করে একটা scoring layer যোগ করা সহজ (existing স্ট্রাকচার ভাঙতে হবে না)।
