package com.vimc.demography.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.w3c.dom.Element;

public class MontaguDB {
  

  public static final String[] countries_97 = new String[] {"AFG","ALB","AGO","ARM","AZE","BGD","BLZ","BEN","BTN","BOL","BIH","BFA","BDI","KHM","CMR",
                  "CPV","CAF","TCD","CHN","COM","COD","COG","CIV","CUB","DJI","EGY","SLV","ERI","ETH","FJI",
                  "GMB","GEO","GHA","GTM","GIN","GNB","GUY","HTI","HND","IND","IDN","IRQ","KEN","KIR","PRK",
                   "XK","KGZ","LAO","LSO","LBR","MDG","MWI","MLI","MHL","MRT","FSM","MDA","MNG","MAR","MOZ",
                  "MMR","NPL","NIC","NER","NGA","PAK","PNG","PRY","PHL","RWA","WSM","STP","SEN","SLE","SLB",
                  "SOM","LKA","SDN","SSD","SWZ","SYR","TJK","TZA","TLS","TGO","TON","TKM","TUV","UGA","UKR",
                  "UZB","VUT","VNM","PSE","YEM","ZMB","ZWE"};
  
  public static final String[] gender_ids = new String[] {"BOTH","MALE", "FEMALE"};
  public static final String[] gender_names = new String[] {"Both","Male","Female"};
  
  public static final byte GENDER_BOTH=0;
  public static final byte GENDER_MALE=1;  
  public static final byte GENDER_FEMALE=2;  
  
  public static final String[] projection_names = new String[] {"UNWPP_Estimates",
                                                           "UNWPP_Medium_Variant",
                                                           "UNWPP_High_Variant",
                                                           "UNWPP_Low_Variant",
                                                           "UNWPP_Constant_Fertility",
                                                           "UNWPP_Instant_Replacement",
                                                           "UNWPP_Momentum",
                                                           "UNWPP_Zero_Migration",
                                                           "UNWPP_Constant_Mortality",
                                                           "UNWPP_No_Change"};
  
  public static final String[] projection_ids = new String[] {"ESTIMATES",
      "MEDIUM_VARIANT",
      "HIGH_VARIANT",
      "LOW_VARIANT",
      "CONSTANT_FERTILITY",
      "INSTANT_REPLACEMENT",
      "MOMENTUM",
      "ZERO_MIGRATION",
      "CONSTANT_MORTALITY",
      "NO_CHANGE"};
  

  
  public static final byte UNWPP_ESTIMATES = 0;
  public static final byte UNWPP_MEDIUM = 1;
  public static final byte UNWPP_HIGH = 2;
  public static final byte UNWPP_LOW = 3;
    
  public static final String[] source_ids = new String[] {"UNWPP_2012",
                                                       "UNWPP_2015",
                                                       "UNWPP_2017"};
  
  public static final String[] source_names = new String[] {"UNWPP 2012","UNWPP 2015","UNWPP 2017"};
  
  public static final byte UNWPP_2012 = 0;
  public static final byte UNWPP_2015 = 1;  
  public static final byte UNWPP_2017 = 2;  
  
  public static final String[] demographic_statistic_types_names = new String[] {"Interpolated_Population"};

  public static final String[] demographic_statistic_types_ids = new String[] {"INT_POP"};


  public static final String[] demographic_statistic_age_inf = new String[] {"Age_years"};
  
  public static final byte INT_POP = 0;
                                                                             
  
  private Connection c;

  
  private void createGendersTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"gender\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"gender\" ( \"id\" TEXT NOT NULL , \"name\" VARCHAR(6) NOT NULL , PRIMARY KEY (\"id\"));");
      for (int i=0; i<gender_ids.length; i++) stmt.executeUpdate("INSERT INTO \"gender\" (id,name) VALUES ('"+gender_ids[i]+"','"+gender_names[i]+"');");
      stmt.close();      
    } catch (Exception e) { e.printStackTrace(); }
  }
  
  private void createProjectionsTable() {
    try {
      Statement stmt = c.createStatement();
      try { // Drop it if it already exists
        stmt.execute("DROP TABLE \"projection_variant\"");
      } catch (Exception e) {}
      stmt.executeUpdate("CREATE TABLE \"projection_variant\" ( \"id\" TEXT NOT NULL , \"name\" VARCHAR NOT NULL DEFAULT 'NULL', PRIMARY KEY (\"id\"));");
      for (int i=0; i<projection_ids.length; i++) stmt.executeUpdate("INSERT INTO \"projection_variant\" (id,name) VALUES ('"+projection_ids[i]+"','"+projection_names[i]+"');");
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
      for (int i=0; i<source_ids.length; i++) stmt.executeUpdate("INSERT INTO \"source\" (id,name) VALUES ('"+source_ids[i]+"','"+source_names[i]+"');");
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
      for (int i=0; i<demographic_statistic_types_ids.length; i++)
        stmt.executeUpdate("INSERT INTO demographic_statistic_type (id,age_interpretation,name) VALUES ('"+demographic_statistic_types_ids[i]+"','"+demographic_statistic_age_inf[i]+"', '"+demographic_statistic_types_names[i]+"')");
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
                         "\"date_end\" DATE NOT NULL, \"projection_variant\" TEXT NOT NULL, \"gender\" TEXT NOT NULL, \"country\" TEXT NOT NULL, \"source\" TEXT NOT NULL, \"demographic_statistic_type\" TEXT NOT NULL, PRIMARY KEY (\"id\"));");
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
    createDataTable();
    createGendersTable();
    createProjectionsTable();
    createDataTypesTable();
    createSourcesTable();
  }
  
  public void populate(Element iso3166) throws Exception {
    long xls_time=0;
    long x = System.currentTimeMillis();
    long start=x;
    com.vimc.demography.unwpp2015.InterpolatedPopulation ip2015 = new com.vimc.demography.unwpp2015.InterpolatedPopulation("data/wpp2015",iso3166);
    xls_time+=System.currentTimeMillis()-x;
    long[] times = ip2015.toSQL(c, countries_97);
    
    x = System.currentTimeMillis();
    com.vimc.demography.unwpp2012.InterpolatedPopulation ip2012 = new com.vimc.demography.unwpp2012.InterpolatedPopulation("data/wpp2012",iso3166);
    xls_time+=System.currentTimeMillis()-x;
    long[] times2 = ip2012.toSQL(c, countries_97);
    
    x = System.currentTimeMillis();
    com.vimc.demography.unwpp2017.InterpolatedPopulation ip2017 = new com.vimc.demography.unwpp2017.InterpolatedPopulation("data/wpp2017",iso3166);
    xls_time+=System.currentTimeMillis()-x;
    long[] times3 = ip2017.toSQL(c, countries_97);
    
    long y = System.currentTimeMillis();
    System.out.println("Done in "+(y-start)/1000.0f+" seconds");
    System.out.println("Excel time: "+(xls_time/1000.f)+" seoncds");
    System.out.println("Processing time: "+((times[0]+times2[0]+times3[0])/1000.f)+" seconds");
    System.out.println("DB Query time: "+((times[1]+times2[1]+times3[1])/1000.f)+" seconds");
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
