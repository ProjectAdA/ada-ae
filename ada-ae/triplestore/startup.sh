#!/bin/bash

echo "AdA project triplestore startup..."

if [ -f /root/virtuoso.ini ]; then
  mv /root/virtuoso.ini .
fi

# TODO: Decide whether to ship ontology with triplestore or retrieve from final repository
if [ ! -f ".ontoset" ]; then
	echo "Downloading ontology..."
	mkdir import
	cd import
	wget -q --show-progress https://github.com/ProjectAdA/public/raw/master/ontology/ada_ontology.owl
	cd ..
	echo "ld_dir('import', '*', 'http://onto.ada.filmontology.org/');" >/tmp/import.sql
	echo "rdf_loader_run();"  >>/tmp/import.sql
	echo "exec('checkpoint');" >>/tmp/import.sql
	echo "WAIT_FOR_CHILDREN; " >>/tmp/import.sql
	echo "Importing ontology..."
	virtuoso-t +wait && isql-v -U dba -P dba </tmp/import.sql
	kill $(ps aux | grep '[v]irtuoso-t' | awk '{print $2}')
	rm /tmp/import.sql
	rm -rf import
	touch .ontoset
fi

if [ ! -f ".pwset" ]; then
	if [ ! "$VIRTUOSO_DBA_PASSWORD" ]; then
		echo "WARNING! Environment variable VIRTUOSO_DBA_PASSWORD is not set. Generating a dba password."
		VIRTUOSO_DBA_PASSWORD=`date | md5sum | head -c16`;
	fi
	echo "user_set_password('dba', '$VIRTUOSO_DBA_PASSWORD');" >/tmp/dbpw.sql
	echo "Setting dba password..."
	virtuoso-t +wait && isql-v -U dba -P dba </tmp/dbpw.sql
	kill $(ps aux | grep '[v]irtuoso-t' | awk '{print $2}')
	rm /tmp/dbpw.sql
	touch .pwset
fi

echo "Starting virtuoso..."
exec virtuoso-t +wait +foreground
