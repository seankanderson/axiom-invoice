package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.datavirtue.axiom.models.inventory.InventoryImageJoin;
import java.sql.SQLException;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */

public class InventoryImageJoinDao extends BaseDaoImpl<InventoryImageJoin, UUID> implements InventoryImageJoinDaoInterface{
    
    public InventoryImageJoinDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, InventoryImageJoin.class);
    }
    
}
