FROM r-base
RUN apt-get update && apt-get -y install gnumeric
COPY xl2csv.sh /usr/bin/xl2csv.sh
ENTRYPOINT ["xl2csv.sh"]
