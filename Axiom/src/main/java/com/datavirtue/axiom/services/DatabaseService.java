package com.datavirtue.axiom.services;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.prefs.BackingStoreException;
import com.datavirtue.axiom.models.KeyValueStore;
import com.datavirtue.axiom.models.contacts.Contact;
import com.datavirtue.axiom.models.contacts.ContactAddress;
import com.datavirtue.axiom.models.contacts.ContactJournal;
import com.datavirtue.axiom.models.inventory.Inventory;
import com.datavirtue.axiom.models.inventory.InventoryImage;
import com.datavirtue.axiom.models.inventory.InventoryImageJoin;
import com.datavirtue.axiom.models.inventory.InventoryItemNote;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.models.invoices.InvoiceCustomerInfo;
import com.datavirtue.axiom.models.invoices.InvoiceItem;
import com.datavirtue.axiom.models.invoices.InvoiceItemReturn;
import com.datavirtue.axiom.models.invoices.InvoiceMessages;
import com.datavirtue.axiom.models.invoices.InvoicePayment;
import com.datavirtue.axiom.models.invoices.InvoicePaymentType;
import com.datavirtue.axiom.models.security.User;
import com.datavirtue.axiom.models.security.UserAudit;

/**
 *
 * @author SeanAnderson
 */
public class DatabaseService {
    private static JdbcConnectionSource connectionSource;
    
    public static JdbcConnectionSource getConnection() throws SQLException, BackingStoreException {
        
        var connectionString = LocalSettingsService.getLocalAppSettings().getConnectionString();
                
        if (connectionSource == null) {
            connectionSource = new JdbcConnectionSource(connectionString);            
        }
        return connectionSource;
    }
    
    public static void closeDatabaseConnections() {
        if (connectionSource != null) {
            connectionSource.closeQuietly();
        }
    }
    
    public static void createTables(boolean dropTablesFirst) throws SQLException, BackingStoreException {
        
        getConnection();
        
        if (dropTablesFirst) {
            TableUtils.dropTable(connectionSource, Inventory.class, true);
            TableUtils.dropTable(connectionSource, InventoryItemNote.class, true);
            TableUtils.dropTable(connectionSource, InventoryImage.class, true);
            TableUtils.dropTable(connectionSource, InventoryImageJoin.class, true);
            TableUtils.dropTable(connectionSource, Contact.class, true);
            TableUtils.dropTable(connectionSource, ContactAddress.class, true);
            TableUtils.dropTable(connectionSource, Invoice.class, true);
            TableUtils.dropTable(connectionSource, InvoiceItem.class, true);
            TableUtils.dropTable(connectionSource, InvoiceItemReturn.class, true);
            TableUtils.dropTable(connectionSource, InvoicePayment.class, true);
            TableUtils.dropTable(connectionSource, InvoiceMessages.class, true);
            TableUtils.dropTable(connectionSource, InvoicePaymentType.class, true);
            TableUtils.dropTable(connectionSource, InvoiceCustomerInfo.class, true);
            //TableUtils.dropTable(connectionSource, User.class, true);
            //TableUtils.dropTable(connectionSource, UserAudit.class, true);
            //TableUtils.dropTable(connectionSource, AppConfig.class, true);
        }
        
        TableUtils.createTableIfNotExists(getConnection(), InventoryImageJoin.class); 
        TableUtils.createTableIfNotExists(getConnection(), Inventory.class); 
        TableUtils.clearTable(connectionSource, Inventory.class);
        TableUtils.createTableIfNotExists(getConnection(), InventoryItemNote.class); 
        TableUtils.clearTable(connectionSource, InventoryItemNote.class);
        
        TableUtils.createTableIfNotExists(getConnection(), InventoryImage.class); 
        TableUtils.clearTable(connectionSource, InventoryImage.class);
        
        TableUtils.createTableIfNotExists(getConnection(), Contact.class); 
        TableUtils.clearTable(connectionSource, Contact.class);
        TableUtils.createTableIfNotExists(getConnection(), ContactJournal.class); 
        TableUtils.clearTable(connectionSource, ContactJournal.class);
        TableUtils.createTableIfNotExists(getConnection(), ContactAddress.class); 
        TableUtils.clearTable(connectionSource, ContactAddress.class);
        TableUtils.createTableIfNotExists(getConnection(), Invoice.class); 
        TableUtils.clearTable(connectionSource, Invoice.class);
        
        TableUtils.createTableIfNotExists(getConnection(), InvoiceItem.class); 
        TableUtils.clearTable(connectionSource, InvoiceItem.class);  
        
        TableUtils.createTableIfNotExists(getConnection(), InvoiceItemReturn.class); 
        TableUtils.clearTable(connectionSource, InvoiceItemReturn.class); 
        
        TableUtils.createTableIfNotExists(getConnection(), InvoicePayment.class); 
        TableUtils.clearTable(connectionSource, InvoicePayment.class);  
        
        TableUtils.createTableIfNotExists(getConnection(), InvoicePaymentType.class); 
        TableUtils.clearTable(connectionSource, InvoicePaymentType.class);  
        
        TableUtils.createTableIfNotExists(getConnection(), InvoiceCustomerInfo.class); 
        TableUtils.clearTable(connectionSource, InvoiceCustomerInfo.class);  
        
        
        TableUtils.createTableIfNotExists(getConnection(), InvoiceMessages.class); 
        TableUtils.clearTable(connectionSource, InvoiceMessages.class);
        TableUtils.createTableIfNotExists(getConnection(), User.class); 
        //TableUtils.clearTable(connectionSource, User.class);   
        TableUtils.createTableIfNotExists(getConnection(), KeyValueStore.class); 
        //TableUtils.clearTable(connectionSource, AppConfig.class);  
        TableUtils.createTableIfNotExists(getConnection(), UserAudit.class); 
    }
}
