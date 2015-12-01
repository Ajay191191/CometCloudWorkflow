#!/bin/sh

ssh dell01 "nohup ./workflow.sh &" &
wait 2
ssh dell02 "nohup ./master.sh &" &
wait 2
ssh dell03 "nohup ./agent.sh &" &
