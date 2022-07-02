package com.datavirtue.axiom.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.param.PaymentLinkCreateParams;

/**
 *
 * @author SeanAnderson
 */
public class PaymentsService {

    public void createStripePaymentLinkForInvoice(String invoiceNumber, Double amount) throws StripeException {
    }
    
}
