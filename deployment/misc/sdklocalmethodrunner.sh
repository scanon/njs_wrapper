#!/usr/bin/env bash
if [ -e /etc/clustername ] ; then
    export PATH=$PATH:$(pwd)
    export USE_SHIFTER=1
    export KBASE_RUN_DIR=/global/cscratch1/sd/kbaserun
    export REFDATA_DIR=$KBASE_RUN_DIR/refdata/ci/refdata/
    export CALLBACK_INTERFACE=ib0
    export SCRATCH=/global/cscratch1/sd/kbaserun/
    mkdir -p $SCRATCH/writeable
    export HOME=$SCRATCH
    NJSW_JAR=`readlink -f NJSWrapper-all.jar`
    JOBID=$1
    KBASE_ENDPOINT=$2
    BASE_DIR=$KBASE_RUN_DIR/jobs/$JOBID
    mkdir -p $BASE_DIR && cd $BASE_DIR
    exec java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err
else
#     export MINI_KB=true
    NJSW_JAR=`readlink -f NJSWrapper-all.jar`
    JOBID=$1
    KBASE_ENDPOINT=$2
    BASE_DIR=$BASE_DIR/$JOBID
    mkdir -p $BASE_DIR && cd $BASE_DIR
    echo "Jar Location = $NJSW_JAR" > jar
    java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunner $JOBID $KBASE_ENDPOINT > sdk_lmr.out 2> sdk_lmr.err
    SDKLMR_EXITCODE=$?
    java -cp $NJSW_JAR us.kbase.narrativejobservice.sdkjobs.SDKLocalMethodRunnerCleanup $JOBID $KBASE_ENDPOINT > cleanup.out 2> cleanup.err
    exit $SDKLMR_EXITCODE
fi
