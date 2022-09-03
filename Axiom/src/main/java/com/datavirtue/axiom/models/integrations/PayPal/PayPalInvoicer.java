
package com.datavirtue.axiom.models.integrations.PayPal;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalInvoicer {
    private PayPalName name;
    private String emailAddress;
    private PayPalAddress address;
    private ArrayList<PayPalPhone> phones;
    private String website;
    private String taxId;
    private String logoUrl;
    private String additionalNotes;
    
    
}
