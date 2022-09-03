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
package com.datavirtue.axiom.services.integrations;

import com.datavirtue.axiom.models.integrations.PayPal.PayPalAmountBreakdown;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoice;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoiceBillingInfo;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoiceDetail;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoiceItem;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoicePaymentTerm;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoiceRecipient;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalMoney;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.services.FormatService;
import com.datavirtue.axiom.services.InvoiceService;
import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter
@Setter
public class PayPalService {

    @Inject
    private InvoiceService invoiceService;

    public PayPalInvoice mapAxiomInvoiceToPayPalInvoice(Invoice axiomInvoice) throws SQLException {
        var payPalInvoice = new PayPalInvoice();
        // add item(s)
        var item = new PayPalInvoiceItem();
        item.setName("Reference Invoice# INV-4515168541");
        var price = new PayPalMoney();
        price.setCurrencyCode("USD");
        price.setValue(FormatService.doubleToFormattedString(invoiceService.calculateInvoiceAmountDue(axiomInvoice)));
        item.setUnitAmount(price);
        item.setUnitOfMeasure("AMOUNT");
        item.setQuantity("1");
        var items = new ArrayList<PayPalInvoiceItem>();
        items.add(item);
        payPalInvoice.setItems(items);

        // set amount
        var invoiceAmount = new PayPalAmountBreakdown();
        invoiceAmount.setItemTotal(price);
        payPalInvoice.setAmount(invoiceAmount);

        // set a due date
        var paymentTerm = new PayPalInvoicePaymentTerm();
        paymentTerm.setDueDate("2022-09-01");

        // set invoice details
        var detail = new PayPalInvoiceDetail();
        detail.setPaymentTerm(paymentTerm);
        detail.setCurrencyCode("USD");
        detail.setMemo("Test memo...include invoice reference details here?");
        detail.setNote("What is this note good for?");
        detail.setReference("INV-315314351");
        payPalInvoice.setDetail(detail);

        // set recipient(s)
        var billingInfo = new PayPalInvoiceBillingInfo();
        billingInfo.setEmailAddress("sean.anderson@gmail.com");
        var recipient = new PayPalInvoiceRecipient();
        recipient.setBillingInfo(billingInfo);
        var recipients = new ArrayList<PayPalInvoiceRecipient>();
        recipients.add(recipient);
        payPalInvoice.setPrimaryRecipients(recipients);

        return payPalInvoice;

    }

}
