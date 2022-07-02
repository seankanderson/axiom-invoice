


package com.datavirtue.axiom.services.exceptions;

public class InvoiceVoidedException extends Exception { 
        public InvoiceVoidedException(String message, Throwable error) {
            super(message, error);
        }
        
        public InvoiceVoidedException(String message) {
            super(message);
        }
    }

