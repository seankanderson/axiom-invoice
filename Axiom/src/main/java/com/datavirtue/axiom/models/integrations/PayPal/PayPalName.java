package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalName {
    private String alternateFullName;
    private String fullName;
    private String givenName;
    private String middleName;
    private String prefix;
    private String suffix;
    private String surname;
}
