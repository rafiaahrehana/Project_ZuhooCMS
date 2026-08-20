package com.businessos.shared.payment.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByContextTypeAndContextId(String contextType, Long contextId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.contextType = :contextType AND w.contextId = :contextId")
    Optional<Wallet> findByContextTypeAndContextIdForUpdate(
        @Param("contextType") String contextType, @Param("contextId") Long contextId);

    boolean existsByContextTypeAndContextId(String contextType, Long contextId);
}
