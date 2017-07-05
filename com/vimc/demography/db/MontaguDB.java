package com.vimc.demography.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.w3c.dom.Element;

public class MontaguDB {
  
  public static final String[] genders = new String[] {"Both",
                                                       "Male",
                                                       "Female"};
  
  public static final byte GENDER_BOTH=0;
  public static final byte GENDER_MALE=1;  
  public static final byte GENDER_FEMALE=2;  
  
  public static final String[] projections = new String[] {"UNWPP_Estimates",
                                                           "UNWPP_Medium_Variant",
                                                           "UNWPP_High_Variant",
                                                           "UNWPP_Low_Variant",
                                                           "UNWPP_Constant_Fertility",
                                                           "UNWPP_Instant_Replacement",
                                                           "UNWPP_Momentum",
                                                           "UNWPP_Zero_Migration",
                                                           "UNWPP_Constant_Mortality",
                                                           "UNWPP_No_Change"};
  
  public static final byte UNWPP_ESTIMATES = 0;
  public static final byte UNWPP_MEDIUM = 1;
  public static final byte UNWPP_HIGH = 2;
  public static final byte UNWPP_LOW = 3;
    
  public static final String[] sources = new String[] {"UNWPP_2012",
                                                       "UNWPP_2015",
                                                       "UNWPP_2017"};
  
  public static final byte UNWPP_2012 = 0;
  public static final byte UNWPP_2015 = 1;  
  public static final byte UNWPP_2017 = 2;  
  
  public static final String[] demographic_statistic_types = new String[] {"Interpolated_Population",
                                                                           "Quinquennial_Population",
                                                                           "Live_Births",
                                                                           "Life_Table",
                                                                           "Under_5_Mortality_Rate",
                                                                           "Infant_Mortality_Rate",
                                                                           "Neonatal_Mortality_Rate",
                                                                           "Life_Expectancy_At_Birth",
                                                                           "Age_Specific_Fertility",
                                                                           "Birth_Gender_Ratio"};
  
  public static final byte INTERPOLATED_POPULATION = 0;
  public static final byte QUINQUENNIAL_POPULATION = 1;  

  public static final String[] demographic_statistic_age_inf = new String[] {"Age_years",
                                                                             "Ageband_5years",
                                                                             "NULL",
                                                                             "Cohort_Age",
                                                                             "NULL",
                                                                             "NULL",
                                                                             "NULL",
                                                                             "NULL",
                                                                             "Ageband_5years_mother",
                                                                             "NULL"};
  
  private Connection c;
  
  private void createGenericIDTable(String name, String[] entries) {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE "+name);
      } catch (Exception e) {}
      int max=0;
      for (int i=0; i<entries.length; i++) max=Math.max(max, entries[i].length());
      stmt.executeUpdate("CREATE TABLE "+name+" (id int NOT NULL UNIQUE, name varchar("+max+") NOT NULL)");
      for (int i=0; i<entries.length; i++) 
        stmt.executeUpdate("INSERT INTO "+name+" (id,name) VALUES ("+i+",'"+entries[i]+"')");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  private void createGendersTable() {
    createGenericIDTable("gender",genders);
  }
  
  private void createProjectionsTable() {
    createGenericIDTable("projection_variant",projections);
  }
   
  private void createSourcesTable() {
    createGenericIDTable("source",sources);
  }
  
  private void createDataTypesTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE demographic_statistic_type");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE demographic_statistic_type (id int NOT NULL UNIQUE, age_interpretation varchar(50), name varchar(50))");
      for (int i=0; i<demographic_statistic_types.length; i++)
        stmt.executeUpdate("INSERT INTO demographic_statistic_type (id,age_interpretation,name) VALUES ("+i+",'"+demographic_statistic_age_inf[i]+"', '"+demographic_statistic_types+"')");
      stmt.close();        
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  
  private void createDataTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE demographic_statistic");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE demographic_statistic (id SERIAL, age_from int, age_to int, value real, date_start int, "+
                         "date_end int, projection_variant int, gender int, country int, source int, demographic_statistic_type int)");
      stmt.close();
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  
  
  @SuppressWarnings("unused")
  private int strIndex(String[] s, String v) {
    int result=-1;
    for (int i=0; i<s.length; i++) { 
      if (s[i].equals(v)) {
        result=i;
        i=s.length;
      }
    }
    return result;
  }
  
  public void InitNew() throws Exception {
    // Drops and rebuilds all the demographic tables
    createGendersTable();
    createProjectionsTable();
    createDataTypesTable();
    createSourcesTable();
    createDataTable();
  }
  
  public void populate(Element iso3166) throws Exception {
    com.vimc.demography.unwpp2015.InterpolatedPopulation ip2015 = new com.vimc.demography.unwpp2015.InterpolatedPopulation("data/wpp2015",iso3166);
    ip2015.toSQL(c, null);
    com.vimc.demography.unwpp2012.InterpolatedPopulation ip2012 = new com.vimc.demography.unwpp2012.InterpolatedPopulation("data/wpp2012",iso3166);
    ip2012.toSQL(c, null);
    com.vimc.demography.unwpp2017.InterpolatedPopulation ip2017 = new com.vimc.demography.unwpp2017.InterpolatedPopulation("data/wpp2017",iso3166);
    ip2017.toSQL(c, null);

  }
  
  public void test() throws Exception {
    Statement stmt = c.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * from demographic_statistic");
    while (rs.next()) {
      System.out.println(rs.getInt("country")+","+rs.getInt("gender")+","+rs.getInt("age_from")+","+rs.getInt("age_to")+","+rs.getInt("value"));
    }
    rs.close();
    stmt.close();
    
  }
    
  public MontaguDB() {
    c = null;
    try {
      Class.forName("org.postgresql.Driver");
      c = DriverManager.getConnection("jdbc:postgresql://support.montagu.dide.ic.ac.uk:8888/montagu","vimc", "changeme");
    
    } catch (Exception e) {
       e.printStackTrace();
       System.err.println(e.getClass().getName()+": "+e.getMessage());
       System.exit(0);
    }
  }
}
