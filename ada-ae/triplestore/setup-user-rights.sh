#!/bin/bash

#FIXME Setup User/Pass for SPARQL UPDATE rights
echo "Grant update rights to SPARQL user..."

echo "GRANT SPARQL_UPDATE to \"SPARQL\";" > autoexec.isql
"$VIRTUOSO" -f +checkpoint-only
rm autoexec.isql

