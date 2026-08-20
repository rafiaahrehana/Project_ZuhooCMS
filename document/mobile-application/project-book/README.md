# Zuhoo Android App — সম্পূর্ণ প্রজেক্ট গাইড ও ইন্টারভিউ প্রস্তুতি বই

> একজন শিক্ষকের মতো, ফাইলে-ফাইলে, মেথডে-মেথডে বুঝিয়ে দেওয়া একটা বই — **Zuhoo Android app** (`frontend/Zuhoo`) নিয়ে। মূলত বাংলায় লেখা, টেকনিক্যাল term ইংরেজিতে। উদ্দেশ্য: এই প্রজেক্ট, Android, Java নিয়ে যেকোনো চাকরির ইন্টারভিউয়ের জন্য প্রস্তুতি, আর প্রজেক্ট নিয়ে ভিডিও/প্রেজেন্টেশন বানানোর রেফারেন্স।

## সূচিপত্র (Table of Contents)

| অধ্যায় | বিষয় |
|---|---|
| [০১](01-project-intro.md) | প্রজেক্ট পরিচিতি — কী, কেন, কারা ব্যবহার করবে |
| [০২](02-tech-stack-architecture.md) | টেক স্ট্যাক ও আর্কিটেকচার — MVVM, DI, Retrofit, Room, STOMP chat, security |
| [০৩](03-android-basics.md) | Android বেসিক ধারণা — Activity lifecycle, Intent, ViewBinding, RecyclerView, LiveData, Gradle |
| [০৪](04-data-layer.md) | Data Layer গভীরভাবে — `ApiClient`, `TokenAuthenticator`, `CachedListViewModel`, Room cache, DI |
| [০৫](05-auth-dashboard-account-common.md) | Authentication, Dashboard, Account ও Shared Components |
| [০৬](06-hr-modules.md) | HR মডিউল — Leave, Expense, Timesheet, Attendance (Selfie+GPS), Payroll |
| [০৭](07-finance-billing.md) | Finance ও Billing — Invoice, SSLCommerz Payment, Wallet, Catalog |
| [০৮](08-service-support.md) | Service Request (dynamic form, real-time chat), Support Ticket, KB, Notice Board, Notification |
| [০৯](09-crm-misc.md) | CRM/Leads, Employee Directory, Global Search, Company Overview |
| [১০](10-design-system-case-study.md) | **Design System ও UI/UX Redesign — সম্পূর্ণ Case Study** (সেরা interview গল্প) |
| [১১](11-interview-qa.md) | ইন্টারভিউ প্রশ্নোত্তর — Java, Android, Architecture, Networking, প্রজেক্ট-নির্দিষ্ট STAR-method প্রশ্ন |
| [১২](12-full-file-index.md) | পরিশিষ্ট — সম্পূর্ণ ফাইল তালিকা (২৪৭টা ফাইল, এক লাইনে বর্ণনা সহ) |

## কীভাবে পড়বে

- **প্রথমবার পড়ছ?** ক্রম অনুযায়ী ০১ থেকে ১২ পর্যন্ত পড়ো।
- **ইন্টারভিউ আগামীকাল?** সরাসরি অধ্যায় ১০ আর ১১ পড়ো, তারপর যে মডিউল নিয়ে সবচেয়ে বেশি প্রশ্ন হতে পারে মনে হয় (৫-৯) সেটা refresh করো।
- **নির্দিষ্ট একটা ফাইল খুঁজছ?** অধ্যায় ১২ (Appendix)-এ Ctrl+F করো, তারপর যে chapter number রেফারেন্স দেওয়া আছে সেখানে যাও।
- **ভিডিও বানাচ্ছ?** অধ্যায় ১০ (Design System case study) সবচেয়ে ভালো narrative — before/after, "কেন এভাবে করা হলো" এর গল্প।

## সম্পর্কিত ডকুমেন্ট

- [`docs/android-client-app-plan.md`](../android-client-app-plan.md) — এই প্রজেক্টের আসল as-built পরিকল্পনা ডকুমেন্ট, API-endpoint-লেভেল বিস্তারিত সহ। এই বই এটার উপর ভিত্তি করেই লেখা, কিন্তু শেখার জন্য সহজ করে, ব্যাখ্যা সহ।
- `docs/project-book/presentation-bn.pptx` ও `presentation-en.pptx` — এই বইয়ের সংক্ষিপ্ত, উপস্থাপনযোগ্য সংস্করণ (বাংলা ও ইংরেজি)।

## প্রজেক্টের সংক্ষিপ্ত পরিসংখ্যান

| মেট্রিক | সংখ্যা |
|---|---|
| ভাষা | Java |
| Architecture | MVVM (আংশিক) + Repository Pattern |
| Activity | ৪৫টা |
| ViewModel | ৮টা |
| মোট Java ফাইল | ২৪৭টা |
| API endpoint ব্যবহৃত | ৬৩+ |
| String resource | ৫৬৮টা × ২ ভাষা |
| এই বইয়ের মোট অধ্যায় | ১২টা |
