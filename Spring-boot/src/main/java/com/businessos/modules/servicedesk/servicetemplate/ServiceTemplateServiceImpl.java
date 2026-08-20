package com.businessos.modules.servicedesk.servicetemplate;

import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategoryRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceTemplateServiceImpl implements ServiceTemplateService {

    private final ServiceTemplateRepository templateRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public ServiceTemplateResponse create(ServiceTemplateRequest request) {
        authorizationService.checkPermission(PermissionCode.SERVICE_TEMPLATE_CREATE);
        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        ServiceTemplate template = ServiceTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .defaultPrice(request.getDefaultPrice())
                .estimatedDays(request.getEstimatedDays())
                .iconUrl(request.getIconUrl())
                .active(request.isActive())
                .build();

        if (request.getFormFields() != null) {
            template.getFormFields().addAll(request.getFormFields().stream().map(f -> 
                TemplateFormField.builder()
                    .serviceTemplate(template)
                    .label(f.getLabel())
                    .fieldType(f.getFieldType())
                    .required(f.isRequired())
                    .validationRules(f.getValidationRules())
                    .sortOrder(f.getSortOrder())
                    .build()
            ).collect(Collectors.toList()));
        }

        if (request.getRequiredDocuments() != null) {
            template.getRequiredDocuments().addAll(request.getRequiredDocuments().stream().map(d -> 
                TemplateRequiredDocument.builder()
                    .serviceTemplate(template)
                    .docName(d.getDocName())
                    .description(d.getDescription())
                    .mandatory(d.isMandatory())
                    .maxAgeDays(d.getMaxAgeDays())
                    .allowedFormats(d.getAllowedFormats())
                    .sortOrder(d.getSortOrder())
                    .build()
            ).collect(Collectors.toList()));
        }

        if (request.getWorkflowStages() != null) {
            template.getWorkflowStages().addAll(request.getWorkflowStages().stream().map(s -> 
                TemplateWorkflowStage.builder()
                    .serviceTemplate(template)
                    .stageName(s.getStageName())
                    .stageDescription(s.getStageDescription())
                    .stageOrder(s.getStageOrder())
                    .requiresClientAction(s.isRequiresClientAction())
                    .requiresPayment(s.isRequiresPayment())
                    .isFinalStage(s.isFinalStage())
                    .build()
            ).collect(Collectors.toList()));
        }

        templateRepository.save(template);
        return ServiceTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional
    public ServiceTemplateResponse update(Long id, ServiceTemplateRequest request) {
        authorizationService.checkPermission(PermissionCode.SERVICE_TEMPLATE_UPDATE);
        ServiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getDefaultPrice() != null) template.setDefaultPrice(request.getDefaultPrice());
        if (request.getEstimatedDays() != null) template.setEstimatedDays(request.getEstimatedDays());
        if (request.getIconUrl() != null) template.setIconUrl(request.getIconUrl());
        template.setActive(request.isActive());

        if (request.getCategoryId() != null && !request.getCategoryId().equals(template.getCategory().getId())) {
            ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            template.setCategory(category);
        }

        templateRepository.save(template);
        return ServiceTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceTemplateResponse getById(Long id) {
        return ServiceTemplateMapper.toResponse(templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceTemplateResponse> listAll(boolean activeOnly, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SERVICE_TEMPLATE_VIEW);
        if (activeOnly) {
            return templateRepository.findByActive(true, pageable).map(ServiceTemplateMapper::toResponse);
        }
        return templateRepository.findAll(pageable).map(ServiceTemplateMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTemplateResponse> listByCategory(Long categoryId) {
        return templateRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(ServiceTemplateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SERVICE_TEMPLATE_DELETE);
        ServiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        template.softDelete();
    }
}
