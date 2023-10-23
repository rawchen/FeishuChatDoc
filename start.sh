#!/bin/sh
APP_NAME=FeishuChatDoc
nohup java -Xmn48m -Xms128m -Xmx128m -Xss256k -jar $APP_NAME.jar --spring.config.location=/root/$APP_NAME/application.yml >> app.log 2>&1 &
echo $! > /var/run/$APP_NAME.pid
