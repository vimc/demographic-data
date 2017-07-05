package com.vimc.demography.unwpp2017;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.tools.VIMC_XLSXDocumentParser;
import com.vimc.demography.tools.XLSXLineParser;

public class InterpolatedDemographicIndicators {
  private static final int no_countries = 201;
  private static final int no_fields = 17;
  private static final int no_years = 150; 
  
  private float[] raw_data = new float[no_countries*no_years*no_fields]; // 201 countries * 150 years * 17 fields
    
  private ArrayList<String> country_a3;     // ISO3166 ABC code for each country index 
  private ArrayList<String> country_i3;     // ISO3166 012 code for each country index
  private Element iso3166;                  // ISO3166 XML db.
 
  // Spreadsheet is entirely 3sf - hence, storing all internal data as ints, multiplying src by *1000
  // Therefore, units are....
  
  public static final byte DEATHS_BOTH = 0;                        // number in 1000s
  public static final byte DEATHS_MALE = 1;                        // number in 1000s
  public static final byte DEATHS_FEMALE = 2;                      // number in 1000s
  public static final byte CRUDE_DEATH_RATE = 3;                   // deaths per 1000 population
  public static final byte LIFE_EXPECTANCY_AT_BIRTH_BOTH = 4;      // years
  public static final byte LIFE_EXPECTANCY_AT_BIRTH_MALE = 5;      // years
  public static final byte LIFE_EXPECTANCY_AT_BIRTH_FEMALE = 6;    // years
  public static final byte INFANT_DEATHS = 7;                      // number in 1000s. (under age 1)
  public static final byte INFANT_MORTALITY_RATE = 8;              // deaths per 1000s live births
  public static final byte UNDER_5_MORTALITY = 9;                  // number in 1000s. (under age 5)
  public static final byte BIRTHS = 10;                            // number in 1000s.
  public static final byte CRUDE_BIRTH_RATE = 11;                  // births per 1000 population
  public static final byte TOTAL_FERTILITY = 12;                   // Live births per woman
  public static final byte TOTAL_POPULATION_NATURAL_CHANGE = 13;   // 1000s (actual increase/decrease of population)
  public static final byte RATE_OF_NATURAL_INCREASE = 14;          // number of people increasing, per 1000 in population
  public static final byte POPULATION_CHANGE = 15;                 // number in 1000s. (different to 13 - check defs)
  public static final byte POPULATION_GROWTH_RATE = 16;            // Percentage
  
  private void setDataUnchecked(byte field, int c_index, int year, float value) {
    year-=1950;
    raw_data[field+(year*no_fields)+(c_index*year*no_fields)]=value;
  }
  
  private float getDataUnchecked(byte field, int c_index, int year) {
    year-=1950;
    return raw_data[field+(year*no_fields)+(c_index*year*no_fields)];
  }

  
  /**   The 2017 data of interpolated annual demographic indicators is in one XLSX files.  
   *    1950 - 2015 is in the "ESTIMATES" sheet
   *    2015 - 2099 is in the "MEDIUM VARIANT" sheet - note the duplication of 2015, and the absence of 2100.
   *    
   *    In both sheets, data starts in row 17,  (counting from first row = 0 - add one if you're viewing in excel)
   *       Column E is numerical country code   (assuming columns start at 0, this is column 4)
   *       Column F is year (1st Jan-31st Dec)  (column 5)
   *       Column G is the first field,         (column 6)
   *       Column W is the last field,          (column 22).
   *       See above for the field ordering.
   *    
   **/
 
  private final static String xlsx_file = "WPP2017_INT_F01_ANNUAL_DEMOGRAPHIC_INDICATORS.xlsx";
  private final static String SHEET_ESTIMATES = "ESTIMATES";
  private final static String SHEET_MEDIUM = "MEDIUM VARIANT";
  private final static int FIRST_DATA_ROW = 17;
  private final static int XLSX_COUNTRY_COL = 4;
  private final static int XLSX_YEAR_COL = 5;  
  

  public float get(String c3, byte field, int year) {
    while (c3.length()<3) c3="0"+c3;
    c3=c3.toUpperCase();
    int c_index=-1;
    if (c3.compareTo("AAA")<0) c_index = country_i3.indexOf(c3);
    else c_index=country_a3.indexOf(c3);
    if (c_index==-1) {
      System.out.println("Country not found");
      return Integer.MIN_VALUE;
    } else if ((year<1950) || (year>2099)) {
      System.out.println("Year wrong");
      return Integer.MIN_VALUE;
    } else {
      return getDataUnchecked(field,c_index,year);
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
            int year = Integer.parseInt(bits[XLSX_YEAR_COL]);
            for (byte field=0; field<no_fields; field++) {
              setDataUnchecked(field,last_country_index,year,Float.parseFloat(bits[6+field]));
            }
          }
        }
      }
      row_no++;   
    }
  }
  
  public void dump(PrintStream p, String[] filter_countries, byte field) {
    p.append("value,date_start,date_end,projection_variant,country\n");
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
        for (int y=1950; y<2100; y+=1) {
          String proj=(y<=2015)?"E":"M";
          p.append(get(i3,field,y)+","+y+"0101"+","+y+"1231,"+proj+","+i3+"\n");
        }
      }
    }
  }

   
  public InterpolatedDemographicIndicators(String path, Element _iso3166) throws Exception {
    country_a3 = new ArrayList<String>();
    country_i3 = new ArrayList<String>();
    iso3166 = _iso3166;
    VIMC_XLSXDocumentParser.parse(path+(path.endsWith(File.separator)?"":File.separator)+xlsx_file, new LineParser());
  }
  
}
