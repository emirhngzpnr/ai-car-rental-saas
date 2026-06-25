package com.aicarrental.application.report;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportCacheInvalidator {
    private static final List<String> REPORT_CACHES = List.of(
            "dashboardSummary",
            "monthlyRevenue",
            "monthlySummary",
            "topVehicles"
    );

    private final CacheManager cacheManager;

    public void evictAfterCommit() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            clearReportCaches();
                        }
                    }
            );
            return;
        }

        clearReportCaches();
    }

    private void clearReportCaches() {
        REPORT_CACHES.stream()
                .map(cacheManager::getCache)
                .filter(java.util.Objects::nonNull)
                .forEach(Cache::clear);
    }
}
