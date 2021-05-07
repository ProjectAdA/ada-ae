#!/bin/bash

echo "Setup SPARQL_UPDATE_USER $SPARQL_UPDATE_USER"

echo "USER_CREATE('$SPARQL_UPDATE_USER', '$SPARQL_UPDATE_PASSWORD');" > autoexec.isql
echo "GRANT SPARQL_UPDATE to \"$SPARQL_UPDATE_USER\";" >> autoexec.isql
echo "GRANT SPARQL_SELECT to \"$SPARQL_UPDATE_USER\";" >> autoexec.isql

"$VIRTUOSO" -f +checkpoint-only
rm autoexec.isql

