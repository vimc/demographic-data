package com.vimc.demography.tools;

import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public class VIMC_XLSDocumentParser {
  private static VIMC_XLSDocumentParser singleton;
  
  public static void parse(String xlsfile, XLSXLineParser vimc_lineparser) throws Exception {
    if (singleton==null) singleton=new VIMC_XLSDocumentParser();
    singleton.parse2(xlsfile,vimc_lineparser);
  }
  
  private void parse2(String xlsfile, XLSXLineParser vimc_lineparser) throws Exception {    
    POIFSFileSystem fs=new POIFSFileSystem(new FileInputStream(xlsfile));
    VIMC_HSSFListener hssf = new VIMC_HSSFListener(vimc_lineparser);
    MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(hssf);
    FormatTrackingHSSFListener formatListener = new FormatTrackingHSSFListener(listener);
    HSSFEventFactory factory = new HSSFEventFactory();
    HSSFRequest request = new HSSFRequest();
    request.addListenerForAllRecords(formatListener);
    factory.processWorkbookEvents(request, fs);
    fs.close();
  }
  
  
  class VIMC_HSSFListener implements HSSFListener {  
    private XLSXLineParser lineParser;
    private StringBuffer one_line = new StringBuffer();
    private int sheetIndex = -1;
    private SSTRecord sstRecord;

    private ArrayList<BoundSheetRecord> boundSheetRecords = new ArrayList<BoundSheetRecord>();
    private BoundSheetRecord[] orderedBSRs;    

    
    public VIMC_HSSFListener(XLSXLineParser lp) {
      lineParser=lp;
    }

    @Override
    public void processRecord(Record record) {
      int thisColumn = -1;
      
      switch (record.getSid()) {
      
        case BoundSheetRecord.sid:
          boundSheetRecords.add((BoundSheetRecord)record);
          break;

        case BOFRecord.sid:
          BOFRecord br = (BOFRecord)record;
          if (br.getType() == BOFRecord.TYPE_WORKSHEET) {
            sheetIndex++;
            if (orderedBSRs == null) {
              orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
            }
            lineParser.newSheet(orderedBSRs[sheetIndex].getSheetname());
          }
          break;

        case BlankRecord.sid:
          BlankRecord brec = (BlankRecord) record;
          thisColumn = brec.getColumn();
          break;
      
        case SSTRecord.sid:
          sstRecord = (SSTRecord) record;
          break;

        case LabelSSTRecord.sid:
          LabelSSTRecord lsrec = (LabelSSTRecord) record;
          thisColumn = lsrec.getColumn();
          one_line.append(sstRecord.getString(lsrec.getSSTIndex()).toString());
          break;
      
        case NumberRecord.sid:
          NumberRecord numrec = (NumberRecord) record;
          thisColumn = numrec.getColumn();
          one_line.append(String.valueOf((float)(numrec.getValue())));
          break;

        default:
          break;
      }

      if (thisColumn>=0) one_line.append("\t");

      if(record instanceof LastCellOfRowDummyRecord) {
        lineParser.parseLine(one_line.toString());
        one_line.setLength(0);
      }
    }
  }
}
