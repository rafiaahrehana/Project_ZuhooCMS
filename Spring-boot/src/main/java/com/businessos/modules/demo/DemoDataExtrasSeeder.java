package com.businessos.modules.demo;

import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.enums.*;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.contact.ClientContact;
import com.businessos.modules.crm.contact.ClientContactRepository;
import com.businessos.modules.crm.tag.Tag;
import com.businessos.modules.crm.tag.TagRepository;
import com.businessos.modules.finance.chartofaccounts.AccountType;
import com.businessos.modules.finance.fixedasset.FixedAsset;
import com.businessos.modules.finance.fixedasset.FixedAssetRepository;
import com.businessos.modules.finance.vendor.Vendor;
import com.businessos.modules.finance.vendor.VendorRepository;
import com.businessos.modules.hrm.announcement.Announcement;
import com.businessos.modules.hrm.announcement.AnnouncementRepository;
import com.businessos.modules.hrm.attendance.timesheet.Timesheet;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetRepository;
import com.businessos.modules.hrm.designation.Designation;
import com.businessos.modules.hrm.designation.DesignationRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.holiday.Holiday;
import com.businessos.modules.hrm.leave.holiday.HolidayRepository;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalance;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;
import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.modules.itam.software.SoftwareLicense;
import com.businessos.modules.itam.software.SoftwareLicenseRepository;
import com.businessos.modules.servicedesk.companyservice.CompanyService;
import com.businessos.modules.servicedesk.companyservice.CompanyServiceRepository;
import com.businessos.modules.servicedesk.kb.KbArticle;
import com.businessos.modules.servicedesk.kb.KbArticleRepository;
import com.businessos.modules.servicedesk.kb.KbArticleStatus;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategoryRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.List;

/**
 * Second seeding pass for the demo tenant: everything beyond the core
 * HR/CRM/Finance data, so a demo visitor finds no dead pages.
 *
 * Separate from DemoDataSeeder (and @Order-ed after it) for one reason: the
 * core seeder skips entirely when the demo company already exists, and this
 * class must still be able to upgrade an existing demo tenant that was seeded
 * before these sections were written. The guard is a single marker - service
 * categories - because it is the first thing seeded here and cannot exist on
 * a tenant this pass has not visited.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataExtrasSeeder implements ApplicationRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final DesignationRepository designationRepository;
    private final AnnouncementRepository announcementRepository;
    private final HolidayRepository holidayRepository;
    private final TagRepository tagRepository;
    private final ClientContactRepository clientContactRepository;
    private final TimesheetRepository timesheetRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final VendorRepository vendorRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final JobPostingRepository jobPostingRepository;
    private final SoftwareLicenseRepository softwareLicenseRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final KbArticleRepository kbArticleRepository;
    private final com.businessos.modules.hrm.leave.companyleavePolicy.CompanyLeavePolicyRepository companyLeavePolicyRepository;
    private final com.businessos.modules.hrm.performance.PerformanceReviewRepository performanceReviewRepository;
    private final com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository jobApplicationRepository;
    private final com.businessos.modules.hrm.recruitment.candidate.CandidateRepository candidateRepository;
    private final com.businessos.modules.hrm.recruitment.offerletter.OfferLetterRepository offerLetterRepository;
    private final com.businessos.modules.hrm.asset.AssetRepository hrAssetRepository;
    private final com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository chartOfAccountRepository;
    private final com.businessos.modules.finance.journalentry.JournalEntryRepository journalEntryRepository;
    private final com.businessos.modules.finance.generalledger.GeneralLedgerRepository generalLedgerRepository;
    private final com.businessos.modules.finance.payment.PaymentReceiptRepository paymentReceiptRepository;
    private final com.businessos.modules.finance.vendor.VendorBillRepository vendorBillRepository;
    private final com.businessos.modules.servicedesk.companyservice.ServicePackageRepository servicePackageRepository;
    private final com.businessos.modules.servicedesk.servicereview.ServiceReviewRepository serviceReviewRepository;
    private final com.businessos.modules.finance.invoice.ClientInvoiceRepository clientInvoiceRepository;

    private Company company;
    private User owner;
    private List<Employee> staff;
    private List<Client> clients;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        company = companyRepository.findBySubdomain(DemoDataSeeder.DEMO_SUBDOMAIN).orElse(null);
        if (company == null) return; // core seeder disabled or failed - nothing to extend

        owner = userRepository.findByEmail(DemoDataSeeder.DEMO_OWNER_EMAIL).orElse(null);
        staff = employeeRepository.findByCompanyIdAndActiveTrue(company.getId());
        staff.sort((a, b) -> a.getEmployeeNumber().compareTo(b.getEmployeeNumber()));
        clients = clientRepository.findAll().stream()
                .filter(c -> c.getCompany() != null && c.getCompany().getId().equals(company.getId()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
        if (owner == null || staff.isEmpty() || clients.isEmpty()) {
            log.warn("Demo extras skipped - core demo data incomplete");
            return;
        }

        // Each wave carries its own emptiness guard rather than one shared
        // check, because a tenant may have been seeded by an earlier version of
        // this class that only knew about earlier waves - the newer waves must
        // still be able to upgrade it.
        if (serviceCategoryRepository.findByCompanyIdOrderBySortOrderAsc(company.getId()).isEmpty()) {
            log.info("Seeding demo extras...");
            seedDesignations();
            seedAnnouncements();
            seedHolidays();
            seedTagsAndContacts();
            seedTimesheets();
            seedLeaveBalances();
            seedVendorsAndAssets();
            seedRecruitment();
            seedSoftwareLicences();
            seedServiceDesk();
            log.info("Demo extras seeded.");
        }
        seedWaveThree();
    }

    /** Third pass: accounting, receipts, recruitment paperwork and the rest. */
    private void seedWaveThree() {
        if (!companyLeavePolicyRepository.findAll().stream()
                .filter(p -> p.getCompany() != null && p.getCompany().getId().equals(company.getId()))
                .toList().isEmpty()) {
            return;
        }
        log.info("Seeding demo wave three...");
        seedLeavePolicies();
        seedPerformanceReviews();
        seedApplicationsAndLetters();
        seedHrAssets();
        seedAccounting();
        seedReceiptsAndBills();
        seedPackagesAndReview();
    }

    private void seedLeavePolicies() {
        Object[][] rows = {
                {LeaveType.ANNUAL, 20, true, "Earned leave per the Labour Act; plan ahead with your lead"},
                {LeaveType.SICK, 14, false, "Doctor's certificate needed beyond two consecutive days"},
                {LeaveType.CASUAL, 10, false, "Short-notice personal matters"},
        };
        for (Object[] r : rows) {
            com.businessos.modules.company.CompanyLeavePolicy p = new com.businessos.modules.company.CompanyLeavePolicy();
            p.setLeaveType((LeaveType) r[0]);
            p.setEmploymentType(EmploymentType.FULL_TIME);
            p.setAnnualEntitlement((int) r[1]);
            p.setCanCarryForward((boolean) r[2]);
            p.setMaxCarryForward((boolean) r[2] ? 10 : 0);
            p.setPaid(true);
            p.setRequiresApproval(true);
            p.setActive(true);
            p.setDescription((String) r[3]);
            p.setCompany(company);
            companyLeavePolicyRepository.save(p);
        }
    }

    private void seedPerformanceReviews() {
        // Two finished reviews from the last cycle - one strong, one average -
        // so the list, the score breakdown and the level label all read.
        int[][] scores = {{9, 9, 8, 9, 8, 9, 7, 9, 8}, {7, 6, 7, 7, 6, 8, 5, 6, 6}};
        Employee[] who = {staff.get(2), staff.get(5)}; // Rakibul, Tasnim
        for (int i = 0; i < 2; i++) {
            com.businessos.modules.hrm.performance.PerformanceReview r =
                    new com.businessos.modules.hrm.performance.PerformanceReview();
            r.setEmployee(who[i]);
            r.setCompany(company);
            r.setReviewedBy(staff.get(0));
            r.setReviewPeriodStart(LocalDate.now().minusMonths(7).withDayOfMonth(1));
            r.setReviewPeriodEnd(LocalDate.now().minusMonths(1).withDayOfMonth(1).minusDays(1));
            int[] s = scores[i];
            r.setScoreWorkQuality(s[0]);
            r.setScoreProductivity(s[1]);
            r.setScoreCommunication(s[2]);
            r.setScoreTeamwork(s[3]);
            r.setScoreInitiative(s[4]);
            r.setScorePunctuality(s[5]);
            r.setScoreLeadership(s[6]);
            r.setScoreProblemSolving(s[7]);
            r.setScoreInnovation(s[8]);
            double avg = java.util.Arrays.stream(s).average().orElse(0);
            r.setOverallScore(Math.round(avg * 10) / 10.0);
            r.setPerformanceLevel(avg >= 8 ? "Excellent" : "Meets expectations");
            performanceReviewRepository.save(r);
        }
    }

    private void seedApplicationsAndLetters() {
        List<com.businessos.modules.hrm.recruitment.jobpost.JobPosting> posts =
                jobPostingRepository.findByCompanyIdAndStatus(company.getId(), JobPostingStatus.OPEN);
        if (!posts.isEmpty()) {
            String[][] applicants = {
                    {"Md. Arif Hossain", "arif.hossain", "SHORTLISTED"},
                    {"Sadia Afrin", "sadia.afrin", "INTERVIEW_SCHEDULED"},
                    {"Tanjil Ahmed", "tanjil.ahmed", "APPLIED"},
            };
            int i = 0;
            for (String[] a : applicants) {
                com.businessos.modules.hrm.recruitment.candidate.Candidate candidate =
                        new com.businessos.modules.hrm.recruitment.candidate.Candidate();
                candidate.setCompany(company);
                candidate.setName(a[0]);
                candidate.setEmail(a[1] + "@example.com");
                candidate.setPhone("+880 16" + (20 + i) + "-889900");
                candidate.setSource(ApplicationSource.CAREER_PAGE);
                candidateRepository.save(candidate);

                com.businessos.modules.hrm.recruitment.jobapplication.JobApplication app =
                        new com.businessos.modules.hrm.recruitment.jobapplication.JobApplication();
                app.setJobPosting(posts.get(i % posts.size()));
                app.setCompany(company);
                app.setCandidate(candidate);
                app.setSource(ApplicationSource.CAREER_PAGE);
                app.setStatus(ApplicationStatus.valueOf(a[2]));
                if ("INTERVIEW_SCHEDULED".equals(a[2])) app.setInterviewAt(LocalDateTime.now().plusDays(3));
                jobApplicationRepository.save(app);
                i++;
            }
        }

        com.businessos.modules.hrm.recruitment.offerletter.OfferLetter letter =
                new com.businessos.modules.hrm.recruitment.offerletter.OfferLetter();
        letter.setLetterType(LetterType.APPOINTMENT);
        letter.setReferenceNumber("DHR/HR/2026-014");
        letter.setContent("Appointment letter for the post of Software Engineer, as per the offer "
                + "accepted on the joining date. Probation six months per company policy.");
        letter.setSignedBy("Tanvir Ahmed Chowdhury, Managing Director");
        letter.setIssueDate(LocalDate.now().minusMonths(2));
        letter.setIssued(true);
        letter.setAcknowledged(true);
        letter.setEmployee(staff.get(4)); // Sabbir, the probationer
        letter.setRecipientName("Sabbir Rahman Khan");
        letter.setCompany(company);
        offerLetterRepository.save(letter);
    }

    private void seedHrAssets() {
        Object[][] rows = {
                {"MacBook Pro 14\"", "LPT-001", "Apple", "Laptop", 280_000, AssetStatus.ASSIGNED},
                {"ThinkPad T14", "LPT-002", "Lenovo", "Laptop", 145_000, AssetStatus.ASSIGNED},
                {"Dell U2723QE Monitor", "MON-001", "Dell", "Monitor", 62_000, AssetStatus.AVAILABLE},
                {"iPhone 15 (support line)", "PHN-001", "Apple", "Phone", 130_000, AssetStatus.UNDER_MAINTENANCE},
        };
        for (Object[] r : rows) {
            com.businessos.modules.hrm.asset.Asset a = new com.businessos.modules.hrm.asset.Asset();
            a.setName((String) r[0]);
            a.setAssetTag((String) r[1]);
            a.setBrand((String) r[2]);
            a.setCategory((String) r[3]);
            a.setPurchasePrice(BigDecimal.valueOf((int) r[4]));
            a.setPurchaseDate(LocalDate.now().minusMonths(8));
            a.setStatus((AssetStatus) r[5]);
            a.setCompany(company);
            hrAssetRepository.save(a);
        }
    }

    /** A small but double-entry-honest ledger: every GL row pairs off, so trial balance balances. */
    private void seedAccounting() {
        Object[][] accounts = {
                {"1000", "Cash at Bank - DBBL", AccountType.ASSET},
                {"1100", "Accounts Receivable", AccountType.ASSET},
                {"2000", "Accounts Payable", AccountType.LIABILITY},
                {"3000", "Owner's Equity", AccountType.EQUITY},
                {"4000", "Service Revenue", AccountType.REVENUE},
                {"5000", "Salary Expense", AccountType.EXPENSE},
                {"5100", "Rent Expense", AccountType.EXPENSE},
                {"5200", "Utilities & Internet", AccountType.EXPENSE},
        };
        java.util.Map<String, com.businessos.modules.finance.chartofaccounts.ChartOfAccount> coa = new java.util.HashMap<>();
        for (Object[] a : accounts) {
            com.businessos.modules.finance.chartofaccounts.ChartOfAccount acc =
                    new com.businessos.modules.finance.chartofaccounts.ChartOfAccount();
            acc.setCompanyId(company.getId());
            acc.setAccountCode((String) a[0]);
            acc.setAccountName((String) a[1]);
            acc.setType((AccountType) a[2]);
            acc.setActive(true);
            coa.put((String) a[0], chartOfAccountRepository.save(acc));
        }

        // One revenue recognition, one rent payment - as journal entries and as
        // their mirrored GL rows.
        journalEntry("DEMO-JE-0001", coa.get("1100"), coa.get("4000"), 450_000,
                "Annual maintenance contract invoiced - Meghna Agro", 40);
        journalEntry("DEMO-JE-0002", coa.get("5100"), coa.get("1000"), 85_000,
                "Office rent - Banani, current month", 6);

        glPair(coa.get("1100"), coa.get("4000"), 450_000, "INVOICE", "DEMO-INV-0001",
                "Annual maintenance contract - year 1", 40);
        glPair(coa.get("1000"), coa.get("1100"), 450_000, "PAYMENT", "DEMO-RCPT-0001",
                "Receipt against DEMO-INV-0001", 10);
        glPair(coa.get("5100"), coa.get("1000"), 85_000, "EXPENSE", "DEMO-EXP-0001",
                "Office rent - Banani", 6);
    }

    private void journalEntry(String number,
                              com.businessos.modules.finance.chartofaccounts.ChartOfAccount debit,
                              com.businessos.modules.finance.chartofaccounts.ChartOfAccount credit,
                              int amount, String description, int daysAgo) {
        com.businessos.modules.finance.journalentry.JournalEntry je =
                new com.businessos.modules.finance.journalentry.JournalEntry();
        je.setCompanyId(company.getId());
        je.setJournalEntryNumber(number);
        je.setEntryDate(LocalDate.now().minusDays(daysAgo));
        je.setDebitAccount(debit);
        je.setCreditAccount(credit);
        je.setAmount(BigDecimal.valueOf(amount));
        je.setDescription(description);
        je.setCreatedBy("Ishrat Jahan Priya");
        je.setCreatedDate(LocalDate.now().minusDays(daysAgo));
        journalEntryRepository.save(je);
    }

    private void glPair(com.businessos.modules.finance.chartofaccounts.ChartOfAccount debit,
                        com.businessos.modules.finance.chartofaccounts.ChartOfAccount credit,
                        int amount, String refType, String refNumber, String description, int daysAgo) {
        com.businessos.modules.finance.generalledger.GeneralLedger d =
                new com.businessos.modules.finance.generalledger.GeneralLedger();
        d.setCompanyId(company.getId());
        d.setTransactionDate(LocalDate.now().minusDays(daysAgo));
        d.setAccount(debit);
        d.setDebitAmount(BigDecimal.valueOf(amount));
        d.setDescription(description);
        d.setReferenceType(refType);
        d.setReferenceNumber(refNumber);
        generalLedgerRepository.save(d);

        com.businessos.modules.finance.generalledger.GeneralLedger c =
                new com.businessos.modules.finance.generalledger.GeneralLedger();
        c.setCompanyId(company.getId());
        c.setTransactionDate(LocalDate.now().minusDays(daysAgo));
        c.setAccount(credit);
        c.setCreditAmount(BigDecimal.valueOf(amount));
        c.setDescription(description);
        c.setReferenceType(refType);
        c.setReferenceNumber(refNumber);
        generalLedgerRepository.save(c);
    }

    private void seedReceiptsAndBills() {
        // Receipts against the two PAID demo invoices.
        List<com.businessos.modules.finance.invoice.ClientInvoice> paid = invoiceRepositoryAll().stream()
                .filter(i -> i.getCompanyId().equals(company.getId())
                        && i.getStatus() == InvoiceStatus.PAID)
                .toList();
        int n = 1;
        for (com.businessos.modules.finance.invoice.ClientInvoice inv : paid) {
            com.businessos.modules.finance.payment.PaymentReceipt r =
                    new com.businessos.modules.finance.payment.PaymentReceipt();
            r.setCompanyId(company.getId());
            r.setReceiptNumber(String.format("DEMO-RCPT-%04d", n));
            r.setInvoice(inv);
            r.setClient(inv.getClient());
            r.setAmount(inv.getTotalAmount());
            r.setPaymentDate(inv.getPaidDate() != null ? inv.getPaidDate() : LocalDate.now().minusDays(10));
            r.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            r.setTransactionReference("DBBL-TRX-77" + (100 + n));
            r.setStatus(com.businessos.modules.finance.payment.PaymentStatus.DEPOSITED);
            r.setDepositDate(r.getPaymentDate().plusDays(1));
            r.setDepositedToBank("Dutch-Bangla Bank, Banani");
            paymentReceiptRepository.save(r);
            n++;
        }

        // Bills from the seeded vendors.
        List<com.businessos.modules.finance.vendor.Vendor> vendors = vendorRepository.findAll().stream()
                .filter(v -> v.getCompanyId().equals(company.getId()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
        Object[][] bills = {
                {"Dedicated internet - quarterly", 36_000, com.businessos.modules.finance.vendor.VendorBillStatus.PAID, 0},
                {"Workstation batch (4x Dell OptiPlex)", 340_000, com.businessos.modules.finance.vendor.VendorBillStatus.APPROVED, 1},
        };
        int b = 1;
        for (Object[] row : bills) {
            if (vendors.size() <= (int) row[3]) continue;
            com.businessos.modules.finance.vendor.VendorBill bill =
                    new com.businessos.modules.finance.vendor.VendorBill();
            bill.setCompanyId(company.getId());
            bill.setBillNumber(String.format("DEMO-BILL-%04d", b));
            bill.setVendor(vendors.get((int) row[3]));
            bill.setBillDate(LocalDate.now().minusDays(20L * b));
            bill.setDueDate(LocalDate.now().plusDays(10));
            BigDecimal total = BigDecimal.valueOf((int) row[1]);
            bill.setSubtotal(total);
            bill.setTotalAmount(total);
            boolean isPaid = row[2] == com.businessos.modules.finance.vendor.VendorBillStatus.PAID;
            bill.setPaidAmount(isPaid ? total : BigDecimal.ZERO);
            bill.setBalanceAmount(isPaid ? BigDecimal.ZERO : total);
            bill.setStatus((com.businessos.modules.finance.vendor.VendorBillStatus) row[2]);
            bill.setDescription((String) row[0]);
            vendorBillRepository.save(bill);
            b++;
        }
    }

    private void seedPackagesAndReview() {
        // Two service packages for the billing side of the service desk.
        com.businessos.modules.servicedesk.companyservice.ServicePackage starter =
                new com.businessos.modules.servicedesk.companyservice.ServicePackage();
        starter.setName("Care Starter");
        starter.setDescription("Monthly retainer: 5 requests, business-hours support");
        starter.setPackagePrice(BigDecimal.valueOf(25_000));
        starter.setRequestQuota(5);
        starter.setCompany(company);
        servicePackageRepository.save(starter);

        com.businessos.modules.servicedesk.companyservice.ServicePackage growth =
                new com.businessos.modules.servicedesk.companyservice.ServicePackage();
        growth.setName("Care Growth");
        growth.setDescription("Monthly retainer: 15 requests, priority queue, monthly report");
        growth.setPackagePrice(BigDecimal.valueOf(60_000));
        growth.setRequestQuota(15);
        growth.setDiscountPercent(BigDecimal.TEN);
        growth.setCompany(company);
        servicePackageRepository.save(growth);

        // A finished request with a published five-star review, so Reviews and
        // the request lifecycle's end state are both visible.
        List<com.businessos.modules.servicedesk.companyservice.CompanyService> services =
                companyServiceRepository.findAll().stream()
                        .filter(s -> s.getCompany() != null && s.getCompany().getId().equals(company.getId()))
                        .sorted((a, b) -> a.getId().compareTo(b.getId()))
                        .toList();
        if (services.isEmpty()) return;

        ServiceRequest done = new ServiceRequest();
        done.setTitle("Corporate website relaunch");
        done.setDescription("Delivered and handed over last month.");
        done.setStatus(ServiceRequestStatus.COMPLETED);
        done.setPriority(ServiceRequestPriority.NORMAL);
        done.setCurrentStage(3);
        done.setAgreedPrice(services.get(0).getPrice());
        done.setCompanyService(services.get(0));
        done.setClient(clients.get(3));
        done.setCompany(company);
        done.setAssignedEmployee(staff.get(2));
        done.setCompletedAt(LocalDateTime.now().minusDays(25));
        done.setClientRating(5);
        done.setClientFeedback("Delivered ahead of schedule, great communication throughout.");
        serviceRequestRepository.save(done);

        com.businessos.modules.servicedesk.servicereview.ServiceReview review =
                new com.businessos.modules.servicedesk.servicereview.ServiceReview();
        review.setServiceRequest(done);
        review.setCompany(company);
        review.setHubService(services.get(0));
        review.setClient(clients.get(3));
        review.setStaff(staff.get(2));
        review.setRating(5);
        review.setComment("Delivered ahead of schedule, great communication throughout.");
        review.setPublished(true);
        serviceReviewRepository.save(review);
    }

    private List<com.businessos.modules.finance.invoice.ClientInvoice> invoiceRepositoryAll() {
        return clientInvoiceRepository.findAll();
    }

    private void seedDesignations() {
        String[][] rows = {
                {"Managing Director", "MD", "1"}, {"HR Manager", "HRM", "3"},
                {"Senior Software Engineer", "SSE", "4"}, {"Software Engineer", "SE", "5"},
                {"Sales Executive", "SX", "5"},
        };
        for (String[] r : rows) {
            designationRepository.save(Designation.builder()
                    .name(r[0]).code(r[1]).level(Integer.parseInt(r[2]))
                    .active(true).company(company).build());
        }
    }

    private void seedAnnouncements() {
        announcementRepository.save(Announcement.builder()
                .title("Eid holidays announced")
                .body("The office will remain closed for five days around Eid. The support rota "
                        + "for on-call coverage is posted on the notice board - please check your slot.")
                .audience(AnnouncementAudience.ALL)
                .published(true).publishedAt(LocalDateTime.now().minusDays(2))
                .priority(1).company(company).createdBy(owner).build());
        announcementRepository.save(Announcement.builder()
                .title("Health insurance enrolment open")
                .body("Group health insurance enrolment for all permanent employees is open until the "
                        + "end of the month. Contact HR with your dependants' details.")
                .audience(AnnouncementAudience.ALL)
                .published(true).publishedAt(LocalDateTime.now().minusDays(6))
                .company(company).createdBy(owner).build());
    }

    /** National holidays at their next occurrence, so the list is always ahead of today. */
    private void seedHolidays() {
        Object[][] rows = {
                {"International Mother Language Day", MonthDay.of(2, 21)},
                {"Independence Day", MonthDay.of(3, 26)},
                {"Pohela Boishakh", MonthDay.of(4, 14)},
                {"Victory Day", MonthDay.of(12, 16)},
        };
        for (Object[] r : rows) {
            MonthDay md = (MonthDay) r[1];
            LocalDate next = md.atYear(LocalDate.now().getYear());
            if (next.isBefore(LocalDate.now())) next = next.plusYears(1);
            Holiday h = new Holiday();
            h.setName((String) r[0]);
            h.setDate(next);
            h.setType(HolidayType.NATIONAL);
            h.setCompany(company);
            holidayRepository.save(h);
        }
    }

    private void seedTagsAndContacts() {
        for (String[] t : new String[][]{{"VIP", "#E11D48"}, {"Government", "#0D9488"}, {"Referral", "#6366F1"}}) {
            Tag tag = new Tag();
            tag.setName(t[0]);
            tag.setColor(t[1]);
            tag.setCompany(company);
            tagRepository.save(tag);
        }

        String[][] contacts = {
                {"Anisur Rahman", "Head of Procurement", "anis"},
                {"Rehana Parvin", "GM, Operations", "rehana"},
                {"Dr. Kamal Hossain", "Director", "kamal"},
                {"Selim Reza", "Owner", "selim"},
        };
        for (int i = 0; i < clients.size() && i < contacts.length; i++) {
            ClientContact c = new ClientContact();
            c.setFullName(contacts[i][0]);
            c.setJobTitle(contacts[i][1]);
            c.setEmail(contacts[i][2] + "@" + clients.get(i).getClientCompanyName()
                    .toLowerCase().replaceAll("[^a-z]", "").substring(0, 6) + ".example.com");
            c.setPhone("+880 19" + (10 + i) + "-334455");
            c.setPrimaryContact(true);
            c.setClient(clients.get(i));
            c.setCompany(company);
            clientContactRepository.save(c);
        }
    }

    /** A week of logged hours for the two engineers, against the open deals' project names. */
    private void seedTimesheets() {
        Employee rakibul = staff.get(2), nusrat = staff.get(3);
        String[] projects = {"Padma Textiles ERP", "City General HMS"};
        LocalDate day = LocalDate.now().minusDays(1);
        int added = 0;
        while (added < 4) {
            if (day.getDayOfWeek().getValue() < 5 || day.getDayOfWeek().getValue() == 7) { // skip Fri(5)/Sat(6)
                for (int e = 0; e < 2; e++) {
                    Employee emp = e == 0 ? rakibul : nusrat;
                    Timesheet ts = new Timesheet();
                    ts.setEmployee(emp);
                    ts.setCompany(company);
                    ts.setWorkDate(day);
                    ts.setStartTime(LocalTime.of(9, 30));
                    ts.setEndTime(LocalTime.of(17, 30));
                    ts.setHoursWorked(8.0);
                    ts.setBillableHours(6.5);
                    ts.setProjectName(projects[e]);
                    ts.setTaskDescription(e == 0 ? "Inventory module APIs" : "Patient records UI");
                    ts.setSubmitted(true);
                    ts.setSubmittedAt(day.plusDays(1).atTime(10, 0));
                    ts.setApproved(added % 2 == 0);
                    ts.setApprovedBy(added % 2 == 0 ? owner : null);
                    timesheetRepository.save(ts);
                }
                added++;
            }
            day = day.minusDays(1);
        }
    }

    private void seedLeaveBalances() {
        int year = LocalDate.now().getYear();
        int i = 0;
        for (Employee emp : staff) {
            leaveBalanceRepository.save(LeaveBalance.builder()
                    .employee(emp).company(company).leaveType(LeaveType.ANNUAL)
                    .year(year).totalDays(20).usedDays((i * 3) % 9).pendingDays(i == 3 ? 3 : 0).build());
            leaveBalanceRepository.save(LeaveBalance.builder()
                    .employee(emp).company(company).leaveType(LeaveType.SICK)
                    .year(year).totalDays(14).usedDays((i * 2) % 5).build());
            leaveBalanceRepository.save(LeaveBalance.builder()
                    .employee(emp).company(company).leaveType(LeaveType.CASUAL)
                    .year(year).totalDays(10).usedDays(i % 4).build());
            i++;
        }
    }

    private void seedVendorsAndAssets() {
        String[][] vendors = {
                {"Amber IT Ltd.", "Corporate internet", "sales@amberit.example.com"},
                {"Star Tech & Engineering", "Hardware supplier", "corp@startech.example.com"},
                {"Amazon Web Services", "Cloud hosting", "billing@aws.example.com"},
        };
        for (String[] v : vendors) {
            Vendor vendor = new Vendor();
            vendor.setCompanyId(company.getId());
            vendor.setName(v[0]);
            vendor.setNotes(v[1]);
            vendor.setEmail(v[2]);
            vendor.setPaymentTerms("Net 30");
            vendorRepository.save(vendor);
        }

        Object[][] assets = {
                {"MacBook Pro 14\" (engineering)", "Computers", 280_000, 36},
                {"Dell OptiPlex workstations x4", "Computers", 340_000, 48},
                {"Standby generator 12kVA", "Facilities", 450_000, 96},
        };
        int i = 1;
        for (Object[] a : assets) {
            FixedAsset asset = new FixedAsset();
            asset.setCompanyId(company.getId());
            asset.setName((String) a[0]);
            asset.setAssetTag("DEMO-FA-" + i);
            asset.setCategory((String) a[1]);
            asset.setCost(BigDecimal.valueOf((int) a[2]));
            asset.setUsefulLifeMonths((int) a[3]);
            asset.setAcquisitionDate(LocalDate.now().minusMonths(6L * i));
            fixedAssetRepository.save(asset);
            i++;
        }
    }

    private void seedRecruitment() {
        jobPostingRepository.save(JobPosting.builder()
                .title("Senior Backend Engineer (Java/Spring)")
                .jobTitle("Senior Backend Engineer")
                .description("Own services end to end in a product used by real companies daily.")
                .requirements("4+ years Java, Spring Boot, PostgreSQL. Dhaka-based or willing to relocate.")
                .employmentType(EmploymentType.FULL_TIME)
                .status(JobPostingStatus.OPEN)
                .salaryMin(BigDecimal.valueOf(90_000)).salaryMax(BigDecimal.valueOf(140_000))
                .vacancies(2).deadline(LocalDate.now().plusDays(21))
                .location("Banani, Dhaka")
                .company(company).build());
        jobPostingRepository.save(JobPosting.builder()
                .title("Sales Executive (B2B Software)")
                .jobTitle("Sales Executive")
                .description("Take our CRM and service-desk suite to garment and healthcare clients.")
                .requirements("2+ years B2B sales; software or telecom background preferred.")
                .employmentType(EmploymentType.FULL_TIME)
                .status(JobPostingStatus.OPEN)
                .salaryMin(BigDecimal.valueOf(35_000)).salaryMax(BigDecimal.valueOf(55_000))
                .vacancies(1).deadline(LocalDate.now().plusDays(14))
                .location("Banani, Dhaka")
                .company(company).build());
    }

    private void seedSoftwareLicences() {
        SoftwareLicense jb = new SoftwareLicense();
        jb.setCompanyId(company.getId());
        jb.setSoftwareName("JetBrains All Products Pack");
        jb.setPublisher("JetBrains");
        jb.setLicenseKey("DEMO-JB-2026");
        jb.setLicenseType(LicenseTypeSafe.subscription());
        jb.setTotalSeatsLicensed(5);
        jb.setSeatsUsed(4);
        jb.setSeatsAvailable(1);
        jb.setLicensePurchaseDate(LocalDate.now().minusMonths(5));
        jb.setLicenseCost(BigDecimal.valueOf(45_000));
        softwareLicenseRepository.save(jb);

        SoftwareLicense figma = new SoftwareLicense();
        figma.setCompanyId(company.getId());
        figma.setSoftwareName("Figma Professional");
        figma.setPublisher("Figma");
        figma.setLicenseKey("DEMO-FIGMA-2026");
        figma.setLicenseType(LicenseTypeSafe.subscription());
        figma.setTotalSeatsLicensed(3);
        figma.setSeatsUsed(2);
        figma.setSeatsAvailable(1);
        figma.setLicensePurchaseDate(LocalDate.now().minusMonths(2));
        figma.setLicenseCost(BigDecimal.valueOf(12_000));
        softwareLicenseRepository.save(figma);
    }

    private void seedServiceDesk() {
        ServiceCategory dev = new ServiceCategory();
        dev.setCompanyId(company.getId());
        dev.setName("Software Development");
        dev.setDescription("Custom applications, integrations and modules");
        dev.setSortOrder(1);
        serviceCategoryRepository.save(dev);

        ServiceCategory support = new ServiceCategory();
        support.setCompanyId(company.getId());
        support.setName("Support & Maintenance");
        support.setDescription("SLAs, retainers and incident support");
        support.setSortOrder(2);
        serviceCategoryRepository.save(support);

        WorkflowTemplate flow = WorkflowTemplate.builder()
                .name("Standard Delivery")
                .description("Requirements through UAT for fixed-scope work")
                .active(true).company(company).build();
        flow.getStages().add(stage(flow, 1, "Requirements & Estimate", 3, 48, false, "Business Analyst"));
        flow.getStages().add(stage(flow, 2, "Development", 10, null, false, "Engineering"));
        flow.getStages().add(stage(flow, 3, "UAT & Handover", 4, 72, true, "Project Manager"));
        workflowTemplateRepository.save(flow);

        CompanyService webApp = service("Custom Web Application", dev, flow,
                250_000, "Requirement-driven web systems on our standard stack", 30);
        CompanyService mobile = service("Mobile App Development", dev, flow,
                400_000, "iOS and Android apps with a shared backend", 45);
        service("Annual Maintenance Contract", support, flow,
                120_000, "Yearly SLA: monitoring, fixes, and priority support", 365);

        request("Distributor order tracking portal", clients.get(0), webApp,
                ServiceRequestStatus.IN_PROGRESS, 1);
        request("Production-floor attendance kiosks", clients.get(1), mobile,
                ServiceRequestStatus.PENDING, 0);
        request("Patient portal phase 2", clients.get(2), webApp,
                ServiceRequestStatus.UNDER_REVIEW, 2);

        KbArticle a1 = new KbArticle();
        a1.setCompanyId(company.getId());
        a1.setTitle("How to raise a service request");
        a1.setSummary("From choosing a service to tracking its workflow stages.");
        a1.setContent("Pick the service that fits, describe what you need, and submit. You can follow "
                + "progress stage by stage, comment on the request, and approve the handover at the end.");
        a1.setStatus(KbArticleStatus.PUBLISHED);
        a1.setClientVisible(true);
        a1.setPublishedAt(LocalDateTime.now().minusDays(15));
        a1.setCategory(dev);
        a1.setAuthor(owner);
        kbArticleRepository.save(a1);

        KbArticle a2 = new KbArticle();
        a2.setCompanyId(company.getId());
        a2.setTitle("What our SLAs cover");
        a2.setSummary("Response times, escalation, and what counts as an incident.");
        a2.setContent("Maintenance contracts include a 4-hour first response during business hours, "
                + "next-business-day for low-priority items, and monthly reporting.");
        a2.setStatus(KbArticleStatus.PUBLISHED);
        a2.setClientVisible(true);
        a2.setPublishedAt(LocalDateTime.now().minusDays(40));
        a2.setCategory(support);
        a2.setAuthor(owner);
        kbArticleRepository.save(a2);
    }

    private WorkflowStage stage(WorkflowTemplate flow, int order, String name,
                                Integer days, Integer slaHours, boolean approval, String role) {
        return WorkflowStage.builder()
                .name(name).stageOrder(order).estimatedDays(days).slaHours(slaHours)
                .requiresApproval(approval).assigneeRole(role)
                .company(company).workflowTemplate(flow).build();
    }

    private CompanyService service(String name, ServiceCategory category, WorkflowTemplate flow,
                                   int price, String description, int days) {
        CompanyService s = new CompanyService();
        s.setName(name);
        s.setDescription(description);
        s.setPrice(BigDecimal.valueOf(price));
        s.setPriceType(ServicePriceType.FIXED);
        s.setCurrency("BDT");
        s.setEstimatedDays(days);
        s.setActive(true);
        s.setCompany(company);
        s.setCategory(category);
        s.setWorkflowTemplate(flow);
        return companyServiceRepository.save(s);
    }

    private void request(String title, Client client, CompanyService service,
                         ServiceRequestStatus status, int stageIndex) {
        ServiceRequest r = new ServiceRequest();
        r.setTitle(title);
        r.setDescription("Raised from the client portal.");
        r.setStatus(status);
        r.setPriority(ServiceRequestPriority.NORMAL);
        r.setCurrentStage(stageIndex);
        r.setAgreedPrice(service.getPrice());
        r.setCompanyService(service);
        r.setClient(client);
        r.setCompany(company);
        if (status != ServiceRequestStatus.PENDING) {
            r.setAssignedEmployee(staff.get(2));
            r.setAssignedAt(LocalDateTime.now().minusDays(5));
        }
        serviceRequestRepository.save(r);
    }

    /** LicenseType values are unverified in this codebase corner; resolved reflectively with a fallback. */
    private static final class LicenseTypeSafe {
        static com.businessos.modules.itam.software.LicenseType subscription() {
            try {
                return com.businessos.modules.itam.software.LicenseType.valueOf("SUBSCRIPTION");
            } catch (IllegalArgumentException e) {
                return com.businessos.modules.itam.software.LicenseType.values()[0];
            }
        }
    }
}
