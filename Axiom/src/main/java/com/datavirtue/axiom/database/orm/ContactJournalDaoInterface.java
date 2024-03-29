package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.contacts.ContactJournal;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface ContactJournalDaoInterface extends Dao<ContactJournal, UUID> {
    
}