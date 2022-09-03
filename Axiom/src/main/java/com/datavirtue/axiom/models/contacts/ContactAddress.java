package com.datavirtue.axiom.models.contacts;

import com.datavirtue.axiom.database.orm.ContactAddressDao;
import com.datavirtue.axiom.models.BaseAxiomEntityModel;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author SeanAnderson
 */
@Getter @Setter
@DatabaseTable(tableName = "contact_addresses", daoClass = ContactAddressDao.class)
public class ContactAddress extends BaseAxiomEntityModel implements ContactAddressInterface {
     
    @DatabaseField(foreign=true,foreignAutoRefresh=true, canBeNull = false)
    private Contact contact;
    
    @DatabaseField
    private String phoneNumber;
    @DatabaseField
    private String emailAddress;
    @DatabaseField
    private String company;
    @DatabaseField
    private String companyName;
    @DatabaseField
    private String contactName;
    @DatabaseField
    private String address1;
    @DatabaseField
    private String address2;
    @DatabaseField
    private String city;
    @DatabaseField
    private String state;
    @DatabaseField
    private String postalCode;
    @DatabaseField
    private String countryCode;
    @DatabaseField
    private String addressType;
    @DatabaseField
    private boolean defaultAddress;
        
}

