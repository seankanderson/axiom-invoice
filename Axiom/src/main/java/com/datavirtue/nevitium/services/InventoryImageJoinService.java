package com.datavirtue.nevitium.services;

import com.datavirtue.nevitium.database.orm.InventoryImageDao;
import com.datavirtue.nevitium.database.orm.InventoryImageJoinDao;
import java.util.List;
import java.sql.SQLException;
import com.datavirtue.nevitium.models.inventory.Inventory;
import com.datavirtue.nevitium.models.inventory.InventoryImage;
import com.datavirtue.nevitium.models.inventory.InventoryImageJoin;
import com.google.inject.Inject;
import com.j256.ormlite.dao.DaoManager;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class InventoryImageJoinService extends BaseService<InventoryImageJoinDao, InventoryImageJoin> {

            
    @Inject
    private AppSettingsService appSettings;
    
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
    
    public void saveImage(InventoryImage image) {
        inventoryImageDao
    }

    public List<Inventory> getAllInventoryByCode(String code) throws SQLException {
        return this.getDao().queryForEq("code", code);
    }

    public List<Inventory> getAllInventoryByCategory(String category) throws SQLException {
        var result = this.getDao().queryBuilder().where().like("category", "%" + category + "%");
        return result.query();
    }

    public List<Inventory> getAllInventoryBySize(String size) throws SQLException {
        var result = this.getDao().queryBuilder().where().like("size", "%" + size + "%");
        return result.query();
    }

    public List<Inventory> getAllInventoryByWeight(String weight) throws SQLException {
        var result = this.getDao().queryBuilder().where().like("weight", "%" + weight + "%");
        return result.query();
    }

    public String[] getInventoryCategories() throws SQLException {
        var results = this.getDao().queryBuilder().distinct().selectColumns("category").query();
        return results.stream().map(p -> p.getCategory()).toArray(size -> new String[results.size()]);
    }

    public List<Inventory> getAllInventoryByDecription(String desc) throws SQLException {
        return this.getDao().queryForEq("description", desc);
    }
    
    public boolean doesInventoryByDescriptionExist(String description) throws SQLException {
        return (this.getDao().queryBuilder().where().eq("description", description).countOf() > 0);
    }

    public double quantityAvailableNow(UUID inventoryId) throws SQLException {
        var result = this.getDao().queryForId(inventoryId);
        return result.getQuantity();
    }
    
    public Inventory getInventoryById(UUID inventoryId) throws SQLException {
        var result = this.getDao().queryForId(inventoryId);
        return result;
    }
    
    public void deleteInventory(Inventory inventory) throws SQLException {
        this.getDao().delete(inventory);
//        TransactionManager.callInTransaction(connection, new Callable<Void>() {
//            public Void call() throws Exception {
//                // delete both objects but make sure that if either one fails, the transaction is rolled back
//                // and both objects are "restored" to the database
//                getDao().delete(inventory);
//                //barDao.delete(bar);
//                return null;
//            }
//        });
    }

    public double calculateMarkup(double cost) throws SQLException {
        
        var settings = appSettings.getObject().getInventory();
        var markupFactor = settings.getDefaultProductMarkupFactor();     
        if (markupFactor == 0) { 
            markupFactor = 1; // failsafe to prevent returning zero
        }
        return cost * markupFactor;
    }

    @Override
    public InventoryImageJoinDao getDao() throws SQLException {
        return DaoManager.createDao(connection, InventoryImageJoin.class);
    }

}
