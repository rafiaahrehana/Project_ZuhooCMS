package com.businessos.modules.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** One hit in the global cross-module search. */
@Getter @Setter @AllArgsConstructor
public class SearchResultItem {

    /** LEAD, CLIENT, OPPORTUNITY, SERVICE_REQUEST, TICKET, INVOICE */
    private String type;

    private Long id;

    private String title;

    /** Short secondary line — status, owner, amount, etc. */
    private String subtitle;

    /** Frontend route to act on the result, e.g. /crm/clients/12 */
    private String link;
}
