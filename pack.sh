#!/bin/bash

mvn clean package -DskipTests=true

rm -r target/dist
mkdir target/dist

cp /Users/jasonwjones/.m2/repository/com/essbase/essbase-es-server/11.1.2.3/essbase-es-server-11.1.2.3.jar  target/dist/
cp /Users/jasonwjones/.m2/repository/com/essbase/essbase-japi/11.1.2.3/essbase-japi-11.1.2.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/oracle/core/ojdl/logging/odl-logging/11.1.2.3/odl-logging-11.1.2.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/org/slf4j/log4j-over-slf4j/1.7.8/log4j-over-slf4j-1.7.8.jar target/dist/
cp /Users/jasonwjones/.m2/repository/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9.jar target/dist/
cp /Users/jasonwjones/.m2/repository/com/h2database/h2/1.4.187/h2-1.4.187.jar target/dist/
cp /Users/jasonwjones/.m2/repository/com/saxifrages/essbase/essbase-utils/1.2.2/essbase-utils-1.2.2.jar target/dist/
cp /Users/jasonwjones/.m2/repository/ch/qos/logback/logback-classic/1.1.3/logback-classic-1.1.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/ch/qos/logback/logback-core/1.1.3/logback-core-1.1.3.jar target/dist/

cp /Users/jasonwjones/Development/Hyperion/vess-essbase-jdbc/target/vess-essbase-jdbc-0.0.1-SNAPSHOT.jar target/dist/

cp target/vess-essbase-jdbc-*.jar target/dist/
cp NOTES.md target/dist/

zip -j target/vess-dist.zip target/dist/*

#cp target/dist/*.jar ../vess-dist

# packaging for use with Dodeca:
# Only need:
# h2
# essbase-utils
# vess-essbase-jdbc

# scp /Users/jasonwjones/.m2/repository/com/h2database/h2/1.4.187/h2-1.4.187.jar dodeca@10.0.0.204:/var/lib/tomcat7/webapps/dodeca/WEB-INF/lib
# scp /Users/jasonwjones/.m2/repository/com/saxifrages/essbase/essbase-utils/1.2.0/essbase-utils-1.2.0.jar dodeca@10.0.0.204:/var/lib/tomcat7/webapps/dodeca/WEB-INF/lib
# scp vess-essbase-jdbc-0.0.1-SNAPSHOT.jar dodeca@10.0.0.204:/var/lib/tomcat7/webapps/dodeca/WEB-INF/lib
