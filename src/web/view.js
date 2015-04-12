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
        super("map2d", "Map (2D)", "road");
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