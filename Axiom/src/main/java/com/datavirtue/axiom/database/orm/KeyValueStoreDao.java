package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.KeyValueStore;

/**
 *
 * @author SeanAnderson
 */
public class KeyValueStoreDao extends BaseDaoImpl<KeyValueStore, Object> {
    public KeyValueStoreDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, KeyValueStore.class);
    }
}
