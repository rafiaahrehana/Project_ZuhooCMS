# অধ্যায় ৬ — Human Resources (Core) মডিউল

> **কোড লোকেশন:** Backend: `backend/BusinessOS/src/main/java/com/businessos/modules/hrm/{employee,department,designation,performance}` + `hrm/attendance/shift` (Shift) + `hrm/recruitment/offerletter` (Letters ইঞ্জিন) · Frontend: `frontend/BusinessFlow/src/app/modules/hrm/components/{employees,employee-detail,departments,designations,shifts,expenses,performance-reviews,offer-letters}`

**একটা সৎ নোট:** "HR Expenses" পেজের নিজস্ব কোনো ব্যাকএন্ড entity নেই — এটা আসলে Finance মডিউলের `/api/company/finance/expenses`-এর একটা HRM-স্টাইল UI wrapper মাত্র।

## ৬.১ Employee — লাইফসাইকেল

`Employee` entity — `employeeNumber` সার্ভার-জেনারেটেড (`EMP-0042` ফরম্যাট, প্রতি-কোম্পানি sequential)। গুরুত্বপূর্ণ ফিল্ড: `employmentType` (FULL_TIME/PART_TIME/CONTRACT/INTERN/CONSULTANT), `employmentStatus` (PROBATION default → CONFIRMED → ACTIVE → ON_LEAVE/SUSPENDED → RESIGNED/TERMINATED/RETIRED), `active` (বুলিয়ান — payroll eligibility নিয়ন্ত্রণ করে, `employmentStatus` থেকে আলাদা), `billableRate` (timesheet-এর approved ঘণ্টার উপর প্রযোজ্য অতিরিক্ত রেট)।

**গুরুত্বপূর্ণ bug-fix:** `update()`-এ কেউ `employmentStatus`-কে terminal status (RESIGNED/TERMINATED/RETIRED/SUSPENDED)-এ পরিবর্তন করলে, এখন **`active` অটোমেটিক `false`** হয়ে যায় আর portal user deactivate হয়। আগে এই দুটো ফিল্ড আলাদা হয়ে যেতে পারতো — একজন "resigned" employee তখনো payroll-eligible আর login করতে পারতো!

**`terminate(id)`** (dedicated endpoint) — soft-delete, portal user deactivate, termination email, আর **reporting manager-কে নোটিফিকেশন** ("reassign their work and confirm asset return") — আগে এই নোটিফিকেশনটাই ছিল না, কেউ জানতোই না equipment ফেরত নিতে হবে।

## ৬.২ Department — Hierarchy ও Validation

`Department` self-referential (`parentDepartmentId`) — গাছের মতো hierarchy। কিন্তু **কোনো visual org-chart/tree UI নেই** — flat card grid, শুধু dropdown আর টেক্সটে parent দেখায়।

**Cycle guard:** `update()`-এ নতুন parent-এর নিজের parent-চেইন হেঁটে দেখে (`while (walker != null) { if (walker.id == id) throw; walker = walker.parent; }`) — শুধু direct self-reference না, A→B→A-এর মতো লম্বা চক্রও ধরে ফেলে।

**Head-employee validation:** department head হিসেবে নির্বাচিত employee-র `EmploymentStatus.ACTIVE` হতেই হবে — server-side enforce করা (আগে শুধু UI-তে filter হতো)।

**Delete guard:** department-এ কোনো employee থাকলে delete করা যায় না — "Reassign employees first"।

## ৬.৩ Designation — Auto-code Generation

কোড force-uppercase হয়, ইউনিক প্রতি-কোম্পানি। **কোনো salary band নেই এখানে** — salary পুরোপুরি `Employee` আর `SalaryStructure`-এ থাকে।

Frontend-এ মজার একটা ফিচার — নাম টাইপ করলে code অটো-জেনারেট হয়: এক শব্দ হলে প্রথম ৪ অক্ষর, একাধিক শব্দ হলে প্রতিটার প্রথম অক্ষর (যেমন "Senior Software Engineer" → "SSE")। ইউজার নিজে code এডিট করলে auto-generation বন্ধ হয়ে যায়।

**Delete guard:** কোনো employee-তে assigned থাকলে delete করা যায় না (Department-এর মতোই প্যাটার্ন — comment-এ লেখা এই guard পরে যোগ করা হয়েছে Department-এর সাথে মিলিয়ে, কারণ আগে না থাকায় একটা designation delete করলে সেই employee-র রিপোর্টে designation-এর জায়গা ভাঙা দেখাতো)।

## ৬.৪ Shift — Working Minutes অটো-ক্যালকুলেশন

`workingMinutes = ChronoUnit.MINUTES.between(startTime, endTime)` — সার্ভার নিজে হিসাব করে, ইউজার ইনপুট না। **Night shift-এর overnight wraparound হ্যান্ডল করে না** (endTime < startTime হলে negative হয়ে যাবে) — এটা একটা edge case gap।

`EmployeeShiftAssignment` — Shift-এর নিজস্ব assignment **history** টেবিল, `Employee.shift` FK থেকে আলাদা। নতুন assignment তৈরি হলে পুরনো active assignment অটো-বন্ধ হয়ে যায়। **গুরুত্বপূর্ণ পর্যবেক্ষণ:** এই সার্ভিস `Employee.shift` পয়েন্টার আপডেট করে না — দুটো field independently maintained, যা একটা সম্ভাব্য inconsistency risk।

## ৬.৫ Performance Review — স্কোরিং লজিক

**৮টা কম্পিটেন্সি** (১-১০ স্কেল): Work Quality, Productivity, Communication, Teamwork, Leadership, Problem Solving, Innovation, Punctuality। (`scoreInitiative` ব্যাকএন্ডে আছে কিন্তু ফ্রন্টএন্ড ফর্ম থেকে সরানো হয়েছে — পুরনো review-গুলোর ডেটা যাতে হারিয়ে না যায়।)

**`overallScore` — কোনো weight নেই, সাধারণ গড়:**
```java
calculateOverall(Integer... scores) {
    return average of non-null scores, rounded to 1 decimal
}
```
কোনো স্কোর ফাঁকা রাখলেও বাকিগুলোর গড় ঠিকই বের হয়ে যায়।

**দুটো ভিন্ন workflow, গুলিয়ে ফেলা যাবে না:**
- `ReviewStatus`: DRAFT → SUBMITTED → ACKNOWLEDGED
- `PerformanceStage`: SELF_ASSESSMENT → MANAGER_REVIEW → HR_APPROVAL → FINAL_APPROVAL → COMPLETED (approval chain, `next()` মেথড দিয়ে)

`finalise()`-এর পর review আর edit করা যায় না। `kpisForEmployee()` — objective KPI (attendance %, leave days, completed tasks, completed projects, client CSAT) **সবসময় live কম্পিউট হয়, review-তে সেভ হয় না** — যদি সেভ করে রাখা হতো, "একই পিরিয়ডের" নাম্বার পরে ভিন্ন ভিন্ন সময়ে আলাদা দেখাতে পারতো একটা নতুন attendance রেকর্ড যোগ হলে।

**Access control:** `PERFORMANCE_VIEW` না থাকলে একজন ইউজার **শুধু নিজের** review দেখতে পারে — URL-এ id বদলে অন্যের salary recommendation/weakness পড়া আটকাতে।

## ৬.৬ Employment Letters — AI-Draft ইঞ্জিন

`OfferLetter` entity (টেবিল নাম `employment_letters`, কোড-প্যাকেজ নাম মেলে না) — **১২ ধরনের চিঠি**: OFFER, APPOINTMENT, CONFIRMATION, PROMOTION, TRANSFER, EXPERIENCE, NOC, SALARY_CERTIFICATE, TERMINATION, RESIGNATION_ACCEPTANCE, WARNING, APPRECIATION।

**Recipient rule:** OFFER আর APPOINTMENT — এই দুটোই একমাত্র pre-employment টাইপ, `JobApplication` (candidate)-কে সম্বোধন করতে হবে। বাকি ১০টা `Employee`-কে সম্বোধন করতে হবে। ভুল রিসিপিয়েন্ট টাইপ দিলে `BadRequestException`।

**PDF না — শুধু টেক্সট:** চিঠির `content` একটা প্লেইন TEXT column-এ সেভ হয়, কোনো PDF রেন্ডারিং নেই এখানে (Employee list export-এর মতো PDF জেনারেশন এখানে wire করা হয়নি)।

**AI-draft — দুই এন্ট্রি পয়েন্ট:**
1. `POST /draft` — শুধু draft জেনারেট করে, সেভ করে না, ইউজারকে রিভিউ/এডিট করতে দেয়
2. `POST /` (create) — যদি `content` ফাঁকা থাকে, সেভ করার **আগেই AI দিয়ে অটো-জেনারেট** করে নেয়

`issue()` করলে আর delete করা যায় না — signed record immutable।

## ৬.৭ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `employee/EmployeeServiceImpl.java` | `create` (portal user + welcome email), `update` (terminal-status sync), `terminate` (dedicated soft-delete + manager notify), `validateNotSelfManager`, `updateMyProfile` |
| `employee/EmployeePdfService.java` | কর্মী তালিকা PDF এক্সপোর্ট (`SimpleTablePdfRenderer` দিয়ে, কোম্পানি-ব্র্যান্ডেড) |
| `department/DepartmentServiceImpl.java` | `create/update` (cycle guard, head-active-validation), `delete` (employee-থাকলে-ব্লক) |
| `designation/DesignationServiceImpl.java` | auto-uppercase code, `delete` (assigned-employee-ব্লক) |
| `hrm/attendance/shift/ShiftServiceImpl.java` | `workingMinutes` অটো-ক্যালকুলেশন |
| `hrm/attendance/shift/EmployeeShiftAssignmentServiceImpl.java` | Assignment history — পুরনো active assignment অটো-ক্লোজ |
| `performance/PerformanceServiceImpl.java` | `create/update/finalise/advanceStage/kpisForEmployee/summarise` (AI), `guardOwnReviewAccess` |
| `recruitment/offerletter/OfferLetterServiceImpl.java` | `draftWithAi/create/issue/delete`, recipient-type ভ্যালিডেশন |

### Frontend

| ফাইল | কাজ |
|---|---|
| `employees.ts` | ফর্ম ভ্যালিডেশন, `accentFor()` (deterministic color), `statusPill()` |
| `employee-detail.ts` | এডিট + education qualification + custom-role tab |
| `departments.ts` | ফ্ল্যাট কার্ড গ্রিড (কোনো tree-view না), `filteredDepartments` |
| `designations.ts` | `generateCode()` অটো-জেনারেশন লজিক |
| `shifts.ts` | CRUD, per-shift-type রং |
| `performance-reviews.ts` | `liveOverall` (client-side mirror), `stageState()`, tag/goal UI |
| `offer-letters.ts` | `isCandidateLetter` getter, `draftWithAi()` |

## ৬.৮ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: `active` আর `employmentStatus` দুটো আলাদা ফিল্ড কেন Employee-তে?**
উত্তর: `employmentStatus` মানুষের কর্মজীবনের বর্ণনামূলক অবস্থা (probation, active, resigned)। `active` হলো একটা সরল boolean যা payroll eligibility এবং login access নিয়ন্ত্রণ করে। দুটো আলাদা রাখার কারণ ছিল ভবিষ্যতে "ON_LEAVE" অবস্থায়ও active থাকা দরকার (payroll চলবে, শুধু কাজে নেই) কিন্তু "TERMINATED" অবস্থায় active থাকা উচিত না — তবে এই দুটো manually sync রাখতে গিয়ে আগে বাগ হয়েছিল (উপরে দেখুন), তাই এখন `update()`-এ terminal status set করলে `active` অটোমেটিক sync হয়।

**প্রশ্ন: Performance Review-এর overallScore-এ কেন কোনো weight নেই, সব competency সমান কেন?**
উত্তর: সরলতা — একটা weighted formula মেইনটেইন করা (কোন কম্পিটেন্সি কোন রোলে কত গুরুত্বপূর্ণ, ইত্যাদি) অতিরিক্ত জটিলতা যোগ করতো যেখানে ব্যবসায়িক প্রয়োজন এখনো এতটা স্পষ্ট না। unweighted average দিয়ে শুরু করে, প্রয়োজন হলে পরে per-role weight যোগ করা সহজ — উল্টোটা (weighted থেকে unweighted-এ ফেরা) করলে পুরনো ডেটার সাথে ইনকনসিস্টেন্সি তৈরি হতো।

**প্রশ্ন: `PerformanceReview.kpisForEmployee()` কেন লাইভ কম্পিউট হয়, review সেভ করার সময় freeze করা হয় না কেন?**
উত্তর: KPI ডেটা (attendance %, completed tasks) অন্য মডিউল থেকে আসা **সত্যিকারের, চলমান তথ্য**। যদি একটা review তৈরির মুহূর্তে freeze করা হতো, তাহলে "এই কর্মচারীর গত ৩ মাসের attendance" — এই একই প্রশ্নের উত্তর review পেজে একরকম, আর সরাসরি Attendance রিপোর্টে গিয়ে দেখলে আরেকরকম হতে পারতো (যদি এর মধ্যে backdated leave approval এসে ডেটা বদলে যায়)। লাইভ কম্পিউট করে এই দুই জায়গার মধ্যে কখনো বৈষম্য তৈরি হয় না — এটা "single source of truth" নীতির একটা প্র্যাক্টিক্যাল প্রয়োগ।

**প্রশ্ন: Department delete করার আগে "employee আছে কিনা" চেক করা কেন একটা business-rule validation, শুধু foreign-key constraint দিয়ে কেন যথেষ্ট না?**
উত্তর: ডেটাবেজ-লেভেলে foreign-key constraint দিয়েও এটা আটকানো যেত (ON DELETE RESTRICT), কিন্তু তাহলে ইউজার একটা raw SQL constraint violation error (500) পেত, কোনো readable ব্যাখ্যা ছাড়াই। Application-লেভেলে explicit চেক করে একটা readable `BadRequestException("Reassign employees first")` দেওয়া হয় — ইউজার বোঝে কেন এবং কী করতে হবে। এটা "fail gracefully with a clear message" নীতির উদাহরণ, শুধু ডেটা-ইন্টিগ্রিটি রক্ষা করাই যথেষ্ট না, ইউজার-এক্সপেরিয়েন্সও গুরুত্বপূর্ণ।
