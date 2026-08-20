# অধ্যায় ১৬ — এই প্রজেক্ট নিয়ে সরাসরি প্রশ্ন

> এগুলো এমন প্রশ্ন যা একজন ইন্টারভিউয়ার সরাসরি "আপনার প্রজেক্ট" নিয়ে জিজ্ঞেস করবে — architecture-এর পেছনের কারণ, tradeoffs, আর আপনি যেসব bug ধরেছেন/ঠিক করেছেন।

## সাধারণ প্রজেক্ট প্রশ্ন

**প্রশ্ন: আপনার প্রজেক্টটা এক মিনিটে বর্ণনা করুন।**
উত্তর: এটা একটা multi-tenant B2B SaaS প্ল্যাটফর্ম (ZuhooCMS) — Spring Boot ব্যাকএন্ড, Angular ফ্রন্টএন্ড। ছোট-মাঝারি ব্যবসার জন্য একসাথে CRM, HR/Recruitment, Finance/Accounting, Service Desk, IT Asset Management — সবকিছু এক জায়গায়। "Multi-tenant" মানে একই কোডবেস, একই ডেটাবেজ, কিন্তু প্রতিটা কোম্পানির ডেটা Hibernate-এর row-level filter দিয়ে সম্পূর্ণ আলাদা রাখা হয় — কোনো কোম্পানি অন্য কোম্পানির ডেটা কখনো দেখতে পায় না।

**প্রশ্ন: এই প্রজেক্টে আপনার নিজের অবদান কী ছিল?**
(এটা ব্যক্তিগত — নিজের ভাষায় বলুন। এই বইয়ে ডকুমেন্টেড রিয়েল কাজের উদাহরণ: Recruitment/ATS মডিউল Candidate-JobApplication স্প্লিট থেকে শুরু করে সম্পূর্ণ CV-parsing/ATS-scoring ফিচার বিল্ড করা, ১০টা রিয়েল বিজনেস-লজিক বাগ খুঁজে বের করে ফিক্স করা (একটা মাল্টি-এজেন্ট কোড-রিভিউ অডিট দিয়ে), একটা CSS বাগ ঠিক করা যেখানে সাইডবার-এর active state সম্পূর্ণ অদৃশ্য ছিল, আর একটা AI মডেল ID বাগ ধরা যা Claude ইন্টিগ্রেশনকে সম্পূর্ণ অকেজো করে রেখেছিল।)

## Multi-tenancy সম্পর্কিত

**প্রশ্ন: কেন আলাদা ডেটাবেজ প্রতি-কোম্পানির বদলে একটাই শেয়ার্ড ডেটাবেজ + row-level filtering বেছে নিলেন?**
উত্তর: "Single database, shared schema" মডেল অপারেশনাল দিক থেকে অনেক সহজ — একটা মাইগ্রেশন, একটা ব্যাকআপ, একটা কানেকশন পুল, নতুন কোম্পানির জন্য কোনো infra প্রভিশনিং লাগে না (শুধু একটা row তৈরি হয়)। খরচ হলো: প্রতিটা কোম্পানির ডেটা isolate রাখার দায়িত্ব app-লেভেলে পড়ে (Hibernate `@Filter`), আর একটা মিসড filter cross-tenant leak হতে পারে (যা এই কোডবেসেই ৪টা entity-তে সত্যিই ঘটেছে — `@Filter` মিসিং)। বিপরীত মডেল (per-tenant ডেটাবেজ) সম্পূর্ণ isolation দেয় কিন্তু অপারেশনাল ওভারহেড অনেক বেশি — হাজার হাজার কোম্পানির জন্য practical না।

**প্রশ্ন: যদি একটা নতুন entity বানান যেটাতে tenant filter দিতে ভুলে যান, কী হয়?**
উত্তর: সেই entity-র জন্য tenant isolation পুরোপুরি নির্ভর করবে repository method-এ ম্যানুয়াল `WHERE company_id = ?` লেখার উপর — একটা মিসড query cross-tenant data leak করে দিতে পারে। এই প্রজেক্টে ঠিক এই সমস্যা `JobOffer`, `Candidate`, `Interview`, `TalentPoolCandidate`-এ পাওয়া গেছে — defense-in-depth কম, কিন্তু এখনো exploit না হওয়ার কারণ সব query manually company-scoped।

## বাগ-ফিক্সিং প্রশ্ন (সবচেয়ে ইন্টারেস্টিং জায়গা)

**প্রশ্ন: একটা কঠিন বাগ বলুন যা আপনি ধরেছেন এবং ফিক্স করেছেন।**
উত্তর (Claude AI মডেল আইডি বাগ): AI Settings-এ Claude প্রোভাইডার কনফিগার করা যায়, কিন্তু ডেটাবেজ চেক করে দেখা গেল দুইটা কোম্পানি Claude configure করে (আসল API key দিয়ে) একটাও সফল conversation পায়নি — শেষে দুটোই Gemini-তে ফিরে গেছে। কোড খুঁজে দেখা গেল `AiModel.java`-তে `CLAUDE_SONNET`-এর model ID `"claude-sonnet-4-6"` — এই মডেল নামটাই Anthropic-এ বাস্তবে **নেই**। ClaudeClient-এর নিজের কোড (auth header, request format) একদম ঠিক ছিল, শুধু model string ভুল ছিল। এই বাগটা শুধু কোড পড়ে ধরা কঠিন ছিল — আসল সংকেত ছিল **লাইভ ডেটা** (কেউ Claude configure করেছে কিন্তু কোনো successful conversation লগ নেই) যা static code review-তে ধরা পড়ত না।

**প্রশ্ন: CSS-এ কীভাবে একটা "invisible bug" ধরলেন যেখানে সাইডবারের active state দেখাই যেত না?**
উত্তর: ইউজার রিপোর্ট করেছিল একটা সাইডবার আইকন দেখা যায় না। খুঁজে দেখা গেল দুইটা আলাদা dead CSS block ছিল — একটাতে দুইটা সরাসরি বিপরীতমুখী কমেন্ট ছিল একই সিলেক্টরের উপরে (একটা বলছে "child items পায় নিজের icon", তার ঠিক নিচেই আরেকটা বলছে "icon শুধু group header-এর জন্য reserved") — আর আসল CSS rule (`display: none`) দ্বিতীয়, ভুল কমেন্টের সাথে মিলে গিয়েছিল। আরেকটা জায়গায়, একটা পুরনো ডিজাইনের leftover rule `background: #ffffff !important` করে দিচ্ছিল active row-কে — যেটা সাদা সাইডবার ব্যাকগ্রাউন্ডের উপর সম্পূর্ণ মিশে অদৃশ্য হয়ে যাচ্ছিল। এই bug-টা "invisible" ছিল কারণ **`!important` স্পেসিফিসিটি ছাড়িয়ে জেতে** — component-এর নিজস্ব (সঠিক) স্টাইল non-important হওয়ায় হেরে যাচ্ছিল।

**প্রশ্ন: ব্যাকএন্ডে একটা bug বলুন যেখানে state transition ভুল ছিল।**
উত্তর: রিক্রুটমেন্ট মডিউলে `updateStatus()`-এ একটা guard ছিল যা `HIRED`/offer-sub-status-এ **সরাসরি সেট করা** আটকাতো, কিন্তু সেখান থেকে **বেরও হতে দিতো** — যদি একটা application ইতিমধ্যে `OFFER_SENT` থাকে, generic dropdown দিয়ে সেটাকে অন্য কোথাও সরানো যেত, যেটা ঠিক ততটাই বিপজ্জনক (দুইটা আলাদা কোড-পাথ থেকে একটা candidate-কে দুইবার অফার ইমেইল পাঠানোর সম্ভাবনা)। ফিক্স: guard-টা দুই দিকেই — না ঢুকতে দেয়, না বেরোতে দেয় জেনেরিক পাথ দিয়ে।

## ডিজাইন Tradeoff প্রশ্ন

**প্রশ্ন: কেন AI-ভিত্তিক ATS স্কোর কখনো auto-reject করে না?**
উত্তর: Resume parsing আর keyword matching নিখুঁত না — একজন ভালো candidate-এর resume হয়তো ভিন্নভাবে ফরম্যাট করা, বা parsing ভুল করে ফেলতে পারে। যদি auto-reject হতো, একটা false-negative সরাসরি একজন যোগ্য প্রার্থীকে হারাতো, কোনো মানুষ কখনো জানতেই পারতো না। তাই ATS score সবসময় শুধু একটা "সংকেত" যা recruiter-এর কাছে দেখানো হয়, সিদ্ধান্ত recruiter-ই নেয় — UI-তেও স্পষ্ট লেখা "Automated signal — confirm manually"।

**প্রশ্ন: কেন `ddl-auto=update` ব্যবহার করা হলো, Flyway/Liquibase কেন না?**
উত্তর: দ্রুত ইটারেশনের জন্য সহজ — নতুন ফিল্ড যোগ করলেই কাজ হয়ে যায়, আলাদা migration স্ক্রিপ্ট লিখতে হয় না। খরচ: **`NOT NULL` কলাম যোগ করা যায় না বিদ্যমান ডেটাযুক্ত টেবিলে** — তাই প্রতিটা নতুন ফিল্ড nullable রাখতে হয়, আর বড় স্কিমা পরিবর্তনের জন্য (যেমন Candidate entity স্প্লিট) বুট-টাইম `ApplicationRunner` দিয়ে one-time data-migration লিখতে হয়েছে (idempotent — প্রতিবার বুটে সেফলি রি-রান করা যায়)। প্রোডাকশন-গ্রেড অ্যাপে সাধারণত Flyway/Liquibase বেছে নেওয়া হয় কারণ এটা migration history ট্র্যাক করে, rollback সাপোর্ট করে — এই প্রজেক্টের সীমাবদ্ধতা সম্পর্কে সচেতন থাকা একটা ভালো ইন্টারভিউ উত্তর।

**প্রশ্ন: Wallet-এর টাকা কেন General Ledger-এ যায় না?**
উত্তর: Wallet একটা প্রি-পেইড ইন-অ্যাপ-সার্ভিস ব্যালেন্স, কোম্পানির অফিসিয়াল আয়-ব্যয় হিসাবের অংশ না। GL-এ টপ-আপ পোস্ট হলে এটা ভুলভাবে "আয়" হিসেবে গণনা হয়ে ফাইন্যান্সিয়াল রিপোর্ট বিকৃত করে ফেলতো। এটা একটা স্পষ্ট **domain boundary** সিদ্ধান্ত — শুধু টাকা জড়িত থাকলেই সেটা "accounting" না।

**প্রশ্ন: কেন AI Assistant একটা agentic tool-calling সিস্টেম না, শুধু prompt-augmented generation?**
উত্তর: Tool-calling/agentic ডিজাইন অনেক বেশি জটিল এবং risk বহন করে (AI নিজে decide করে কোন ডেটা query করবে, ভুল হলে ভুল ডেটা টেনে আনতে পারে বা অপ্রত্যাশিত সাইড-ইফেক্ট ঘটাতে পারে)। এই প্রজেক্টে বেছে নেওয়া হয়েছে একটা সরল, predictable প্যাটার্ন — নির্দিষ্ট, hardcoded backend সার্ভিস মেথড রিয়েল ডেটা টেনে prompt-এ বসায়, AI শুধু সেই prompt থেকে টেক্সট জেনারেট করে। কম flexible, কিন্তু অনেক বেশি নির্ভরযোগ্য এবং debug করা সহজ।

## Scalability/Performance প্রশ্ন

**প্রশ্ন: এই প্রজেক্টে কোথায় pessimistic locking ব্যবহার হয়েছে, কেন?**
উত্তর: যেখানেই একটা সীমিত রিসোর্স (asset assignment, package quota, seat availability) একই সাথে দুইজন consume করার race condition থাকতে পারে — `findByIdAndCompanyIdForUpdate` (SQL `SELECT FOR UPDATE`) ব্যবহার করে প্রথম ট্রানজ্যাকশন শেষ না হওয়া পর্যন্ত দ্বিতীয়টা অপেক্ষা করে, নিশ্চিত করে কখনো দুইজনকে একই asset assign হয় না বা quota-র বেশি consume হয় না।

**প্রশ্ন: Async processing কোথায় কোথায় ব্যবহার হয়েছে, কেন synchronous না?**
উত্তর: যেকোনো ধীর অপারেশন যা ইউজারের অপেক্ষা করার দরকার নেই — ইমেইল পাঠানো, CV parsing/ATS scoring, নোটিফিকেশন পাঠানো। এগুলো `@Async` দিয়ে ব্যাকগ্রাউন্ডে চলে, ইউজার সাথে সাথেই রেসপন্স পায়, কাজ শেষ হওয়ার জন্য অপেক্ষা করতে হয় না। ব্যতিক্রম: `sendClientPortalInviteEmail` ইচ্ছাকৃতভাবে synchronous রাখা হয়েছে, কারণ ইউজার একটা confirmation dialog-এ অপেক্ষা করছে আর `@Async` মেথডের exception caller-এ propagate হয় না — এখানে "ইমেইল পাঠানো হয়েছে" মিথ্যা বলার চেয়ে সামান্য অপেক্ষা করানো ভালো।

## ক্রস-মডিউল ডিজাইন প্রশ্ন

**প্রশ্ন: এই প্রজেক্টে "soft-delete" প্যাটার্ন সবখানে ব্যবহার করা হয়েছে, কখনো hard-delete না কেন?**
উত্তর: ব্যবসায়িক ডেটা সাধারণত ইতিহাস হিসেবে গুরুত্বপূর্ণ — একটা মুছে ফেলা Employee-র পুরনো Payroll রেকর্ড, একটা মুছে ফেলা Client-এর পুরনো Invoice — এসব রেফারেন্স ভেঙে গেলে (foreign key null বা orphan হয়ে গেলে) রিপোর্ট/অডিট ভেঙে যেত। Soft-delete (`deleted=true` + `@SQLRestriction`) দিয়ে রেকর্ড "অদৃশ্য" হয়ে যায় সাধারণ UI/query থেকে, কিন্তু ডেটা এবং তার সম্পর্ক অক্ষত থাকে — দরকার হলে recover করা সম্ভব, আর historical রিপোর্ট সবসময় সঠিক থাকে।

**প্রশ্ন: এই প্রজেক্টে "cannot delete if referenced" প্যাটার্ন কতবার দেখা যায়, একটা কমন উদাহরণ দাও।**
উত্তর: এটা একটা বারবার-দেখা রক্ষণাত্মক প্যাটার্ন — Department (employee থাকলে), Designation (assigned employee থাকলে), Shift (assigned employee থাকলে), Client (open opportunity থাকলে), CompanyLeavePolicy-জাতীয় configuration entity, Loan (repayment হয়ে গেলে cancel করা যায় না)। প্রতিটাতে যুক্তি একই: একটা "referenced" রেকর্ড মুছে ফেললে, যেসব জায়গা থেকে সেটাকে রেফার করা হচ্ছিল, সেগুলো broken/অর্থহীন হয়ে যেত। Application-লেভেলে explicit চেক করে readable এরর মেসেজ দেওয়া, শুধু ডেটাবেজ constraint-এর উপর নির্ভর না করা — একটা কনসিসটেন্ট ডিজাইন ভাষা যা পুরো কোডবেসে অনুসৃত হয়েছে।

**প্রশ্ন: AI ফিচার যেখানেই আছে (Announcement Draft, Leave Policy Draft, AI Summary), সবগুলোই কি একই প্যাটার্ন অনুসরণ করে?**
উত্তর: হ্যাঁ — একটা কমন প্যাটার্ন: (১) প্রাসঙ্গিক real ডেটা fetch করা, (২) একটা `PromptBuilder` ক্লাস দিয়ে সেই ডেটা একটা স্ট্রাকচার্ড prompt-এ ফরম্যাট করা, (৩) `aiService.generateRaw(AiFeature.X, prompt)` কল করা, (৪) ফলাফল ইউজারকে দেখানো (edit-এবল, সরাসরি সেভ হয় না বেশিরভাগ ক্ষেত্রে)। AI-সংশ্লিষ্ট মেথডগুলো প্রায়ই একটা "split transaction" প্যাটার্ন ব্যবহার করে (`AiTransactionBoundary`) — ডেটা লোড/প্রম্পট-বিল্ড একটা ছোট transaction-এ (কানেকশন দ্রুত রিলিজ করার জন্য), তারপর ধীর AI কল কোনো open transaction ছাড়াই।

## Interview-এর শেষ কথা

**একটা ইন্টারভিউয়ার যদি জিজ্ঞেস করে "আপনার প্রজেক্টে কী উন্নতি করতেন যদি আরও সময় পেতেন":**
সৎ উত্তর দেওয়ার জন্য এই প্রজেক্টেই কয়েকটা রিয়েল উদাহরণ আছে —
- ৪টা entity-তে missing tenant `@Filter` (JobOffer, Candidate, Interview, TalentPoolCandidate) — ঠিক করা যেত defense-in-depth বাড়ানোর জন্য
- Feature Flag সিস্টেম সম্পূর্ণ করা — এখন শুধু admin toggle আছে, কোথাও enforce হয় না
- Leave accrual/carry-forward ইঞ্জিন — এখন ফ্ল্যাট বার্ষিক এনটাইটেলমেন্ট, প্রকৃত মাসিক accrual নেই
- Biometric matching-কে একটা রিয়েল fingerprint algorithm (SourceAFIS) দিয়ে replace করা
- কাস্টম `ThreadPoolTaskExecutor` কনফিগার করা `@Async`-এর জন্য (এখন Spring Boot-এর ডিফল্ট)

এই ধরনের উত্তর দেখায় যে আপনি কোডবেস শুধু ব্যবহার করেননি, গভীরভাবে বুঝেছেন — এবং honest সীমাবদ্ধতা স্বীকার করাটা একটা সিনিয়র-লেভেল সিগন্যাল, "সব perfect ছিল" বলার চেয়ে।
