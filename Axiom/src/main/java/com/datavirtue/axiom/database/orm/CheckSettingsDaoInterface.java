/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.datavirtue.axiom.database.orm;

import com.j256.ormlite.dao.Dao;
import com.datavirtue.axiom.models.KeyValueStore;
import java.util.UUID;

/**
 *
 * @author SeanAnderson
 */
public interface CheckSettingsDaoInterface extends Dao<KeyValueStore, UUID> { 
    
}
