package com.astral.express.pccms.billing.entity;

public enum InvoiceStatus {
    DRAFT,
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED,
    REFUNDED
}
