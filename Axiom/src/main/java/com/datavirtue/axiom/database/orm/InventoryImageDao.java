package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.datavirtue.axiom.models.inventory.InventoryImage;
import java.sql.SQLException;

/**
 *
 * @author SeanAnderson
 */

public class InventoryImageDao extends BaseDaoImpl<InventoryImage, Object> implements InventoryImageDaoInterface{
    
    public InventoryImageDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, InventoryImage.class);
    }
    
}
