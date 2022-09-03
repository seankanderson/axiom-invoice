package com.datavirtue.axiom.models.integrations.PayPal;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoice {
    
    @Expose (serialize = false, deserialize = true) 
    private String id;
    
    private PayPalInvoiceDetail detail;
    private ArrayList<String> additionalRecipients;
    private PayPalAmountBreakdown amount;
    private PayPalInvoiceConfiguration configuration;
    private PayPalMoney dueAmount;
    private PayPalMoney gratuity;
    private PayPalInvoicer invoicer;
    
    
    private ArrayList<PayPalInvoiceItem> items;
    private ArrayList<PayPalLinkDescription> links;
    
    private String parentId;
    
    private PayPalInvoicePayments payments;
    
    private ArrayList<PayPalInvoiceRecipient> primaryRecipients;
    
    private PayPalInvoiceRefunds refunds;
    
    private String status;
}
