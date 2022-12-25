package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.contacts.ContactAddress;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class ContactAddressDao extends BaseDaoImpl<ContactAddress, UUID> implements ContactAddressDaoInterface {
    public ContactAddressDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ContactAddress.class);
    }
    
}
