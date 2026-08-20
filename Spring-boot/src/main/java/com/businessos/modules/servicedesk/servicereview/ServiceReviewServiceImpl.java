package com.businessos.modules.servicedesk.servicereview;

import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.enums.ServiceRequestStatus;
import com.businessos.modules.company.Company;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor

public class ServiceReviewServiceImpl implements ServiceReviewService {

    private final ServiceReviewRepository reviewRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ClientRepository clientRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public ServiceReviewResponse submitOrUpdate(ServiceReviewRequest request) {
        Long companyId = requireCompanyId();

        /*
         * BUG-FIX: was serviceRequestRepository.findById() with no companyId check.
         * A client from Company A could submit a review referencing Company B's request.
         * Fixed to findByIdAndCompanyId() to enforce tenant isolation.
         */
        ServiceRequest sr = serviceRequestRepository
                .findByIdAndCompanyId(request.getServiceRequestId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found: " + request.getServiceRequestId()));

        if (sr.getStatus() != ServiceRequestStatus.COMPLETED)
            throw new BadRequestException("Reviews can only be submitted for completed requests");

        Client client = clientRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Only clients can submit reviews"));

        if (!sr.getClient().getId().equals(client.getId()))
            throw new BadRequestException("You can only review your own service requests");

        ServiceReview review = reviewRepository
                .findByServiceRequestIdAndClientId(sr.getId(), client.getId())
                .orElse(null);

        if (review != null) {
            // Use updatedAt if set, otherwise fall back to createdAt, for the edit window
            LocalDateTime editBase = review.getUpdatedAt() != null
                    ? review.getUpdatedAt() : review.getCreatedAt();
            if (editBase.isBefore(LocalDateTime.now().minusDays(7)))
                throw new BadRequestException("Reviews can no longer be edited after 7 days");

            review.setRating(request.getRating());
            review.setComment(request.getComment());
        } else {
            review = ServiceReview.builder()
                    .serviceRequest(sr)
                    .hubService(sr.getCompanyService())
                    .client(client)
                    .company(companyRef(companyId))
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .published(true)
                    .publishedAt(LocalDateTime.now())
                    .build();
        }

        reviewRepository.save(review);
        return ServiceReviewMapper.toServiceReviewResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceReviewResponse getById(Long id) {
        Long companyId = requireCompanyId();
        /*
         * BUG-FIX: was reviewRepository.findById(id) with no tenant check.
         * Any platformuser knowing a review ID could read another company's review.
         * Fixed to findByIdAndCompanyId().
         */
        return ServiceReviewMapper.toServiceReviewResponse(
                reviewRepository.findByIdAndCompanyId(id, companyId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Service review not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceReviewResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.REVIEW_VIEW);
        Long companyId = requireCompanyId();
        /*
         * BUG-FIX: was reviewRepository.findAll(pageable) — returned ALL tenants' reviews.
         * This is a critical data leak. Fixed to findByCompanyId().
         */
        return reviewRepository.findByCompanyId(companyId, pageable)
                .map(ServiceReviewMapper::toServiceReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceReviewResponse> listByService(Long hubServiceId, Pageable pageable) {
        return reviewRepository.findByHubServiceId(hubServiceId, pageable)
                .map(ServiceReviewMapper::toServiceReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingByService(Long hubServiceId) {
        return reviewRepository.findAverageRatingByServiceId(hubServiceId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingByCompany() {
        return reviewRepository.findAverageRatingByCompanyId(requireCompanyId()).orElse(null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.REVIEW_DELETE);
        Long companyId = requireCompanyId();
        /*
         * BUG-FIX 1: was reviewRepository.findById(id) — no tenant check. Fixed to
         * findByIdAndCompanyId() so admins can only delete their own company's reviews.
         *
         * BUG-FIX 2: was 'if (review.isPublished()) throw' — this prevented admins from
         * ever deleting published reviews, which is the opposite of the intended behaviour
         * (the controller restricts this endpoint to ADMIN role for exactly this purpose).
         * Guard removed: the @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')") on the controller endpoint
         * is the correct gate. An admin should be able to delete any review for moderation.
         */
        ServiceReview review = reviewRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service review not found: " + id));
        review.softDelete();
        reviewRepository.save(review);
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
