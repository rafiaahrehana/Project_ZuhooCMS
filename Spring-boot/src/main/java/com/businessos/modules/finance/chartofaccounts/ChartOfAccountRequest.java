package com.businessos.modules.finance.chartofaccounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

// AllArgsConstructor access is package-private (not public) so Jackson can't use it as
// a deserialization creator - a creator requires every constructor parameter, so a JSON
// body omitting a primitive int/boolean field fails with "Cannot map null into type
// int/boolean" before @Valid runs. Package-private forces the no-args+setters path,
// which correctly leaves missing primitives at their Java default.
@Data @NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PACKAGE) @Builder
public class ChartOfAccountRequest {

    @NotBlank(message = "Account code is required")
    @Size(min = 3, max = 10)
    private String accountCode;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotNull(message = "Account type is required")
    private AccountType type;

    private String description;
    // Matches ChartOfAccountResponse.isHeaderAccount naming/JSON key - see the comment there.
    @JsonProperty("isHeaderAccount")
    private boolean isHeaderAccount;
    @JsonProperty("isBankAccount")
    private boolean isBankAccount;
    @Builder.Default
    private boolean allowDirectPosting = true;
    @Builder.Default
    private boolean active = true;
    private String notes;

    // Migration support: the balance this account starts with (from a previous
    // accounting system). Posts a balanced entry against Opening Balance Equity on
    // create - can't just set the balance field or the ledger wouldn't back it up.
    @DecimalMin(value = "0.0")
    private java.math.BigDecimal openingBalance;
    private java.time.LocalDate openingBalanceDate;
}
