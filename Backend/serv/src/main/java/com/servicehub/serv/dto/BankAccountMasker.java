package com.servicehub.serv.dto;

public final class BankAccountMasker {

    private BankAccountMasker() {
    }

    public static String mask(String accountNumber) {
        if (accountNumber == null) {
            return null;
        }
        if (accountNumber.length() <= 4) {
            return "*".repeat(accountNumber.length());
        }
        return "*".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }
}
