package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.datavirtue.axiom.models.inventory.InventoryImageJoin;
import java.sql.SQLException;

/**
 *
 * @author SeanAnderson
 */

public class InventoryImageJoinDao extends BaseDaoImpl<InventoryImageJoin, Object> implements InventoryImageJoinDaoInterface{
    
    public InventoryImageJoinDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, InventoryImageJoin.class);
    }
    
}
