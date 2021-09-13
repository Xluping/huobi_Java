#! /bin/sh

rm -rf /root/jars/*

mkdir /root/jars/a
mkdir /root/jars/b
mkdir /root/jars/c
mkdir /root/jars/d
mkdir /root/jars/e
## m1
cp /root/spot1-jar-with-dependencies.jar /root/jars/a/a1-jar-with-dependencies.jar
cp /root/spot1-jar-with-dependencies.jar /root/jars/b/b1-jar-with-dependencies.jar
cp /root/spot1-jar-with-dependencies.jar /root/jars/c/c1-jar-with-dependencies.jar
cp /root/spot1-jar-with-dependencies.jar /root/jars/d/d1-jar-with-dependencies.jar
cp /root/spot1-jar-with-dependencies.jar /root/jars/e/e1-jar-with-dependencies.jar
## m2
cp /root/spot2-jar-with-dependencies.jar /root/jars/a/a2-jar-with-dependencies.jar
cp /root/spot2-jar-with-dependencies.jar /root/jars/b/b2-jar-with-dependencies.jar
cp /root/spot2-jar-with-dependencies.jar /root/jars/c/c2-jar-with-dependencies.jar
cp /root/spot2-jar-with-dependencies.jar /root/jars/d/d2-jar-with-dependencies.jar
cp /root/spot2-jar-with-dependencies.jar /root/jars/e/e2-jar-with-dependencies.jar
## m3
cp /root/spot3-jar-with-dependencies.jar /root/jars/a/a3-jar-with-dependencies.jar
cp /root/spot3-jar-with-dependencies.jar /root/jars/b/b3-jar-with-dependencies.jar
cp /root/spot3-jar-with-dependencies.jar /root/jars/c/c3-jar-with-dependencies.jar
cp /root/spot3-jar-with-dependencies.jar /root/jars/d/d3-jar-with-dependencies.jar
cp /root/spot3-jar-with-dependencies.jar /root/jars/e/e3-jar-with-dependencies.jar

rm -rf spot1-jar-with-dependencies.jar
rm -rf spot2-jar-with-dependencies.jar
rm -rf spot3-jar-with-dependencies.jar
ps -ef | grep java