//const apiUrl='http://127.0.0.1:7070/api'; 
const apiUrl='/api'; 

var movies = [];

function request_movie_metadata() {
	console.log("request_movie_metadata");
	return $.ajax({
		url: apiUrl + "/getMovieMetadata",
		timeout: 120000,
		error: function(request, status, error) {
			alert("Loading movie metadata failed. API could not be reached. Please reload the page.\n responseText: " + request.responseText + "\n request.status: " + request.status + "\n status: " + status + "\n error: " + error);
		}
	});
}

function getMetadata() {
	request_movie_metadata().then(function(values) {
		render_table(values);
	});
}

function render_table(data) {
	console.log("render_table");
	
	var header = ['Category', 'Title', 'Year', 'Runtime', 'Director', 'Film Version', 'IMDB', 'Annotations', 'Metadata'];
	
	var table = document.createElement("TABLE");
	table.id = "video_list";
	table.setAttribute('class', "table table-striped");
	
	var head = table.createTHead();
	var row = head.insertRow(0);  
	
	header.forEach(function(element) {
		var cell = document.createElement("th");
		row.appendChild(cell);
		cell.innerHTML = element;
	});
	
	var tbdy = document.createElement('tbody');
	
	data.forEach(function(row_data) {
		var tr = document.createElement('tr');
		
		tr.appendChild(create_cell(row_data.category));
		tr.appendChild(create_cell(row_data.title));
		tr.appendChild(create_cell(row_data.year));
		tr.appendChild(create_cell(row_data.runtime + " min"));
		tr.appendChild(create_cell(row_data.director));
		tr.appendChild(create_cell_shortend(row_data.filmversion));
		if (row_data.imdbId == null || row_data.imdbId == "") {
			tr.appendChild(create_cell(" "));
		} else {
			tr.appendChild(create_cell_with_link(row_data.imdbId, "https://www.imdb.com/title/"+row_data.imdbId));
		}
		tr.appendChild(create_cell(row_data.annotationsTotal));
		tr.appendChild(create_cell_with_link("Link", row_data.mediauri));
		
		tbdy.appendChild(tr);
	});
	
	table.appendChild(tbdy);
	document.getElementById("corpus_table").appendChild(table);
}

function create_cell(content) {
	
	var td = document.createElement('td');
	td.innerHTML = content;
	
	return td;
}

function create_cell_with_link(content, url) {
	
	var td = document.createElement('td');
	var link = document.createElement("a");
	link.setAttribute("href", url);
	link.className = "link";
	link.appendChild(document.createTextNode("Link"));
	link.title = content;
	link.target = "_blank";
	td.appendChild(link);
	
	return td;
}


function create_cell_shortend(content) {
	
	var td = document.createElement('td');
	
	var abbr = document.createElement('abbr');
	abbr.title = content;
	abbr.innerHTML = truncate_name(content, 28);
	td.appendChild(abbr);
	
	return td;
}


function truncate_name(name, length) {
	if (name.length > length) {
		return name.substring(0,length -3)+"...";
	} else {
		return name;
	}
}

