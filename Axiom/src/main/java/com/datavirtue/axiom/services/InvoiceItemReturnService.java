


package com.datavirtue.axiom.services;

import com.datavirtue.axiom.database.orm.InvoiceItemReturnDao;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.models.invoices.InvoiceItemReturn;
import com.datavirtue.axiom.models.settings.AppSettings;
import com.google.inject.Inject;
import com.j256.ormlite.dao.DaoManager;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author SeanAnderson
 */
public class InvoiceItemReturnService extends BaseService<InvoiceItemReturnDao, InvoiceItemReturn>{

    @Inject
    private AppSettingsService appSettingsService;
    private AppSettings appSettings;
        
    @Override
    public InvoiceItemReturnDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InvoiceItemReturn.class);
    }
    
    public List<InvoiceItemReturn> getReturnsForInvoice(Invoice invoice) throws SQLException {
        var query =  this.getDao().queryBuilder().where().eq("invoiceId", invoice.getId()); 
        return query.query();
    }

}
