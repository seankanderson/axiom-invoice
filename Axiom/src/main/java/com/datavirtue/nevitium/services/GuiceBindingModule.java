package com.datavirtue.axiom.services;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.datavirtue.axiom.services.LocalSettingsService;


/**
 *
 * @author SeanAnderson
 */
public class GuiceBindingModule extends AbstractModule{
       
    @Override
    protected void configure() {
        //bind(Inventory.class).to(Inventory.class);
        //new JdbcConnectionSource("jdbc:h2:mem:axiom")
       try {
            bind(JdbcConnectionSource.class)
                .annotatedWith(Names.named("DatabaseConnection"))
                .toInstance(new JdbcConnectionSource(LocalSettingsService.getLocalAppSettings().getConnectionString()));
       }catch(Exception e) {
           e.printStackTrace();
            //swallow exception...should check the database connection for null
       }
       
    }
}
