#!/bin/bash

jar cf $1 -C /media/sf_Code/cometcloud_workflow/bin/ .
rsync -avz --progress $1 jz362@spring.rutgers.edu:/cac/u01/jz362/cometcloud/cometWorkflow08052015/lib/
rsync -avz --progress $1 jz362@spring.rutgers.edu:/cac/u01/jz362/cometcloud/Workflow/lib/

