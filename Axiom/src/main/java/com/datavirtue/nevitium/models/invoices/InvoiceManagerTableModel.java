
package com.datavirtue.axiom.models.invoices;

import com.datavirtue.axiom.models.AbstractCollectionTableModel;
import com.datavirtue.axiom.services.InvoiceService;
import java.util.Date;
import java.util.List;

/**
 *
 * @author SeanAnderson
 */
public class InvoiceManagerTableModel extends AbstractCollectionTableModel<Invoice> {

    public InvoiceManagerTableModel(List<Invoice> invoices) {
        this.items = invoices;
        this.columns = new String[]{"Date", "Number", "Customer", "Total"};
        
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (items == null) {
            return;
        }
        var item = items.get(row);

        switch (col) {
            case 0:
                item.setInvoiceDate((Date) value);
                break;
            case 1:
                item.setInvoiceNumber((String)value);
                break;
            case 2: 
                item.setBillTo((InvoiceCustomerInfo)value);
                break;
            case 3: 
                break;
                
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (items == null) {
            return new Object();
        }
        var item = items.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return item.getInvoiceDate();
            case 1:
                return item.getInvoiceNumber();
            case 2:
                return item.getBillTo() != null ? item.getBillTo().getCompanyName() + " - " + item.getBillTo().getContactName() : ""; 
            case 3:
                return InvoiceService.calculateInvoiceTotals(item).getGrandTotal();
        }
        return null;

    }

}
