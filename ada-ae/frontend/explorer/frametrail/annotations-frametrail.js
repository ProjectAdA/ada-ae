var dialogueTextCache = {},
    image2TextCache = {},
    resultCache = {},
    videoFileSources = {},
    resultOptionsView = 'movie',
    resultOptionsUnit = 'movie',
    resultOptionsSortBy = 'movieTitle',
    prioritizedAnnotationTypes = [],
    totalMoviesCount = 0,
    totalScenesCount = 0,
    ontologyDate = '';

$(document).ready(function() {
    
    
    if (htaccessLogin) {
        $('header #loginContainer').addClass('active');
        login();
    }
    
    $('#sidebarToggleButton').click(function() {
        $('body').toggleClass('sidebarClosed');
        setTimeout(function() {
            $(window).resize();
        }, 400);
    });

    $('#optionsToggleBarchart').click(function() {
        if (this.checked) {
            $('body').attr('data-timeline-charts', 'true');
        } else {
            $('body').attr('data-timeline-charts', 'false');
        }
        updateURL();
    });

    $('#optionsToggleBackground').click(function() {
        if (this.checked) {
            $('body').attr('data-timeline-background', 'true');
        } else {
            $('body').attr('data-timeline-background', 'false');
        }
        updateURL();
    });

    $('#optionsToggleHorizontalGrid').click(function() {
        if (this.checked) {
            $('body').attr('data-timeline-horizontal-grid', 'true');
        } else {
            $('body').attr('data-timeline-horizontal-grid', 'false');
        }
        updateURL();
    });

    $('#optionsToggleColor').click(function() {
        if (this.checked) {
            $('body').attr('data-timeline-color', 'true');
        } else {
            $('body').attr('data-timeline-color', 'false');
        }
        updateURL();
    });

    $('#optionsToggleKeys').click(function() {
        if (this.checked) {
            $('body').attr('data-timeline-keys', 'true');
        } else {
            $('body').attr('data-timeline-keys', 'false');
        }
        updateURL();
    });

    $('#resultPanelActions .button[data-result-view]').click(function() {
        if (resultOptionsView != $(this).attr('data-result-view')) {
            $('#resultPanelActions .button[data-result-view]').removeClass('active');
            $(this).addClass('active');
            resultOptionsView = $(this).attr('data-result-view');
            updateURL();
            initFrameTrail();
        }
    });

    $('#resultPanelActions .button[data-result-unit]').click(function() {
        if (resultOptionsUnit != $(this).attr('data-result-unit')) {
            $('#resultPanelActions .button[data-result-unit]').removeClass('active');
            $(this).addClass('active');
            resultOptionsUnit = $(this).attr('data-result-unit');
            updateURL();
            initFrameTrail();
        }
    });

    $('.button.downloadResult').click(function() {
    	downloadResult();
    });

    $('#resultPanelActions .button#videoSourceSettingsButton').click(function() {
        openVideoSourceSettings();
    });

    $('#resultPanelActions .button#timelineOrderSettingsButton').click(function() {
        openTimelineOrderSettings();
    });

});