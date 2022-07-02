package com.datavirtue.axiom.services;

import com.datavirtue.axiom.database.orm.ContactJournalDao;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import com.datavirtue.axiom.models.contacts.ContactJournal;
import com.j256.ormlite.dao.DaoManager;

/**
 *
 * @author SeanAnderson
 */
public class ContactJournalService extends BaseService<ContactJournalDao, ContactJournal> {
    
    public ContactJournalService() {
        
    }    
    
    public List<ContactJournal> getJournalsForContact(UUID contactId) throws SQLException  {
        return this.getDao().queryForEq("contact_id", contactId);
    }
    
    @Override
    public ContactJournalDao getDao() throws SQLException {
        return DaoManager.createDao(connection, ContactJournal.class);
    }
    
}
