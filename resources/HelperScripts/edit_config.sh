#!/bin/bash

#"${MASTER%%.*}"
ARGS=("$@")
WORKFLOWIP="${ARGS[0]%%.*}"
MASTERIP="${ARGS[1]%%.*}"
echo $MASTERIP
AGENTIP="${ARGS[2]%%.*}"
#WORKERS=$4
SIMPLERUNDIRECTORY=/user/ajaysudh/cometWorkflow08052015/simple_run

WorkerLimit=""
Cost=""
Perf=""


total=${#ARGS[*]}
for (( i=3; i<=$(( $total -1 )); i++ ))
do
    WorkerLimit+="${ARGS[$i]%%.*}:1;"
    Cost+="${ARGS[$i]%%.*}:0;"
    Perf+="${ARGS[$i]%%.*}:1;"
done


sed -i 's/^[ \t]*publicIpManager[ \t]*=\([ \t]*.*\)$/publicIpManager='${WORKFLOWIP}'/g' $SIMPLERUNDIRECTORY/workflow/manager.properties

workflowMasterURI="${MASTERIP}:7777"
sed -i 's/^[ \t]*workflowmasterURI[ \t]*=\([ \t]*.*\)$/workflowmasterURI='${workflowMasterURI}'/g' $SIMPLERUNDIRECTORY/workflow/manager.properties
sed -i 's/^[ \t]*CentralManagerAddress[ \t]*=\([ \t]*.*\)$/CentralManagerAddress='${MASTERIP}'/g' $SIMPLERUNDIRECTORY/workflow/manager.properties
sed -i 's/^[ \t]*IsolatedProxy[ \t]*=\([ \t]*.*\)$/IsolatedProxy='${MASTERIP}'/g' $SIMPLERUNDIRECTORY/master/comet.properties
sed -i 's/^[ \t]*IsolatedProxy[ \t]*=\([ \t]*.*\)$/IsolatedProxy='${MASTERIP}'/g' $SIMPLERUNDIRECTORY/agent/comet.properties
echo "$MASTERIP:2" > $SIMPLERUNDIRECTORY/master/nodeFile
cat > $SIMPLERUNDIRECTORY/master/exceptionFile <<EOL
$MASTERIP:5555
comet.NodeType=MASTER
$MASTERIP:5556
comet.NodeType=REQUEST_HANDLER
EOL
echo "$MASTERIP" > $SIMPLERUNDIRECTORY/master/RequestHandlerList

sed -i 's/^[ \t]*publicIpAgent[ \t]*=\([ \t]*.*\)$/publicIpAgent='${AGENTIP}'/g' $SIMPLERUNDIRECTORY/agent/agent.properties
centralManagerServ="${WORKFLOWIP}:7778"
sed -i 's/^[ \t]*CentralManagerServer[ \t]*=\([ \t]*.*\)$/CentralManagerServer='${centralManagerServ}'/g' $SIMPLERUNDIRECTORY/agent/agent.properties
sed -i 's/^[ \t]*WorkerLimit[ \t]*=\([ \t]*.*\)$/WorkerLimit='${WorkerLimit}'/g' $SIMPLERUNDIRECTORY/agent/clusterSpring
sed -i 's/^[ \t]*Cost[ \t]*=\([ \t]*.*\)$/Cost='${Cost}'/g' $SIMPLERUNDIRECTORY/agent/clusterSpring
sed -i 's/^[ \t]*Perf[ \t]*=\([ \t]*.*\)$/Perf='${Perf}'/g' $SIMPLERUNDIRECTORY/agent/clusterSpring

sed -i 's/ajaysudh@.*:/ajaysudh@'${MASTERIP}':/g' $SIMPLERUNDIRECTORY/client/Workflow.xml
