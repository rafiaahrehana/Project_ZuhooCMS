package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.finance.expense.ExpenseRequest;
import com.businessos.modules.finance.expense.ExpenseResponse;
import com.businessos.modules.finance.expense.ExpenseService;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubmitExpenseTool implements AiTool {

    private final ExpenseService expenseService;

    @Override
    public String name() {
        return "submit_expense";
    }

    @Override
    public String description() {
        return "Submit an expense claim for the employee (goes to the normal approval queue, does not reimburse automatically).";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "title", Map.of("type", "string"),
                "description", Map.of("type", "string"),
                "amount", Map.of("type", "number"),
                "vendorName", Map.of("type", "string"),
                "category", Map.of("type", "string")
            ),
            "required", java.util.List.of("description", "amount")
        );
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        return "submit an expense claim for " + args.get("amount") + " (\"" + args.get("description") + "\")";
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        if (args == null || args.get("description") == null || args.get("amount") == null) {
            return AiToolResult.failure("Missing a description or an amount.");
        }

        ExpenseRequest request = new ExpenseRequest();
        request.setDescription(args.get("description").toString());
        try {
            request.setAmount(new BigDecimal(args.get("amount").toString()));
        } catch (NumberFormatException e) {
            return AiToolResult.failure("Couldn't understand the amount: " + args.get("amount"));
        }
        request.setExpenseDate(LocalDate.now());
        request.setTitle(args.get("title") != null ? args.get("title").toString() : request.getDescription());
        if (args.get("vendorName") != null) request.setVendorName(args.get("vendorName").toString());
        if (args.get("category") != null) request.setCategory(args.get("category").toString());

        try {
            ExpenseResponse response = expenseService.create(request);
            return AiToolResult.ok(
                "Submitted an expense claim for " + request.getAmount() + " (\"" + request.getDescription()
                    + "\"), now pending approval (expense #" + response.getId() + ").",
                response);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't submit that expense: " + e.getMessage());
        }
    }
}
