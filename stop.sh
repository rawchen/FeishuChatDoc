#!/bin/sh
PID=$(cat /var/run/FeishuChatDoc.pid)
kill -9 $PID
