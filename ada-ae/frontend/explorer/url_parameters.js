var searchTypeMapping = {
    'moviesearch': 'm',
    'textsearch': 't',
    'imagesearch': 'i',
    'valuesearch': 'v'
}

var uiOptionsMapping = {
    'timeline-charts': 'c',
    'timeline-background': 'b',
    'timeline-color': 'f',
    'timeline-horizontal-grid': 'h',
    'timeline-keys': 'k'
}

$(document).ready(function() {
    window.onpopstate = function(event) {
        //updateUIfromURL();
    }
});

function updateURL() {
    history.pushState(null, "", "?"+ getSerializedParameters());
}

function updateUIfromURL() {
    var urlValues = getURLValues();
    if (urlValues.uiOptions.length != 0) {
        for (var i = 0; i < urlValues.uiOptions.length; i++) {
            var fieldName = uiOptionsMapping[urlValues.uiOptions[i]];
            $('body').attr('data-'+ urlValues.uiOptions[i], 'true');
            $('#resultPanelOptions input[name="'+ fieldName +'"]').attr('checked', true);
        }
    } else {
        // apply default values
        $('body').attr('data-timeline-charts', 'true');
        $('#resultPanelOptions input[name="'+ uiOptionsMapping['timeline-charts'] +'"]').attr('checked', true);
        /*
        $('body').attr('data-timeline-background', 'false');
        $('#resultPanelOptions input[name="'+ uiOptionsMapping['timeline-background'] +'"]').attr('checked', false);
        */
        $('body').attr('data-timeline-color', 'true');
        $('#resultPanelOptions input[name="'+ uiOptionsMapping['timeline-color'] +'"]').attr('checked', true);
        $('body').attr('data-timeline-keys', 'true');
        $('#resultPanelOptions input[name="'+ uiOptionsMapping['timeline-keys'] +'"]').attr('checked', true);
    }

    $('#resultPanelActions .button[data-result-view]').removeClass('active');
    $('#resultPanelActions .button[data-result-view="'+ urlValues.view +'"]').addClass('active');
    resultOptionsView = urlValues.view;

    $('#resultPanelActions .button[data-result-unit]').removeClass('active');
    $('#resultPanelActions .button[data-result-unit="'+ urlValues.unit +'"]').addClass('active');
    resultOptionsUnit = urlValues.unit;

    if (urlValues.prioritizedTypes) {
        prioritizedAnnotationTypes = [];
        var typeIDList = urlValues.prioritizedTypes.split('-').map(function(x){return parseInt(x)});
        var listOfAllTypes = getTypeListFromOntology();
        for (var i = 0; i < typeIDList.length; i++) {
            var thisLabel = '';
            for (var a = 0; a < listOfAllTypes.length; a++) {
                if (listOfAllTypes[a].id == typeIDList[i]) {
                    thisLabel = listOfAllTypes[a].label;
                }
            }
            prioritizedAnnotationTypes.push({
                'value': typeIDList[i],
                'id': typeIDList[i],
                'label': thisLabel
            });
        }
    }
	
	var request_objects = [];

	urlValues.requests.forEach(function(req){
		var parameterMappingOk = 1;
		
		var movieids = req.movieIDs.map(m => getMovieLongId(m)).filter(m => m !== "");
		if (req.movieIDs.length != movieids.length) {
			alert("Unknown movie short id used in URL: "+req.movieIDs+"\n"+JSON.stringify(req));
			parameterMappingOk = 0;
		}
		
		var sceneids = [];
		if (movieids.length == 1 && typeof req.sceneIDs !== 'undefined') {
			sceneids = req.sceneIDs.map(s => movieids[0] + "_" + getSceneLongId(movieids[0], s)).filter(s => !s.endsWith("_"));
			if (req.sceneIDs.length != sceneids.length) {
				alert("Unknown scene short id used in URL: "+req.sceneIDs+"\n"+JSON.stringify(req));
				parameterMappingOk = 0;
			}
		}

		var typeids = req.typeIDs.map(t => getTypeLongId(t)).filter(t => t !== "");
		if (req.typeIDs.length != typeids.length) {
			alert("Unknown type short id used in URL: "+req.typeIDs+"\n"+JSON.stringify(req));
			parameterMappingOk = 0;
		}

		var searchterm = Object.keys(req).includes("searchText") ? req.searchText : "";
		var wholeWord = Object.keys(req).includes("wholeWord") ? req.wholeWord : false;
		
		var valueids = req.valueIDs.map(v => v.map(sv => getValueLongId(sv)).filter(v => v !== ""));
		if (req.valueIDs.length != valueids.length) {
			alert("Unknown value short id used in URL: "+req.valueIDs+"\n"+JSON.stringify(req));
			parameterMappingOk = 0;
		}
		for (let i = 0; i < req.valueIDs.length; i++) {
			if (req.valueIDs[i].length != valueids[i].length) {
				alert("Unknown value short id used in URL: "+req.valueIDs[i]+"\n"+JSON.stringify(req));
				parameterMappingOk = 0;
			}
		} 
		
		if (parameterMappingOk == 1) {
			var ros = generate_request_objects(req.searchType, movieids, sceneids, typeids, searchterm, wholeWord, valueids);
			request_objects = request_objects.concat(ros);
		}
	});
	
	if (request_objects.length > 0) {
		execute_requests(request_objects);
	}
}

function getURLValues() {
    var queryVariables = getAllQueryVariables(),
        returnObj = {
            requests: [],
            uiOptions: [],
            view: 'movie',
            unit: 'movie'
        }

    for (var i = 0; i < queryVariables.length; i++) {
        if (queryVariables[i].name == 'r[]') {
            returnObj.requests.push(parseRequestInfo(queryVariables[i].value));
        } else if (queryVariables[i].name == 'ui') {
            returnObj.uiOptions = parseUIInfo(queryVariables[i].value);
        } else if (queryVariables[i].name == 'view') {
            returnObj.view = queryVariables[i].value;
        } else if (queryVariables[i].name == 'unit') {
            returnObj.unit = queryVariables[i].value;
        } else if (queryVariables[i].name == 'pt') {
            returnObj.prioritizedTypes = queryVariables[i].value;
        }
    }

    return returnObj;
}

function parseRequestInfo(requestString) {
	
    var requestStringParts = requestString.split('_'),
        type = requestStringParts[0],
        requestInfo = {};

    requestInfo.searchType = Object.keys(searchTypeMapping).filter(function(key) {return searchTypeMapping[key] === type})[0];

    switch (type) {
        case 'm': 
            requestInfo.movieIDs = (requestStringParts[1] != 'n') ? requestStringParts[1].split('-') : [];
            requestInfo.sceneIDs = (requestStringParts[2] != 'n') ? requestStringParts[2].split('-') : [];
            requestInfo.typeIDs = (requestStringParts[3] != 'n') ? requestStringParts[3].split('-') : [];
			requestInfo.valueIDs = [];
            break;
        case 't': 
            requestInfo.movieIDs = (requestStringParts[1] != 'n') ? requestStringParts[1].split('-') : [];
            requestInfo.searchText = (requestStringParts[2] != 'n') ? requestStringParts[2] : null;
            requestInfo.wholeWord = (requestStringParts[3] == 'w') ? true : false;
            requestInfo.typeIDs = (requestStringParts[4] != 'n') ? requestStringParts[4].split('-') : [];
			requestInfo.valueIDs = [];
            break;
        case 'i': 
            //TODO: implement image search parts
            break;
        case 'v': 
            requestInfo.movieIDs = (requestStringParts[1] != 'n') ? requestStringParts[1].split('-') : [];
            requestInfo.valueIDs = (requestStringParts[2] != 'n') ? requestStringParts[2].split("--").map(v => v.split("-")) : [];
			requestInfo.typeIDs = [];
            break;
        default:
            break;
    }

    return requestInfo;
}

function parseUIInfo(uiString) {

    var uiStringParts = uiString.split(''),
        uiInfo = [];

    for (var i = 0; i < uiStringParts.length; i++) {
        var actualKey = Object.keys(uiOptionsMapping).filter(function(key) {return uiOptionsMapping[key] === uiStringParts[i]})[0];
        uiInfo.push(actualKey);
    }

    return uiInfo;
}

function getSerializedParameters() {
    var paramString = '',
        requests = [];

    for (var i = 0; i < request_results.length; i++) {
		var searchType = searchTypeMapping[request_results[i].searchtype];
		var movieIDs = (request_results[i].movieids && request_results[i].movieids.length != 0) ? request_results[i].movieids.map(m => getMovieShortId(m)).join('-') : 'n';
		var sceneIDs = 'n';
		if (request_results[i].movieids && request_results[i].movieids.length == 1 && request_results[i].sceneids && request_results[i].sceneids.length != 0) {
			sceneIDs = request_results[i].sceneids.map(s => getSceneShortId(request_results[i].movieids[0], s.split("_")[1])).join('-');
		}
		var typeIDs = (request_results[i].typeids && request_results[i].typeids.length != 0) ? request_results[i].typeids.map(m => getTypeShortId(m)).join('-') : 'n';
		var searchText = (searchType == 't') ? request_results[i].searchterm : '';
		var wholeWord = (searchType == 't' && request_results[i].whole) ? 'w' : 's';
		
		var valueIDs = (request_results[i].value_ids && request_results[i].value_ids.length != 0) ? request_results[i].value_ids.map(v => v.map(s => getValueShortId(s)).join("-")).join("--") : 'n';
		
        var requestString;
        switch (searchType) {
            case 'm': 
                requestString = searchType +'_'+ movieIDs +'_'+ sceneIDs +'_'+ typeIDs;
                break;
            case 't': 
                requestString = searchType +'_'+ movieIDs +'_'+ searchText +'_'+ wholeWord +'_'+ typeIDs;
                break;
            case 'i': 
                //TODO: implement image search parts
                requestString = searchType +'_';
                break;
            case 'v': 
                requestString = searchType +'_' + movieIDs + '_' + valueIDs;
                break;
            default:
                break;
        }
        
        requests.push(requestString);

        paramString += 'r[]='+ requestString +'&';
    }

    //paramString.slice(0, -1);

    var resultPanelOptionsString = $('#resultPanelOptions :input').filter(function(index, element) {
        //console.log(element);
        if ($(element).attr('type') == 'submit') {
            return false;
        } else if ($(element).val() != '') {
            return true;
        } else {
            return false;
        }
    }).serialize().replace(/=on/g, '').replace(/&/g, '');

    paramString += 'ui='+ resultPanelOptionsString;

    paramString += '&unit='+ resultOptionsUnit;
    paramString += '&view='+ resultOptionsView;

    if (prioritizedAnnotationTypes.length != 0) {
        var prioritizedString = '';
        for (var i = 0; i < prioritizedAnnotationTypes.length; i++) {
            prioritizedString += prioritizedAnnotationTypes[i].id;
            if (i != prioritizedAnnotationTypes.length-1) {
                prioritizedString += '-';
            }
        }
        paramString += '&pt='+ prioritizedString;
    }

    return paramString;
}

function getAllQueryVariables() {
    var query = window.location.search.substring(1),
        vars = query.split("&"),
        returnArray = [];
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("="),
            returnValues = null;
        
        pair[0] = decodeURIComponent(pair[0]);
        pair[1] = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
        
        if (pair[0].indexOf('[]') != -1) {
            if (!returnValues) returnValues = [];
            returnValues.push(pair[1]);
        } else {
            returnValues = pair[1];
        }

        returnArray.push({
            name: pair[0],
            value: pair[1]
        });
    }

    return returnArray;
}

function getQueryVariable(variable) {
    var query = window.location.search.substring(1),
        vars = query.split("&"),
        pair,
        returnValues = null;
    for (var i = 0; i < vars.length; i++) {
        pair = vars[i].split("=");
        
        pair[0] = decodeURIComponent(pair[0]);
        pair[1] = decodeURIComponent(pair[1]).replace(/\+/g, ' ');
        
        if (pair[0].indexOf('[]') != -1) {
            if (pair[0].replace('[]', '') == variable) {
                if (!returnValues) returnValues = [];
                returnValues.push(pair[1]);
            }
        } else if (pair[0] == variable) {
            returnValues = pair[1];
        }
    }

    return returnValues;
}