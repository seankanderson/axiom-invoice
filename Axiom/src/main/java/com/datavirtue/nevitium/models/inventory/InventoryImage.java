package com.datavirtue.nevitium.models.inventory;

import com.datavirtue.nevitium.models.BaseModel;
import com.datavirtue.nevitium.models.contacts.Contact;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "inventory_images", daoClass = InventoryImage.class)
public class InventoryImage extends BaseModel {
     
    @DatabaseField(foreign=true,foreignAutoRefresh=false, canBeNull = false)
    private Contact inventory;
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] image;
    @DatabaseField
    private String caption;
    @DatabaseField
    private String description;
    @DatabaseField
    private int sortOrder;
    
}
