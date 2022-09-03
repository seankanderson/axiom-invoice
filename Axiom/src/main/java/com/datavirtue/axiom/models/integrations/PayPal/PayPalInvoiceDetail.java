
package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoiceDetail {
        
    private String currencyCode;
    private PayPalFileReference attachments;
    private String memo;
    private String note;
    private String reference;
    private String termsAndConditions;
    private String invoiceDate;  //^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])$.
        
    private String id;  // response only
    private String invoiceNumber;
    private PayPalMetadata metadata;
    private PayPalInvoicePaymentTerm paymentTerm;
    
    
}
