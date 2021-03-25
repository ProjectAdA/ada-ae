function downloadResult() {
    var filename = 'AdA-Export-'+ getUUID() +'.html',
        resultData = JSON.stringify(getCurrentAnnotationData()),
        customCSSFiles = ['css/color-definitions.css', 'css/custom-frametrail.css', 'css/custom-frametrail-timelines.css'],
        customCSS = '',
        activeSearchFacetsDOM = $('#activeSearchFacets').clone(),
        FrameTrailMethodsJS = '',
        originURL = exportBaseURL + location.search;

        activeSearchFacetsDOM.children().each(function() {
            $(this).children('button').remove();
        });

        var activeSearchFacetsHTML = activeSearchFacetsDOM.html();

        $.each(document.styleSheets, function(sheetIndex, sheet) {
            if (sheet.ownerNode.attributes.href && customCSSFiles.indexOf(sheet.ownerNode.attributes.href.nodeValue) != -1) {
                $.each(sheet.cssRules || sheet.rules, function(ruleIndex, rule) {
                    customCSS += rule.cssText+'\n';
                });
            }
        });
        //console.log(customCSS);

        $.ajax({
            type: 'GET',
            url: 'annotations-frametrail-methods.js',
            dataType: 'text',
            success: function( data ) {
                FrameTrailMethodsJS = data;

                fileHTML =  '<!DOCTYPE html>\n'
                        +   '<html lang="en">\n'
                        +   '  <head>\n'
                        +   '    <title>Semantic Video Annotation Explorer - AdA Project - Export</title>\n'
                        +   '    <meta charset="UTF-8">\n'
                        +   '    <meta name="viewport" content="width=device-width, initial-scale=1">\n'
                        +   '    <link rel="stylesheet" href="https://frametrail.org/build/FrameTrail.min.css">\n'
                        +   '    <style type="text/css">'+ customCSS +'</style>\n'
                        +   '    <style type="text/css">\n'
                        +   '      .container {display: flex; min-height: 100vh; flex-direction: column;}\n'
                        +   '      #activeSearchFacets {top: 0px; width: calc(100% - 165px) !important; padding: 8px 157px 8px 9px;}\n'
                        +   '      #content {position: relative; display: flex; flex: 1; top: 0px; height: 100%; width: 100%; box-sizing: border-box;}\n'
                        +   '      #resultPanelActions {position: absolute; top: 0px; right: 0px;}\n'
                        +   '      #resultPanelActions > * {float: left; margin: 0 8px 0 0; line-height: 40px; padding: 0 6px;}\n'
                        +   '      .query_label {display: inline-block; border: 2px solid #2f323a; border-radius: 11px; padding: 1px 8px 1px 6px; margin: 0 4px 4px 0; line-height: 19px;}'
                        +   '      #resultPanelContainer {position: absolute; top: 0px; left: 10px;}\n'
                        +   '      .resultMovieItem { width: calc(100vw - 20px) !important; }\n'
                        +   '      body.compareMode .resultMovieItem { width: calc(50vw - 40px) !important; }\n'
                        +   '      .timelineList .timelineValues { width: calc(100vw - 30px) !important; }\n'
                        +   '      body.compareMode .timelineList .timelineValues { width: calc(50vw - 50px) !important; }\n'
                        +   '    </style>\n'
                        +   '  </head>\n'
                        +   '  <body data-timeline-charts="'+ $('body').attr('data-timeline-charts') +'" data-timeline-opacity="'+ $('body').attr('data-timeline-opacity') +'" data-timeline-background="'+ $('body').attr('data-timeline-background') +'" data-timeline-color="'+ $('body').attr('data-timeline-color') +'" data-timeline-horizontal-grid="'+ $('body').attr('data-timeline-horizontal-grid') +'" data-timeline-keys="'+ $('body').attr('data-timeline-keys') +'">\n'
                        +   '    '
                        +   '    <div class="container">'
                        +   '      <div id="activeSearchFacets">'+ activeSearchFacetsHTML +'</div>\n'
                        +   '      <div id="resultPanelActions"><a id="viewLiveResultBtn" href="'+ originURL +'" target="_blank">View Live Result</a><div id="settingsButton" class="button" onclick="openVideoSourceSettings()" title="Video Source Settings"><span class="icon-cog"></span></div></div>'
                        +   '      <div id="content">'
                        +   '        <div id="resultPanelContainer">\n'
                        +   '          <div id="resultPanel"></div>\n'
                        +   '          <div id="resultIndicatorContainer">\n'
                        +   '            <div id="resultIndicatorScrollContainer"></div>\n'
                        +   '          </div>\n'
                        +   '          <div id="resultCount">Total Items: <span id="resultCountNumber">0</span></div>\n'
                        +   '        </div>\n'
                        +   '        <div class="resultPanelNavButton" id="resultPanelNavLeft"><span class="icon-left-open-big"></span></div>\n'
                        +   '        <div class="resultPanelNavButton" id="resultPanelNavRight"><span class="icon-right-open-big"></span></div>\n'
                        +   '        <div id="loadingResults" class="loadingScreen active">\n'
                        +   '            <div class="workingSpinner"></div>\n'
                        +   '        </div>\n';
                        +   '      </div>\n';
                        +   '    </div>\n';
                fileHTML += '    <script type="text/javascript" src="https://frametrail.org/build/FrameTrail.min.js"></script>\n';
                fileHTML += '    <script type="text/javascript">'+ FrameTrailMethodsJS +'</script>\n';
                fileHTML += '    <script type="text/javascript">\n'
                        +   '      var resultData = '+ JSON.stringify(resultData) +';\n'
                        +   '      var movies = '+ JSON.stringify(movies) +';\n'
                        +   '      var annotationlevels = '+ JSON.stringify(annotationlevels) +';\n'
                        +   '      var apiUrl = "'+ apiUrl +'";\n'
                        +   '      var videoFilePrefix = "http://ada.filmontology.org/videos/";\n'
                        +   '      var dialogueTextCache = '+ JSON.stringify(dialogueTextCache) +';\n'
                        +   '      var image2TextCache = '+ JSON.stringify(image2TextCache) +';\n'
                        +   '      var resultCache = {};\n'
                        +   '      var videoFileSources = '+ JSON.stringify(videoFileSources) +';\n'
                        +   '      var resultOptionsView = "'+ resultOptionsView +'";\n'
                        +   '      var resultOptionsUnit = "'+ resultOptionsUnit +'";\n'
                        +   '      var resultOptionsSortBy = "'+ resultOptionsSortBy +'";\n'
                        +   '      var typesToRenderAsSoundwave = "'+ typesToRenderAsSoundwave +'";\n'
                        +   '      var typesToRenderAsBarchart = "'+ typesToRenderAsBarchart +'";\n'
                        +   '      var specialTypesToRenderAsBarchart = "'+ specialTypesToRenderAsBarchart +'";\n'
                        +   '      var prioritizedAnnotationTypes = '+ JSON.stringify(prioritizedAnnotationTypes) +';\n'
                        +   '      $(document).ready(function() { var isPrivateVideo = false; updateLocalMovieList(); for (var key in videoFileSources) {  if (videoFileSources[key].indexOf("/private/") != -1) { isPrivateVideo = true; break; } } if (isPrivateVideo) { login(false, true) } else { initFrameTrail(); } });\n'
                        +   '      function getCurrentAnnotationData() { return JSON.parse(resultData); };\n'
                        +   '    </script>\n';
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
        });
}

function getUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}