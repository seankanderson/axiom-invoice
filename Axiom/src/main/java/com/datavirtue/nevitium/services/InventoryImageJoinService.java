package com.datavirtue.nevitium.services;

import com.datavirtue.nevitium.database.orm.InventoryImageJoinDao;
import java.util.List;
import java.sql.SQLException;
import com.datavirtue.nevitium.models.inventory.InventoryImage;
import com.datavirtue.nevitium.models.inventory.InventoryImageJoin;
import com.j256.ormlite.dao.DaoManager;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InventoryImageJoinService extends BaseService<InventoryImageJoinDao, InventoryImageJoin> {

       
    public InventoryImageJoinService() {

    }

    public List<InventoryImage> getAllImagesForInventory(UUID inventoryId) throws SQLException {
        var images = this.getDao().queryForEq("inventory_id", inventoryId);
        var imageList = new ArrayList<InventoryImage>();
        for(var image : images) {
            imageList.add(image.getImage());
        }
        return imageList;
    }
    
    @Override
    public InventoryImageJoinDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InventoryImageJoin.class);
    }

}
