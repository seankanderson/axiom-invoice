package com.datavirtue.axiom.models.inventory;

import com.datavirtue.axiom.database.orm.InventoryImageJoinDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
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
public class InventoryImageJoin extends BaseAxiomEntityModel {
    
    public InventoryImageJoin() {
    }
    
    @DatabaseField(foreign=true,foreignAutoRefresh=false, canBeNull = false)
    private Inventory inventory;
    
    @DatabaseField(foreign=true,foreignAutoRefresh=true, canBeNull = false)
    private InventoryImage image;
    
}
