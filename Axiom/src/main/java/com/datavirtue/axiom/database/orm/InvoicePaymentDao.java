package com.datavirtue.axiom.database.orm;


import com.datavirtue.axiom.models.invoices.InvoicePayment;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InvoicePaymentDao extends BaseDaoImpl<InvoicePayment, UUID> implements InvoicePaymentDaoInterface{
    public InvoicePaymentDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, InvoicePayment.class);
        
    }
}
