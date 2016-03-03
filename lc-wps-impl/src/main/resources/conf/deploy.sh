#!/bin/sh

CATALINA_HOME=/opt/tomcat
SERVICE_NAME=bc-tomcat
RUN_AS=tomcat

sudo service bc-tomcat stop
sudo -u $RUN_AS mkdir -p $CATALINA_HOME/conf/lc-cci
sudo -u $RUN_AS cp lc-cci-wps.properties $CATALINA_HOME/conf/lc-cci
sudo -u $RUN_AS cp *.jar $CATALINA_HOME/webapps/bc-wps/WEB-INF/lib
sudo service bc-tomcat start
