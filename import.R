source("functions.R")

countries_keep <- readLines("countries_keep.txt")

download_data()

host <- Sys.getenv("MONTAGU_DB_HOST", "localhost")
port <- as.integer(Sys.getenv("MONTAGU_DB_PORT", 8888))

iso3166 <- read_xml("data/iso3166.xml")
xml_countries <- xml2::xml_find_all(iso3166, "//c")
iso3166_df<- data.frame(id = as.numeric(xml2::xml_attr(xml_countries, "n3")),
                        code = xml2::xml_attr(xml_countries, "c3"),
                        stringsAsFactors = FALSE)

# Filter to only include countries we care about.

iso3166_df_97 <- iso3166_df[iso3166_df$code %in% countries_keep, ]

db <- DBI::dbConnect(RPostgres::Postgres(),
                      dbname = "montagu",
                      host = host,
                      port = port,
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
