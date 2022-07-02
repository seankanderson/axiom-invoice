package com.datavirtue.axiom.services;

import com.datavirtue.axiom.database.orm.InvoiceCustomerInfoDao;
import com.datavirtue.axiom.models.invoices.InvoiceCustomerInfo;
import com.j256.ormlite.dao.DaoManager;
import java.sql.SQLException;

/**
 *
 * @author SeanAnderson
 */
public class InvoiceCustomerInfoService extends BaseService<InvoiceCustomerInfoDao, InvoiceCustomerInfo> {
    
    @Override
    public InvoiceCustomerInfoDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InvoiceCustomerInfo.class);
    }
    
}
