package com.raf.zuhoo.ui.expense;

import android.content.Context;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.ExpenseCategory;

public final class ExpenseCategoryLabels {

    private ExpenseCategoryLabels() {
    }

    public static String labelFor(Context context, String category) {

        if (category == null) {
            return "";
        }

        switch (category) {
            case ExpenseCategory.TRAVEL:
                return context.getString(R.string.expense_category_travel);
            case ExpenseCategory.MEALS:
                return context.getString(R.string.expense_category_meals);
            case ExpenseCategory.OFFICE_SUPPLIES:
                return context.getString(R.string.expense_category_office_supplies);
            case ExpenseCategory.TRANSPORT:
                return context.getString(R.string.expense_category_transport);
            case ExpenseCategory.ACCOMMODATION:
                return context.getString(R.string.expense_category_accommodation);
            case ExpenseCategory.LICENSING:
                return context.getString(R.string.expense_category_licensing);
            case ExpenseCategory.OTHER:
                return context.getString(R.string.expense_category_other);
            default:
                return category;
        }
    }

    public static String[] allLabels(Context context) {
        String[] labels = new String[ExpenseCategory.VALUES.length];
        for (int i = 0; i < ExpenseCategory.VALUES.length; i++) {
            labels[i] = labelFor(context, ExpenseCategory.VALUES[i]);
        }
        return labels;
    }
}
