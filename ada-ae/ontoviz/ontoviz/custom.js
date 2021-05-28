var METADATA_LINK = "/api/getOntology";

var nodes, edges, network, nodesFull, edgesFull, nodesInitial, container, data, options;

var doubleClickTime = 0;
var threshold = 200;


function getMetadata() {
	console.log("getMetadata");
	
	$.ajax({
		url: METADATA_LINK,
		type: 'GET',
		dataType: "json",
		contentType: "application/json; charset=utf-8",		
		success: function(result) {
			if(result.error) {
				alert(result.error.message + " Code: " + result.error.code);
			}else {
				initNetwork(result);
			}
		}
	});
}


function expandAll() {
	
	var waittext = document.getElementById('wait');
	waittext.innerHTML = "Please wait for the full network to be redrawn...";
	
	console.log('begin expand:');
	var n = new vis.DataSet();
	var e = new vis.DataSet();
	var dataTemp = {
		nodes: n,
		edges: e
	};
	
	network = new vis.Network(container, dataTemp, options);
	
	nodes = new vis.DataSet();
	edges = new vis.DataSet();
	
	nodesFull.forEach(function(elem)  {
		nodes.add(elem);
	});
	edgesFull.forEach(function(elem)  {
		edges.add(elem);
	});

	data = {
			nodes: nodes,
			edges: edges
	};

	network = new vis.Network(container, data, options);
	addEvents();
	console.log('end expand:');
}


function collapseAll() {
	var n = new vis.DataSet();
	var e = new vis.DataSet();
	var dataTemp = {
		nodes: n,
		edges: e
	};
	
	network = new vis.Network(container, dataTemp, options);
	
	nodes = new vis.DataSet();
	edges = new vis.DataSet();
	
	nodesInitial.forEach(function(elem)  {
		nodes.add(elem);
	});
	edgesInitial.forEach(function(elem)  {
		edges.add(elem);
	});

	data = {
		nodes: nodes,
		edges: edges
	};
	
	network = new vis.Network(container, data, options);
	addEvents();
}


function initNetwork(metadata) {
	console.log("initNetwork");
	
	nodes = new vis.DataSet();
	edges = new vis.DataSet();
	nodesFull = new vis.DataSet();
	edgesFull = new vis.DataSet();
	
	nodes.add({id: 0, label: "eMAEX\nannotation\nmethod", url: "http://ada.cinepoetics.org/resource/2021/05/19/eMAEXannotationMethod"});
	nodesFull.add({id: 0, label: "eMAEX\nannotation\nmethod", url: "http://ada.cinepoetics.org/resource/2021/05/19/eMAEXannotationMethod"});
	
	i = 1;
	for( const leveldata of metadata ) {
		var r_value = 0;
		var g_value = 0;
		var b_value = 0;
		var counter = 0;

		edges.add({from: 0, to: i});
		edgesFull.add({from: 0, to: i});
		
		levelParentId = i;
		i++;
		
		for( const typedata of leveldata.subElements ) {
			nodesFull.add({id: i, label: typedata.elementName.replace(" ", "\n"), title: typedata.elementDescription, color: typedata.elementColor, url: typedata.elementUri});
			
			edgesFull.add({from: levelParentId, to: i});
			
			typeParentId = i;
			i++;
			
			var type_color = typedata.elementColor;
			type_color = type_color.replace("#", "");
			
			r_value = r_value + parseInt(type_color.substring(0,2), 16);
			g_value = g_value + parseInt(type_color.substring(2,4), 16);
			b_value = b_value + parseInt(type_color.substring(4,6), 16);
			counter++;				
			
			var value_color = add_rgbcolor(typedata.elementColor, 40);

			if( typedata.subElements != null ) {
				for( const valuedata of typedata.subElements ) {
					nodesFull.add({id: i, label: valuedata.elementName.replace(" ", "\n"), title: valuedata.elementDescription, color: '#' + value_color, url: valuedata.elementUri});
					
					edgesFull.add({from: typeParentId, to: i});
					
					i++;	
					
				}
			}
		}
		
		r_value = Math.round(r_value / counter);
		g_value = Math.round(g_value / counter);
		b_value = Math.round(b_value / counter);
		r_value = r_value.toString(16);
		g_value = g_value.toString(16);
		b_value = b_value.toString(16);
		
		if( r_value.length == 1 ) {
			r_value = '0' + r_value;
		}

		if( g_value.length == 1 ) {
			g_value = '0' + g_value;
		}
	
		if( b_value.length == 1 ) {
			b_value = '0' + b_value;
		}
	
		var level_color = '#' + r_value + g_value + b_value;

		nodes.add({id: levelParentId, label: leveldata.elementName.replace(" ", "\n"), color: level_color, url: leveldata.elementUri});
		nodesFull.add({id: levelParentId, label: leveldata.elementName.replace(" ", "\n"), title: leveldata.elementDescription, color: level_color, url: leveldata.elementUri});		
	}
	
	nodesInitial = new vis.DataSet();
	edgesInitial = new vis.DataSet();
	
	nodes.forEach(function(elem)  {
		nodesInitial.add(elem);
	});
	edges.forEach(function(elem)  {
		edgesInitial.add(elem);
	});
	
	container = document.getElementById('mynetwork');
	data = {
		nodes: nodes,
		edges: edges
	};
	options = {
			nodes: {
				//shape: 'dot',
				size: 30,
				font: {
					size: 16
				},
				borderWidth: 2,
				shadow:true
			},
			edges: {
				width: 2,
				shadow:true
			},
			width: (window.innerWidth - 100) + "px",
			height: (window.innerHeight) + "px"
		};
	network = new vis.Network(container, data, options);
	
	addEvents();
}


function add_rgbcolor(type_color, summand) {
	type_color = type_color.replace("#", "");
	
	var r_value = parseInt(type_color.substring(0,2), 16);
	var g_value = parseInt(type_color.substring(2,4), 16);
	var b_value = parseInt(type_color.substring(4,6), 16);
	
	r_value = r_value + summand;
	if( r_value > 255 ) {
		r_value = 255;
	}
	
	g_value = g_value + summand;
	if( g_value > 255 ) {
		g_value = 255;
	}

	b_value = b_value + summand;
	if( b_value > 255 ) {
		b_value = 255;
	}
	
	r_value = r_value.toString(16);
	g_value = g_value.toString(16);
	b_value = b_value.toString(16);
	
	value_color = r_value + g_value + b_value;
	
	return value_color;
}


function addEvents() {
	network.on('click', onClick);
	network.on('doubleClick', onDoubleClick);
	network.on('afterDrawing', afterDrawing);
}


function afterDrawing(params) {
	network.off('afterDrawing');
	
	var waittext = document.getElementById('wait');
	waittext.innerHTML = "";
	
}


function onClick(params) {
	var t0 = new Date();
	if (t0 - doubleClickTime > threshold) {
		setTimeout(function () {
			if (t0 - doubleClickTime > threshold) {
				doOnClick(params);
			}
		},threshold);
	}
}


function doOnClick(params) {
	if (params.nodes.length === 1) {
		var nodeID = params.nodes[0];
		var clickedNode = nodes.get(nodeID);
		window.open(clickedNode.url, '_blank');
	}
}


function onDoubleClick(params) {
	doubleClickTime = new Date();
	if (params.nodes.length === 1) {
		var nodeID = params.nodes[0];
		var clickedNode = nodes.get(nodeID);
		
							
		var outEdgesFull = edgesFull.get({
				filter: function (item) {
					return (item.from == nodeID)
				}
		});
		
		var child_node_ids = [];
		edges.get({
			filter: function (item) {
				if( item.from == nodeID || child_node_ids.includes(item.from) ) {
					child_node_ids.push(item.to);
					return (item.from == nodeID)
				}
			}
		});       		
		
		var nodes_to_remove = [];
		edges.get({
			filter: function (item) {
				if( item.from == nodeID || child_node_ids.includes(item.from) ) {
					nodes_to_remove.push(item.to);
					return true;
				}
			}
		}); 
		
		var outEdges = edges.get({
			filter: function (item) {
				if( item.from == nodeID || nodes_to_remove.includes(item.from) ) {
					return true;
				}
			}
		}); 
		
		var outNodesFull = new Array();
		for (var i = 0; i < outEdgesFull.length; i++) {
			outNodesFull.push(nodesFull.get(outEdgesFull[i].to));
		}

		var outNodes = new Array();
		for (var i = 0; i < outEdges.length; i++) {
			outNodes.push(nodes.get(outEdges[i].to));
		}
		
		if (outEdges.length > 0) {
			edges.remove(outEdges);
			nodes.remove(outNodes);
		} else {
			edges.add(outEdgesFull);
			nodes.add(outNodesFull);
		}
	}
}