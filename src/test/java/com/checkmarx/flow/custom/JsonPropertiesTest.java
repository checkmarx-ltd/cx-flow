package com.checkmarx.flow.custom;

import com.checkmarx.flow.config.properties.JsonProperties;
import org.junit.Test;

public class JsonPropertiesTest {

    @Test
    public void getFileNameFormat() {
        JsonProperties jsonProperties = new JsonProperties();
        assert jsonProperties.getFileNameFormat().equals("[APP]-[BRANCH]-[TIME]");
    }
   
    @Test
    public void setFileNameFormat() {
        JsonProperties jsonProperties = new JsonProperties();
        String test = "123";
        jsonProperties.setFileNameFormat(test);
        assert jsonProperties.getFileNameFormat().equals(test);
    }

    @Test
    public void getDataFolder() {
        JsonProperties jsonProperties = new JsonProperties();
        assert jsonProperties.getDataFolder().equals("/tmp");
    }

    @Test
    public void setDataFolder() {
        JsonProperties jsonProperties = new JsonProperties();
        String test = "123";
        jsonProperties.setDataFolder(test);
        assert jsonProperties.getDataFolder().equals(test);
    }
}