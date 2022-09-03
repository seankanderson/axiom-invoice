package com.datavirtue.axiom.models;

import com.datavirtue.axiom.database.orm.KeyValueStoreDao;
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
@DatabaseTable(tableName = "key_value_store", daoClass = KeyValueStoreDao.class)
public class KeyValueStore extends BaseAxiomEntityModel {
    
    @DatabaseField(canBeNull = false, unique = true)
    private String key;
    @DatabaseField(dataType = DataType.LONG_STRING)
    private String value;
}
