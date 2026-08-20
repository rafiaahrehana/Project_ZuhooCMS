package com.businessos.modules.finance.journalentry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JournalEntryService {

    JournalEntryResponse create(JournalEntryRequest request);

    JournalEntryResponse getById(Long id);

    Page<JournalEntryResponse> getAll(Pageable pageable);

    void approve(Long id);

    void post(Long id);

    JournalEntryResponse reverse(Long id);

    JournalEntryResponse delete(Long id);
}
