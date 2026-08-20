package com.businessos.modules.servicedesk.document;

import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final SecurityUtil securityUtil;
    private final RequiredDocumentRepository requiredDocumentRepository;

    @Override
    public DocumentResponse upload(Long serviceRequestId, CreateDocumentRequest request) {
        ServiceRequest sr = findRequestInTenant(serviceRequestId);
        Long companyId = requireCompanyId();

        Company companyRef = new Company();
        companyRef.setId(companyId);

        // Format validation based on RequiredDocument rules
        requiredDocumentRepository.findByCompanyIdAndServiceIdOrderBySortOrderAsc(companyId, sr.getCompanyService().getId())
            .stream()
            .filter(rd -> request.getLabel() != null && !request.getLabel().isBlank()
                && rd.getDocName().trim().equalsIgnoreCase(request.getLabel().trim()))
            .findFirst()
            .ifPresent(rd -> {
                String allowed = rd.getAllowedFormats();
                if (allowed != null && !allowed.isBlank()) {
                    String fileName = request.getFileName().toLowerCase();
                    boolean matches = java.util.Arrays.stream(allowed.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .anyMatch(ext -> fileName.endsWith("." + ext) || (request.getFileType() != null && request.getFileType().toLowerCase().contains(ext)));
                    if (!matches) {
                        throw new BadRequestException("Invalid file format. Allowed formats: " + allowed);
                    }
                }
            });

        Document doc = new Document();
        doc.setFileName(request.getFileName());
        doc.setFileUrl(request.getFileUrl());
        doc.setFileType(request.getFileType());
        doc.setFileSizeBytes(request.getFileSizeBytes());
        doc.setLabel(request.getLabel());
        doc.setNotes(request.getNotes());
        doc.setServiceRequest(sr);
        doc.setUploadedBy(securityUtil.getCurrentUser());
        doc.setCompany(companyRef);

        return DocumentMapper.toResponse(documentRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listForRequest(Long serviceRequestId) {
        findRequestInTenant(serviceRequestId);
        return documentRepository.findByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId)
                .stream()
                .map(DocumentMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long serviceRequestId, Long documentId) {
        findRequestInTenant(serviceRequestId);
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (doc.getServiceRequest() == null || !doc.getServiceRequest().getId().equals(serviceRequestId)) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        doc.softDelete();
        documentRepository.save(doc);
    }

    private ServiceRequest findRequestInTenant(Long serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findByIdAndCompanyId(serviceRequestId, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + serviceRequestId));
        guardAccess(sr);
        return sr;
    }

    // Mirrors ServiceRequestServiceImpl.guardAccess(): staff can reach any request
    // in their company, but a CLIENT may only touch documents on their own requests.
    private void guardAccess(ServiceRequest sr) {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null || !user.getRole().name().equals("CLIENT")) return;

        boolean isOwner = sr.getClient() != null
                && sr.getClient().getUser() != null
                && sr.getClient().getUser().getId().equals(user.getId());

        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to access this service request");
        }
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
