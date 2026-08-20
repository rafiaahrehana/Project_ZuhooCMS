package com.businessos.shared.payment.wallet;

import com.businessos.enums.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallet")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return ResponseEntity.ok(walletService.getOrCreateWallet());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(walletService.getTransactions(type,
                PageRequest.of(page, size, Sort.by("transactedAt").descending())));
    }
}


