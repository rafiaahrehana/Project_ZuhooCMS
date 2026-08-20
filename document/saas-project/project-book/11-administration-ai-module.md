# অধ্যায় ১১ — Administration ও AI মডিউল

> **কোড লোকেশন:** Administration: `auth/user`, `auth/role`, `modules/company` (Company Profile), `hrm/announcement` · AI: `backend/BusinessOS/src/main/java/com/businessos/modules/ai/`, Frontend: `frontend/BusinessFlow/src/app/modules/ai/`

## ১১.১ Administration — কে কী করতে পারে

Administration গ্রুপটা "যা মাসে একবারও খোলা হয় না" — কে লগইন করতে পারবে, কী করার অনুমতি আছে, আর কোম্পানির প্রোফাইল/AI/সিকিউরিটি সেটিংস — সব একসাথে। (আগে এর নাম ছিল "Authentication" — একটা রিনেম কমিটে ঠিক করা হয়েছে, কারণ পুরনো নামটা গ্রুপের একটা কাজকেই বর্ণনা করতো, বাকিগুলোকে না।)

### Users ও Roles & Permissions

প্রতিটা তথ্য পুরো Permission সিস্টেম অধ্যায় ২-এ বিস্তারিত আছে — এখানে সংক্ষেপে: `User.role` (coarse Spring Security role) আর `User.customRole` (fine-grained, tenant-নিজস্ব) — দুই স্তর। `COMPANY_OWNER` role-এর ইউজার সবসময় সব permission পায় (`hasPermission()`-এ hardcoded bypass)। বাকি সবাই তাদের `CustomRole`-এর সাথে যুক্ত `RolePermission` টেবিল দিয়ে নির্ধারিত হয়। **Roles & Permissions পেজ শুধু `COMPANY_OWNER`-এর জন্য** — একজন সাধারণ ম্যানেজারও এই পেজে ঢুকতে পারবে না, এমনকি user-management permission থাকলেও।

### Company Profile

কোম্পানির নিজস্ব তথ্য — নাম, ঠিকানা, fiscal year শুরু, base currency, bank info — সব একসাথে। আগে নাম ছিল "Billing Settings" যা এই পেজের সবটুকু কাজ বোঝাতো না।

### AI Settings

নিচে ১১.২-এ বিস্তারিত।

## ১১.২ AI মডিউল — গভীর বিশ্লেষণ

### AI Settings পেজ

**৫টা প্রোভাইডার সাপোর্টেড:** `GEMINI`, `CLAUDE`, `OPENAI`, `GROQ`, আর `MOCK` (একটা fake প্রোভাইডার — টেস্টিং/ডেমোর জন্য, আসল API কল হয় না)।

**কনফিগারেশন মডেল — একাধিক config, একটাই active:** একটা কোম্পানি প্রতিটা প্রোভাইডারের জন্য আলাদা config সেভ করে রাখতে পারে (পাশাপাশি), কিন্তু যেকোনো সময় শুধু **একটাই "Active"**। নতুন config সেভ করলে অটোমেটিক আগেরটা deactivate হয়ে যায়। **"platform default ব্যবহার করো" — এমন কোনো এক্সপ্লিসিট টগল নেই** — এটা implicit: কোম্পানির কোনো active config না থাকলেই সিস্টেম প্ল্যাটফর্ম ডিফল্টে পড়ে যায়।

**৩-ধাপের resolution cascade (`AiProviderResolver.resolve()`):**
1. কোম্পানির নিজের active config
2. প্ল্যাটফর্ম-জোড়া ফলব্যাক config (`company_id IS NULL`, প্ল্যাটফর্ম admin সেট করে)
3. Hardcoded `application.properties` ডিফল্ট (`ai.default-provider=GEMINI`)

মানে **একটা নতুন কোম্পানি শূন্য সেটআপেও AI ব্যবহার করতে পারে**, প্ল্যাটফর্মের নিজের key-তে চড়ে, যতক্ষণ না তারা নিজেদের key যোগ করে।

**Rate limiting:** ২০০ রিকোয়েস্ট/দিন প্রতি কোম্পানি, ২০ রিকোয়েস্ট/ঘণ্টা প্রতি ইউজার — হার্ড ক্যাপ, UI থেকে এডিট করা যায় না, শুধু `application.properties`-এ।

**Prompt Templates:** ১৩টা `AiFeature`-এর প্রতিটার জন্য কোম্পানি নিজের custom prompt সেভ করে বিল্ট-ইন ডিফল্ট ওভাররাইড করতে পারে — এটা কোনো on/off টগল না, বরং self-service prompt engineering। Versioned, পুরনো ভার্সন deactivated থাকে নতুনটা সেভ করলে।

### AI Assistant — এটা আসলে কী

**একটা ফ্রি-ফর্ম চ্যাটবট না** — বরং "একটা ফিচার বেছে নাও, প্রম্পট লেখো, জেনারেট করো" স্টাইলের টুল। প্রতিটা জেনারেশন এক-শট রিকোয়েস্ট/রেসপন্স, মাল্টি-টার্ন কনভার্সেশন থ্রেড না।

**কোনো real function/tool-calling নেই** — Claude/OpenAI/Gemini/Groq ক্লায়েন্টের কোনোটাতেই `tools`/`function_call` প্যারামিটার পাঠানো হয় না। এটা একটা **"prompt augmented with real data, তারপর plain text completion"** প্যাটার্ন:

- বেশিরভাগ ফিচারের জন্য জেনেরিক `POST /api/ai/generate` — ইউজারকে নিজেই প্রম্পটে context টাইপ করতে হয়
- **৩টা ফিচার বিশেষভাবে wired** — নিজেদের মডিউলের endpoint দিয়ে যায়, আর *সেগুলো* সত্যিই real ডেটা টেনে আনে prompt-এ বসানোর আগে: **Announcement Draft**, **Holiday Draft**, **Leave Policy Draft** — এই তিনটা `{title, body}`/`{name, date...}` স্ট্রাকচার্ড রেসপন্স রিটার্ন করে, "Save as..." বাটন দিয়ে সরাসরি রিয়েল রেকর্ড তৈরি করা যায়

মানে AI Assistant একদিকে সীমিতভাবে "real ডেটা পড়তে" পারে (এই ৩টা ফিচারে), কিন্তু "invoice #123 দেখাও" স্টাইলের arbitrary query করতে পারে না — একটা agentic assistant যা করতো, এটা তা করে না।

**Conversation history সেভ হয়:** প্রতিটা `/generate` কল `AiConversation`-এ পার্সিস্ট হয় (UUID, feature, provider, model, পুরো request/response payload) — "Recent Conversations" প্যানেল এখান থেকেই আসে।

### AI অন্য মডিউলে কোথায় কোথায় দেখা যায়

| জায়গা | কী করে |
|---|---|
| HRM → Offer Letters | "Draft with AI" |
| HRM → Announcements/Holidays/Leave Policies | "Draft with AI" (স্ট্রাকচার্ড, save-ready) |
| HRM → Performance Reviews | per-row "AI Summary" |
| CRM → Leads/Client Detail | "AI Summary" + suggested next action |
| Finance → Invoices | "Draft with AI" (সামারি টেক্সট) |
| Service Desk → Request Detail | "AI Summary" |
| Service Desk → Workflows | "Suggest with AI" — একটা লক্ষ্য টাইপ করলে workflow stages সাজেস্ট করে |
| Global Search | "AI Answer" কার্ড — সার্চ রেজাল্ট থেকে সরাসরি উত্তর সংশ্লেষণ |
| Dashboard | "AI Insights" — SLA breach, overdue invoice ইত্যাদি রিয়েল মেট্রিক থেকে ৩টা priority insight |

### Platform-level AI Admin

আলাদা কোনো entity/controller নেই — একই `AiController`/`AiProviderConfig` টেবিল পুনর্ব্যবহার হয়। একজন প্ল্যাটফর্ম অ্যাডমিন লগইন করলে `companyId = null` রেজলভ হয়, তারা যখন `/ai/settings`-এ config সেভ করে, সেটাই `company_id = null` রো হয়ে যায় — যেটা প্রতিটা কোম্পানির জন্য fallback হিসেবে কাজ করে (২-নং cascade স্টেপ)।

## ১১.৩ ইন্টারভিউ প্রশ্ন

**প্রশ্ন: AI Assistant কি একটা সত্যিকারের "agent" — যেটা নিজে ডেটাবেজে query চালিয়ে উত্তর দিতে পারে?**
উত্তর: না। এটা একটা "retrieve-then-generate" প্যাটার্ন, agentic tool-calling না। কোনো AI provider client-এ `tools`/`function_call` প্যারামিটার পাঠানো হয় না। মাত্র ৩টা বিশেষ ফিচার (Announcement/Holiday/Leave-Policy draft) নিজেদের ব্যাকএন্ড সার্ভিসে রিয়েল ডেটা টেনে prompt-এ বসিয়ে দেয় — সেটাও AI নিজে সিদ্ধান্ত নিয়ে করে না, বরং একটা fixed, hardcoded prompt-building পাথ।

**প্রশ্ন: একটা নতুন কোম্পানি কোনো AI key কনফিগার না করেও কীভাবে AI ফিচার ব্যবহার করতে পারে?**
উত্তর: `AiProviderResolver.resolve(companyId)`-এর ৩-ধাপ cascade — প্রথমে কোম্পানির নিজের config খোঁজে, না পেলে platform-wide fallback config (`company_id IS NULL`), সেটাও না থাকলে `application.properties`-এর hardcoded ডিফল্ট (Gemini)। তাই প্রতিটা নতুন কোম্পানি zero-setup-এই AI ব্যবহার করতে পারে, প্ল্যাটফর্মের নিজের কী শেয়ার করে, যতক্ষণ না তারা নিজেদের কী যোগ করে।

## ১১.৪ Key Files রেফারেন্স টেবিল

### Backend

| ফাইল | কাজ |
|---|---|
| `ai/AiServiceImpl.java` | `generate()` — মূল entry point, prompt template resolve, rate limit enforce |
| `ai/AiProviderResolver.java` | ৩-ধাপ cascade (company → platform → hardcoded default) |
| `ai/client/{ClaudeClient,OpenAiClient,GeminiClient,GroqClient,MockAiClient}.java` | একই `AiHttpClient` ইন্টারফেস, প্রতিটার নিজস্ব API ফরম্যাট |
| `ai/enums/AiModel.java` | প্রোভাইডার → আসল model ID স্ট্রিং ম্যাপিং (Claude বাগের উৎস) |
| `auth/role/service/AuthorizationServiceImpl.java` | পুরো Administration-এর permission চেক এর কেন্দ্র (অধ্যায় ২ দেখুন) |

### Frontend

| ফাইল | কাজ |
|---|---|
| `ai-settings.ts` | Provider config CRUD, prompt template এডিটর, usage ড্যাশবোর্ড |
| `ai-assistant.ts` | ফিচার-বাছাই + প্রম্পট + জেনারেট, Recent Conversations প্যানেল |

## ১১.৫ আরও ইন্টারভিউ প্রশ্ন

**প্রশ্ন: `AiUsageLog`-এর মাধ্যমে rate limiting কীভাবে কার্যকর হয়?**
উত্তর: প্রতিটা successful/failed generation `AiUsageLog`-এ একটা রো লেখে (কোম্পানি, ইউজার, তারিখ, ফিচার, টোকেন কাউন্ট সহ)। `enforceRateLimits()` প্রতিটা নতুন রিকোয়েস্টের আগে সেই টেবিলে count query চালায় — আজকের জন্য কোম্পানির মোট রিকোয়েস্ট ২০০ ছাড়িয়েছে কিনা, আর এই ঘণ্টায় ইউজারের রিকোয়েস্ট ২০ ছাড়িয়েছে কিনা। এটা database-backed rate limiting, কোনো in-memory cache (Redis ইত্যাদি) ছাড়াই — সরল কিন্তু প্রতিটা রিকোয়েস্টে একটা এক্সট্রা count query খরচ হয়।

**প্রশ্ন: Prompt Template versioning কীভাবে কাজ করে, পুরনো ভার্সন মুছে ফেলা হয় কেন হয় না?**
উত্তর: একটা কোম্পানি যখন কোনো ফিচারের জন্য নতুন কাস্টম prompt সেভ করে, পুরনো ভার্সনটা deactivate হয় (একটা বুলিয়ান ফ্ল্যাগ) কিন্তু মুছে ফেলা হয় না। এটা গুরুত্বপূর্ণ কারণ পুরনো `AiConversation` রেকর্ডগুলো (যেগুলো পুরনো template দিয়ে জেনারেট হয়েছিল) এখনো ট্রেসেবল থাকে — "এই আউটপুটটা কোন prompt দিয়ে তৈরি হয়েছিল" প্রশ্নের উত্তর সবসময় পাওয়া যায়, একটা অডিট ট্রেইলের মতো।
