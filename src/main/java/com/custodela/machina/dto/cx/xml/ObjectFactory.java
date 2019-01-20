package com.custodela.machina.dto.cx.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.model.cx package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CxXMLResults_QNAME = new QName("", "CxXMLResults");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.model.cx
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CxXMLResultsType }
     *
     */
    public CxXMLResultsType createCxXMLResultsType() {
        return new CxXMLResultsType();
    }

    /**
     * Create an instance of {@link SnippetType }
     *
     */
    public SnippetType createSnippetType() {
        return new SnippetType();
    }

    /**
     * Create an instance of {@link PathType }
     *
     */
    public PathType createPathType() {
        return new PathType();
    }

    /**
     * Create an instance of {@link PathNodeType }
     *
     */
    public PathNodeType createPathNodeType() {
        return new PathNodeType();
    }

    /**
     * Create an instance of {@link ResultType }
     *
     */
    public ResultType createResultType() {
        return new ResultType();
    }

    /**
     * Create an instance of {@link LineType }
     *
     */
    public LineType createLineType() {
        return new LineType();
    }

    /**
     * Create an instance of {@link QueryType }
     *
     */
    public QueryType createQueryType() {
        return new QueryType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CxXMLResultsType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "", name = "CxXMLResults")
    public JAXBElement<CxXMLResultsType> createCxXMLResults(CxXMLResultsType value) {
        return new JAXBElement<CxXMLResultsType>(_CxXMLResults_QNAME, CxXMLResultsType.class, null, value);
    }

}
