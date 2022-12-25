package com.datavirtue.axiom.services;

import com.datavirtue.axiom.database.orm.InventoryImageDao;
import java.util.List;
import java.sql.SQLException;
import com.datavirtue.axiom.models.inventory.Inventory;
import com.datavirtue.axiom.models.inventory.InventoryImage;
import com.datavirtue.axiom.models.inventory.InventoryImageJoin;
import com.google.inject.Inject;
import com.j256.ormlite.dao.DaoManager;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InventoryImageService extends BaseService<InventoryImageDao, InventoryImage> {

            
    @Inject
    private AppSettingsService appSettings;
    
    @Inject
    private InventoryImageJoinService joinService;
    
    @Inject
    private InventoryService inventoryService;
    
    public InventoryImageService() {

    }

    public List<InventoryImage> getAllImagesForInventory(UUID inventoryId) throws SQLException {
        var results = joinService.getAllImagesForInventory(inventoryId);
        return results;
    }
    
    /**
     * Attempts to create a link between an Inventory item and an InventoryImage. Inventory and Inventory image are saved ONLY if the IDs are null--in order to create the link.
     * @param inventory Inventory is saved if the ID is null
     * @param image InventoryImage is saved if the ID is null
     * @throws SQLException 
     */
    public void saveIfNewAndlinkImageToInventory(Inventory inventory, InventoryImage image) throws SQLException {
        // TODO: implement transaction
        
        if (inventory.getId() == null) {
            inventoryService.save(inventory);
        }
        
        if (image.getId() == null) {
            this.save(image);            
        }
        
        var imageJoin = new InventoryImageJoin();
        imageJoin.setInventory(inventory);
        imageJoin.setImage(image);
        joinService.save(imageJoin);
        
    }
    
    @Override
    public InventoryImageDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InventoryImage.class);
    }

}
