package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Employee.contractEndDate is captured on hire/edit but, unlike probationEndDate
 * (which drives an HR dashboard reminder), passing it triggered nothing - no
 * status change, no alert - so payroll kept paying a contractor past their own
 * end date with nothing warning anyone. This only reminds; ending the
 * employment (deactivating, stopping pay) stays an explicit HR action via the
 * existing terminate/resign flow, same as every other status change in HR.
 */
@Component
@RequiredArgsConstructor
public class ContractExpiryScheduler {

    private static final int REMINDER_WINDOW_DAYS = 14;

    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void remindContractEndingSoon() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(REMINDER_WINDOW_DAYS);

        List<Employee> ending = employeeRepository.findContractEndingSoonUnnotified(today, cutoff);
        for (Employee employee : ending) {
            employee.setContractEndReminderSentAt(LocalDateTime.now());

            Employee recipient = employee.getReportingManager();
            Long recipientUserId = recipient != null && recipient.getUser() != null
                    ? recipient.getUser().getId()
                    : (employee.getCompany().getOwner() != null ? employee.getCompany().getOwner().getId() : null);
            if (recipientUserId == null) continue;

            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.CONTRACT_ENDING,
                    "Contract ending soon",
                    employee.getFullName() + "'s contract ends on " + employee.getContractEndDate()
                            + " - review whether to renew, extend, or offboard.",
                    "/hrm/employees/" + employee.getId(),
                    recipientUserId,
                    employee.getCompany().getId()));
        }
    }
}
