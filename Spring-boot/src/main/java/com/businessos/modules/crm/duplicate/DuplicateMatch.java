package com.businessos.modules.crm.duplicate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A possible duplicate found before creating a new Lead or Client. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateMatch {
    private Long clientId;
    private String clientCompanyName;
    private String matchedOn; // "company name" | "email" | "phone" | "domain"
}
