package com.businessos.modules.demo;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.enums.*;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.crm.activity.CrmActivity;
import com.businessos.modules.crm.activity.CrmActivityRepository;
import com.businessos.modules.crm.activity.CrmActivityType;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.lead.Lead;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.modules.crm.opportunity.Opportunity;
import com.businessos.modules.crm.opportunity.OpportunityRepository;
import com.businessos.modules.crm.opportunity.OpportunityStage;
import com.businessos.modules.finance.budget.Budget;
import com.businessos.modules.finance.budget.BudgetRepository;
import com.businessos.modules.finance.expense.Expense;
import com.businessos.modules.finance.expense.ExpenseRepository;
import com.businessos.modules.finance.expense.ExpenseStatus;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceItem;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.modules.hrm.attendance.attendance.Attendance;
import com.businessos.modules.hrm.attendance.attendance.AttendanceRepository;
import com.businessos.modules.hrm.attendance.attendance.AttendanceStatus;
import com.businessos.modules.hrm.attendance.attendance.ShiftType;
import com.businessos.modules.hrm.attendance.shift.EmployeeShiftAssignment;
import com.businessos.modules.hrm.attendance.shift.EmployeeShiftAssignmentRepository;
import com.businessos.modules.hrm.attendance.shift.Shift;
import com.businessos.modules.hrm.attendance.shift.ShiftRepository;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.department.DepartmentRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequest;
import com.businessos.modules.hrm.leave.leaverequest.LeaveRequestRepository;
import com.businessos.modules.hrm.payroll.Payroll;
import com.businessos.modules.hrm.payroll.PayrollRepository;
import com.businessos.modules.hrm.salary.SalaryStructure;
import com.businessos.modules.hrm.salary.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the read-only demo tenant that "See Demo" on the landing page drops
 * visitors into.
 *
 * Runs at startup when app.demo.enabled=true and the demo subdomain does not
 * exist yet, so wiping the database (the plan for the AWS deploy) recreates the
 * demo automatically on next boot.
 *
 * The cast is hand-written rather than faker-generated on purpose: a curated
 * Bangladeshi company reads like a real business, random names attached to
 * random numbers read like a test database. All dates are relative to today so
 * the demo looks freshly alive in any month.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_SUBDOMAIN = "demo";
    public static final String DEMO_OWNER_EMAIL = "demo@dhrubotara.example.com";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final CrmActivityRepository crmActivityRepository;
    private final ClientInvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final com.businessos.modules.hrm.payroll.settings.PayrollSettingsService payrollSettingsService;
    private final PasswordEncoder passwordEncoder;

    private Company company;
    private User owner;
    private final List<Employee> staff = new ArrayList<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (companyRepository.existsBySubdomain(DEMO_SUBDOMAIN)) {
            return;
        }
        log.info("Seeding demo tenant '{}'...", DEMO_SUBDOMAIN);
        seedCompanyAndOwner();
        // Persist the default payroll policy while we are in a read-write
        // transaction - the salary sheet's read path must never have to.
        payrollSettingsService.getOrCreate(company.getId());
        List<Department> departments = seedDepartments();
        seedStaff(departments);
        Shift shift = seedShift();
        seedAttendance(shift);
        seedLeaves();
        seedPayroll();
        List<Client> clients = seedClients();
        seedLeads();
        seedOpportunities(clients);
        seedFinance(clients);
        log.info("Demo tenant seeded: company id {}, {} employees.", company.getId(), staff.size());
    }

    // ── Company ─────────────────────────────────────────────────

    private void seedCompanyAndOwner() {
        owner = userRepository.save(User.builder()
                .firstName("Tanvir Ahmed")
                .lastName("Chowdhury")
                .email(DEMO_OWNER_EMAIL)
                // Random password, never told to anyone: the demo session
                // endpoint mints tokens directly, and nobody should be able to
                // log into the demo account with credentials.
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .role(Role.COMPANY_OWNER)
                .active(true)
                .emailVerified(true)
                .build());

        company = companyRepository.save(Company.builder()
                .companyName("Dhrubotara Software Ltd.")
                .subdomain(DEMO_SUBDOMAIN)
                .companyEmail("hello@dhrubotara.example.com")
                .companyPhone("+880 1711-000000")
                .subscriptionPlan("FREE")
                .status(CompanyStatus.TRIAL)
                .owner(owner)
                .build());
        company.setBankName("Dutch-Bangla Bank");
        company.setBankAccountName("Dhrubotara Software Ltd.");
        company.setBankAccountNumber("117.110.0000123");
        company.setBankBranch("Banani Branch, Dhaka");
        companyRepository.save(company);
    }

    private List<Department> seedDepartments() {
        List<Department> out = new ArrayList<>();
        for (String[] d : new String[][]{
                {"MGT", "Management"}, {"HR", "Human Resources"}, {"ENG", "Engineering"},
                {"DES", "Design"}, {"FIN", "Finance"}, {"SAL", "Sales"}, {"SUP", "Support"}}) {
            out.add(departmentRepository.save(Department.builder()
                    .code(d[0]).name(d[1]).active(true).company(company).build()));
        }
        return out;
    }

    // ── People ──────────────────────────────────────────────────

    private record Person(String first, String last, String title, int dept, long basic) {}

    private void seedStaff(List<Department> departments) {
        // Owner is also employee #1 so "my work" screens resolve for the demo user.
        List<Person> people = List.of(
                new Person("Tanvir Ahmed", "Chowdhury", "Managing Director", 0, 150_000),
                new Person("Farhana", "Yasmin", "HR Manager", 1, 65_000),
                new Person("Md. Rakibul", "Hasan", "Senior Software Engineer", 2, 85_000),
                new Person("Nusrat Jahan", "Mim", "Software Engineer", 2, 60_000),
                new Person("Sabbir Rahman", "Khan", "Junior Developer", 2, 35_000),
                new Person("Tasnim", "Akter", "UI/UX Designer", 3, 55_000),
                new Person("Md. Shahriar", "Kabir", "DevOps Engineer", 2, 75_000),
                new Person("Ishrat Jahan", "Priya", "Accounts Officer", 4, 45_000),
                new Person("Abdullah", "Al Mamun", "Sales Executive", 5, 40_000),
                new Person("Sumaiya", "Islam", "Support Engineer", 6, 38_000));

        int n = 1;
        for (Person p : people) {
            User user = p.first().startsWith("Tanvir") ? owner : userRepository.save(User.builder()
                    .firstName(p.first()).lastName(p.last())
                    .email(slug(p.first()) + "." + slug(p.last()) + "@dhrubotara.example.com")
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(Role.EMPLOYEE).active(true).emailVerified(true).build());

            BigDecimal basic = BigDecimal.valueOf(p.basic());
            Employee emp = employeeRepository.save(Employee.builder()
                    .user(user)
                    .company(company)
                    .department(departments.get(p.dept()))
                    .employeeNumber(String.format("DEMO-%04d", n))
                    .jobTitle(p.title())
                    .officialEmail(slug(p.first()) + "@dhrubotara.example.com")
                    .workPhone("+880 17" + String.format("%02d", n) + "-123456")
                    .employmentType(EmploymentType.FULL_TIME)
                    .employmentStatus(n == 5 ? EmploymentStatus.PROBATION : EmploymentStatus.ACTIVE)
                    .hireDate(LocalDate.now().minusMonths(6 + n * 2L))
                    .basicSalary(basic)
                    .houseRent(pct(basic, 40))
                    .medicalAllowance(pct(basic, 10))
                    .transportAllowance(pct(basic, 10))
                    .active(true)
                    .build());
            staff.add(emp);

            // Approved, effective-dated structure - what payroll and the
            // salary sheet actually read.
            BigDecimal gross = basic.add(pct(basic, 40)).add(pct(basic, 10)).add(pct(basic, 10));
            salaryStructureRepository.save(SalaryStructure.builder()
                    .employee(emp).company(company)
                    .effectiveFrom(LocalDate.now().minusMonths(4).withDayOfMonth(1))
                    .basicSalary(basic)
                    .houseRent(pct(basic, 40))
                    .medicalAllowance(pct(basic, 10))
                    .transportAllowance(pct(basic, 10))
                    .providentFund(pct(basic, 10))
                    .taxDeduction(pct(basic, 5))
                    .grossSalary(gross)
                    .approvedBy(owner)
                    .build());
            n++;
        }
    }

    private Shift seedShift() {
        Shift shift = shiftRepository.save(Shift.builder()
                .name("General Shift")
                .shiftType(ShiftType.MORNING)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .gracePeriodMinutes(10)
                .weeklyOffDays("FRI,SAT")
                .active(true)
                .company(company)
                .build());
        for (Employee emp : staff) {
            shiftAssignmentRepository.save(EmployeeShiftAssignment.builder()
                    .companyId(company.getId()).employee(emp).shift(shift)
                    .assignmentStartDate(LocalDate.now().minusMonths(4))
                    .active(true).assignedBy("Seeder").build());
        }
        return shift;
    }

    /**
     * ~40 working days back from yesterday. Mostly present, a deterministic
     * scatter of lates and a few absences, so the HR dashboard tiles, the Late
     * column and the salary sheet's absence deductions all have real shape.
     * Deterministic (keyed on day/employee index), not random: the same demo
     * should look the same on every reseed.
     */
    private void seedAttendance(Shift shift) {
        // Today gets check-ins with no check-out (the team is mid-shift), so
        // "Present Today" on the HR dashboard is alive on the day the demo is
        // viewed. Safe from the absentee scheduler, which only settles past
        // days. Skipped on the weekly off days, honestly.
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (today != DayOfWeek.FRIDAY && today != DayOfWeek.SATURDAY) {
            int e = 0;
            for (Employee emp : staff) {
                boolean late = e % 5 == 2;
                attendanceRepository.save(Attendance.builder()
                        .companyId(company.getId())
                        .employee(emp)
                        .attendanceDate(LocalDate.now())
                        .shiftType(ShiftType.MORNING)
                        .status(late ? AttendanceStatus.LATE : AttendanceStatus.PRESENT)
                        .checkInTime(LocalTime.of(9, late ? 18 : 0).plusMinutes(e % 4))
                        .isLate(late)
                        .lateMinutes(late ? 8 + e : 0)
                        .approved(true)
                        .build());
                e++;
            }
        }

        LocalDate day = LocalDate.now().minusDays(1);
        int workingDays = 0;
        while (workingDays < 40) {
            DayOfWeek dow = day.getDayOfWeek();
            if (dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SATURDAY) {
                int e = 0;
                for (Employee emp : staff) {
                    int key = (workingDays * 31 + e * 7) % 23;
                    boolean absent = key == 11 && e > 0;         // ~4% absence, never the MD
                    boolean late = !absent && key % 6 == 1;      // ~16% late
                    int lateBy = late ? 12 + (key * 3) % 25 : 0; // 12-36 minutes

                    Attendance.AttendanceBuilder b = Attendance.builder()
                            .companyId(company.getId())
                            .employee(emp)
                            .attendanceDate(day)
                            .shiftType(ShiftType.MORNING);
                    if (absent) {
                        b.status(AttendanceStatus.ABSENT);
                    } else {
                        LocalTime in = LocalTime.of(9, 0).plusMinutes(late ? lateBy : (key % 8) - 4);
                        if (in.isBefore(LocalTime.of(8, 40))) in = LocalTime.of(8, 56);
                        b.status(late ? AttendanceStatus.LATE : AttendanceStatus.PRESENT)
                                .checkInTime(in)
                                .checkOutTime(LocalTime.of(18, 0).plusMinutes(key % 3 == 0 ? 15 : 0))
                                .isLate(late)
                                .lateMinutes(late ? Math.max(0, lateBy - shift.getGracePeriodMinutes()) : 0)
                                .approved(true);
                    }
                    attendanceRepository.save(b.build());
                    e++;
                }
                workingDays++;
            }
            day = day.minusDays(1);
        }
    }

    private void seedLeaves() {
        // One pending (drives the approval queue), one approved (history).
        leaveRequestRepository.save(LeaveRequest.builder()
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(9))
                .totalDays(3)
                .reason("Family trip to Cox's Bazar")
                .status(LeaveRequestStatus.PENDING)
                .employee(staff.get(3))  // Nusrat
                .company(company)
                .build());
        leaveRequestRepository.save(LeaveRequest.builder()
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.now().minusDays(20))
                .endDate(LocalDate.now().minusDays(19))
                .totalDays(2)
                .reason("Fever")
                .status(LeaveRequestStatus.APPROVED)
                .reviewedBy(owner)
                .reviewedAt(LocalDateTime.now().minusDays(21))
                .employee(staff.get(2))  // Rakibul
                .company(company)
                .build());
    }

    /** Last month run and PAID, so Payroll Runs and Payslips show finished work. */
    private void seedPayroll() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        for (Employee emp : staff) {
            BigDecimal basic = emp.getBasicSalary();
            BigDecimal gross = basic.add(pct(basic, 40)).add(pct(basic, 10)).add(pct(basic, 10));
            BigDecimal pf = pct(basic, 10);
            BigDecimal tax = pct(basic, 5);
            payrollRepository.save(Payroll.builder()
                    .employee(emp).company(company)
                    .payMonth(lastMonth.getMonthValue()).payYear(lastMonth.getYear())
                    .basicSalary(basic)
                    .houseRent(pct(basic, 40))
                    .medicalAllowance(pct(basic, 10))
                    .transportAllowance(pct(basic, 10))
                    .providentFundDeduction(pf)
                    .taxDeduction(tax)
                    .netSalary(gross.subtract(pf).subtract(tax))
                    .status(PayrollStatus.PAID)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .paymentReference("DBBL-" + lastMonth.getMonthValue() + "-" + emp.getEmployeeNumber())
                    .paidAt(lastMonth.withDayOfMonth(28))
                    .approvedBy(staff.get(0))
                    .build());
        }
    }

    // ── CRM ─────────────────────────────────────────────────────

    private List<Client> seedClients() {
        List<Client> out = new ArrayList<>();
        String[][] rows = {
                {"Meghna Agro Foods Ltd.", "Food & Beverage", "Motijheel, Dhaka"},
                {"Padma Textiles", "Garments", "Narayanganj"},
                {"City General Hospital", "Healthcare", "Dhanmondi, Dhaka"},
                {"Sonar Bangla Tours & Travels", "Travel", "Sylhet"},
        };
        for (String[] r : rows) {
            Client c = new Client();
            c.setClientCompanyName(r[0]);
            c.setIndustry(r[1]);
            c.setBillingAddress(r[2]);
            c.setStatus(ClientStatus.ACTIVE);
            c.setOnboardedAt(LocalDate.now().minusMonths(3));
            c.setCompany(company);
            c.setAccountManager(staff.get(8)); // Abdullah, Sales
            out.add(clientRepository.save(c));
        }
        return out;
    }

    private void seedLeads() {
        Object[][] rows = {
                // name, org, status, source, priority, value, note
                {"Mahfuz Alam", "Rupali Logistics", LeadStatus.NEW, LeadSource.WEBSITE, Priority.HIGH, 700_000, "GM - wants fleet tracking for 120 trucks"},
                {"Dr. Salma Khatun", "Lazz Pharma (chain)", LeadStatus.QUALIFIED, LeadSource.REFERRAL, Priority.HIGH, 950_000, "Pharmacy POS + inventory across 14 outlets"},
                {"Rafiq Uddin", "Uttara Motors Service", LeadStatus.CONTACTED, LeadSource.SOCIAL_MEDIA, Priority.NORMAL, 250_000, "Service booking portal"},
                {"Sharmin Sultana", "Green Dhaka School", LeadStatus.CONTACTED, LeadSource.WEBSITE, Priority.NORMAL, 180_000, "School management system"},
                {"Kamrul Islam", "Bhai Bhai Traders", LeadStatus.DISQUALIFIED, LeadSource.PHONE, Priority.LOW, 30_000, "Budget nowhere near scope"},
                {"Nasrin Akter", "Kushiara Fisheries", LeadStatus.NEW, LeadSource.EMAIL, Priority.NORMAL, 400_000, "Cold-chain monitoring dashboard"},
                {"Imran Hossain", "Dhaka FM 91.2", LeadStatus.QUALIFIED, LeadSource.REFERRAL, Priority.NORMAL, 220_000, "Ad booking + billing system"},
                {"Sultana Razia", "Comilla Dairy", LeadStatus.CONTACTED, LeadSource.COLD_CALL, Priority.LOW, 150_000, "Distributor order app"},
        };
        int i = 0;
        for (Object[] r : rows) {
            Lead lead = new Lead();
            lead.setContactName((String) r[0]);
            lead.setCompanyName((String) r[1]);
            lead.setEmail(slug(((String) r[0]).split(" ")[0]) + "@" + slug(((String) r[1]).split(" ")[0]) + ".example.com");
            lead.setPhone("+880 18" + String.format("%02d", i + 10) + "-556677");
            lead.setStatus((LeadStatus) r[2]);
            lead.setSource((LeadSource) r[3]);
            lead.setPriority((Priority) r[4]);
            lead.setEstimatedValue(BigDecimal.valueOf((int) r[5]));
            lead.setNotes((String) r[6]);
            lead.setCompany(company);
            lead.setAssignedTo(staff.get(8));
            Lead saved = leadRepository.save(lead);

            // Two follow-ups due this week feed the dashboard's list; one is
            // already overdue so the notify-once scheduler has something real.
            if (i < 2) {
                crmActivityRepository.save(CrmActivity.builder()
                        .type(CrmActivityType.CALL)
                        .subject("Follow up on " + r[1] + " proposal")
                        .activityDate(LocalDateTime.now().minusDays(3))
                        .followUpAt(LocalDateTime.now().plusDays(i == 0 ? 2 : -1))
                        .followUpDone(false)
                        .completed(true)
                        .lead(saved)
                        .performedBy(owner)
                        .company(company)
                        .build());
            }
            i++;
        }
    }

    private void seedOpportunities(List<Client> clients) {
        // Open pipeline - one per stage, one deliberately undated.
        saveOpp("ERP for garments unit", clients.get(1), OpportunityStage.NEGOTIATION, 1_200_000,
                LocalDate.now().plusDays(20), null, null);
        saveOpp("Hospital management system", clients.get(2), OpportunityStage.PROPOSAL, 850_000,
                LocalDate.now().plusMonths(1), null, null);
        saveOpp("Grocery delivery app", clients.get(0), OpportunityStage.PRESENTATION, 600_000,
                LocalDate.now().plusMonths(2), null, null);
        saveOpp("Website revamp", clients.get(3), OpportunityStage.QUALIFICATION, 150_000,
                null, null, null); // undated on purpose - feeds the hygiene row

        // Won - the wonThisMonth tile and revenue chart.
        Opportunity won = saveOpp("Annual maintenance contract", clients.get(0), OpportunityStage.WON,
                450_000, LocalDate.now().minusDays(10), null, null);
        won.setActualCloseDate(LocalDate.now().minusDays(10));
        opportunityRepository.save(won);

        // Lost with picklist codes - the "Why Deals Are Lost" chart.
        Object[][] lost = {
                {"Corporate intranet", clients.get(1), 500_000, LostReason.PRICE, "Quoted 40% above their ceiling"},
                {"Attendance devices rollout", clients.get(2), 350_000, LostReason.PRICE, "Went with cheaper hardware bundle"},
                {"Booking engine", clients.get(3), 420_000, LostReason.COMPETITOR, "Chose a local agency with tourism references"},
                {"Inventory audit tool", clients.get(0), 200_000, LostReason.NO_RESPONSE, "Silent after two demos"},
        };
        int d = 30;
        for (Object[] r : lost) {
            Opportunity o = saveOpp((String) r[0], (Client) r[1], OpportunityStage.LOST,
                    (int) r[2], LocalDate.now().minusDays(d), (LostReason) r[3], (String) r[4]);
            o.setActualCloseDate(LocalDate.now().minusDays(d));
            opportunityRepository.save(o);
            d += 25;
        }
    }

    private Opportunity saveOpp(String name, Client client, OpportunityStage stage, int amount,
                                LocalDate close, LostReason lostCode, String lostText) {
        Opportunity o = new Opportunity();
        o.setName(name);
        o.setClient(client);
        o.setStage(stage);
        o.setProbability(stage.getDefaultProbability());
        o.setAmount(BigDecimal.valueOf(amount));
        o.setExpectedCloseDate(close);
        o.setLostReasonCode(lostCode);
        o.setLostReason(lostText);
        o.setSource(LeadSource.REFERRAL);
        o.setOwner(staff.get(8));
        o.setCompany(company);
        return opportunityRepository.save(o);
    }

    // ── Finance ─────────────────────────────────────────────────

    private void seedFinance(List<Client> clients) {
        // Invoices: paid, partially paid and one overdue, so every status badge
        // and the finance dashboard's outstanding tile are real.
        invoice("DEMO-INV-0001", clients.get(0), 450_000, InvoiceStatus.PAID, -40, -10,
                "Annual maintenance contract - year 1");
        invoice("DEMO-INV-0002", clients.get(1), 300_000, InvoiceStatus.PAID, -35, -20,
                "ERP discovery & requirements phase");
        invoice("DEMO-INV-0003", clients.get(2), 425_000, InvoiceStatus.PARTIALLY_PAID, -25, 5,
                "HMS phase 1 - patient records module");
        invoice("DEMO-INV-0004", clients.get(3), 150_000, InvoiceStatus.OVERDUE, -45, -15,
                "Website revamp - design sprint");
        invoice("DEMO-INV-0005", clients.get(0), 120_000, InvoiceStatus.ISSUED, -5, 25,
                "Delivery app - UX prototype");

        Object[][] expenses = {
                {"Office rent - Banani", 85_000, "RENT", ExpenseStatus.PAID},
                {"Amber IT - dedicated internet", 12_000, "UTILITIES", ExpenseStatus.PAID},
                {"DESCO electricity", 9_500, "UTILITIES", ExpenseStatus.APPROVED},
                {"AWS bill", 28_000, "SOFTWARE", ExpenseStatus.APPROVED},
                {"Team lunch - project delivery", 7_800, "MEALS", ExpenseStatus.PENDING},
                {"JetBrains licences renewal", 45_000, "SOFTWARE", ExpenseStatus.PAID},
        };
        int i = 1;
        for (Object[] e : expenses) {
            Expense exp = new Expense();
            exp.setCompanyId(company.getId());
            exp.setExpenseNumber(String.format("DEMO-EXP-%04d", i));
            exp.setTitle((String) e[0]);
            exp.setAmount(BigDecimal.valueOf((int) e[1]));
            exp.setCategory((String) e[2]);
            exp.setStatus((ExpenseStatus) e[3]);
            exp.setExpenseDate(LocalDate.now().minusDays(3L * i));
            exp.setSubmittedBy(staff.get(7)); // Ishrat, Accounts
            if (e[3] != ExpenseStatus.PENDING) {
                exp.setApprovedBy(owner);
                exp.setApprovedDate(LocalDate.now().minusDays(3L * i - 1));
            }
            expenseRepository.save(exp);
            i++;
        }

        // Budgets per expense category; software deliberately runs slightly
        // over so the budget bar shows a genuine warning, not a decorated one.
        int year = LocalDate.now().getYear();
        budget("RENT", year, 1_100_000);
        budget("UTILITIES", year, 300_000);
        budget("SOFTWARE", year, 60_000);
        budget("MEALS", year, 120_000);
    }

    private void invoice(String number, Client client, int amount, InvoiceStatus status,
                         int issuedDaysFromNow, int dueDaysFromNow, String line) {
        ClientInvoice inv = new ClientInvoice();
        inv.setCompanyId(company.getId());
        inv.setInvoiceNumber(number);
        inv.setClient(client);
        inv.setInvoiceDate(LocalDate.now().plusDays(issuedDaysFromNow));
        inv.setDueDate(LocalDate.now().plusDays(dueDaysFromNow));
        inv.setStatus(status);
        BigDecimal total = BigDecimal.valueOf(amount);
        inv.setSubtotal(total);
        inv.setTotalAmount(total);
        BigDecimal paid = switch (status) {
            case PAID -> total;
            case PARTIALLY_PAID -> total.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
        inv.setPaidAmount(paid);
        inv.setBalanceAmount(total.subtract(paid));
        if (status == InvoiceStatus.PAID) inv.setPaidDate(inv.getDueDate());

        ClientInvoiceItem item = new ClientInvoiceItem();
        item.setInvoice(inv);
        item.setDescription(line);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(total);
        item.setLineTotal(total);
        inv.getItems().add(item);

        invoiceRepository.save(inv);
    }

    private void budget(String category, int year, int amount) {
        Budget b = new Budget();
        b.setCompanyId(company.getId());
        b.setCategory(category);
        b.setFiscalYear(year);
        b.setAmount(BigDecimal.valueOf(amount));
        budgetRepository.save(b);
    }

    // ── Helpers ─────────────────────────────────────────────────

    private static BigDecimal pct(BigDecimal base, int percent) {
        return base.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z]", "");
    }
}
