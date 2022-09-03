package com.datavirtue.axiom.models.integrations.PayPal;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class PayPalMetadata {
    private String createTime;
    private String createdBy;
    private String lastUpdateTime;
    private String lastUpdatedBy;
    private String cancelTime;
    private String cancelledBy;
    private String createdByFlow;
    private String firstSentTime;
    private String invoicerViewUrl;
    private String lastSentBy;
    private String lastSentTime;
    private String recipientViewUrl;
    
}
