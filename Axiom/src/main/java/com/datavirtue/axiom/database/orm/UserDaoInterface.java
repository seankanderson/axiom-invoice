package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.security.AxiomUser;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface UserDaoInterface extends Dao<AxiomUser, UUID>{
    
}
