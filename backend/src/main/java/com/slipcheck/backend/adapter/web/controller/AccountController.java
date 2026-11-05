package com.slipcheck.backend.adapter.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.slipcheck.backend.adapter.web.dto.Dtos.AccountDto;
import com.slipcheck.backend.adapter.web.dto.Dtos.AccountRequest;
import com.slipcheck.backend.adapter.web.service.AccountService;
import com.slipcheck.backend.persistence.entity.SellerBankAccountEntity;
import com.slipcheck.backend.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

/**
 * Receiving bank accounts. Responses only ever contain the last 4 digits
 * (never the full number, never the ciphertext).
 */
@RestController
@RequestMapping("/api/v1/seller/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountDto> list() {
        return accountService.list(SecurityUtils.currentSellerId()).stream()
                .map(AccountController::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AccountDto> add(@Valid @RequestBody AccountRequest request) {
        SellerBankAccountEntity account = accountService.add(
                SecurityUtils.currentSellerId(),
                request.bankCode(), request.accountNo(), request.holderName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        accountService.remove(SecurityUtils.currentSellerId(), id);
        return ResponseEntity.noContent().build();
    }

    static AccountDto toDto(SellerBankAccountEntity account) {
        return new AccountDto(account.getId(), account.getBankCode(),
                account.getAccountLast4(), account.getHolderName());
    }
}