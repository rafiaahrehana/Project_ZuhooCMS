package com.businessos.modules.finance.chartofaccounts;

public enum AccountType {
    ASSET("Asset", "1000-1999", "Resources owned by company"),
    LIABILITY("Liability", "2000-2999", "Amounts owed by company"),
    EQUITY("Equity", "3000-3999", "Owner's interest in company"),
    REVENUE("Revenue", "4000-4999", "Income from operations"),
    EXPENSE("Expense", "5000-5999", "Costs of operations"),
    CONTRA_ASSET("Contra Asset", "1500-1599", "Reduces asset value"),
    CONTRA_LIABILITY("Contra Liability", "2500-2599", "Reduces liability value"),
    CONTRA_REVENUE("Contra Revenue", "4500-4599", "Reduces revenue");

    private final String label;
    private final String codeRange;
    private final String description;

    AccountType(String label, String codeRange, String description) {
        this.label = label;
        this.codeRange = codeRange;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getCodeRange() { return codeRange; }
    public String getDescription() { return description; }

    /**
     * Single source of truth for debit/credit-normal classification - previously
     * duplicated independently in ChartOfAccount, GeneralLedgerServiceImpl, and
     * FinancialReportServiceImpl, risking silent drift between them.
     */
    public boolean isCreditNormal() {
        return this == LIABILITY || this == CONTRA_ASSET || this == EQUITY || this == REVENUE;
    }
}
