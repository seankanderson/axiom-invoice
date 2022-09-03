package com.datavirtue.axiom.models.inventory;

import com.datavirtue.axiom.database.orm.InventoryImageDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
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
@DatabaseTable(tableName = "inventory_images", daoClass = InventoryImageDao.class)
public class InventoryImage extends BaseAxiomEntityModel {
     
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] image;
    @DatabaseField
    private String caption;
    @DatabaseField
    private String description;
    @DatabaseField
    private int sortOrder;
    
}
