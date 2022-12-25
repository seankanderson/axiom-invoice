package com.datavirtue.axiom.services;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import java.sql.SQLException;
import java.util.List;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 * @param <T1> database entity-typed dao implementation
 * @param <T2> database model entity type
 */
public abstract class BaseService<T1 extends BaseDaoImpl<T2, UUID>, T2 extends BaseAxiomEntityModel> implements BaseServiceInterface<T1> {
    
    @Inject
    @Named("DatabaseConnection")
    protected JdbcConnectionSource connection;
    
//    protected JdbcConnectionSource getDatabaseConnection() {
//        JdbcConnectionSource conn;
//        try {
//             conn = DatabaseService.getConnection();
//             return conn;
//        }catch(SQLException e){
//            System.out.println(e.getSQLState());
//        }
//        return null;
//    }    
    
    public List<T2> getAll() throws SQLException {
        return this.getDao().queryForAll();
    }

    public int save(T2 item) throws SQLException {
        if (item.getId() == null) {
            return this.getDao().create(item);
        } else {
            return this.getDao().update(item);
        }
    }
    
    public int delete(T2 item) throws SQLException {
        if (item.getId() != null) {
            return this.getDao().delete(item);
        }else {
            return 0;
        }
    }  
    
    public List<T2> searchField(String fieldName, String forValue) throws SQLException {
        return this.getDao().queryForEq(fieldName, forValue);
    }
}
