export type PeriodStatus = 'OPEN' | 'CLOSED';

export interface FiscalYearSummary {
  fiscalYear: number;
  name: string;
  startDate: string;
  endDate: string;
  totalPeriods: number;
  openPeriods: number;
  closedPeriods: number;
  status: 'DRAFT' | 'ACTIVE' | 'CLOSED';
  yearEndPosted: boolean;
  current: boolean;
  createdAt?: string;
}

export interface Vendor {
  id: number;
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  taxId?: string;
  address?: string;
  paymentTerms?: string;
  notes?: string;
  active: boolean;
  outstandingBalance: number;
  createdAt?: string;
}

export interface VendorRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  taxId?: string;
  address?: string;
  paymentTerms?: string;
  notes?: string;
}

export type VendorBillStatus = 'DRAFT' | 'APPROVED' | 'PARTIALLY_PAID' | 'OVERDUE' | 'PAID' | 'CANCELLED';

export interface VendorBill {
  id: number;
  billNumber: string;
  vendorReference?: string;
  vendorId: number;
  vendorName?: string;
  expenseAccountId?: number;
  expenseAccountName?: string;
  billDate: string;
  dueDate: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  status: VendorBillStatus;
  description?: string;
  createdBy?: string;
  approvedBy?: string;
  approvedDate?: string;
  createdAt?: string;
}

export interface VendorBillRequest {
  vendorId: number | null;
  billDate: string;
  dueDate: string;
  subtotal: number;
  taxAmount?: number;
  vendorReference?: string;
  description?: string;
  expenseAccountId?: number | null;
}

export interface StatementImportUnmatchedLine {
  date: string;
  description: string;
  amount: number;
  reason: string;
}

export interface StatementImportResult {
  totalLines: number;
  matched: number;
  unmatchedCount: number;
  unmatchedLines: StatementImportUnmatchedLine[];
  reconciliation: BankReconciliation;
}

export interface ApAgeingLine {
  billId: number;
  billNumber: string;
  vendorId?: number;
  vendorName?: string;
  dueDate?: string;
  balanceAmount: number;
  daysOverdue: number;
  bucket: string;
}

export interface ApAgeingReport {
  asOfDate: string;
  current: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  over90: number;
  totalOutstanding: number;
  lines: ApAgeingLine[];
}

export interface Budget {
  id: number;
  category: string;
  fiscalYear: number;
  amount: number;
  notes?: string;
  actualSpend: number;
  remaining: number;
  usedPercent: number;
  overBudget: boolean;
}

export interface BudgetRequest {
  category: string;
  fiscalYear: number;
  amount: number;
  notes?: string;
}

export type FixedAssetStatus = 'ACTIVE' | 'FULLY_DEPRECIATED' | 'DISPOSED';

export interface FixedAsset {
  id: number;
  name: string;
  assetTag?: string;
  category?: string;
  cost: number;
  salvageValue: number;
  usefulLifeMonths: number;
  acquisitionDate: string;
  accumulatedDepreciation: number;
  bookValue: number;
  monthlyDepreciation: number;
  status: FixedAssetStatus;
  notes?: string;
  createdAt?: string;
}

export interface FixedAssetRequest {
  name: string;
  assetTag?: string;
  category?: string;
  cost: number;
  salvageValue?: number;
  usefulLifeMonths: number;
  acquisitionDate: string;
  notes?: string;
  postPurchaseToLedger?: boolean;
}

export interface DepreciationRun {
  id: number;
  year: number;
  month: number;
  totalAmount: number;
  assetsDepreciated: number;
  runBy?: string;
  runAt?: string;
}

export interface AccountingPeriod {
  id: number;
  fiscalYear: number;
  periodNumber: number;
  startDate: string;
  endDate: string;
  label: string;
  status: PeriodStatus;
  closedBy?: string;
  closedAt?: string;
  reopenedBy?: string;
  reopenedAt?: string;
}

export interface ChartOfAccount {
  id: number;
  companyId: number;
  accountCode: string;
  accountName: string;
  type: string;
  balance: number;
  isHeaderAccount: boolean;
  isBankAccount: boolean;
  allowDirectPosting: boolean;
  active: boolean;
  description?: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Expense {
  id: number;
  companyId: number;
  expenseNumber: string;
  submittedByName?: string;
  description: string;
  amount: number;
  vendorName?: string;
  category?: string;
  expenseAccountId?: number;
  expenseAccountName?: string;
  expenseDate: string;
  receiptUrl?: string;
  status: string;
  approvedByName?: string;
  approvalNotes?: string;
  submittedAt?: string;
  notes?: string;
  createdAt: string;
  title?: string;
  currency?: string;
  employeeId?: number;
  reimbursedDate?: string;
  reimbursementMethod?: string;
  referenceNumber?: string;
  updatedAt?: string;
}

// Matches the backend's actual ClientInvoiceResponse (not a generic "Invoice" -
// this platform only has client invoices). Field names below were previously out
// of sync with the server (claimed outstandingAmount/discountAmount, which don't
// exist; the real remaining-balance field is balanceAmount).
export interface InvoiceItem {
  id?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal?: number;
  notes?: string;
}

export interface Invoice {
  id: number;
  companyId?: number;
  invoiceNumber: string;
  clientId?: number;
  clientName?: string;
  invoiceDate?: string;
  dueDate?: string;
  items?: InvoiceItem[];
  subtotal: number;
  taxRatePercent?: number;
  taxAmount: number;
  discountAmount?: number;
  currency?: string;
  exchangeRate?: number;
  totalAmount: number;
  paidAmount: number;
  creditedAmount?: number;
  balanceAmount: number;
  status: string;
  paymentTerms?: string;
  description?: string;
  notes?: string;
  sentDate?: string;
  paidDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface InvoiceItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  notes?: string;
}

export interface InvoiceRequest {
  clientId: number;
  invoiceDate: string;
  dueDate: string;
  items: InvoiceItemRequest[];
  taxAmount?: number;
  taxRatePercent?: number;
  discountAmount?: number;
  currency?: string;
  exchangeRate?: number;
  paymentTerms?: string;
  description?: string;
  notes?: string;
}

export interface CreditNote {
  id: number;
  creditNoteNumber: string;
  clientInvoiceId: number;
  invoiceNumber?: string;
  clientId?: number;
  clientName?: string;
  amount: number;
  reason?: string;
  issuedByName?: string;
  issuedAt?: string;
}

export interface CreditNoteRequest {
  clientInvoiceId: number;
  amount: number;
  reason?: string;
}

// NOTE: Vendor and VendorPayment were removed - there is no separate Vendor entity in the
// backend. Vendor identity is a plain free-text 'vendorName' field on Expense, and vendor
// payments now go through Expense's existing approve -> reject -> mark-as-paid lifecycle
// (see ExpenseService.getByVendor/markAsPaid and components/expenses).

export interface JournalEntryLine {
  id?: number;
  accountId: number;
  accountCode?: string;
  accountName?: string;
  debitAmount: number;
  creditAmount: number;
  lineDescription?: string;
}

export interface JournalEntryLineRequest {
  accountId: number | null;
  debitAmount: number;
  creditAmount: number;
  lineDescription?: string;
}

export interface JournalEntryRequest {
  entryDate: string;
  lines: JournalEntryLineRequest[];
  description?: string;
  notes?: string;
}

export interface JournalEntry {
  id: number;
  companyId: number;
  journalEntryNumber: string;
  entryDate: string;
  lines?: JournalEntryLine[];
  debitAccountId: number;
  debitAccountName?: string;
  creditAccountId: number;
  creditAccountName?: string;
  amount: number;
  description?: string;
  notes?: string;
  createdBy?: string;
  createdDate?: string;
  approvedBy?: string;
  approvedDate?: string;
  approved: boolean;
  posted: boolean;
  postedDate?: string;
  reversed?: boolean;
  reversalEntryId?: number;
  reversedFromEntryId?: number;
  reversedDate?: string;
}

export type PaymentMethod = 'BKASH' | 'NAGAD' | 'ROCKET' | 'SSLCOMMERZ' | 'BANK_TRANSFER' | 'CASH' | 'WALLET' | 'CHEQUE';
export const PAYMENT_METHODS: PaymentMethod[] = ['BKASH', 'NAGAD', 'ROCKET', 'SSLCOMMERZ', 'BANK_TRANSFER', 'CASH', 'WALLET', 'CHEQUE'];

/**
 * Methods valid for paying money OUT to an employee.
 *
 * SSLCOMMERZ is excluded because it is a collection gateway - it moves money
 * *to* the company from a payer's card or mobile wallet. It has no payout API,
 * so offering it on a salary payment implied a capability that does not exist.
 * WALLET is excluded because the wallet is a company-level balance, not a
 * per-employee one, so "paid from wallet" has no counterparty.
 *
 * Selecting any of these still only RECORDS how the payment was made - the
 * actual transfer happens in the bank's or MFS provider's own system.
 */
export const PAYROLL_PAYMENT_METHODS: PaymentMethod[] =
  ['BANK_TRANSFER', 'BKASH', 'NAGAD', 'ROCKET', 'CHEQUE', 'CASH'];

export type PaymentReceiptStatus = 'PENDING' | 'CONFIRMED' | 'DEPOSITED' | 'FAILED' | 'REVERSED';

export interface PaymentReceipt {
  id: number;
  receiptNumber: string;
  invoiceId?: number;
  invoiceNumber?: string;
  clientId: number;
  clientName?: string;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  transactionReference?: string;
  status: PaymentReceiptStatus;
  depositedToBank?: string;
  notes?: string;
  createdAt: string;
  companyId?: number;
  updatedAt?: string;
}

export interface PaymentReceiptRequest {
  clientId: number;
  invoiceId?: number;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  transactionReference?: string;
  notes?: string;
}

export interface GeneralLedgerEntry {
  id: number;
  transactionDate: string;
  accountId: number;
  accountName: string;
  accountCode: string;
  accountType?: string;
  debitAmount: number;
  creditAmount: number;
  description?: string;
  referenceType?: string;
  referenceId?: number;
  referenceNumber?: string;
  isReconciled: boolean;
  reconciliationNotes?: string;
  postedBy?: string;
  postedDate?: string;
  posted: boolean;
}

export interface AccountLedgerReport {
  accountId: number;
  accountCode: string;
  accountName: string;
  periodStart: string;
  periodEnd: string;
  openingBalance: number;
  entries: GeneralLedgerEntry[];
  closingBalance: number;
  generatedDate: string;
}

export interface BankReconciliation {
  id: number;
  bankAccountId: number;
  bankAccountName?: string;
  reconciliationDate: string;
  glBalance: number;
  bankStatementBalance: number;
  // glBalance - adjustedBankBalance; must be ~0 before this can be marked reconciled.
  difference: number;
  outstandingDepositsTotal: number;
  outstandingChecksTotal: number;
  adjustedBankBalance: number;
  reconciled: boolean;
  reconciledDate?: string;
  reconciledBy?: string;
  discrepancyNotes?: string;
  companyId?: number;
  statementFileName?: string;
  statementFileUrl?: string;
  statementUploadedAt?: string;
}

export interface BankReconciliationRequest {
  bankAccountId: number;
  bankStatementBalance: number;
}

// Subset of GeneralLedgerResponse used for the bank reconciliation transaction checklist.
export interface ReconciliationTransaction {
  id: number;
  transactionDate: string;
  description?: string;
  debitAmount: number;
  creditAmount: number;
  referenceNumber?: string;
  isReconciled: boolean;
}

export type WalletTransactionType = 'CREDIT' | 'DEBIT' | 'CREDIT_APPLIED' | 'REFUND_CREDIT' | 'REFERRAL_REWARD';
export const WALLET_TRANSACTION_TYPES: WalletTransactionType[] = ['CREDIT', 'DEBIT', 'CREDIT_APPLIED', 'REFUND_CREDIT', 'REFERRAL_REWARD'];

export interface Wallet {
  id: number;
  balance: number;
  creditBalance: number;
  totalAvailable: number;
  currency: string;
}

export interface WalletTransaction {
  id: number;
  type: WalletTransactionType;
  amount: number;
  balanceAfter: number;
  reference?: string;
  notes?: string;
  transactedAt: string;
}

export interface ProfitLossReport {
  periodStart: string;
  periodEnd: string;
  totalRevenue: number;
  totalExpense: number;
  netProfit: number;
  generatedDate: string;
}

export interface BalanceSheetReport {
  asOfDate: string;
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  balanced: boolean;
  outOfBalanceAmount: number;
  generatedDate: string;
}

export interface TrialBalanceAccountBalance {
  accountId: number;
  accountCode: string;
  accountName: string;
  debitBalance: number;
  creditBalance: number;
}

export interface TrialBalanceReport {
  asOfDate: string;
  accounts: TrialBalanceAccountBalance[];
  totalDebit: number;
  totalCredit: number;
  generatedDate: string;
}

export interface AgeingLine {
  invoiceId: number;
  invoiceNumber: string;
  clientId?: number;
  clientName?: string;
  dueDate: string;
  balanceAmount: number;
  daysOverdue: number;
  bucket: string;
}

export interface AgeingReport {
  asOfDate: string;
  current: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  over90: number;
  totalOutstanding: number;
  lines: AgeingLine[];
}

export interface CashFlowLine {
  category: string;
  inflow: number;
  outflow: number;
}

export interface CashFlowReport {
  periodStart: string;
  periodEnd: string;
  openingBalance: number;
  closingBalance: number;
  totalInflows: number;
  totalOutflows: number;
  netChange: number;
  lines: CashFlowLine[];
}
