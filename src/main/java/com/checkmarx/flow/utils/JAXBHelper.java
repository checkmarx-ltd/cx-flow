package com.checkmarx.flow.utils;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
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

    /**
     * When the object doesn't contain XmlRootElement annotations, since a class object cannot be inferred in compile time, a class must be passed
	 * as a parameter besides the object itself so it can support marshalling of the object.
	 *
     * @param objToConvert The object to be marshaled
     * @param classToConvert The class object needs to be marshaled
     * @param fileLocation   File location to save the output
     * @throws JAXBException
     */
    public static <T> void convertObjectIntoXml(T objToConvert, Class<T> classToConvert, String fileLocation) throws JAXBException {
        File file = new File(fileLocation);
        JAXBContext jaxbContext = JAXBContext.newInstance(classToConvert);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        // Process unknown object to be serialized to XML
        String packageName = objToConvert.getClass().getPackage().getName();
        String simpleClassName = classToConvert.getSimpleName();
        QName qName = new QName(packageName, simpleClassName);
        jaxbMarshaller.marshal(new JAXBElement<T>(qName, classToConvert, objToConvert), file);

        log.info("Class object: {} without @XmlRootElement annotion was successfully marshaled into file location: {}", objToConvert.getClass().getName(), fileLocation);
    }
}