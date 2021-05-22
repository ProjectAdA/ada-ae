$(document).ready(function() {
    $('.resultPanelNavButton#resultPanelNavLeft').click(function() {
        moveResultsLeft();
    });

    $('.resultPanelNavButton#resultPanelNavRight').click(function() {
        moveResultsRight();
    });
});

var resizeTimer;
$(window).resize(function() {
    updateTrapezoids();
    if (resultOptionsView != 'list') {
        //updateMovieOffset();
        clearTimeout(resizeTimer);
        //var offsetResult = parseInt($('#resultPanel').attr('data-offset-movie'));
        resizeTimer = setTimeout(function() {
            updateMovieOffset();
            setTimeout(function() {
                updateMovieOffset();
                //updateActiveResultIndicator(offsetResult);
            },200);
        }, 200);
    }
});


var previousAnnotationData;

function initFrameTrail(resultOffset) {
    console.log("initFrameTrail");

    //$('#loadingResults').show();

    if (!resultOffset) {
        
        for (var i = FrameTrail.instances.length - 1; i >= 0; i--) {
            FrameTrail.instances[i].destroy();
        }
        
        $('#resultPanel').empty();
        $('#resultPanel').css('left', '0px');
        $('#resultPanel').css('width', '');
        $('#resultPanel').attr('data-offset-movie', 0);
        resultCache = {};
        var annotationData = getCurrentAnnotationData();
        previousAnnotationData = annotationData;
        renderResultIndicators(annotationData);
    } else {
        var annotationData = previousAnnotationData;
    }

    updateTrapezoids();

    totalScenesCount = 0;
    
    //console.log(annotationData);

    // old - ontologyDate = annotationlevels[0].types[0].uri.split('resource/')[1].split('/AnnotationType')[0];
	
	ontologyDate = annotationlevels[0].elementUri.split('resource/')[1].split('/AnnotationLevel')[0];

    totalMoviesCount = annotationData.length
    $('#resultPanel').attr('data-movie-count', totalMoviesCount);

    //console.log(annotationData[0].scenes[0].annotations);

    //console.log('Number of Movies: '+ annotationData.length);

    if (totalMoviesCount == 0) {
        $('#loadingResults').hide();
        $('#intro').show();
        $('#searchFacetOptions').hide();
        return;
    }

    $('#intro').hide();
    $('#searchFacetOptions').show();

    if (resultOptionsView == 'list') {

        $('body').removeClass('compareMode');

        var annotationList = [],
            firstAnnotationOffsetStart = null,
            lastAnnotationOffsetEnd = null;

        for (var m=0; m<annotationData.length; m++) {
            var movie = annotationData[m];
            
            resultCache[movie.id] = movie;

            for (var s=0; s<movie.scenes.length; s++) {
                var scene = movie.scenes[s];
                totalScenesCount++;
                for (var a=0; a<scene.annotations.length; a++) {
                    scene.annotations[a].movieID = movie.id;
                    scene.annotations[a].movieTitle = movie.title;
                    scene.annotations[a].movieDuration = movie.duration / 1000;
                    scene.annotations[a].sceneTitle = scene.title;
                    scene.annotations[a].sceneIndex = s;
                    annotationList.push(scene.annotations[a]);
                }
            }
        }

        annotationList = optimizeAnnotations(annotationList);
        //console.log(annotationList);

        renderResultList(annotationList);

    } else {

        var resultItemOffset = (resultOffset) ? resultOffset : 0;

        var localMovieList = getLocalMovieListFromStorage();

        if (resultOptionsUnit == 'scene') {
            
            var allScenes = [];

            for (var m=0; m<annotationData.length; m++) {
                
                var movie = annotationData[m];
                resultCache[annotationData[m].id] = annotationData[m];
                window['player'+ movie.id + 'movieTitle'] = annotationData[m].title;
                videoFileSources[movie.id] = (localMovieList[movie.id]) ? localMovieList[movie.id] : annotationData[m].source;

                for (var s=0; s<movie.scenes.length; s++) {
                    movie.scenes[s].movieID = movie.id;
                    movie.scenes[s].movieDuration = movie.duration / 1000;
                    movie.scenes[s].sceneIndex = s;
                    allScenes.push(movie.scenes[s]);
                    totalScenesCount++;
                }
            }

            for (var as=resultItemOffset; as<allScenes.length; as++) {
                    
                // only load a maximum of 2 results
                var maxResults = (resultItemOffset == 0) ? resultItemOffset+1 : resultItemOffset;
                if (as > maxResults) {
                    $('#loadingResults').hide();
                    continue;
                }

                var resultMovieItem = $('<div class="resultMovieItem" data-movie-id="'+ allScenes[as].movieID +'" data-scene-index="'+ allScenes[as].sceneIndex +'" data-movie-duration="'+ allScenes[as].movieDuration +'">'
                                    +   '    <div class="playerPanel"></div>'
                                    +   '</div>');
                $('#resultPanel').append(resultMovieItem);
                
                renderResultScene(allScenes[as], resultMovieItem);

            }

        } else {
            for (var m=resultItemOffset; m<annotationData.length; m++) {
                // only load a maximum of 2 results
                var maxResults = (resultItemOffset == 0) ? resultItemOffset+1 : resultItemOffset;
                if (m > maxResults) {
                    $('#loadingResults').hide();
                    break;
                }

                var resultMovieItem = $('<div class="resultMovieItem" data-movie-id="'+ annotationData[m].id +'" data-scene-index=""></div>'),
                    scenesPanel = $('<div class="scenesPanel"></div>'),
                    playerPanel = $('<div class="playerPanel"></div>');

                var timelinesContainer = $('<div class="timelinesContainer"></div>'),
                    timelineList = $('<div class="timelineList"></div>'),
                    userTimelineWrapper = $('<div class="userTimelineWrapper"></div>'),
                    timelineLabel = $('<div class="userLabel" style="color: #fff">'+ annotationData[m].title +' <span class="icon-right-open"></span> Filter Scenes: <span class="filteredSceneLabel"></span></div>'),
                    timelineClearButton = $('<button class="timelineClearButton button">Remove Scene Filters <span class="icon-erase"></span><button>'),
                    sceneTimeline = $('<div class="userTimeline"></div>');

                timelineClearButton.click(function() {
                    $(this).hide();
                    $(this).siblings('.filteredSceneLabel').text('');
                    var movieID = $(this).parents('.resultMovieItem').attr('data-movie-id');
                    renderResultMovie(resultCache[movieID], $(this).parents('.resultMovieItem'));
                });

                timelineLabel.append(timelineClearButton);
                userTimelineWrapper.append(timelineLabel, sceneTimeline);
                timelineList.append(userTimelineWrapper);
                timelinesContainer.append(timelineList);

                scenesPanel.append(timelinesContainer);

                resultMovieItem.append(scenesPanel, playerPanel);

                $('#resultPanel').append(resultMovieItem);

                resultCache[annotationData[m].id] = annotationData[m];

                renderResultMovie(annotationData[m], resultMovieItem);

            }
        }
        var totalResultsCount = (resultOptionsUnit == 'movie') ? totalMoviesCount : totalScenesCount;
        //console.log('TOTAL RESULTS: '+totalResultsCount);
        if (totalResultsCount > 1) {
            $('body').addClass('compareMode');
        } else {
            $('body').removeClass('compareMode');
        }

        $('#resultPanel').attr('data-result-count', totalResultsCount);

        $('#resultPanel').width( $(window).width() * totalResultsCount );
        if (totalResultsCount > 2) {
            updateResultMovieButtons();
        } else {
            $('.resultPanelNavButton').removeClass('active');
        }
        
    }
    
}

function renderResultList(annotationList) {
    renderAnnotationTimelines(annotationList, $('#resultPanel'), 'annotationType', resultOptionsSortBy, true);
}

function renderResultScene(sceneAnnotationData, resultMovieElement) {

    var movieID = resultMovieElement.data('movie-id'),
        fullMovieDuration = parseFloat(resultMovieElement.data('movie-duration')),
        sceneIndex = parseInt(resultMovieElement.data('scene-index'));
    
    //console.log('SCENE INDEX: '+sceneIndex);

    window['player'+ movieID + sceneIndex + 'sceneTitle'] = ' - Scene: <b>'+ sceneAnnotationData.title +'</b>';
    window['player'+ movieID + sceneIndex + 'currentSceneOffsetStart'] = sceneAnnotationData.startTime / 1000;
    window['player'+ movieID + sceneIndex + 'currentSceneOffsetEnd'] = sceneAnnotationData.endTime / 1000;

    var sceneLabel = window['player'+ movieID + 'movieTitle'] +'<br>Scene: <b>'+ sceneAnnotationData.title +'</b>';
    resultMovieElement.append('<div class="sceneLabel">'+ sceneLabel +'</div>');

    renderAnnotations(sceneAnnotationData.annotations, '.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .playerPanel', fullMovieDuration, window['player'+ movieID + sceneIndex + 'currentSceneOffsetStart'], window['player'+ movieID + sceneIndex + 'currentSceneOffsetEnd'], movieID, sceneIndex);

    $('#loadingResults').hide();


}

function renderResultMovie(movieAnnotationData, resultMovieElement) {

    var movieID = movieAnnotationData.id,
        fullMovieDuration = movieAnnotationData.duration / 1000;

    $('.resultMovieItem[data-movie-id="'+ movieID +'"] .scenesPanel .userTimeline').empty();

    var localMovieList = getLocalMovieListFromStorage();

    videoFileSources[movieID] = (localMovieList[movieID]) ? localMovieList[movieID] : movieAnnotationData.source;
    window['player'+ movieID + 'movieTitle'] = movieAnnotationData.title;

    var tsScenes = movieAnnotationData.scenes;

    //console.log('Number of Scenes for '+ movieAnnotationData.title +': '+ movieAnnotationData.scenes.length);

    var annotationsOfAllScenes = [],
        firstSceneOffsetStart = null,
        lastSceneOffsetEnd = null;

    // Add FrameTrail-specific annotation info
    for (var s=0; s<tsScenes.length; s++) {

        tsScenes[s]["frametrail:type"] = "Annotation";
        tsScenes[s]["body"] = {};
        tsScenes[s].body["frametrail:type"] = "text";
        tsScenes[s].body["frametrail:attributes"] = {};
        tsScenes[s].body["frametrail:attributes"].text = tsScenes[s].title;

        var sceneID = tsScenes[s].id;

        var dataStart = tsScenes[s].startTime / 1000,
            dataEnd = tsScenes[s].endTime / 1000,
            cleanStart = formatTime(dataStart),
            cleanEnd = formatTime(dataEnd),
            compareTimelineElement = $(
                '<div class="compareTimelineElement" '
            +   ' data-start="'
            +   dataStart
            +   '" data-end="'
            +   dataEnd
            +   '" data-scene-index="'+ s +'"'
            +   ' data-scene-id="'
            +   movieID +'-'+sceneID
            +   '" title="'
            +   'Select Scene'
            +   '">'
            +   '    <div class="previewWrapper">'
            +   '        <div class="resourceThumb" data-type="text">'
            +   '            <div class="resourceOverlay">'
            +   '                <div class="resourceIcon">'
            //+   '                    <span class="icon-doc-text"></span>'
            +   '                </div>'
            +   '            </div>'
            +   '            <div class="resourceTitle">Custom Text/HTML</div>'
            +   '            <div class="resourceTextPreview">'
            +   tsScenes[s].title
            +   '            </div>'
            +   '        </div>'
            +   '    </div>'
            +   '    <div class="compareTimelineElementTime">'
            +   '        <div class="compareTimeStart">'
            +   cleanStart
            +   '        </div>'
            +   '        <div class="compareTimeEnd">'
            +   cleanEnd
            +   '        </div>'
            +   '    </div>'
            +   '</div>'
        ),

        timeStart     = dataStart,
        timeEnd       = dataEnd;
        videoDuration   = fullMovieDuration,
        positionLeft    = 100 * (timeStart / videoDuration),
        width           = 100 * ((dataEnd - dataStart) / videoDuration);

        compareTimelineElement.css({
            left:  positionLeft + '%',
            width: width + '%'
        });

        compareTimelineElement.click(function() {
            //console.log($(this).data('scene-id'));

            var movieID = $(this).parents('.resultMovieItem').data('movie-id');

            if ($(this).hasClass('active')) {
                $(this).parents('.resultMovieItem').find('.timelineClearButton').hide();
                $(this).parents('.resultMovieItem').find('.filteredSceneLabel').text('');
                renderResultMovie(resultCache[movieID], $(this).parents('.resultMovieItem'));
            } else {

                var sceneLabel = $(this).find('.resourceTextPreview').text();

                $(this).parents('.resultMovieItem').find('.timelineClearButton').show();
                $(this).parents('.resultMovieItem').find('.filteredSceneLabel').text(sceneLabel);

                window['player'+ movieID + 'sceneTitle'] = ' - Scene: <b>'+ sceneLabel +'</b>';

                $('.resultMovieItem[data-movie-id="'+ movieID +'"] .scenesPanel .userTimeline .compareTimelineElement').removeClass('active');
                $(this).addClass('active');

                updateTrapezoids();

                window['player'+ movieID + 'currentSceneOffsetStart'] = parseFloat($(this).data('start'));
                window['player'+ movieID + 'currentSceneOffsetEnd'] = parseFloat($(this).data('end'));

                var sceneIndex = parseInt($(this).data('scene-index'));
                renderAnnotations(tsScenes[sceneIndex].annotations, '.resultMovieItem[data-movie-id="'+ movieID +'"] .playerPanel', fullMovieDuration, window['player'+ movieID + 'currentSceneOffsetStart'], window['player'+ movieID + 'currentSceneOffsetEnd'], movieID);
            }

        });

        $('.resultMovieItem[data-movie-id="'+ movieID +'"] .scenesPanel .userTimeline').append(compareTimelineElement);

        //console.log('Annotations for '+ window['player'+ movieID + 'movieTitle'] +' / Scene '+ (s+1) +': '+ tsScenes[s].annotations.length);

        for (var a=0; a<tsScenes[s].annotations.length; a++) {
            annotationsOfAllScenes.push(tsScenes[s].annotations[a]);
        }

        if (firstSceneOffsetStart === null) {
            firstSceneOffsetStart = dataStart;
        } else if (dataStart < firstSceneOffsetStart) {
            firstSceneOffsetStart = dataStart;
        }
        if (!lastSceneOffsetEnd) {
            lastSceneOffsetEnd = dataEnd;
        } else if (dataEnd > lastSceneOffsetEnd) {
            lastSceneOffsetEnd = dataEnd;
        }

    }

    //console.log(fullMovieDuration);

    renderAllAnnotations(annotationsOfAllScenes, fullMovieDuration, firstSceneOffsetStart, lastSceneOffsetEnd, movieID);

    $('#loadingResults').hide();


}

function renderAnnotations(sceneAnnotations, targetSelector, movieDuration, offsetStart, offsetEnd, movieID, sceneIndex) {

    $('#loadingResults').show();
    if (typeof sceneIndex == 'undefined') {
        sceneIndex = '';
    }

    /*
    console.log('MOVIE DURATION: '+movieDuration);
    console.log('OFFSET START: '+offsetStart);
    console.log('OFFSET END: '+offsetEnd);
    */

    $(targetSelector).empty();
    window['player'+ movieID + sceneIndex] = null;
    
    var tsAnnotations = sceneAnnotations;
    
    // Add FrameTrail-specific annotation info
    tsAnnotations = optimizeAnnotations(tsAnnotations);

    // INIT FRAMETRAIL

    $(targetSelector).attr('data-movie-id', movieID);
    $(targetSelector).attr('data-offset-start', offsetStart);
    $(targetSelector).attr('data-offset-end', offsetEnd);

    // rewrite vimeo urls
    var vimeoParts = /^(http\:\/\/|https\:\/\/|\/\/)?(www\.|player\.)?(vimeo\.com\/)(video\/)?([0-9]+)$/.exec(videoFileSources[movieID]);
    if (vimeoParts !== null) {
        if (vimeoParts[4] && vimeoParts[4] != 'video/') {
            videoFileSources[movieID] = "//player.vimeo.com/video/" + vimeoParts[4];
        } else {
            videoFileSources[movieID] = "//player.vimeo.com/video/" + vimeoParts[5];
        } 
    }

    // rewrite youtube urls
    var youtubeParts = null;
    var yt_list = [ /youtube\.com\/watch\?v=([^\&\?\/]+)/,
                    /youtube\.com\/embed\/([^\&\?\/]+)/,
                    /youtube\.com\/v\/([^\&\?\/]+)/,
                    /youtu\.be\/([^\&\?\/]+)/ ];
    for (var i in yt_list) {
        var res = yt_list[i].exec(videoFileSources[movieID]);
        if (res !== null) {
            youtubeParts = res;
            var timeCode = /t=([0-9]*)/.exec(videoFileSources[movieID]),
                tcString = (timeCode) ? '?start=' + timeCode[1] : '';
            videoFileSources[movieID] = '//www.youtube.com/embed/' + res[1] + tcString;
        }
    }

    var hypervideoContents = [];
    //console.log(window.loggedIn, videoFileSources[movieID]);
    if (!vimeoParts && !youtubeParts && (window.loggedIn || videoFileSources[movieID].indexOf('/public/') != -1 || !htaccessLogin)) {
        hypervideoContents.push({
            "@context": [
                "http://www.w3.org/ns/anno.jsonld",
                {
                    "frametrail": "http://frametrail.org/ns/"
                }
            ],
            "creator": {
                "nickname": "Advene",
                "type": "Agent",
                "id": "0"
            },
            "created": "Wed Jan 29 2020 08:00:26 GMT+0100 (British Summer Time)",
            "type": "Annotation",
            "frametrail:type": "Overlay",
            "frametrail:tags": [],
            "target": {
                "type": "Video",
                "source": videoFileSources[movieID],
                "selector": {
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "type": "FragmentSelector",
                    "value": "t="+ offsetStart +","+ offsetEnd +"&xywh=percent:0,0,100,100"
                }
            },
            "body": {
                "type": "Video",
                "frametrail:type": "video",
                "format": "video/mp4",
                "source": videoFileSources[movieID],
                "frametrail:name": window['player'+ movieID + 'movieTitle'] + window['player'+ movieID + sceneIndex + 'sceneTitle'],
                "selector": {
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "type": "FragmentSelector",
                    "value": "t="+ offsetStart +","+ offsetEnd +"&xywh=percent:0,0,100,100"
                }
            },
            "frametrail:events": {},
            "frametrail:attributes": {
                "autoPlay": true
            }
        });
    } else if (!vimeoParts && !youtubeParts && htaccessLogin) {
        hypervideoContents.push({
            "@context": [
                "http://www.w3.org/ns/anno.jsonld",
                {
                    "frametrail": "http://frametrail.org/ns/"
                }
            ],
            "creator": {
                "nickname": "Advene",
                "type": "Agent",
                "id": "0"
            },
            "created": "Wed Jan 29 2020 08:00:26 GMT+0100 (British Summer Time)",
            "type": "Annotation",
            "frametrail:type": "Overlay",
            "frametrail:tags": [],
            "target": {
                "type": "Video",
                "source": videoFileSources[movieID],
                "selector": {
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "type": "FragmentSelector",
                    "value": "t="+ offsetStart +","+ offsetEnd +"&xywh=percent:0,44,100,30"
                }
            },
            "body": {
                "type": "TextualBody",
                "frametrail:type": "text",
                "format": "text/html",
                "value": "",
                "frametrail:name": "Custom Text/HTML"
            },
            "frametrail:events": {},
            "frametrail:attributes": {
                "text": "&lt;span style=\"display: block; height: 100%; line-height: 100%; text-align: center; color: rgb(255, 252, 252); font-size: 30px;quot;;\"&gt;Please &lt;u style=\"cursor: pointer;\" onclick=\"login(true);\"&gt;login&lt;/u&gt; to see video contents.&lt;/span&gt;"
            }
        });
    }

    window['player'+ movieID + sceneIndex] = FrameTrail.init({
        target:             targetSelector,
        contentTargets:     {},
        contents:           [{
            hypervideo: {
                "meta": {
                    "name": window['player'+ movieID + 'movieTitle'] + window['player'+ movieID + sceneIndex + 'sceneTitle'],
                    "thumb": "",
                    "creator": "Advene",
                    "creatorId": "0",
                    "created": 1519713627469,
                    "lastchanged": 1521025330334
                },
                "config": {
                    "slidingMode": "adjust",
                    "slidingTrigger": "key",
                    "autohideControls": false,
                    "captionsVisible": false,
                    "clipTimeVisible": true,
                    "hidden": false,
                    "layoutArea": {
                        "areaTop": [
                            {
                                "type": "Timelines",
                                "contentSize": "large",
                                "name": "Annotations",
                                "description": "",
                                "cssClass": "",
                                "collectionFilter": {
                                    "tags": [],
                                    "types": [],
                                    "users": [],
                                    "text": ""
                                },
                                "onClickContentItem": "",
                                "html": "",
                                "transcriptSource": "",
                                "filterAspect": "annotationType",
                                "zoomControls": true
                            }
                        ],
                        "areaBottom": [],
                        "areaLeft": [],
                        "areaRight": [
                            {
                                "type": "CustomHTML",
                                "contentSize": "large",
                                "name": '<span title="Image 2 Text" class="icon-file-image"></span>',
                                "description": "",
                                "cssClass": "",
                                "collectionFilter": {
                                    "tags": [],
                                    "types": [],
                                    "users": [],
                                    "text": ""
                                },
                                "onClickContentItem": "",
                                "html": "",
                                "transcriptSource": ""
                            },
                            {
                                "type": "CustomHTML",
                                "contentSize": "large",
                                "name": '<span title="Transcript" class="icon-mic"></span>',
                                "description": "",
                                "cssClass": "",
                                "collectionFilter": {
                                    "tags": [],
                                    "types": [],
                                    "users": [],
                                    "text": ""
                                },
                                "onClickContentItem": "",
                                "html": "",
                                "transcriptSource": ""
                            }
                        ]
                    }
                },
                "clips": [
                    {
                        "resourceId": null,
                        "src": (vimeoParts || youtubeParts) ? videoFileSources[movieID] : null,
                        "duration": movieDuration,
                        "start": 0,
                        "end": 0,
                        "in": offsetStart,
                        "out": offsetEnd
                    }
                ],
                "globalEvents": {
                    "onReady": "",
                    "onPlay": "",
                    "onPause": "",
                    "onEnded": ""
                },
                "customCSS": "",
                "contents": hypervideoContents,
                "subtitles": []
            },
            annotations: tsAnnotations,
        }],
        startID: '0',
        resources: [{
            label: "Choose Resources",
            data: {},
            type: "frametrail"
        }],
        tagdefinitions: {
        },
        config: {
            "updateServiceURL": null,
            "autoUpdate": false,
            "defaultUserRole": "user",
            "captureUserTraces": false,
            "userTracesStartAction": "",
            "userTracesEndAction": "",
            "userNeedsConfirmation": true,
            "alwaysForceLogin": false,
            "allowCollaboration": false,
            "allowUploads": true,
            "theme": "bright",
            "defaultHypervideoHidden": false,
            "userColorCollection": [
                "597081",
                "339966",
                "16a09c",
                "cd4436",
                "0073a6",
                "8b5180",
                "999933",
                "CC3399",
                "7f8c8d",
                "ae764d",
                "cf910d",
                "b85e02"
            ]
        },
        users: {},
        language: 'en-US'
    });
    
    window['player'+ movieID + sceneIndex].on('ready', function() {

        console.log('Rendering Text Transcript');

        var videoElem = $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"]').find('video');
        videoElem.click(function(evt) {
            if (evt.currentTarget.nodeName == 'VIDEO') {
                if (evt.currentTarget.paused) {
                    window['player'+ movieID + sceneIndex].play();
                } else {
                    window['player'+ movieID + sceneIndex].pause();
                }
            }
        });

        $('.areaTopContainer .layoutAreaToggleCloseButton').off('click').on('click', function() {
            $('.resultMovieItem').find('.mainContainer').toggleClass('playerHidden');
        });

        var targetElem = $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .areaRightContainer .contentViewContainer:nth-child(2) .customhtmlContainer'),
            i2tTargetElem = $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .areaRightContainer .contentViewContainer:nth-child(1) .customhtmlContainer'),
            offsetStart = window['player'+ movieID + sceneIndex + 'currentSceneOffsetStart'],
            offsetEnd = window['player'+ movieID + sceneIndex + 'currentSceneOffsetEnd'];

        // Render Dialogue Text Annotations
        if (!dialogueTextCache[movieID] || dialogueTextCache[movieID].length == 0) {
            dialogueTextCache[movieID] = [];
            $.ajax({
                url : requestUrls['moviesearch'] + movieID +'/'+transcriptType,
                type: 'GET',
                success : function(result) {
                    
                    dialogueTextCache[movieID] = [];

                    var dtAnnotations = result['@graph'];

                    if (!dtAnnotations) {
                        $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
                        return;
                    }

                    for (var dta=0; dta<dtAnnotations.length; dta++) {
                        if (dtAnnotations[dta].body.annotationType && 
                            dtAnnotations[dta].body.annotationType.endsWith(transcriptType)) {
                            dialogueTextCache[movieID].push(dtAnnotations[dta]);
                        }
                    }
                    
                    renderDialogueTextTranscript(dialogueTextCache[movieID], targetElem, offsetStart, offsetEnd);

                    $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();

                },
                error : function(err) {
                    console.log(err);
                    $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
                }
            });
        } else {
            window.setTimeout(function() {
                $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
            }, 1000);
            renderDialogueTextTranscript(dialogueTextCache[movieID], targetElem, offsetStart, offsetEnd);
        }

        // Render Image2Text Annotations
        if (!image2TextCache[movieID] || image2TextCache[movieID].length == 0) {
            image2TextCache[movieID] = [];
            $.ajax({
                url : requestUrls['moviesearch'] + movieID +'/'+visualDescriptionType,
                type: 'GET',
                success : function(result) {
                    
                    image2TextCache[movieID] = [];

                    var i2tAnnotations = result['@graph'];

                    if (!i2tAnnotations) {
                        $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
                        return;
                    }

                    for (var i2ta=0; i2ta<i2tAnnotations.length; i2ta++) {
                        if (i2tAnnotations[i2ta].body.annotationType && 
                            i2tAnnotations[i2ta].body.annotationType.endsWith(visualDescriptionType)) {
                            image2TextCache[movieID].push(i2tAnnotations[i2ta]);
                        }
                    }
                    
                    renderImage2Text(image2TextCache[movieID], i2tTargetElem, offsetStart, offsetEnd);

                    $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();

                },
                error : function(err) {
                    console.log(err);
                    $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
                }
            });
        } else {
            window.setTimeout(function() {
                $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoContainer .workingIndicator').hide();
            }, 1000);
            renderImage2Text(image2TextCache[movieID], i2tTargetElem, offsetStart, offsetEnd);
        }
    });

    window['player'+ movieID + sceneIndex].on('pause', function() {
        var targetElem = $('.resultMovieItem[data-movie-id="'+ movieID +'"][data-scene-index="'+ sceneIndex +'"] .videoStartOverlay');
        targetElem.removeClass('inactive').show();
    });

    $('#loadingResults').hide();

}

function renderAllAnnotations(allAnnotations, resultDuration, offsetStart, offsetEnd, movieID) {

    window['player'+ movieID + 'sceneTitle'] = '';

    $('.resultMovieItem[data-movie-id="'+ movieID +'"] .scenesPanel .userTimeline .compareTimelineElement').removeClass('active');

    updateTrapezoids();
    //console.log(allAnnotations);

    window['player'+ movieID + 'currentSceneOffsetStart'] = offsetStart;
    window['player'+ movieID + 'currentSceneOffsetEnd'] = offsetEnd;

    renderAnnotations(allAnnotations, '.resultMovieItem[data-movie-id="'+ movieID +'"] .playerPanel', resultDuration, offsetStart, offsetEnd, movieID);

    
}

function optimizeAnnotations(annotationArray) {
    
    var annotationIDs = [],
        maxShotDuration = 0;
        
    for (var a=annotationArray.length-1; a>=0; a--) {

        if (annotationArray[a].id && annotationIDs.indexOf(annotationArray[a].id) != -1) {
            annotationArray.splice(a, 1);
            continue;
        } else if (annotationArray[a].id) {
            annotationIDs.push(annotationArray[a].id);
        }
        
        var bodyWithNumericInfo,
            bodyWithTextualInfo;
        if (Array.isArray(annotationArray[a].body)) {
            if (annotationArray[a].body[1].type == 'TextualBody') {
                bodyWithNumericInfo = annotationArray[a].body[0];
                bodyWithTextualInfo = annotationArray[a].body[1];
            } else {
                bodyWithNumericInfo = annotationArray[a].body[1];
                bodyWithTextualInfo = annotationArray[a].body[0];
            }
        } else {
            bodyWithNumericInfo = annotationArray[a].body;
            bodyWithTextualInfo = annotationArray[a].body;
        }

        var textualValue = bodyWithTextualInfo.value;

        annotationArray[a]["frametrail:type"] = "Annotation";

        if (typesToRenderAsSoundwave.indexOf(annotationArray[a]['advene:type']) != -1) {
            annotationArray[a]["frametrail:graphdata"] = textualValue;
            annotationArray[a]["frametrail:graphdatatype"] = 'soundwave';
        } else if (typesToRenderAsBarchart.indexOf(annotationArray[a]['advene:type']) != -1) {
            //TODO: remove replace when data is fixed
            annotationArray[a]["frametrail:graphdata"] = textualValue.replace(/  /g, ' ');
            annotationArray[a]["frametrail:graphdatatype"] = 'barchart';
        }

        //console.log(annotationArray[a]);

        if (Array.isArray(annotationArray[a].body)) {
            annotationArray[a].body[0]["frametrail:name"] = textualValue;
            annotationArray[a].body[0]["frametrail:type"] = "entity";
            annotationArray[a].body[0]["frametrail:attributes"] = {};
            annotationArray[a].body[0]["frametrail:attributes"].text = textualValue;

            if (annotationArray[a].id) {
                annotationArray[a].body[0].source = annotationArray[a].id;
            }

            /*
            if (annotationArray[a].body[1].annotationValue) {
                annotationArray[a].body[0].annotationValue = annotationArray[a].body[1].annotationValue;
            }
            */

            annotationArray[a].body[0].value = textualValue;
            annotationArray[a].body[0].type = bodyWithNumericInfo.type;
            annotationArray[a].body[0].annotationValueIndex = getAnnotationValueIndex(bodyWithNumericInfo.annotationType, bodyWithNumericInfo.annotationValue);
            annotationArray[a].body[0].annotationValue = (bodyWithNumericInfo.annotationValueSequence) ? bodyWithNumericInfo.annotationValueSequence : bodyWithNumericInfo.annotationValue;
            annotationArray[a].body[0].annotationNumericValue = (bodyWithNumericInfo.annotationNumericValueSequence) ? bodyWithNumericInfo.annotationNumericValueSequence : bodyWithNumericInfo.annotationNumericValue;
            annotationArray[a].body[0].maxNumericValue = getMaxNumericValue(annotationArray[a].body[0].annotationType);
            
            if (specialTypesToRenderAsBarchart.indexOf(annotationArray[a]['advene:type']) != -1 && textualValue > maxShotDuration) {
                maxShotDuration = textualValue;
            }
            if (specialTypesToRenderAsBarchart.indexOf(annotationArray[a]['advene:type']) != -1) {
                annotationArray[a].body[0].annotationNumericValue = textualValue;
            } else {
                annotationArray[a].body.maxNumericValue = getMaxNumericValue(annotationArray[a].body.annotationType);
            }
        } else {
            annotationArray[a].body["frametrail:name"] = textualValue;
            annotationArray[a].body["frametrail:type"] = "entity";
            annotationArray[a].body["frametrail:attributes"] = {};
            annotationArray[a].body["frametrail:attributes"].text = textualValue;

            if (annotationArray[a].id) {
                annotationArray[a].body.source = annotationArray[a].id;
            }

            annotationArray[a].body.value = textualValue;
            annotationArray[a].body.type = bodyWithNumericInfo.type;
            annotationArray[a].body.annotationValueIndex = getAnnotationValueIndex(annotationArray[a].body.annotationType, annotationArray[a].body.annotationValue);
            annotationArray[a].body.annotationValue = (bodyWithNumericInfo.annotationValueSequence) ? bodyWithNumericInfo.annotationValueSequence : bodyWithNumericInfo.annotationValue;
            annotationArray[a].body.annotationNumericValue = (bodyWithNumericInfo.annotationNumericValueSequence) ? bodyWithNumericInfo.annotationNumericValueSequence : bodyWithNumericInfo.annotationNumericValue;
            
            if (specialTypesToRenderAsBarchart.indexOf(annotationArray[a]['advene:type']) != -1 && textualValue > maxShotDuration) {
                maxShotDuration = textualValue;
            }
            if (specialTypesToRenderAsBarchart.indexOf(annotationArray[a]['advene:type']) != -1) {
                annotationArray[a].body.annotationNumericValue = textualValue;
            } else {
                annotationArray[a].body.maxNumericValue = getMaxNumericValue(annotationArray[a].body.annotationType);
            }
        }

    }

    for (var an=0; an<annotationArray.length; an++) {
        if (specialTypesToRenderAsBarchart.indexOf(annotationArray[an]['advene:type']) != -1) {
            if (Array.isArray(annotationArray[an].body)) {
                annotationArray[an].body[0].maxNumericValue = maxShotDuration;
            } else {
                annotationArray[an].body.maxNumericValue = maxShotDuration;
            }
        }
    }

    annotationArray.sort(function(a, b) {
        var timeStartA = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(a.target.selector.value)[1]),
            timeStartB = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(b.target.selector.value)[1]);

        if (timeStartA < timeStartB)
            return -1;
        if (timeStartA > timeStartB)
            return 1;
        return 0;
    });

    //console.log(annotationArray);

    return annotationArray;
}

function renderDialogueTextTranscript(dialogueTextAnnotations, targetElement, offsetStart, offsetEnd) {

    if (targetElement.length === 0) {
        return;
    }

    // INIT DialogueText Annotations as Transcript

    targetElement.empty();

    //console.log(dialogueTextAnnotations);

    function compare(a,b) {
        
        var timeStartA = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(a.target.selector.value)[1]),
            timeStartB = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(b.target.selector.value)[1]);

        if (timeStartA < timeStartB)
            return -1;
        if (timeStartA > timeStartB)
            return 1;
        return 0;

    }
    
    dialogueTextAnnotations.sort(compare);

    var highlightedSearchText = null;
    if (typeof getURLValues === 'function') {
        var urlValues = getURLValues();
        for (var i = 0; i < urlValues.requests.length; i++) {
            if (urlValues.requests[i].searchText && urlValues.requests[i].searchText.length != 0) {
                highlightedSearchText = urlValues.requests[i].searchText;
                break;
            }
        }
    }
    

    for (var dt=0; dt<dialogueTextAnnotations.length; dt++) {
        var timeStart = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(dialogueTextAnnotations[dt].target.selector.value)[1]),
            timeEnd = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(dialogueTextAnnotations[dt].target.selector.value)[2]);
        if (timeStart >= offsetStart && timeEnd <= offsetEnd) {
            var thisTextValue = dialogueTextAnnotations[dt].body.value;
            if (highlightedSearchText) {
                var re = new RegExp(highlightedSearchText, 'g');
                thisTextValue = dialogueTextAnnotations[dt].body.value.replace(re, '<em>'+ highlightedSearchText +'</em>');
            }
            targetElement[0].innerHTML += '<span data-start="'+ timeStart +'" data-end="'+ timeEnd +'" class="timebased">'+ thisTextValue +' </span>';

        }
    }
}

function renderImage2Text(image2TextAnnotations, targetElement, offsetStart, offsetEnd) {

    if (targetElement.length === 0) {
        return;
    }

    // INIT DialogueText Annotations as Transcript

    targetElement.empty();

    //console.log(dialogueTextAnnotations);

    function compare(a,b) {
        
        var timeStartA = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(a.target.selector.value)[1]),
            timeStartB = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(b.target.selector.value)[1]);

        if (timeStartA < timeStartB)
            return -1;
        if (timeStartA > timeStartB)
            return 1;
        return 0;

    }
    
    image2TextAnnotations.sort(compare);

    var highlightedSearchText = null;
    if (typeof getURLValues === 'function') {
        var urlValues = getURLValues();
        for (var i = 0; i < urlValues.requests.length; i++) {
            if (urlValues.requests[i].searchText && urlValues.requests[i].searchText.length != 0) {
                highlightedSearchText = urlValues.requests[i].searchText;
                break;
            }
        }
    }
    

    for (var i2t=0; i2t<image2TextAnnotations.length; i2t++) {
        var timeStart = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(image2TextAnnotations[i2t].target.selector.value)[1]),
            timeEnd = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(image2TextAnnotations[i2t].target.selector.value)[2]);
        if (timeStart >= offsetStart && timeEnd <= offsetEnd) {
            var thisTextValue = image2TextAnnotations[i2t].body.value;
            if (highlightedSearchText) {
                var re = new RegExp(highlightedSearchText, 'g');
                thisTextValue = image2TextAnnotations[i2t].body.value.replace(re, '<em>'+ highlightedSearchText +'</em>');
            }
            targetElement[0].innerHTML += '<span data-start="'+ timeStart +'" data-end="'+ timeEnd +'" class="timebased">'+ thisTextValue +' </span>';

        }
    }
}

function formatTime(aNumber) {

    var hours, minutes, seconds, hourValue;

    seconds     = Math.ceil(aNumber);
    hours       = Math.floor(seconds / (60 * 60));
    hours       = (hours >= 10) ? hours : '0' + hours;
    minutes     = Math.floor(seconds % (60*60) / 60);
    minutes     = (minutes >= 10) ? minutes : '0' + minutes;
    seconds     = Math.ceil(seconds % (60*60) % 60);
    seconds     = (seconds >= 10) ? seconds : '0' + seconds;

    if (hours >= 1) {
        hourValue = hours + ':';
    } else {
        hourValue = '';
    }

    return hourValue + minutes + ':' + seconds;

}

function getLabelFromURI(uri) {

    var label = uri;
    var slashFragments = uri.split('/');
    if (slashFragments.length > 1) {
        label = slashFragments[slashFragments.length-1];
    }
    var underscoreFragments = uri.split('_');
    if (underscoreFragments.length > 1) {
        underscoreFragments.splice(0, 1);
        label = underscoreFragments.join('-');
    }

    return label;

}

function getMaxNumericValue(annotationType) {
    
    var result = null;

    for (var i=0; i<annotationlevels.length; i++) {
        tmpresult = $.grep(annotationlevels[i].subElements, function(e){ return e.elementUri == annotationType; });
        if (tmpresult.length != 0) {
            result = tmpresult[0].maxNumericValue;
            break;
        }
    }
    return result;
}

function getAnnotationTypeValues(annotationType) {

    var annotationTypeURI = annotationType;
    var returnObj = {
        'description': '',
        'maxNumericValue': null,
        'values': []
    };

    for (var i=0; i<annotationlevels.length; i++) {
        tmpresult = $.grep(annotationlevels[i].subElements, function(e){ return e.elementUri == annotationTypeURI; });
        if (tmpresult.length != 0) {
            returnObj['description'] = tmpresult[0].elementDescription;
            returnObj['maxNumericValue'] = tmpresult[0].maxNumericValue;
            returnObj['values'] = tmpresult[0].subElements;
			// Fix name attribute for Frametrail.js
			returnObj['values'].forEach(v => v['name'] = v.elementName);
            break;
        }
    }

    //console.log(returnObj);

    return returnObj;

}

function getAnnotationValueIndex(annotationType, annotationValueURI) {

    var annotationValueIndex = null;

    var ontologyAnnotationValues = getAnnotationTypeValues(annotationType)['values'];

    //console.log(ontologyAnnotationValues, annotationValueURI);

    for (var i=0; i<ontologyAnnotationValues.length; i++) {
        if (ontologyAnnotationValues[i].elementUri == annotationValueURI) {
        	annotationValueIndex = i*1+1;
        	break;
        }
    }
    //console.log('RESULT for value '+ annotationValueURI +': ', annotationValueIndex);
    // CAREFUL, the index starts at 1, not at 0 (timeline colors)
    return annotationValueIndex;

}

function getSceneOffsets(movieID, sceneIndex) {
    var returnObj = {
        'startTime': 0,
        'endTime': null
    };

    var scene = resultCache[movieID].scenes[sceneIndex];

    returnObj.startTime = scene.startTime / 1000;
    returnObj.endTime = scene.endTime / 1000;

    return returnObj;
}

function updateTrapezoids() {
    clearRaphael();

    $('.compareTimelineElement.active').each(function() {
        var targetElem = $('.playerPanel[data-movie-id="' + $(this).parents('.resultMovieItem').attr('data-movie-id') +'"]');

        drawTrapezoid($(this), 'bottom', targetElem, 'top', {
            'fill': '#ffffff',
            'stroke-width': 0
        });
    });
}

function moveResultsLeft() {
    var amountPX = $('.resultMovieItem').eq(0).outerWidth() + 10;

    /*
    if ($('#resultPanel').offset().left >= 0) {
        $('#resultPanel').css('left', '0px');
        return;
    }
    */

    var currentOffsetMovie = parseInt($('#resultPanel').attr('data-offset-movie'));
    $('#resultPanel').attr('data-offset-movie', currentOffsetMovie-1);

    $('#resultPanel').css('left', '-' + amountPX*(currentOffsetMovie-1)  +'px');

    updateResultMovieButtons();
    updateActiveResultIndicator(currentOffsetMovie-1);
}

function moveResultsRight() {
    var amountPX = $('.resultMovieItem').eq(0).outerWidth() + 10;

    /*
    if (Math.abs($('#resultPanel').position().left) >= (parseInt($('#resultPanel').attr('data-movie-count'))-2) * amountPX) {
        return;
    }
    */
    var currentOffsetMovie = parseInt($('#resultPanel').attr('data-offset-movie'));
    $('#resultPanel').attr('data-offset-movie', currentOffsetMovie+1);

    $('#resultPanel').css('left', '-' + amountPX*(currentOffsetMovie+1)  +'px');

    if ($('.resultMovieItem').length < parseInt($('#resultPanel').attr('data-result-count')) 
        && $('.resultMovieItem').length == currentOffsetMovie + 2 ) {
        initFrameTrail($('.resultMovieItem').length);
        updateResultMovieButtons();
    }

    updateResultMovieButtons();
    updateActiveResultIndicator(currentOffsetMovie+1);
}

function updateMovieOffset() {
    var currentOffsetMovie = parseInt($('#resultPanel').attr('data-offset-movie'));
    var amountPX = ($('.resultMovieItem').eq(0).outerWidth() + 10) * currentOffsetMovie;

    $('#resultPanel').css('left', '-' + amountPX +'px');
}

function updateResultMovieButtons() {
    var currentOffsetMovie = parseInt($('#resultPanel').attr('data-offset-movie'));
    if (currentOffsetMovie > 0) {
        $('#resultPanelNavLeft').addClass('active');
    } else {
        $('#resultPanelNavLeft').removeClass('active');
    }
    if (currentOffsetMovie+2 >= parseInt($('#resultPanel').attr('data-result-count'))) {
        $('#resultPanelNavRight').removeClass('active');
    } else {
        $('#resultPanelNavRight').addClass('active');
    }
}

function renderResultIndicators(dataInput) {
    $('#resultIndicatorScrollContainer').empty();
    var totalCount = 0;
    if (resultOptionsView == 'movie') {
        if (resultOptionsUnit == 'scene') {
            for (var m=0; m<dataInput.length; m++) {
                var movie = dataInput[m];
                for (var s=0; s<movie.scenes.length; s++) {
                    totalCount++;
                    var label = movie.title +' - Scene: '+ movie.scenes[s].title,
                        resultIndicator = $('<div class="resultIndicatorItem" title="'+ label +'">'+ totalCount +'</div>')
                    $('#resultIndicatorScrollContainer').append(resultIndicator);
                }
            }

        } else {
            for (var m=0; m<dataInput.length; m++) {
                totalCount++;
                var label = dataInput[m].title,
                    resultIndicator = $('<div class="resultIndicatorItem" title="'+ label +'">'+ totalCount +'</div>')
                $('#resultIndicatorScrollContainer').append(resultIndicator);
            }
        }

        updateActiveResultIndicator(0);
        $('#resultCount #resultCountNumber').text(totalCount);
    }
}

function updateActiveResultIndicator(resultIndex) {
    $('#resultIndicatorContainer .resultIndicatorItem').removeClass('active');
    $('#resultIndicatorContainer .resultIndicatorItem').eq(resultIndex).addClass('active');
    $('#resultIndicatorContainer .resultIndicatorItem').eq(resultIndex+1).addClass('active');
    var resultIndexPosition = $('#resultIndicatorContainer .resultIndicatorItem').eq(resultIndex).position();
    if (resultIndexPosition) {
        if ( resultIndexPosition.left > $('#resultIndicatorContainer').width() / 2) {
            var newLeftPosition = - (resultIndexPosition.left - ($('#resultIndicatorContainer').width() / 2) + 19);
            $('#resultIndicatorScrollContainer').css('left', newLeftPosition +'px');
        } else {
            $('#resultIndicatorScrollContainer').css('left', 0 +'px');
        }
    }
}

window.loggedIn = false;
window.loginAttempts = 0;

function login(reloadAfterSuccess, initAfter) {
    if (window.loggedIn === true) {
        //console.log("LOGIN SUCCESS");
        $('#loginButton').hide();
        $('#logoutButton').show();
        window.loginAttempts++;
        if (initAfter) {
            initFrameTrail();
        }
        if (reloadAfterSuccess) {
            location.reload();
        }
        return true;
    } else {
        checkAuth(function(successful) {
            if (successful === true) {
                //console.log("LOGIN SUCCESS");
                window.loggedIn = true;
                $('#loginButton').hide();
                $('#logoutButton').show();
                window.loginAttempts++;
                if (initAfter) {
                    initFrameTrail();
                }
                if (reloadAfterSuccess) {
                    location.reload();
                }
                return true;
            } else {
                console.log("LOGIN FAIL");
                window.loginAttempts++;
                window.loggedIn = false;
                $('#logoutButton').hide();
                $('#loginButton').show();
                if (initAfter) {
                    initFrameTrail();
                }
                return false;
            }
        });
    }
}

function checkAuth(loginCallback) {

    var testURL = videoFilePrefix + 'private/';

    $.ajax({
        url : testURL +'test.json',
        type: 'GET',
        dataType: 'script',
        scriptAttrs: {
            'crossorigin': 'anonymous'
        },
        crossDomain: true
    }).done(function(result) {
        loginCallback(true);            
    }).fail(function(jqXHR, textStatus, errorThrown) {
        
        //console.log(textStatus, errorThrown);

        if (textStatus == 'error') {
            var isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
            if (isSafari) {
                var authIframe = $('<iframe style="position: absolute; top: 0px; left: 0px; width: 200px; height: 200px; background: #ffffff;" src="'+ testURL +'"></iframe>');
                authIframe.on('load', function(evt) {
                    //console.log('LOAD EVENT');
                    authIframe.remove();
                    $.ajax({
                        url : testURL +'test.json',
                        type: 'GET',
                        dataType: 'jsonp'
                    }).done(function(result) {
                        loginCallback(true);            
                    }).fail(function(jqXHR, textStatus) {
                        if (textStatus == 'error') {
                            loginCallback(false);
                        } else {
                            loginCallback(true);
                        }
                    });
                });
                $('body').append(authIframe);
            } else {
                loginCallback(false);
            }
            
        } else {
            loginCallback(true);
        }
    });
}

function openVideoSourceSettings() {

    var previewDialog = $('<div class="settingsDialog" title="Video Source Settings">'
                      +   '  <div class="settingsContainer">'
                      +   '    <div class="message warning active">You can either replace the Video Base Path or choose to change each video path manually.<br> Paths can be either to an MP4 file or a YouTube / Vimeo URL.<br><br>CAREFUL: Local paths (eg. URL: "file:///[..]/video.mp4") only work if the Annotation Explorer is also opened directly from the HTML file (URL: "file:///[..]/AnnotationExplorer.html").</div>'
                      +   '    <label for="currentVideoFileBasePath">Replace Video Base Path: </label>'
                      +   '    <input id="currentVideoFileBasePath"  style="width: 25%;"type="text" value="'+ videoFilePrefix +'">'
                      +   '    <label for="newVideoFileBasePath">with: </label>'
                      +   '    <input id="newVideoFileBasePath" placeholder="Enter new Video Base Path" style="width: 25%;"type="text" value="">'
                      +   '    <button class="button" type="button" id="replaceVideoFileBasePathButton">REPLACE ALL</button>'
                      +   '    <button class="button" type="button" id="resetToDefaultButton" style="float: right;">RESET ALL</button>'
                      +   '    <div>'
                      +   '      <input id="searchMovieBasePath" type="text" value="" placeholder="Search for Movies" style="width: 50%;">'
                      +   '      <span>Showing </span><span id="searchMovieResultCount"></span><span> / </span><span id="searchMovieTotalCount"></span>'
                      +   '    </div>'
                      +   '    <div id="editableMovieListContainer"></div>'
                      +   '  </div>'
                      +   '</div>');
    
    renderEditableMovieList(previewDialog.find('#editableMovieListContainer'), false);

    previewDialog.find('#replaceVideoFileBasePathButton').click(function() {
        var inputElems = $('#editableMovieListContainer').find('input[type="text"]'),
            fromBasePath = new RegExp(previewDialog.find('#currentVideoFileBasePath').val(), 'g'),
            toBasePath = previewDialog.find('#newVideoFileBasePath').val();

        inputElems.each(function() {
            var newValue = $(this).val().replace(fromBasePath, toBasePath);
            $(this).val(newValue);
        });
    });
    previewDialog.find('#resetToDefaultButton').click(function() {
        renderEditableMovieList(previewDialog.find('#editableMovieListContainer'), true);
    });

    var movieCount = (movies) ? movies.length : 0;
    previewDialog.find('#searchMovieTotalCount, #searchMovieResultCount').text(movieCount);
    previewDialog.find('#searchMovieBasePath').keyup(function() {
        var searchText = $(this).val(),
            inputElems = $('#editableMovieListContainer').find('input[type="text"]'),
            hitCount = 0;
        if (searchText.length > 1) {
            inputElems.each(function() {
                if (!$(this).prev('label').text().toLowerCase().includes(searchText.toLowerCase())) {
                    $(this).hide();
                    $(this).prev('label').hide();
                } else {
                    $(this).show();
                    $(this).prev('label').show();
                    hitCount++;
                }
            });
            $('#searchMovieResultCount').text(hitCount);
        } else {
            $('#editableMovieListContainer').find('input[type="text"], label').show();
            hitCount = inputElems.length;
            $('#searchMovieResultCount').text(hitCount);
        }
    });
    previewDialog.dialog({
        resizable: true,
        minWidth: 600,
        minHeight: 300,
        maxHeight: '80%',
        width: '90%',
        height: 'auto',
        modal: true,
        draggable: false,
        buttons: [
            {
                text: "Save Settings",
                icon: "icon-floppy",
                class: "active",
                click: function() {
                    saveModifiedMovieListToStorage();
                    $( this ).dialog( "close" );
                    window.setTimeout(function() {
                        initFrameTrail();
                    }, 1000);
                }
            },
            {
                text: "Cancel",
                click: function() {
                    $( this ).dialog( "close" );
                }
            }
        ],
        close: function() {
            $(this).dialog('close');
            $(this).remove();
        },
        closeOnEscape: true,
        open: function( event, ui ) {
            //
        }
    });
}

function renderEditableMovieList(targetElement, resetToDefault) {
    if (!movies) return;

    targetElement.empty();
    var localMovieList = getLocalMovieListFromStorage();

    for (var i = 0; i < movies.length; i++) {
        var savedPath = localMovieList[movies[i].id],
            currentPath = (resetToDefault || !savedPath) ? movies[i].playoutUrl : savedPath,
            labelElem = $('<label>'+ movies[i].title +'</label>'),
            inputElem = $('<input data-movie-id="'+ movies[i].id +'" type="text" value="'+ currentPath +'">');

        targetElement.append(labelElem, inputElem);
    }
}

function getLocalMovieListFromStorage() {
    var storedMovieList = localStorage.getItem('ada-movie-list'),
        movieList = {};
    
    if (storedMovieList) {
        movieList = JSON.parse(storedMovieList);
    }

    return movieList;
}

function saveModifiedMovieListToStorage() {
    var inputElems = $('#editableMovieListContainer').find('input[type="text"]'),
        movieList = {};

    inputElems.each(function() {
        var thisMovieID = $(this).data('movie-id');
        movieList[thisMovieID] = $(this).val();
    });
    localStorage.setItem('ada-movie-list', JSON.stringify(movieList));
}

function updateLocalMovieList() {
    var movieList = getLocalMovieListFromStorage();

    for (var movieID in videoFileSources) {
        movieList[movieID] = videoFileSources[movieID];
    }
    localStorage.setItem('ada-movie-list', JSON.stringify(movieList));
}

function openTimelineOrderSettings() {

    var timelineOrderDialog = $('<div class="settingsDialog" title="Prioritized Annotation Types / Order in Timeline">'
                            +   '  <div class="settingsContainer">'
                            +   '    <div class="message warning active" autofocus>The selected Annotation Types will always be displayed on top of the list of timelines.<br> Changes are applied immediately after closing this dialog.</div>'
                            +   '    <div>'
                            +   '      <input type="text" id="typeSearchAutocomplete" placeholder="Search Types" style="float: left;">'
                            +   '      <button class="button" type="button" id="resetToDefaultButton" style="float: right;">RESET PRIORITIZED TYPES</button>'
                            +   '      <div style="clear: both;"></div>'
                            +   '    </div>'
                            +   '    <ul id="prioritizedTypeList"></ul>'
                            +   '  </div>'
                            +   '</div>');
    
    var listOfAllTypes = getTypeListFromOntology();
    window.orderHasChanged = false;

    updatePrioritizedTypeList(timelineOrderDialog.find('#prioritizedTypeList'), prioritizedAnnotationTypes);

    timelineOrderDialog.find('#typeSearchAutocomplete').on( "keydown", function( event ) {                                                     // don't navigate away from the field on tab when selecting an item
        if ( event.keyCode === $.ui.keyCode.TAB && $( this ).autocomplete( "instance" ).menu.active ) {
            event.preventDefault();
        }
    }).autocomplete({
        minLength: 0,
        source: function( request, response ) {
            // delegate back to autocomplete, but extract the last term
            var filteredList = [];
            for (var i = 0; i < listOfAllTypes.length; i++) {
                if (prioritizedAnnotationTypes.filter(function(e) { return e.id === listOfAllTypes[i].id }).length == 0 && listOfAllTypes[i].label.toLowerCase().indexOf(request.term.toLowerCase()) != -1) {
                    filteredList.push(listOfAllTypes[i]);
                }
            }
            //console.log(filteredList);
            response(filteredList);
        },
        appendTo: timelineOrderDialog,
        focus: function() {
            // prevent value inserted on focus
            return false;
        },
        select: function(event, ui) {
            event.preventDefault();
            event.stopPropagation();
            $(event.target).blur();
            prioritizedAnnotationTypes.push(ui.item);
            updatePrioritizedTypeList(timelineOrderDialog.find('#prioritizedTypeList'), prioritizedAnnotationTypes); 
            updateURL();
            window.orderHasChanged = true;
        },
        change: function(event, ui) {
            //console.log(ui.item.value);
        }
    }).focus(function() {
        $(this).autocomplete("search", $(this).val());
    });

    timelineOrderDialog.find('#resetToDefaultButton').click(function() {
        prioritizedAnnotationTypes = [];
        updatePrioritizedTypeList(timelineOrderDialog.find('#prioritizedTypeList'), prioritizedAnnotationTypes);
        updateURL();
        window.orderHasChanged = true;
    });

    timelineOrderDialog.dialog({
        resizable: true,
        minWidth: 600,
        minHeight: 300,
        maxHeight: '80%',
        width: 800,
        height: 'auto',
        modal: true,
        draggable: false,
        buttons: [
            {
                text: "Close",
                icon: "icon-arrows-cw",
                class: "active",
                click: function() {
                    $( this ).dialog( "close" );
                }
            }
        ],
        close: function() {
            $(this).dialog('close');
            $(this).remove();
            if (window.orderHasChanged) {
                window.setTimeout(function() {
                    initFrameTrail();
                }, 600);
            }
        },
        closeOnEscape: false,
        open: function( event, ui ) {
            //
        }
    });
}

function updatePrioritizedTypeList(targetElement, prioritizedTypes) {
    targetElement.empty();
    
    var listOfAllTypes = getTypeListFromOntology();
    var filteredList = [];
    for (var i = 0; i < prioritizedTypes.length; i++) {
        var thisLabel = '';
        for (var a = 0; a < listOfAllTypes.length; a++) {
            if (listOfAllTypes[a].id == prioritizedTypes[i].id) {
                thisLabel = listOfAllTypes[a].label;
            }
        }
        filteredList.push({
            'value': prioritizedTypes[i].value,
            'id': prioritizedTypes[i].id,
            'label': thisLabel
        });
    }
    for (var i = 0; i < filteredList.length; i++) {
        var domElem = $('<li data-type-id="'+ filteredList[i].id +'"><span class="icon-cancel-circled" title="Remove Type"></span>'+ filteredList[i].label +'</li>');
        domElem.find('.icon-cancel-circled').click(function() {
            var thisIndex = $(this).parent().index();
            prioritizedAnnotationTypes.splice(thisIndex, 1);
            updatePrioritizedTypeList(targetElement, prioritizedAnnotationTypes);
            updateURL();
            window.orderHasChanged = true;
        });
        targetElement.append(domElem);
    }
    
    targetElement.sortable({
        placeholder: 'ui-state-highlight',
        axis: 'y',
        stop: function( event, ui ) {
            var sortedData = targetElement.sortable('toArray', { attribute: 'data-type-id' });
            //console.log(sortedData, listOfAllTypes);
            prioritizedAnnotationTypes = [];
            for (var i = 0; i < sortedData.length; i++) {
                var thisLabel = '';
                for (var a = 0; a < listOfAllTypes.length; a++) {
                    if (listOfAllTypes[a].id == sortedData[i]) {
                        thisLabel = listOfAllTypes[a].label;
                    }
                }
                prioritizedAnnotationTypes.push({
                    'value': sortedData[i],
                    'id': sortedData[i],
                    'label': thisLabel
                });
            }
            updateURL();
            window.orderHasChanged = true;
        }
    });
}

function getTypeListFromOntology() {
    var typeList = [];
    if (!annotationlevels) return typeList;

    for (var i=0; i<annotationlevels.length; i++) {
        for (var at = 0; at < annotationlevels[i].subElements.length; at++) {
            var thisType = annotationlevels[i].subElements[at];            
            typeList.push({
                'label': thisType.elementFullName,
                'id': thisType.sequentialNumber,
                'value': thisType.sequentialNumber
            });
        }
    }

    // sort alphabetically
    typeList.sort(function(a, b) {
        if (a.label < b.label)
            return -1;
        if (a.label > b.label)
            return 1;
        if (a.label == b.label) {
            return 0;
        }
    });
    return typeList;
}

function updateLocalTimelineOrder() {
    var timelineLabelList = getLocalTimelineOrderFromStorage();

    localStorage.setItem('frametrail-timeline-order', JSON.stringify(timelineLabelList));
}

function getAnnotationDataAsCSV(annotationData) {
    
    var csvString = 'StartTime\tEndTime\tValue\n';

    for (var i = 0; i < annotationData.length; i++) {
        if (annotationData[i].target.selector["advene:end"]) {
            var thisValue = (Array.isArray(annotationData[i].body)) ? annotationData[i].body[0].value : annotationData[i].body.value,
                startTime = annotationData[i].target.selector["advene:begin"],
                endTime = annotationData[i].target.selector["advene:end"];
            csvString += startTime +'\t' + endTime +'\t'+ thisValue +'\n';
        } else {
            var thisValue = (Array.isArray(annotationData[i].body)) ? annotationData[i].body[0].value : annotationData[i].body.value,
                startTime = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(annotationData[i].target.selector.value)[1]),
                endTime = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(annotationData[i].target.selector.value)[2]);
            csvString += startTime +'\t' + endTime +'\t'+ thisValue +'\n';
        }
    }

    var csvBlob = new Blob([csvString], { type: "text/csv;charset=utf-8" });

    // return ObjectURL
    return URL.createObjectURL(csvBlob);
}

/******************************
* Re-used FrameTrail Methods
******************************/

function renderAnnotationTimelines(annotationCollection, targetElement, filterAspect, sortBy, zoomControls) {
    
    targetElement.empty();

    var ftBody = $('<div class="frametrail-body" data-frametrail-theme="bright" style="margin-top: 10px; height: calc(100% - 20px);"></div>'),
        ftMainContainer = $('<div class="mainContainer"></div>'),
        timelineList = $('<div class="timelineList" data-zoom-level="1"></div>');

    //console.log(annotationCollection);
    var collectedAnnotationsPerAspect = [];

    for (var anno in annotationCollection) {
        
        //console.log(annotationCollection[anno]);

        var currentAspectID;
        if (Array.isArray(annotationCollection[anno].body)) {
            currentAspectID = annotationCollection[anno].body[0][filterAspect];
        } else {
            currentAspectID = annotationCollection[anno].body[filterAspect];
        }

        var sceneIdentifier = (resultOptionsUnit == 'scene') ? annotationCollection[anno].sceneIndex : '';
        
        if (!collectedAnnotationsPerAspect[currentAspectID + annotationCollection[anno].movieTitle + sceneIdentifier]) {
            
            var startOffset,
                endOffset;
            if (resultOptionsUnit == 'scene') {
                var sceneOffsets = getSceneOffsets(annotationCollection[anno].movieID, annotationCollection[anno].sceneIndex);
                startOffset = sceneOffsets.startTime;
                endOffset = sceneOffsets.endTime;
            } else {
                startOffset = 0;
                endOffset = annotationCollection[anno].movieDuration
            }

            collectedAnnotationsPerAspect[currentAspectID + annotationCollection[anno].movieTitle + sceneIdentifier] = {
                'userID': annotationCollection[anno].creator,
                'label': annotationCollection[anno]["advene:type_title"],
                'color' : (annotationCollection[anno]["advene:type_color"]) ? annotationCollection[anno]["advene:type_color"] : '444444',
                'annotations': [],
                'movieDuration': annotationCollection[anno].movieDuration,
                'movieTitle': annotationCollection[anno].movieTitle,
                'sceneTitle': (resultOptionsUnit == 'scene') ? annotationCollection[anno].sceneTitle : '',
                'sceneIndex': sceneIdentifier,
                'startOffset': startOffset,
                'endOffset': endOffset
            };
            
            collectedAnnotationsPerAspect[currentAspectID + annotationCollection[anno].movieTitle + sceneIdentifier]['annotationTypeValues'] = getAnnotationTypeValues(currentAspectID);
        }

        collectedAnnotationsPerAspect[currentAspectID + annotationCollection[anno].movieTitle + sceneIdentifier]['annotations'].push(annotationCollection[anno]);
    }

    collectedAnnotationsPerAspectData = [];
    for (var obj in collectedAnnotationsPerAspect) {
        collectedAnnotationsPerAspectData.push(collectedAnnotationsPerAspect[obj]);
    }

    if (sortBy) {
        collectedAnnotationsPerAspectData.sort(function(a, b) {
            if (a[sortBy] < b[sortBy])
                return -1;
            if (a[sortBy] > b[sortBy])
                return 1;
            if (a[sortBy] == b[sortBy]) {
                if (a.label < b.label) 
                    return -1;
                if (a.label > b.label)
                    return 1;
                return 0;
            }
        });
    }

    var customTimelineOrder = null;
    if (prioritizedAnnotationTypes && prioritizedAnnotationTypes.length != 0) {
        customTimelineOrder = [];
        for (var i = 0; i < prioritizedAnnotationTypes.length; i++) {
            customTimelineOrder.push(prioritizedAnnotationTypes[i].label);
        }
    }
    if (customTimelineOrder) {
        collectedAnnotationsPerAspectData.sort(function(a, b) {
            if (customTimelineOrder.indexOf(b.label) != -1) {
                return 1;
            } else {
                return -1;
            }
        });
        collectedAnnotationsPerAspectData.sort(function(a, b) {
            if (customTimelineOrder.indexOf(a.label) != -1 && customTimelineOrder.indexOf(b.label) != -1) {
                if (customTimelineOrder.indexOf(a.label) < customTimelineOrder.indexOf(b.label)) {
                    return -1;
                }
                if (customTimelineOrder.indexOf(a.label) > customTimelineOrder.indexOf(b.label)) {
                    return 1;
                }
            } else {
                return 0;
            }
        });
    }

    //console.log('ASPECTS: ', collectedAnnotationsPerAspectData);

    var timelineZoomWrapper = $('<div class="timelineZoomWrapper"></div>'),
        timelineZoomScroller = $('<div class="timelineZoomScroller"></div>');

    timelineZoomScroller.appendTo(timelineZoomWrapper);
    
    if (zoomControls) {
        
        timelineZoomWrapper.on('scroll', function(evt) {
            var scrollLeftVal = $(this).scrollLeft();
            $(this).find('.userLabel').css('left', scrollLeftVal + 'px');
        });

        var zoomControlsWrapper = $('<div class="zoomControlsWrapper"></div>'),
            zoomMinus = $('<button class="button zoomMinus"><span class="icon-zoom-out"></span></button>'),
            zoomPlus = $('<button class="button zoomPlus"><span class="icon-zoom-in"></span></button>'),
            sortByType = $('<button class="button" data-sort-by="label"><span class="icon-tag"></span>Sort By Type<span class="icon-sort-name-up"></span></button>'),
            sortByMovie = $('<button class="button" data-sort-by="movieTitle"><span class="icon-hypervideo"></span>Sort By Movie<span class="icon-sort-name-up"></span></button>');
        
        zoomMinus.click(function() {
            var currentZoomLevel = parseFloat($(this).parent().parent().attr('data-zoom-level'));
            zoomTimelines(timelineZoomWrapper, currentZoomLevel-0.5 );
        });
        zoomPlus.click(function() {
            var currentZoomLevel = parseFloat($(this).parent().parent().attr('data-zoom-level'));
            zoomTimelines(timelineZoomWrapper, currentZoomLevel+0.5);
            //console.log(currentZoomLevel);
        });
        sortByType.click(function() {
            resultOptionsSortBy = 'label';
            renderAnnotationTimelines(annotationCollection, $('#resultPanel'), 'annotationType', 'label', true);
        });
        sortByMovie.click(function() {
            resultOptionsSortBy = 'movieTitle';
            renderAnnotationTimelines(annotationCollection, $('#resultPanel'), 'annotationType', 'movieTitle', true);
        });
        zoomControlsWrapper.append(zoomPlus, zoomMinus, sortByType, sortByMovie);

        zoomControlsWrapper.find('button[data-sort-by="'+ resultOptionsSortBy +'"]').addClass('active');

        timelineList.append(zoomControlsWrapper);

        
        var timelineProgress = $('<div class="timelineProgressWrapper"><div class="timelineProgressRange"></div></div>');
        timelineZoomScroller.append(timelineProgress);

        var leftStart;
    }

    for (var i=0; i<collectedAnnotationsPerAspectData.length; i++) {

        var aspectLabel =  collectedAnnotationsPerAspectData[i].label,
            aspectColor = collectedAnnotationsPerAspectData[i].color,
            aspectDuration = collectedAnnotationsPerAspectData[i].movieDuration,
            aspectValues = collectedAnnotationsPerAspectData[i].annotationTypeValues;

        //console.log(aspectLabel);
        var iconClass = (filterAspect == 'creatorId') ? 'icon-user' : 'icon-tag';
        var movieLabel = (collectedAnnotationsPerAspectData[i].sceneTitle.length != 0) ? collectedAnnotationsPerAspectData[i].movieTitle +' - Scene: '+ collectedAnnotationsPerAspectData[i].sceneTitle : collectedAnnotationsPerAspectData[i].movieTitle;

        var exportFileName = collectedAnnotationsPerAspectData[i].movieTitle+ '_'+ collectedAnnotationsPerAspectData[i].sceneTitle +'_'+ aspectLabel;
        exportFileName = exportFileName.replace(/[\s:]/g, '-').replace(/[|&;:$%@<>()+,]/g, '').replace(/__/g, '_').replace(/--/g, '-');
        var exportData = getAnnotationDataAsCSV(collectedAnnotationsPerAspectData[i].annotations);
        var exportButtonString = '<a class="exportTimelineDataButton" title="Export Annotation Data as CSV File" download="'+ exportFileName +'.csv" href="'+ exportData +'">Export Data</a>';
        
        var userTimelineWrapper = $(    '<div class="userTimelineWrapper">'
                                    +   '    <div class="userLabel" style="color: '+ aspectColor +'">'
                                    +   '        <span class="'+ iconClass +'"></span>'
                                    +   '        <span>'+ aspectLabel +' - <span class="icon-hypervideo"></span> '+ movieLabel +'</span>'+exportButtonString
                                    +   '        <div class="timelineValues"></div>'
                                    +   '    </div>'
                                    +   '    <div class="userTimeline"></div>'
                                    +   '</div>'),
            legendContainer = userTimelineWrapper.find('.timelineValues'),
            userTimeline = userTimelineWrapper.find('.userTimeline');

        if (aspectValues && aspectValues.values.length != 0) {
            
            var evolvingValuesLegendElement = $('<span class="timelineLegendLabel" data-origin-type="ao:EvolvingValuesAnnotationType" title="Evolving Values" style="color: #777;">TO</span>'),
                contrastingValuesLegendElement = $('<span class="timelineLegendLabel" data-origin-type="ao:ContrastingValuesAnnotationType" title="Contrasting Values" style="color: #777;">VS</span>');

            evolvingValuesLegendElement.hover(function(evt) {
                var thisOriginType = $(this).attr('data-origin-type');
                $(this).siblings().css('opacity', 0.2);
                $(this).css('opacity', 1);
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type]), [data-timeline-color]').removeClass('opaque');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type])').addClass('transparentBackground');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement[data-origin-type]:not([data-origin-type="'+ thisOriginType +'"]), [data-origin-type]:not([data-origin-type="'+ thisOriginType +'"])').addClass('opaque');
            }, function(evt) {
                $(this).siblings().css('opacity', '');
                $(this).css('opacity', '');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type]), [data-origin-type]').removeClass('opaque');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type])').removeClass('transparentBackground');
            });
            contrastingValuesLegendElement.hover(function(evt) {
                var thisOriginType = $(this).attr('data-origin-type');
                $(this).siblings().css('opacity', 0.2);
                $(this).css('opacity', 1);
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type]), [data-timeline-color]').removeClass('opaque');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type])').addClass('transparentBackground');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement[data-origin-type]:not([data-origin-type="'+ thisOriginType +'"]), [data-origin-type]:not([data-origin-type="'+ thisOriginType +'"])').addClass('opaque');
            }, function(evt) {
                $(this).siblings().css('opacity', '');
                $(this).css('opacity', '');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type]), [data-origin-type]').removeClass('opaque');
                $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-origin-type])').removeClass('transparentBackground');
            });
            legendContainer.append(evolvingValuesLegendElement, contrastingValuesLegendElement);

            for (var v=0; v<aspectValues.values.length; v++) {
                var numericRatio = aspectValues.values[v].elementNumericValue / aspectValues.maxNumericValue,
                    relativeHeight = 100 * (numericRatio),
                    timelineColor = (aspectValues.maxNumericValue) ? Math.round(numericRatio * 12) : v*1 + 1;
                if (aspectLabel.indexOf('Colour Range') != -1 || aspectLabel.indexOf('Colour Accent') != -1) {
                    var valueLegendElement = $('<span class="timelineLegendLabel" data-numeric-value="'+ aspectValues.values[v].elementNumericValue +'" data-timeline-color="'+ aspectValues.values[v].elementName +'" style="color: '+ aspectValues.values[v].elementName +';">'+ aspectValues.values[v].elementName +'</span>');
                } else {
                    var valueLegendElement = $('<span class="timelineLegendLabel" data-numeric-value="'+ aspectValues.values[v].elementNumericValue +'" data-timeline-color="'+ timelineColor +'">'+ aspectValues.values[v].elementName +'</span>');
                }
                valueLegendElement.hover(function(evt) {
                    var thisColor = $(this).attr('data-timeline-color');
                    $(this).siblings().css('opacity', 0.2);
                    $(this).css('opacity', 1);
                    $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-timeline-color]), [data-timeline-color]').removeClass('opaque');
                    $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-timeline-color])').addClass('transparentBackground');
                    $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement[data-timeline-color]:not([data-timeline-color="'+ thisColor +'"]), [data-timeline-color]:not([data-timeline-color="'+ thisColor +'"])').addClass('opaque');
                }, function(evt) {
                    $(this).siblings().css('opacity', '');
                    $(this).css('opacity', '');
                    $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-timeline-color]), [data-timeline-color]').removeClass('opaque');
                    $(this).parents('.userLabel').next('.userTimeline').find('.compareTimelineElement:not([data-timeline-color])').removeClass('transparentBackground');
                });
                legendContainer.append(valueLegendElement);
            }
        }

        var firstAnnotation = (collectedAnnotationsPerAspectData[i].annotations[0]) ? collectedAnnotationsPerAspectData[i].annotations[0] : null,
            timelineMaxValue = 1;

        //console.log(firstAnnotation);
        var firstAnnotationMaxNumericValue;
        if (Array.isArray(firstAnnotation.body)) {
            if (firstAnnotation.body[0].type == 'TextualBody') {
                firstAnnotationMaxNumericValue = firstAnnotation.body[1].maxNumericValue;
            } else {
                firstAnnotationMaxNumericValue = firstAnnotation.body[0].maxNumericValue;
            }
        } else {
            firstAnnotationMaxNumericValue = firstAnnotation.body.maxNumericValue;
        }

        if (firstAnnotation && firstAnnotation.body && firstAnnotationMaxNumericValue && specialTypesToRenderAsBarchart.indexOf(firstAnnotation['advene:type']) == -1) {
            timelineMaxValue = firstAnnotationMaxNumericValue;
            for (var gl=1; gl<timelineMaxValue; gl++) {
                var bottomValue = 100 * (gl / timelineMaxValue);
                userTimeline.append('<div class="horizontalGridLine" style="bottom: '+ bottomValue +'%;"></div>');
            }
        }

        userTimelineWrapper.attr('data-timeline-max-value', timelineMaxValue);
        userTimelineWrapper.attr('data-type-label', aspectLabel);
        
        var overlapLeft = false,
            overlapRight = false;

        for (var idx in collectedAnnotationsPerAspectData[i].annotations) {
            var compareTimelineItem = renderCompareTimelineItem(collectedAnnotationsPerAspectData[i].annotations[idx], collectedAnnotationsPerAspectData[i].startOffset, collectedAnnotationsPerAspectData[i].endOffset);
            if (compareTimelineItem.hasClass('overlapLeft')) overlapLeft = true;
            if (compareTimelineItem.hasClass('overlapRight')) overlapRight = true;
            //TODO: Fix conflict between aspectColor and value (in same element)
            compareTimelineItem.css('background-color', aspectColor);
            if (compareTimelineItem.attr('data-origin-type') == 'ao:EvolvingValuesAnnotationType') {
                compareTimelineItem.find('path').attr('fill', aspectColor);
            }

            userTimeline.append(compareTimelineItem);
        }

        if (overlapLeft) {
            userTimeline.append('<div class="overlapIndicatorLeft"><span class="icon-angle-double-left"></span></div>');
        }
        if (overlapRight) {
            userTimeline.append('<div class="overlapIndicatorRight"><span class="icon-angle-double-right"></span></div>');
        }

        timelineZoomScroller.append(userTimelineWrapper);

    }

    timelineList.append(timelineZoomWrapper);
    ftMainContainer.append(timelineList);
    ftBody.append(ftMainContainer);
    targetElement.append(ftBody);

    makeTimelinesSortable(timelineZoomScroller);

}

function makeTimelinesSortable(containerElement) {
    containerElement.sortable({
        placeholder: 'ui-state-highlight',
        items: '> .userTimelineWrapper',
        axis: 'y'
    });
}

function zoomTimelines(targetElement, zoomLevel) {

    if (zoomLevel < 1) {
        zoomLevel = 1;
    }

    var zoomPercent = zoomLevel*100,
        currentLeft = parseInt(targetElement.eq(0).scrollLeft()),
        currentWidth = targetElement.find('.timelineZoomScroller').eq(0).width(),
        focusPoint = 2,
        positionLeft = (targetElement.width() * (zoomLevel/focusPoint)) + (targetElement.width()/focusPoint),
        currentOffset = currentLeft + (currentLeft + currentWidth - targetElement.width());

    /*
    console.log('Left: '+ currentLeft);
    console.log('Right: '+ (currentLeft + currentWidth - targetElement.width()));
    console.log('Offset: '+ currentOffset / zoomLevel);
    */

    positionLeft = (positionLeft + (currentOffset / zoomLevel));

    if (positionLeft > 0 || zoomLevel == 1) {
        positionLeft = 0;
    }

    if ( (targetElement.width()*zoomLevel) - targetElement.width() + currentOffset < 0  ) {
        positionLeft = (targetElement.width()*zoomLevel) - targetElement.width();
    }

    targetElement.find('.timelineZoomScroller').css({
        width: zoomPercent + '%'
    });

    //TODO: FIX POSITIONING
    //targetElement.scrollLeft(positionLeft);

    targetElement.parent().attr('data-zoom-level', zoomLevel);

}

function renderCompareTimelineItem(annotationData, startOffset, endOffset) {
    
    var startTime = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(annotationData.target.selector.value)[1]),
        endTime = parseFloat(/t=(\d+\.?\d*),(\d+\.?\d*)/g.exec(annotationData.target.selector.value)[2]),
        cleanStart = formatTime(startTime),
        cleanEnd = formatTime(endTime),
        compareTimelineElement = $(
            '<div class="compareTimelineElement" '
        +   ' data-type="entity" data-uri="'
        +   annotationData.id
        +   '" data-start="'
        +   startTime
        +   '" data-end="'
        +   endTime
        +   '">'
        +   '    <div class="previewWrapper"></div>'
        +   '    <div class="compareTimelineElementTime">'
        +   '        <div class="compareTimeStart">'
        +   cleanStart
        +   '        </div>'
        +   '        <div class="compareTimeEnd">'
        +   cleanEnd
        +   '        </div>'
        +   '    </div>'
        +   '</div>'
    ),

        timeStart       = startTime - startOffset,
        timeEnd         = endTime - endOffset,
        videoDuration   = endOffset - startOffset,
        positionLeft    = 100 * (timeStart / videoDuration),
        width           = 100 * ((endTime - startTime) / videoDuration);

    if (endTime > endOffset) {
        compareTimelineElement.addClass('overlapRight');
    }
    if (startOffset > startTime) {
        compareTimelineElement.addClass('overlapLeft');
    }

    var numericValue = false,
        maxNumericValue = '5',
        annotationValueIndex = false,
        dataType = false,
        annotationType = '';
    
    if (Array.isArray(annotationData.body)) {
        if (annotationData.body[0].type == 'TextualBody') {
            numericValue = annotationData.body[1].annotationNumericValue;
            maxNumericValue = annotationData.body[1].maxNumericValue;
            annotationValueIndex = annotationData.body[1].annotationValueIndex;
            dataType = annotationData.body[1].type;
            annotationType = annotationData.body[1].annotationType;
        } else {
            numericValue = annotationData.body[0].annotationNumericValue;
            maxNumericValue = annotationData.body[0].maxNumericValue;
            annotationValueIndex = annotationData.body[0].annotationValueIndex;
            dataType = annotationData.body[0].type;
            annotationType = annotationData.body[0].annotationType;
        }
    } else {
        numericValue = annotationData.body.annotationNumericValue;
        maxNumericValue = annotationData.body.maxNumericValue;
        annotationValueIndex = annotationData.body.annotationValueIndex;
        dataType = annotationData.body.type;
        annotationType = annotationData.body.annotationType;
    }

    //console.log('annotationValueIndex: ' + annotationValueIndex);

    //console.log('ORIGIN BODY:', annotationData.body);
    //console.log('MaxNumericValue:', maxNumericValue);
    //console.log('NumericValue:', numericValue);

    if (numericValue || annotationValueIndex) {
        if (numericValue && Array.isArray(numericValue)) {
            compareTimelineElement.attr({
                'data-origin-type': dataType,
                'data-numeric-value': numericValue,
                'data-numeric-min': '0',
                'data-numeric-max': maxNumericValue
            });
            if (dataType == 'ao:EvolvingValuesAnnotationType') {
                var svgElem = renderEvolvingValues(numericValue, maxNumericValue);
                compareTimelineElement.append(svgElem);
                //jQuery SVG Hack
                compareTimelineElement.html(compareTimelineElement.html());
            } else if (dataType == 'ao:ContrastingValuesAnnotationType') {
                var highestNumericValue = Math.max.apply(null, numericValue),
                    relativeHeight = 100 * (highestNumericValue / maxNumericValue)
                var contrastingElems = renderContrastingValues(numericValue, maxNumericValue, highestNumericValue);
                compareTimelineElement.append(contrastingElems);
                compareTimelineElement.css('height', relativeHeight + '%');
            }
        } else {
            var numericRatio = numericValue / maxNumericValue,
                relativeHeight = 100 * (numericRatio),
                timelineColor = (numericValue) ? Math.round(numericRatio * 12) : annotationValueIndex;

            compareTimelineElement.attr({
                'data-origin-type': dataType,
                'data-numeric-value': numericValue,
                'data-numeric-min': '0',
                'data-numeric-max': maxNumericValue,
            });
            if (annotationType.indexOf('ColourAccent') != -1) {
                var tmpText = '';
                if (Array.isArray(annotationData.body)) {
                    if (annotationData.body[0]["frametrail:attributes"] && annotationData.body[0]["frametrail:attributes"].text) {
                        tmpText = annotationData.body[0]["frametrail:attributes"].text;
                    }
                } else {
                    if (annotationData.body["frametrail:attributes"] && annotationData.body["frametrail:attributes"].text) {
                        tmpText = annotationData.body["frametrail:attributes"].text;
                    }
                }
                compareTimelineElement.attr('data-timeline-color', tmpText);
                compareTimelineElement.append('<div class="barchartFraction" style="height: 100%; top: 0%; background-color: '+ tmpText +';" data-timeline-color="'+ tmpText +'"></div>');
            } else {
                compareTimelineElement.attr('data-timeline-color', timelineColor);
            }
            compareTimelineElement.css('height', relativeHeight + '%');
            //compareTimelineElement.css('opacity', numericRatio);
        }
    } else {
        
        var multipleAnnotationValues = null,
            annotationType = null;
        if (Array.isArray(annotationData.body)) {
            if (annotationData.body[0].annotationValue) {
                multipleAnnotationValues  = annotationData.body[0].annotationValue;
                annotationType  = annotationData.body[0].annotationType;
            } else if (annotationData.body[1].annotationValue) {
                multipleAnnotationValues  = annotationData.body[1].annotationValue;
                annotationType  = annotationData.body[1].annotationType;
            }
        } else {
            multipleAnnotationValues  = annotationData.body.annotationValue;
            annotationType  = annotationData.body.annotationType;
        }
        if (multipleAnnotationValues) {
            //console.log('HERE', multipleAnnotationValues, annotationData);
            compareTimelineElement.attr('data-origin-type', dataType);
            var multipleElems = renderMultipleValues(annotationType, multipleAnnotationValues);
            compareTimelineElement.append(multipleElems);
        }
    }

    if (Array.isArray(annotationData.body)) {
        if (annotationData.body[0]["frametrail:attributes"] && annotationData.body[0]["frametrail:attributes"].text) {
            var decoded_string = $("<div/>").html(annotationData.body[0]["frametrail:attributes"].text).text();
            compareTimelineElement.attr('title', decoded_string);
        }
    } else {
        if (annotationData.body["frametrail:attributes"] && annotationData.body["frametrail:attributes"].text) {
            var decoded_string = $("<div/>").html(annotationData.body["frametrail:attributes"].text).text();
            compareTimelineElement.attr('title', decoded_string);
        }
    }
    
    compareTimelineElement.css({
        left:  positionLeft + '%',
        width: width + '%'
    });

    compareTimelineElement.removeClass('previewPositionLeft previewPositionRight');

    if (positionLeft < 10 && width < 10) {
        compareTimelineElement.addClass('previewPositionLeft');
    } else if (positionLeft > 90) {
        compareTimelineElement.addClass('previewPositionRight');
    }

    var thumbElement = $('<div class="resourceThumb" data-type="entity">'
                       + '    <div class="resourceOverlay">'
                       + '        <div class="resourceIcon"><span class="icon-tag-1"></span></div>'
                       + '    </div>'
                       + '    <div class="resourceTitle"></div>'
                       + '    <div class="resourceTextPreview">'+ decoded_string +'</div>'
                       + '</div>');

    var previewButton = $('<div class="resourcePreviewButton"><span class="icon-eye"></span></div>').click(function(evt) {
        openPreview( $(this).parent() );
        evt.stopPropagation();
        evt.preventDefault();
    });
    thumbElement.append(previewButton);

    compareTimelineElement.find('.previewWrapper').append(thumbElement);

    if (annotationData["frametrail:graphdata"]) {
        if (annotationData["frametrail:graphdatatype"] == 'soundwave') {
            compareTimelineElement.append(renderSoundwave(annotationData["frametrail:graphdata"]));
        } else if (annotationData["frametrail:graphdatatype"] == 'barchart') {
            compareTimelineElement.append(renderBarchart(annotationData["frametrail:graphdata"]));
        }
    }

    return compareTimelineElement;

}

function renderEvolvingValues(values, maxValue) {
    var svg = $('<svg width="100%" height="100%" viewBox="0 0 100 100" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg"></svg>'),
        stepWidth = 100 / (values.length - 1),
        invertedValues = [];

    for (var i = 0; i < values.length; i++) {
        var numericRatio = values[i] / maxValue,
            relativeValue = 100 * (numericRatio);
        invertedValues.push(100 - relativeValue);
    }
    
    var path = "M 0 " + invertedValues[0];

    for (var v=1; v<invertedValues.length; v++) {
        path += " L " + stepWidth * v + " " + invertedValues[v];
    }

    path += " L 100 100 L 0 100 Z";
    svg.append('<path d="' + path + '"></path>');

    var timelineColor = Math.round(numericRatio * 12);
    svg.attr('data-timeline-color', timelineColor);

    return svg;
}

function renderContrastingValues(values, maxValue, highestValue) {
    
    var barchartFractions = '';

    values.sort(function(a, b) {
        return b - a;
    });

    for (var v=0; v<values.length; v++) {
        var numericRatio = (values[v] / maxValue),
            timelineColor = Math.round(numericRatio * 12),
            fractionPercentage = 100 * (values[v] / highestValue);
        barchartFractions += '<div class="barchartFraction" style="height: '+ fractionPercentage +'%" data-timeline-color="'+ timelineColor +'"></div>';
    }

    return $(barchartFractions);
}

function renderMultipleValues(annotationType, values) {
    
    var barchartFractions = '',
        heightPercentage = 100 / values.length;

    for (var v=0; v<values.length; v++) {
        var valueIndex = getAnnotationValueIndex(annotationType, values[v]),
            numericRatio = (v / values.length),
            timelineColor = valueIndex,
            fractionPercentage = 100 * (v / values.length);
        if (annotationType.indexOf('ColourRange') != -1 || annotationType.indexOf('ColourAccent') != -1) {
            var thisLabel = getLabelFromURI(values[v]);
            barchartFractions += '<div class="barchartFraction" style="height: '+ heightPercentage +'%; top: '+ fractionPercentage +'%; background-color: '+ thisLabel +';" data-timeline-color="'+ thisLabel +'"></div>';
        } else {
            barchartFractions += '<div class="barchartFraction" style="height: '+ heightPercentage +'%; top: '+ fractionPercentage +'%" data-timeline-color="'+ timelineColor +'"></div>';
        }
    }

    return $(barchartFractions);
}

function renderSoundwave(soundwaveDataString) {
                    
    var graphDataElem = $('<div class="graphDataContainer"></div>');

    var width = 8000,
        height = 60,
        data = soundwaveDataString.split(" "),
        node = d3.select(graphDataElem[0]).append("canvas")
            .attr("width", width)
            .attr("height", height);

        var y = d3.scaleLinear().range([0, height]);
        var max_val = 100;
        y.domain([0, max_val]);
        var x = d3.scaleLinear().domain([0, data.length]);
        var bar_width = width / data.length;

        var chart = node;
        var context = chart.node().getContext("2d");

        data.forEach(function(d, i) {
            var thisY = height - Math.abs(y(d)/2) - height/2 + 2;
            var thisX = i * bar_width;
            var thisHeight = Math.abs(y(d)) + 2;
            
            context.beginPath();
            context.rect(thisX, thisY, bar_width, thisHeight);
            context.fillStyle="#333333";
            context.fill();
            context.closePath();
        });

    return graphDataElem;
}

function renderBarchart(barchartDataString) {
                    
    var graphDataElem = $('<div class="graphDataContainer"></div>');

    var width = 30000,
        height = 60,
        data = barchartDataString.split(" ");

    var i,
        j,
        dataChunk,
        chunkSize = 2000,
        numberOfChunks = Math.ceil(data.length / chunkSize),
        canvasPercentWidth = 100 / numberOfChunks,
        //finalChunkSize = Math.round(data.length / numberOfChunks);
        finalChunkSize = data.length / numberOfChunks;

    for (i=0,j=data.length; i<j; i+=finalChunkSize) {
        
        dataChunk = data.slice(i,i+finalChunkSize);
        
        var node = d3.select(graphDataElem[0]).append("canvas")
            .attr("width", width)
            .attr("height", height)
            .attr("style", "width: "+ canvasPercentWidth +"%;");

        var y = d3.scaleLinear().range([0, height]);
        var max_val = 100;
        y.domain([0, max_val]);
        var x = d3.scaleLinear().domain([0, dataChunk.length]);
        var bar_width = width / dataChunk.length;

        var chart = node;
        var context = chart.node().getContext("2d");

        dataChunk.forEach(function(d, c) {
            var thisY = height - Math.abs(y(d));
            var thisX = c * bar_width;
            var thisHeight = Math.abs(y(d));
            
            context.beginPath();
            context.rect(thisX, thisY, bar_width, thisHeight);
            context.fillStyle="#333333";
            context.fill();
            context.closePath();
        });
    }    

    return graphDataElem;
}

function openPreview(elementOrigin) {

    var animationDiv = elementOrigin.clone(),
        originOffset = elementOrigin.offset(),
        finalTop = ($(window).height()/2) - 240,
        finalLeft = ($(window).width()/2) - 440,
        entityURI = elementOrigin.parents('.compareTimelineElement').attr('data-uri'),
        entityTitle = elementOrigin.find('.resourceTextPreview').text();

    animationDiv.addClass('resourceAnimationDiv').css({
        position: 'absolute',
        top: originOffset.top + 'px',
        left: originOffset.left + 'px',
        width: elementOrigin.width(),
        height: elementOrigin.height(),
        zIndex: 101
    }).appendTo('body');

    animationDiv.animate({
        top: finalTop + 'px',
        left: finalLeft + 'px',
        width: 880 + 'px',
        height: 480 + 'px',
    }, 300, function() {
        var previewDialog   = $('<div class="resourcePreviewDialog" title="'+ entityTitle +'">'
                            +   '    <div class="resourceDetail" data-type="entity">'
                            +   '        <div class="resourceDetail" data-type="entity"></div>'
                            +   '    </div>'
                            +   '</div>');
        
        var downloadButton = '<a class="button" href="'+ entityURI +'" target="_blank">Open in new tab</a>';

        var iFrame = $(
                '<iframe frameborder="0" webkitAllowFullScreen mozallowfullscreen allowFullScreen src="'
            +   entityURI
            +   '" sandbox="allow-same-origin allow-scripts allow-popups allow-forms">'
            +    '</iframe>'
        ).bind('error, message', function() {
            return true;
        });

        previewDialog.find('.resourceDetail').append(iFrame);
        previewDialog.find('.resourceDetail').append('<div class="resourceOptions"><div class="resourceButtons">'+ downloadButton +'</div></div>');

        previewDialog.dialog({
            resizable: false,
            width: 880,
            height: 480,
            modal: true,
            draggable: false,
            close: function() {
                
                $(this).dialog('close');
                $(this).remove();
                animationDiv.animate({
                    top: originOffset.top + 'px',
                    left: originOffset.left + 'px',
                    width: elementOrigin.width(),
                    height: elementOrigin.height()
                }, 300, function() {
                    $('.resourceAnimationDiv').remove();
                });
            },
            closeOnEscape: true,
            open: function( event, ui ) {
                $('.ui-widget-overlay').click(function() {
                    previewDialog.dialog('close');
                });

            }
        });
    });

}