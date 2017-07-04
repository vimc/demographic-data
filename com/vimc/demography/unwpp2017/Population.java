package com.vimc.demography.unwpp2017;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.tools.VIMC_XLSXDocumentParser;
import com.vimc.demography.tools.XLSXLineParser;

public class Population {
  private static final int no_countries = 201;
  private static final int no_year_points_estimates = 14; // 1950-1955 until 2015-2010 are estimates
  private static final int no_year_points_projection = 17; // 2015-2100 (2100 is included.)
  private static final int no_projections = 9;
  private static final int no_age_points = 21; // Actually only 17 for pre 1990, but let's not confuse things over that.
  private static final int no_genders = 3;
  private int[] raw_data_estimates = new int[no_genders*no_countries*no_year_points_estimates*no_age_points];
  private int[] raw_data_predictions = new int[no_genders*no_countries*no_year_points_projection*no_projections*no_age_points];
    
  private ArrayList<String> country_a3;     // ISO3166 ABC code for each country index 
  private ArrayList<String> country_i3;     // ISO3166 012 code for each country index
  private Element iso3166;                  // ISO3166 XML db.
    
  private void setDataUnchecked(int gender, int c_index, int age, int year, int projection, int value) {
    age=age/5;
    if (year<=2015) {
      year=(year-1950)/5;
      raw_data_estimates[age+(no_age_points*year)+(no_age_points*no_year_points_estimates*c_index)
                         +(gender*no_age_points*no_year_points_estimates*no_countries)]=value;
       
    } else {
      year=(year-2020)/5;
      raw_data_predictions[age+(no_age_points*year)+(no_age_points*no_year_points_projection*c_index)
                         +(no_age_points*no_year_points_projection*no_countries*(projection-1))
                         +(gender*no_age_points*no_year_points_projection*no_countries*no_projections)]=value;
      
    }
  }
  
  private int getDataUnchecked(int gender, int c_index, int age, int year, int projection) {
    age=age/5;
    if (year<=2015) {
      year=(year-1950)/5;
      return raw_data_estimates[age+(no_age_points*year)+(no_age_points*no_year_points_estimates*c_index)
                         +(gender*no_age_points*no_year_points_estimates*no_countries)];
       
    } else {
      year=(year-2020)/5;
      return raw_data_predictions[age+(no_age_points*year)+(no_age_points*no_year_points_projection*c_index)
                         +(no_age_points*no_year_points_projection*no_countries*(projection-1))
                         +(gender*no_age_points*no_year_points_projection*no_countries*no_projections)];
       
    }
  }

  
  /**   The 2017 data for population with projection variants is in three XLSX files, one for each gender.
   *    The resolution is 5-year time and 5-year age points, for all projections.  
   *    1950 - 2015 is in the "ESTIMATES" sheet
   *    2015 - 2100 are in the other sheets, one for each variant. See list below.
   *    
   *    In all sheets, data starts in row 17,  (counting from first row = 0 - add one if you're viewing in excel)
   *       Column E is numerical country code   (assuming columns start at 0, this is column 4)
   *       Column F is year (1st July)          (column 5)
   *       Column G is age 0 (x>=0, x<1)        (column 6)
   *       Column CH is age 79. (x>=79, x<80)   (column 85)
   *       
   *       FOR THE ESTIMATES SHEET:-
   *       
   *         For years 1950..1985 inclusive: (ie, 1950-1955 until 1985-1989) 
   *           Column CI is age 80+ (x>=80)       (column 86)
   *           column CJ..DD is "..." = NA
   *         
   *         For years 1990..2095 inclusive: (ie, 1990-1995 until 2095-2100) 
   *           Column CI is "..." = NA            (column 86)
   *           Column CJ..DC are ages 80..99      (columns 87-106)
   *           Column DD is 100 (100+) ?          (column 107) 
   *         
   *      FOR THE MEDIUM VARIANT SHEET (years are always after 1990)
   *      they don't bother with the 80+ column at all.
   *      
   *        For years 2015..2100 inclusive:
   *           Column CH is age 79          (Column 85)
   *           Column CI is age 80          (Column 86)
   *           Column CJ is age 81          (Column 87)
   *           Column DC is age 100+        (column 106)
   *         
   *         
   *    Note that reference date for these populations is 1st July in the given year.
   *    
   *    Three files give the data separately for BOTH, MALE and FEMALE gender:-
   **/
   
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
   
   public final static byte BOTH_GENDER=0;
   public final static byte MALE=1;
   public final static byte FEMALE=2;

   private final static String[] xlsx_files = new String[] {
       "WPP2017_POP_F07_1_POPULATION_BY_AGE_BOTH_SEXES.XLSX",
       "WPP2017_POP_F07_2_POPULATION_BY_AGE_MALE.XLSX",
       "WPP2017_POP_F07_3_POPULATION_BY_AGE_FEMALE.XLSX"};

  private final static int FIRST_DATA_ROW = 17;
  private final static int XLSX_COUNTRY_COL = 4;
  private final static int XLSX_YEAR_COL = 5;

  public int get(int age, String c3, int gender, int projection, int year) {
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
    } else if (((year<1990) && (age>80)) || (age>100) || (age<0)) {
      System.out.println("Age wrong");;
      return Integer.MIN_VALUE;
    } else if ((gender<0) || (gender>2)) {
      System.out.println("Gender wrong");
      return Integer.MIN_VALUE;
    } else  {
      return getDataUnchecked(gender,c_index,age,year,projection);
    }
  }
  
  private class LineParser implements XLSXLineParser {
    int row_no = 0;
    String current_sheet = "";
    int current_projection_id=-1;
    String last_country_code = "";
    int last_country_index = -1;
    int gender=-1;
    
    public LineParser(int g) { gender=g; }
    
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
            int year = Integer.parseInt(bits[XLSX_YEAR_COL]);
            if (current_sheet.equals(projection_names[ESTIMATES])) {
              if (year<1990) {
                for (int age=0; age<=80; age+=5) {
                  setDataUnchecked(gender,last_country_index,age,year,current_projection_id,(int)(1000.0f*Float.parseFloat(bits[6+(age/5)])));
                }
              } else {
                for (int age=0; age<=75; age+=5) {
                  setDataUnchecked(gender,last_country_index,age,year,current_projection_id,(int)(1000.0f*Float.parseFloat(bits[6+(age/5)])));
                }
                for (int age=80; age<=100; age+=5) {
                  setDataUnchecked(gender,last_country_index,age,year,current_projection_id,(int)(1000.0f*Float.parseFloat(bits[7+(age/5)])));
                }
              }
            } else {
              for (int age=0; age<=100; age+=5) {
                setDataUnchecked(gender,last_country_index,age,year,current_projection_id,(int)(1000.0f*Float.parseFloat(bits[6+(age/5)])));
              }
            
            }
          }
        }
      }
      row_no++;   
    }
  }
  
   
  public Population(String path, Element _iso3166) throws Exception {
    country_a3 = new ArrayList<String>();
    country_i3 = new ArrayList<String>();
    iso3166 = _iso3166;
    for (int gender=0; gender<3; gender++) {
      VIMC_XLSXDocumentParser.parse(path+(path.endsWith(File.separator)?"":File.separator)+xlsx_files[gender], new LineParser(gender));
    }
  }
}
