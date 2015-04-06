"use strict";

class Tag {

    constructor(i, l/*tagJSON*/) {

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
}

class TagIndex {

    constructor(callback) {
        "use strict";

        this.tag = { };

        var that = this;
        $.getJSON('/tag/index')
            .done(function(tagMap) {
                for (var i in tagMap)
                    that.updateTag(i, tagMap[i]);

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

    getGraph() {

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
        for (var t in this.tag) {
            if (t.indexOf(' ')!=-1) {
                console.error('invalid tag ID: ' + t);
                continue;
            }

            x.nodes.push({
                id: t,
                //content: t,
                style: {
                    shape: 'rectangle',
                    height: 25,
                    width: 25
                }
            } );
        }
        return x;


                //{id: 'b',
                //    style: {
                //        shape: 'triangle',
                //        height: 24,
                //        width: 16
                //    }
                //},

                /*{id: 'b1', parent: 'p',
                 style: { _content: 'x', shape: 'triangle', height: 4, width: 4 }
                 },*/
                /*{id: 'b2', parent: 'p',
                 style: { _content: 'y', shape: 'triangle', width: 8, height: 8 }
                 },
                 {id: 'b3', parent: 'p',
                 style: { _content: 'z', shape: 'triangle', width: 8, height: 8 }
                 },
                 */
                /*{
                 id: 'serial' + parseInt(Math.random()*100),
                 width: 16,
                 height: 16,

                 widget: {
                 html: "<div contenteditable='true' class='editable' style='overflow: auto; resizable: both'></div>",
                 scale: 0.8,
                 style: {width: '300px', height: '300px'},
                 },
                 },*/
                //{id: 'd',
                //    form: {
                //        value: {
                //            firstname: 'First',
                //            surname: 'Last',
                //            age: 0
                //        },
                //        style: {width: '300px', height: '260px'},
                //        scale: 1
                //    },
                //    widget: {
                //        html: '<x-metawidget id="metawidget1" path="person"></x-metawidget>',
                //        style: {},
                //        //html: '<iframe width="600px" height="600px" src="http://enenews.com"></iframe><br/><button>x</button>',
                //        scale: 1,
                //        pixelScale: 300.0,
                //        minPixels: 2,
                //    },
                //    style: {
                //        height: "24",
                //        width: "24",
                //        opacity: 0.75
                //    }
                //},
                ///*{id: 'u',
                // width: 48, height: 32, url: 'http://wikipedia.org'},*/
                //{id: 'e',
                //    width: 64,
                //    height: 64,
                //    widget: {
                //        html: '<div style="width: 100%; height: 100%; background-color: orange; border: 2px solid black;"><div contenteditable="true" class="editable">WTF</div><br/><button>OK</button></div>',
                //        style: {},
                //        scale: 0.9,
                //        minPixels: 8,
                //    }
                //},
                //{id: 'e1',
                //    width: 64,
                //    height: 64,
                //    widget: {
                //        html: '<div style="background-color: green; border: 2px solid black;"><div contenteditable="true" class="editable">OR(AND(F,B),Z) => X</div><br/><button>OK</button></div>',
                //        style: {},
                //        scale: 0.9,
                //        minPixels: 16,
                //    }
                //}


                //{id: 'f', parent: 'e'}
        //    ],
        //    edges: [
        //        {id: 'eb', source: 'e', target: 'b',
        //            style: {
        //                'line-color': 'blue',
        //                'line-width': '44'
        //            }
        //        },
        //        {id: 'db', source: 'd', target: 'b'},
        //        //{id: 'b1b2', source: 'b1', target: 'b2'},
        //        //{id: 'b1b3', source: 'b1', target: 'b3'}
        //        //{id: 'eb', source: 'e', target: 'b'}
        //    ]
        //};


    }

    updateTag(i,l) {
        "use strict";
        this.tag[i] = new Tag(l);
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
