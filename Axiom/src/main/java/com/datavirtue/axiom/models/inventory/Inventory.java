package com.datavirtue.axiom.models.inventory;

import com.datavirtue.axiom.database.orm.InventoryDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "inventory", daoClass = InventoryDao.class)
public class Inventory extends BaseAxiomEntityModel{    
    @DatabaseField
    private String upc;
    @DatabaseField
    private String code;
    @DatabaseField(canBeNull = false)
    private String description;
    @DatabaseField
    private String size;
    @DatabaseField
    private String weight;
    @DatabaseField
    private Double quantity;
    @DatabaseField
    private Double cost;
    @DatabaseField
    private Double price;    
    @DatabaseField
    private String category;    
    @DatabaseField
    private boolean tax1;
    @DatabaseField
    private boolean tax2;
    @DatabaseField
    private boolean available;
    @DatabaseField
    private Date lastSale = new Date();
    @DatabaseField
    private Date lastReceived = new Date();
    @DatabaseField
    private int reorderCutoff;
    @DatabaseField
    private boolean partialSaleAllowed;
    
    private Collection<InventoryImage> images = new ArrayList();
}
