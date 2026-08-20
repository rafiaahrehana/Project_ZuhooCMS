package com.raf.zuhoo.ui.servicerequest;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.model.response.ServiceFormField;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the per-service custom fields into a container at runtime and reads the answers back
 * out as the formData map the API expects (field id -> string answer).
 *
 * Built in code rather than as layouts because the field set is defined per service by an admin
 * and isn't known until the service is chosen — hardcoding a fixed set of extra inputs is exactly
 * what this replaces.
 *
 * Text/number/email/phone/file-upload/date/dropdown fields are all rendered as a
 * TextInputLayout (see the Widget.Zuhoo.TextInputLayout* styles) so they match the rest of the
 * app's redesigned form fields; the TextInputLayout's floating hint carries the field's label, so
 * those types get no separate label view. Radio/checkbox fields have no TextInputLayout
 * equivalent, so they keep a small leading label TextView (restyled to TextAppearance.Zuhoo.Label)
 * above the control.
 */
public class DynamicFormRenderer {

    private final Context context;
    private final LinearLayout container;
    private final int fieldSpacing;

    // Insertion-ordered so validation reports the first missing field in the order shown.
    private final Map<ServiceFormField, View> inputs = new LinkedHashMap<>();

    public DynamicFormRenderer(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
        this.fieldSpacing = context.getResources().getDimensionPixelSize(R.dimen.field_spacing);
    }

    public void render(List<ServiceFormField> fields) {

        container.removeAllViews();
        inputs.clear();

        if (fields == null || fields.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        boolean firstVisibleField = true;

        for (ServiceFormField field : fields) {

            // FORMULA fields are computed by the backend from other answers — showing an input
            // for one would invite the user to type something that gets overwritten.
            if (ServiceFormField.TYPE_FORMULA.equals(field.getFieldType())) {
                continue;
            }

            String type = field.getFieldType() == null ? "" : field.getFieldType();
            boolean needsSeparateLabel = ServiceFormField.TYPE_RADIO.equals(type)
                    || ServiceFormField.TYPE_CHECKBOX.equals(type);

            if (needsSeparateLabel) {
                View labelView = label(field);
                applyTopSpacing(labelView, firstVisibleField);
                container.addView(labelView);
                firstVisibleField = false;
            }

            View input = inputFor(field, type);
            if (!needsSeparateLabel) {
                applyTopSpacing(input, firstVisibleField);
            }
            inputs.put(field, input);
            container.addView(input);
            firstVisibleField = false;
        }

        container.setVisibility(inputs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** Adds the field_spacing gap above a field/label, skipped for the very first one in the form. */
    private void applyTopSpacing(View view, boolean isFirst) {

        if (isFirst) {
            return;
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).topMargin = fieldSpacing;
        }
    }

    private TextView label(ServiceFormField field) {

        TextView view = new TextView(context);
        view.setLayoutParams(fullWidth());
        view.setText(hintFor(field));
        view.setTextAppearance(R.style.TextAppearance_Zuhoo_Label);
        view.setPadding(0, 0, 0, dp(4));

        return view;
    }

    private View inputFor(ServiceFormField field, String type) {

        switch (type) {

            case ServiceFormField.TYPE_TEXTAREA:
                return textInput(field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);

            case ServiceFormField.TYPE_NUMBER:
                return textInput(field, InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);

            case ServiceFormField.TYPE_EMAIL:
                return textInput(field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 1);

            case ServiceFormField.TYPE_PHONE:
                return textInput(field, InputType.TYPE_CLASS_PHONE, 1);

            case ServiceFormField.TYPE_DROPDOWN:
                return dropdown(field);

            case ServiceFormField.TYPE_RADIO:
                return radioGroup(field);

            case ServiceFormField.TYPE_CHECKBOX:
                return checkBox();

            case ServiceFormField.TYPE_DATE:
                return datePicker(field);

            case ServiceFormField.TYPE_FILE_UPLOAD:
                // Uploading needs a file picker and a round trip through /api/upload; until that
                // is wired here, accept the URL of an already-uploaded file rather than silently
                // dropping the field from the form.
                return textInput(field, InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI, 1);

            default:
                return textInput(field, InputType.TYPE_CLASS_TEXT, 1);
        }
    }

    /** Plain text-style field: TextInputLayout at its theme default (Widget.Zuhoo.TextInputLayout,
     *  wired via the app theme's textInputStyle) wrapping a TextInputEditText. */
    private TextInputLayout textInput(ServiceFormField field, int inputType, int lines) {

        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(fullWidth());
        layout.setHint(hintFor(field));

        TextInputEditText edit = new TextInputEditText(context);
        edit.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        edit.setInputType(inputType);
        edit.setLines(lines);
        edit.setGravity(lines > 1 ? Gravity.TOP | Gravity.START : Gravity.CENTER_VERTICAL);

        layout.addView(edit);
        return layout;
    }

    /** Exposed-dropdown field, replacing the old raw Spinner. TextInputLayout has no public
     *  constructor that takes a style resource directly, so the Dropdown-styled chrome comes from
     *  inflating the small reusable view_dynamic_dropdown_field.xml rather than building it purely
     *  in code. */
    private TextInputLayout dropdown(ServiceFormField field) {

        TextInputLayout layout = (TextInputLayout) LayoutInflater.from(context)
                .inflate(R.layout.view_dynamic_dropdown_field, container, false);
        layout.setHint(hintFor(field));

        AutoCompleteTextView dropdown = (AutoCompleteTextView) layout.getEditText();

        List<String> options = new ArrayList<>(field.options());
        // A non-required dropdown needs a way to answer nothing at all.
        if (!field.isRequired()) {
            options.add(0, context.getString(R.string.form_field_no_selection));
        }

        dropdown.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, options));
        // Non-editable dropdown defaults to the first option, same as Spinner's implicit
        // position-0 selection.
        if (!options.isEmpty()) {
            dropdown.setText(options.get(0), false);
        }

        return layout;
    }

    private RadioGroup radioGroup(ServiceFormField field) {

        RadioGroup group = new RadioGroup(context);
        group.setLayoutParams(fullWidth());
        group.setOrientation(LinearLayout.VERTICAL);

        List<String> options = field.options();

        for (int i = 0; i < options.size(); i++) {
            RadioButton button = new RadioButton(context);
            button.setId(View.generateViewId());
            button.setText(options.get(i));
            button.setTag(options.get(i));
            group.addView(button);
        }

        return group;
    }

    private CheckBox checkBox() {
        CheckBox view = new CheckBox(context);
        view.setLayoutParams(fullWidth());
        return view;
    }

    /** Date field, replacing the old bare-TextView-as-button + DatePickerDialog pattern. Same
     *  inflate-a-styled-resource approach as dropdown(), using the calendar-end-icon Date style. */
    private TextInputLayout datePicker(ServiceFormField field) {

        TextInputLayout layout = (TextInputLayout) LayoutInflater.from(context)
                .inflate(R.layout.view_dynamic_date_field, container, false);
        layout.setHint(hintFor(field));

        EditText edit = layout.getEditText();

        View.OnClickListener openPicker = v -> {

            Calendar now = Calendar.getInstance();

            new DatePickerDialog(context, (picker, year, month, day) ->
                    // ISO-8601 so the backend parses it the same way the web app's date inputs
                    // are submitted.
                    edit.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)).show();
        };

        // Matches CreateLeaveRequestActivity's date fields: only the EditText itself is wired to
        // open the picker, the calendar end-icon is the same purely visual affordance used there.
        edit.setOnClickListener(openPicker);

        return layout;
    }

    private String hintFor(ServiceFormField field) {
        return field.isRequired()
                ? context.getString(R.string.form_field_required_label, field.getLabel())
                : field.getLabel();
    }

    /** @return the label of the first required field left blank, or null when the form is complete. */
    public String firstMissingRequiredLabel() {

        for (Map.Entry<ServiceFormField, View> entry : inputs.entrySet()) {

            ServiceFormField field = entry.getKey();

            if (field.isRequired() && valueOf(field, entry.getValue()).isEmpty()) {
                return field.getLabel();
            }
        }

        return null;
    }

    /** @return field id -> answer, omitting anything left blank. Null when nothing was answered. */
    public Map<String, String> answers() {

        Map<String, String> answers = new LinkedHashMap<>();

        for (Map.Entry<ServiceFormField, View> entry : inputs.entrySet()) {

            String value = valueOf(entry.getKey(), entry.getValue());

            if (!value.isEmpty() && entry.getKey().getId() != null) {
                answers.put(String.valueOf(entry.getKey().getId()), value);
            }
        }

        return answers.isEmpty() ? null : answers;
    }

    private String valueOf(ServiceFormField field, View input) {

        if (input instanceof CheckBox) {
            // Only send a checkbox when it's ticked — an unticked optional box is "no answer",
            // and sending "false" would make it look answered.
            return ((CheckBox) input).isChecked() ? "true" : "";
        }

        if (input instanceof RadioGroup) {
            int checkedId = ((RadioGroup) input).getCheckedRadioButtonId();
            if (checkedId == -1) {
                return "";
            }
            RadioButton checked = input.findViewById(checkedId);
            return checked == null || checked.getTag() == null ? "" : checked.getTag().toString();
        }

        if (input instanceof TextInputLayout) {

            EditText edit = ((TextInputLayout) input).getEditText();
            String value = edit == null || edit.getText() == null
                    ? "" : edit.getText().toString().trim();

            // Dropdown's placeholder "no selection" option reads as blank, same as an unticked
            // optional checkbox — it's a real list item so the user can explicitly select it, but
            // it must not travel to the backend as an answer.
            if (ServiceFormField.TYPE_DROPDOWN.equals(field.getFieldType())
                    && value.equals(context.getString(R.string.form_field_no_selection))) {
                return "";
            }

            return value;
        }

        return "";
    }

    private ViewGroup.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
