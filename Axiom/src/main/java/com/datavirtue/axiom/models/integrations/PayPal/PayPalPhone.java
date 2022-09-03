package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalPhone {
    private String countryCode;
    private String nationalNumber;
    private String phoneType;
}
