package com.datavirtue.nevitium.models.inventory;

import com.datavirtue.nevitium.models.BaseModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "inventory_image_join", daoClass = InventoryImageJoinDao.class)
public class InventoryImageJoin extends BaseModel {
    @DatabaseField(foreign=true,foreignAutoRefresh=false, canBeNull = false)
    private Inventory inventory;
     @ForeignCollectionField(eager = false)
    private Collection<InventoryImage> images = new ArrayList();
}
