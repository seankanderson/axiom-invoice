package com.datavirtue.axiom.models.invoices;

import com.datavirtue.axiom.database.orm.InvoiceDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter
@Setter
@DatabaseTable(tableName = "invoices", daoClass = InvoiceDao.class)
public class Invoice extends BaseAxiomEntityModel {

    @DatabaseField
    private String invoiceNumber;
    
    @DatabaseField
    private Date invoiceDate;
    
    @DatabaseField
    private Date dueDate;
    
    @DatabaseField
    private String paymentTerms;
    
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private InvoiceCustomerInfo billTo;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private InvoiceCustomerInfo shipTo;

    @DatabaseField
    private UUID customerId;
    
    @ForeignCollectionField(eager = true)
    private Collection<InvoiceItem> items;
    
    @DatabaseField
    private InvoiceStatus status;    
    public enum InvoiceStatus {
        QUOTE, // this status is exclusive to quotes or "saved invoices"
        NEW_OPEN, // invoice is open and awaiting payment
        OPEN_UNPAID, // invoice has passed due date without being paid in full
        OPEN_OVERPAID, // refunds, returns and excessive payments can cause this status  
        PAID, // invoice is closed and paid as agreed        
        LOSS, // invoice was not fully paid or not paid at all...the remaining balance is the write-off amount
        VOID // the way we delete an invoice
    }
    
    /*
    
    Possible invoice status flows...
    NEW_OPEN -> OPEN_UNPAID -> PAID
             L> PAID        L> OPEN_OVERPAID
             L> LOSS        L> LOSS
             L> VOID        L> VOID    
    
    */
    
    @DatabaseField
    private String customerNote;
    
    @DatabaseField
    private String invoiceMessage; 
        
    @DatabaseField
    private String purchaseOrderReference;

    @DatabaseField
    private String otherReference;

    @DatabaseField
    private String internalMemo;   
    
    @ForeignCollectionField(eager = true)
    private Collection<InvoiceItemReturn> returns;
    
    @ForeignCollectionField(eager = true)
    private Collection<InvoicePayment> payments;

}
