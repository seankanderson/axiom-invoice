/*
 * Copyright (C) 2022 sean.anderson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoicePaymentRequest {
    private PaymentMethod method;
    public static enum PaymentMethod {
        BANK_TRANSFER,
        CASH,
        CHECK,
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        WIRE_TRANSFER,
        OTHER
    }
    private PayPalMoney amount;
    private String note;
    private String paymentDate; // yyyy-MM-dd z
    private String paymentId;
    private PayPalContactInformation shipping_info;
    private PaymentType type; 
    public static enum PaymentType {
        PAYPAL,
        EXTERNAL
    }
}
