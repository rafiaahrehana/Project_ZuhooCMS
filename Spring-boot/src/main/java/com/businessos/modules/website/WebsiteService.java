package com.businessos.modules.website;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class WebsiteService {

    private final CompanyRepository companyRepository;
    private final WebsiteSettingsRepository settingsRepository;
    private final NavItemRepository navItemRepository;
    private final ServiceRepository serviceRepository;
    private final WebsiteContentRepository contentRepository;
    private final WebsitePersonRepository personRepository;
    private final FaqRepository faqRepository;
    private final PortalProjectRepository projectRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public Long resolveCompanyId(String host, String subdomainParam) {
        String subdomain = subdomainParam;
        if (subdomain == null && host != null) {
            String[] parts = host.split("\\.");
            if (parts.length > 2) subdomain = parts[0];
        }
        if (subdomain == null) {
            throw new ResourceNotFoundException("Tenant not identified");
        }
        final String resolved = subdomain;
        Company company = companyRepository.findBySubdomain(resolved.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + resolved));
        return company.getId();
    }

    public WebsiteSettings getSettings(Long companyId) {
        return settingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> defaultSettings(companyId));
    }

    public List<NavNode> getNav(Long companyId) {
        List<NavItem> flat = navItemRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
        Map<Long, List<NavItem>> byParent = flat.stream()
                .collect(Collectors.groupingBy(n -> n.getParentId() == null ? -1L : n.getParentId()));
        return buildTree(byParent, -1L);
    }

    private List<NavNode> buildTree(Map<Long, List<NavItem>> byParent, Long parentId) {
        List<NavItem> children = byParent.getOrDefault(parentId, List.of());
        List<NavNode> nodes = new ArrayList<>();
        for (NavItem n : children) {
            nodes.add(new NavNode(n.getId(), n.getLabel(), n.getUrl(), n.isExternal(), n.isMega(),
                    buildTree(byParent, n.getId())));
        }
        return nodes;
    }

    public record NavNode(Long id, String label, String url, boolean external, boolean mega, List<NavNode> children) {}

    public List<com.businessos.modules.website.Service> getServices(Long companyId, String category, String q) {
        if (q != null && !q.isBlank()) {
            return serviceRepository.findByCompanyIdAndTitleContainingIgnoreCase(q, companyId);
        }
        if (category != null && !category.isBlank()) {
            return serviceRepository.findByCompanyIdAndCategoryNameIgnoreCase(companyId, category);
        }
        return serviceRepository.findByCompanyId(companyId);
    }

    public com.businessos.modules.website.Service getService(Long companyId, String slug) {
        return serviceRepository.findBySlugAndCompanyId(slug, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + slug));
    }

    public List<WebsiteContent> getBlogs(Long companyId, String category) {
        if (category != null && !category.isBlank()) {
            return contentRepository.findByCompanyIdAndTypeAndCategoryIgnoreCase(companyId, ContentType.POST, category);
        }
        return contentRepository.findByCompanyIdAndTypeOrderByPublishedAtDesc(companyId, ContentType.POST);
    }

    public WebsiteContent getBlog(Long companyId, String slug) {
        return contentRepository.findBySlugAndCompanyIdAndType(slug, companyId, ContentType.POST)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + slug));
    }

    public List<WebsitePerson> getTestimonials(Long companyId) {
        return personRepository.findByCompanyIdAndType(companyId, PersonType.TESTIMONIAL);
    }

    public List<Faq> getFaqs(Long companyId) {
        return faqRepository.findByCompanyId(companyId);
    }

    public List<WebsitePerson> getTeam(Long companyId) {
        return personRepository.findByCompanyIdAndType(companyId, PersonType.TEAM_MEMBER);
    }

    public List<PortalProject> getProjects(Long companyId) {
        return projectRepository.findByCompanyId(companyId);
    }

    public List<PricingPlan> getPricing(Long companyId) {
        return pricingPlanRepository.findByCompanyId(companyId);
    }

    public List<Stat> getStats(Long companyId) {
        return getSettings(companyId).getStats();
    }

    public WebsiteContent getPage(Long companyId, String slug) {
        return contentRepository.findBySlugAndCompanyIdAndType(slug, companyId, ContentType.PAGE)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + slug));
    }

    @Transactional
    public void submitContact(Long companyId, WebsiteRequests.ContactRequest req) {
        // Persist as a service request for tracking; notifications handled by admin.
        ServiceRequest r = ServiceRequest.builder()
                .companyId(companyId)
                .code(generateCode())
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .message((req.subject() != null ? "[" + req.subject() + "] " : "") + req.message())
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();
        serviceRequestRepository.save(r);
    }

    @Transactional
    public void submitNewsletter(Long companyId, WebsiteRequests.NewsletterRequest req) {
        // In a real system this would subscribe the email; here we just record intent.
    }

    @Transactional
    public String createServiceRequest(Long companyId, WebsiteRequests.ServiceRequestPayload req) {
        String code = generateCode();
        ServiceRequest r = ServiceRequest.builder()
                .companyId(companyId)
                .code(code)
                .serviceId(req.serviceId())
                .serviceTitle(req.serviceTitle())
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .message(req.message())
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();
        serviceRequestRepository.save(r);
        return code;
    }

    public ServiceRequest trackRequest(Long companyId, String code) {
        return serviceRequestRepository.findByCodeAndCompanyId(code, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + code));
    }

    private String generateCode() {
        int n = 100000 + new Random().nextInt(900000);
        return "SR-" + LocalDateTime.now().getYear() + "-" + n;
    }

    private WebsiteSettings defaultSettings(Long companyId) {
        return WebsiteSettings.builder()
                .companyId(companyId)
                .companyName("Your Company")
                .tagline("Professional services, delivered with care")
                .primaryColor("#6D28D9")
                .secondaryColor("#2563EB")
                .gradient("linear-gradient(135deg, #6D28D9 0%, #2563EB 100%)")
                .font("'Inter', 'Plus Jakarta Sans', sans-serif")
                .radius(14)
                .buttonStyle("rounded")
                .darkMode(false)
                .navbarStyle("transparent")
                .footerStyle("dark")
                .animations(true)
                .spacing(1)
                .heroHeading("Welcome to Your Company")
                .heroSubheading("We help businesses grow with trusted, professional services.")
                .aboutText("About your company will appear here once configured from the dashboard.")
                .build();
    }
}
