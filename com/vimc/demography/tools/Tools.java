package com.vimc.demography.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class Tools {
  
  public static Element loadDocument(String file) {
    Element root = null;
    try {
      File f = new File(file);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(f);
      root=doc.getDocumentElement();
      root.normalize();
    } catch (Exception e) { e.printStackTrace(); }
    return root;
  }
  
  public static String getAttribute(Node parent, String attname) {
    Node n = parent.getAttributes().getNamedItem(attname);
    if (n==null) return null;
    else return n.getTextContent();
  }
  
  public static int countChildren(Node parent,String tag) {
    int i=0;
    if (parent==null) return 0;
    else if (parent.getChildNodes()==null) return 0;
    else {
      for (int j=0; j<parent.getChildNodes().getLength(); j++) {
        if (parent.getChildNodes().item(j).getNodeType()==Node.ELEMENT_NODE) {
          if (parent.getChildNodes().item(j).getNodeName().equals(tag)) i++;
        }
      }
      return i;
    }
  }

  public static Node getChildNo(Node parent,String tag,int n) {
    int i=0;
    Node result=null;
    for (int j=0; j<parent.getChildNodes().getLength(); j++) {
      if (parent.getChildNodes().item(j).getNodeType()==Node.ELEMENT_NODE) {
        if (parent.getChildNodes().item(j).getNodeName().equals(tag)) {
          if (i==n) {
            result = parent.getChildNodes().item(j);
            j=parent.getChildNodes().getLength();
          }
          i++;
        }
      }
    }
    return result;
  }
  
  public static Node getTagWhereAttr(Node parent, String tag, String attr, String attrValue) {
    Node resultNode = null;
    int count = countChildren(parent,tag);
    for (int i=0; i<count; i++) {
      Node n = getChildNo(parent,tag,i);
      if (n.getAttributes().getNamedItem(attr).getTextContent().equals(attrValue)) {
        resultNode=n;
        i=count;
      }
    }
    return resultNode;
  }
  
  public static void downloadFile(String url, String save_to, boolean skip_if_exists) throws Exception {
    if ((!skip_if_exists) || (!new File(save_to).exists())) {
      URL website = new URL(url);
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      FileOutputStream fos = new FileOutputStream(save_to);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      fos.close();
    }
  }

  public static void ensureDirectoriesExist(String[] dirs) throws Exception {
    for (int i=0; i<dirs.length; i++) {
      if (!new File(dirs[i]+File.separator).exists()) { new File(dirs[i]+File.separator).mkdirs(); }
    }
  }

}
