# অধ্যায় ৮ — Time & Leave এবং Payroll মডিউল

> **কোড লোকেশন:** Backend: `backend/BusinessOS/src/main/java/com/businessos/modules/hrm/{attendance,leave,salary,payroll}` · Frontend: `frontend/BusinessFlow/src/app/modules/{attendance,hrm}`

## ৮.১ Attendance — Clock In/Out

**Worked-hours হিসাব:** `(checkOutTime − checkInTime) মিনিটে / ৬০`, HALF_UP রাউন্ডিং, ২ দশমিক। কোনো lunch-break বিয়োগ হয় না।

**Late detection:**
```java
lateMinutes = checkInTime - shift.startTime (মিনিটে);
if (lateMinutes > shift.gracePeriodMinutes) { isLate = true; status = LATE; }
```

**`AbsenteeMarkingService`** — প্রতি রাতে চলা একটা scheduler যেটা প্রতিটা active employee-র জন্য চেক করে: weekly-off দিন হলে স্কিপ; approved leave থাকলে `ON_LEAVE` রেকর্ড তৈরি করে (আগে এটা silently skip হতো, dashboard-এর "% on leave" স্ট্যাট ভুল দেখাতো); নাহলে `ABSENT` রেকর্ড তৈরি করে। এই ABSENT/ON_LEAVE রেকর্ডই পরে payroll-এর attendance-deduction পড়ে।

`settleIncompleteDays` — check-in আছে কিন্তু check-out নেই এমন পুরনো দিনের রেকর্ড অটোমেটিক ABSENT-এ ফ্লিপ হয়ে যায় (অসম্পূর্ণ দিন বেতনযোগ্য না, তাই lateness-এর কোনো মানে নেই এতে)।

**Timesheet** — Attendance থেকে আলাদা একটা self-reported লগ (Attendance-এর অ্যাগ্রিগেশন না)। `hoursWorked` ম্যানুয়ালি এন্টার করা। শুধু **approved billable hours** payroll-এ যায় (`billablePay = billableRate × approvedBillableHours`)।

**Biometric matching — এটা একটা স্টাব, প্রোডাকশন-রেডি না।** কোড কমেন্টেই লেখা আছে: আসল fingerprint matching algorithm (যেমন SourceAFIS) ব্যবহার করা উচিত প্রোডাকশনে। বর্তমানে দুটো টেমপ্লেট স্ট্রিং **ক্যারেক্টার-বাই-ক্যারেক্টার** তুলনা করে একটা match score বের করে — ডেমো-উপযোগী, বাস্তব বায়োমেট্রিক না।

## ৮.২ Leave — Application ও Balance

**`apply()` এর ভ্যালিডেশন পাইপলাইন:**
1. তারিখ ঠিক আছে কিনা
2. **Backdating নিষিদ্ধ** যদি না approver নিজে (`LEAVE_APPROVE`) আবেদন করছে — একজন employee payroll চলার ঠিক আগে একটা ABSENT দিনকে retroactively approved leave বানিয়ে ফেলতে পারবে না
3. Overlap চেক
4. **`countChargeableDays()`** — রেঞ্জের প্রতিটা দিন হাঁটে, weekly-off আর company holiday বাদ দিয়ে। বৃহস্পতি-রবি একটা আবেদন শুক্র/শনি সপ্তাহান্ত থাকলে ৪ দিন না, **২ দিন** চার্জ হয়
5. Policy-র `maxConsecutiveDays` ক্যাপ
6. Balance চেক (UNPAID বাদে) — না থাকলে policy থেকে **lazily auto-provision** হয়

**Approval-এ একটা গুরুত্বপূর্ণ সিঙ্ক:** APPROVED হলে, `AbsenteeMarkingService` যদি ইতিমধ্যে ঐ তারিখগুলোতে ABSENT রেকর্ড বানিয়ে থাকে, সেগুলো **`ON_LEAVE`-এ রিকনসাইল** হয়ে যায় — যাতে backdated approval-এর পরেও payroll ও রিপোর্ট সঠিক থাকে।

**গ্যাপ — accrual engine নেই:** `annualEntitlement` একটা ফ্ল্যাট বার্ষিক সংখ্যা, প্রতি মাসে prorate হয় না। `maxCarryForward`/`canCarryForward` ফিল্ড আছে কিন্তু বছরের শেষে carry-forward চালানোর কোনো job পাওয়া যায়নি — এটা একটা real gap, ভিডিওতে live-build করার ভালো জায়গা হতে পারে।

## ৮.৩ Payroll — টাকার ইঞ্জিন (সবচেয়ে গুরুত্বপূর্ণ অংশ)

### `SalaryStructure` — কম্পোনেন্ট ব্রেকডাউন

```
basic   = gross × basicPercentage / 100     (ডিফল্ট ৫০%)
hra     = basic × hraPercentage / 100       (ডিফল্ট ৪০%)
fixed   = medical + transport + meal        (টেমপ্লেট থেকে ফিক্সড এমাউন্ট)
special = gross - basic - hra - fixed       (কখনো নেগেটিভ না, বাকিটা শুষে নেয়)
```

### `PayrollSettings` — "একটা দিন/ঘণ্টার দাম কত" এর একমাত্র উৎস

`perDayBasis` — ৪ ধরনের ডিভাইজার: `CALENDAR_DAYS` (মাসের আসল দিন সংখ্যা দিয়ে ভাগ), `FIXED_30`, `FIXED_26` (গার্মেন্টস কনভেনশন — ছোট ডিভাইজার মানে ডিডাকশন বেশি কামড়ায়), `ACTUAL_WORKING_DAYS` (শুক্র/শনি + holiday বাদ)।

`overtimeMultiplier` ডিফল্ট **২.০০** — বাংলাদেশ শ্রম আইন ২০০৬ ধারা ১০৮ অনুযায়ী দ্বিগুণ রেট, কোড কমেন্টে সরাসরি সাইটেশন আছে। `overtimeEnabled` ডিফল্ট **false** — বেতনভুক্ত পদে ডিফল্টে ওভারটাইম নেই।

### মূল ফর্মুলাসমূহ (ইন্টারভিউ-এর জন্য মুখস্থ রাখার মতো)

| কনসেপ্ট | ফর্মুলা |
|---|---|
| প্রতিদিনের রেট | `মাসিক এমাউন্ট / perDayDivisor` |
| Absence deduction | `perDayRate(base) × absentDays` যেখানে `absentDays = ABSENT রেকর্ড + approved UNPAID leave` |
| Overtime hourly rate | `(perDayRate(base) / standardHoursPerDay) × overtimeMultiplier` |
| Overtime pay | `hourlyRate × overtimeHours` |
| Billable pay | `billableRate × approved timesheet billable hours` |
| Loan installment | `min(monthlyInstallment, remainingBalance)` |
| Net salary | `gross − deductions − tax − insurance − PF − attendanceDeduction − extraDeductions − loanDue` |

**গুরুত্বপূর্ণ ডিজাইন — Two-phase loan settlement:** payroll **DRAFT** তৈরির সময় শুধু হিসাব (`min(installment, remaining)`) করে **freeze** করা হয়, কিন্তু loan-এর `remainingBalance` তখনো বদলায় না। শুধু `markPaid()`-এ গিয়ে সত্যিকারের deduction হয়। কেন? কারণ একটা DRAFT বা APPROVED payroll মুছে ফেলা হলে (ভুল হলে), loan balance রিভার্স করার কোনো দরকারই পড়ে না — ওটা তো কখনো বদলায়নি।

**GL পোস্টিং (`markPaid()`-এ):**
```
Dr Salaries & Wages Expense    (gross)
Cr Cash                        (netSalary)
Cr Payroll Payable             (withheld: tax+PF+insurance+deductions+loan)
```

### `PayrollRun` — batch workflow

```
DRAFT → CALCULATED → PENDING_APPROVAL → APPROVED → PAID
                                    ↘ REJECTED (re-enterable)
```

`approve()` প্রতিটা DRAFT লাইনকে লুপ করে approve করে; `pay()` প্রতিটাকে `markPaid()` কল করে — মানে batch পেমেন্টও ঠিক একই GL posting আর loan settlement লজিক দিয়েই যায়, যেটা একটা সিঙ্গেল ম্যানুয়াল পেমেন্টে হতো।

### `SalarySheetService` — Live Preview বনাম Frozen Actual

যদি সেই মাসের `Payroll` রেকর্ড ইতিমধ্যে তৈরি হয়ে থাকে, salary sheet সেটাই দেখায় (frozen)। না থাকলে, **ঠিক একই ফর্মুলা** দিয়ে live হিসাব করে দেখায়। কোড কমেন্টে স্পষ্ট লেখা: এই duplication ইচ্ছাকৃত, যাতে "preview" আর "actual" কখনো একে অপরের সাথে মিথ্যা না বলে।

## ৮.৪ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `attendance/attendance/AttendanceServiceImpl.java` | `checkIn/checkOut`, `applyLateDetection`, `createManual`, `getMyMonthlySummary` |
| `attendance/attendance/AbsenteeMarkingService.java` | রাতের ব্যাচ — ABSENT/ON_LEAVE রেকর্ড তৈরি, `settleIncompleteDays` |
| `attendance/timesheet/TimesheetServiceImpl.java` | `log/submitForReview/approve` — billable hours-এর উৎস |
| `attendance/biometric/*` | Device/enrollment/verification — matching একটা স্টাব |
| `leave/LeaveServiceImpl.java` | `apply` (ভ্যালিডেশন পাইপলাইন), `review` (approve/reject + attendance reconcile), `cancel` |
| `leave/CompanyLeavePolicyServiceImpl.java` | Policy CRUD, `draftWithAi()` |
| `hrm/salary/SalaryStructureServiceImpl.java` | Effective-dated স্ট্রাকচার, auto-expire পুরনোটা |
| `payroll/PayrollServiceImpl.java` | সবচেয়ে বড় ফাইল — `create/generateForAllEmployees`, `calculateOvertime/calculateAttendanceDeduction/calculateLoanDue`, `markPaid` (GL পোস্টিং + লোন সেটেলমেন্ট) |
| `payroll/PayrollRunService.java` | Batch workflow state machine |
| `payroll/PayslipPdfService.java` | openhtmltopdf দিয়ে PDF জেনারেশন |
| `payroll/loan/LoanAdvanceService.java` | Loan lifecycle, one-active-loan-per-employee রুল |

### Frontend

| ফাইল | কাজ |
|---|---|
| `attendance-list.ts` | `consolidatedRecords` (ডুপ্লিকেট মার্জ), owner-only backfill |
| `timesheets.ts` | `weekOvertimeHours` (client-side heuristic) |
| `leaves.ts` | Approver বনাম self view |
| `payroll.ts` | `onEmployeeSelected` (structure prefill), Payroll Run workflow UI |
| `salary-structures.ts` | `autoBalanceSpecial`, `breakupPresets` |
| `my-payslips.ts` | `canViewAll`/`canDownload` — backend gate mirror |
| `salary-sheet.ts` | Live-vs-frozen totals |

## ৮.৫ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: overtime-এর ঘণ্টা কীভাবে গণনা হয়, চেক-ইন/চেক-আউট থেকে অটোমেটিক?**
উত্তর: না — `Attendance.overtimeHours` একটা ম্যানুয়ালি/HR-এন্ট্রি ফিল্ড, শিফট শেষ হওয়ার পরের সময় থেকে অটো-ক্যালকুলেট হয় না। কিন্তু সেই ঘণ্টার **রেট** সম্পূর্ণ অটোমেটিক — `PayrollSettings` থেকে লাইভ কম্পিউট হয়ে payroll জেনারেশনের সময় frozen হয়ে যায়, যাতে পরে কোম্পানির multiplier বদলালে ইতিমধ্যে ইস্যু হওয়া payslip চুপচাপ পাল্টে না যায়।

**প্রশ্ন: চেয়ারড leave chargeable days হিসাব করার সময় ৭ দিনের একটা রেঞ্জ কেন সবসময় ৭ দিন চার্জ করে না?**
উত্তর: কারণ ছুটির মধ্যে যদি সপ্তাহান্ত বা কোম্পানির ছুটির দিন পড়ে, সেই দিনগুলো এমনিতেই কাজের দিন না — সেগুলোর জন্য leave balance খরচ করার কোনো মানে হয় না। `countChargeableDays()` প্রতিটা দিন হেঁটে দেখে, employee-র shift-এর `weeklyOffDays` আর company `Holiday` টেবিল বাদ দিয়ে হিসাব করে।

**প্রশ্ন: Biometric matching-কে "স্টাব" বলছেন কেন, এটা তো ডেটা এনরোল/ভেরিফাই করে?**
উত্তর: এনরোলমেন্ট পাইপলাইন (ডিভাইস রেজিস্ট্রেশন, টেমপ্লেট স্টোরেজ, ক্যাপাসিটি ট্র্যাকিং) সত্যিই বাস্তব ও কার্যকরী। কিন্তু আসল "matching" — দুইটা বায়োমেট্রিক টেমপ্লেট মেলে কিনা যাচাই করার এলগরিদম — এখানে একটা সরল **ক্যারেক্টার-বাই-ক্যারেক্টার স্ট্রিং তুলনা**, প্রকৃত fingerprint-matching এলগরিদম (SourceAFIS-এর মতো) না। কোডের কমেন্টেই স্পষ্ট লেখা "production-এ real algorithm ব্যবহার করা উচিত"। এটা একটা ডেমো-উপযোগী প্লেসহোল্ডার, প্রোডাকশন-রেডি সিকিউরিটি ফিচার না — একজন ইন্টারভিউয়ারকে honestly এটা বলা ভালো, লুকানো ঠিক না।

**প্রশ্ন: `PayrollRun`-এর `approve()` কেন প্রতিটা লাইন আলাদাভাবে লুপ করে approve করে, একটা bulk UPDATE কেন না?**
উত্তর: প্রতিটা `Payroll` লাইনের নিজস্ব `approve()` মেথডে business validation থাকতে পারে (ভবিষ্যতে যোগ হতে পারে, যেমন "শূন্য net salary approve করা যাবে না")। একটা bulk SQL UPDATE এই validation বাইপাস করে যেত। লুপ করে প্রতিটা লাইনের নিজস্ব সার্ভিস মেথড কল করাটা ধীর কিন্তু নিরাপদ — প্রতিটা রেকর্ড ঠিক একই বিজনেস রুল মেনে চলে, তা সে single manual approval হোক বা batch-এর অংশ।
