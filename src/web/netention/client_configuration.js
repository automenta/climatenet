var configuration = window.configuration =
{
    "autoLoginDefaultProfile": true,
    "initialView": "wall",
    "avatarMenuDisplayInitially": true,
    "focusEditDisplayStartup": true,
    "favicon": null,
    "loginLogo": "/theme/login-logo.png",
    "defaultAvatarIcon": "/theme/default-avatar.jpg",
    "wikiStartPage": "Life",
    "showPlanOnSelfPage": true,
    "defaultTheme": "bootswatch.cerulean",
    "maxStartupObjects": 8192,
    "defaultMapMode2D": true,
    "mapDefaultLocation": [40.44, -80],
    "ontologySearchIntervalMS": 1500,
    "viewlockDefault": false,
    "viewUpdateTime": [[150, 50, 250], [0, 0, 100]],
    "views": ["us", "map", "browse", "wiki", "graph", "share", "forum", "main", "trends", "time", "notebook", "wall", "slides"],
    "newUserProperties": [],
    "shareTags": ["Offer", "Sell", "Lend", "Rent", "Swap", "GiveAway", "Need"],
    "shareCategories": ["Food", "Service", "Volunteer", "Shelter", "Tools", "Health", "Transport", "Animal"],
    "knowTags": ["Learn", "Teach", "Do"],
    "defaultScope": 7
};
configuration.enableAnonymous = true;
configuration.siteName = 'Curiosume';
configuration.siteDescription = 'http://netention.org';
configuration.requireIdentity = true;
configuration.connection = 'static';
configuration.webrtc = false;
configuration.wikiProxy = '/phproxy/proxy.php?url=';

/* Netention Client Configuration */

//all hardcoded stuff here is temporary until icons are specified by ontology
var defaultIcons = {
    'default': '/icon/rrze/status/true.png',

    'unknown': '/icon/unknown.png',

    'Favorite': '/icon/rrze/observe.png',
    'Goal': '/icon/rrze/error.png',
    'Earthquake': '/icon/quake.png',
    'NuclearFacility': '/icon/nuclear.png',
    'Human': '/icon/rrze/crown.png',
    'User': '/icon/rrze/ID-clip.png',
    'Message': '/icon/rrze/at.png',
    'Decision.Agree': '/icon/loomio/agree.png',
    'Decision.Disagree': '/icon/loomio/disagree.png',
    'Decision.Block': '/icon/loomio/block.png',
    'Decision.Abstain': '/icon/loomio/abstain.png',
    'Event': '/icon/rrze/dial-in.png',
    'Report': '/icon/rrze/add.png',
    'Similar': '/icon/approx_equal.png',
    'Emotion.happy': '/icon/emoticon/happy.svg',
    'Emotion.sad': '/icon/emoticon/sad.svg',
    'Emotion.angry': '/icon/emoticon/angry.svg',
    'Emotion.surprised': '/icon/emoticon/surprised.svg',
    'Tweet': '/icon/twitter.png',
    'GoalCentroid': '/icon/rrze/workflow-cycle.png',
    'Item': '/icon/rrze/database.png',
    'Volunteer': '/icon/sparkrelief/cat-volunteer-28.png',
    'Shelter': '/icon/sparkrelief/cat-shelter-28.png',
    'Food': '/icon/sparkrelief/cat-food-28.png',
    'Tools': '/icon/sparkrelief/cat-goods-28.png',
    'Health': '/icon/sparkrelief/cat-medical-28.png',
    'Transport': '/icon/sparkrelief/cat-transportation-28.png',
    'Service': '/icon/sparkrelief/cat-services-28.png',
    'Animal': '/icon/sparkrelief/cat-pets-28.png'
};


var themes = {
    //BASIC
    "_bright": 'Bright',
    //"ui-darkness":'Dark',
    "_dark": 'Dark',

    //ORIGINAL
    "_matrix-green": 'Matrix (green)',
    "_matrix-red": 'Matrix (red)',
    "_matrix-blue": 'Matrix (blue)',
    "_space": 'Space',
    "_cybernetic": 'Cybernetic',
    "_notebook": 'Notebook',
    "_rainforest": 'Rainforest',
    "_metamaps": 'MetaMaps',
    "_scroogle": 'Google',
    "_n$a": 'N$A',

    //from Bootswatch
    "_bootswatch.cerulean": "Cerulean",
    "_bootswatch.darkly": "Darkly",
    "_bootswatch.cyborg": "Cyborg",
    "_bootswatch.readable": "Readable",
    "_bootswatch.simplex": "Simplex",
    "_bootswatch.spacelab": "SpaceLab"

};

var tagAlias = {};