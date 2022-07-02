package com.datavirtue.axiom.models.invoices;

import com.datavirtue.axiom.database.orm.InvoicePaymentTypeDao;
import com.datavirtue.axiom.models.BaseModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@DatabaseTable(tableName = "invoice_payment_types", daoClass = InvoicePaymentTypeDao.class)
public class InvoicePaymentType extends BaseModel {

    @DatabaseField
    private String name;
    @DatabaseField
    private String description;
    @DatabaseField
    private boolean invoiceDebit;
    @DatabaseField
    private boolean invoiceCredit;
        
}
