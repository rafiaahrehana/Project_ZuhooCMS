package com.businessos.modules.crm.lead;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.LeadSource;
import com.businessos.enums.LeadStatus;
import com.businessos.enums.Priority;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk lead import from a CSV export (spreadsheets, another CRM).
 *
 * Header-driven: the first row names the columns, order does not matter, and
 * unknown columns are ignored - so an exported sheet with extra columns works
 * as-is. Rows that cannot become a lead are skipped with a per-line reason
 * rather than failing the file: a 300-row import with three bad rows should
 * produce 297 leads and three explanations, not an error.
 */
@Service
@RequiredArgsConstructor
public class LeadCsvImportService {

    /** Import ceiling per file - one screenful of feedback, not a data migration tool. */
    private static final int MAX_ROWS = 1000;

    private final LeadRepository leadRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Getter
    public static class ImportResult {
        private int created;
        private final List<String> skipped = new ArrayList<>();
    }

    @Transactional
    public ImportResult importCsv(InputStream csv) {
        authorizationService.checkPermission(PermissionCode.LEAD_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) throw new BadRequestException("No company context");

        List<List<String>> rows;
        try {
            rows = parse(csv);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the file: " + e.getMessage());
        }
        if (rows.isEmpty()) throw new BadRequestException("The file is empty");
        if (rows.size() - 1 > MAX_ROWS) {
            throw new BadRequestException("Too many rows - the limit is " + MAX_ROWS + " per file");
        }

        Map<String, Integer> col = headerIndex(rows.get(0));
        if (!col.containsKey("name")) {
            throw new BadRequestException(
                    "No name column found. Expected a header row with at least: name (or contact_name), email, phone");
        }

        ImportResult result = new ImportResult();
        var company = companyRepository.getReferenceById(companyId);

        for (int i = 1; i < rows.size(); i++) {
            int line = i + 1; // 1-based, header included - matches what a spreadsheet shows
            List<String> row = rows.get(i);
            String name = at(row, col, "name");
            String email = at(row, col, "email");
            String phone = at(row, col, "phone");

            if (isBlank(name)) { result.skipped.add("Line " + line + ": no name"); continue; }
            if (isBlank(email) && isBlank(phone)) {
                result.skipped.add("Line " + line + ": no email or phone");
                continue;
            }
            if (!isBlank(email) && leadRepository.existsByEmailAndCompanyIdAndDeletedFalse(email, companyId)) {
                result.skipped.add("Line " + line + ": a lead with email " + email + " already exists");
                continue;
            }
            if (!isBlank(phone) && leadRepository.existsByPhoneAndCompanyIdAndDeletedFalse(phone, companyId)) {
                result.skipped.add("Line " + line + ": a lead with phone " + phone + " already exists");
                continue;
            }

            Lead lead = new Lead();
            lead.setContactName(name);
            lead.setEmail(isBlank(email) ? null : email);
            lead.setPhone(isBlank(phone) ? null : phone);
            lead.setCompanyName(at(row, col, "company"));
            lead.setIndustry(at(row, col, "industry"));
            lead.setJobTitle(at(row, col, "jobtitle"));
            lead.setNotes(at(row, col, "notes"));
            lead.setStatus(LeadStatus.NEW);
            lead.setSource(parseEnum(LeadSource.class, at(row, col, "source"), LeadSource.OTHER));
            lead.setPriority(parseEnum(Priority.class, at(row, col, "priority"), Priority.NORMAL));
            lead.setEstimatedValue(parseDecimal(at(row, col, "estimatedvalue")));
            lead.setCompany(company);
            leadRepository.save(lead);
            result.created++;
        }
        return result;
    }

    /** Normalised header -> index. "Contact Name", "contact_name" and "contactname" all resolve alike. */
    private Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = header.get(i).toLowerCase().replaceAll("[^a-z]", "");
            switch (key) {
                case "name", "contactname", "fullname", "lead" -> index.putIfAbsent("name", i);
                case "company", "companyname", "organisation", "organization" -> index.putIfAbsent("company", i);
                case "email", "emailaddress" -> index.putIfAbsent("email", i);
                case "phone", "phonenumber", "mobile", "contact" -> index.putIfAbsent("phone", i);
                case "industry" -> index.putIfAbsent("industry", i);
                case "jobtitle", "designation", "title", "position" -> index.putIfAbsent("jobtitle", i);
                case "source", "leadsource" -> index.putIfAbsent("source", i);
                case "priority" -> index.putIfAbsent("priority", i);
                case "estimatedvalue", "value", "amount", "dealvalue" -> index.putIfAbsent("estimatedvalue", i);
                case "notes", "note", "message", "description" -> index.putIfAbsent("notes", i);
                default -> { /* unknown columns are fine - exported sheets carry extras */ }
            }
        }
        return index;
    }

    /**
     * Minimal RFC-4180 reader: quoted fields, escaped quotes, commas and
     * newlines inside quotes. Deliberately not a library dependency for one
     * import endpoint.
     */
    private List<List<String>> parse(InputStream in) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> current = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            int c;
            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (inQuotes) {
                    if (ch == '"') {
                        int next = reader.read();
                        if (next == '"') field.append('"');
                        else {
                            inQuotes = false;
                            if (next == -1) break;
                            ch = (char) next;
                            if (ch == ',') { current.add(field.toString()); field.setLength(0); }
                            else if (ch == '\n' || ch == '\r') { endRow(rows, current, field); }
                        }
                    } else {
                        field.append(ch);
                    }
                } else if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                } else if (ch == '\n') {
                    endRow(rows, current, field);
                } else if (ch != '\r') {
                    field.append(ch);
                }
            }
            if (field.length() > 0 || !current.isEmpty()) endRow(rows, current, field);
        }
        return rows;
    }

    private void endRow(List<List<String>> rows, List<String> current, StringBuilder field) {
        current.add(field.toString());
        field.setLength(0);
        // A blank line is not a row.
        if (current.stream().anyMatch(s -> !s.isBlank())) rows.add(new ArrayList<>(current));
        current.clear();
    }

    private String at(List<String> row, Map<String, Integer> col, String key) {
        Integer i = col.get(key);
        if (i == null || i >= row.size()) return null;
        String v = row.get(i).trim();
        return v.isEmpty() ? null : v;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (isBlank(value)) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return fallback; // an unrecognised source/priority should not kill the row
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) return null;
        try {
            return new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
