package com.businessos.modules.search;

public interface GlobalSearchService {

    /** Keyword search across leads, clients, opportunities, service requests, tickets and invoices */
    GlobalSearchResponse search(String query);

    /** AI-powered answer: searches first, then asks the company's AI provider using the results as context */
    AskResponse ask(AskRequest request);
}
