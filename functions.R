init_country_table <- function(db,iso3166) {
  dbExecute(db, "DELETE FROM country")
  country <- data.frame(id = iso3166$code,
                        name = iso3166$code,
                        stringsAsFactors = FALSE)
  DBI::dbWriteTable(db, "country", country, append = TRUE)
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
  print(sprintf("Processing %s",xlfile))

  age_cols_pre_1990  <- as.character(c(0:79,"80+"))
  age_cols_from_1990 <- as.character(c(0:99,"100+"))
  age1_pre_1990  <- c(0L:80L)
  age2_pre_1990    <- c(0L:79L,120L)
  age1_from_1990 <- c(0L:100L)
  age2_from_1990   <- c(0L:99L,120L)

  #97 countries, 3 genders, (40 years of 81 ages), (112 years of 101 ages)
  alloc  <- 97 * 3 * ((40*81)+(112*101))
  print("Pre-allocation")

  df_age_from <- rep(1L,alloc)
  df_age_to <- rep(1L,alloc)
  df_value <- rep(1.0,alloc)
  df_date_start <- rep('yyyy-mm-dd',alloc)
  df_date_end <- rep('yyyy-mm-dd',alloc)
  df_projection <- rep('MEDIUM_VARIANT',alloc)
  df_gender <- rep('FEMALE',alloc)
  df_country <- rep('ZZZ',alloc)
  df_type <- rep('INT_POP',alloc)
  df_source <- rep(source,alloc)

  print("Build frame")

  big_frame <- data.frame(age_from = df_age_from,
                          age_to = df_age_to,
                          value = df_value,
                          date_start = df_date_start,
                          date_end = df_date_end,
                          projection_variant = df_projection,
                          gender= df_gender,
                          country = df_country,
                          demographic_statistic_type = df_type,
                          source = df_source,
                          stringsAsFactors=FALSE)

  db_rowno <- 1
  sheet_no <- 0

  while (sheet_no<length(sheets)) {
    sheet_no<-sheet_no+1
    print(sprintf("Sheet %d",sheet_no))

    variant_pre_1990  <- rep(sheets[sheet_no],81)
    variant_from_1990 <- rep(sheets[sheet_no],101)

    # Add ISO3 column to XLS and select only the countries we want

    xl <- read_excel(xlfile, sheet=sheets[sheet_no], skip=16, col_names=TRUE)
    select_countries <- match(xl$"Country code", iso3166$id)
    xl$iso3 <- iso3166$code[select_countries]
    j <- !is.na(xl$iso3)
    xl_subset<-as.data.frame(xl[j, ])

    rowno<-0
    while (rowno<nrow(xl_subset)) {
      rowno<-rowno+1
      if ((rowno %% 100)==0) print(sprintf("Row %d/%d",rowno,nrow(xl_subset)))

      # Change column 6 to something better
      year<-xl_subset[rowno,6]

      # Each excel row is either 81, or 101 db rows, depending on year.

      if (year<1990) {
        big_frame$age_from[db_rowno:(db_rowno+80)]     <- age1_pre_1990
        big_frame$age_to[db_rowno:(db_rowno+80)]       <- age2_pre_1990
        big_frame$value[db_rowno:(db_rowno+80)]        <- unname(unlist(as.numeric(xl_subset[rowno,age_cols_pre_1990])))
        big_frame$date_start[db_rowno:(db_rowno+80)]   <- rep(paste(as.character(year),"-07-01",sep=""),81)
        big_frame$date_end[db_rowno:(db_rowno+80)]     <- rep(paste(as.character(year+1),"-06-30",sep=""),81)
        big_frame$projection_variant[db_rowno:(db_rowno+80)]  <- variant_pre_1990
        big_frame$country[db_rowno:(db_rowno+80)]             <- rep(xl_subset[rowno,"iso3"],81)
        db_rowno<-(db_rowno+81)

      } else {
        big_frame$age_from[db_rowno:(db_rowno+100)]    <- age1_from_1990
        big_frame$age_to[db_rowno:(db_rowno+100)]      <- age2_from_1990
        big_frame$value[db_rowno:(db_rowno+100)]       <- unname(unlist(as.numeric(xl_subset[rowno,age_cols_from_1990])))
        big_frame$date_start[db_rowno:(db_rowno+100)]  <- rep(paste(as.character(year),"-07-01",sep=""),101)
        big_frame$date_end[db_rowno:(db_rowno+100)]    <- rep(paste(as.character(year+1),"-06-30",sep=""),101)
        big_frame$projection_variant[db_rowno:(db_rowno+100)]  <- variant_from_1990
        big_frame$country[db_rowno:(db_rowno+100)]             <- rep(xl_subset[rowno,"iso3"],101)
        db_rowno<-(db_rowno+101)
      }
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
#  process_interpolated_population(db,
#                                  "data/wpp2017/WPP2017_INT_F03_1_POPULATION_BY_AGE_ANNUAL_BOTH_SEXES.xlsx",
#                                  "BOTH", sheet_names_2015, variant_names, "UNWPP_2017", iso3166)

#  process_interpolated_population(db,
#                                  "data/wpp2017/WPP2017_INT_F03_2_POPULATION_BY_AGE_ANNUAL_MALE.xlsx",
#                                  "MALE", sheet_names_2015,variant_names, "UNWPP_2017", iso3166)

#  process_interpolated_population(db,
#                                  "data/wpp2017/WPP2017_INT_F03_3_POPULATION_BY_AGE_ANNUAL_FEMALE.xlsx",
#                                  "FEMALE", sheet_names_2015, variant_names, "UNWPP_2017", iso3166)


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
