package com.businessos.modules.ai.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Splits an AI-backed service method into transactions that end before the
 * provider call instead of spanning it.
 *
 * <p>An AI request blocks for up to ~31s in the worst case (three attempts at a
 * 10s provider timeout, plus backoff). A {@code @Transactional} method that calls
 * AI in the middle of its body keeps its pooled JDBC connection checked out for
 * that whole time, because a transaction cannot release its connection before it
 * commits - so a slow or unreachable provider drains the connection pool and
 * takes unrelated requests down with it. Suspending the transaction does not
 * help: a suspended transaction still owns its connection.
 *
 * <p>Callers therefore do their entity reads inside {@link #load}, which commits
 * as soon as the callback returns, then make the provider call with no
 * transaction open, then persist through {@link #persist} if they need to.
 *
 * <p>Uses its own templates rather than the auto-configured {@code
 * TransactionTemplate} bean so that {@code readOnly} can differ between the two
 * phases, and so no second bean of that type is introduced (which would make the
 * existing by-type injection in {@code AssetImportServiceImpl} ambiguous).
 *
 * <p>Entities returned by {@code load} are detached once it commits - build the
 * response DTO (and read every lazy association) inside the callback, not after.
 */
@Component
public class AiTransactionBoundary {

    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;

    public AiTransactionBoundary(PlatformTransactionManager transactionManager) {
        this.readOnly = new TransactionTemplate(transactionManager);
        // Matches the @Transactional(readOnly = true) these call sites used to
        // carry: Hibernate keeps FlushMode.MANUAL, so nothing accidentally
        // dirtied while assembling a prompt gets written back.
        this.readOnly.setReadOnly(true);

        this.readWrite = new TransactionTemplate(transactionManager);
    }

    /** Reads entities and assembles a prompt; commits before returning. */
    public <T> T load(Supplier<T> work) {
        return readOnly.execute(status -> work.get());
    }

    /** Persists the outcome of an AI call in a fresh read-write transaction. */
    public <T> T persist(Supplier<T> work) {
        return readWrite.execute(status -> work.get());
    }
}
