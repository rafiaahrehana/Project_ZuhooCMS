package com.businessos.modules.website;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/website")
public class WebsiteController {

    private final WebsiteService websiteService;

    private Long companyId(HttpServletRequest request, String subdomain) {
        return websiteService.resolveCompanyId(request.getHeader("Host"), subdomain);
    }

    @GetMapping("/settings")
    public WebsiteSettings settings(HttpServletRequest request,
                                    @RequestParam(required = false) String subdomain) {
        return websiteService.getSettings(companyId(request, subdomain));
    }

    @GetMapping("/nav")
    public List<WebsiteService.NavNode> nav(HttpServletRequest request,
                                            @RequestParam(required = false) String subdomain) {
        return websiteService.getNav(companyId(request, subdomain));
    }

    @GetMapping("/services")
    public List<com.businessos.modules.website.Service> services(HttpServletRequest request,
                                  @RequestParam(required = false) String subdomain,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String q) {
        return websiteService.getServices(companyId(request, subdomain), category, q);
    }

    @GetMapping("/services/{slug}")
    public com.businessos.modules.website.Service service(HttpServletRequest request,
                          @RequestParam(required = false) String subdomain,
                          @PathVariable String slug) {
        return websiteService.getService(companyId(request, subdomain), slug);
    }

    @GetMapping("/blog")
    public List<WebsiteContent> blogs(HttpServletRequest request,
                                @RequestParam(required = false) String subdomain,
                                @RequestParam(required = false) String category) {
        return websiteService.getBlogs(companyId(request, subdomain), category);
    }

    @GetMapping("/blog/{slug}")
    public WebsiteContent blog(HttpServletRequest request,
                        @RequestParam(required = false) String subdomain,
                        @PathVariable String slug) {
        return websiteService.getBlog(companyId(request, subdomain), slug);
    }

    @GetMapping("/testimonials")
    public List<WebsitePerson> testimonials(HttpServletRequest request,
                                         @RequestParam(required = false) String subdomain) {
        return websiteService.getTestimonials(companyId(request, subdomain));
    }

    @GetMapping("/faqs")
    public List<Faq> faqs(HttpServletRequest request,
                          @RequestParam(required = false) String subdomain) {
        return websiteService.getFaqs(companyId(request, subdomain));
    }

    @GetMapping("/team")
    public List<WebsitePerson> team(HttpServletRequest request,
                                 @RequestParam(required = false) String subdomain) {
        return websiteService.getTeam(companyId(request, subdomain));
    }

    @GetMapping("/projects")
    public List<PortalProject> projects(HttpServletRequest request,
                                  @RequestParam(required = false) String subdomain) {
        return websiteService.getProjects(companyId(request, subdomain));
    }

    @GetMapping("/pricing")
    public List<PricingPlan> pricing(HttpServletRequest request,
                                     @RequestParam(required = false) String subdomain) {
        return websiteService.getPricing(companyId(request, subdomain));
    }

    @GetMapping("/stats")
    public List<Stat> stats(HttpServletRequest request,
                            @RequestParam(required = false) String subdomain) {
        return websiteService.getStats(companyId(request, subdomain));
    }

    @GetMapping("/pages/{slug}")
    public WebsiteContent page(HttpServletRequest request,
                        @RequestParam(required = false) String subdomain,
                        @PathVariable String slug) {
        return websiteService.getPage(companyId(request, subdomain), slug);
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> contact(HttpServletRequest request,
                                       @RequestParam(required = false) String subdomain,
                                       @RequestBody WebsiteRequests.ContactRequest body) {
        websiteService.submitContact(companyId(request, subdomain), body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/newsletter")
    public ResponseEntity<Void> newsletter(HttpServletRequest request,
                                          @RequestParam(required = false) String subdomain,
                                          @RequestBody WebsiteRequests.NewsletterRequest body) {
        websiteService.submitNewsletter(companyId(request, subdomain), body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/service-requests")
    public ResponseEntity<Map<String, String>> serviceRequest(HttpServletRequest request,
                                                              @RequestParam(required = false) String subdomain,
                                                              @RequestBody WebsiteRequests.ServiceRequestPayload body) {
        String code = websiteService.createServiceRequest(companyId(request, subdomain), body);
        return ResponseEntity.ok(Map.of("code", code));
    }

    @GetMapping("/service-requests/track/{code}")
    public ServiceRequest track(HttpServletRequest request,
                               @RequestParam(required = false) String subdomain,
                               @PathVariable String code) {
        return websiteService.trackRequest(companyId(request, subdomain), code);
    }
}
