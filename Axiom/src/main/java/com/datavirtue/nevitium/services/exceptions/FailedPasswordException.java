

package com.datavirtue.axiom.services.exceptions;

/**
 *
 * @author SeanAnderson
 */
public class FailedPasswordException extends Exception {

    @Override 
    public String getMessage() {
        return "Failed password attemp.";
    }
}
