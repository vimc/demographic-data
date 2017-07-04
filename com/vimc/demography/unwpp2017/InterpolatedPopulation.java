package com.vimc.demography.unwpp2017;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.tools.VIMC_XLSXDocumentParser;
import com.vimc.demography.tools.XLSXLineParser;

public class InterpolatedPopulation {
  private static final int no_countries = 201;
  private static final int age_size_pre_1990 = 81;
  private static final int age_size_post_1990 = 101;
  private  static final int years_pre_1990 = 40;
  private static final int years_post_1990 = 111;
  private static final int no_genders = 3;
  
  private int[] raw_data_pre_1990 = new int[no_countries*no_genders*years_pre_1990*age_size_pre_1990];
  private int[] raw_data_post_1990 = new int[no_countries*no_genders*years_post_1990*age_size_post_1990];
  
  private ArrayList<String> country_a3;     // ISO3166 ABC code for each country index 
  private ArrayList<String> country_i3;     // ISO3166 012 code for each country index
  private Element iso3166;                 // ISO3166 XML db.
  
  
  
  private void setDataUnchecked(byte gender, int c_index, int year, int age, int value) {
    if (year<1990) raw_data_pre_1990[age+((year-1950)*age_size_pre_1990)+(c_index*age_size_pre_1990*years_pre_1990)+(gender*age_size_pre_1990*years_pre_1990*no_countries)]=value;
    else raw_data_post_1990[age+((year-1990)*age_size_post_1990)+(c_index*age_size_post_1990*years_post_1990)+(gender*age_size_post_1990*years_post_1990*no_countries)]=value;
  }
  
  private int getDataUnchecked(byte gender, int c_index, int year, int age) {
    if (year<1990) return raw_data_pre_1990[age+((year-1950)*age_size_pre_1990)+(c_index*age_size_pre_1990*years_pre_1990)+(gender*age_size_pre_1990*years_pre_1990*no_countries)];
    else return raw_data_post_1990[age+((year-1990)*age_size_post_1990)+(c_index*age_size_post_1990*years_post_1990)+(gender*age_size_post_1990*years_post_1990*no_countries)];
  }

  
  /**   The 2017 data for age-decomposed interpolated population is in three XLSX files.  
   *    1950 - 2015 is in the "ESTIMATES" sheet
   *    2015 - 2100 is in the "MEDIUM VARIANT" sheet - note the duplication of 2015.
   *    In both sheets, data starts in row 17,  (counting from first row = 0 - add one if you're viewing in excel)
   *       Column E is numerical country code   (assuming columns start at 0, this is column 4)
   *       Column F is year (1st July)          (column 5)
   *       Column G is age 0 (x>=0, x<1)        (column 6)
   *       Column CH is age 79. (x>=79, x<80)   (column 85)
   *       
   *       FOR THE ESTIMATES SHEET:-
   *       
   *         For years 1950..1989 inclusive: 
   *           Column CI is age 80+ (x>=80)       (column 86)
   *           column CJ..DD is "..." = NA
   *         
   *         For years 1990..2100 inclusive: 
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
 
  private final static String[] xlsx_files = new String[] {
      "WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx",
      "WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx",
      "WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx"};
    
  public final static byte BOTH_GENDER=0;
  public final static byte MALE=1;
  public final static byte FEMALE=2;
  
  private final static String SHEET_ESTIMATES = "ESTIMATES";
  private final static String SHEET_MEDIUM = "MEDIUM VARIANT";
  private final static int FIRST_DATA_ROW = 17;
  private final static int XLSX_COUNTRY_COL = 4;
  private final static int XLSX_YEAR_COL = 5;  
  

  public int get(int age, String c3, byte gender, int year) {
    while (c3.length()<3) c3="0"+c3;
    c3=c3.toUpperCase();
    int c_index=-1;
    if (c3.compareTo("AAA")<0) c_index = country_i3.indexOf(c3);
    else c_index=country_a3.indexOf(c3);
    if (c_index==-1) {
      System.out.println("Country not found");
      return Integer.MIN_VALUE;
    } else if ((gender<0) || (gender>2)) { 
      System.out.println("Gender value wrong");
      return Integer.MIN_VALUE;
    } else if ((year<1950) || (year>2100)) {
      System.out.println("Year wrong");
      return Integer.MIN_VALUE;
    } else if ((year<1990) && ((age<0) || (age>80))) {
      System.out.println("Age out of bounds");
      return Integer.MIN_VALUE;
    } else if ((year>=1990) && ((age<0) || (age>100))) {
      System.out.println("Age out of bounds");
      return Integer.MIN_VALUE;
    } else {
      return getDataUnchecked(gender,c_index,year,age);
    }
  }
  
  private class LineParser implements XLSXLineParser {
    int row_no = 0;
    byte gender = -1;
    String current_sheet = "";
    String last_country_code = "";
    int last_country_index = -1;
    
    public LineParser(byte _gender) { gender=_gender; }
    
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
            if (current_sheet.equals(SHEET_ESTIMATES)) {
              if (year<1990) {
                for (int age=0; age<=80; age++)
                  setDataUnchecked(gender,last_country_index,year,age,(int) (Float.parseFloat(bits[6+age])*1000.0f));
            
              } else { // Skip "80+" column for 1990 and later.
                for (int age=0; age<=79; age++) 
                  setDataUnchecked(gender,last_country_index,year,age,(int) (Float.parseFloat(bits[6+age])*1000.0f));
                for (int age=80; age<=100; age++) 
                  setDataUnchecked(gender,last_country_index,year,age,(int) (Float.parseFloat(bits[7+age])*1000.0f));
              }
            } else { // Must be MEDIUM VARIANT sheet. year is always more than 1990 - no "80+" column.

              for (int age=0; age<=100; age++) 
                setDataUnchecked(gender,last_country_index,year,age,(int) (Float.parseFloat(bits[6+age])*1000.0f));
               
            }
          }
        }
      }
      row_no++;   
    }
  }
  
  public void dump(PrintStream p, String[] filter_countries) {
    p.append("age_from,age_to,value,date_start,date_end,projection_variant,gender,country\n");
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
        for (byte g=0; g<no_genders; g++) {
          String gg = (g==0)?"B":(g==1)?"M":"F";
          for (int y=1950; y<=1989; y++) {
            for (int a=0; a<=79; a++) {
              p.append(a+","+(a+1)+","+get(a,i3,g,y)+","+y+"0701"+","+(y+1)+"0630,E,"+gg+","+i3+"\n");
            }
            p.append("80,120,"+get(80,i3,g,y)+","+y+"0701"+","+(y+1)+"0630,E,"+gg+","+i3+"\n");
          }
          for (int y=1990; y<=2100; y++) {
            String proj=(y<=2015)?"E":"M";
            for (int a=0; a<=99; a++) {
              p.append(a+","+(a+1)+","+get(a,i3,g,y)+","+y+"0701"+","+(y+1)+"0630,"+proj+","+gg+","+i3+"\n");
            }
            p.append("100,120,"+get(100,i3,g,y)+","+y+"0701"+","+(y+1)+"0630,"+proj+","+gg+","+i3+"\n");
          }
        }
      }
    }
  }
   
  public InterpolatedPopulation(String path, Element _iso3166) throws Exception {
    country_a3 = new ArrayList<String>();
    country_i3 = new ArrayList<String>();
    iso3166 = _iso3166;
    
    for (byte gender=0; gender<3; gender++) { 
      VIMC_XLSXDocumentParser.parse(path+(path.endsWith(File.separator)?"":File.separator)+xlsx_files[gender], new LineParser(gender));
    }
  }
  
}
