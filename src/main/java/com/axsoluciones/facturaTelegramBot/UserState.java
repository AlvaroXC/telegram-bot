package com.axsoluciones.facturaTelegramBot;

public enum UserState {
    AWAITING_TYPE,
    AWAITING_PHONE_NUMBER,
    AWAITING_DETAILS,
    AWAITING_INVOICE_DATE,
    AWAITING_EXPIRATION,
    TEMP_TYPE,
    TEMP_PHONE,
}
