# অধ্যায় ৭ — Recruitment / ATS মডিউল

> **কোড লোকেশন:** Backend: `backend/BusinessOS/src/main/java/com/businessos/modules/hrm/recruitment/` · Frontend: `frontend/BusinessFlow/src/app/modules/hrm/components/{job-postings,candidates,applications,pipeline,interviews,offers,talent-pool,recruitment-reports}` + `frontend/BusinessFlow/src/app/modules/careers/` (career page)

## ৭.১ এই মডিউল কী করে

একটা কোম্পানি নতুন লোক নিয়োগ দেওয়ার পুরো প্রক্রিয়া — job posting দেওয়া থেকে শুরু করে candidate খুঁজে বের করা, তাদের CV পার্স করে স্কোর দেওয়া, ইন্টারভিউ নেওয়া, অফার পাঠানো, শেষে hire করে Employee বানানো পর্যন্ত — সবকিছু এখানে। এটা প্রজেক্টের সবচেয়ে জটিল মডিউলগুলোর একটা, কারণ এখানে state machine (application status pipeline), automated CV parsing, আর KPI aggregation — তিনটাই একসাথে আছে।

## ৭.২ Data Model — কে কী

### `Candidate` (টেবিল: `recruitment_candidates`)

একজন **মানুষ** — নাম, ইমেইল, ফোন, resume URL, LinkedIn/portfolio লিংক, skills (CSV), `source` (কীভাবে প্রথম যোগাযোগ হয়েছিল — `ApplicationSource` enum: `CAREER_PAGE, LINKEDIN, FACEBOOK, JOB_PORTAL, EMPLOYEE_REFERRAL, AGENCY, DIRECT, OTHER`)।

**গুরুত্বপূর্ণ ডিজাইন ডিসিশন:** একজন মানুষ একাধিক জবে apply করতে পারে — প্রতিবার নতুন candidate তৈরি না করে, `CandidateService.findOrCreate(companyId, email, ...)` একই ইমেইল থাকলে পুরনো candidate-কেই reuse করে। এটা আগে ছিল না — আগে `JobApplication`-এর ভেতরেই `applicantName/applicantEmail/applicantPhone` ফিল্ড ছিল, একই মানুষ দুইবার apply করলে দুইটা আলাদা "মানুষ" তৈরি হতো ডেটাবেজে। `Candidate` entity বানিয়ে এই সমস্যা সমাধান করা হয়েছে।

### `JobApplication` (টেবিল: `job_applications`)

একজন candidate-এর **একটা নির্দিষ্ট জব-এ আবেদন**। `Candidate` entity-র সাথে `@ManyToOne` সম্পর্ক। এর সাথে আছে:

- `status` — ১৩টা সম্ভাব্য মান নিয়ে একটা state machine (নিচে বিস্তারিত)
- `source` — এই নির্দিষ্ট আবেদনটা কোন চ্যানেল থেকে এসেছে (candidate-এর নিজের `source`-এর চেয়ে ভিন্ন হতে পারে — যেমন কেউ প্রথমে LinkedIn-এ পাওয়া গিয়েছিল, পরে ভিন্ন একটা রোলের জন্য রেফারেল হিসেবে আবার apply করলো)
- Manual evaluation স্কোর: `scoreEducation, scoreExperience, scoreTechnicalSkills, scoreInterview, scoreCommunication` (প্রতিটা ০-১০০) + কম্পিউটেড `overallScore`
- ATS (automated) স্কোর: `atsScore`, `atsMatchedRequiredSkills`, `atsMissingRequiredSkills`, `atsMatchedPreferredSkills`, `atsExtractedExperienceYears`, `atsMeetsEducationRequirement`, `atsParseStatus`, `atsParsedAt`

### `ApplicationStatus` — ১৩-স্টেপ পাইপলাইন

```
APPLIED → SCREENING → SHORTLISTED → INTERVIEW_SCHEDULED → INTERVIEWED
→ SELECTED → OFFER_PENDING → OFFER_SENT → OFFER_ACCEPTED / OFFER_REJECTED
→ HIRED
(যেকোনো ধাপ থেকে) → REJECTED / WITHDRAWN
```

`OFFER_PENDING, OFFER_SENT, OFFER_ACCEPTED, OFFER_REJECTED` — এই চারটাকে বলা হয় **offer sub-pipeline**। এগুলো `JobOfferController`-এর নিজস্ব endpoints দিয়ে ছাড়া অন্য কোনোভাবে সেট করা যায় না — এমনকি generic status-update endpoint দিয়েও না। কেন? কারণ অফার পাঠানো মানে ইমেইল যাওয়া — যদি generic dropdown দিয়েও status `OFFER_SENT` করা যেত, তাহলে দুইটা আলাদা কোড-পাথ থেকে একই candidate-কে দুইবার অফার ইমেইল যেতে পারতো (এটা আসলে একটা রিয়েল বাগ ছিল, ফিক্স করা হয়েছে — নিচে দেখুন)।

## ৭.৩ Backend Service Layer

### `RecruitmentServiceImpl` — মূল business logic

**`apply(...)`** — নতুন আবেদন জমা নেয়।
```java
Candidate candidate = candidateService.findOrCreate(companyId, name, email, phone, source, resumeUrl, ...);
// duplicate check: একই candidate একই job posting-এ আগে থেকেই সক্রিয় আবেদন করে থাকলে আটকায়
boolean exists = applicationRepository.existsByJobPostingIdAndCandidateIdAndStatusNotIn(
    jobPostingId, candidate.getId(), List.of(REJECTED, WITHDRAWN, OFFER_REJECTED));
```
`OFFER_REJECTED`-কে duplicate-check-এর exclusion লিস্টে রাখা হয়েছে — কারণ যদি কারো অফার প্রত্যাখ্যাত হয়, সে ভবিষ্যতে আবার apply করতে চাইলে সিস্টেম যেন তাকে "আপনি ইতিমধ্যে আবেদন করেছেন" বলে না আটকায়। এটাও একটা রিয়েল বাগ-ফিক্স ছিল — আগে এই enum ভ্যালুটা exclusion লিস্টে ছিল না।

**`updateStatus(...)`** — generic status change। এখানে সবচেয়ে গুরুত্বপূর্ণ guard:
```java
if (application.getStatus() == ApplicationStatus.HIRED
        || OFFER_SUB_STATUSES.contains(application.getStatus())) {
    throw new BadRequestException(
        "This application has an active offer or is already hired - "
        + "use the Offers screen or the Hire action, not the generic dropdown");
}
```
এটা শুধু "আপনি অফার-স্ট্যাটাসে সরাসরি সেট করতে পারবেন না" তা-ই না — **বর্তমান স্ট্যাটাস যদি ইতিমধ্যে অফার-সাব-স্ট্যাটাস বা HIRED হয়, সেখান থেকে বেরও হতে পারবেন না** এই জেনেরিক পথ দিয়ে। কারণ HIRED/অফার অবস্থা থেকে সরে আসাও ঠিক ততটাই ঝুঁকিপূর্ণ যতটা সেখানে ঢোকা।

**`hire(...)`** — একজন candidate-কে সত্যিকারের Employee বানায়। Gate করা আছে: `status != OFFER_ACCEPTED` হলে reject করে। Permission check করা হয় `EMPLOYEE_CREATE` দিয়ে (আগে ভুলবশত `APPLICATION_UPDATE` permission দিয়ে চেক হতো, যেটা logically ভুল ছিল — একজন যার শুধু application আপডেট করার permission আছে, তার নতুন Employee তৈরি করার ক্ষমতা থাকা উচিত না)।

### `JobOfferController` — অফার সাব-পাইপলাইনের মালিক

প্রতিটা action application-এর status-ও সিঙ্ক করে:

| Action | Application Status হয় |
|---|---|
| `create()` (draft offer) | `OFFER_PENDING` |
| `send()` | `OFFER_SENT` |
| `accept()` | `OFFER_ACCEPTED` |
| `decline()` | `OFFER_REJECTED` |
| `withdraw()` (employer নিজে সরিয়ে নিলো) | `SELECTED` (candidate-এর দোষ না, তাই dead-end status-এ আটকে না রেখে আবার offer-able state-এ ফেরত) |
| `delete()` (draft বাতিল) | যদি `OFFER_PENDING` ছিল, `SELECTED`-এ ফেরত (নাহলে application "লিম্বো"-তে আটকে থাকতো — এটাও একটা bug-fix) |

প্রতিটা action-এ একটা `requireApplicationStatus(offer, expectedStatus)` helper defense-in-depth হিসেবে চেক করে — শুধু offer-এর নিজের status না, application-এর status-ও ঠিক আছে কিনা।

### `CvScoringService` — ATS ম্যাচ স্কোরিং ইঞ্জিন

এটাই এই মডিউলের সবচেয়ে ইন্টারেস্টিং অংশ। কাজ করে এভাবে:

1. **Extract:** `CvTextExtractor` — PDF হলে PDFBox (`PDDocument` + `PDFTextStripper`), DOCX হলে Apache POI (`XWPFWordExtractor`)। resume `.doc` বা অন্য কিছু হলে `UNSUPPORTED_FORMAT`।
2. **Match Required/Preferred Skills:** প্রতিটা skill token-এর জন্য case-insensitive word-boundary regex দিয়ে খোঁজে। এখানে একটা সূক্ষ্ম বাগ ছিল আর ফিক্স হয়েছে — `containsSkill()` মেথডে "C" স্কিল খুঁজতে গিয়ে "C++" বা "C#"-এর ভেতরের "C"-কেও ম্যাচ করে ফেলতো। ফিক্স: negative lookahead `(?![+#])` যোগ করা হয়েছে যাতে "C" আলাদা টোকেন হিসেবেই ম্যাচ করে, "C++"-এর অংশ হিসেবে না।
3. **Extract Experience Years:** regex দিয়ে `"(\d+)\+?\s*(?:years?|yrs?)"` প্যাটার্ন খোঁজে, টেক্সটে যত জায়গায় পাওয়া যায় তার মধ্যে সবচেয়ে বড় সংখ্যাটা নেয়।
4. **Extract Education Level:** keyword→enum ম্যাপ (`EducationLevel: NONE, DIPLOMA, BACHELOR, MASTER, PHD`)। এখানেও একটা বাগ ফিক্স হয়েছে — regex-এ `m\.?a` মতো প্যাটার্ন থাকায় "Master's" শব্দটা bare "MA" আর "Bachelor's"-এর মধ্যে ভুল ম্যাচ হয়ে যেত; ফিক্স করে ছোট abbreviation-গুলোতে period বাধ্যতামূলক করা হয়েছে (`m\.a\.?`)।
5. **Compute Score:** ৪টা category-তে weighted — Required Skills 40%, Experience 25%, Education 15%, Preferred Skills & Certifications 20%। যদি কোনো job posting-এ কোনো category-র requirement সেট না থাকে (যেমন `minEducationLevel` null), সেই category বাদ দিয়ে বাকি categories-এর weight **renormalize** করা হয় — যোগফল সবসময় ১০০% থাকে।

**Bug fix — NaN:** `minExperienceYears` যদি ০ বা তার কম হয়, division-by-zero-এ ফলাফল `NaN` হয়ে যেত। ফিক্স:
```java
posting.getMinExperienceYears() <= 0 ? 1.0
    : extractedYears == null ? 0.0
    : Math.min(1.0, extractedYears / (double) posting.getMinExperienceYears());
```

**Security — SSRF প্রতিরোধ:** `resolveOwnUploadPath()` resume ফাইল পড়ার আগে চেক করে যে stored URL-টা সত্যিই এই সার্ভারের নিজের `/uploads/` পাথের — কখনো কোনো external URL fetch করে না। candidate যদি resume URL হিসেবে কোনো internal network address বা malicious URL দিয়ে দেয়, সিস্টেম সেটাকে fetch করার চেষ্টাই করবে না। এখানেও একটা bug ছিল — malformed path দিলে `InvalidPathException` uncaught থেকে যেত, পুরো async job crash করতো; এখন `try/catch` দিয়ে গ্রেসফুলি `null` রিটার্ন করে।

**Async ও transaction টাইমিং:** স্কোরিং চলে `@Async`-এ, কিন্তু trigger হয় `apply()`-এর ট্রানজ্যাকশন **কমিট হওয়ার পরে** — `TransactionSynchronizationManager.registerSynchronization()` দিয়ে `afterCommit()` callback ব্যবহার করে। কেন? কারণ যদি ট্রানজ্যাকশন কমিট হওয়ার আগেই async স্কোরিং শুরু হয়ে যায়, সেই থ্রেড হয়তো এখনো-কমিট-না-হওয়া application row-টা ডেটাবেজে খুঁজেই পাবে না (race condition)।

**Self-invocation পিটফল:** `scheduleAfterCommit()` মেথডটা যদি সরাসরি `this.scoreApplication(...)` কল করতো, Spring-এর AOP proxy বাইপাস হয়ে যেত আর `@Async`/`@Transactional` কাজই করতো না (Java-তে same-class মেথড কল প্রক্সির ভেতর দিয়ে যায় না)। ফিক্স: `@Autowired @Lazy` দিয়ে নিজের ইন্টারফেসকে নিজেই inject করা (`self` ফিল্ড), তারপর `self.scoreApplication(...)` কল করা — এতে কলটা প্রক্সির মধ্য দিয়ে যায়।

### `InterviewController`

**`feedback(...)`** — ইন্টারভিউয়ের ফিডব্যাক জমা দিলে, যদি এটাই সেই application-এর শেষ scheduled ইন্টারভিউ হয় (আর no-show না হয়), application status automatic `INTERVIEWED`-এ চলে যায়। Bug fix: আগে no-show হলেও এই auto-advance হয়ে যেত — এখন `!request.isNoShow()` চেক যোগ করা হয়েছে।

**`cancel(...)`** — একটা scheduled ইন্টারভিউ বাতিল হলে, যদি application-এর আর কোনো scheduled ইন্টারভিউ না থাকে, status `SHORTLISTED`-এ ফেরত যায় (bug fix — আগে `INTERVIEW_SCHEDULED`-এ আটকে থাকতো, একটা dead-end)।

### KPI Layer — `RecruitmentKpiServiceImpl`

`RecruitmentKpiResponse` — একটা বড় flat DTO, যেখানে থাকে:
- হেডলাইন কাউন্ট (openPositions, totalCandidates, totalApplications, hiresThisMonth)
- Funnel stages (Applied → Screening → Interview → Offer → Hired)
- রেট (`avgTimeToHireDays`, `applicationToInterviewRate`, `interviewToHireRate`, `offerAcceptanceRate`)
- Source breakdown (কোন চ্যানেল থেকে কতজন)
- **Per-job breakdown** (`JobKpi`) আর **per-recruiter breakdown** (`RecruiterKpi`) — প্রতিটাতে `avgAtsMatchScore` ফিল্ডও আছে
- Top-evaluated candidates

সব রেট `null`-হতে পারে (boxed `Double`) — ডেটা না থাকলে ০ না দেখিয়ে honest-ভাবে "no data yet" বোঝানো একটা established convention পুরো ড্যাশবোর্ড লেয়ারে।

`avgAtsMatchScore` কম্পিউট হয় শুধুমাত্র `atsParseStatus == SUCCESS && atsScore != null` — এমন আবেদনগুলো ফিল্টার করে, একটা shared helper মেথড দিয়ে (তিন জায়গায় — company-wide, per-job, per-recruiter — একই লজিক পুনর্ব্যবহার করা হয়েছে)।

## ৭.৪ Frontend Components

| Component | কাজ |
|---|---|
| `job-postings` | জব পোস্ট করা — title, department, required/preferred skills (নতুন `skill-tag-input` shared component দিয়ে), `minExperienceYears`, `minEducationLevel` |
| `candidates` | সব candidate-এর তালিকা, সার্চ, প্রতিটার সাথে কয়টা application আছে দেখায় |
| `applications` | মূল ওয়ার্কিং পেজ — স্ট্যাটাস ফিল্টার, Evaluate/Update Status actions, candidate নাম ক্লিক করলে পূর্ণ detail modal খোলে (candidate info + manual evaluation + ATS breakdown একসাথে) |
| `pipeline` | Kanban board — `@angular/cdk` drag-drop দিয়ে। Offer আর Hired column non-droppable (ওগুলো শুধু নিজেদের ডেডিকেটেড স্ক্রিন দিয়ে বদলাতে হয়) |
| `interviews` | ইন্টারভিউ শিডিউল করা, ফিডব্যাক দেওয়া |
| `offers` | Draft offer তৈরি, send, accept/decline track করা |
| `talent-pool` | রিজেক্টেড কিন্তু ভবিষ্যতে কাজে লাগতে পারে এমন candidate-দের আলাদা রাখার জায়গা |
| `recruitment-reports` | KPI ড্যাশবোর্ড — funnel chart, source doughnut, job/recruiter টেবিল, date-range filter |
| `careers/career-page` | পাবলিক (unauthenticated) — বাইরের কেউ job দেখে resume আপলোড করে সরাসরি apply করতে পারে |

## ৭.৫ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `candidate/Candidate.java` + `CandidateServiceImpl` | `findOrCreate()` — ইমেইল দিয়ে re-use বা নতুন তৈরি |
| `jobapplication/JobApplication.java` | ১৩-স্ট্যাটাস পাইপলাইন, manual + ATS দুই ধরনের স্কোর ফিল্ড |
| `RecruitmentServiceImpl.java` | `apply` (duplicate-check), `updateStatus` (offer-sub-status গার্ড), `hire` (permission + status gate) |
| `offer/JobOfferController.java` | `create/send/accept/decline/withdraw/delete` — প্রতিটাতে application status sync |
| `interview/InterviewController.java` | `feedback` (auto-advance guard), `cancel` (SHORTLISTED fallback) |
| `ats/CvTextExtractor.java` | PDFBox (PDF) + POI `XWPFWordExtractor` (DOCX) |
| `ats/CvScoringService.java` | `scoreApplication` (weighted category scoring), `containsSkill` (regex ম্যাচিং), `resolveOwnUploadPath` (SSRF গার্ড) |
| `kpi/RecruitmentKpiServiceImpl.java` | Funnel, রেট, source breakdown, per-job/per-recruiter breakdown |

### Frontend

| ফাইল | কাজ |
|---|---|
| `job-postings.ts` | ফর্ম, `skill-tag-input` ইন্টিগ্রেশন |
| `candidates.ts` | তালিকা + সার্চ |
| `applications.ts` | মূল ওয়ার্কিং পেজ, `openDetail()` (applicant-detail modal), Evaluate/Update Status |
| `pipeline.ts` | Kanban কলাম ম্যাপিং, drag-drop `onDrop()` |
| `interviews.ts` | শিডিউলিং, ফিডব্যাক ফর্ম |
| `offers.ts` | Offer lifecycle actions |
| `recruitment-reports.ts` | চার্ট + টেবিল, date-range filter |
| `careers-page.ts` | পাবলিক apply ফর্ম, resume আপলোড |

## ৭.৬ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: কেন `Candidate` আর `JobApplication` আলাদা entity, একটাতে মিশিয়ে দিলে কী সমস্যা হতো?**
উত্তর: একজন মানুষ একাধিক জবে apply করতে পারে। যদি সব তথ্য `JobApplication`-এ থাকতো, একই ইমেইল দিয়ে দ্বিতীয়বার apply করলে একটা নতুন "ব্যক্তি" তৈরি হয়ে যেত ডেটাবেজে — নাম/ইমেইল ডুপ্লিকেট হতো, আর "এই লোক কোন কোন জবে apply করেছে" — এই প্রশ্নের উত্তর খুঁজতে হতো text matching দিয়ে, foreign key দিয়ে না।

**প্রশ্ন: async স্কোরিং কেন transaction commit-এর পরে চালানো হয়, সাথে সাথে না?**
উত্তর: race condition এড়াতে। `apply()` মেথডটা এখনো transaction-এর ভেতরে থাকা অবস্থায় যদি একটা আলাদা থ্রেড (async) সেই সদ্য-তৈরি application row পড়তে যায়, ডেটাবেজে row-টা এখনো visible নাও হতে পারে (isolation level অনুযায়ী)। `afterCommit()` দিয়ে নিশ্চিত করা হয় যে row পুরোপুরি persist হওয়ার পরেই async কাজ শুরু হবে।

**প্রশ্ন: ATS স্কোর কখনো candidate-কে auto-reject করে না কেন?**
উত্তর: ডিজাইন সিদ্ধান্ত — resume parsing আর keyword matching নিখুঁত না (ভালো candidate-এর resume হয়তো ভিন্নভাবে লেখা, বা parsing ভুল করতে পারে)। তাই ATS score শুধু একটা "signal", recruiter-এর judgment-কে সাহায্য করার জন্য, কখনো override করার জন্য না — UI-তেও লেখা থাকে "Automated signal — confirm manually"।

**প্রশ্ন: `containsSkill()`-এর regex-এ `(?![+#])` negative lookahead কী করে, কেন দরকার হলো?**
উত্তর: এই lookahead চেক করে যে ম্যাচ হওয়া স্কিলের ঠিক পরের ক্যারেক্টার `+` বা `#` না। এটা দরকার হয়েছিল কারণ "C" স্কিল খুঁজতে গিয়ে regex "C++" বা "C#"-এর ভেতরের "C"-কেও ভুলভাবে ম্যাচ করে ফেলতো (word-boundary `\b` একা যথেষ্ট না, কারণ `+`/`#` word character না, তাই `\b` সেখানেও একটা বাউন্ডারি হিসেবে গণ্য করে)। এই lookahead যোগ করে নিশ্চিত করা হয়েছে "C" শুধু তখনই ম্যাচ করবে যখন সেটা সত্যিই একটা আলাদা ভাষা "C", "C++"/"C#"-এর অংশ না।

**প্রশ্ন: `OFFER_PENDING/SENT/ACCEPTED/REJECTED` — এই চারটা সাব-স্ট্যাটাসকে কেন `JobOfferController`-এর বাইরে থেকে সেট করা ব্লক করা হয়েছে?**
উত্তর: প্রতিটা offer-status পরিবর্তন একটা রিয়েল-ওয়ার্ল্ড সাইড-ইফেক্ট বহন করে — যেমন `send()` মানে candidate-কে সত্যিই একটা অফার ইমেইল যাওয়া। যদি generic `updateStatus()` endpoint দিয়েও `OFFER_SENT` সেট করা যেত, দুইটা সম্পূর্ণ স্বাধীন কোড-পাথ (generic dropdown + Offers স্ক্রিন) একই ইভেন্টের জন্য দুইবার ইমেইল পাঠাতে পারতো — এটা প্রকৃতপক্ষে এই কোডবেসে একটা রিয়েল বাগ ছিল, ফিক্স করা হয়েছে এই status-গুলোকে শুধুমাত্র তাদের নিজস্ব ডেডিকেটেড endpoint দিয়ে বদলানো যাবে এই নিয়ম করে।
