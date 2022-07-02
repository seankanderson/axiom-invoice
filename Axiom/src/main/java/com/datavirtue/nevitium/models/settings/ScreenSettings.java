package com.datavirtue.axiom.models.settings;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author SeanAnderson
 */
@Getter @Setter
public class ScreenSettings {
    WindowSizeAndPosition main;
    WindowSizeAndPosition contacts;
    WindowSizeAndPosition inventory;
    WindowSizeAndPosition invoices;
    WindowSizeAndPosition invoiceMiscItem;
    WindowSizeAndPosition invoiceManager;
    WindowSizeAndPosition checkPrinter;
    WindowSizeAndPosition settings;    
}
