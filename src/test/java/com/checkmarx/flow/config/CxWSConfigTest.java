package com.checkmarx.flow.config;

import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

public class CxWSConfigTest {

    private static final CxProperties cxProperties = new CxProperties();
    private static final Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();

    @Test
    public void marshallerWithNullProperties() {
        CxWSConfig cxWSConfig = new CxWSConfig(null);
        Jaxb2Marshaller jaxb2Marshaller = cxWSConfig.marshaller();
        assert jaxb2Marshaller.getContextPath() == null;
    }

    @Test
    public void marshallerWithEmptyPropertiesNullValue() {
        cxProperties.setPortalPackage(null);
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        Jaxb2Marshaller jaxb2Marshaller = cxWSConfig.marshaller();
        assert jaxb2Marshaller.getContextPath() == null;
    }

    @Test
    public void marshallerWithEmptyPropertiesEmptyValue() {
        cxProperties.setPortalPackage("");
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        Jaxb2Marshaller jaxb2Marshaller = cxWSConfig.marshaller();
        assert jaxb2Marshaller.getContextPath() == null;
    }

    @Test
    public void marshallerWithDefaultProperties() {
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        Jaxb2Marshaller jaxb2Marshaller = cxWSConfig.marshaller();
        assert jaxb2Marshaller.getContextPath() == cxProperties.getPortalPackage();
    }

    @Test
    public void webServiceTemplateWithDefaultProperties() {
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(jaxb2Marshaller);
        assert webServiceTemplate.getDefaultUri() == cxProperties.getPortalUrl();
    }

    @Test
    public void webServiceTemplateWithNullProperties() {
        CxWSConfig cxWSConfig = new CxWSConfig(null);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(jaxb2Marshaller);
        assert webServiceTemplate.getDefaultUri() == null;
    }

    @Test
    public void webServiceTemplateWithEmptyPropertiesNullValue() {
        cxProperties.setPortalPackage(null);
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(jaxb2Marshaller);
        assert webServiceTemplate.getDefaultUri() == null;
    }

    @Test
    public void webServiceTemplateWithEmptyPropertiesEmptyValue() {
        cxProperties.setPortalPackage("");
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(jaxb2Marshaller);
        assert webServiceTemplate.getDefaultUri() == null;
    }

    @Test
    public void webServiceTemplateWithDefaultPropertiesNullArg() {
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(null);
        assert webServiceTemplate.getDefaultUri() == cxProperties.getPortalUrl();
    }

    @Test
    public void webServiceTemplateWithNullPropertiesNullArg() {
        CxWSConfig cxWSConfig = new CxWSConfig(null);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(null);
        assert webServiceTemplate.getDefaultUri() == null;
    }

    @Test
    public void webServiceTemplateWithEmptyPropertiesNullValueNullArg() {
        cxProperties.setPortalPackage(null);
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(null);
        assert webServiceTemplate.getDefaultUri() == null;
    }

    @Test
    public void webServiceTemplateWithEmptyPropertiesEmptyValueNullArg() {
        cxProperties.setPortalPackage("");
        CxWSConfig cxWSConfig = new CxWSConfig(cxProperties);
        WebServiceTemplate webServiceTemplate = cxWSConfig.webServiceTemplate(null);
        assert webServiceTemplate.getDefaultUri() == null;
    }
}