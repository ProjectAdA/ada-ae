/******************************
* Generic Config Parameters
******************************/
const apiUrl='/api';

const requestUrls = {
	moviesearch: apiUrl + '/jsonld/getAnnotations/',
	textsearch: apiUrl + '/jsonld/textSearch/',
	valuesearch: apiUrl + '/jsonld/valueSearch/'
}

// used to link back to the "live" version from an exported file
const exportBaseURL = 'http://ada.filmontology.org/explorer/';

// should htaccess auth methods be used? 
// (incl. login button & auth dialog on initialization)
const htaccessLogin = false;

// Corpus Types and Auto Types
const CorpusTypes = 'AnnotationType/DialogueEmotion,DialogueIntensity,MusicMood,SoundGestureDynamics,Volume,BodyLanguageEmotion,BodyLanguageIntensity,CameraAngle,CameraMovementDirection,RecordingPlaybackSpeed,ColourRange,DominantMovementDirection,FieldSize,ImageBrightness,ImageIntrinsicMovement,DialogueText,FoundFootage,MontageFigureMacro,Shot,ShotDuration,ImageContent,Setting,Scene';
const AutoTypes = 'AnnotationType/shotdetection,motion_dynamics,deepspeech,aspect_ratio,im2txt,densecap,soundenvelope,yamnet_music,yamnet_speech,yamnet_silence,yamnet_other';

// annotation types to render as soundwave, based on "advene:type" property
const typesToRenderAsSoundwave = [
    'Volume',
    'soundenvelope'
];

// annotation types to render as barchart, based on "advene:type" property
const typesToRenderAsBarchart = [
    'motion_dynamics'
];

// annotation types to render as barchart, regardless of a scale
// will detect the max value (of the currently rendered annotations) automatically
// an render bar height relatively
const specialTypesToRenderAsBarchart = [
    'ShotDuration'
];

// video file prefix (can change while using video source settings window)
let videoFilePrefix = 'http://ada.filmontology.org/videos/';

// searchtab.js config parameters
let picture_preview = true;
let picture_preview_width = 200;
let max_value_fields = 5;
let predefined_values_selectable = false;

// After making a selection in the Fancytree, wait x ms before reloading Frametrail
let tree_selection_timeout = 2000;
