package com.datavirtue.nevitium.models.inventory;

import com.datavirtue.nevitium.database.orm.InventoryImageDao;
import com.datavirtue.nevitium.models.BaseModel;
import com.j256.ormlite.field.DataType;
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
@DatabaseTable(tableName = "inventory_images", daoClass = InventoryImageDao.class)
public class InventoryImage extends BaseModel {
     
    @ForeignCollectionField(eager = false)
    private Collection<InventoryImageJoin> images = new ArrayList();
    
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] image;
    @DatabaseField
    private String caption;
    @DatabaseField
    private String description;
    @DatabaseField
    private int sortOrder;
    
}
