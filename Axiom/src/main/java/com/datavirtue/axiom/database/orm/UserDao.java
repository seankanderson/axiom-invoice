package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.security.AxiomUser;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public class UserDao extends BaseDaoImpl<AxiomUser, UUID> implements UserDaoInterface {
    public UserDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, AxiomUser.class);
    }
    
}
