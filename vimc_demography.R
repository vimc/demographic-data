install.packages('RPostgreSQL')
install.packages('readxl')
install.packages('xml2')

require(readxl)
require(xml2)


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
iso3166_df<- data.frame(id = as.numeric(xml2::xml_attr(xml_countries, "n3")), 
                        code = xml2::xml_attr(xml_countries, "c3"), 
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

