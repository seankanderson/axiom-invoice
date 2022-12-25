
package com.datavirtue.axiom.database.orm;

import com.datavirtue.axiom.models.inventory.Inventory;
import com.j256.ormlite.dao.Dao;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface InventoryDaoInterface extends Dao<Inventory, UUID> {
}

