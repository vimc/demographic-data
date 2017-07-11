install.packages('RPostgreSQL')
install.packages('readxl')
install.packages('xml2')

require(RPostgreSQL)
require(readxl)
require(xml2)


init_country_table <- function(db,iso3166) {
  #Not sure if this is necessary on main DB 
  #Perhaps just for my test...
  for (i in 1:nrow(iso3166)) {
    sql <- sprintf("INSERT INTO country (id,name) VALUES ('%s','%s');",
                   iso3166[i,"code"],iso3166[i,"code"]);
    dbExecute(db,sql)
  }
}

init_tables <- function(db) {
  gender_table <- data.frame(c("BOTH","MALE","FEMALE"),
                             c("Both","Male","Female"))
  colnames(gender_table) <- c("id","name")
  dbWriteTable(db,"gender",gender_table, append=TRUE)

  
  projection_table <- data.frame(c("ESTIMATES","MEDIUM_VARIANT","HIGH_VARIANT","LOW_VARIANT","CONSTANT_FERTILITY",
                                   "INSTANT_REPLACEMENT","MOMENTUM","ZERO_MIGRATION","CONSTANT_MORTALITY","NO_CHANGE"),
                                 c("Estimates","Medium Variant","High Variant","Low Variant","Constant Fertility",
                                   "Instant Replacement","Momentum","Zero Migration","Constant Mortality","No Change"))
  colnames(projection_table) <- c("id","name")
  
  dbWriteTable(db,"projection_variant",projection_table, append=TRUE)

  
  demographic_statistic_type_table <- data.frame(c("INT_POP"),c("Age (years)"),c("Interpolated Population"))
  colnames(demographic_statistic_type_table) <- c("id","age_interpretation","name")
  
  dbWriteTable(db,"demographic_statistic_type",demographic_statistic_type_table, append=TRUE)


  source_table <- data.frame(c("UNWPP_2012","UNWPP_2015","UNWPP_2017"),
                             c("UNWPP 2012","UNWPP 2015","UNWPP 2017"))
  colnames(source_table) <- c("id","name")
  
  dbWriteTable(db,"source",source_table, append=TRUE)
}


download_single <- function(url, dest) {
  if (!file.exists(dest)) download.file(url,dest,method='libcurl', mode="wb")
}

download_data <- function() {
  if (!file.exists('data')) dir.create('data')
  if (!file.exists('data/wpp2012')) dir.create('data/wpp2012')
  if (!file.exists('data/wpp2015')) dir.create('data/wpp2015')
  if (!file.exists('data/wpp2017')) dir.create('data/wpp2017')
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls","data/wpp2012/WPP2012_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls","data/wpp2012/WPP2012_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2012/WPP2012_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls","data/wpp2012/WPP2012_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls","data/wpp2015/WPP2015_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.XLS")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls","data/wpp2015/WPP2015_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.XLS")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2015/WPP2015_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls","data/wpp2015/WPP2015_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.XLS")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx","data/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx","data/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/unwpp/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx","data/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx")
  download_single("https://mrcdata.dide.ic.ac.uk/resources/iso3166.xml","data/iso3166.xml")
}

process_interpolated_population <- function(db, xlfile, gender, sheets, variant_names,source, iso3166) {
  
  big_frame <- data.frame(age_from = integer(0),
                          age_to = integer(0),
                          value = numeric(0),
                          date_start = character(0),
                          date_end = character(0),
                          projection_variant = character(0),
                          gender= character(0),
                          country = character(0),
                          demographic_statistic_type = character(0),
                          source = character(0),
                          stringsAsFactors=FALSE)
  
  for (sheet in 1:length(sheets)) {
    # Add ISO3 column to XLS and select only the countries we want
    
    xl <- read_excel(xlfile, sheet=sheets[sheet], skip=16, col_names=TRUE)
    select_countries <- match(xl$"Country code", iso3166$id)
    xl$iso3 <- iso3166$code[select_countries]
    j <- !is.na(xl$iso3)
    xl_subset<-as.data.frame(xl[j, ])
    
    age_cols_pre_1990 <- as.character(c(0:79,"80+"))
    age_cols_from_1990 <- as.character(c(0:99,"100+"))
    
    
    for (rowno in 1:nrow(xl_subset)) {
      ## Change column 6 to something better
      year<-xl_subset[rowno,6]
      
      # Each excel row is either 81, or 101 db rows, depending on year.
      
      if (year<1990) {
        age_from   <- c(0:80)
        age_to     <- c(0:79,120)
        values     <- unname(unlist(as.numeric(xl_subset[rowno,age_cols_pre_1990])))
      } else {
        age_from   <- c(0:100)
        age_to     <- c(0:99,120)
        values     <- unname(unlist(as.numeric(xl_subset[rowno,age_cols_from_1990])))
      }
        
      date_from  <- rep(paste(as.character(year),"-07-01",sep=""),length(age_from))
      date_to    <- rep(paste(as.character(year+1),"-06-30",sep=""),length(age_from))
      projection <- rep(variant_names[sheet],length(age_from))
      df_gender  <- rep(gender,length(age_from))
      country    <- rep(xl_subset[rowno,"iso3"],length(age_from))
      dstype     <- rep("INT_POP",length(age_from))
      df_source  <- rep(source,length(age_from))
      
      big_frame <- rbind(big_frame, data.frame(
                              age_from = age_from,
                              age_to = age_to,
                              value = values,
                              date_start = date_from,
                              date_end = date_to,
                              projection_variant = projection,
                              gender= df_gender,
                              country = country,
                              demographic_statistic_type = dstype,
                              source = df_source,
                              stringsAsFactors=FALSE)
      )

    }
  }
  dbWriteTable(db, "demographic_statistic", big_frame, append=TRUE)
}


process_all_interpolated_population <- function(db, iso3166) {
  variant_names <- c("ESTIMATES","MEDIUM_VARIANT")
  sheet_names_2015 <- c("ESTIMATES","MEDIUM VARIANT")
  sheet_names_2012 <- c("ESTIMATES","MEDIUM FERTILITY")
  
  #2015
  process_interpolated_population(db,
                                  "data/wpp2015/WPP2015_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls",
                                  "BOTH", sheet_names_2015, variant_names, "UNWPP_2015", iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2015/WPP2015_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls",
                                  "MALE", sheet_names_2015, variant_names, "UNWPP_2015",iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2015/WPP2015_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls",
                                  "FEMALE", sheet_names_2015, variant_names, "UNWPP_2015",iso3166)
  
  
  #2017
  process_interpolated_population(db,
                                  "data/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx",
                                  "BOTH", sheet_names_2015, variant_names, "UNWPP_2017", iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx",
                                  "MALE", sheet_names_2015,variant_names, "UNWPP_2017", iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx",
                                  "FEMALE", sheet_names_2015, variant_names, "UNWPP_2017", iso3166)
  
  
  #2012
  process_interpolated_population(db,
                                  "data/wpp2012/WPP2012_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xls",
                                  "BOTH",sheet_names_2012, variant_names, "UNWPP_2012",iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2012/WPP2012_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xls",
                                  "MALE",sheet_names_2012, variant_names, "UNWPP_2012",iso3166)
  
  process_interpolated_population(db,
                                  "data/wpp2012/WPP2012_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xls",
                                  "FEMALE",sheet_names_2012, variant_names, "UNWPP_2012",iso3166)
  
}


###################################################################################

# Check that all the XLS/XLSX files exist.
download_data()

# Countries we care about.
countries_97 <- c("AFG","ALB","AGO","ARM","AZE","BGD","BLZ","BEN","BTN","BOL","BIH","BFA","BDI","KHM","CMR",
                  "CPV","CAF","TCD","CHN","COM","COD","COG","CIV","CUB","DJI","EGY","SLV","ERI","ETH","FJI",
                  "GMB","GEO","GHA","GTM","GIN","GNB","GUY","HTI","HND","IND","IDN","IRQ","KEN","KIR","PRK",
                   "XK","KGZ","LAO","LSO","LBR","MDG","MWI","MLI","MHL","MRT","FSM","MDA","MNG","MAR","MOZ",
                  "MMR","NPL","NIC","NER","NGA","PAK","PNG","PRY","PHL","RWA","WSM","STP","SEN","SLE","SLB",
                  "SOM","LKA","SDN","SSD","SWZ","SYR","TJK","TZA","TLS","TGO","TON","TKM","TUV","UGA","UKR",
                  "UZB","VUT","VNM","PSE","YEM","ZMB","ZWE")

# Load full ISO 3166 XML file, and XML->DataFrame (id,code)

iso3166 <- read_xml("data/iso3166.xml")
xml_countries <- xml2::xml_find_all(iso3166, "//c")
xml_n3 <- xml2::xml_attr(xml_countries, "n3")
xml_c3 <- xml2::xml_attr(xml_countries, "c3")
iso3166_df<- data.frame(id = as.numeric(xml_n3), 
                        code = xml_c3, 
                        stringsAsFactors = FALSE)

# Filter to only include countries we care about.

iso3166_df_97 <- iso3166_df[iso3166_df$code %in% countries_97,]


db <- DBI::dbConnect(RPostgres::Postgres(),
                      dbname = "montagu",
                      host = "support.montagu.dide.ic.ac.uk",
                      port = 6543,
                      password = "changeme",
                      user = "vimc")


# Initialise the enum tables
init_tables(db)

# Only do this on test DB if country table isn't already populated.
#init_country_table(db,iso3166_df)

process_all_interpolated_population(db, iso3166_df_97)





dbGetQuery(db, "SELECT * from demographic_statistic where id<100")

rs <- dbSendQuery(db, "SELECT * from demographic_statistic")
dbFetch(rs)

