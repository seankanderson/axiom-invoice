package com.datavirtue.axiom.ui.shared;

import java.util.ArrayList;
import javax.swing.AbstractListModel;
import javax.swing.event.EventListenerList;

/**
 *
 * @author SeanAnderson
 */
public class CollectionMappedListModel<T> extends AbstractListModel<T> {

    protected EventListenerList listenerList = new EventListenerList();
    
    public CollectionMappedListModel(ArrayList<T> data) {
        this.theCollection = data;
    }
    
    private ArrayList<T> theCollection;
    
    @Override
    public int getSize() {
        return theCollection.size();
    }

    @Override
    public T getElementAt(int index) {
        return theCollection.get(index);
    }
  
    
}
