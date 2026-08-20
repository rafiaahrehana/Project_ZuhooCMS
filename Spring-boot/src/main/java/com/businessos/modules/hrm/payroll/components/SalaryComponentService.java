package com.businessos.modules.hrm.payroll.components;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.salary.SalaryStructure;
import com.businessos.modules.hrm.salary.SalaryStructureRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.businessos.modules.hrm.payroll.components.SalaryComponent.CalculationType.FIXED;
import static com.businessos.modules.hrm.payroll.components.SalaryComponent.CalculationType.PERCENTAGE;
import static com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.*;

@Service
@RequiredArgsConstructor
public class SalaryComponentService {

    private final SalaryComponentRepository componentRepository;
    private final SalaryStructureTemplateRepository templateRepository;
    private final StructureExtraComponentRepository extraRepository;
    private final SalaryStructureRepository structureRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    // ── Catalog ────────────────────────────────────────────────

    /** Lists the catalog, seeding the standard IT-company set on first read. */
    @Transactional
    public List<SalaryComponent> listCatalog() {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        if (!componentRepository.existsByCompanyId(companyId)) {
            seedDefaults(companyId);
        }
        return componentRepository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId);
    }

    @Transactional
    public SalaryComponent createComponent(SalaryComponent input) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        Company company = companyRepository.getReferenceById(securityUtil.getCurrentCompanyId());
        input.setCompany(company);
        input.setId(null);
        return componentRepository.save(input);
    }

    @Transactional
    public SalaryComponent updateComponent(Long id, SalaryComponent input) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        SalaryComponent existing = owned(id);
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setCalculationType(input.getCalculationType());
        existing.setTaxable(input.getTaxable());
        existing.setActive(input.getActive());
        if (input.getSortOrder() != null) existing.setSortOrder(input.getSortOrder());
        return componentRepository.save(existing);
    }

    private SalaryComponent owned(Long id) {
        SalaryComponent c = componentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Component not found"));
        if (!c.getCompany().getId().equals(securityUtil.getCurrentCompanyId()))
            throw new IllegalArgumentException("Component not found");
        return c;
    }

    private void seedDefaults(Long companyId) {
        Company company = companyRepository.getReferenceById(companyId);
        record Def(String name, SalaryComponent.ComponentType type,
                   SalaryComponent.CalculationType calc, boolean taxable) {}
        List<Def> defs = List.of(
            new Def("House Rent Allowance", EARNING, PERCENTAGE, true),
            new Def("Medical Allowance", EARNING, FIXED, true),
            new Def("Transport Allowance", EARNING, FIXED, true),
            new Def("Internet Allowance", EARNING, FIXED, true),
            new Def("Mobile Allowance", EARNING, FIXED, true),
            new Def("Meal Allowance", EARNING, FIXED, true),
            new Def("Special Allowance", EARNING, FIXED, true),
            new Def("Project Allowance", EARNING, FIXED, true),
            new Def("Performance Bonus", EARNING, FIXED, true),
            new Def("Festival Bonus", EARNING, FIXED, true),
            new Def("Commission", EARNING, PERCENTAGE, true),
            new Def("Income Tax", DEDUCTION, FIXED, false),
            new Def("Provident Fund (Employee)", DEDUCTION, PERCENTAGE, false),
            new Def("Health Insurance", DEDUCTION, FIXED, false),
            new Def("Loan Deduction", DEDUCTION, FIXED, false),
            new Def("Professional Tax", DEDUCTION, FIXED, false),
            new Def("Other Deduction", DEDUCTION, FIXED, false),
            new Def("Provident Fund (Employer)", EMPLOYER_CONTRIBUTION, PERCENTAGE, false),
            new Def("Gratuity", EMPLOYER_CONTRIBUTION, FIXED, false),
            new Def("Health Insurance Contribution", EMPLOYER_CONTRIBUTION, FIXED, false),
            new Def("Pension Contribution", EMPLOYER_CONTRIBUTION, FIXED, false));
        int order = 0;
        for (Def d : defs) {
            componentRepository.save(SalaryComponent.builder()
                    .company(company).name(d.name()).type(d.type())
                    .calculationType(d.calc()).taxable(d.taxable())
                    .active(true).sortOrder(order++).build());
        }
    }

    // ── Templates ──────────────────────────────────────────────

    /** Lists templates, seeding a standard IT grade card on first read. */
    @Transactional
    public List<SalaryStructureTemplate> listTemplates() {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        List<SalaryStructureTemplate> existing =
                templateRepository.findByCompanyIdOrderByStructureNameAsc(companyId);
        if (!existing.isEmpty()) return existing;

        // A typical Bangladeshi IT grade card: basic 50% of gross, HRA 40% of
        // basic, fixed allowances scaled with the grade. Editable/deletable -
        // this is a starting point, not policy.
        Company company = companyRepository.getReferenceById(companyId);
        record Grade(String name, int gross, int medical, int transport, int meal, int internet, int mobile) {}
        List<Grade> grades = List.of(
                new Grade("Intern", 20_000, 1_500, 1_000, 1_000, 500, 500),
                new Grade("Junior Software Engineer", 45_000, 3_000, 2_000, 1_500, 1_000, 500),
                new Grade("Software Engineer", 80_000, 4_000, 2_500, 2_000, 1_500, 1_000),
                new Grade("Senior Software Engineer", 130_000, 5_000, 3_000, 2_500, 2_000, 1_500),
                new Grade("Tech Lead", 200_000, 6_000, 4_000, 3_000, 2_500, 2_000));
        for (Grade g : grades) {
            templateRepository.save(SalaryStructureTemplate.builder()
                    .company(company)
                    .structureName(g.name())
                    .defaultGross(BigDecimal.valueOf(g.gross()))
                    .basicPercentage(new BigDecimal("50"))
                    .hraPercentage(new BigDecimal("40"))
                    .medicalAmount(BigDecimal.valueOf(g.medical()))
                    .transportAmount(BigDecimal.valueOf(g.transport()))
                    .mealAmount(BigDecimal.valueOf(g.meal()))
                    .internetAmount(BigDecimal.valueOf(g.internet()))
                    .mobileAmount(BigDecimal.valueOf(g.mobile()))
                    .active(true)
                    .build());
        }
        return templateRepository.findByCompanyIdOrderByStructureNameAsc(companyId);
    }

    @Transactional
    public SalaryStructureTemplate saveTemplate(Long id, SalaryStructureTemplate input) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();
        SalaryStructureTemplate t;
        if (id == null) {
            t = new SalaryStructureTemplate();
            t.setCompany(companyRepository.getReferenceById(companyId));
        } else {
            t = templateRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found"));
            if (!t.getCompany().getId().equals(companyId))
                throw new IllegalArgumentException("Template not found");
        }
        t.setStructureName(input.getStructureName());
        t.setDefaultGross(input.getDefaultGross());
        t.setBasicPercentage(nz(input.getBasicPercentage()));
        t.setHraPercentage(nz(input.getHraPercentage()));
        t.setMedicalAmount(nz(input.getMedicalAmount()));
        t.setTransportAmount(nz(input.getTransportAmount()));
        t.setInternetAmount(nz(input.getInternetAmount()));
        t.setMobileAmount(nz(input.getMobileAmount()));
        t.setMealAmount(nz(input.getMealAmount()));
        if (input.getActive() != null) t.setActive(input.getActive());
        return templateRepository.save(t);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_DELETE);
        SalaryStructureTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        if (!t.getCompany().getId().equals(securityUtil.getCurrentCompanyId()))
            throw new IllegalArgumentException("Template not found");
        templateRepository.delete(t);
    }

    /**
     * The spec's breakdown: basic = gross x basic%, HRA = basic x hra%, the
     * fixed allowances as configured, and special allowance soaks up whatever
     * of the gross remains (never negative).
     *
     * Internet and mobile are NOT subtracted from gross: the structure has no
     * columns for them, so they attach as recurring extra components paid on
     * top of gross - subtracting them here made the six structure fields sum
     * to less than gross and the form flagged a mismatch.
     */
    public Map<String, BigDecimal> breakdown(Long templateId, BigDecimal gross) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        SalaryStructureTemplate t = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        if (!t.getCompany().getId().equals(securityUtil.getCurrentCompanyId()))
            throw new IllegalArgumentException("Template not found");
        BigDecimal g = nz(gross);
        BigDecimal basic = pct(g, t.getBasicPercentage());
        BigDecimal hra = pct(basic, t.getHraPercentage());
        BigDecimal fixed = nz(t.getMedicalAmount()).add(nz(t.getTransportAmount())).add(nz(t.getMealAmount()));
        BigDecimal special = g.subtract(basic).subtract(hra).subtract(fixed);
        if (special.signum() < 0) special = BigDecimal.ZERO;

        Map<String, BigDecimal> out = new LinkedHashMap<>();
        out.put("grossSalary", g);
        out.put("basicSalary", basic);
        out.put("houseRent", hra);
        out.put("medicalAllowance", nz(t.getMedicalAmount()));
        out.put("transportAllowance", nz(t.getTransportAmount()));
        out.put("internetAllowance", nz(t.getInternetAmount()));
        out.put("mobileAllowance", nz(t.getMobileAmount()));
        out.put("foodAllowance", nz(t.getMealAmount()));
        out.put("specialAllowance", special);
        return out;
    }

    // ── Per-structure extra components ─────────────────────────

    public List<StructureExtraComponent> listExtras(Long structureId) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_VIEW);
        ownedStructure(structureId);
        return extraRepository.findByStructureIdOrderByIdAsc(structureId);
    }

    /** Replaces the structure's extra-component list wholesale. */
    @Transactional
    public List<StructureExtraComponent> setExtras(Long structureId, List<ExtraLine> lines) {
        authorizationService.checkPermission(PermissionCode.SALARY_STRUCTURE_CREATE);
        SalaryStructure structure = ownedStructure(structureId);
        extraRepository.deleteByStructureId(structureId);
        List<StructureExtraComponent> saved = new ArrayList<>();
        for (ExtraLine line : lines) {
            SalaryComponent component = owned(line.componentId());
            saved.add(extraRepository.save(StructureExtraComponent.builder()
                    .structure(structure).component(component)
                    .amount(nz(line.amount())).build()));
        }
        return saved;
    }

    /** Sum of active extra components of one type - what payroll folds in. */
    public BigDecimal sumExtras(Long structureId, SalaryComponent.ComponentType type) {
        return extraRepository.findByStructureIdOrderByIdAsc(structureId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getComponent().getActive()))
                .filter(e -> e.getComponent().getType() == type)
                .map(e -> nz(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record ExtraLine(Long componentId, BigDecimal amount) {}

    private SalaryStructure ownedStructure(Long id) {
        SalaryStructure s = structureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Salary structure not found"));
        if (!s.getCompany().getId().equals(securityUtil.getCurrentCompanyId()))
            throw new IllegalArgumentException("Salary structure not found");
        return s;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal pct(BigDecimal base, BigDecimal percent) {
        return base.multiply(nz(percent)).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
