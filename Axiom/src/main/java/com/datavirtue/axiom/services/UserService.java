package com.datavirtue.axiom.services;

import com.datavirtue.axiom.database.orm.UserDao;
import java.sql.SQLException;
import com.datavirtue.axiom.models.security.AxiomUser;
import com.datavirtue.axiom.services.exceptions.DuplicateUserNameException;
import com.datavirtue.axiom.services.util.PBE;
import com.j256.ormlite.dao.DaoManager;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author SeanAnderson
 */
public class UserService extends BaseService<UserDao, AxiomUser> {

    public static String SECURITY_DISABLED = "Security is disabled";
    public static String USER_NOT_FOUND = "User does not exist";
    public static String PASSWORD_FAILED = "Incorrect password";
    public static String NOT_AUTHORIZED = "Not authorized";
    public static String PASSWORD_SUCCESS = "User authorized";
    
    private static AxiomUser currentUser;
    private static String defaultUsername = "admin";
    public static void setCurrentUser(AxiomUser user){
        currentUser = user;
    }
    public static AxiomUser getCurrentUser() {
        
        if (currentUser == null) {
            currentUser = new AxiomUser();
            currentUser.setAdmin(true);
            currentUser.setUserName(defaultUsername);
        }
        return currentUser;
    }
    
    public static void logoutCurrentUser() {
        currentUser = null;
    }
    
    @Override
    public UserDao getDao() throws SQLException {
        return DaoManager.createDao(connection, AxiomUser.class);
    }

    public AxiomUser getRootAdminUser() throws SQLException {
        var results = this.getDao().queryForEq("username", "admin");
        if (results == null || results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    public boolean isSecurityEnabled() throws SQLException {
        var results = this.getDao().queryForEq("username", "admin");
        if (results == null || results.size() == 0) {
            return false;
        }
        return !StringUtils.isEmpty(results.get(0).getPassword());
    }

    public String authenticateUser(String username, char[] password) throws DuplicateUserNameException, SQLException, Exception {
                        
        var users = this.getDao().queryForEq("username", username);
        if (users == null || users.size() == 0) {
            return USER_NOT_FOUND;
        }
        if (users.size() > 1) {
            throw new DuplicateUserNameException();
        }
        
        
        var user = users.get(0);

        var cipher = user.getPassword();

        String decrypted = null;
        try {
            decrypted = PBE.decrypt(password, cipher);
        } catch (Exception ex) {            
            throw ex; // decryption issue probably stemming from unsigned library etc...
        }
        if (decrypted != null) {
            var pw = new String(password);
            if (decrypted.equals(pw)) {
                currentUser = user;
                return PASSWORD_SUCCESS;
            } else {
                return PASSWORD_FAILED;
            }
        }else {
            return PASSWORD_FAILED;
        }        
    }

}
