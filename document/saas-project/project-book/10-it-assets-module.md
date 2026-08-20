# অধ্যায় ১০ — IT Assets মডিউল

> **কোড লোকেশন:** Frontend: `frontend/BusinessFlow/src/app/modules/itam/` · Backend (মূল entity): `backend/BusinessOS/src/main/java/com/businessos/modules/hrm/asset/` (হ্যাঁ, `hrm` প্যাকেজে, `itam`-এ না!)

## ১০.১ একটা স্থাপত্য-বিষয়ক চমক

এই মডিউলের কোনো নিজস্ব "Hardware" entity/controller নেই। `Asset` entity — যেটা `com.businessos.modules.hrm.asset` প্যাকেজে থাকে — একই সাথে HRM-এর "Company Assets" পেজ আর ITAM-এর "Hardware" পেজ, দুটোকেই সার্ভ করে। একই `AssetController` (`/api/hr/assets`), একই `AssetService`। এটা কোড কমেন্টেই স্পষ্ট বলা আছে।

## ১০.২ Hardware Asset — `Asset` Entity

সাধারণ ফিল্ড: `name`, `assetTag`, `brand`, `model`, `serialNumber`, `category`, `purchasePrice/Date`, `warrantyExpiry`, `status` (`AssetStatus`: AVAILABLE/ASSIGNED/UNDER_MAINTENANCE/DISPOSED)। IT-নির্দিষ্ট ফিল্ড: `ipAddress`, `macAddress`, `processorModel`, `ramSize`, `storageSize`, `operatingSystem`।

**`assign(id, employeeId)`** — **pessimistic lock** (`findByIdAndCompanyIdForUpdate`) ব্যবহার করে, যাতে দুইজন admin একই সাথে একই asset assign করার race condition এড়ানো যায়।

**`dispose(id, reason)`** — এখনো assigned থাকলে dispose করা যায় না, আগে unassign করতে হবে।

## ১০.৩ Software License — Seat Tracking

`SoftwareLicense`: `totalSeatsLicensed`/`seatsUsed`/`seatsAvailable`, `licenseStatus` (ACTIVE/EXPIRED/EXPIRING_SOON/SUSPENDED/REVOKED), `isExpiringSoon()` (৩০ দিনের মধ্যে)। `assignSeat()`/`releaseSeat()` — এখানেও pessimistic lock, আর seat আছে কিনা (`seatsAvailable > 0`) চেক করা হয়।

## ১০.৪ Offboarding — ৫-ধাপের Checklist, শুধু "সব রিভোক করে দাও" না

`OffboardingChecklist` — ৫টা স্বাধীন বুলিয়ান ধাপ:
1. `hardwareCollected` 2. `licensesRevoked` 3. `accessRevoked` 4. `dataHandedOver` 5. `exitInterviewCompleted`

**এখানে সবচেয়ে ইন্টারেস্টিং অংশ — দুইটা ধাপ শুধু "রেকর্ড" করে না, "গার্ড" করে:**
- `markHardwareCollected()` — `AssetRepository` চেক করে, employee-তে এখনো কোনো hardware assigned থাকলে **exception ছুঁড়ে দেয়** — আগে Hardware পেজ থেকে সত্যিই unassign করতে হবে, তারপরই এই বক্স টিক দেওয়া যাবে।
- `markLicensesRevoked()` — একই প্যাটার্ন, `SoftwareLicenseSeat` চেক করে।

মানে "Offboarding assignment রিভোক করে দেয়" — এটা **অটোমেটিক না**, বরং এটা নিশ্চিত করে যে অ্যাডমিন আগে অন্য জায়গায় গিয়ে সত্যিই রিভোক করেছে। একটা ইচ্ছাকৃত safety gate।

## ১০.৫ Bulk Asset Import

`AssetImportServiceImpl.importCsv()` — সীমা ৫MB, ১০০০ row। hand-rolled CSV parser (quoted field, escaped `""`, CRLF হ্যান্ডল করে)। **প্রতিটা row নিজস্ব transaction-এ চলে** (`transactionTemplate.executeWithoutResult`) — একটা খারাপ row-এর rollback অন্য সফল row-গুলোকে undo করে না। `employeeNumber` (মানুষ-পড়া যায় এমন, যেমন `EMP-0001`) দিলে অটো-অ্যাসাইন হয়ে যায়, আর `AssetHistory` রেকর্ডও লেখা হয় (আগে এটা মিস হতো, ফলে পরে `unassign()` চুপচাপ কিছুই করতো না)।

## ১০.৬ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: Offboarding checklist কেন Hardware/License automatically রিভোক করে না, শুধু চেক করে?**
উত্তর: automatic revocation ঝুঁকিপূর্ণ — যদি সিস্টেম নিজে থেকেই সব ফিরিয়ে নেয়, admin হয়তো ভুলে যেতে পারে আসলেই ফিজিক্যাল hardware ফেরত পেয়েছে কিনা যাচাই করতে। Guard-স্টাইল ডিজাইন করে বাধ্য করা হয়েছে — আগে সত্যিই hardware/license পেজে গিয়ে explicitly ফেরত/রিভোক করতে হবে, তবেই checklist সেই ধাপটা "সম্পূর্ণ" হিসেবে মানবে। এটা একটা human-verification-first ডিজাইন সিদ্ধান্ত।

**প্রশ্ন: bulk import-এ প্রতিটা row আলাদা transaction-এ কেন, পুরো ফাইল একসাথে না?**
উত্তর: যদি পুরো ফাইল একটা transaction হতো, ১০০০ row-এর মধ্যে একটাতে ভুল থাকলে (যেমন duplicate serial number) পুরো ব্যাচ rollback হয়ে যেত — বাকি ৯৯৯টা ঠিক row-ও বাদ যেত। Per-row transaction দিয়ে, শুধু সমস্যাযুক্ত row-টাই ব্যর্থ হয়, বাকিগুলো সফলভাবে ইমপোর্ট হয়, আর একটা error লিস্ট রিটার্ন হয় ইউজারকে জানানোর জন্য কোন row-গুলো ঠিক করতে হবে।

## ১০.৭ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `hrm/asset/Asset.java` + `AssetServiceImpl` | `assign` (pessimistic lock), `unassign`, `setMaintenance`, `dispose` |
| `itam/software/SoftwareLicenseServiceImpl.java` | `assignSeat`/`releaseSeat` (pessimistic lock), seat availability চেক |
| `itam/offboarding/OffboardingCheckListServiceImpl.java` | ৫-ধাপ checklist, `markHardwareCollected`/`markLicensesRevoked` (cross-entity গার্ড) |
| `itam/AssetImportServiceImpl.java` | Hand-rolled CSV parser, per-row transaction, ৫MB/১০০০-row সীমা |

### Frontend

| ফাইল | কাজ |
|---|---|
| `hardware.ts` | Asset CRUD, assign/unassign UI |
| `assignments.ts` | Read-only history list |
| `offboarding.ts` | Checklist UI, `returnAsset()`/`revokeLicenseSeat()` — ব্লকার সরানোর শর্টকাট |
| `asset-import.ts` | Template download, ফাইল আপলোড, রেজাল্ট সামারি |

**প্রশ্ন: Software License seat assign করার সময় কেন pessimistic lock, optimistic lock না কেন?**
উত্তর: Optimistic locking (একটা `version` কলাম দিয়ে conflict detect করা) কাজ করতো, কিন্তু তাহলে দ্বিতীয় ইউজারকে একটা "conflict, আবার চেষ্টা করুন" এরর দেখাতে হতো এবং সে আবার রিট্রাই করতে হতো। Pessimistic lock দিয়ে দ্বিতীয় রিকোয়েস্ট সহজভাবে **অপেক্ষা করে** প্রথমটা শেষ হওয়া পর্যন্ত, তারপর সঠিক আপ-টু-ডেট `seatsAvailable` দেখে এগোয় — ইউজারকে কোনো ম্যানুয়াল রিট্রাই লজিক দিতে হয় না। যেহেতু seat-assignment একটা কম-ফ্রিকোয়েন্সি অপারেশন (বছরে কয়েকবার), সামান্য ব্লকিং ডিলে কোনো সমস্যা না, আর pessimistic lock কোড অনেক সরল।
