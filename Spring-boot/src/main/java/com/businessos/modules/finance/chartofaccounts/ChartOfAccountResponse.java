package com.businessos.modules.finance.chartofaccounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccountResponse {
    private Long id;
    private Long companyId;
    private String accountCode;
    private String accountName;
    private AccountType type;
    private BigDecimal balance;
    // Jackson strips the "is" prefix from Lombok's isHeaderAccount() getter by default
    // (JSON key would be "headerAccount"), which wouldn't match the frontend's
    // isHeaderAccount field - force the full name explicitly.
    @JsonProperty("isHeaderAccount")
    private boolean isHeaderAccount;
    @JsonProperty("isBankAccount")
    private boolean isBankAccount;
    private boolean allowDirectPosting;
    private boolean active;
    private String description;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
