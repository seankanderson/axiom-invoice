package com.datavirtue.axiom.services.exceptions;


public class PartialQuantityException extends Exception { 
        public PartialQuantityException(String message, Throwable error) {
            super(message, error);
        }
        
        public PartialQuantityException(String message) {
            super(message);
        }
    }

