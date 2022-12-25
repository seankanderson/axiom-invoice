package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.contacts.ContactJournal;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class ContactJournalDao extends BaseDaoImpl<ContactJournal, UUID> implements ContactJournalDaoInterface {
    
    public ContactJournalDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ContactJournal.class);
    }
    
    
}
