package com.checkmarx.flow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.beans.ConstructorProperties;

@Configuration
public class CxWSConfig {

    private final CxProperties properties;

    @ConstructorProperties({"properties"})
    public CxWSConfig(CxProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        if (properties != null && properties.getPortalPackage() != null && !properties.getPortalPackage().isEmpty()) {
            marshaller.setContextPaths(properties.getPortalPackage());
        }
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate ws = new WebServiceTemplate();
        if (properties != null && properties.getPortalUrl() != null && !properties.getPortalUrl().isEmpty()) {
            ws.setDefaultUri(properties.getPortalUrl());
        }
        if(marshaller != null) {
            ws.setMarshaller(marshaller);
            ws.setUnmarshaller(marshaller);
        }
        return ws;
    }


}
