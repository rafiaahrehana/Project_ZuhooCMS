package com.businessos.shared.payment.wallet;


public class WalletMapper {

    public static WalletResponse toResponse(Wallet w) {
        WalletResponse r = new WalletResponse();
        r.setId(w.getId());
        r.setBalance(w.getBalance());
        r.setCreditBalance(w.getCreditBalance());
        r.setTotalAvailable(w.getTotalAvailable());
        r.setCurrency(w.getCurrency());
        return r;
    }

    public static WalletTransactionResponse toTransactionResponse(WalletTransaction t) {
        WalletTransactionResponse r = new WalletTransactionResponse();
        r.setId(t.getId());
        r.setType(t.getType());
        r.setAmount(t.getAmount());
        r.setBalanceAfter(t.getBalanceAfter());
        r.setReference(t.getReference());
        r.setNotes(t.getNotes());
        r.setTransactedAt(t.getTransactedAt());
        return r;
    }
}
