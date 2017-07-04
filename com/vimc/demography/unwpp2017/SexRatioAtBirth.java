package com.vimc.demography.unwpp2017;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.tools.VIMC_XLSXDocumentParser;
import com.vimc.demography.tools.XLSXLineParser;

public class SexRatioAtBirth {
  private static final int no_countries = 201;
  private static final int no_year_points = 30; // 1950-1955 until 2095-2100  
  private int[] raw_data = new int[no_countries*no_year_points]; 
    
  private ArrayList<String> country_a3;     // ISO3166 ABC code for each country index 
  private ArrayList<String> country_i3;     // ISO3166 012 code for each country index
  private Element iso3166;                 // ISO3166 XML db.
    
  private void setDataUnchecked(int c_index, int year, int value) {
    // eg, 1955 return 1955-1960, 
    year=(year-1950)/5;
    raw_data[(year)+(c_index*year)]=value;
  }
  
  private int getDataUnchecked(int c_index, int year) {
    year=(year-1950)/5;
    return raw_data[(year)+(c_index*year)];
  }

  
  /**   The 2017 data of sex ratio at birth is in one XLSX files.
   *    Timepoints are 5-yearly, and there is just one value per year:
   *    The number of male births per female birth.
   *      
   *    1950-1955 until 2010-2015 is in the "ESTIMATES" sheet
   *    2015-2020 until 2095-2100 is in the "MEDIUM VARIANT" sheet.
   *    
   *    In both sheets, data starts in row 17,  (counting from first row = 0 - add one if you're viewing in excel)
   *       Column E is numerical country code   (assuming columns start at 0, this is column 4)
   *       
   *       Then for the estimates sheet:
   *       
   *       Column F is the first data point -   (1950-1955, column 5)
   *       Column R is the last data point -    (2010-2015, column 17)
   *       
   *       For the medium variant sheet:
   *
   *       Column F is the first data point -   (2015-2020, column 5)
   *       Column V is the last data point -    (2095-2100, column 21)
   *    
   **/
 
  private final static String xlsx_file = "WPP2017_FERT_F02_SEX_RATIO_AT_BIRTH.xlsx";
  private final static String SHEET_ESTIMATES = "ESTIMATES";
  private final static String SHEET_MEDIUM = "MEDIUM VARIANT";
  private final static int FIRST_DATA_ROW = 17;
  private final static int XLSX_COUNTRY_COL = 4;

  public int get(String c3, int year) {
    while (c3.length()<3) c3="0"+c3;
    c3=c3.toUpperCase();
    int c_index=-1;
    if (c3.compareTo("AAA")<0) c_index = country_i3.indexOf(c3);
    else c_index=country_a3.indexOf(c3);
    if (c_index==-1) {
      System.out.println("Country not found");
      return Integer.MIN_VALUE;
    } else if ((year<1950) || (year>2100)) {
      System.out.println("Year wrong");
      return Integer.MIN_VALUE;
    } else {
      return getDataUnchecked(c_index,year);
    }
  }
  
  private class LineParser implements XLSXLineParser {
    int row_no = 0;
    String current_sheet = "";
    String last_country_code = "";
    int last_country_index = -1;
    
    public LineParser() {}
    
    public void newSheet(String s) { row_no=0; current_sheet=new String(s); }
    
    public void parseLine(String s) {
      if ((current_sheet.equals(SHEET_ESTIMATES)) || (current_sheet.equalsIgnoreCase(SHEET_MEDIUM))) {
        if (row_no>=FIRST_DATA_ROW) {
          String[] bits = s.split("\t");
          if (!bits[XLSX_COUNTRY_COL].equals(last_country_code)) {             // Change of country
            int next_country_code=Integer.parseInt(bits[XLSX_COUNTRY_COL]);    // This is the integer code. (Could be <3 digits)
            if (next_country_code<900) {                                       // We want country, not WORLD/CONTINENT.
              
              // Have we met this country before?  
            
              last_country_code=((next_country_code<10)?"0":"")+((next_country_code<100)?"0":"")+bits[XLSX_COUNTRY_COL];  // Insert missing 0s
              last_country_index=country_i3.indexOf(last_country_code);                                                   // Seek country
              if (last_country_index==-1) {                                                                               // Not found
                last_country_index=country_i3.size();                                                                     // Index will be last element
                country_i3.add(last_country_code);                                                                        // Add numeric 3-digit code
                country_a3.add(Tools.getAttribute(Tools.getTagWhereAttr(iso3166,"c","n3",last_country_code),"c3"));       // Add alpha 3-digit code from XML                
              } 
            } else last_country_index=-1;          // If country code was more than 900, then reset index.
          }
          
          if (last_country_index>=0) {
            // Deal with actual data. last_country_index will now be correctly set, either "-1" if we don't want this row, or the correct country index if we do.                
            if (current_sheet.equals(SHEET_ESTIMATES)) {
              for (int year=1950; year<=2010; year+=5) {
                setDataUnchecked(last_country_index,year,(int) (Float.parseFloat(bits[((year-1950)/5)+5])*1000.0f));
              }
            } else if (current_sheet.equals(SHEET_MEDIUM)) {
              for (int year=2015; year<=2095; year+=5) {
                setDataUnchecked(last_country_index,year,(int) (Float.parseFloat(bits[((year-2015)/5)+5])*1000.0f));
              }
            }
          }
        }
      }
      row_no++;   
    }
  }

  
//  private int[] raw_data = new int[no_countries*no_year_points]; 

  
  public void dump(PrintStream p, String[] filter_countries) {
    p.append("value\tdate_start\tdate_end\tprojection_variant\tcountry\n");
    for (int i=0; i<no_countries; i++) {
      String i3 = country_i3.get(i);
      boolean pick_country = (filter_countries==null);
      if (filter_countries!=null) {
        for (int j=0; j<filter_countries.length; j++) {
          if ((i3.equals(filter_countries[j])) || (country_a3.get(i).equals(filter_countries[j]))) {
            pick_country=true;
            j=filter_countries.length;
          }
        }
      }
      
      if (pick_country) {
        for (int y=1950; y<2100; y+=5) {
          String proj=(y<2015)?"E":"M";
          p.append(get(i3,y)+"\t"+y+"0701\t"+(y+5)+"0630\t"+proj+"\t"+i3+"\n");
        }
      }
    }
  }
   
  public SexRatioAtBirth(String path, Element _iso3166) throws Exception {
    country_a3 = new ArrayList<String>();
    country_i3 = new ArrayList<String>();
    iso3166 = _iso3166;
    VIMC_XLSXDocumentParser.parse(path+(path.endsWith(File.separator)?"":File.separator)+xlsx_file, new LineParser());
  }
}
