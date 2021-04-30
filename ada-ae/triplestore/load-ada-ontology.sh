#!/bin/bash

echo "Downloading ontology $ONTOLOGY_URL ..."
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
rm -rf import
rm autoexec.isql

