package com.slipcheck.backend.adapter.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.slipcheck.backend.adapter.web.exception.AccountNotFoundException;
import com.slipcheck.backend.adapter.web.exception.InvalidAccountNumberException;
import com.slipcheck.backend.audit.AuditService;
import com.slipcheck.backend.audit.AuditService.ActorType;
import com.slipcheck.backend.persistence.entity.SellerBankAccountEntity;
import com.slipcheck.backend.persistence.repo.SellerBankAccountRepository;
import com.slipcheck.backend.security.CryptoService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Receiving bank accounts. Numbers are AES-GCM encrypted at rest; callers only ever
 * get entities whose full number is ciphertext, and the web layer exposes only the last 4 digits.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final SellerBankAccountRepository accounts;
    private final CryptoService crypto;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<SellerBankAccountEntity> list(UUID sellerId) {
        return accounts.findBySellerIdOrderByCreatedAtAsc(sellerId);
    }

    @Transactional
    public SellerBankAccountEntity add(UUID sellerId, String bankCode, String accountNo, String holderName) {
        String digits = accountNo.replaceAll("[^0-9]", "");
        if (digits.length() < 6 || digits.length() > 16) {
            throw new InvalidAccountNumberException();
        }
        SellerBankAccountEntity account = new SellerBankAccountEntity(
                sellerId,
                bankCode.trim().toUpperCase(),
                crypto.encrypt(digits),
                digits.substring(digits.length() - 4),
                holderName.trim());
        accounts.save(account);
        audit.record(sellerId, ActorType.SELLER, sellerId.toString(), "ACCOUNT_ADDED",
                "seller_bank_account", account.getId().toString(),
                Map.of("bankCode", account.getBankCode(), "last4", account.getAccountLast4()));
        return account;
    }

    @Transactional
    public void remove(UUID sellerId, UUID accountId) {
        SellerBankAccountEntity account = accounts.findByIdAndSellerId(accountId, sellerId)
                .orElseThrow(AccountNotFoundException::new);
        accounts.delete(account);
        audit.record(sellerId, ActorType.SELLER, sellerId.toString(), "ACCOUNT_REMOVED",
                "seller_bank_account", accountId.toString(), Map.of("last4", account.getAccountLast4()));
    }
}