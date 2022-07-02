package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.contacts.Contact;

/**
 *
 * @author SeanAnderson
 */
public class ContactDao extends BaseDaoImpl<Contact, Object> implements ContactDaoInterface {
    
    public ContactDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Contact.class);
    }
    
    
}
