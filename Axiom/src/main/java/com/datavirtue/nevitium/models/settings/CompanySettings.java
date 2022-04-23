package com.datavirtue.nevitium.models.settings;

import com.datavirtue.nevitium.models.contacts.ContactAddressInterface;
import lombok.Getter;
import lombok.Setter;

/**
 * Tuesday DEc 7th 2021
 * @author SeanAnderson
 */
@Getter @Setter
public class CompanySettings implements ContactAddressInterface {
    private String companyName;
    private String contactName;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String postalCode;
    private String countryCode = "US";
    private String phoneNumber;
    private String email;
    private String taxId;
    private boolean showTaxIdOnInvoice = false;
    private String companyLogo;
    private String mainScreenLogo;
        
    private FontSetting companyFont;
}
