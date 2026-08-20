package com.businessos.modules.servicedesk.companyservice;

import com.businessos.enums.SubscriptionStatus;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.company.Company;
import com.businessos.auth.user.User;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class ServicePackageServiceImpl implements ServicePackageService {

    private final ServicePackageRepository     packageRepository;
    private final PackageSubscriptionRepository subscriptionRepository;
    private final CompanyServiceRepository     companyServiceRepository;
    private final ClientRepository             clientRepository;
    private final SecurityUtil                 securityUtil;
    private final AuthorizationService         authorizationService;

    // ── Package catalog ───────────────────────────────────────────

    @Override
    @Transactional
    public ServicePackageResponse create(ServicePackageRequest request) {
        Long companyId = requireCompanyId();

        if (packageRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException(
                "A package named '" + request.getName() + "' already exists");
        }

        ServicePackage pkg = ServicePackage.builder()
            .name(request.getName())
            .nameBn(request.getNameBn())
            .description(request.getDescription())
            .descriptionBn(request.getDescriptionBn())
            .iconUrl(request.getIconUrl())
            .packagePrice(request.getPackagePrice())
            .discountPercent(request.getDiscountPercent() != null
                ? request.getDiscountPercent() : BigDecimal.ZERO)
            .billingCycle(request.getBillingCycle())
            .requestQuota(request.getRequestQuota())
            .autoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true)
            .active(true)
            .deliveryDays(request.getDeliveryDays())
            .terms(request.getTerms())
            .featured(request.isFeatured())
            .popular(request.isPopular())
            .company(companyRef(companyId))
            .services(new ArrayList<>())
            .build();

        // Attach services
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<CompanyService> services = resolveServices(
                request.getServiceIds(), companyId);
            pkg.getServices().addAll(services);
        }

        packageRepository.save(pkg);
        
        return ServicePackageMapper.toResponse(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackageResponse getById(Long id) {
        return ServicePackageMapper.toResponse(findPackageInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServicePackageResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SERVICE_PACKAGE_VIEW);
        return packageRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(ServicePackageMapper::toResponse);
    }

    // Deliberately NOT gated by SERVICE_PACKAGE_VIEW here: the controller has no
    // @PreAuthorize on this endpoint on purpose - CLIENT-role users (who have no
    // CustomRole) browse the active package catalog from the client portal to subscribe.
    @Override
    @Transactional(readOnly = true)
    public List<ServicePackageResponse> listActive() {
        return packageRepository.findByCompanyIdAndActiveTrue(requireCompanyId())
            .stream().map(ServicePackageMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ServicePackageResponse update(Long id, ServicePackageRequest request) {
        Long companyId = requireCompanyId();
        ServicePackage pkg = findPackageInTenant(id);

        if (!pkg.getName().equals(request.getName())
                && packageRepository.existsByCompanyIdAndNameAndIdNot(
                    companyId, request.getName(), id)) {
            throw new BadRequestException(
                "A package named '" + request.getName() + "' already exists");
        }

        if (request.getName()            != null) pkg.setName(request.getName());
        if (request.getNameBn()          != null) pkg.setNameBn(request.getNameBn());
        if (request.getDescription()     != null) pkg.setDescription(request.getDescription());
        if (request.getDescriptionBn()   != null) pkg.setDescriptionBn(request.getDescriptionBn());
        if (request.getIconUrl()         != null) pkg.setIconUrl(request.getIconUrl());
        // Always applied (not guarded by != null): the frontend always resends the
        // full form, and a null packagePrice means "switch back to auto-calculated
        // price" - it must be allowed to clear a previously-set manual override.
        pkg.setPackagePrice(request.getPackagePrice());
        pkg.setDiscountPercent(request.getDiscountPercent() != null
            ? request.getDiscountPercent() : BigDecimal.ZERO);
        if (request.getBillingCycle()    != null) pkg.setBillingCycle(request.getBillingCycle());
        if (request.getRequestQuota()    != null) pkg.setRequestQuota(request.getRequestQuota());
        if (request.getAutoRenew()       != null) pkg.setAutoRenew(request.getAutoRenew());
        if (request.getDeliveryDays()    != null) pkg.setDeliveryDays(request.getDeliveryDays());
        if (request.getTerms()           != null) pkg.setTerms(request.getTerms());
        pkg.setFeatured(request.isFeatured());
        pkg.setPopular(request.isPopular());

        if (request.getServiceIds() != null) {
            pkg.getServices().clear();
            if (!request.getServiceIds().isEmpty()) {
                pkg.getServices().addAll(
                    resolveServices(request.getServiceIds(), companyId));
            }
        }

        packageRepository.save(pkg);
        return ServicePackageMapper.toResponse(pkg);
    }

    @Override
    @Transactional
    public ServicePackageResponse toggleActive(Long id) {
        ServicePackage pkg = findPackageInTenant(id);
        pkg.setActive(!pkg.isActive());
        packageRepository.save(pkg);
        
        return ServicePackageMapper.toResponse(pkg);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ServicePackage pkg = findPackageInTenant(id);

        boolean hasActiveSubs = subscriptionRepository
            .existsByServicePackageIdAndStatus(id, SubscriptionStatus.ACTIVE);

        if (hasActiveSubs) {
            throw new BadRequestException(
                "Cannot delete a package that has active subscriptions. " +
                "Deactivate it instead.");
        }

        pkg.softDelete();
        packageRepository.save(pkg);
    }

    // ── Subscriptions ─────────────────────────────────────────────

    @Override
    @Transactional
    public PackageSubscriptionResponse subscribe(SubscribeRequest request) {
        Long companyId = requireCompanyId();

        ServicePackage pkg = findPackageInTenant(request.getPackageId());
        if (!pkg.isActive()) {
            throw new BadRequestException("This package is not currently available");
        }

        // Resolve client — CLIENT role resolves from JWT; admin passes clientId explicitly
        Client client = resolveClient(request.getClientId(), companyId);

        // Enforce one-active-subscription-per-package rule
        subscriptionRepository
            .findByCompanyIdAndClientIdAndServicePackageIdAndStatus(
                companyId, client.getId(), pkg.getId(), SubscriptionStatus.ACTIVE)
            .ifPresent(existing -> {
                throw new BadRequestException(
                    "Client already has an active subscription to this package");
            });

        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = calculateEndDate(startDate, pkg.getBillingCycle());

        boolean autoRenew = request.getAutoRenew() != null
            ? request.getAutoRenew()
            : pkg.isAutoRenew();

        PackageSubscription subscription = PackageSubscription.builder()
            .servicePackage(pkg)
            .client(client)
            .company(companyRef(companyId))
            .status(SubscriptionStatus.PENDING_PAYMENT)
            .billingCycle(pkg.getBillingCycle())
            .startDate(startDate)
            .endDate(endDate)
            .nextBillingDate(endDate)
            .pricePaid(pkg.getEffectivePrice())
            .requestQuota(pkg.getRequestQuota())
            .requestsUsed(0)
            .autoRenew(autoRenew)
            .build();

        subscriptionRepository.save(subscription);
        
        return ServicePackageMapper.toSubscriptionResponse(subscription);
    }

    @Override
    @Transactional
    public PackageSubscriptionResponse activate(Long subscriptionId) {
        return activateForCompany(requireCompanyId(), subscriptionId);
    }

    @Override
    @Transactional
    public PackageSubscriptionResponse activateForCompany(Long companyId, Long subscriptionId) {
        PackageSubscription sub = subscriptionRepository.findByIdAndCompanyId(subscriptionId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (sub.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                "Only PENDING_PAYMENT subscriptions can be activated. " +
                "Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setActivatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);

        
        return ServicePackageMapper.toSubscriptionResponse(sub);
    }

    @Override
    @Transactional
    public PackageSubscriptionResponse suspend(Long subscriptionId, String reason) {
        PackageSubscription sub = findSubscriptionInTenant(subscriptionId);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException(
                "Only ACTIVE subscriptions can be suspended. " +
                "Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.SUSPENDED);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setCancellationReason(reason);
        subscriptionRepository.save(sub);

        
        return ServicePackageMapper.toSubscriptionResponse(sub);
    }

    @Override
    @Transactional
    public PackageSubscriptionResponse cancel(Long subscriptionId, String reason) {
        PackageSubscription sub = findSubscriptionInTenant(subscriptionId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled");
        }
        if (sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new BadRequestException("Cannot cancel an expired subscription");
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setCancellationReason(reason);
        subscriptionRepository.save(sub);

        
        return ServicePackageMapper.toSubscriptionResponse(sub);
    }

    @Override
    @Transactional
    public PackageSubscriptionResponse reactivate(Long subscriptionId) {
        PackageSubscription sub = findSubscriptionInTenant(subscriptionId);

        if (sub.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw new BadRequestException(
                "Only SUSPENDED subscriptions can be reactivated. " +
                "Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelledAt(null);
        sub.setCancellationReason(null);
        subscriptionRepository.save(sub);


        return ServicePackageMapper.toSubscriptionResponse(sub);
    }

    // System entry points (scheduler) — no security/tenant context, mirrors activateForCompany().

    @Override
    @Transactional
    public PackageSubscription renewSubscription(Long subscriptionId) {
        PackageSubscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException(
                "Only ACTIVE subscriptions can be renewed. Current status: " + sub.getStatus());
        }

        LocalDate newStart = sub.getEndDate() != null ? sub.getEndDate() : LocalDate.now();
        LocalDate newEnd = calculateEndDate(newStart, sub.getBillingCycle());

        sub.setStartDate(newStart);
        sub.setEndDate(newEnd);
        sub.setNextBillingDate(newEnd);
        // Re-price from the package's current effective price rather than keeping the
        // old pricePaid — package price changes apply automatically at the client's next renewal.
        sub.setPricePaid(sub.getServicePackage().getEffectivePrice());
        sub.setRequestsUsed(0);
        subscriptionRepository.save(sub);
        return sub;
    }

    @Override
    @Transactional
    public PackageSubscription expireSubscription(Long subscriptionId) {
        PackageSubscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException(
                "Only ACTIVE subscriptions can be expired. Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setCancellationReason("Subscription period ended");
        subscriptionRepository.save(sub);
        return sub;
    }

    @Override
    @Transactional(readOnly = true)
    public PackageSubscriptionResponse getSubscriptionById(Long id) {
        return ServicePackageMapper.toSubscriptionResponse(findSubscriptionInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PackageSubscriptionResponse> listSubscriptions(
            SubscriptionStatus status, Pageable pageable) {
        Long companyId = requireCompanyId();
        Page<PackageSubscription> page = status != null
            ? subscriptionRepository.findByCompanyIdAndStatus(companyId, status, pageable)
            : subscriptionRepository.findByCompanyId(companyId, pageable);
        return page.map(ServicePackageMapper::toSubscriptionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PackageSubscriptionResponse> listMySubscriptions(Pageable pageable) {
        Long companyId = requireCompanyId();
        Client client = clientRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("Client profile not found"));
        return subscriptionRepository
            .findByCompanyIdAndClientId(companyId, client.getId(), pageable)
            .map(ServicePackageMapper::toSubscriptionResponse);
    }

    @Override
    @Transactional
    public PackageSubscription consumeQuota(Long subscriptionId) {
        try {
            PackageSubscription sub = subscriptionRepository.findByIdAndCompanyIdForUpdate(subscriptionId, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Subscription not found: " + subscriptionId));

            if (!sub.isUsable()) {
                throw new BadRequestException(
                    "Subscription is not active or has expired");
            }
            if (!sub.hasRemainingQuota()) {
                // If the package defines an overage rate, allow the request
                // through — UsageBillingService will invoice the extra unit
                // when the request is completed.
                if (sub.getServicePackage().getOverageRate() == null) {
                    throw new BadRequestException(
                        "Request quota exhausted for this subscription. " +
                        "Remaining: 0 of " + sub.getRequestQuota());
                }
            }

            sub.setRequestsUsed(sub.getRequestsUsed() + 1);
            subscriptionRepository.save(sub);
            return sub;
        } catch (org.springframework.dao.PessimisticLockingFailureException ex) {
            throw new BadRequestException("The subscription is currently being updated by another request. Please try again.", ex);
        }
    }

    @Override
    @Transactional
    public PackageSubscription releaseQuota(Long subscriptionId) {
        try {
            PackageSubscription sub = subscriptionRepository.findByIdAndCompanyIdForUpdate(subscriptionId, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Subscription not found: " + subscriptionId));

            if (sub.getRequestsUsed() > 0) {
                sub.setRequestsUsed(sub.getRequestsUsed() - 1);
                subscriptionRepository.save(sub);
            }
            return sub;
        } catch (org.springframework.dao.PessimisticLockingFailureException ex) {
            throw new BadRequestException("The subscription is currently being updated by another request. Please try again.", ex);
        }
    }


    private ServicePackage findPackageInTenant(Long id) {
        return packageRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Service package not found: " + id));
    }

    private PackageSubscription findSubscriptionInTenant(Long id) {
        PackageSubscription sub = subscriptionRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Subscription not found: " + id));
        guardSubscriptionAccess(sub);
        return sub;
    }

    // Staff (COMPANY_OWNER/EMPLOYEE) can reach any subscription in their company -
    // getById/cancel are intentionally open to CLIENT too, but a client may only
    // reach their own subscription, not one belonging to another client.
    private void guardSubscriptionAccess(PackageSubscription sub) {
        User user = securityUtil.getCurrentUser();
        if (user == null || user.getRole() == null || !user.getRole().name().equals("CLIENT")) return;

        boolean isOwner = sub.getClient() != null
                && sub.getClient().getUser() != null
                && sub.getClient().getUser().getId().equals(user.getId());

        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to access this subscription");
        }
    }

    private Client resolveClient(Long requestedClientId, Long companyId) {
        User currentUser = securityUtil.getCurrentUser();
        boolean isClient = currentUser.getRole().name().equals("CLIENT");

        if (isClient) {
            return clientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Client profile not found"));
        }

        if (requestedClientId == null) {
            throw new BadRequestException(
                "clientId is required when subscribing on behalf of a client");
        }
        return clientRepository.findByIdAndCompanyId(requestedClientId, companyId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Client not found: " + requestedClientId));
    }


    private List<CompanyService> resolveServices(List<Long> ids, Long companyId) {
        List<CompanyService> result = new ArrayList<>();
        for (Long serviceId : ids) {
            result.add(
                companyServiceRepository.findByIdAndCompanyId(serviceId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found or does not belong to your company: " + serviceId))
            );
        }
        return result;
    }

     //Calculates the subscription end date from start date and billing cycle.
     //ONE_TIME returns null — the subscription never expires automatically.

    private LocalDate calculateEndDate(LocalDate start, com.businessos.enums.BillingCycle cycle) {
        return cycle.addTo(start);
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
