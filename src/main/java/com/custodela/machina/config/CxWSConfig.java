package com.custodela.machina.config;

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
        marshaller.setContextPaths(properties.getPortalPackage());
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate ws = new WebServiceTemplate();
        ws.setDefaultUri(properties.getPortalUrl());
        ws.setMarshaller(marshaller);
        ws.setUnmarshaller(marshaller);
        return ws;
    }


}
