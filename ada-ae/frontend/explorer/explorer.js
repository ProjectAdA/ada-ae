var movies = [];
var annotationlevels = [];
var type_list = [];
var request_results = [];
var autocomplete_values = {};
var tree_timeout_id;
var trees_deselected_nodes = [];

const zeroPad = (num, places) => String(num).padStart(places, '0')

function lock_interface() {
	$('#overlay').fadeIn();
}

function unlock_interface() {
	$('#overlay').fadeOut();
}

function request_status() {
	console.log("request_status");
	return $.ajax({
		url: apiUrl + "/status",
		timeout: 15000,
		error: function(request, status, error) {
			console.log("Loading of API status failed. \n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
		}
	});
}

function request_ontology() {
	console.log("request_ontology");
	return $.ajax({
		url: apiUrl + "/getOntology",
		timeout: 50000,
		error: function(request, status, error) {
			alert("Loading ontology failed. API could not be reached. Please reload the page.\n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
			unlock_interface();
		}
	});
}

function request_annotations(url) {
	console.log("request_annotations");
	return $.ajax({
		url: url,
		timeout: 180000,
		error: function(request, status, error) {
			alert("Loading annotations failed. API could not be reached. Please submit the request again.\n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
			unlock_interface();
		}
	});	
}

function request_movie_metadata() {
	console.log("request_movie_metadata");
	return $.ajax({
		url: apiUrl + "/getMovieMetadata",
		timeout: 120000,
		error: function(request, status, error) {
			alert("Loading movie metadata failed. API could not be reached. Please reload the page.\n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
			unlock_interface();
		}
	});
}

function request_image_search(file, numresults) {
	console.log("request_image_search");

	var data = new FormData();
	data.append('upload_file', file);
	data.append('numresults', numresults);
	return $.ajax({
		url: apiUrl + "/imageSearch",
		data: data,
		type: 'POST',
		processData: false,
		contentType: false,
		timeout: 50000,
		error: function(request, status, error) {
			alert("Image search request failed. API could not be reached. Please submit the request again. \n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
			unlock_interface();
		}
	});
}


function truncate_name(name, length) {
	if (name.length > length) {
		return name.substring(0,length -3)+"...";
	} else {
		return name;
	}
}

function split( val ) {
    return val.split( /,\s*/ );
}

function extractLast( term ) {
    return split( term ).pop();
}

function addEntryToField(field, id, label) {
	var entries = field.data("entries");
	if (entries === undefined) {
		entries = [];
	}
	if (entries.filter(e => e.id == id).length == 0) {
		entries.push({id: id, label: label});
	}
	field.data("entries", entries);
}

function getEntriesFromField(field) {
	var entries = field.data("entries");
	if (entries === undefined) {
		entries = [];
	}
	return entries;
}

function setEntriesToField(field, entries) {
	field.data("entries", entries);
}

function replaceInput(value, input) {
	var terms = split( value );
	terms.pop();
	terms.push( input );
	terms.push( "" );
	return terms.join( ", " );
}

function add_autocomplete(fieldname, autocomplete_valuename, focusFunction) {
	$( fieldname ).on( "keydown", function( event ) {
            if ( event.keyCode === $.ui.keyCode.TAB && $( this ).autocomplete( "instance" ).menu.active ) {
                event.preventDefault();
            }
        }).autocomplete({
            minLength: 0,
            source: function( request, response ) {
						var input_terms = split(request.term).filter(e => e != "");
						var filtered_values = autocomplete_values[autocomplete_valuename].filter(e => !input_terms.includes(e.label));
                        response( $.ui.autocomplete.filter( filtered_values, extractLast( request.term ) ) );
                    },
            focus: function() {
                        return false;
                    },
            select: function( event, ui ) {
				this.value = replaceInput(this.value, ui.item.label);
				addEntryToField($( this ), ui.item.value, ui.item.label);
				document.getElementById(event.target.id).style.borderColor = "";
				document.getElementById(event.target.id).style.color = "";
				document.getElementById(event.target.id).scrollLeft = document.getElementById(event.target.id).scrollWidth;
				return false;
			},
			change: function( event, ui ) {
				var dataEntries = getEntriesFromField($(this));
				var inputLabels = split( this.value );
				if (inputLabels[inputLabels.length-1] == "") {inputLabels.pop();}
				var removedEntries = dataEntries.filter(e => !inputLabels.includes(e.label.trim()));
				var keepedEntries = dataEntries.filter(e => inputLabels.includes(e.label.trim()));
				setEntriesToField($(this),keepedEntries);
				
				var dataLabels = keepedEntries.map(e => e.label);
				
				document.getElementById(event.target.id).scrollLeft = document.getElementById(event.target.id).scrollWidth;

				if (inputLabels.filter(x => !dataLabels.includes(x)).length > 0) {
					document.getElementById(event.target.id).style.borderColor = "red";
					document.getElementById(event.target.id).style.color = "red";
				} else {
					document.getElementById(event.target.id).style.borderColor = "";
					document.getElementById(event.target.id).style.color = "";
				}
			}
        }).focus(focusFunction);
}

function retrieve_trees_selection_state() {
	console.log("retrieve_trees_selection_state");
	var annoTree = $.ui.fancytree.getTree("#annotation_tree");
	var movieTree = $.ui.fancytree.getTree("#movie_tree");
	var resanno = annoTree.getSelectedNodes().map(n => n.key);
	var resmovie = movieTree.getSelectedNodes().map(n => n.key);
	return resanno.concat(resmovie);
}

function get_deselected_nodes() {
	var annoTree = $.ui.fancytree.getTree("#annotation_tree");
	var movieTree = $.ui.fancytree.getTree("#movie_tree");
	var result = [];
	annoTree.visit(function(node){
		if (!node.unselectable && node.getChildren() == null) {
			if (!node.isSelected() && !node.isPartsel()) {
				result.push(node.key);
			}
		}
	});
	movieTree.visit(function(node){
		if (!node.unselectable && node.getChildren() == null) {
			if (!node.isSelected() && !node.isPartsel()) {
				result.push(node.key);
			}
		}
	});
	return result;
}

function fix_deselected_nodes() {
	if (trees_deselected_nodes.length == 0) {
		return;
	}
	var annoTree = $.ui.fancytree.getTree("#annotation_tree");
	var movieTree = $.ui.fancytree.getTree("#movie_tree");
	annoTree.visit(function(node){
		if (trees_deselected_nodes.includes(node.key)) {
			node.setSelected(false);
		}
	});
	movieTree.visit(function(node){
		if (trees_deselected_nodes.includes(node.key)) {
			node.setSelected(false);
		}
	});
	
}

function setTimeouts() {
	if (typeof tree_timeout_id !== 'undefined') {
		window.clearTimeout(tree_timeout_id);
	}
	
	trees_deselected_nodes = get_deselected_nodes();
	
	tree_timeout_id = window.setTimeout(reload_frametrail, tree_selection_timeout);
}

function tree_select_eventhandler(event, data) {
	var node = data.node;
	if (typeof data.originalEvent !== 'undefined') {
		setTimeouts();
	}
}

function init_ontology_tree() {
	console.log("init_ontology_tree");
	
	var tree_data = [];

	annotationlevels.sort((a,b) => a.sequentialNumber - b.sequentialNumber);
	
	annotationlevels.forEach(function(level){
		var level_data = {title: truncate_name(level.elementName, 27), original_title: level.elementName, key: level.id, tooltip: level.elementDescription, count: 0, uri: level.elementUri, expanded: false, unselectable: true, children: null};
		tree_data.push(level_data);
		if (level.subElements !== null) {
			level.subElements.sort((a,b) => a.sequentialNumber - b.sequentialNumber);
			var lchildren = [];
			level.subElements.forEach(function(type) {
				var type_data = {title: truncate_name(type.elementName, 27), original_title: type.elementName, key: type.id, tooltip: type.elementDescription, count: 0, uri: type.elementUri, expanded: false, unselectable: true, children: null};
				lchildren.push(type_data);
				if (type.subElements !== null) {
					type.subElements.sort((a,b) => a.sequentialNumber - b.sequentialNumber);
					var tchildren = [];
					type.subElements.forEach(function(value) {
						tchildren.push({title: truncate_name(value.elementName, 27), original_title: value.elementName, key: value.id, tooltip: value.elementDescription, count: 0, uri: value.elementUri, expanded: false, unselectable: true});
					});
				}
				type_data.children = tchildren;
			});
			level_data.children = lchildren;
		}
	});
	
	
	$("#annotation_tree").fancytree({
		checkbox: true,
		selectMode: 3,
		icon: false,
		source: tree_data,
		activate: function(event, data) {
			var node = data.node;
			if( node.data.uri !== null ){
				window.open(node.data.uri, "_blank");
			}
		},
		select: tree_select_eventhandler,
		cookieId: "fancytree-annotations",
		idPrefix: "fancytree-annotations-"
	});
	
	$("#btnSelectAll_annotation").click(function(){
		$.ui.fancytree.getTree("#annotation_tree").visit(function(node){
			if (!node.unselectable) {
				node.setSelected(true);
			}
		});
		setTimeouts();
	});
	$("#btnDeselectAll_annotation").click(function(){
		$.ui.fancytree.getTree("#annotation_tree").visit(function(node){
			node.setSelected(false);
		});
		setTimeouts();
	});
	$("#btnExpand_level").click(function(){
		$.ui.fancytree.getTree("#annotation_tree").visit(function(node){
			if (node.isTopLevel()) {
				node.setExpanded(true);
			} else {
				node.setExpanded(false);
			}
		});
	});
	$("#btnExpandAll_annotations").click(function(){
		$.ui.fancytree.getTree("#annotation_tree").expandAll(true);
	});
	$("#btnCollapseAll_annotations").click(function(){
		$.ui.fancytree.getTree("#annotation_tree").expandAll(false);
	});

}

function post_process_movie_metadata() {
	// Add scene short ids
	movies.forEach(function(movie){
		if (movie.scenes !== null && movie.scenes.length > 0) {
			movie.scenes.sort((a,b) => a.startTime - b.startTime);
			var i = 1;
			movie.scenes.forEach(function(scene) {
				scene["shortId"] = i;
				i = i + 1;
			});
		}
	});
}

function post_process_ontology_metadata() {
	// Replace null with empty subelement arrays
	annotationlevels.forEach(function(level) {
		if (level.subElements !== null) {
			level.subElements.forEach(function(type) {
				if (type.subElements == null) {
					type.subElements = [];
				}
			});
		} else {
			level.subElements = [];
		}
	});
}

function init_movie_tree() {
	console.log("init_movie_tree");
	
	var tree_data = [];
	
	var cats = [...new Set(movies.map(m => m.category))].sort();
	cats.forEach(c => tree_data.push({title: c, original_title: c, key: c, tooltip: c, count: 0, uri: null, expanded: false, unselectable: true, children: []}));
	
	movies.forEach(function(movie){
		var tooltip = movie.title;
		if (movie.abstract !== null) {
			tooltip += " // " + movie.abstract;
		}
		var movie_data = {title: truncate_name(movie.title, 27), original_title:movie.title, key: movie.id, tooltip: tooltip, count: 0, uri: movie.mediauri, expanded: false, unselectable: true, children: []};
		if (movie.scenes !== null && movie.scenes.length > 0) {
			movie.scenes.forEach(function(scene) {
				movie_data.children.push({title: zeroPad(scene.shortId,2)+": "+truncate_name(scene.name, 23), original_title: zeroPad(scene.shortId,2)+": "+scene.name, tooltip: scene.name, key: scene.id, uri: null, expanded: false, unselectable: true });
			});
		}
		
		var cat = tree_data.find(x => x.key === movie.category);
		cat.children.push(movie_data);
		cat.children.sort((a,b) => ('' + a.original_title).localeCompare(b.original_title));
	});
	
	$("#movie_tree").fancytree({
		checkbox: true,
		selectMode: 3,
		icon: false,
		source: tree_data,
		activate: function(event, data) {
			var node = data.node;
			if( node.data.uri !== null ){
				window.open(node.data.uri, "_blank");
			}
		},
		select: tree_select_eventhandler,
		cookieId: "fancytree-movies",
		idPrefix: "fancytree-movies-"
	});
	
	$("#btnSelectAll_movies").click(function(){
		$.ui.fancytree.getTree("#movie_tree").visit(function(node){
			if (!node.unselectable) {
				node.setSelected(true);
			}
		});
		setTimeouts();
	});
	$("#btnDeselectAll_movies").click(function(){
		$.ui.fancytree.getTree("#movie_tree").visit(function(node){
			node.setSelected(false);
		});
		setTimeouts();
	});
	$("#btnExpand_categories").click(function(){
		$.ui.fancytree.getTree("#movie_tree").visit(function(node){
			if (node.isTopLevel()) {
				node.setExpanded(true);
			} else {
				node.setExpanded(false);
			}
		});
	});
	$("#btnExpandAll_movies").click(function(){
		$.ui.fancytree.getTree("#movie_tree").expandAll(true);
	});
	$("#btnShrinkAll_movies").click(function(){
		$.ui.fancytree.getTree("#movie_tree").expandAll(false);
	});
}

function generate_movie_autocomplete_list(allmovies) {
	var result = [];
	console.log("generate_movie_autocomplete_list");
	movies.forEach(function(movie){
		var cat = "";
		if (movie.category !== null) {
			switch(movie.category) {
				case "Documentary":
					cat = "Doc";
					break;
				case "Feature Film":
					cat = "Feat";
					break;
				case "TV News":
					cat = "News";
					break;
				default:
					cat = movie.category.substring(0,3);
			}
		}
		result.push({label: cat+" | "+truncate_name(movie.title.replace(/,/g, ' -'), 45)+" ("+movie.annotationsTotal+")", value: movie.id});
	});
	return result.sort((a,b) => ('' + a.label).localeCompare(b.label));
}

function generate_scene_autocomplete_list(movieid) {
	var result = [];
	
	var movie = movies.find(x => x.id === movieid);
	if (movie.scenes !== null && movie.scenes.length > 0) {
		result.push({label: truncate_name(movie.title.replace(/,/g, ' -'), 20)+" | -- All Scenes --", value: movie.id+"_all"});

		movie.scenes.forEach(function(scene) {
			result.push({label: truncate_name(movie.title.replace(/,/g, ' -'), 20)+" | "+zeroPad(scene.shortId,2)+": "+truncate_name(scene.name.replace(/,/g, ' -'), 23), value: movie.id+"_"+scene.id});
		});
	}
	return result;
}

function generate_type_autocomplete_list(ontodata, movieids) {
	var result = [];
	console.log("generate_type_autocomplete_list");
	
	if (movieids !== null) {
		result.push({label: "-- Corpus Analysis Types --", value: CorpusTypes});
		result.push({label: "-- Auto Types --", value: AutoTypes});
	}
	
	var resultTypeCounts = {};
	
	// Sum up type counts of each movie
	if (movieids !== null) {
		var filtered_movies = movies.filter(e => movieids.includes(e.id));
		filtered_movies.forEach(function(movie){
			if (movie.typeCounts !== null) {
				for (const [key, value] of Object.entries(movie.typeCounts)) {
					var accValue = resultTypeCounts[key];
					if (accValue === undefined) {
						accValue = value;
					} else {
						accValue += value;
					}
					resultTypeCounts[key] = accValue;
				}
			}
		});
	}
	
	annotationlevels.forEach(function(level) {
		if (level.subElements !== null) {
			level.subElements.forEach(function(type) {
				var short_typeid = type.id.substring(type.id.lastIndexOf("/")+1);
				var count = 0;
				if (Object.keys(resultTypeCounts).includes(short_typeid)) {
					count = resultTypeCounts[short_typeid];
				}
				if (movieids !== null) {
					result.push({label: type.elementFullName.replace(/,/g, ' -')+" ("+ count + ")", value: type.id});
				} else {
					result.push({label: type.elementFullName.replace(/,/g, ' -'), value: type.id});
				}
			});
		}
	});
	return result.sort((a,b) => ('' + a.label).localeCompare(b.label));
}

function generate_value_autocomplete_list(ontodata) {
	var result = [];
	console.log("generate_value_autocomplete_list");
	
	annotationlevels.forEach(function(level) {
		if (level.subElements !== null) {
			level.subElements.forEach(function(type) {
				if (type.subElements !== null) {
					type.subElements.forEach(function(value) {
						result.push({label: type.elementName.replace(/,/g, ' -') + " - " + value.elementName.replace(/,/g, ' -'), value: value.id});
					});
				}
			});
		}
	});

	return result.sort((a,b) => ('' + a.label).localeCompare(b.label));
}

function add_valuesearch_field() {
    console.log("add_valuesearchfield");
    var container = document.getElementById("valuesearchcontainer");
	
	var i = 1;
	while(i <= max_value_fields) {
		if( document.getElementById("field_container_" + i) ) {
			i++;
			if (i > max_value_fields) {
				alert("You have reached the limit of additional value fields.");
				return;
			}
		} else {
			break;
		}
	}

    var filterlabel = document.getElementById("valuesearchfilterlabel");
	
	var valuediv = document.createElement("div");
	valuediv.id = "field_container_" + i;
	valuediv.innerHTML = '<span class="searchFieldLabel">&nbsp;&nbsp;&nbsp;AND:</span>\n<span class="ui-widget">\n<input id="value_search_field_'+i+'" name="value_search_field_'+i+'" class="moviesearchfield" autocomplete="off"><button class="icon icon-minus" onclick="this.parentElement.parentElement.remove()"></button>\n</span>'
	container.insertBefore(valuediv, filterlabel);
	
	autocomplete_values['values'] = generate_value_autocomplete_list(annotationlevels);
	add_autocomplete("#value_search_field_"+i, "values", function() {
			$(this).autocomplete("search", $(this).val());
	});
}


function init_search_fields() {
	console.log("init_search_fields");

	// Movie Search - Movies
	autocomplete_values['movies'] = generate_movie_autocomplete_list(movies);
	add_autocomplete("#MovieSearch_MovieField", "movies", function() {
		$(this).autocomplete("search", $(this).val());
	});

	// Movie Search - Scenes
	autocomplete_values['scenes'] = [];
	add_autocomplete("#MovieSearch_SceneField", "scenes", function() {
		var movies = getEntriesFromField($( "#MovieSearch_MovieField" ));
		autocomplete_values['scenes'] = [];
		movies.forEach(function(movie){
			var scenes = generate_scene_autocomplete_list(movie.id);
			autocomplete_values['scenes'] = autocomplete_values['scenes'].concat(scenes);
		});
		
		autocomplete_values['scenes'] = autocomplete_values['scenes'].sort((a,b) => ('' + a.label).localeCompare(b.label));
		
		$(this).autocomplete("search", $(this).val());
	});
	
	// Movie Search - Types
	autocomplete_values['types'] = generate_type_autocomplete_list(annotationlevels, null);
	add_autocomplete("#MovieSearch_TypeField", "types", function() {
		var movieids = getEntriesFromField($( "#MovieSearch_MovieField" )).map(m => m.id);
		autocomplete_values['types'] = generate_type_autocomplete_list(annotationlevels, movieids);
		$(this).autocomplete("search", $(this).val());
	});

	// Value Search - 1st value field
	autocomplete_values['values'] = generate_value_autocomplete_list(annotationlevels);
	add_autocomplete("#value_search_field_1", "values", function() {
		$(this).autocomplete("search", $(this).val());
	});
	
	// Value Search - Movies
	add_autocomplete("#ValueSearchFieldMovies", "movies", function() {
		$(this).autocomplete("search", $(this).val());
	});
	
	// Text Search - Movie Filter
	add_autocomplete("#TextSearchFieldMovies", "movies", function() {
		$(this).autocomplete("search", $(this).val());
	});
	
	// Text Search - Type Filter
	autocomplete_values['typestext'] = generate_type_autocomplete_list(annotationlevels, null);
	add_autocomplete("#TextSearchFieldTypes", "typestext", function() {
		$(this).autocomplete("search", $(this).val());
	});

}

function generateUUID() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
		return v.toString(16);
	});
}

function getMovieLabelById(id) {
	var result = "";
	var movie = movies.find(m => m.id == id);
	if (typeof movie !== 'undefined') {
		result = movie.title;
	}
	return result;
}

function getMovieShortId(movieid) {
	var result = "";
	var movie = movies.find(m => m.id == movieid);
	if (typeof movie !== 'undefined') {
		result = zeroPad(movie.shortId,2);
	}
	return result;
}

function getMovieLongId(shortId) {
	var result = "";
	var movie = movies.find(m => m.shortId == shortId);
	if (typeof movie !== 'undefined') {
		result = movie.id;
	}
	return result;
}

function getSceneLongId(movieid, shortId) {
	var result = "";
	
	var movie = movies.find(m => m.id === movieid);
	if (typeof movie !== 'undefined') {
		if (movie.scenes !== null && movie.scenes.length > 0) {
			var scene = movie.scenes.find(m => m.shortId === Number(shortId));
			if (typeof scene !== 'undefined') {
				result = scene.id;
			}
		}
	}
	
	return result;
}

function getSceneShortId(movieid, sceneid) {
	var result = "";
	
	var movie = movies.find(m => m.id === movieid);
	if (typeof movie !== 'undefined') {
		if (movie.scenes !== null && movie.scenes.length > 0) {
			var scene = movie.scenes.find(m => m.id === sceneid);
			if (typeof scene !== 'undefined') {
				result = zeroPad(scene.shortId,2);
			}
		}
	}
	
	return result;
}

function getTypeShortId(typeid) {
	var result = "";
	
	if (typeid == CorpusTypes) {
		result = "c"
	} else 
	if (typeid == AutoTypes) {
		result = "a"
	} else {
		annotationlevels.forEach(function(level) {
			if (level.subElements !== null) {
				var type = level.subElements.find(t => t.id == typeid);
				if (typeof type !== 'undefined') {
					result = type.sequentialNumber;
				}
			}
		});
	}
	return result;
}

function getValueShortId(id) {
	var result = "";

	annotationlevels.forEach(function(level) {
		if (level.subElements !== null) {
			level.subElements.forEach(function(type){
				if (type.subElements !== null) {
					var value = type.subElements.find(v => v.id.split("/")[1] == id);
					if (value) {
						result = value.sequentialNumber;
					}
				}
			});
		}
	});
	return result;
}


function getValueLongId(shortId) {
	var result = "";
	annotationlevels.forEach(function(level) {
		if (level.subElements !== null) {
			level.subElements.forEach(function(type){
				if (type.subElements !== null) {
					var value = type.subElements.find(v => v.sequentialNumber == Number(shortId));
					if (value) {
						result = value.id.split("/")[1];
					}
				}
			});
		}
	});
	return result;
}


function getTypeLongId(shortId) {
	var result = "";
	
	if (shortId == "c") {
		result = CorpusTypes
	} else 
	if (shortId == "a") {
		result = AutoTypes;
	} else {
		annotationlevels.forEach(function(level) {
			if (level.subElements !== null) {
				var type = level.subElements.find(t => t.sequentialNumber == Number(shortId));
				if (typeof type !== 'undefined') {
					result = type.id;
				}
			}
		});
	}
	return result;
}

function getValueLabelById(id) {
	var res = null;
	var restype = null;
	annotationlevels.forEach(function(level){
		if (level.subElements !== null) {
			level.subElements.forEach(function(type){
				if (type.subElements !== null) {
					var value = type.subElements.find(v => v.id.split("/")[1] == id);
					if (value) {
						res = value;
						restype = type;
					}
				}
			});
		}
	});
	
	if (res) {
		return restype.elementName + " - " + res.elementName;
	} else {
		return null;
	}
}


function getTypeLabelById(id) {
	var result = "";
	if (id == CorpusTypes) {
		result = "Corpus Types"
	} else 
	if (id == AutoTypes) {
		result = "Auto Types"
	} else {
		annotationlevels.forEach(function(level) {
			if (level.subElements !== null) {
				var type = level.subElements.find(t => t.id == id);
				if (typeof type !== 'undefined') {
					result = type.elementName;
				}
			}
		});
	}
	return result;
}


function generate_request_objects(searchtype, movieids, sceneids, typeids, searchterm, whole, value_ids) {
	console.log("generate_request_objects", searchtype);
	
	var url = requestUrls[searchtype];
	var movie_filter = movieids.length > 0 ? movieids.join(",") : "";
	var movie_filter_label = movieids.length > 0 ? movieids.map(m => getMovieLabelById(m)).join(",") : "";
	var type_filter = typeids.length > 0 ? "AnnotationType/" + typeids.join(",").replaceAll("AnnotationType/","") : "";
	var type_filter_label = typeids.length > 0 ? typeids.map(t => getTypeLabelById(t)) : "";
	
	var grouped_sceneids = {};
	sceneids.forEach(function(sid){
		var splits = sid.split(/_/);
		if (Object.keys(grouped_sceneids).includes(splits[0])) {
			var group = grouped_sceneids[splits[0]];
			group += "_"+splits[1];
			grouped_sceneids[splits[0]] = group;
		} else {
			grouped_sceneids[splits[0]] = splits[1];
		}
	});
	
	Object.keys(grouped_sceneids).forEach(function(key){
		if (grouped_sceneids[key].includes("all")) {
			delete grouped_sceneids[key];
			sceneids = sceneids.filter(s => !s.includes(key));
		}
	});
	
	var request_objects = [];
	var id = generateUUID();
	
	if (searchtype == "moviesearch") {
		movieids.forEach(function(mid){
			var url = requestUrls[searchtype];
			if (Object.keys(grouped_sceneids).includes(mid)) {
				url = url + mid + "_" + grouped_sceneids[mid];
			} else {
				url = url + mid;
			}
			if (type_filter !== "") {
				url = url + "/" + type_filter;
			}
			var label = "<span class=\"icon-hypervideo\"></span> "+getMovieLabelById(mid)+" (";
			if (Object.keys(grouped_sceneids).includes(mid)) {
				var sids = grouped_sceneids[mid].split("_");
				var sceneShortIds = sids.map(s => getSceneShortId(mid,s));
				label = label + sceneShortIds.join(",")+")";
			} else {
				label = label + "all)";
			}
			
			if (type_filter.length > 0) {
				label = label + " - "+type_filter_label;
			} else {
				label = label + " - All Types";
			}
			
			label = label + "<button onclick=\"remove_request('"+id+"')\"><span class=\"icon-cancel-circled\"></span></button>";
			
			request_objects.push({id: id, searchtype: searchtype, movieids: [mid], sceneids: sceneids, typeids: typeids, url: url, label: label, searchterm: searchterm, whole: whole, value_ids: value_ids });
		});
	}

	if (searchtype == "textsearch") {
		var url = requestUrls[searchtype]+searchterm;
		var label = "<span class=\"icon-search\"></span> \""+searchterm+"\" ";
		if (whole) {
			url = url + "/wholeword";
		} else {
			url = url + "/substring";
		}
		
		if (movie_filter.length > 0) {
			url = url + "/" + movie_filter;
			label = label + "(" + movie_filter_label + ") ";
		}
		
		if (type_filter.length > 0) {
			if (movie_filter.length == 0) {
				url = url + "/all";
			}
			url = url + "/" + type_filter;
			label = label + "[" + type_filter_label + "] ";
		}
		
		label = label + "<button onclick=\"remove_request('"+id+"')\"><span class=\"icon-cancel-circled\"></span></button>";
		
		request_objects.push({id: id, searchtype: searchtype, movieids: movieids, sceneids: sceneids, typeids: typeids, url: url, label: label, searchterm: searchterm, whole: whole, value_ids: value_ids });
	}
	
	if (searchtype == "valuesearch") {
		var url = requestUrls[searchtype];
		var label = "<span class=\"icon-tag\"></span> ";
		var movie_label_string = "";
		var movielabels = movieids.map(id => getMovieLabelById(id));
		movie_label_string = movielabels.join(", ");
		if (movie_label_string == "") {
			movie_label_string = "All Movies";
		}
		
		var value_label_string = "";
		value_ids.forEach(function(values){
			var labels = values.map(v => getValueLabelById(v));
			value_label_string = value_label_string + "(" + labels.join(" <i>OR</i> ") + ") <i>AND</i> ";
		});
		value_label_string = value_label_string.slice(0, -12);
		label = label + movie_label_string+": "+value_label_string;
		
		var value_request_string = "";
		value_ids.forEach(function(vids){
			value_request_string = value_request_string + vids.join() + ";";
		});
		value_request_string = value_request_string.slice(0, -1);
		var movie_request_string = "";
		if (movieids.length > 0) {
			movie_request_string = "/" + movieids.join();
		}
		url = url + value_request_string + movie_request_string;
		
		label = label + "<button onclick=\"remove_request('"+id+"')\"><span class=\"icon-cancel-circled\"></span></button>";
		
		request_objects.push({id: id, searchtype: searchtype, movieids: movieids, sceneids: sceneids, typeids: typeids, url: url, label: label, searchterm: searchterm, whole: whole, value_ids: value_ids });
	}

	return request_objects;
	
}

function remove_request(id) {
	console.log("remove_request");
	
	lock_interface();
	request_results = request_results.filter(ro => ro.id !== id);
	document.getElementById(id).remove();
	updateURL();
	initFrameTrail();
	// initFrameTrail(false, function() {
		// console.log("READY CALLBACK");
	// });
	unlock_interface();	
}


function reload_frametrail() {
	lock_interface();
	initFrameTrail();
	// initFrameTrail(false, function() {
		// console.log("READY CALLBACK");
	// });
	unlock_interface();
}

function clear_workspace() {
	console.log("clear_workspace");
	
	lock_interface();
	var ids = request_results.map(ro => ro.id);
	ids.forEach(id => document.getElementById(id).remove());
	request_results = [];
	$('body').removeClass('compareMode');
	updateURL();
	initFrameTrail();
	unlock_interface();
}

function execute_requests(request_objects) {
	console.log("execute_requests");
	
	lock_interface();
	Promise.all(request_objects.map(ro => request_annotations(ro.url))).then(function(results){
		var i = 0;
		var requests_with_results = [];
		request_objects.forEach(function(ro){
			var result = results[i];
			if (Object.keys(result).includes("@graph")) {
				ro['annotations'] = results[i]["@graph"];
				requests_with_results.push(ro);
				
				var container = document.getElementById("activeSearchFacets");
				var input = document.createElement("div");
				input.id = ro.id;
				input.classList.add("query_label");
				input.innerHTML = ro.label;
				container.appendChild(input);
			}
			i = i +1;
		});
		request_results = request_results.concat(requests_with_results);
		updateURL();
		initFrameTrail();
		// initFrameTrail(false, function() {
			// console.log("READY CALLBACK");
		// });
		unlock_interface();
		if (request_results.length == 0) {
			$( "div.zerowarning" ).fadeIn( 300 ).delay( 3500 ).fadeOut( 400 );
		}
	});
}

function submit_movie_search() {
	console.log("submit_movie_search");
	
	var movs = getEntriesFromField($( "#MovieSearch_MovieField" )).map(e => e.id);
	var scenes = getEntriesFromField($( "#MovieSearch_SceneField" )).map(e => e.id);
	var types = getEntriesFromField($( "#MovieSearch_TypeField" )).map(e => e.id);
	
	if (movs.length == 0) {
		return;
	}
	
	var request_objects = generate_request_objects("moviesearch", movs, scenes, types, "", false, []);
	execute_requests(request_objects);
}

function submit_text_search() {
	console.log("submit_text_search");
	
	var searchterm = document.getElementById("TextSearchFieldInput").value;
	var whole = document.getElementById("WholeWordsCheckbox").checked;
	
	var movs = getEntriesFromField($( "#TextSearchFieldMovies" )).map(e => e.id);
	var types = getEntriesFromField($( "#TextSearchFieldTypes" )).map(e => e.id);

	if (typeof searchterm === 'undefined' || searchterm.length == 0) {
		return;
	}
	
	var request_objects = generate_request_objects("textsearch", movs, [], types, searchterm, whole, []);
	execute_requests(request_objects);
}

function submit_value_search() {
	console.log("submit_value_search");
	
	var user_values = [];
	var invalidfield = false;
	
	var i = 1;
    while( i <= max_value_fields ) {
		var values = getEntriesFromField($("#value_search_field_"+i));
		if (values.length > 0) {
			user_values.push(values);
			if (document.getElementById("value_search_field_"+i).style.borderColor == "red") {
				invalidfield = true;
			}
		}
		i++;
	}
	
	if (document.getElementById("ValueSearchFieldMovies").style.borderColor == "red") {
		invalidfield = true;
	}
	
	if (invalidfield || user_values.length == 0) {
		return;
	}
	
	var movs = getEntriesFromField($("#ValueSearchFieldMovies")).map(e => e.id);
	
	var value_ids = [];
	user_values.forEach(function(values){
		var ids = values.map(v => v.id.split("/")[1]);
		value_ids.push(ids);
	});
	
	var request_objects = generate_request_objects("valuesearch", movs, [], [], "", false, value_ids);
	execute_requests(request_objects);
	
}

function msToTime(s) {
	var pad = (n, z = 2) => ('00' + n).slice(-z);
	return pad(s/3.6e6|0) + ':' + pad((s%3.6e6)/6e4 | 0) + ':' + pad((s%6e4)/1000|0) + '.' + pad(s%1000, 3);
}

function msToTimeShort(s) {
	var pad = (n, z = 2) => ('00' + n).slice(-z);
	return pad(s/3.6e6|0) + ':' + pad((s%3.6e6)/6e4 | 0) + ':' + pad((s%6e4)/1000|0);
}

function remove_image_search_result() {
	console.log("remove_image_search_result");
	
	var imagelistdiv = document.getElementById('imagelist');
	imagelistdiv.innerHTML = '';

	$('body').toggleClass('imagesearch', false); 
}

function download_image_search_result() {
	var filename = 'AdA-ImageSearch-'+ getUUID() +'.html';
	
	fileHTML =  '<!DOCTYPE html>\n'
			+   '<html lang="en">\n'
			+   '  <head>\n'
			+   '    <title>Image Search Result of the Semantic Video Annotation Explorer - AdA Project</title>\n'
			+   '    <meta charset="UTF-8">\n'
			+   '    <style type="text/css">\n'
			+   '    table, th, td {\n'
			+   '       border: 1px solid black;\n'
			+   '       border-collapse: collapse;\n'
			+   '    }\n'
			+   '    th, td {\n'
			+   '       padding: 8px;\n'
			+   '    }\n'
			+   '    </style>\n'
			+   '  </head>\n'
			+   '  <body>\n'
			+   '  <h3>Query Image</h3>\n'
			+   '  <img src="'+imagepreview.src+'" width="300">\n'
			+   '  <h3>Image Search Result</h3>\n'
			+   '  <table style="padding: 10px; border: 1px solid #bbb;">\n'
			+   '  <tr><th>#</th><th>Frame</th><th>Movie</th><th>Timestamp</th><th>Distance</th></tr>\n'
	
	var i = 1;
	imageSearchNeighbors.forEach(function(nres){
		var nid = nres[0];
		var nmilli = nres[1];
		var ndist = nres[2];
		var nimg = nres[3];
		var nlabel = getMovieLabelById(nid);
		
		fileHTML += '<tr>';
		fileHTML += '<td>'+i+'</td>';
		fileHTML += '<td><img src="data:image/png;base64,'+nimg+'"></td>';
		fileHTML += '<td>'+nlabel+'</td>';
		fileHTML += '<td>'+msToTime(nmilli)+'</td>';
		fileHTML += '<td>'+ndist+'</td>';
		fileHTML += '</tr>\n';
		
		i++;
	});
	
	fileHTML += '  </table>\n';
	fileHTML += '  </body>\n';
	fileHTML += '</html>';

	var element = document.createElement('a');
	element.setAttribute('href', 'data:text/html;charset=utf-8,' + encodeURIComponent(fileHTML));
	element.setAttribute('download', filename);
	element.style.display = 'none';
	document.body.appendChild(element);
	element.click();
	document.body.removeChild(element);
}

function findFrametrailInstance(movieid) {
	var movieLabel = getMovieLabelById(movieid);
	
	var filteredInstances = FrameTrail.instances.filter(ft => ft.metadata.hypervideoName === movieLabel);
	
	if (filteredInstances.length == 1) {
		return filteredInstances[0];
	} else {
		return null;
	}
}

function jump_to_frametrail(movieid, timemilli) {
	console.log("jump_to_frametrail", movieid, timemilli);
	
	console.log("FrameTrail.instances", FrameTrail.instances);
	
	var fti = findFrametrailInstance(movieid);
	
	if (fti !== null) {
		fti.currentTime = timemilli / 1000;
	} else {
		// var request_objects = generate_request_objects("moviesearch", [movieid], [], ["AnnotationType/Scene"], "", false, []);
		// console.log(request_objects);
		// execute_requests(request_objects);
		//TODO - Request Scene Annotation for movie, initialize frametrail and set currentTime
		
	}
}

var imageSearchNeighbors = '';

function show_image_search_result(framesearchresult) {
	console.log("show_image_search_result");

	var neighbors = framesearchresult['nearest_neighbors'];
	if (typeof neighbors == 'undefined') {
		alert("Image search result is corrupt. Expected field 'nearest_neighbors'. \n Request result: \n"+JSON.stringify(framesearchresult));
		return;
	}
	
	var downloadbutton = document.getElementById('DownloadImageSearchResult');
	
	var imagelistdiv = document.getElementById('imagelist');
	imagelistdiv.innerHTML = '';
	
	if (neighbors.length == 0) {
		imagelistdiv.innerHTML = 'Image search did not find any similar frames';
		downloadbutton.disabled = true;
	} else {
		imageSearchNeighbors = neighbors;
		downloadbutton.disabled = false;
		var corruptFound = false;
		neighbors.forEach(function(nres){
			if (nres.length != 4) {
				corruptFound = true;
			}
		});
		
		if (corruptFound) {
			alert("Image search result is corrupt. Expected 4 entries. \n Nearest neighbors: \n"+JSON.stringify(neighbors));
			return;
		}
		
		var i = 1;
		neighbors.forEach(function(nres){
			var nid = nres[0];
			var nmilli = nres[1];
			var ndist = nres[2];
			var nimg = nres[3];
			var nlabel = getMovieLabelById(nid);
			
			var imgdiv = document.createElement("div");
			imgdiv.classList.add('imagethumbdiv');
			
			var acid = "image_a_"+i;
			var img = document.createElement("img");
			img.setAttribute('src', 'data:image/png;base64,'+nimg);
			img.setAttribute('width', 167);
			img.setAttribute('style', 'margin-bottom: 0px; margin-top: 5px; cursor: pointer;');
			img.setAttribute('title', ' ');
			img.setAttribute('onclick', 'jump_to_frametrail("'+nid+'",'+nmilli+');');
			img.id = acid;
			imgdiv.appendChild(img);
			
			var labeldiv = document.createElement("div");
			labeldiv.classList.add("imagethumbmovielabel");
			var labeltext = document.createTextNode(truncate_name(i+": "+nlabel, 25));
			labeldiv.appendChild(labeltext);
			imgdiv.appendChild(labeldiv);

			var distdiv = document.createElement("div");
			distdiv.classList.add("imagethumbdistlabel");
			var disttext = document.createTextNode(parseFloat(ndist).toFixed(2));
			distdiv.appendChild(disttext);
			imgdiv.appendChild(distdiv);

			var timediv = document.createElement("div");
			timediv.classList.add("imagethumbtimelabel");
			var timetext = document.createTextNode(msToTimeShort(nmilli));
			timediv.appendChild(timetext);
			imgdiv.appendChild(timediv);
			
			imagelistdiv.appendChild(imgdiv);
			
			
			i++;
			
			var acfield = "#"+acid;
			$( acfield ).tooltip({ content: '<img src="data:image/png;base64,'+nimg+'" /><div class="imageresultlabel">'+nlabel+'</div><div class="imageresultdistance">Distance: '+ndist+'</div><div class="imageresulttime">Time: '+msToTime(nmilli)+'</div>' });
		});
		
	}
}

function submit_image_search() {
	console.log("submit_image_search");
	
	var imgup = document.getElementById('imageupload');
	var numresults = document.getElementById('ImageSearchFieldMax').value;

	if (typeof numresults == 'undefined' || numresults == '') {
		numresults = 50;
	}
	
	if (imgup.files.length > 0) {
		lock_interface();
		$.when(request_image_search(imgup.files.item(0), numresults)).then(function(result){
			$('body').toggleClass('imagesearch', true); 
			show_image_search_result(result);
			unlock_interface();
		})
	}
}

function filter_sceneids(annotations, requested_scenes) {
	var result = annotations;
	// Remove scene id assignments for non-requested scenes to avoid display of large cross-scene annotations
	result.forEach(function(annotation) {
		if (Array.isArray(annotation['sceneId'])) {
			var filteredScenes = annotation['sceneId'].filter(s => requested_scenes.includes(s));
			if (filteredScenes.length == 1) {
				annotation['sceneId'] = filteredScenes[0];
			} else {
				annotation['sceneId'] = filteredScenes;
			}
		}
	});
	
	return result;
}

function node_update(node) {
	var textsize = 27;
	if (node.getLevel() == 3) {
		textsize = 23;
	}
	if (node.data.count == 0) {
		node.setTitle(truncate_name(node.data.original_title, textsize));
		node.unselectable = false;
		node.setSelected(false);
		node.unselectableStatus = false;
		node.unselectable = true;
	} else {
		node.setTitle(truncate_name(node.data.original_title, textsize) + " ("+node.data.count+")");
		node.unselectable = false;
		node.unselectableStatus = undefined;
		if (node.getLevel() == 3 || (node.getLevel() == 2 && node.getChildren() == null)) {
			node.setSelected(true);
		}
	}
}

function node_sum(node) {
	var subnodes = node.getChildren();
	if (typeof subnodes !== 'undefined' && subnodes != null) {
		node.data.count = subnodes.reduce((acc, val) => acc + val.data.count, 0);
	}
}

function update_fancytrees() {
	console.log("update_fancytrees");
	
	var annoTree = $.ui.fancytree.getTree("#annotation_tree");
	var movieTree = $.ui.fancytree.getTree("#movie_tree");
	
	annoTree.getRootNode().getChildren().forEach(node_sum);
	movieTree.getRootNode().getChildren().forEach(node_sum);
	
	annoTree.visit(node_update);
	movieTree.visit(node_update);

	annoTree.render();
	movieTree.render();
}

function count_annotations(annotations) {
	console.log("count_annotations");
	
	var counts = new Map();
	var countarray = [];
	
	// Count scene ids
	var allsceneids = annotations.flatMap(a => a.sceneId);
	var sceneid_counts = allsceneids.reduce((acc, e) => acc.set(e, (acc.get(e) || 0) + 1), new Map());

	// Count movie ids
	var allmovies = annotations.map(a => a.target.source.substr(a.target.source.lastIndexOf("/")+1));
	var movie_counts = allmovies.reduce((acc, e) => acc.set(e, (acc.get(e) || 0) + 1), new Map());

	// Get annotation type ids that have subelements (predefined values)
	var metatypes = annotationlevels.flatMap(l => l.subElements);
	var metatypeid_with_subelements = metatypes.filter(t => t.subElements.length > 0).map(t => t.elementUri);

	// Retrieve annotation bodies and filter textual bodies that also have a predefinded values body
	var allbodies = annotations.flatMap(a => a.body)
	var filtered_bodies = allbodies.filter(b => (!metatypeid_with_subelements.includes(b.annotationType) && b.type == "TextualBody") || (b.type !== "TextualBody") );
	
	// Count type ids
	var alltypes = filtered_bodies.flatMap(b => b.annotationType);
	alltypes.forEach(function(part, index, theArray) {
		theArray[index] = "AnnotationType/"+theArray[index].substr(theArray[index].lastIndexOf("/")+1);
	});
	var type_counts = alltypes.reduce((acc, e) => acc.set(e, (acc.get(e) || 0) + 1), new Map());
	
	// Count value ids
	var allvalues = filtered_bodies.flatMap(b => b.annotationValue).filter(v => typeof v !== 'undefined');
	var allvalues = allvalues.concat(filtered_bodies.flatMap(b => b.annotationValueSequence).filter(v => typeof v !== 'undefined'));
	allvalues.forEach(function(part, index, theArray) {
		theArray[index] = "AnnotationValue/"+theArray[index].substr(theArray[index].lastIndexOf("/")+1);
	});
	var value_counts = allvalues.reduce((acc, e) => acc.set(e, (acc.get(e) || 0) + 1), new Map());

	// Set counts on nodes of annotation type tree
	var annotree_counts = new Map([...type_counts, ...value_counts]);
	$.ui.fancytree.getTree("#annotation_tree").visit(function(node){
		var count = annotree_counts.get(node.key);
		if (typeof count !== 'undefined') {
			node.data.count = count;
		} else {
			node.data.count = 0;
		}
	});

	// Set counts on nodes of movie tree
	var movietree_counts = new Map([...sceneid_counts, ...movie_counts]);
	$.ui.fancytree.getTree("#movie_tree").visit(function(node){
		var count = movietree_counts.get(node.key);
		if (typeof count !== 'undefined') {
			node.data.count = count;
		} else {
			node.data.count = 0;
		}
	});
}

function isAnnotationKept(annotation, deselected_nodes) {
	
	var sceneids = [];
	
	if (Array.isArray(annotation['sceneId'])) {
		sceneids = annotation['sceneId'];
	} else {
		sceneids.push(annotation['sceneId']);
	}
	
	sceneids = sceneids.filter(s => !deselected_nodes.includes(s));
	if (sceneids.length == 0) {
		return false;
	}
	
	var bodies = [];
	if (Array.isArray(annotation['body'])) {
		bodies = annotation['body'];
	} else {
		bodies.push(annotation['body']);
	}
	
	var types = [...new Set(bodies.flatMap(b => b.annotationType))];
	types.forEach(function(part, index, theArray) {
		theArray[index] = "AnnotationType/"+theArray[index].substr(theArray[index].lastIndexOf("/")+1);
	});
	
	types = types.filter(t => !deselected_nodes.includes(t));
	if (types.length == 0) {
		return false;
	}
	
	var aval = bodies.flatMap(b => b.annotationValue).filter(v => typeof v !== 'undefined');
	var avalseq = bodies.flatMap(b => b.annotationValueSequence).filter(v => typeof v !== 'undefined');
	
	var values = [...new Set(aval.concat(avalseq))];
	values.forEach(function(part, index, theArray) {
		theArray[index] = "AnnotationValue/"+theArray[index].substr(theArray[index].lastIndexOf("/")+1);
	});
	
	if (values.length !== 0) {
		values = values.filter(t => !deselected_nodes.includes(t));
		if (values.length == 0) {
			return false;
		}
	}
	
	return true;
}

function checkAnnotationConsistency(annotations) {
	var result = [];
	
	annotations.forEach(function(anno) {
		var check_passed = true;
		
		if (typeof anno['id'] === 'undefined') {
			check_passed = false;
			console.log("Corrupt annotation filtered", "id not found", anno);
		}
		
		var effected_movie = "";
		
		var mediauri = anno['id'].substr(0, anno['id'].lastIndexOf("/"));
		if (typeof mediauri !== 'undefined') {
			var movieid = mediauri.substr(mediauri.lastIndexOf("/")+1);
			effected_movie = getMovieLabelById(movieid);
		}
		
		if (typeof anno['advene:type'] === 'undefined') {
			check_passed = false;
			console.log("Corrupt annotation filtered:", "\""+effected_movie+"\"",  "advene:type not found", anno['id']);
		}
		// if (typeof anno['advene:type_color'] === 'undefined') {
			// check_passed = false;
			// console.log("Corrupt annotation filtered:", "\""+effected_movie+"\"", "advene:type_color not found", anno['id']);
		// }
		if (typeof anno['advene:type_title'] === 'undefined') {
			check_passed = false;
			console.log("Corrupt annotation filtered:", "\""+effected_movie+"\"", "advene:type_title not found", anno['id']);
		}
		if (typeof anno['target'] === 'undefined') {
			check_passed = false;
			console.log("Corrupt annotation filtered:", "\""+effected_movie+"\"",  "target not found", anno['id']);
		}
		if (Array.isArray(anno['target'])) {
			check_passed = false;
			console.log("Corrupt annotation filtered:", "\""+effected_movie+"\"",  "multiple targets found", anno['id']);
		}		
		if (check_passed == true) {
			result.push(anno);
		}
	});
	
	return result; 
}

function getCurrentAnnotationData() {
	console.log("getCurrentAnnotationData");
	var annotation_structure = [];
	
	// Get all moviesearch annotation from the request results
	var movie_search_annotations = request_results.flatMap(ro => ro.searchtype == "moviesearch" ? ro.annotations : []);
	// Deep copy annotations in order keep originals inside of "requests_results" and pass modified annotations to Frametrail
	movie_search_annotations = JSON.parse(JSON.stringify(movie_search_annotations));
	var requested_scenes = request_results.flatMap(ro => ro.sceneids.map(s => s.split("_")[1]));
	var movie_search_annotations = requested_scenes.length > 0 ? filter_sceneids(movie_search_annotations, requested_scenes) : movie_search_annotations;
	
	var rest_annotations = request_results.flatMap(ro => ro.searchtype != "moviesearch" ? ro.annotations : []);
	// Deep copy annotations in order keep originals inside of "requests_results" and pass modified annotations to Frametrail
	rest_annotations = JSON.parse(JSON.stringify(rest_annotations));
	
	var all_annotations = movie_search_annotations.concat(rest_annotations);
	
	// Filter corrupt annotations
	var consistent_annotations = checkAnnotationConsistency(all_annotations);
	if (consistent_annotations.length != all_annotations.length) {
		$( "div.filtererror" ).fadeIn( 300 ).delay( 8000 ).fadeOut( 400 );
	}

	// Filter duplicates that can come from different requests
	var tmp_anno_ids = [];
	var filtered_annotations = [];
	consistent_annotations.forEach(function(anno){
		if (!tmp_anno_ids.includes(anno.id)) {
			tmp_anno_ids.push(anno.id);
			filtered_annotations.push(anno);
		}
	});
	
	count_annotations(filtered_annotations);
	update_fancytrees();
	fix_deselected_nodes();
	
	// Filter according to the selection in the trees
	var frametrail_annotations = [];
	filtered_annotations.forEach(function(anno){
		if (isAnnotationKept(anno, trees_deselected_nodes)) {
			frametrail_annotations.push(anno);
		}
	});
	
	var all_movies = [...new Set(frametrail_annotations.map(a => a.target.source.substr(a.target.source.lastIndexOf("/")+1)))];
	var all_scenes = [...new Set(frametrail_annotations.flatMap(a => a.sceneId))];
	
	all_movies.forEach(function(movieid) {
		var movie = movies.find(m => m.id == movieid);
		var struct = {id: movie.id, source: movie.playoutUrl, title: movie.title, uri: movie.mediauri, scenes: [], duration: movie.duration};
		annotation_structure.push(struct);
		var scenes = [];
		if (movie.scenes !== null && movie.scenes.length > 0) {
			movie.scenes.forEach(function(scene){
				if (all_scenes.includes(scene.id)) {
					scenes.push({id: scene.id, title: zeroPad(scene.shortId,2)+": "+scene.name, startTime: scene.startTime, endTime: scene.endTime, annotations: []});
				}
			});
		}
		struct.scenes = scenes;
		
	});
	
	frametrail_annotations.forEach(function(anno){
		var movieid = anno.target.source.substr(anno.target.source.lastIndexOf("/")+1);
		var movie = annotation_structure.find(m => m.id == movieid);
		var scenes_to_process = [];
		
		// Patch data URIs to current protocol and hostname. This is necessary to load the annotation preview in case if mixed http/https and if the frontend is running on a different hostname.
		if (window.location.protocol.toLowerCase().startsWith("http") && window.location.hostname != "" ) {
			var enturl = new URL(anno['id']);
			if (typeof enturl.pathname !== 'undefined' && enturl.pathname != "") {
				enturl.protocol = window.location.protocol;
				enturl.hostname = window.location.hostname;
				enturl.port = window.location.port;
				anno['id'] = enturl.href;
			}
		}
		
		if (typeof anno.sceneId === 'undefined') {
			console.log("Error: scene id is missing for annotation",anno.id);
		} else {
			if (Array.isArray(anno.sceneId)) {
				scenes_to_process = anno.sceneId;
			} else {
				scenes_to_process.push(anno.sceneId);
			}
			scenes_to_process.forEach(function(sceneid){
				var scene = movie.scenes.find(s => s.id == sceneid);
				if (typeof scene === 'undefined') {
					console.log("Error: scene id of annotation is missing in movie metadata",anno.id);
				} else {
					scene.annotations.push(anno);
				}
			});
		}
	});
	
	return annotation_structure;
}

function image_selection(event) {
	var imagepreview = document.getElementById('imagepreview');
	//imagepreview.src = URL.createObjectURL(event.target.files[0]);
	
	const reader = new FileReader();
	reader.addEventListener('load', (event) => {
		imagepreview.src = event.target.result
		//imageSearchUploadedFile = event.target.result;
	});
	reader.readAsDataURL(event.target.files[0]);
}

function init_interface() {
	console.log("init_interface");
	
	document.getElementById('imageupload').value = '';
	document.getElementById('statusLabel').title = '';
	document.getElementById('statusLabel').textContent = '';
	document.getElementById('statusLabel').style.fontStyle = null;

	request_status().then(function(values){
		if (typeof values !== 'undefined') {
			if (typeof values['status'] !== 'undefined' && typeof values['annotationsTotal'] !== 'undefined') {
				document.getElementById('statusLabel').textContent = values['annotationsTotal'];
			}
			if (typeof values['error'] !== 'undefined') {
				document.getElementById('statusLabel').textContent = 'Error...';
				document.getElementById('statusLabel').title = values['error'];
				document.getElementById('statusLabel').style.fontStyle = 'italic';
			}
		}
	}).catch(
		function(errorThrown) {
			document.getElementById('statusLabel').textContent = 'Error...';
			document.getElementById('statusLabel').title = 'API could not be reached.';
			document.getElementById('statusLabel').style.fontStyle = 'italic';
		}
	);
	
		// lock_interface();
		// init_ontology_tree();
		// init_movie_tree();
		// init_search_fields();
		// unlock_interface();
		// updateUIfromURL();

		lock_interface();
		Promise.all([request_ontology(), request_movie_metadata()]).then(function(values){
			annotationlevels = values[0];
			post_process_ontology_metadata();
			movies = values[1];
			post_process_movie_metadata();
			init_ontology_tree();
			init_movie_tree();
			init_search_fields();
			unlock_interface();
			updateUIfromURL();
		});
}
