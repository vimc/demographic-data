package com.vimc.demography;

import org.w3c.dom.Element;

import com.vimc.demography.tools.Tools;
import com.vimc.demography.unwpp2017.AgeSpecificFertility;
import com.vimc.demography.unwpp2017.InterpolatedDemographicIndicators;
import com.vimc.demography.unwpp2017.InterpolatedPopulation;
import com.vimc.demography.unwpp2017.SexRatioAtBirth;

public class Test {
  
  public static void checkInitData() throws Exception {
    Tools.ensureDirectoriesExist(new String[] {"data/wpp2012","data/wpp2015","data/wpp2017"});
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls","data/wpp2012/WPP2012_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls","data/wpp2012/WPP2012_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls","data/wpp2012/WPP2012_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls","data/wpp2015/WPP2015_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.XLS",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls","data/wpp2015/WPP2015_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.XLS",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls","data/wpp2015/WPP2015_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.XLS",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_FERT_F02_SEX_RATIO_AT_BIRTH.xlsx","data/wpp2017/WPP2017_FERT_F02_SEX_RATIO_AT_BIRTH.xlsx",true);    
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_FERT_F07_AGE_SPECIFIC_FERTILITY.xlsx","data/wpp2017/WPP2017_FERT_F07_AGE_SPECIFIC_FERTILITY.xlsx",true);    
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F01_ANNUAL_DEMOGRAPHIC_INDICATORS.xlsx","data/wpp2017/WPP2017_INT_F01_ANNUAL_DEMOGRAPHIC_INDICATORS.xlsx",true);    
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx","data/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx","data/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx","data/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_POP_F07_1_POPULATION_BY_AGE_BOTH_SEXES.xlsx","data/wpp2017/WPP2017_POP_F07_1_POPULATION_BY_AGE_BOTH_SEXES.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_POP_F07_2_POPULATION_BY_AGE_MALE.xlsx","data/wpp2017/WPP2017_POP_F07_2_POPULATION_BY_AGE_MALE.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_POP_F07_3_POPULATION_BY_AGE_FEMALE.xlsx","data/wpp2017/WPP2017_POP_F07_3_POPULATION_BY_AGE_FEMALE.xlsx",true);
    Tools.downloadFile("https://mrcdata.dide.ic.ac.uk/resources/iso3166.xml","data/iso3166.xml",true);
  }
  
  
  
  public static void main(String[] args) throws Exception {
    checkInitData();
    Element iso3166 = Tools.loadDocument("data/iso3166.xml");

    // Some quick point-test examples of each dataset.
    
    InterpolatedPopulation ip2017 = new InterpolatedPopulation("data/wpp2017",iso3166);
    System.out.println("GBR, male, 1963, 25 = "+ip2017.get(25,"GBR", InterpolatedPopulation.MALE, 1963));
    System.out.println("UGA, Female, 2072, 72 = "+ip2017.get(72,"UGA", InterpolatedPopulation.FEMALE, 2072));    
    
    InterpolatedDemographicIndicators idi = new InterpolatedDemographicIndicators("data/wpp2017",iso3166);
    System.out.println("GBR, life expect(0), 2015 = "+idi.get("GBR", InterpolatedDemographicIndicators.LIFE_EXPECTANCY_AT_BIRTH_MALE, 2015));
    
    SexRatioAtBirth srab = new SexRatioAtBirth("data/wpp2017",iso3166);
    System.out.println("GBR, 2045 = "+srab.get("GBR",2045));
    
    AgeSpecificFertility asf = new AgeSpecificFertility("data/wpp2017",iso3166);
    System.out.println("GBR, 2045 = "+asf.get(25,"GBR",AgeSpecificFertility.MEDIUM_VARIANT,2030));
    
    com.vimc.demography.unwpp2015.InterpolatedPopulation ip2015 = new com.vimc.demography.unwpp2015.InterpolatedPopulation("data/wpp2015",iso3166);
    System.out.println("GBR, male, 1963, 25 = "+ip2015.get(25,"GBR", com.vimc.demography.unwpp2015.InterpolatedPopulation.MALE, 1963));
    System.out.println("UGA, Female, 2072, age 72 = "+ip2015.get(72,"UGA", com.vimc.demography.unwpp2015.InterpolatedPopulation.FEMALE, 2072));    

    com.vimc.demography.unwpp2012.InterpolatedPopulation ip2012 = new com.vimc.demography.unwpp2012.InterpolatedPopulation("data/wpp2012",iso3166);
    System.out.println("GBR, male, 1963, 25 = "+ip2012.get(25,"GBR", com.vimc.demography.unwpp2012.InterpolatedPopulation.MALE, 1963));
    System.out.println("UGA, Female, 2072, 72 = "+ip2012.get(72,"UGA", com.vimc.demography.unwpp2012.InterpolatedPopulation.FEMALE, 2072));    
    System.out.println("800 (=UGA), Female, 2072, 72 = "+ip2012.get(72,"800", com.vimc.demography.unwpp2012.InterpolatedPopulation.FEMALE, 2072));    

    com.vimc.demography.unwpp2017.Population p2017 = new com.vimc.demography.unwpp2017.Population("data/wpp2017",iso3166);
    System.out.println("GBR, male, 2050, 25, low = "+p2017.get(25,"GBR", com.vimc.demography.unwpp2017.Population.MALE, 
                                                               com.vimc.demography.unwpp2017.Population.LOW_VARIANT,2050));
    System.out.println("GBR, male, 2050, 25, med = "+p2017.get(25,"GBR", com.vimc.demography.unwpp2017.Population.MALE, 
                                                               com.vimc.demography.unwpp2017.Population.MEDIUM_VARIANT,2050));
    System.out.println("GBR, male, 2050, 25, high= "+p2017.get(25,"GBR", com.vimc.demography.unwpp2017.Population.MALE, 
                                                               com.vimc.demography.unwpp2017.Population.HIGH_VARIANT,2050));
    // And some dump examples.
    
    ip2017.dump(System.out,new String[] {"PAK","IND","ETH","NIG","COD"}); // Pine countries.
    ip2015.dump(System.out,null); // All countries     
    ip2012.dump(System.out,null); // All countries
    srab.dump(System.out, null);
    p2017.dump(System.out, new String[] {"GBR"},  new String[] {"E","H","M","L"}); // Estimates, high, medium, low variant. (See Population.java for other codes)
    
    
  }
}
