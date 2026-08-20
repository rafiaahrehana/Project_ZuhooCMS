package com.businessos.shared.payment.gateway;

import com.businessos.auth.role.enums.Role;
import com.businessos.auth.user.User;
import com.businessos.enums.WalletTransactionType;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.company.CompanyService;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.modules.finance.invoice.ClientInvoiceService;
import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.modules.servicedesk.companyservice.PackageSubscriptionRepository;
import com.businessos.modules.servicedesk.companyservice.ServicePackageService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.payment.wallet.WalletService;
import com.businessos.shared.subscription.SubscriptionPlanDefinition;
import com.businessos.shared.subscription.SubscriptionPlanDefinitionRepository;
import com.businessos.shared.webhook.WebhookLog;
import com.businessos.shared.webhook.WebhookLogRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("deprecation")
public class SslCommerzServiceImpl implements SslCommerzService {

    private final SslCommerzProperties properties;
    private final PaymentGatewayTransactionRepository transactionRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final ClientInvoiceService invoiceService;
    private final ClientInvoiceRepository invoiceRepository;
    private final WalletService walletService;
    private final ServicePackageService packageService;
    private final PackageSubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final SubscriptionPlanDefinitionRepository subscriptionPlanDefinitionRepository;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    @Value("${app.backend-url:http://localhost:8085}")
    private String backendUrl;

    @Override
    @Transactional
    public String initiate(GatewayPurpose purpose, Long targetId, BigDecimal amount) {
        if (properties.getStoreId() == null || properties.getStoreId().isBlank()) {
            throw new BadRequestException("Online payments are not configured (sslcommerz.store-id missing)");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context");
        }
        User user = securityUtil.getCurrentUser();
        validateTarget(purpose, targetId, companyId, user);

        // The client-supplied amount is only trustworthy for purposes where it's
        // derived from a record the server already owns (invoice/subscription total,
        // wallet top-up choice). A platform plan upgrade must never be priced by the
        // caller - always charge the catalog price for the requested plan.
        BigDecimal effectiveAmount = purpose == GatewayPurpose.PLATFORM_SUBSCRIPTION
            ? decodePlan(targetId).getPrice()
            : amount;
        if (effectiveAmount == null || effectiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        String tranId = "BOS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        PaymentGatewayTransaction tx = PaymentGatewayTransaction.builder()
            .tranId(tranId)
            .purpose(purpose)
            .targetId(targetId)
            .companyId(companyId)
            .initiatedByUserId(user != null ? user.getId() : null)
            .amount(effectiveAmount)
            .currency(properties.getCurrency())
            .status(GatewayTransactionStatus.INITIATED)
            .initiatedAt(LocalDateTime.now())
            .build();
        transactionRepository.save(tx);

        String cb = backendUrl + "/api/payments/sslcommerz/callback";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", properties.getStoreId());
        form.add("store_passwd", properties.getStorePassword());
        form.add("total_amount", effectiveAmount.toPlainString());
        form.add("currency", properties.getCurrency());
        form.add("tran_id", tranId);
        form.add("success_url", cb + "/success");
        form.add("fail_url", cb + "/fail");
        form.add("cancel_url", cb + "/cancel");
        form.add("ipn_url", backendUrl + "/api/payments/sslcommerz/ipn");
        form.add("product_category", purpose.name());
        form.add("product_name", purpose.name() + (targetId != null ? "-" + targetId : ""));
        form.add("product_profile", "non-physical-goods");
        form.add("shipping_method", "NO");
        // SSLCommerz requires customer fields; use what we have
        form.add("cus_name", user != null ? user.getEmail() : "customer");
        form.add("cus_email", user != null ? user.getEmail() : "customer@example.com");
        form.add("cus_add1", "N/A");
        form.add("cus_city", "N/A");
        form.add("cus_country", "Bangladesh");
        form.add("cus_phone", "N/A");

        try {
            String body = restClient.post()
                .uri(properties.initiateUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
            JsonNode json = objectMapper.readTree(body);
            if (!"SUCCESS".equalsIgnoreCase(json.path("status").asText())) {
                log.error("SSLCommerz initiate failed: {}", json.path("failedreason").asText());
                throw new BadRequestException("Payment gateway rejected the request: "
                    + json.path("failedreason").asText("unknown reason"));
            }
            return json.path("GatewayPageURL").asText();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("SSLCommerz initiate error", e);
            throw new BadRequestException("Could not reach the payment gateway. Try again later.");
        }
    }

    /**
     * Staff (COMPANY_OWNER/EMPLOYEE) may initiate payment against any target in
     * their own company - tenant scoping via companyId is enough. A CLIENT may
     * only pay their own invoice/subscription; without this, any authenticated
     * client could pass an arbitrary targetId belonging to another client in the
     * same tenant and activate/settle it with their own card.
     */
    private void validateTarget(GatewayPurpose purpose, Long targetId, Long companyId, User user) {
        boolean isClient = user != null && user.getRole() == Role.CLIENT;

        switch (purpose) {
            case INVOICE -> {
                ClientInvoice invoice = invoiceRepository.findByIdAndCompanyId(targetId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + targetId));
                if (isClient && !ownedByUser(invoice.getClient(), user)) {
                    throw new ForbiddenException("This invoice does not belong to you");
                }
            }
            case PACKAGE_SUBSCRIPTION -> {
                PackageSubscription subscription = subscriptionRepository
                    .findByIdAndCompanyId(targetId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + targetId));
                if (isClient && !ownedByUser(subscription.getClient(), user)) {
                    throw new ForbiddenException("This subscription does not belong to you");
                }
            }
            case WALLET_TOPUP -> {
                // Credits the company's shared wallet, not a specific client's record -
                // no per-target ownership to check beyond the companyId tenant scope.
            }
            case PLATFORM_SUBSCRIPTION -> {
                if (user == null || user.getRole() != Role.COMPANY_OWNER) {
                    throw new ForbiddenException("Only the company owner can change the subscription plan");
                }
                SubscriptionPlanDefinition targetPlan = decodePlan(targetId);
                Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                BigDecimal currentPrice = subscriptionPlanDefinitionRepository
                    .findByCode(company.getSubscriptionPlan())
                    .map(SubscriptionPlanDefinition::getPrice)
                    .orElse(BigDecimal.ZERO);
                if (targetPlan.getPrice().compareTo(currentPrice) <= 0) {
                    throw new BadRequestException("You can only upgrade to a higher-priced plan than your current one");
                }
            }
        }
    }

    /** InitiateGatewayPaymentRequest.targetId is the SubscriptionPlanDefinition's own id. */
    private SubscriptionPlanDefinition decodePlan(Long targetId) {
        if (targetId == null) {
            throw new BadRequestException("Invalid subscription plan");
        }
        return subscriptionPlanDefinitionRepository.findById(targetId)
            .filter(SubscriptionPlanDefinition::isActive)
            .orElseThrow(() -> new BadRequestException("Invalid subscription plan"));
    }

    private boolean ownedByUser(com.businessos.modules.crm.client.Client client, User user) {
        return client != null && client.getUser() != null && client.getUser().getId().equals(user.getId());
    }

    @Override
    @Transactional
    public GatewayTransactionStatus handleSuccess(Map<String, String> params) {
        String tranId = params.get("tran_id");
        String valId = params.get("val_id");
        logWebhook(params);

        PaymentGatewayTransaction tx = transactionRepository.findByTranIdForUpdate(tranId)
            .orElseThrow(() -> new ResourceNotFoundException("Unknown transaction: " + tranId));

        // Idempotency: success callback AND IPN both land here
        if (tx.getStatus() == GatewayTransactionStatus.SUCCESS) {
            return GatewayTransactionStatus.SUCCESS;
        }

        if (!validateWithGateway(tx, valId)) {
            tx.setStatus(GatewayTransactionStatus.VALIDATION_FAILED);
            transactionRepository.save(tx);
            return GatewayTransactionStatus.VALIDATION_FAILED;
        }

        // Apply to the domain object using the companyId captured at initiate
        // time - callbacks have no security context.
        switch (tx.getPurpose()) {
            case INVOICE -> invoiceService.recordPaymentForCompany(
                tx.getCompanyId(), tx.getTargetId(), tx.getAmount());
            case WALLET_TOPUP -> walletService.credit(
                "COMPANY", tx.getCompanyId(), tx.getAmount(),
                WalletTransactionType.CREDIT, tx.getTranId(), "SSLCommerz top-up");
            case PACKAGE_SUBSCRIPTION -> packageService.activateForCompany(
                tx.getCompanyId(), tx.getTargetId());
            case PLATFORM_SUBSCRIPTION -> companyService.applyPaidPlanUpgrade(
                tx.getCompanyId(), decodePlan(tx.getTargetId()), tx.getAmount(),
                tx.getTranId(), tx.getInitiatedByUserId());
        }

        tx.setStatus(GatewayTransactionStatus.SUCCESS);
        tx.setValId(valId);
        tx.setBankTranId(params.get("bank_tran_id"));
        tx.setCardType(params.get("card_type"));
        tx.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(tx);
        return GatewayTransactionStatus.SUCCESS;
    }

    /** Server-side validation - never trust the browser redirect alone. */
    private boolean validateWithGateway(PaymentGatewayTransaction tx, String valId) {
        if (valId == null || valId.isBlank()) return false;
        try {
            String url = properties.validationUrl()
                + "?val_id=" + valId
                + "&store_id=" + properties.getStoreId()
                + "&store_passwd=" + properties.getStorePassword()
                + "&format=json";
            String body = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode json = objectMapper.readTree(body);
            String status = json.path("status").asText();
            boolean statusOk = "VALID".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status);
            boolean amountOk = tx.getAmount()
                .compareTo(new BigDecimal(json.path("amount").asText("0"))) == 0;
            boolean currencyOk = tx.getCurrency().equalsIgnoreCase(json.path("currency").asText(""));
            if (!statusOk || !amountOk || !currencyOk) {
                log.warn("SSLCommerz validation mismatch for {}: status={} amountOk={} currencyOk={}",
                    tx.getTranId(), status, amountOk, currencyOk);
            }
            return statusOk && amountOk && currencyOk;
        } catch (Exception e) {
            log.error("SSLCommerz validation error for {}", tx.getTranId(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void markFailed(String tranId) {
        transactionRepository.findByTranId(tranId).ifPresent(tx -> {
            if (tx.getStatus() == GatewayTransactionStatus.INITIATED) {
                tx.setStatus(GatewayTransactionStatus.FAILED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(tx);
            }
        });
    }

    @Override
    @Transactional
    public void markCancelled(String tranId) {
        transactionRepository.findByTranId(tranId).ifPresent(tx -> {
            if (tx.getStatus() == GatewayTransactionStatus.INITIATED) {
                tx.setStatus(GatewayTransactionStatus.CANCELLED);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(tx);
            }
        });
    }

    private void logWebhook(Map<String, String> params) {
        try {
            WebhookLog wl = new WebhookLog();
            wl.setProvider("SSLCOMMERZ");
            wl.setEventType("PAYMENT_CALLBACK");
            wl.setTransactionId(params.get("tran_id"));
            wl.setPayload(objectMapper.writeValueAsString(params));
            webhookLogRepository.save(wl);
        } catch (Exception e) {
            log.warn("Could not persist webhook log: {}", e.getMessage());
        }
    }
}
