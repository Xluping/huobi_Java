#! /bin/sh

# 数组的形式,循环启动,  server 

# 端口号
PORTS=(8091  8092 8093 8094 8095)
# 系统模块
MODULES=(a b c d e)
# 系统模块名称
# MODULE_NAMES=(m1 m2 m3 m4 m5)
# jar包数组
JARS=(a3-jar-with-dependencies.jar b3-jar-with-dependencies.jar c3-jar-with-dependencies.jar d3-jar-with-dependencies.jar e3-jar-with-dependencies.jar)
# jar包路径
# JAR_PATH='/Users/xlp/Desktop/jars'
JAR_PATH='/root/jars'
# 日志路径
# LOG_PATH='/Users/xlp/Desktop/jars'
start() {
  local MODULE=
  # local MODULE_NAME=
  local JAR_NAME=
  local command1="$1"
  local SPOT="$2"
  local USDT="$3"
  local commandOk=0
  local count=0
  local okCount=0
  local port=0
  for((i=0;i<${#MODULES[@]};i++))
  do
    MODULE=${MODULES[$i]}
    # MODULE_NAME=${MODULE_NAMES[$i]}
    JAR_NAME=${JARS[$i]}
    PORT=${PORTS[$i]}
    if [ "$command1" == "all" ] || [ "$command1" == "$MODULE" ];then
      commandOk=1
      count=0
      PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
      if [ -n "$PID" ];then
        echo "$MODULE 3---$SPOT :已经运行,PID= $PID"
      else
          cd $JAR_PATH/$MODULE/
          java -jar -Dserver.port=$PORT $JAR_PATH/$MODULE/$JAR_NAME  "$SPOT" "$USDT" &
          # java -jar -Dserver.port=$PORT $JAR_PATH/$JAR_NAME  "$SPOT" "$USDT" &

        # PID=`netstat -apn | grep $PORT | awk '{print $7}' | cut -d/ -f 1`
        PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
        while [ -z "$PID" ]
        do
          if (($count == 30));then
            echo "$MODULE 3---$SPOT: $(expr $count \* 10)秒内未启动,请检查!"
            break
          fi
          count=$(($count + 1))
          echo "$SPOT $USDT 3启动中.................."
          sleep 10s
          # PID=`netstat -apn | grep $PORT | awk '{print $7}' | cut -d/ -f 1`
           PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
        done
        okCount=$(($okCount+1))
        echo "$MODULE 3---$SPOT :已经启动成功,PID= $PID"
      fi
    fi
  done
  if(($commandOk == 0));then
    echo "第二个参数请输入:a|b|c|d|e"
  else
    echo "............s3.sh 本次共启动: $okCount 个服务..........."
  fi
}

stop() {
  local MODULE=
  # local MODULE_NAME=
  local JAR_NAME=
  local command="$1"
  local commandOk=0
  local okCount=0
  for((i=0;i<${#MODULES[@]};i++))
  do
    MODULE=${MODULES[$i]}
    # MODULE_NAME=${MODULE_NAMES[$i]}
    JAR_NAME=${JARS[$i]}
    if [ "$command" = "all" ] || [ "$command" = "$MODULE" ];then
      commandOk=1
      PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
      if [ -n "$PID" ];then
        echo "$MODULE 3---$JAR_NAME:准备结束,PID=$PID"
        kill -9 $PID
        PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
        while [ -n "$PID" ]
        do
          sleep 3s
          PID=`ps -ef |grep $(echo $JAR_NAME | awk -F/ '{print $NF}') | grep -v grep | awk '{print $2}'`
        done
        echo "$MODULE 3--- :成功结束"
        okCount=$(($okCount+1))
      else
        echo "$MODULE 3--- :未运行"
      fi
    fi
  done
  if (($commandOk == 0));then
    echo "第二个参数请输入:a|b|c|d|e"
  else
    echo "............s3.sh 本次共停止: $okCount 个服务............"
  fi
}

case "$1" in
  start)
    start "$2" "$3" "$4"
  ;;
  stop)
    stop "$2"
  ;;
  restart)
    stop "$2"
    sleep 3s
    start "$2" "$3" "$4"
  ;;
  *)
    echo "第一个参数请输入:start|stop|restart"
    exit 1
  ;;
esac