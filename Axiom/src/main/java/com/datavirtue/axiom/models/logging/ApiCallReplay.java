/*
 * Copyright (C) 2022 sean.anderson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.

    September 4th, 2022
 */

package com.datavirtue.axiom.models.logging;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * ApiCallReplay enables an asynchronous attempt to make API calls for integrations where the action/call is updating a system. GET calls are not treated or handled.
 * The replay engine should be able to execute a APiCallReplay and invoke the requisit business logic that is mapped in various properties of the class.
 * Are concerns being mixed? API call retry and business logic tracking? The busines sl ogic involves the timeing and retry status, so it seems appropo 
 * 
 * @author sean.anderson
 */
@Getter @Setter
public class ApiCallReplay {
    
    private String businessActionName; // a key that denotes a business action: PayPalRecordPaymentAgainstInvoice
    
    
    /** 
     * Describes business scenario entities so that duplicates or offsetting transactions cannot be put into the queue.
     * 
     * Logic is yet undetermined but should map to the businessActionName so that automatic 
     * decisions about whether or not to insert calls into the queue which may be affected by earlier queued items
     * examine entities involved, if a payment is made and then a delete is attempted, the just expire both, for instance
     * 
     * 
     * Interface to contain "post processing" call that is passed the response?
     * ReplayEngine.
     * 
    */
    private String businessActionObject; // cast and handled/de-serialized as a type by the caller...
    
    private Date initialFailureAttempt;
    private String initialFailureReason; // disconnected, bad credeials, etc...
      
    private String httpRequest; //serialized JSON of java.net.http.HttpRequest
    
    private int maxRetryCount;
    private int retryCount = 0;
    private int exponentialBackoffInHours; // retry count X exponentialBackoffInHours 
    
    
    private Date completedAttempt;
    
    private Date attemptExpiration; // the last dateTime an attempt is valid...cancels attempts
    
    private Date finalFailureAttempt; 
    private String finalFailureReason; // reached expiration, reached retry count, user cancelled, etc...
        
}
