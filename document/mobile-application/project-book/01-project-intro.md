# অধ্যায় ১ — প্রজেক্ট পরিচিতি (Project Introduction)

> এই বইটা এমনভাবে লেখা হয়েছে যেন একজন শিক্ষক তোমার পাশে বসে পুরো প্রজেক্টটা বুঝিয়ে দিচ্ছেন — প্রতিটা সিদ্ধান্তের পেছনের "কেন" সহ। Interview-এ শুধু "আমি এই feature বানিয়েছি" বললে চলবে না; "কেন এভাবে বানিয়েছি, অন্য উপায়ে বানালে কী সমস্যা হতো" — এটা বলতে পারলেই তুমি বাকিদের থেকে আলাদা হয়ে যাবে। প্রতিটা অধ্যায়ে সেই "কেন"-টার উপর জোর দেওয়া হয়েছে।

## ১.১ প্রজেক্ট কী?

**Zuhoo** একটা multi-tenant SaaS প্ল্যাটফর্মের (ওয়েব ভার্সনের নাম BusinessOS) **native Android companion app**। সহজ বাংলায় — এটা এমন একটা মোবাইল অ্যাপ যেটা দিয়ে একই কোম্পানির customer (client) এবং সেই কোম্পানির staff (owner/employee) — দুই ধরনের মানুষই কাজ করতে পারে, কিন্তু কে কী দেখবে সেটা তাদের **role** অনুযায়ী ঠিক হয়।

Multi-tenant মানে কী? একটাই অ্যাপ, একটাই backend, কিন্তু হাজারটা আলাদা কোম্পানি (tenant) এটা ব্যবহার করতে পারে — প্রত্যেকের ডেটা একে অপরের থেকে সম্পূর্ণ আলাদা ও সুরক্ষিত। যেমন ধরো, Shopify একটা platform, আর তার উপর হাজারটা আলাদা দোকান (tenant) চলে — একজন দোকানদার আরেকজনের অর্ডার দেখতে পারে না। Zuhoo-ও ঠিক তেমন।

## ১.২ কারা ব্যবহার করবে — তিনটা Role

অ্যাপে লগইন করলে সার্ভার থেকে `role` আসে, আর পুরো UI সেই role-এর উপর ভিত্তি করে সাজে (branch করে):

| Role | কে এরা | কী করতে পারে |
|---|---|---|
| **CLIENT** | কোম্পানির একজন গ্রাহক | Service request করা, invoice দেখে payment করা, catalog browse করা, company-র সাথে live chat করা |
| **COMPANY_OWNER** | কোম্পানির মালিক | Client-দের request handle করা, staff-এর কাজ দেখা, platform support-এ ticket তোলা |
| **EMPLOYEE** | কোম্পানির কর্মচারী | Owner-এর মতোই কাজ, কিন্তু কিছু জায়গায় কম permission |

**গুরুত্বপূর্ণ:** এই অ্যাপে `SUPER_ADMIN`, `SYSTEM_ADMIN`-এর মতো platform-staff role নেই — ওরা পুরো SaaS-টা চালায়, সেটা আলাদা (web-only) system। এই অ্যাপ শুধু **একটা tenant-এর ভেতরের** মানুষদের জন্য।

> **Interview tip:** "আপনার অ্যাপে role-based access কীভাবে করলেন?" — এই প্রশ্নের উত্তরে বলবে: role-টা `LoginResponse`-এর সাথে আসে, `TokenManager`-এ save হয়, আর `BottomNavActivity` role অনুযায়ী আলাদা menu (`bottom_nav_client.xml` vs `bottom_nav_staff.xml`) দেখায়। Real authorization backend-এ Spring Security দিয়ে হয় — client-side role check শুধু UI দেখানোর জন্য, security-র জন্য নয় (এটা বলা জরুরি, কারণ interviewer জিজ্ঞেস করতেই পারে "client-side role hide করলেই কি security হয়ে যায়?" — উত্তর: না, real security backend-এ থাকতে হয়)।

## ১.৩ এই অ্যাপ কেন বানানো হলো?

Web app-এ (Angular) ইতিমধ্যে client আর staff — দুজনের জন্যই পোর্টাল ছিল। Android app সেটার **replacement না, companion** — মানে সম্পূর্ণ web app-টা মোবাইলে নিয়ে আসা হয়নি, বরং মোবাইলে যেটা বেশি দরকারি সেটাই আনা হয়েছে:

- একজন client রাস্তায় দাঁড়িয়েই একটা service request করতে পারবে, push notification পাবে status change হলে, invoice pay করতে পারবে।
- একজন staff অফিসে না বসেও client-এর request দেখে respond করতে পারবে, real-time chat করতে পারবে।

**যা এই অ্যাপে নেই (ইচ্ছাকৃতভাবে):** HRM, CRM, Finance, ITAM-এর মতো ভারী admin module — এগুলো ওয়েবেই থাকবে। মোবাইলে সবকিছু আনলে অ্যাপ জটিল হয়ে যেত আর কেউ ব্যবহারও করত না। এটা একটা গুরুত্বপূর্ণ **product decision** — "সবকিছু বানাও" না বলে "যেটা মোবাইলে দরকার সেটাই বানাও"।

## ১.৪ প্রজেক্টের আকার (as-built সংখ্যা)

| Metric | Count |
|---|---|
| Java files | ১৬২টা মূল app কোড + এই বইয়ের redesign-এ যোগ হওয়া নতুন ফাইল |
| Lines of code | ~১২,৩০০+ |
| Activities | ৪৫টা (redesign-এর পর) |
| ViewModels | ৮টা |
| Layout XML | ৭০+ |
| API endpoints ব্যবহৃত | ৬৩+ |
| String resources | ৫৬৮টা × ২ ভাষা (English + বাংলা) |
| Unit tests | ৩৭টা |

## ১.৫ Backend কানেকশন

অ্যাপটা কথা বলে `backend/Zuhoo`-র সাথে, যেটা port **৮০৮৬**-এ চলে। এটা `SAAS-PROJECT/backend/BusinessOS`-এর একটা **source-identical copy** (package নাম শুধু `com.businessos` থেকে `com.zuhoo` করা হয়েছে), আর দুটোই **একই PostgreSQL database** (`businessflow`) শেয়ার করে। তাই Angular web app (port 8085) আর এই Android app — দুটোই একই ডেটা দেখে, একই সাথে চলতে পারে।

> **কেন আলাদা backend কপি করা হলো, একটাই backend ব্যবহার না করে?** — এটা একটা ভালো interview প্রশ্ন হতে পারে নিজেকে জিজ্ঞেস করার মতো। উত্তর: mobile app-এর জন্য কিছু বিশেষ পরিবর্তন দরকার হয়েছিল (device token endpoint, নির্দিষ্ট authorization ওপেন করা) যেগুলো মূল BusinessOS প্রজেক্টের সাথে সরাসরি মিশিয়ে দিলে ঝুঁকি বাড়তো। Same-database কিন্তু separate deployment রাখাটা একটা middle-ground সিদ্ধান্ত।

## ১.৬ এই বই কীভাবে পড়বে

1. **অধ্যায় ২-৪**: Architecture, tech stack, Android basics, data layer — এগুলো না বুঝলে বাকি সব মুখস্থ মনে হবে। আগে এগুলো ভালোভাবে পড়ো।
2. **অধ্যায় ৫-৯**: Module-by-module deep dive — প্রতিটা feature কীভাবে কাজ করে, কোন ফাইলে কী আছে।
3. **অধ্যায় ১০**: এই প্রজেক্টে করা একটা বড় UI/UX redesign-এর case study — এটা interview-তে "একটা challenging কাজের গল্প বলো" প্রশ্নের জন্য সোনার খনি।
4. **অধ্যায় ১১**: সরাসরি interview প্রশ্নোত্তর — Java, Android, Spring Boot concept, আর এই প্রজেক্ট নিয়ে নির্দিষ্ট প্রশ্ন।
5. **Appendix**: সব ফাইলের তালিকা, এক লাইনে কী কাজ করে তার বিবরণ সহ।

চলো শুরু করি।
