# demographic-data
Scripts for parsing/transforming UNWPP demographic spreadsheets

See the Test.java for example gets. 
The original WPP files get downloaded from a mirror on mrcdata.dide.ic.ac.uk into data/wpp2012, data/wpp2015 etc. (546Mb)

Files supported so far:

WPP 2012:
Interpolated Population                 (age_1, country, gender, year_1)

WPP 2015:
Interpolated Population                 (age_1, country, gender, year_1)

WPP 2017:
Interpolated Population                 (age_1, country,gender, year_1)
Standard 5-yearly Population            (age_5, country,gender, projection,year_5)
Interpolated Demographic Indicators     (country, field, year)
Sex Ratio At Birth                      (country, year)  (1000s males born per female)
Age Specific Fertility                  (age_of_mother, country, projection, year_5)

If projection is not specified, then presume MEDIUM.

