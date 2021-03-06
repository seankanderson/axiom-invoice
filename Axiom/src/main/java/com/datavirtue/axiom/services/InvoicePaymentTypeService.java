package com.datavirtue.axiom.services;
import com.datavirtue.axiom.database.orm.InvoicePaymentTypeDao;
import com.datavirtue.axiom.models.invoices.InvoicePaymentType;
import com.j256.ormlite.dao.DaoManager;
import java.sql.SQLException;


public class InvoicePaymentTypeService extends BaseService<InvoicePaymentTypeDao, InvoicePaymentType> {

   
    @Override
    public InvoicePaymentTypeDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InvoicePaymentType.class);
    }

    public InvoicePaymentType getTypeByName(String name) throws SQLException {
         var types =  this.getDao().queryForEq("name", name);
         return types == null ? null : types.get(0);         
    }
    
   
}
