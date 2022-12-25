package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.KeyValueStore;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface AppSettingsDaoInterface extends Dao<KeyValueStore, UUID> {
    
}
