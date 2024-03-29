

package com.datavirtue.axiom.database.orm;

import com.datavirtue.axiom.models.invoices.InvoiceItem;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InvoiceItemDao extends BaseDaoImpl<InvoiceItem, UUID> implements InvoiceItemDaoInterface{
    public InvoiceItemDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, InvoiceItem.class);
        
    }
}
