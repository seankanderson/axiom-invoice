package com.datavirtue.axiom.services.integrations;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author sean.anderson
 */
@Getter @Setter
public class HttpApiCall {
    
    public HttpApiCall() {
        header.add("Content-Type: application/json");
    }
    private String baseUrl = "";
    private String urlPath = "";
    private ArrayList<String> header = new ArrayList();
    public void setHeader(String headerEntry) {
        header.add(headerEntry);
    }  
    
    
    
}
