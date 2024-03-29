package com.datavirtue.axiom.models.invoices;

import com.datavirtue.axiom.database.orm.InvoicePaymentDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter
@Setter
@DatabaseTable(tableName = "invoice_payments", daoClass = InvoicePaymentDao.class)
public class InvoicePayment extends BaseAxiomEntityModel {

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Invoice invoice;

    @DatabaseField
    private Date paymentEffectiveDate = new Date();

    @DatabaseField
    private Date paymentActivityDate = new Date();

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private InvoicePaymentType paymentType;

    @DatabaseField
    private String memo;

    @DatabaseField
    private double debit;
    @DatabaseField
    private double credit;

    @DatabaseField
    private String paymentSystem;

    @DatabaseField
    private String paymentSystemReference;

       

}
