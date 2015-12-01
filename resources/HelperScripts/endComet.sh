#!/bin/bash

for i in `seq 1 3`; do
 ssh dell0$i "killall -u jz362";
done
