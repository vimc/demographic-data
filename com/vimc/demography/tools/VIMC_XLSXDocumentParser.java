package com.vimc.demography.tools;

import java.io.File;
import java.io.InputStream;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.vimc.demography.tools.XSSFSheetXMLHandlerNumeric.SheetContentsHandler;


public class VIMC_XLSXDocumentParser {
  private static VIMC_XLSXDocumentParser singleton;

  
  public static void parse(String xlsfile, XLSXLineParser vimc_lineparser) throws Exception {
    if (singleton==null) singleton=new VIMC_XLSXDocumentParser();
    singleton.parse2(xlsfile,vimc_lineparser);
  }
  
  private void parse2(String xlsfile, XLSXLineParser vimc_lineparser) throws Exception {
      OPCPackage p = OPCPackage.open(new File(xlsfile).getPath(), PackageAccess.READ);
    ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(p);
    XSSFReader xssfReader = new XSSFReader(p);
    StylesTable styles = xssfReader.getStylesTable();
    XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
    while (iter.hasNext()) {
      InputStream stream = iter.next();
      String sheetName = iter.getSheetName();
      vimc_lineparser.newSheet(sheetName);
      SheetParseEvent shandler = new SheetParseEvent(vimc_lineparser);
      DataFormatter formatter = new DataFormatter(true);
      InputSource sheetSource = new InputSource(stream);
      try {
        XMLReader sheetParser = SAXHelper.newXMLReader();
        ContentHandler handler = new XSSFSheetXMLHandlerNumeric(styles, null, strings, shandler, formatter , true);
        sheetParser.setContentHandler(handler);
        sheetParser.parse(sheetSource);
      } catch (Exception e) { e.printStackTrace(); } 
        
      stream.close();
    }
  }
  
  class SheetParseEvent implements SheetContentsHandler {
    int lines_index;
    StringBuffer buffer = new StringBuffer();
    XLSXLineParser lineParser;
    
    SheetParseEvent(XLSXLineParser lp) {
      lineParser=lp;
    }
    
    private boolean firstCellOfRow = false;
    private int currentRow = -1;
    private int currentCol = -1;
      
    private void outputMissingRows(int number) {
      for (int i=0; i<number; i++) { 
        lineParser.parseLine(""); 
      }
    }

    @Override
    public void startRow(int rowNum) {
      outputMissingRows(rowNum-currentRow-1);
      buffer.setLength(0);
      firstCellOfRow = true;
      currentRow = rowNum;
      currentCol = -1;
    }

    @Override
    public void endRow(int rowNum) {
      lineParser.parseLine(buffer.toString());
    }
    
    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
      if (firstCellOfRow) firstCellOfRow = false;
      else buffer.append("\t");

      // gracefully handle missing CellRef here in a similar way as XSSFCell does
      if(cellReference == null) cellReference = new CellAddress(currentRow, currentCol).formatAsString();

      // Did we miss any cells?
      int thisCol = (new CellReference(cellReference)).getCol();
      int missedCols = thisCol - currentCol - 1;
      for (int i=0; i<missedCols; i++) buffer.append("\t");
      currentCol = thisCol;
      buffer.append(formattedValue);
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {}
  }
}
