package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import com.datavirtue.axiom.models.security.User;

/**
 *
 * @author SeanAnderson
 */
public class UserDao extends BaseDaoImpl<User, Object> implements UserDaoInterface {
    public UserDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, User.class);
    }
    
}
