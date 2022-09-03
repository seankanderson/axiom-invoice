
package com.datavirtue.axiom.services;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

/**
 *
 * @author sean.anderson
 */
public class JsonHelper {

    public static Gson getGson() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        
        return gson;
    }
    
     public static Object getSingleValueFromJsonProperty(String json, String key) {
        JSONObject object = new JSONObject(json);
        //String[] keys = JSONObject.getNames(object);
        return object.get(key);        
    }
    
}
