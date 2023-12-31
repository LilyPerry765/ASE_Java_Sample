/*
 * Copyright (C) 2004, 2005, 2006 Joe Walnes.
 * Copyright (C) 2006, 2007, 2008, 2009, 2011, 2014, 2015 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 * Created on 07. March 2004 by Joe Walnes
 */
package com.thoughtworks.xstream.io.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;


public class DomDriver extends AbstractXmlDriver {

    private final String encoding;
    private DocumentBuilderFactory documentBuilderFactory;

    /**
     * Construct a DomDriver.
     */
    public DomDriver() {
        this(null);
    }

    /**
     * Construct a DomDriver with a specified encoding. The created DomReader will ignore any
     * encoding attribute of the XML header though.
     */
    public DomDriver(String encoding) {
        this(encoding, new XmlFriendlyNameCoder());
    }

    /**
     * @since 1.4
     */
    public DomDriver(String encoding, NameCoder nameCoder) {
        super(nameCoder);
        this.encoding = encoding;
    }

    /**
     * @since 1.2
     * @deprecated As of 1.4, use {@link #DomDriver(String, NameCoder)} instead.
     */
    public DomDriver(String encoding, XmlFriendlyReplacer replacer) {
        this(encoding, (NameCoder)replacer);
    }

    public HierarchicalStreamReader createReader(Reader in) {
        return createReader(new InputSource(in));
    }

    public HierarchicalStreamReader createReader(InputStream in) {
        return createReader(new InputSource(in));
    }

    public HierarchicalStreamReader createReader(URL in) {
        return createReader(new InputSource(in.toExternalForm()));
    }

    public HierarchicalStreamReader createReader(File in) {
        return createReader(new InputSource(in.toURI().toASCIIString()));
    }

    private HierarchicalStreamReader createReader(InputSource source) {
        try {
            if (documentBuilderFactory == null) {
                synchronized (this) {
                    if (documentBuilderFactory == null) {
                        documentBuilderFactory = createDocumentBuilderFactory();
                    }
                }
            }
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            if (encoding != null) {
                source.setEncoding(encoding);
            }
            Document document = documentBuilder.parse(source);
            return new DomReader(document, getNameCoder());
        } catch (FactoryConfigurationError e) {
            throw new StreamException(e);
        } catch (ParserConfigurationException e) {
            throw new StreamException(e);
        } catch (SAXException e) {
            throw new StreamException(e);
        } catch (IOException e) {
            throw new StreamException(e);
        }
    }

    public HierarchicalStreamWriter createWriter(Writer out) {
        return new PrettyPrintWriter(out, getNameCoder());
    }

    public HierarchicalStreamWriter createWriter(OutputStream out) {
        try {
            return createWriter(encoding != null
                ? new OutputStreamWriter(out, encoding)
                : new OutputStreamWriter(out));
        } catch (UnsupportedEncodingException e) {
            throw new StreamException(e);
        }
    }

    /**
     * Create the DocumentBuilderFactory instance.
     * 
     * @return the new instance
     * @since upcoming
     */
    protected DocumentBuilderFactory createDocumentBuilderFactory() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (JVM.is15()) {
            try {
                Method method = DocumentBuilderFactory.class.getMethod("setFeature",
                    new Class[]{ String.class, boolean.class });
                method.invoke(factory, new Object[]{ "http://apache.org/xml/features/disallow-doctype-decl", Boolean.TRUE });
            } catch (NoSuchMethodException e) {
                // Ignore
            } catch (IllegalAccessException e) {
                throw new ObjectAccessException("Cannot set feature of DocumentBuilderFactory.", e);
            } catch (InvocationTargetException e) {
                throw new StreamException(e.getCause());
            }
        }
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
