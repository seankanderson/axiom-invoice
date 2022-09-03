package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoiceRecipient {
    private PayPalInvoiceBillingInfo billingInfo;
    private PayPalContactInformation shippingInfo;
    
}
