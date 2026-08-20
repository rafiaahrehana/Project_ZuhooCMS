package com.businessos.modules.hrm.recruitment.careerpage;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/** Company-side configuration of the public careers page. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/career-page")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class CareerPageController {

    private static final String SLUG_PATTERN = "[a-z0-9](?:[a-z0-9-]{1,58}[a-z0-9])?";

    private final CareerPageSettingsRepository settingsRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @GetMapping
    @Transactional
    public ResponseEntity<CareerPageSettingsDto> get() {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_VIEW);
        Long companyId = requireCompanyId();
        CareerPageSettings settings = settingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> settingsRepository.save(defaults(companyId)));
        return ResponseEntity.ok(CareerPageSettingsDto.from(settings));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<CareerPageSettingsDto> update(@RequestBody CareerPageSettingsDto request) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_CREATE);
        Long companyId = requireCompanyId();

        String slug = request.getSlug() == null ? "" : request.getSlug().trim().toLowerCase(Locale.ROOT);
        if (!slug.matches(SLUG_PATTERN)) {
            throw new BadRequestException(
                "Slug must be 1-60 lowercase letters, digits or hyphens (no leading/trailing hyphen)");
        }
        if (settingsRepository.existsBySlugIgnoreCaseAndCompanyIdNot(slug, companyId)) {
            throw new BadRequestException("That slug is already taken - pick another");
        }

        CareerPageSettings settings = settingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> defaults(companyId));
        settings.setSlug(slug);
        settings.setHeadline(request.getHeadline());
        settings.setAbout(request.getAbout());
        settings.setBrandColor(request.getBrandColor());
        settings.setPublished(request.isPublished());
        return ResponseEntity.ok(CareerPageSettingsDto.from(settingsRepository.save(settings)));
    }

    /** First open seeds a slug from the company name so the page works immediately. */
    private CareerPageSettings defaults(Long companyId) {
        String base = companyRepository.findById(companyId)
                .map(Company::getCompanyName)
                .orElse("company")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "company";
        if (base.length() > 40) base = base.substring(0, 40).replaceAll("-$", "");
        String slug = base;
        int suffix = 2;
        while (settingsRepository.existsBySlugIgnoreCaseAndCompanyIdNot(slug, companyId)) {
            slug = base + "-" + suffix++;
        }
        return CareerPageSettings.builder()
                .companyId(companyId)
                .slug(slug)
                .headline("Join our team")
                .published(true)
                .build();
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    @Getter @Setter
    public static class CareerPageSettingsDto {
        private String slug;
        private String headline;
        private String about;
        private String brandColor;
        private boolean published;

        static CareerPageSettingsDto from(CareerPageSettings s) {
            CareerPageSettingsDto dto = new CareerPageSettingsDto();
            dto.slug = s.getSlug();
            dto.headline = s.getHeadline();
            dto.about = s.getAbout();
            dto.brandColor = s.getBrandColor();
            dto.published = s.isPublished();
            return dto;
        }
    }
}
