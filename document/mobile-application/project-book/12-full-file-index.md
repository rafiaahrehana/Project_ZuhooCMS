# পরিশিষ্ট (Appendix) — সম্পূর্ণ ফাইল তালিকা

এই অধ্যায়ে অ্যাপের **সবগুলো** Java ফাইল (২৪৭টা) এক জায়গায়, এক লাইনে বর্ণনা সহ। কোনো নির্দিষ্ট ফাইল খুঁজতে এই পাতাটা ব্যবহার করো (Ctrl+F দিয়ে ফাইলের নাম খুঁজো), তারপর সংশ্লিষ্ট অধ্যায়ে গিয়ে বিস্তারিত পড়ো।

## Root

| ফাইল | কাজ |
|---|---|
| `ZuhooApplication.java` | `Application` subclass — app শুরু হওয়ার সাথে সাথে `AppGraph` তৈরি করে, edge-to-edge insets padding fix প্রয়োগ করে (`EdgeToEdgeContentPadder`), FCM channel init করে |

## `ui/` — UI Layer (১০৩টা ফাইল, অধ্যায় ৫-৯-এ বিস্তারিত)

### auth, dashboard, account, common (অধ্যায় ৫ থেকে)
| ফাইল | কাজ |
|---|---|
| `LoginActivity.java` | মূল লগইন ফর্ম, session auto-forward, biometric prompt, Google Sign-In শুরু |
| `ClientRegisterActivity.java` | CLIENT self-registration — existing company-তে যোগদান |
| `CompanyOwnerRegisterActivity.java` | COMPANY_OWNER self-registration — নতুন company তৈরি |
| `CompleteGoogleSignupActivity.java` | নতুন Google identity-র জন্য company বাছাই ও signup সম্পূর্ণ করা |
| `GoogleSignInHelper.java` | Credential Manager → Firebase ID token exchange করার হেল্পার |
| `ForgotPasswordActivity.java` | ইমেইল দিয়ে reset link/code চাওয়া |
| `ResetPasswordActivity.java` | Token + নতুন পাসওয়ার্ড দিয়ে reset সম্পূর্ণ করা |
| `VerifyEmailActivity.java` | Verification code দিয়ে ইমেইল যাচাই, resend করার সুবিধাসহ |
| `SessionExpiry.java` | Refresh token invalid হলে session cleanup ও login-এ ফেরত পাঠানোর কেন্দ্রীয় জায়গা |
| `DashboardActivity.java` | Home tab — role অনুযায়ী UI দেখানো, navigation তারযুক্ত করা, push নিবন্ধন |
| `DashboardViewModel.java` | Client-summary বনাম staff-stats fetch করা, role অনুযায়ী কোন call হবে ঠিক করা |
| `AccountActivity.java` | Settings hub — role-conditional মেনু, biometric toggle, ভাষা পরিবর্তন, logout |
| `EditProfileActivity.java` | ব্যক্তিগত প্রোফাইল (নাম/ইমেইল/ফোন) ও পাসওয়ার্ড পরিবর্তন |
| `ClientProfileActivity.java` | Client-এর কোম্পানি প্রোফাইল দেখা/সম্পাদনা (শুধু CLIENT role) |
| `Event.java` | One-shot LiveData wrapper — `consume()` একবারই আসল মান দেয় |
| `CachedListViewModel.java` | সব list-screen ViewModel-এর abstract ভিত্তি — cache-first, network-refresh |
| `BottomNavActivity.java` | `setContentView` override করে shared bottom navigation bar ইনজেক্ট করে |
| `StatusBadgeView.java` | ৮টা per-domain status-badge ক্লাসের আউটপুট রেন্ডার করার একক shared "soft-tint pill" স্টাইল |
| `StateView.java` | Loading/empty/error-এর একক shared custom View |
| `AttachmentPicker.java` | যেকোনো ফাইল বাছাই → `/api/upload` → attachment URL |
| `SelfieCapture.java` | ক্যামেরা-only ছবি তোলা → upload — attendance check-in/out-এ ব্যবহৃত |
| `SecureScreen.java` | `FLAG_SECURE` বসিয়ে screenshot/recents-thumbnail বন্ধ করে |
| `UiErrors.java` | সব ব্যর্থ API response দেখানোর একক সিদ্ধান্ত-কেন্দ্র |
| `CacheStamp.java` | "showing saved data from …" রিলেটিভ-টাইম লাইন রেন্ডার করে |
| `PdfOpener.java` | PDF response ডাউনলোড করে cache-এ লিখে, `FileProvider` দিয়ে external viewer-এ খোলে |

### leave, expense, timesheet, attendance, payroll (অধ্যায় ৬ থেকে)
| ফাইল | কাজ |
|---|---|
| `LeaveRequestStatusBadge.java` | Status → রঙ ও localized লেবেল |
| `LeaveTypeLabels.java` | Leave type constant → localized লেবেল, dropdown-এর জন্য `allLabels()` |
| `LeaveBalanceAdapter.java` | Horizontal RecyclerView adapter — leave balance card strip |
| `LeaveRequestListViewModel.java` | `CachedListViewModel` subclass — নিজের requests + balances |
| `LeaveApprovalListViewModel.java` | `CachedListViewModel` subclass — pending approvals |
| `LeaveRequestListActivity.java` | "My Leave" স্ক্রিন — list + balance strip + cancel |
| `LeaveRequestAdapter.java` | দুই স্ক্রিনে shared adapter (mode flag দিয়ে) |
| `LeaveApprovalListActivity.java` | "Leave Approvals" স্ক্রিন — approve/reject dialog |
| `CreateLeaveRequestActivity.java` | নতুন ছুটির আবেদন ফর্ম — dropdown + দুইটা date picker |
| `ExpenseStatusBadge.java` | Status → রঙ ও লেবেল |
| `ExpenseCategoryLabels.java` | Category constant → localized লেবেল |
| `ExpenseAdapter.java` | দুই স্ক্রিনে shared adapter (`showSubmittedBy` flag) |
| `ExpenseListActivity.java` | "My Expenses" — ViewModel ছাড়া, সরাসরি Repository কল |
| `ExpenseApprovalListActivity.java` | "Expense Approvals" — approve/reject |
| `CreateExpenseActivity.java` | নতুন claim ফর্ম — BigDecimal amount, receipt attachment |
| `TimesheetAdapter.java` | List item bind — status raw টেক্সট হিসেবে |
| `TimesheetListActivity.java` | List + "Log Time" ডায়ালগ |
| `LocationHelper.java` | Runtime permission + একবারের GPS fix |
| `CheckInActivity.java` | Selfie + GPS ready হলেই check-in/out বাটন সক্রিয় |
| `AttendanceLocationSettingsActivity.java` | Owner-only: office lat/lng/radius/enforcement কনফিগার |
| `PayrollStatusBadge.java` | Status → রঙ ও লেবেল |
| `PayslipAdapter.java` | List item bind — pay period, net salary, status, download |
| `PayslipListActivity.java` | দুই-ধাপে fetch, PDF ডাউনলোড ও ওপেন |

### invoice, payment, wallet, catalog (অধ্যায় ৭ থেকে)
| ফাইল | কাজ |
|---|---|
| `InvoiceDetailViewModel.java` | Invoice load, PDF download, payment-confirmation backoff poll |
| `InvoiceListViewModel.java` | `CachedListViewModel<InvoiceSummary>` subclass |
| `InvoiceStatusBadge.java` | `InvoiceStatus` → রং ও লেবেল |
| `InvoiceAdapter.java` | Invoice list-এর RecyclerView adapter |
| `InvoiceListActivity.java` | Invoice list স্ক্রিন — `StateView`, `CacheStamp` |
| `InvoiceDetailActivity.java` | Line items, totals, Pay Now, PDF, payment result |
| `PaymentActivity.java` | SSLCommerz gateway WebView, navigation intercept |
| `PaymentReceiptAdapter.java` | Receipt list-এর RecyclerView adapter |
| `SubscriptionStatusBadge.java` | `SubscriptionStatus` → রং ও লেবেল |
| `PaymentReceiptListActivity.java` | সব payment receipt-এর তালিকা |
| `SubscriptionAdapter.java` | Subscription list-এর adapter, Pay Now/Cancel |
| `SubscriptionListActivity.java` | Client-এর সব package subscription |
| `WalletTransactionAdapter.java` | Transaction history-এর adapter |
| `WalletActivity.java` | Wallet balance + transaction history |
| `CatalogRow.java` | header/service union-স্টাইল model |
| `CatalogAdapter.java` | Category-wise grouped catalog adapter |
| `PackageAdapter.java` | Subscription package তালিকার adapter |
| `CatalogActivity.java` | Categories + active services merge করে দেখায় |
| `PackageListActivity.java` | সব সক্রিয় subscription package, subscribe confirmation |

### servicerequest, support, kb, noticeboard, notification (অধ্যায় ৮ থেকে)
| ফাইল | কাজ |
|---|---|
| `DynamicFormRenderer.java` | Custom form field runtime-এ কোড দিয়ে বানায় |
| `CreateServiceRequestActivity.java` | নতুন request তৈরির ফর্ম, dynamic field সহ |
| `ServiceRequestListActivity.java` | Client-এর নিজের request list |
| `ServiceRequestListViewModel.java` | `CachedListViewModel<ServiceRequestSummary>` subclass |
| `StaffServiceRequestListActivity.java` | Staff-এর request list, দুই mode |
| `ServiceRequestAdapter.java` | List item bind, `StatusBadgeView` |
| `ServiceRequestDetailActivity.java` | মূল detail screen — quotation, timeline, comment, ৬টা dialog |
| `ServiceRequestDetailViewModel.java` | Detail screen state, chat subscription owner |
| `CommentAdapter.java` | Comment thread adapter, id দিয়ে dedupe |
| `StatusBadge.java` | `ServiceRequestStatus` → রং ও লেবেল |
| `CreateSupportTicketActivity.java` | নতুন ticket তৈরি |
| `SupportTicketListActivity.java` | Company/staff-এর নিজের ticket list |
| `SupportTicketAdapter.java` | Ticket list item adapter |
| `SupportTicketDetailActivity.java` | Ticket detail + message thread + satisfaction rating |
| `SupportTicketDetailViewModel.java` | Detail screen state, chat subscription |
| `SupportMessageAdapter.java` | Message thread adapter, dedupe |
| `TicketStatusBadge.java` | `TicketStatus` → রং ও লেবেল |
| `KbArticleListActivity.java` | Article list + keyword search |
| `KbArticleDetailActivity.java` | Article content, "helpful" মার্ক |
| `KbArticleAdapter.java` | Article list item |
| `NoticeBoardActivity.java` | Announcement feed + holiday strip |
| `AnnouncementAdapter.java` | Announcement list item |
| `HolidayAdapter.java` | Holiday strip item |
| `NotificationListActivity.java` | Notification centre |
| `NotificationListViewModel.java` | `CachedListViewModel<NotificationResponse>` subclass |
| `NotificationAdapter.java` | Notification list item, unread dot |
| `NotificationPreferencesActivity.java` | ৯টা boolean toggle |

### crm, directory, search, overview (অধ্যায় ৯ থেকে)
| ফাইল | কাজ |
|---|---|
| `LeadStatusBadge.java` | `LeadStatus` → রং ও লেবেল |
| `LeadAdapter.java` | Lead list adapter |
| `LeadListActivity.java` | "My Leads" স্ক্রিন, staff-only |
| `DirectoryAdapter.java` | Employee list adapter |
| `DirectoryActivity.java` | Company-র employee directory |
| `SearchResultTypeLabels.java` | Result type → localized লেবেল |
| `SearchResultAdapter.java` | Search result list adapter |
| `SearchActivity.java` | Cross-entity global search, দুই empty state |
| `CompanyOverviewActivity.java` | Finance+HR stat dashboard |

## `data/` + `di/` + `push/` — Data Layer (১৪৩টা ফাইল, অধ্যায় ৪-এ বিস্তারিত)

### `data/api/` — Networking core
| ফাইল | কাজ |
|---|---|
| `ApiClient.java` | Retrofit singleton (double-checked locking) |
| `ApiConfig.java` | Base URL ও WebSocket URL derive করে (build flavor থেকে) |
| `ApiErrors.java` | Error response body parse করার হেল্পার |
| `ApiService.java` | সব API endpoint-এর Retrofit interface (৬৩+ মেথড) |
| `AuthInterceptor.java` | প্রতিটা request-এ `Authorization` header যোগ করে |
| `TokenAuthenticator.java` | 401-এ silent token refresh, race-condition safe |
| `TokenRefreshClient.java` | শুধু refresh endpoint-এর জন্য আলাদা, সরল Retrofit instance (infinite loop এড়াতে) |

### `data/chat/` — Real-time messaging
| ফাইল | কাজ |
|---|---|
| `ChatSocket.java` | STOMP subscribe/reconnect/backoff লজিক, reference-counted subscription |
| `StompFrame.java` | STOMP protocol frame parse/build (~১২০ লাইন, হাতে লেখা) |

### `data/local/` — Token ও local storage
| ফাইল | কাজ |
|---|---|
| `TokenManager.java` | Access/refresh token, role, companyId — `EncryptedSharedPreferences`-এ |
| `PushTokenStore.java` | FCM push token, logout-এর পরেও বেঁচে থাকে |
| `CacheDao.java` | Room DAO — insert/query annotation |
| `CacheMeta.java` | Cache metadata entity |
| `CachedItem.java` | Room `@Entity` — collection/itemId/json/updatedAt |
| `ListCache.java` | Room-এর উপর সহজ read/write API |
| `ZuhooDatabase.java` | Room `@Database` ক্লাস |

### `data/notification/`
| ফাইল | কাজ |
|---|---|
| `NotificationCenter.java` | Notification badge count, live WebSocket push |

### `data/model/` — DTO Layer (৭৫+ ফাইল)
> এগুলো সবই plain data-holder ক্লাস (POJO) — backend API-র JSON structure-এর সাথে field-ভিত্তিক মিল রেখে বানানো, কোনো business logic নেই। নামকরণ স্ব-ব্যাখ্যামূলক (self-descriptive) — `CreateLeaveRequestRequest` মানে যা বোঝাচ্ছে ঠিক তাই। এনাম-সদৃশ status ক্লাস (`LeaveRequestStatus`, `InvoiceStatus`, ইত্যাদি) সম্পর্কে বিস্তারিত অধ্যায় ৪.১২-এ।

| উপ-প্যাকেজ | ফাইল সংখ্যা | বিষয়বস্তু |
|---|---|---|
| `data/model/` (root) | ১২ | Status/enum-সদৃশ ক্লাস: `ExpenseCategory`, `ExpenseStatus`, `InvoiceStatus`, `LeadStatus`, `LeaveRequestStatus`, `LeaveType`, `PasswordPolicy`, `Role`, `ServiceRequestPriority`, `ServiceRequestStatus`, `SubscriptionStatus`, `TicketStatus` |
| `data/model/request/` | ৩২ | প্রতিটা POST/PUT/PATCH call-এর body — যেমন `CreateLeaveRequestRequest`, `LoginRequest`, `SubmitQuotationRequest`, `UpdateNotificationPreferenceRequest` |
| `data/model/response/` | ৪৫ | প্রতিটা API response — যেমন `LeaveRequestResponse`, `LoginResponse`, `ServiceRequestDetail`, `PageResponse<T>` (generic pagination wrapper) |

### `di/`
| ফাইল | কাজ |
|---|---|
| `AppGraph.java` | হাতে-লেখা DI container — token store, chat socket, cache, notification centre |

### `push/`
| ফাইল | কাজ |
|---|---|
| `ZuhooMessagingService.java` | FCM message receive করার service |
| `PushRouter.java` | Notification data payload → কোন Activity খুলবে |
| `PushChannels.java` | ৩টা Notification Channel (requests/billing/general) |

### `data/repository/` (২৭টা)
| ফাইল | কাজ |
|---|---|
| `AuthRepository.java` | Login, register, password reset |
| `AttendanceRepository.java` | Check-in/out, location settings |
| `CatalogRepository.java` | Service catalog, categories |
| `ClientProfileRepository.java` | Client profile fetch/update |
| `ClientRepository.java` | Client-related মিসলেনিয়াস কল |
| `CompanyOverviewRepository.java` | Overview stat fetch |
| `CompanyRepository.java` | Public company list (registration picker) |
| `DashboardRepository.java` | Role-based dashboard summary |
| `DeviceTokenRepository.java` | FCM token register/unregister |
| `EmployeeRepository.java` | Directory-তে employee তালিকা |
| `ExpenseRepository.java` | Expense claims ও approvals |
| `InvoiceRepository.java` | Invoice list/detail/PDF |
| `KbArticleRepository.java` | Knowledge Base article |
| `LeadRepository.java` | CRM lead তালিকা |
| `LeaveRequestRepository.java` | Leave request submit/approve |
| `NoticeBoardRepository.java` | Company announcement, holiday |
| `NotificationRepository.java` | Notification centre, preferences |
| `PackageRepository.java` | Service package তালিকা |
| `PaymentReceiptRepository.java` | Payment receipt তালিকা |
| `PaymentRepository.java` | SSLCommerz initiate |
| `PayrollRepository.java` | Payslip তালিকা |
| `ReviewRepository.java` | Service review submit |
| `SearchRepository.java` | Global search |
| `ServiceRequestRepository.java` | মূল Service Request CRUD |
| `SubscriptionRepository.java` | Package subscription |
| `SupportTicketRepository.java` | Support ticket ও chat |
| `TimesheetRepository.java` | Timesheet log |
| `UploadRepository.java` | File/image upload |
| `UserProfileRepository.java` | User account profile |
| `WalletRepository.java` | Wallet balance, transaction |

---

## সারসংক্ষেপ — মোট ফাইল সংখ্যা

| স্তর | ফাইল সংখ্যা |
|---|---|
| `ui/` (Activity, ViewModel, Adapter, Badge-helper) | ১০৩ |
| `data/` + `di/` + `push/` (networking, cache, DTO, DI) | ১৪৩ |
| Root (`ZuhooApplication`) | ১ |
| **মোট** | **২৪৭** |

এছাড়াও এই সেশনে UI/UX redesign-এর সময় নতুন যোগ হয়েছে: `StatusBadgeView.java`, `StateView.java` (উপরে তালিকাভুক্ত, `ui/common/`-এ), আর ১৫টা নতুন vector icon, নতুন `dimens.xml`/`type.xml`/`styles.xml` resource ফাইল (অধ্যায় ১০ দ্রষ্টব্য)।
