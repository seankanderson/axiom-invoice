package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.invoices.Invoice;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InvoiceDao extends BaseDaoImpl<Invoice, UUID> implements InvoiceDaoInterface {
    public InvoiceDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Invoice.class);
    }
}
