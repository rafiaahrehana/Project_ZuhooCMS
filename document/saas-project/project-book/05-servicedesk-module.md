# অধ্যায় ৫ — Company Services / Service Desk মডিউল

> **কোড লোকেশন:** Backend: `backend/BusinessOS/src/main/java/com/businessos/modules/servicedesk/` · Frontend: `frontend/BusinessFlow/src/app/modules/servicedesk/`

## ৫.১ কনসেপ্ট — Category, Service, Package, Template — কে কী

এখানে একটা কনফিউশন হতে পারে কারণ দুইটা আলাদা "template" কনসেপ্ট আছে:

- **`ServiceCategory`** — শুধু গ্রুপিং/ট্যাক্সোনমি (যেমন "Legal & Compliance")
- **`CompanyService`** — আসল বিক্রয়যোগ্য catalog item (যেমন "Trade License Registration"), নিজের দাম (`price`, `priceType`: FIXED/HOURLY/DAILY/MONTHLY/YEARLY/CUSTOM) আছে। একটা `Category`-র অন্তর্গত, আর ঐচ্ছিকভাবে একটা `WorkflowTemplate` (আসল রানটাইম স্টেজ ইঞ্জিন) এবং একটা `ServiceTemplate` (ব্লুপ্রিন্ট) সাথে যুক্ত থাকতে পারে
- **`ServicePackage`** — একাধিক `CompanyService`-এর bundle, নিজস্ব দাম (বা `getEffectivePrice()` দিয়ে সদস্য সার্ভিসগুলোর দাম যোগ করে discount বাদ দিয়ে হিসাব), `billingCycle`, আর `requestQuota` (null মানে unlimited)
- **`ServiceTemplate`** — একটা ডিজাইন-টাইম ব্লুপ্রিন্ট: `formFields`, `requiredDocuments`, আর `workflowStages` (নেস্টেড) নিয়ে গঠিত

**গুরুত্বপূর্ণ dead-code পয়েন্ট (interview-এ ভালো প্রশ্ন):** `ServiceTemplate`-এর ভেতরের `workflowStages` (TemplateWorkflowStage) ফ্রন্টএন্ডের Templates কম্পোনেন্টে এডিট করা যায়, কিন্তু **`ServiceRequestServiceImpl` কখনো এটা পড়েই না।** আসল রানটাইম স্টেজ ইঞ্জিন সম্পূর্ণ আলাদা একটা entity — `WorkflowTemplate`/`WorkflowStage` — যেটা `CompanyService.workflowTemplate`-এর সাথে যুক্ত। এটা একটা duplicated modeling concept, কার্যত ব্যবহার হয় না।

## ৫.২ `ServiceRequest` — State Machine

```
PENDING → QUOTATION_PENDING → ASSIGNED → IN_PROGRESS → WAITING_CLIENT → UNDER_REVIEW → COMPLETED
                                                                                  ↘ REJECTED / CANCELLED / RESUBMITTED
```

মূল লজিক `ServiceRequestServiceImpl`-এ:

- **create():** নতুন request সবসময় `PENDING`-এ শুরু হয়। `agreedPrice > 0` হলে auto-invoice তৈরি ও পাঠানো হয়।
- **submitQuotation() / acceptQuotation() / rejectQuotation():** quotation-ভিত্তিক service-এর জন্য দাম নেগোশিয়েশন। Accept করলে `agreedPrice = quotationAmount` সেট হয়ে আবার `PENDING`-এ ফেরে, তারপর ইনভয়েস তৈরি হয়। `quotationValidUntil` পার হয়ে গেলে auto-expire হয়ে যায়।
- **assign():** `assignedEmployee` সেট হয়, স্ট্যাটাস `ASSIGNED`। কিন্তু যদি এখনো `PENDING`-এ থাকে, সব বাধ্যতামূলক document upload হয়েছে কিনা আগে যাচাই হয় (`validateRequiredDocuments()`)।
- **changeStatus(target):** জেনেরিক transition endpoint। Terminal status (`COMPLETED/REJECTED/CANCELLED`)-এর জন্য `SERVICE_REQUEST_CLOSE` permission লাগে; বাকিতে `SERVICE_REQUEST_APPROVE`। `COMPLETED`-এ যেতে হলে: (১) কোনো Task যদি `COMPLETED/CANCELLED` না হয়ে থাকে, block; (২) কোনো `StageApproval` যদি এখনো `PENDING` থাকে, block।
- **cancel():** একবার employee assign হয়ে গেলে client নিজে cancel করতে পারে না। Subscription quota রিলিজ হয়, ইনভয়েস cancel/refund হয়।
- **permanentlyClosed = true** হয়ে গেলে (`guardNotClosed()`) — update, changeStatus, assign, advanceStage, cancel — কিছুই আর করা যায় না।
- প্রতিটা transition `RequestStatusHistory`-তে লগ হয় (oldStatus, newStatus, reason, changedBy) — একটা সম্পূর্ণ audit trail।

## ৫.৩ Workflow Engine — Stage, Approval, SLA

### Stage progression — `advanceStage()`

1. `CompanyService.workflowTemplate`-এর ordered `WorkflowStage` লিস্ট থেকে পরের স্টেজ বের করে
2. যদি সেই স্টেজে `requiresApproval = true` হয় আর এখনো কোনো `APPROVED` `StageApproval` না থাকে, একটা `PENDING` approval তৈরি হয় **আলাদা transaction-এ** (`REQUIRES_NEW` propagation দিয়ে, কারণ calling method সাথে সাথেই exception ছুঁড়ে দেয়, যেটা normal transaction হলে just-created approval row-ও rollback করে দিত), তারপর `BadRequestException` — ইউজারকে approvals queue-তে পাঠানো হয়
3. অনুমোদিত হলে `currentStage++`
4. নতুন স্টেজে `slaHours` থাকলে, `slaDeadline` আবার রিসেট হয় (`now + slaHours`) আর `slaBreach = false` — **SLA প্রতিটা স্টেজে নতুন করে শুরু হয়**, পুরো request-এর জীবদ্দশায় একবার সেট হয়ে থাকে না
5. নতুন স্টেজে `requiresPayment` থাকলে, milestone payment-এর নোটিফিকেশন যায় (কিন্তু auto-invoice হয় না — পেমেন্ট কালেক্ট করা staff-এর manual কাজ)

### Approval — কীভাবে গেট করে

`StageApprovalServiceImpl.approve()` — approve করলে **সাথে সাথেই `requestService.advanceStage()` কল হয়** — মানে approve করাই আসলে `currentStage` এগিয়ে দেয় (একই `advanceStage()` লজিক আবার চলে, এবার `APPROVED` row পেয়ে এগিয়ে যায়)। Reject করলে request একই স্টেজে আটকে থাকে, কোনো auto status change হয় না।

### SLA Breach — কীভাবে ফ্ল্যাগ হয়

`SlaBreachScheduler` (`@Scheduled` প্রতি ৩০ মিনিটে):
1. `slaDeadline < now AND slaBreach = false AND status বন্ধ না` — এমন request খোঁজে
2. এক **bulk UPDATE**-এ সবগুলোর `slaBreach = true` করে দেয়
3. প্রতিটার assigned employee-কে নোটিফাই করে

**গুরুত্বপূর্ণ:** ফ্রন্টএন্ডে কোনো লাইভ কাউন্টডাউন টাইমার নেই — শুধু একটা স্ট্যাটিক লাল "SLA BREACHED" ব্যাজ (`slaBreach` বুলিয়ান রেন্ডার হয়) আর ডেডলাইনের প্লেইন টেক্সট। এটা একটা periodic sweep, event-driven না।

## ৫.৪ গুরুত্বপূর্ণ Service Methods

- **`ServicePackageServiceImpl.subscribe()`** — একজন client-এর একটা প্যাকেজে একবারে শুধু একটা ACTIVE subscription থাকতে পারে। `pricePaid = getEffectivePrice()` subscribe করার মুহূর্তে **লক** হয়ে যায় — পরে প্যাকেজের দাম বদলালেও পুরনো subscriber-দের দাম বদলায় না।
- **`consumeQuota()` / `releaseQuota()`** — pessimistic lock (`findByIdAndCompanyIdForUpdate`) দিয়ে quota বাড়ানো/কমানো — যাতে concurrent request একই সময়ে quota consume করতে গেলে race condition না হয়। `overageRate` কনফিগার করা থাকলে quota-এর বেশি ব্যবহারও অনুমোদিত, পরে অতিরিক্ত বিল করা হয়।
- **`WorkflowServiceImpl`** — প্রতিটা stage add/update/delete-এ `template.version++` হয়। একটা stage থাকা অবস্থায় template deactivate/delete করা যায় না।
- **`ServiceReviewServiceImpl.submitOrUpdate()`** — শুধু `COMPLETED` request-এর জন্য, শুধু owning client, প্রতি request-এ একটাই review (unique), তৈরি হওয়ার ৭ দিন পর্যন্ত এডিট করা যায়।
- **`UsageBillingService.handleCompletion()`** — request কমপ্লিট হলে, যদি subscription quota ছাড়িয়ে যায়, স্বয়ংক্রিয়ভাবে একটা "Overage Charge" ইনভয়েস তৈরি হয়।

## ৫.৫ Frontend — গুরুত্বপূর্ণ কম্পোনেন্ট

- **`RequestDetail`** — সবচেয়ে সমৃদ্ধ কম্পোনেন্ট, একটা single request-এর সম্পূর্ণ ড্যাশবোর্ড: lifecycle actions, task CRUD, approval-gated `advanceStage()` (approval লাগলে backend-এর raw error message-ই দেখানো হয়, যেটা সরাসরি approvals queue-এর দিকে নির্দেশ করে), quotation actions, document upload, proposal flow, WebSocket দিয়ে live chat (`/user/queue/service-requests/{id}/messages`)
- **`Workflows`** — workflow builder UI; AI suggest ফিচার (`suggestWorkflow()`) স্টেজগুলো **একটার পর একটা sequentially** তৈরি করে (parallel না), যাতে `stageOrder` AI-এর সাজেস্ট করা ক্রম ধরে রাখে
- **`Approvals`** — approval queue; reject করার সময় client-side-এই একটা note বাধ্যতামূলক (reject-এর কারণ সবসময় লেখা থাকা উচিত — এই নীতি backend-এ enforce করা না থাকলেও frontend-এ enforced)

## ৫.৬ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `servicecategory/*` | `ServiceCategory` — টাক্সোনমি/গ্রুপিং |
| `companyservice/CompanyService.java` + `CompanyServiceServiceImpl` | মূল বিক্রয়যোগ্য catalog item, `listActive()` permission-gate ছাড়াই (picker-এর জন্য) |
| `companyservice/ServicePackage.java` + `ServicePackageServiceImpl` | বান্ডেল, `subscribe/activate/suspend/cancel`, `consumeQuota/releaseQuota` (pessimistic lock) |
| `servicetemplate/ServiceTemplate.java` | ডিজাইন-টাইম ব্লুপ্রিন্ট (dead `workflowStages` সাব-লিস্ট সহ) |
| `workflow/workflow/WorkflowServiceImpl.java` | Template/stage CRUD, `version++`, `suggest()` (AI) |
| `servicerequest/ServiceRequestServiceImpl.java` | সবচেয়ে বড় ফাইল (~১১৫০ লাইন) — পুরো state machine, `advanceStage`, quotation flow, `validatePrerequisites`, `validateAndSerializeFormData` |
| `approval/StageApprovalServiceImpl.java` | `approve()` (cascades into `advanceStage`), `reject()` |
| `servicereview/ServiceReviewServiceImpl.java` | Review CRUD, ৭-দিনের এডিট উইন্ডো |
| `kb/KbArticleServiceImpl.java` | Knowledge base CRUD, ক্লায়েন্ট-ফেসিং vs স্টাফ-ফেসিং ব্রাঞ্চ |
| `core/scheduler/SlaBreachScheduler.java` | ৩০ মিনিট অন্তর bulk SLA breach flag আপডেট |

### Frontend

| ফাইল | কাজ |
|---|---|
| `categories.ts` | Category CRUD, client-side icon/color heuristic |
| `services.ts` | Catalog admin, `groupedServices` getter |
| `packages.ts` | দুই-ট্যাব (packages/subscriptions), `computedEffectivePrice` mirror |
| `workflows.ts` | Stage builder, `createStagesSequentially()` (AI suggest অনুসরণ করে ক্রম) |
| `requests.ts` | Request তালিকা + তৈরি, role-aware tabs |
| `request-detail.ts` | সবচেয়ে সমৃদ্ধ কম্পোনেন্ট (lifecycle, task, approval, quotation, document, proposal, live chat) |
| `approvals.ts` | Approval queue, reject-এ note বাধ্যতামূলক |
| `reviews.ts` | Admin-side moderation |
| `knowledge-base.ts` | KB CRUD, `markHelpful()` |

## ৫.৭ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: একটা approval তৈরি হওয়ার সময় `REQUIRES_NEW` transaction propagation কেন ব্যবহার করা হলো?**
উত্তর: `advanceStage()` মেথড approval তৈরি করার পরপরই `BadRequestException` ছুঁড়ে দেয় (ইউজারকে জানাতে যে approval লাগবে)। যদি একই transaction-এ approval-টা তৈরি হতো, exception-এর কারণে সেই পুরো transaction rollback হয়ে যেত — মানে just-created approval row-টাও মুছে যেত! `REQUIRES_NEW` দিয়ে approval creation-কে একটা সম্পূর্ণ আলাদা, স্বাধীন transaction-এ চালানো হয়, যাতে বাইরের method exception ছুঁড়লেও এই আলাদা transaction ইতিমধ্যে কমিট হয়ে গেছে।

**প্রশ্ন: SLA breach detection কেন event-driven না করে periodic polling (cron job) দিয়ে করা হলো?**
উত্তর: SLA breach একটা "সময় পার হয়ে গেছে" ধরনের condition — এটা কোনো নির্দিষ্ট user action-এর সাথে ট্রিগার হয় না, বরং শুধু সময় এগোনোর সাথে সাথে ঘটে। এমন condition চেক করার জন্য scheduled polling স্বাভাবিক সমাধান — প্রতি ৩০ মিনিটে একবার bulk-চেক করে নেওয়া, সবগুলো request-এর জন্য আলাদা টাইমার/ইভেন্ট সেট করার চেয়ে অনেক সহজ ও efficient।

**প্রশ্ন: `ServiceTemplate.workflowStages` কেন dead code, কীভাবে বুঝলেন?**
উত্তর: `ServiceTemplate`-এর ভেতরে একটা নেস্টেড `TemplateWorkflowStage` কালেকশন আছে, যা ফ্রন্টএন্ডের Templates কম্পোনেন্টে এডিট করা যায়। কিন্তু `ServiceRequestServiceImpl.advanceStage()` কোথাও এই কালেকশন পড়ে না — এটা শুধু `CompanyService.workflowTemplate` (একটা সম্পূর্ণ আলাদা entity, `WorkflowTemplate`/`WorkflowStage`) থেকে stage তথ্য নেয়। এটা কোড পড়ে ধরা — grep করে দেখা যে `TemplateWorkflowStage`-এর কোনো ব্যবহার নেই runtime execution পাথে, শুধু CRUD এর মধ্যেই সীমাবদ্ধ।

**প্রশ্ন: `PackageSubscription.consumeQuota()`-এ pessimistic lock কেন দরকার?**
উত্তর: একটা subscription-এ সীমিত quota (`requestQuota`) থাকে — যদি দুইজন ক্লায়েন্ট (বা একই ক্লায়েন্টের দুইটা ব্রাউজার ট্যাব) একই সাথে দুইটা request তৈরি করে ঠিক তখনই যখন quota-তে মাত্র ১টা স্লট বাকি, optimistic (lock-ছাড়া) কোড উভয়কেই "quota আছে" দেখাতে পারে — ফলে quota-র বেশি consume হয়ে যায়। `findByIdAndCompanyIdForUpdate` (`SELECT FOR UPDATE`) দ্বিতীয় ট্রানজ্যাকশনকে প্রথমটা শেষ না হওয়া পর্যন্ত ব্লক করে রাখে, নিশ্চিত করে quota গণনা সবসময় সঠিক।
