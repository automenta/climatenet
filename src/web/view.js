"use strict";

/** netention view, abstract interface */
class NView {

    constructor(name, icon) {
        this.name = name;
        this.icon = icon;
    }

    /** start the view in the target container element */
    start(v, cb) {

    }

    /** called before the container is destroyed */
    stop(v) {

    }

}

/* loads a static HTML file via AJAX URL to the view */
class HTMLView extends NView {

    constructor(name, icon, url) {
        super(name, icon);
        this.url = url;
    }

    start(v, cb) {

        //v.append('<iframe src="" style="width:100%;height100%">');

        $.get(this.url)
            .done(function (h) {
                v.html(h);
            })
            .fail(function (err) {
                v.html(err);
            });

    }

    stop(v) {

    }
}

class FeedView extends NView {

    constructor(app) {
        super("Feeds", "cubes");
        this.app = app;
    }

    start(v, app, cb) {

        setInterval(function() {

            //var vv = $('<div class="ui items" style="background-color: white"></div>');

            v.html('');
            v.addClass('ui items');

            /*
             <div class="ui items">
                 <div class="item">
                     <div class="ui tiny image">
                         <img src="/images/wireframe/image.png">
                     </div>
                     <div class="middle aligned content">
                         <a class="header">12 Years a Slave</a>
                     </div>
                 </div>
             */
            var count = 0;
            for (var c in app.focus) {

                var ii = $('<div class="item" style="background-color: white"></div>');

                var jj = $('<div class="middle aligned content"></div>');
                jj.append('<a class="header">' + c + '</a>');

                try {
                    var chan = app.data(c);

                    jj.append('<pre>' + JSON.stringify(chan, null, 4) + '</pre>');
                }
                catch (e) {
                    jj.append('<i>No data</i>');
                }

                ii.append('<div class="ui tiny image"><img src="icon/play.png"/></div>', jj);
                v.append(ii);

                count++;
            }

            if (!count)
                v.append('Focus empty');


        }, 500);
    }

    stop() {

    }
}

/** spacegraph (via cytoscape.js) */
class GraphView extends NView {

    constructor() {
        super("Graph", "cubes");
    }

    start(v, app, cb) {
        this.s = spacegraph(v, {
            start: function () {

                newSpacePopupMenu(this);

                //s.nodeProcessor.push(new ListToText());
                //s.nodeProcessor.push(new UrlToIFrame());

                var m = newSpacegraphDemoMenu(this);
                m.css('position', 'absolute');
                m.css('right', '0');
                m.css('bottom', '0');
                m.css('z-index', '10000');
                m.css('opacity', '0.8');

                v.append(m);

                if (cb) cb();
            }
        });
    }

    stop() {
        if (this.s) {
            this.s.destroy();
            this.s = null;
        }
    }

}

/** spacegraph (via cytoscape.js) */
class Map2DView extends NView {

    constructor() {
        super("Map (2D)", "road");
    }

    start(v, app, cb) {
        var testIcon = L.icon({
            iconUrl: 'icon/unknown.png',
            iconSize: [32, 32],
            iconAnchor: [16, 16]
        });

        var map = this.map = L.map(v[0]);
        //map.setView([51.505, -0.09], 13);
        map.setView([35.98909,-84.2566178],13);
        //map.setView([0,0], 7);

        L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

        if (cb) cb();

        var agentIcons = { };


        setInterval(function() {
            for (var c in app.focus) {
                var d = app.data(c);
                for (var a in d) {
                    if (d[a].coordinates) {

                        if (!agentIcons[a]) {
                            var marker = L.marker([0, 0], {
                                icon: testIcon
                            }).addTo(map);
                            agentIcons[a] = marker;
                        }

                        //console.log(a, d[a].coordinates[0][0 /* first poly corner */]);
                        agentIcons[a].setLatLng({
                            lat: d[a].coordinates[0][0][0],
                            lng: d[a].coordinates[0][0][1]
                        });
                    }
                }
            }
        }, 750);
    }

    stop() {
        if (this.map != null) {
            this.map.remove();
            this.map = null;
        }
    }

}

//this is a hack to make Cesium's require.js work with Netention's screwed up util/ client-server code-sharing
var exports = undefined;
var modules = undefined;

/** spacegraph (via cytoscape.js) */
class Map3DView extends NView {

    constructor() {
        super("Map (3D)", "globe");
    }

    start(v, app, cb) {

        var that = this;

        var init = function () {

            var u = uuid();
            var d = newDiv(u);
            v.append(d);

            //http://cesiumjs.org/refdoc.html
            var viewer = new Cesium.Viewer(u /*'cesiumContainer'*/, {
                timeline: false,
                homeButton: false,
                animation: false,
                cesiumLogo: false
            });
            $('.cesium-widget-credits').remove();
            $('.cesium-viewer').css('height', '100%');
            $(viewer.cesiumLogo).remove();
            //$(viewer.timeline).remove();
            //$(viewer.animation).remove();

            that.viewer = viewer;

            v.append(viewer);
        };

        //ensure Cesium loaded
        if (!this.cesiumLoaded) {

            this.cesiumLoaded = true;

            loadCSS('lib/cesium/Widgets/CesiumWidget/CesiumWidget.css');

            $LAB
                .script('lib/cesium/Cesium.js')
                .wait(init);

        }
        else {
            init();
        }
    }

    stop() {
        if (this.viewer) {
            this.viewer.destroy()
            this.viewer = null;
        }
    }

}



class NObjectEditView extends NView {

    constructor(name) {
        super("Edit: " + name, "edit");
        this.name = name;

    }

    start(v, cb) {
        /** see edit.js */
        new NObjectEdit(v, uuid(), this.name);
    }

    stop(v) {
        //ask for save?
    }

}