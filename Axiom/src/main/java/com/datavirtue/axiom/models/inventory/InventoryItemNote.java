package com.datavirtue.axiom.models.inventory;

import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "inventory_item_notes")
public class InventoryItemNote extends BaseAxiomEntityModel{
    
    @DatabaseField
    private UUID inventoryItemId;
    @DatabaseField
    private Date date = new Date();
    @DatabaseField
    private String note;
    
}
