package com.myfinance.service;

import com.myfinance.entity.FinanceItem;
import com.myfinance.repository.FinanceItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DueDateSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(DueDateSchedulerService.class);

    private final FinanceItemRepository financeItemRepository;
    private final EmailNotificationService emailNotificationService;

    public DueDateSchedulerService(FinanceItemRepository financeItemRepository,
                                   EmailNotificationService emailNotificationService) {
        this.financeItemRepository = financeItemRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Scheduled(cron = "${app.scheduler.due-date-cron:0 0 8 * * *}")
    @Transactional
    public int processDueDateNotifications() {
        LocalDate today = LocalDate.now();
        List<FinanceItem> dueItems = financeItemRepository.findDueItemsForNotification(today);
        log.info("DueDateScheduler triggered for {}: found {} pending due items.", today, dueItems.size());

        for (FinanceItem item : dueItems) {
            emailNotificationService.sendDueDateReminder(item);
            item.setReminderSent(true);
            item.setLastRemindedAt(Instant.now());
            if (item.getDueDate().isBefore(today)) {
                item.setStatus("OVERDUE");
            }
        }
        financeItemRepository.saveAll(dueItems);
        return dueItems.size();
    }
}
