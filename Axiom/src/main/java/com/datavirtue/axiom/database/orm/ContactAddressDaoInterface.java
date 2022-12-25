package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.contacts.ContactAddress;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface ContactAddressDaoInterface  extends Dao<ContactAddress, UUID>{
    
}
