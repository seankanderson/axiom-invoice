package com.datavirtue.axiom.models.invoices;

import com.datavirtue.axiom.database.orm.InvoiceDao;
import com.datavirtue.axiom.models.BaseModel;
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
public class Invoice extends BaseModel {

    @DatabaseField
    private String invoiceNumber;
    @DatabaseField
    private Date invoiceDate;
    
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private InvoiceCustomerInfo billTo;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private InvoiceCustomerInfo shiptTo;

    @DatabaseField
    private UUID customerId;
    @DatabaseField
    private boolean voided;
    @DatabaseField
    private boolean paid;
    @DatabaseField
    private String message;    
    @DatabaseField
    private boolean isQuote;
    
    @DatabaseField
    private String purchaseOrder;    

    @ForeignCollectionField(eager = true)
    private Collection<InvoiceItem> items;

    @ForeignCollectionField(eager = true)
    private Collection<InvoiceItemReturn> returns;
    
    @ForeignCollectionField(eager = true)
    private Collection<InvoicePayment> paymentActivity;

}
