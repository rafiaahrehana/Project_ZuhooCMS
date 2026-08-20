package com.businessos.modules.servicedesk.companyservice;

import com.businessos.modules.company.Company;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.enums.ServicePriceType;
import com.businessos.enums.ServiceRequestPriority;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategoryRepository;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class CompanyServiceServiceImpl implements CompanyServiceService {

    // FIX: was HubServiceRepository — renamed to CompanyServiceRepository
    private final CompanyServiceRepository   companyServiceRepository;
    private final ServiceCategoryRepository  categoryRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final com.businessos.modules.servicedesk.servicetemplate.ServiceTemplateRepository serviceTemplateRepository;
    private final SecurityUtil               securityUtil;
    private final AuthorizationService       authorizationService;

    @Override
    @Transactional
    public CompanyServiceResponse create(CompanyServiceRequest request) {
        authorizationService.checkPermission(PermissionCode.SERVICE_CATALOG_CREATE);
        Long companyId = requireCompanyId();

        ServiceCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndCompanyId(request.getCategoryId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Service category not found: " + request.getCategoryId()));
        }

        WorkflowTemplate workflow = null;
        if (request.getWorkflowTemplateId() != null) {
            workflow = templateRepository
                .findByIdAndCompanyId(request.getWorkflowTemplateId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Workflow template not found: " + request.getWorkflowTemplateId()));
        }

        com.businessos.modules.servicedesk.servicetemplate.ServiceTemplate serviceTemplate = null;
        if (request.getServiceTemplateId() != null) {
            serviceTemplate = serviceTemplateRepository.findById(request.getServiceTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Service template not found: " + request.getServiceTemplateId()));
        }

        CompanyService service = CompanyService.builder()
            .name(request.getName())
            .nameBn(request.getNameBn())
            .description(request.getDescription())
            .descriptionBn(request.getDescriptionBn())
            .price(request.getPrice())
            .priceType(request.getPriceType() != null ? request.getPriceType() : ServicePriceType.FIXED)
            .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
            .estimatedDays(request.getEstimatedDays())
            .defaultPriority(request.getDefaultPriority() != null
                ? request.getDefaultPriority() : ServiceRequestPriority.NORMAL)
            .company(companyRef(companyId))
            .category(category)
            .workflowTemplate(workflow)
            .serviceTemplate(serviceTemplate)
            .featured(request.isFeatured())
            .remote(request.isRemote())
            .onSite(request.isOnSite())
            .online(request.isOnline())
            .maximumOrders(request.getMaximumOrders())
            .autoApproval(request.isAutoApproval())
            .requiresQuotation(request.isRequiresQuotation())
            .requiresDocuments(request.isRequiresDocuments())
            .supportsCustomWorkflow(request.isSupportsCustomWorkflow())
            .aiAssisted(request.isAiAssisted())
            .visibility(request.getVisibility() != null ? request.getVisibility() : com.businessos.enums.ServiceVisibility.DRAFT)
            .build();

        companyServiceRepository.save(service);
        
        return CompanyServiceMapper.toResponse(service);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyServiceResponse getById(Long id) {
        return CompanyServiceMapper.toResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyServiceResponse> listAll(Long categoryId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SERVICE_CATALOG_VIEW);
        Long companyId = requireCompanyId();
        Page<CompanyService> page = categoryId != null
            ? companyServiceRepository.findByCompanyIdAndCategoryId(companyId, categoryId, pageable)
            : companyServiceRepository.findByCompanyId(companyId, pageable);
        return page.map(CompanyServiceMapper::toResponse);
    }

    // Deliberately NOT gated by SERVICE_CATALOG_VIEW here: this is the active-service
    // picker consumed by Requests (creating a service request) and Packages (attaching
    // services to a package) - users with SERVICE_REQUEST_VIEW/SERVICE_PACKAGE_VIEW but
    // not SERVICE_CATALOG_VIEW still need it to populate that dropdown.
    @Override
    @Transactional(readOnly = true)
    public List<CompanyServiceResponse> listActive() {
        return companyServiceRepository.findByCompanyIdAndActiveTrue(requireCompanyId())
            .stream().map(CompanyServiceMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CompanyServiceResponse update(Long id, CompanyServiceRequest request) {
        authorizationService.checkPermission(PermissionCode.SERVICE_CATALOG_UPDATE);
        Long companyId = requireCompanyId();
        CompanyService service = findInTenant(id);

        if (request.getName()            != null) service.setName(request.getName());
        if (request.getNameBn()          != null) service.setNameBn(request.getNameBn());
        if (request.getDescription()     != null) service.setDescription(request.getDescription());
        if (request.getDescriptionBn()   != null) service.setDescriptionBn(request.getDescriptionBn());
        if (request.getPrice()           != null) service.setPrice(request.getPrice());
        if (request.getPriceType()       != null) service.setPriceType(request.getPriceType());
        if (request.getEstimatedDays()   != null) service.setEstimatedDays(request.getEstimatedDays());
        if (request.getDefaultPriority() != null) service.setDefaultPriority(request.getDefaultPriority());
        if (request.getCurrency()        != null) service.setCurrency(request.getCurrency());
        service.setFeatured(request.isFeatured());
        service.setRemote(request.isRemote());
        service.setOnSite(request.isOnSite());
        service.setOnline(request.isOnline());
        if (request.getMaximumOrders()   != null) service.setMaximumOrders(request.getMaximumOrders());
        service.setAutoApproval(request.isAutoApproval());
        service.setRequiresQuotation(request.isRequiresQuotation());
        service.setRequiresDocuments(request.isRequiresDocuments());
        service.setSupportsCustomWorkflow(request.isSupportsCustomWorkflow());
        service.setAiAssisted(request.isAiAssisted());
        if (request.getVisibility()      != null) service.setVisibility(request.getVisibility());

        if (request.getCategoryId() != null) {
            service.setCategory(categoryRepository.findByIdAndCompanyId(request.getCategoryId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Service category not found: " + request.getCategoryId())));
        }
        if (request.getWorkflowTemplateId() != null) {
            service.setWorkflowTemplate(
                templateRepository.findByIdAndCompanyId(request.getWorkflowTemplateId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Workflow template not found: " + request.getWorkflowTemplateId())));
        }

        return CompanyServiceMapper.toResponse(service);
    }

    @Override
    @Transactional
    public CompanyServiceResponse toggleActive(Long id) {
        authorizationService.checkPermission(PermissionCode.SERVICE_CATALOG_UPDATE);
        CompanyService service = findInTenant(id);
        service.setActive(!service.isActive());
        return CompanyServiceMapper.toResponse(service);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SERVICE_CATALOG_DELETE);
        CompanyService service = findInTenant(id);
        service.softDelete();
        companyServiceRepository.save(service);
    }

    private CompanyService findInTenant(Long id) {
        return companyServiceRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
