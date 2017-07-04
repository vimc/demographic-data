package com.vimc.demography.unwpp2017;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.tools.VIMC_XLSXDocumentParser;
import com.vimc.demography.tools.XLSXLineParser;

public class AgeSpecificFertility {
  private static final int no_countries = 201;
  private static final int no_year_points_estimates = 13; // 1950-1955 until 2095-2100
  private static final int no_year_points_projection = 17;
  private static final int no_projections = 9;
  private static final int no_age_points = 7;
  private int[] raw_data_estimates = new int[no_countries*no_year_points_estimates*no_age_points];
  private int[] raw_data_predictions = new int[no_countries*no_year_points_projection*no_projections*no_age_points];
    
  private ArrayList<String> country_a3;     // ISO3166 ABC code for each country index 
  private ArrayList<String> country_i3;     // ISO3166 012 code for each country index
  private Element iso3166;                 // ISO3166 XML db.
    
  private void setDataUnchecked(int c_index, int age, int year, int projection, int value) {
    age=(age-15)/5;
    if (year<2015) {
      year=(year-1950)/5;
      raw_data_estimates[age+(no_age_points*year)+(no_age_points*no_year_points_estimates*c_index)]=value;
       
    } else {
      year=(year-2015)/5;
      raw_data_predictions[age+(no_age_points*year)+(no_age_points*no_year_points_projection*c_index)
                         +(no_age_points*no_year_points_projection*no_countries*(projection-1))]=value;
      
    }
  }
  
  private int getDataUnchecked(int c_index, int age, int year, int projection) {
    age=(age-15)/5;
    if (year<2015) {
      year=(year-1950)/5;
      return raw_data_estimates[age+(no_age_points*year)+(no_age_points*no_year_points_estimates*c_index)];
       
    } else {
      year=(year-2015)/5;
      return raw_data_predictions[age+(no_age_points*year)+(no_age_points*no_year_points_projection*c_index)
                         +(no_age_points*no_year_points_projection*no_countries*(projection-1))];
      
    }
  }

  
  /**   The 2017 data of age specific fertility is in one XLSX files.
   *    Timepoints are 5-yearly, with 7 values per year, which are the age
   *    of mother giving birth. (15-19, 20-24, 25-29, 30-34, 35-39, 40-44, 45-49)
   *    
   *    This file has all the projections on different sheets. Same format for all.
   *    
   *    Sheet names are below...
   *    
*/
  
   private static String[] projection_names = new String[] {"ESTIMATES","MEDIUM VARIANT","HIGH VARIANT","LOW VARIANT","CONSTANT-FERTILITY",
                                                            "INSTANT-REPLACEMENT","MOMENTUM","ZERO-MIGRATION","CONSTANT-MORTALITY","NO CHANGE"};
   
   public static final byte ESTIMATES=0;
   public static final byte MEDIUM_VARIANT=1;
   public static final byte HIGH_VARIANT=2;
   public static final byte LOW_VARIANT=3;
   public static final byte CONSTANT_FERTILITY=4;
   public static final byte INSTANT_REPLACEMENT=5;
   public static final byte MOMENTUM=6;
   public static final byte ZERO_MIGRATION=7;
   public static final byte CONSTANT_MORTALITY=8;
   public static final byte NO_CHANGE=9;

     
  /*      
   *    In all sheets, data starts in row 17,   (counting from first row = 0 - add one if you're viewing in excel)
   *       Column E is numerical country code   (assuming columns start at 0, this is column 4)
   *       
   *       Then for the estimates sheet:
   *       
   *       Column F is the year, in the form aaaa-bbbb - (1950-1955), column 5
   *       Column G is age 15-19   (col 6)
   *       Column H is age 20-24   (col 7)
   *       Column I is age 25-29   (col 8)
   *       Column J is age 30-34   (col 9)
   *       Column K is age 35-39   (col 10)
   *       Column K is age 40-44   (col 11)
   *       Column K is age 45-49   (col 12)
   *       
   *       For the medium variant sheet:
   *
   *       Column F is the first data point -   (2015-2020, column 5)
   *       Column V is the last data point -    (2095-2100, column 21)
   *    
   *       Note that data values are double with >12 sf, so I've left them as that.
   *       
   **/
 
  private final static String xlsx_file = "WPP2017_FERT_F07_AGE_SPECIFIC_FERTILITY.xlsx";
  private final static int FIRST_DATA_ROW = 17;
  private final static int XLSX_COUNTRY_COL = 4;

  public int get(int age, String c3, int projection, int year) {
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
      return getDataUnchecked(c_index,age,year,projection);
    }
  }
  
  private class LineParser implements XLSXLineParser {
    int row_no = 0;
    String current_sheet = "";
    int current_projection_id=-1;
    String last_country_code = "";
    int last_country_index = -1;
    
    public LineParser() {}
    
    public void newSheet(String s) { 
      row_no=0; 
      current_sheet=new String(s);
      current_projection_id=-1;
      for (int i=0; i<projection_names.length; i++) {
        if (current_sheet.equals(projection_names[i])) {
          current_projection_id=i;
          i=projection_names.length;
        }
      }
      if (current_projection_id==-1) System.out.println("Ignoring sheet "+current_sheet);
    }
    
    public void parseLine(String s) {
      if (current_projection_id>=0) {
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
            int year = Integer.parseInt(bits[5].split("-")[0]);
            for (int age=15; age<=45; age+=5) {
              setDataUnchecked(last_country_index,age,year,current_projection_id,(int)(1000.0f*Float.parseFloat(bits[6+((age-15)/5)])));
            }
          }
        }
      }
      row_no++;   
    }
  }
  
  public void dump(PrintStream p, String[] filter_countries, String[] filter_projections) {
    p.append("age_from,age_to,value,date_start,date_end,projection_variant,country\n");
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
        boolean proj_estimates=(filter_projections==null);
        if (filter_projections!=null) {
          for (int k=0; k<filter_projections.length; k++) {
            if (filter_projections[k].equals("E")) {
              proj_estimates=true;
              k=filter_projections.length;
            }
          }
        }
        if (proj_estimates) {
          for (int y=1950; y<2015; y+=5) {
            for (int a=15; a<=45; a+=5) {
              p.append(a+","+(a+5)+","+get(a,i3,ESTIMATES,y)+","+y+"0701"+","+(y+5)+"0630,E,"+i3+"\n");
            }
          }
        }
          
        String[] proj_codes = new String[] {"E","M","H","L","F","I","U","Z","C","N"};
        for (int pcode=1; pcode<proj_codes.length; pcode++) {
          boolean dump_this_proj = (filter_projections==null);
          if (filter_projections!=null) {
            for (int k=0; k<filter_projections.length; k++) {
              if (filter_projections[k].equals(proj_codes[pcode])) {
                dump_this_proj=true;
                k=filter_projections.length;
              }
            } 
          }
          if (dump_this_proj) {
            for (int y=2015; y<2100; y+=5) {
              for (int a=15; a<=45; a+=5) {
                p.append(a+","+(a+5)+","+get(a,i3,pcode,y)+","+y+"0701"+","+(y+5)+"0630,"+proj_codes[pcode]+","+i3+"\n");
              }
            }
          }
        }
      }
    }
  }

  
   
  public AgeSpecificFertility(String path, Element _iso3166) throws Exception {
    country_a3 = new ArrayList<String>();
    country_i3 = new ArrayList<String>();
    iso3166 = _iso3166;
    VIMC_XLSXDocumentParser.parse(path+(path.endsWith(File.separator)?"":File.separator)+xlsx_file, new LineParser());
  }
}
