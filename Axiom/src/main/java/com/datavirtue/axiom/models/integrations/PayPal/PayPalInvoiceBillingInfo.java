package com.datavirtue.axiom.models.integrations.PayPal;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoiceBillingInfo {
    private PayPalName name;
    private PayPalAddress address;
    private ArrayList<PayPalPhone> phones;
    private String emailAddress;
}
