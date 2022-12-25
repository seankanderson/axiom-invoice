package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.invoices.Invoice;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface InvoiceDaoInterface  extends Dao<Invoice, UUID> {

    
}
