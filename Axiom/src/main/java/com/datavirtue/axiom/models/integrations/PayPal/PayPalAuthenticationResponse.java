package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalAuthenticationResponse {
    private String scope;
    private String accessToken;
    private String tokenType;
    private String appId;
    private long expiresIn;
    private String nonce;    
}
