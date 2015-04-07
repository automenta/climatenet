"use strict";

class Tag {

    constructor(id, data/*tagJSON*/) {

        this.id = id;
        this.name = data.name;
        this.inh = data.inh;

        //var n = tag.node(i);
        //if (!n) {
        //n = newTag(i);
        //}
        //
        ////TODO copy other metadata, use _.extend
        //n.name = l.name || i;
        //
        //if (l.style) {
        //    n.style = l.style;
        //}
        //if (l.styleUrl) {
        //    n.styleUrl = l.styleUrl;
        //}
        //
        //n.meta = l.meta || { };
        //
        //if (n.meta.wmsLayer) {
        //    //n.features[n.meta.wmsLayer] = newWMSLayer(n.meta.wmsLayer);
        //}
        //if (n.meta.tileLayer) {
        //    //n.features[n.meta.tileLayer] = newTileLayer(n.meta.tileLayer);
        //}
        //
        //
        //if (l.inh) {
        //    n.inh = l.inh;
        //}
        //
        //n.update();

    }

    getPanelHTML() {
        var x = '<div style="width: 100%; height: 100%; color: black; background-color: orange; border: 2px solid black;">';
        x += '<h1>' + this.name + '</h1>';
        x += JSON.stringify(this.inh);
        x += '</div>'
        return x;
    }
}

class TagIndex {

    constructor(callback) {
        "use strict";

        this.tag = new graphlib.Graph({multigraph: true});

        //TEMPORARY
        var x = {
            id: 'untitled_' + parseInt(Math.random() * 100),
            style: {
                'node': {
                    'content': 'data(content)',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'shape': 'rectangle'
                },
                '$node > node': {
                    'padding-top': '2px',
                    'padding-left': '2px',
                    'padding-bottom': '2px',
                    'padding-right': '2px',
                    'text-valign': 'top',
                    'text-halign': 'center'
                },
                'edge': {
                    'target-arrow-shape': 'triangle',
                    //'line-style': 'dashed',
                    'line-width': '16'
                },
                ':selected': {
                    //'background-color': 'black',
                    'line-color': 'black',
                    'target-arrow-color': 'black',
                    'source-arrow-color': 'black'
                }
            },
            nodes: [],
            edges: []
        };




        this.channel = new Channel(x);


        var that = this;
        $.getJSON('/tag/index')
            .done(function(tagMap) {

                for (var i in tagMap)
                    that.updateTag(i, tagMap[i]);

                //add edges
                var nn = that.tag.nodes();
                for (var i = 0; i < nn.length; i++) {
                    var t = that.tag.node(nn[i]);
                    if (!t.inh) continue;

                    for (var j in t.inh) {
                        //var strength = t.inh[j];
                        that.tag.setEdge(j, t.id);
                    }
                }

                if (callback)
                    callback(that);
            })
            .fail(ajaxFail);

        /*
        $.getJSON('/tag/meta', {id: JSON.stringify(layerIDs)})
            .done(function (r) {
                updateTags(r);
                if (callback)  callback();
            })
            .fail(ajaxFail);
            */

    }


    activateRoots(levels) {
        var MAX_NODES = 4, count = 0;

        var roots = [];

        var nn = this.tag.nodes();
        for (var i = 0; i < nn.length; i++) {
            var t = this.tag.node(nn[i]);

            var id = t.id;

            //TODO temporary
            if (id.indexOf(' ')!=-1) {
                console.error('invalid tag ID: ' + id);
                continue;
            }

            var parent = this.tag.predecessors( id );
            if (parent.length > 0) {
                continue;
            }

            roots.push(t);

            this.activate(id, levels);

            if (count++ == MAX_NODES) break; //TEMPORARY
        }

        return roots;
    }

    activate(t, levels) {

        if (typeof(t) === "string") t = this.tag.node(t);

        //TODO temporary
        if (t.id.indexOf(' ')!=-1) {
            console.error('invalid tag ID: ' + t.id);
            return null;
        }

        var w = {
            id: t.id,
            style: {
                shape: 'rectangle',
                width: 64,
                height: 32
            },
            widget: {
                html: t.getPanelHTML(),
                style: {},
                scale: 0.9,
                pixelScale: 320.0,
                minPixels: 8
            }
        };

        this.channel.data.nodes.push(w);

        if (levels > 0) {
            var children = this.tag.successors(t.id);
            for (var i = 0; i < children.length; i++) {
                var v = this.activate(children[i], levels - 1);
                if (v) {
                    //create edge from this node to child
                    var edgeID = t.id + '_' + children[i];
                    var e = {id: edgeID, source: t.id, target: children[i],
                            style: {
                                'line-color': 'white',
                                'line-width': '64'
                            }
                        };
                    this.channel.data.edges.push(e);
                }
            }
        }

        return w;
    }

    updateTag(i,l) {
        "use strict";
        this.tag.setNode(i, new Tag(i, l));
    }



}


var Channel = function (initialData, connection) {

    var synchPeriodMS = 1500;

    //set channel name
    if (typeof(initialData)==="string")
        initialData = { id: initialData };
    
    var c = {
        ui: null,
        data: initialData,
        socket: connection,
        prev: {},
        id: function () {
            return this.data.id;
        }
    };

    c.init = function (ui) {
        this.ui = ui;

        //this.commit();
    };
    
    c.destroy = function() {
        
    };


    c.removeNode = function(n) {
        n.data().removed = true;

        var removedAny = false;
        var id = n.data().id;
        this.data.nodes = _.filter(this.data.nodes, function(e) {
            if (e.id === id) {
                removedAny = true;
                return false;
            }
        });
                        
        return removedAny;
    };
    
    c.addNode = function(n) {
        
        if (!this.data)
            this.data = { };
        
        if (!this.data.nodes)
            this.data.nodes = [];
        
        c.data.nodes.push(n);
    
    };
    
    c.commit = _.throttle(function () {

        if (!this.socket || !this.socket.opened) {
            return;
        }

        /** include positions in update only if p is defined and is object */
        if (this.data.p && typeof(this.data.p)==="object") {
            //get positions
            var eles = this.ui.elements();
            var P = {};
            for (var i = 0; i < eles.length; i++) {
                var ele = eles[i];
                //console.log( ele.id() + ' is ' + ( ele.selected() ? 'selected' : 'not selected' ) );
                var p = ele.position();
                var x = p.x;
                if (!isFinite(x))
                    continue;
                var y = p.y;
                P[ele.id()] = [parseInt(x), parseInt(y)];
            }
            this.data.p = P; //positions; using 1 character because this is updated frequently
        }

        //https://github.com/Starcounter-Jack/Fast-JSON-Patch
        var diff = jsonpatch.compare(this.prev, this.data);

        this.prev = _.clone(this.data, true);

        if (diff.length > 0) {
            this.socket.send(['p' /*patch*/, this.data.id, diff]);
        }

    }, synchPeriodMS);

    return c;
};


/** creates a websocket connection object */
function Websocket(path, conn) {

    if (!conn) conn = { };
    
    if (!conn.url)
        conn.url = 'ws://' + window.location.hostname + ':' + window.location.port + '/' + path;    
    
    //subscriptions: channel id -> channel
    conn.subs = { };
    

    var ws = conn.socket = new WebSocket(conn.url);

    ws.onopen = function () {

        conn.opened = true;

        //console.log('websocket connected');

        if (conn.onOpen)
            conn.onOpen(this);


    };
    
    ws.onclose = function () {
        //already disconnected?
        if (!this.opt)
            return;

        conn.opened = false;

        //console.log("Websocket disconnected");

        if (conn.onClose)
            conn.onClose();
        
        //attempt reconnect?
    };
    ws.onerror = function (e) {
        console.log("Websocket error", e);
        if (conn.onError)
            conn.onError(e);
    };

    conn.send = function(data) {
        var jdata = /*jsonUnquote*/( JSON.stringify(data) );
        
        //console.log('send:', jdata.length, jdata);

        this.socket.send(jdata);
    };

    conn.handler = {
        '=': function(d) {
            var channelData = d[1];
                        
            //console.log('replace', channelData);
            
            var chanID = channelData.id;
            var chan = conn.subs[chanID];
            if (!chan) {
                chan = new Channel( channelData, conn );
                if (window.s)
                    window.s.addChannel(chan);
                                
                if (conn.onChange)
                    conn.onChange(chan);
            }
            else {
                chan.data = channelData;
                if (window.s)
                    window.s.updateChannel(chan);
                
                if (conn.onChange)
                    conn.onChange(chan);                
            }
        },
        '+': function(d) {
            var channelID = d[1];
            var patch = d[2];
            
                        
            //{ id: channelData.id, data:channelData}
            var c = conn.subs[channelID];
            if (c) {
                //console.log('patch', patch, c, c.data);

                jsonpatch.apply(c.data, patch);
                
                if (window.s)
                    window.s.addChannel(c);                
                
                if (conn.onChange)
                    conn.onChange(c);
            }
            else {
                console.error('error patching', d);
            }
        }

                
    };
    
    ws.onmessage = function (e) {
        /*e.data.split("\n").forEach(function (l) {
         output(l, true);
         });*/
        
        //try {
            var d = JSON.parse(e.data);
            
            if (d[0]) {
                
                //array, first element = message type
                var messageHandler = conn.handler[d[0]];
                if (messageHandler) {                    
                    //return conn.apply(messageHandler,d);
                    return messageHandler(d);
                }
            }
            
            notify('websocket data (unrecognized): ' + JSON.stringify(d));
        /*}
        catch (ex) {
            notify('in: ' + e.data);
            console.log(ex);
        }*/
    };
    
    conn.on = function(channelID, callback) {
        
        if (conn.subs[channelID]) {
            //already subbed            
        }
        else {
            conn.subs[channelID] = new Channel(channelID);
            if (callback)
                callback.off = function() { conn.off(channelID); };            
        }
        
        conn.send(['on', channelID]);
        
        //TODO save callback in map so when updates arrive it can be called
        
        return callback;
    };
    
    //reload is just sending an 'on' event again
    conn.reload = function(channelID) {
        conn.send(['!', channelID]);
    };
    
    conn.operation = function(op, channelID) {
        conn.send([op, channelID]);
    };
    
    conn.off = function(channelID) {
        if (!conn.subs[channelID]) return;
        
        delete conn.subs[channelID];
        
        conn.send(['off', channelID]);        
        
    };

    return conn;

}




function jsonUnquote(json) {
    return json.replace(/\"([^(\")"]+)\":/g, "$1:");  //This will remove all the quotes
}



function notify(x) {
    PNotify.desktop.permission();
    if (typeof x === "string")
        x = { text: x };
    else if (!x.text)
        x.text = '';
    if (!x.type)
        x.type = 'info';
    x.animation = 'none';
    x.styling = 'fontawesome';

    new PNotify(x);
    //.container.click(_notifyRemoval);
}


//faster than $('<div/>');
function newDiv(id) {
    var e = newEle('div');
    if (id)
        e.attr('id', id);
    return e;
}

function newEle(e, dom) {
    var d = document.createElement(e);
    if (dom)
        return d;
    return $(d);
}


function urlQuery(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split("=");
        if (pair[0] === variable) {
            return pair[1];
        }
    }
    return(false);
}

var ajaxFail = function (v, m) {
    console.error('AJAJ Err:', v, m);
};
