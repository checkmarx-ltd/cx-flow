package com.checkmarx.flow.utils;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;

@Slf4j
public class JAXBHelper {

    private JAXBHelper() {
    }

    /**
     *
     * @param classToConvert The class object needs to be marshaled
     * @param fileLocation   File location to save the output
     * @param <T>
     * @throws JAXBException
     */
    public static <T> void convertObjectIntoXml(T classToConvert, String fileLocation) throws JAXBException {
        File file = new File(fileLocation);
        JAXBContext jaxbContext = JAXBContext.newInstance(classToConvert.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(classToConvert, file);

        log.info("Class object: {} was successfully marshaled into file location: {}", classToConvert.getClass().getName(), fileLocation);
    }
}