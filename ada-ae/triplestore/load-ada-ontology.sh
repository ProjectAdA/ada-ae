#!/bin/bash

ONTOLOGY_URL=https://github.com/ProjectAdA/public/raw/master/ontology/ada_ontology.owl

echo "Downloading ontology..."
mkdir import
cd import
wget -q --show-progress $ONTOLOGY_URL
cd ..
echo "ld_dir('import', '*', 'http://onto.ada.filmontology.org/');" > autoexec.isql
echo "rdf_loader_run();"  >> autoexec.isql
echo "exec('checkpoint');" >> autoexec.isql
echo "WAIT_FOR_CHILDREN; " >> autoexec.isql
echo ""
echo "Importing ontology..."
"$VIRTUOSO" -f +checkpoint-only
#virtuoso-t +wait && isql-v -U dba -P dba </tmp/import.sql
#kill $(ps aux | grep '[v]irtuoso-t' | awk '{print $2}')
#rm /tmp/import.sql
rm -rf import
rm autoexec.isql

