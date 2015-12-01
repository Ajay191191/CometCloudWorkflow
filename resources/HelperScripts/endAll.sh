#!/bin/bash

for i in `seq 4 9`; do
 ssh dell0$i "killall -u jz362";
done


for i in `seq 10 20`; do
 ssh dell$i "killall -u jz362";
done
