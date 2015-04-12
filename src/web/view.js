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

/** spacegraph (via cytoscape.js) */
class GraphView extends NView {

    constructor() {
        super("Graph", "cubes");
    }

    start(v, cb) {
        this.s = spacegraph(v, {
            start: function () {

                newPopupMenu(this);

                //s.nodeProcessor.push(new ListToText());
                //s.nodeProcessor.push(new UrlToIFrame());

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

    start(v, cb) {

        var map = this.map = L.map(v[0]);
        map.setView([51.505, -0.09], 13);

        L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

        if (cb) cb();
    }

    stop() {
        if (this.map!=null) {
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

    start(v, cb) {

        var that = this;

        var init = function() {

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
        if (that.viewer) {
            that.viewer.destroy()
            that.viewer = null;
        }
    }

}
