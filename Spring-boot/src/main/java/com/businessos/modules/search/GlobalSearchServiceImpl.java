package com.businessos.modules.search;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.SearchAnswerPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.lead.Lead;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.modules.crm.opportunity.Opportunity;
import com.businessos.modules.crm.opportunity.OpportunityRepository;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestRepository;
import com.businessos.modules.support.ticket.SupportTicket;
import com.businessos.modules.support.ticket.SupportTicketRepository;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.modules.finance.invoice.Refund;
import com.businessos.modules.finance.invoice.RefundRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;

import org.springframework.data.domain.Page;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private static final int PER_TYPE_LIMIT = 10;

    private final LeadRepository leadRepository;
    private final ClientRepository clientRepository;
    private final OpportunityRepository opportunityRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ClientInvoiceRepository invoiceRepository;
    private final RefundRepository refundRepository;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;
    private final SecurityUtil securityUtil;

    @Override
    public GlobalSearchResponse search(String query) {
        Long companyId = requireCompanyId();
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters");
        }
        String keyword = escapeLikeKeyword(query.trim());
        Pageable top = PageRequest.of(0, PER_TYPE_LIMIT);

        GlobalSearchResponse response = new GlobalSearchResponse();
        response.setQuery(keyword);
        List<SearchResultItem> results = response.getResults();
        long totalMatches = 0;

        Page<Lead> leadPage = leadRepository.searchLeads(companyId, keyword, top);
        totalMatches += leadPage.getTotalElements();
        leadPage.forEach(lead -> results.add(new SearchResultItem("LEAD", lead.getId(),
                lead.getContactName(),
                (lead.getCompanyName() != null ? lead.getCompanyName() + " · " : "") + lead.getStatus(),
                "/crm/leads")));

        Page<Client> clientPage = clientRepository.searchClients(companyId, keyword, top);
        totalMatches += clientPage.getTotalElements();
        clientPage.forEach(client -> results.add(new SearchResultItem("CLIENT", client.getId(),
                client.getClientCompanyName() != null ? client.getClientCompanyName()
                        : client.getUser().getFirstName() + " " + client.getUser().getLastName(),
                client.getIndustry() != null ? client.getIndustry() : "Account",
                "/crm/clients/" + client.getId())));

        Page<Opportunity> oppPage = opportunityRepository.searchOpportunities(companyId, keyword, top);
        totalMatches += oppPage.getTotalElements();
        oppPage.forEach(opp -> results.add(new SearchResultItem("OPPORTUNITY", opp.getId(),
                opp.getName(),
                opp.getStage() + (opp.getAmount() != null ? " · " + opp.getAmount() : ""),
                "/crm/pipeline")));

        Page<ServiceRequest> srPage = serviceRequestRepository.findByCompanyIdAndTitleContainingIgnoreCaseAndDeletedFalse(companyId, keyword, top);
        totalMatches += srPage.getTotalElements();
        srPage.forEach(sr -> results.add(new SearchResultItem("SERVICE_REQUEST", sr.getId(),
                sr.getTitle(), String.valueOf(sr.getStatus()),
                "/servicedesk/requests/" + sr.getId())));

        Page<SupportTicket> ticketPage = supportTicketRepository.findByCompanyIdAndTitleContainingIgnoreCase(companyId, keyword, top);
        totalMatches += ticketPage.getTotalElements();
        ticketPage.forEach(ticket -> results.add(new SearchResultItem("TICKET", ticket.getId(),
                ticket.getTitle(),
                ticket.getTicketNumber() + " · " + ticket.getStatus(),
                "/support/tickets")));

        Page<ClientInvoice> invoicePage = invoiceRepository.findByCompanyIdAndInvoiceNumberContainingIgnoreCase(companyId, keyword, top);
        totalMatches += invoicePage.getTotalElements();
        invoicePage.forEach(invoice -> results.add(new SearchResultItem("INVOICE", invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus() + " · " + invoice.getTotalAmount(),
                "/finance/invoices")));

        Page<Refund> refundPage = refundRepository.searchRefunds(companyId, keyword, top);
        totalMatches += refundPage.getTotalElements();
        refundPage.forEach(refund -> results.add(new SearchResultItem("REFUND", refund.getId(),
                "Refund · " + refund.getClientInvoice().getInvoiceNumber(),
                refund.getStatus() + " · " + refund.getRequestedAmount(),
                "/finance/refunds")));

        response.setTotalMatches(totalMatches);
        return response;
    }

    // NOT_SUPPORTED overrides this class's @Transactional so the provider call
    // isn't inside a transaction - see AiTransactionBoundary. The old
    // timeout = 30 existed to bound that same transaction and is now moot,
    // since nothing transactional spans the AI call any more.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AskResponse ask(AskRequest request) {
        // search() is a self-invocation, so it gets no transaction of its own -
        // running it inside load() keeps its many repository reads in a single
        // transaction, as they were before, and commits them before the AI call.
        GlobalSearchResponse searchResults = aiTx.load(() -> search(request.getQuestion()));

        if (searchResults.getResults().isEmpty()) {
            AskResponse response = new AskResponse();
            response.setQuestion(request.getQuestion());
            response.setAnswer("No matching records found. Try a different search term.");
            response.setSources(List.of());
            return response;
        }

        StringBuilder context = new StringBuilder();
        for (SearchResultItem item : searchResults.getResults()) {
            context.append("- [").append(item.getType()).append("] ")
                    .append(item.getTitle()).append(" (").append(item.getSubtitle()).append(")\n");
        }

        String prompt = SearchAnswerPromptBuilder.builder()
                .setQuestion(request.getQuestion())
                .setContext(context.isEmpty() ? null : context.toString())
                .build();

        String answer = aiService.generateRaw(AiFeature.SEARCH_ANSWER, prompt);

        AskResponse response = new AskResponse();
        response.setQuestion(request.getQuestion());
        response.setAnswer(answer);
        response.setSources(searchResults.getResults());
        return response;
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for current platformuser");
        }
        return companyId;
    }

    /**
     * '!' is the LIKE escape character in every search query (ESCAPE '!').
     * The previous backslash scheme silently broke the queries: in HQL string
     * literals a backslash escapes the closing quote, so ESCAPE '\' swallowed
     * the rest of the OR-chain into the literal and every search matched
     * nothing.
     */
    private String escapeLikeKeyword(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
