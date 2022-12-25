package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.contacts.Contact;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface ContactDaoInterface extends Dao<Contact, UUID> {
    
}