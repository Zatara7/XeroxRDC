package com.example.xeroxrdc;

import android.annotation.SuppressLint;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by robertkatulka on 3/11/15.
 *
 * This file holds three classes: XmlObject, XmlObjects and XmlDataFile
 * XmlObject - Individual object data for XML files
 * XmlObjects - List of XmlObject instances
 * XmlDataFile -
 */
public class XmlDataFile {
    public XmlObjects data(String file) {
        XmlDataFile xml = new XmlDataFile();
        XmlObjects xmlObjects;
        XmlObject obj1 = new XmlObject();
        XmlObject obj2 = new XmlObject();
        XmlObject obj3 = new XmlObject();
        XmlObject obj4 = new XmlObject();
        XmlObject obj5 = new XmlObject();

        obj1.setName("first");
        obj1.addDistance((float) 0.0);
        obj1.setReference(true);

        obj2.setName("second");
        obj2.addDistance((float) 1.0);
        obj2.setReference(false);

        obj3.setName("third");
        obj3.addDistance((float) 2.0);
        obj3.setReference(false);

        obj4.setName("fourth");
        obj4.addDistance((float) 3.0);
        obj4.addDistance((float)3.1);
        obj4.addDistance((float)3.0);
        obj4.setReference(false);

        obj5.setName("fifth");
        ArrayList<Float> list = new ArrayList<Float>();
        list.add((float) 0.0);
        list.add((float) 0.5);
        list.add((float) 1.0);
        list.add((float) 0.5);
        obj5.addDistances(list);
        obj5.setReference(false);

        xmlObjects = new XmlObjects();

        xmlObjects.add(obj1);
        xmlObjects.add(obj2);
        xmlObjects.add(obj3);
        xmlObjects.add(obj4);
        xmlObjects.add(obj5);
        xmlObjects.remove(obj2);

        xmlObjects.print();

        String fileName = xml.writeXml(xmlObjects, file);
        if(fileName != null) {
            xmlObjects = xml.parseXml(new File(file));
        }

        return xmlObjects;
    }

    public XmlObjects parseXml(File file) {
        XmlObjects xmlObjects = new XmlObjects();

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(file);

            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("Object");

            for(int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    XmlObject xmlObject = new XmlObject();
                    Element element = (Element) node;
                    xmlObject.setName(element.getTextContent());

                    String[] floats = element.getAttribute("distance").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(" ", "").split(",");
                    for(String f : floats) {
                        if(!f.equals("")) {
                            xmlObject.addDistance(Float.parseFloat(f));
                        }
                    }

                    if(element.getAttribute("reference").equals("true")) {
                        xmlObject.setReference(Boolean.TRUE);
                    } else {
                        xmlObject.setReference(Boolean.FALSE);
                    }
                    xmlObjects.add(xmlObject);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return xmlObjects;
    }

    private Document populateXml(XmlObjects xmlObjects) {
        Document doc = createXml();

        Element root = doc.createElement("data");
        doc.appendChild(root);

        for(XmlObject obj : xmlObjects.getXmlObjects()) {
            writeObject(doc, root, obj);
        }

        return doc;
    }

    public String writeXml(XmlObjects xmlObjects, String file) {
        Document doc = populateXml(xmlObjects);
        String fileName = null;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            Date date = new Date();
            //@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_'at'_hh_mm_ss");
            fileName = file/* + ".xml"*/;

            StreamResult result = new StreamResult(new File(fileName));
            transformer.transform(source, result);
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private void writeObject(Document doc, Element root, XmlObject obj) {
        Element object = doc.createElement("Object");

        object.setAttribute("distance", obj.getDistances().toString());
        object.setAttribute("reference", obj.getReference().toString());

        object.appendChild(doc.createTextNode(obj.getName()));
        root.appendChild(object);
    }

    private Document createXml() {
        Document doc = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
            doc = dbBuilder.newDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return doc;
    }
}

/**
 * Stores xmlObject instances.  Keeps the name of all object instances unique and only one of the objects is the reference
 * xmlObjects - A list of XmlObject instances
 */
class XmlObjects {
    private ArrayList<XmlObject> xmlObjects;

    /**
     * Constructor for the class.  Responsible for instantiating the list of XmlObject instances
     */
    public XmlObjects() {
        xmlObjects = new ArrayList<XmlObject>();
    }

    /**
     * Adds an XmlObject to the list.  Fails if the object's name is already being used, or the object is the second reference object to be added
     * @param xmlObject - Stores the data associated to an object that is detected in an image or frame
     */
    public void add(XmlObject xmlObject) {
        Boolean add = Boolean.TRUE;

        // If the object's name is already being used, throw an error
        for(XmlObject obj : xmlObjects) {
            if(obj.getName().equals(xmlObject.getName())) {
                add = Boolean.FALSE;
                System.out.println("The name of the object is not unique");
            }
        }

        // If the object is the second reference object, throw an error
        if(xmlObject.getReference() && add) {
            for(XmlObject obj: xmlObjects) {
                if(obj.getReference()) {
                    add = Boolean.FALSE;
                    System.out.println("Error ref already exists");
                }
            }
        }

        // Add only if the name is unique or the object's name is not already in use
        if(add) {
            xmlObjects.add(xmlObject);
        }
    }

    /**
     * Returns the list of xmlObjects in an image or video feed
     * @return xmlObjects - A list of xmlObjects
     */
    public ArrayList<XmlObject> getXmlObjects() { return xmlObjects; }

    /**
     * Prints the list of XmlObject instances in the list (used for debugging).  Uncomment for debugging
     */
     public void print() {
         for(XmlObject obj : xmlObjects) {
            System.out.println("Name: " + obj.getName() + "\t Distance: " + obj.getDistances().toString() + "\t Reference: " + obj.getReference().toString());
         }
     }

    /**
     * Remove an XmlObject from this class based on its name
     * @param obj - The object to be removed from the list xmlObjects
     */
    public void remove(XmlObject obj) {
        xmlObjects.remove(obj);
    }
}

/**
 * Stores the data associated to an object that is detected in an image or frame
 * name - The name of the object.  Default value = ""
 * distance - The distance of the object to the reference object.  Default value = 0.0
 * reference - True if the object is the reference object and False otherwise.  Default value = false
 */
class XmlObject {
    private String name;
    private ArrayList<Float> distances;
    private Boolean reference;

    /**
     * The constructor for the class
     *
     * Sets the default values for all instances of the object
     */
    public XmlObject() {
        name = "";
        distances = new ArrayList<Float>();
        reference = Boolean.FALSE;
    }

    /**
     * Returns the name of an object
     * @return - returns the name of the object
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the distance list of a non-reference object from its reference object
     * @return - returns the distance of an object from the reference object.  0 if object is the reference object
     */
    public ArrayList<Float> getDistances() {
        return distances;
    }

    /**
     * Returns the state of the object, reference or non-reference
     * @return - 1 for reference, 0 for non-reference
     */
    public Boolean getReference() {
        return reference;
    }

    /**
     * Adds the distance of a non-reference object from its reference object.  Will not change the distance list if the object is reference
     * @param distance - a single distance to be added to the distance list
     */
    public void addDistance(float distance) {
        if(!this.reference) {
            distances.add(distance);
        }
    }

    /**
     * Adds a list of distances of a non-reference object from its reference object.  Will not change the distance list if the object is reference
     * @param distances - the list of distances to be added to the distance list
     */
    public void addDistances(ArrayList<Float> distances) {
        if(!this.reference) {
            for (Float distance : distances) {
                addDistance(distance);
            }
        }
    }

    /**
     * Sets the name of an object
     * @param name - sets the name of an object
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the reference of the object.  If the object is the reference object, then set distance list to null
     * @param reference - 1 for reference, 0 for non-reference
     */
    public void setReference(Boolean reference) {
        this.reference = reference;
        if(this.reference) {
            distances.clear();
            addDistance((float)0.0);
        }
    }
}
