#!/bin/bash

rm -r target/dist
mkdir target/dist

cp /Users/jasonwjones/.m2/repository/com/essbase/essbase-es-server/11.1.2.3/essbase-es-server-11.1.2.3.jar  target/dist/
cp /Users/jasonwjones/.m2/repository/com/essbase/essbase-japi/11.1.2.3/essbase-japi-11.1.2.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/oracle/core/ojdl/logging/odl-logging/11.1.2.3/odl-logging-11.1.2.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/org/slf4j/log4j-over-slf4j/1.7.8/log4j-over-slf4j-1.7.8.jar target/dist/
cp /Users/jasonwjones/.m2/repository/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9.jar target/dist/
cp /Users/jasonwjones/Development/Hyperion/essbase-jdbc/target/essbase-jdbc-0.0.1-SNAPSHOT.jar target/dist/
cp /Users/jasonwjones/.m2/repository/com/h2database/h2/1.4.187/h2-1.4.187.jar target/dist/
cp /Users/jasonwjones/.m2/repository/com/saxifrages/essbase/essbase-utils/1.2.0/essbase-utils-1.2.0.jar target/dist/
cp /Users/jasonwjones/.m2/repository/ch/qos/logback/logback-classic/1.1.3/logback-classic-1.1.3.jar target/dist/
cp /Users/jasonwjones/.m2/repository/ch/qos/logback/logback-core/1.1.3/logback-core-1.1.3.jar target/dist/

cp target/essbase-jdbc-*.jar target/dist/
cp NOTES.md target/dist/
