
package com.datavirtue.axiom.services.integrations;

import java.util.Base64;

/**
 *
 * @author sean.anderson
 */
public class HttpHelper {
    public static String getBasicAuthorizationHeaderValue(String username, String password) {
        String valueToEncode = username + ":" + password;
        var auth = "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
        return auth;

    }
    
      
}
