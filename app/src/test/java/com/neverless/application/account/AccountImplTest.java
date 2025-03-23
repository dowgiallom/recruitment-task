package com.neverless.application.account;

import com.neverless.domain.Amount;
import com.neverless.domain.account.AccountId;
import com.neverless.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountImplTest {

    private AccountImpl account;
    private AccountId accountId;
    private Amount initialBalance;

    @BeforeEach
    void setUp() {
        // Create an AccountId and initial Amount for testing
        accountId = AccountId.random();
        initialBalance = new Amount(100); // Initial balance of 100
        account = new AccountImpl(accountId, initialBalance);
    }

    @Test
    void testInitialBalance() {
        // Assert that the initial balance is correct
        assertEquals(100, account.balance().value(), "Initial balance should be correctly set.");
    }

    @Test
    void testAddAmount() {
        // Create an Amount to add to the account
        Amount amountToAdd = new Amount(50);

        // Add the amount and check if the balance updates correctly
        account.add(amountToAdd);
        assertEquals(150, account.balance().value(), "Balance should be updated after adding an amount.");
    }

    @Test
    void testDeductAmount_Successful() {
        // Create an Amount to deduct from the account
        Amount amountToDeduct = new Amount(30);

        // Deduct the amount and check if the balance updates correctly
        account.deduct(amountToDeduct);
        assertEquals(70, account.balance().value(), "Balance should be updated after deducting an amount.");
    }

    @Test
    void testDeductAmount_InsufficientFunds() {
        // Create an Amount that exceeds the current balance
        Amount amountToDeduct = new Amount(150);

        // Attempt to deduct more than the available balance and expect an exception
        InsufficientFundsException exception = assertThrows(
            InsufficientFundsException.class,
            () -> account.deduct(amountToDeduct),
            "Expected deduct() to throw, but it didn't"
        );

        // Check if the exception message indicates insufficient funds
        assertTrue(exception.getMessage().contains("Insufficient funds"), "Exception message should indicate insufficient funds.");
    }

    @Test
    void testAccountId() {
        // Assert that the account's ID is correctly returned
        assertEquals(accountId, account.id(), "Account ID should be correctly returned.");
    }
}
