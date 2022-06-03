package com.datavirtue.nevitium.models.inventory;

import com.datavirtue.nevitium.database.orm.InventoryImageJoinDao;
import com.datavirtue.nevitium.models.BaseModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "inventory_image_join", daoClass = InventoryImageJoinDao.class)
public class InventoryImageJoin extends BaseModel {
    
    public InventoryImageJoin() {
    }
    
    @DatabaseField(foreign=true,foreignAutoRefresh=false, canBeNull = false)
    private Inventory inventory;
    
    @DatabaseField(foreign=true,foreignAutoRefresh=false, canBeNull = false)
    private InventoryImage image;
    
}
