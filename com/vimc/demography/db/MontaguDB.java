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
        stmt.execute("DROP TABLE \""+name+"\"");
      } catch (Exception e) {}
      int max=0;
      for (int i=0; i<entries.length; i++) max=Math.max(max, entries[i].length());
      stmt.executeUpdate("CREATE TABLE \""+name+"\" (\"id\" INTEGER NOT NULL UNIQUE, \"name\" TEXT NOT NULL, PRIMARY KEY (\"id\"));");
      for (int i=0; i<entries.length; i++) 
        stmt.executeUpdate("INSERT INTO "+name+" (id,name) VALUES ("+i+",'"+entries[i]+"')");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  private void createGendersTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"gender\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"gender\" ( \"id\" TEXT NOT NULL , \"name\" VARCHAR(6) NOT NULL , PRIMARY KEY (\"id\"));");
      for (int i=0; i<genders.length; i++) stmt.executeUpdate("INSERT INTO \"gender\" (id,name) VALUES ('"+i+"','"+genders[i]+"');");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  private void createProjectionsTable() {
    createGenericIDTable("projection_variant",projections);
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"projection_variant\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"projection_variant\" ( \"id\" SERIAL , \"name\" VARCHAR NOT NULL DEFAULT 'NULL', PRIMARY KEY (\"id\"));");
      for (int i=0; i<projections.length; i++) stmt.executeUpdate("INSERT INTO \"projection_variant\" (id,name) VALUES ("+i+",'"+projections[i]+"');");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
   
  private void createSourcesTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"source\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"source\" ( \"id\" TEXT NOT NULL , \"name\" VARCHAR NOT NULL , PRIMARY KEY (\"id\"));");
      for (int i=0; i<sources.length; i++) stmt.executeUpdate("INSERT INTO \"source\" (id,name) VALUES ('"+i+"','"+sources[i]+"');");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  private void createDataTypesTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"demographic_statistic_type\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE demographic_statistic_type (\"id\" TEXT NOT NULL, \"age_interpretation\" TEXT NOT NULL, \"name\" VARCHAR NOT NULL, PRIMARY KEY (\"id\"));");
      for (int i=0; i<demographic_statistic_types.length; i++)
        stmt.executeUpdate("INSERT INTO demographic_statistic_type (id,age_interpretation,name) VALUES ('"+i+"','"+demographic_statistic_age_inf[i]+"', '"+demographic_statistic_types[i]+"')");
      stmt.close();        
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  
  private void createDataTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"demographic_statistic\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"demographic_statistic\" (\"id\" SERIAL, \"age_from\" INTEGER NOT NULL, \"age_to\" INTEGER, \"value\" DECIMAL NOT NULL, \"date_start\" DATE NOT NULL, "+
                         "\"date_end\" DATE NOT NULL, \"projection_variant\" INTEGER, \"gender\" TEXT NOT NULL, \"country\" TEXT NOT NULL, \"source\" TEXT NOT NULL, \"demographic_statistic_type\" TEXT NOT NULL, PRIMARY KEY (\"id\"));");
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
  
  public void InitNew(Element iso3166) throws Exception {
    //  Drops and rebuilds all the demographic tables
    createGendersTable();
    createProjectionsTable();
    createDataTypesTable();
    createSourcesTable();
    createDataTable();
    createISO3166Table(iso3166);
  }
  
  public void populate(Element iso3166) throws Exception {
    com.vimc.demography.unwpp2015.InterpolatedPopulation ip2015 = new com.vimc.demography.unwpp2015.InterpolatedPopulation("data/wpp2015",iso3166);
    ip2015.toSQL(c, null);
    com.vimc.demography.unwpp2012.InterpolatedPopulation ip2012 = new com.vimc.demography.unwpp2012.InterpolatedPopulation("data/wpp2012",iso3166);
    ip2012.toSQL(c, null);
    com.vimc.demography.unwpp2017.InterpolatedPopulation ip2017 = new com.vimc.demography.unwpp2017.InterpolatedPopulation("data/wpp2017",iso3166);
    ip2017.toSQL(c, null);

  }
  
  public void createISO3166Table(Element iso3166) {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE iso3166");
      } catch (Exception e) {}
     /* stmt.executeUpdate("CREATE TABLE iso3166 (id int, c3 varchar(3), name varchar(255))");
      int countTags = Tools.countChildren(iso3166, "c");
      for (int i=0; i<countTags; i++) {
        Node n = Tools.getChildNo(iso3166,"c",i);
        String cname = Tools.getAttribute(n, "n");
        cname=cname.replace("'","`");
        stmt.executeUpdate("INSERT INTO iso3166 (id,c3,name) VALUES ("+Integer.parseInt(Tools.getAttribute(n,"n3"))+",'"+Tools.getAttribute(n, "c3")+"','"+cname+"')");
      }
      */
      stmt.close();
    } catch (Exception e) { e.printStackTrace(); }

    
  }
  
  public void test() throws Exception {
    Statement stmt = c.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * from demographic_statistic WHERE id=1");
    while (rs.next()) {
      System.out.println(rs.getString("country")+","+rs.getFloat("value"));
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
