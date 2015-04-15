"use strict";

class NClient extends EventEmitter {

    constructor() {
        this.focus = { };
        this.index = { };
        this.views = {
            'feed': new FeedView(this),
            'graph': new GraphView(),
            'map2d': new Map2DView(),
            'map3d': new Map3DView(),
            'wikipedia': new WikipediaView('Happiness'),
            'space1': new HTMLView('Spaces Test', 'lab', 'space.html')
        };
    }

    setFocus(tag, amount) {
        var prevFocus = this.focus[tag] || 0;
        if (prevFocus == amount) return;

        this.focus[tag] = amount;

        var t = this.index.tag.node(tag);

        if (prevFocus == 0) {
            //add focus

            if (t.channel) {
                //already open??? this shouldnt happen normally
                console.error('newly focused tag', t, 'already has channel opened');
            }
            else {
                var c = t.newChannel({
                    onChange: function (cc) {
                        console.log('change', JSON.stringify(cc.data));
                    }
                });
                t.channel = c; //the default, we'll store there
            }

            console.log('app focus on: ', tag, t, c);
        }
        else if (amount == 0) {
            //remove focus
            if (t.channel) {
                if (t.channel.off)
                    t.channel.off();
                delete t.channel;
            }
            console.log('app focus off: ', tag, t, c);
            delete this.focus[tag];
        }
        else {
            //change focus
        }

        console.log('app focus: ', app.focus);

    }


    setView(v, cb) {

        if (this.currentView) {
            try {
                this.currentView.stop();
            }
            catch (e) {
                console.error(e);
                notify(JSON.stringify(e));
            }
            $('#view').remove();
        }

        this.currentView = this.views[v];

        if (this.currentView) {
            var viewTarget = $('<div id="view"></div>');
            $('body').append(viewTarget);
            try {
                this.currentView.start(viewTarget, cb);
            }
            catch (e) {
                console.trace();
                console.error(e);
                viewTarget.html('ERROR setView(' + v + '): ' + e);
            }
        }

    }

    newViewControl() {
        //http://semantic-ui.com/elements/button.html#colored-group
        /*<div class="large ui buttons">
         <div class="ui button">One</div>
         <div class="ui button">Two</div>
         <div class="ui button">Three</div>
         </div>
         */
        var that = this;

        var d = $('<div class="ui buttons inverted">');
        _.each(that.views, function(v, k) {
            d.append($('<div class="ui icon button"></div>').append($('<i class="' + v.icon + ' icon"></i>'))
                /*.attr('data-content', v.name)
                 .popup()*/
                .click(function() {
                    //setTimeout(function() {
                    that.setView(k);
                    //}, 0);
                }));
        });
        return d;

    }

}

function ready() {


    var app = new NClient();

    window.app = app; //for console access

    Backbone.Router.extend({

        routes: {
//                "help":                 "help",    // #help
//                "search/:query":        "search",  // #search/kiwis
//                "search/:query/p:page": "search"   // #search/kiwis/p7
        }


//            help: function() {
//
//            },
//
//            search: function(query, page) {
//
//            }

    });
    Backbone.history.start({pushState: true});




    $('#sidebar').append(app.newViewControl());

    app.index = new TagIndex(function (i) {

        var t = new TagIndexAccordion(i);

        t.newElementHeader = function(tag) {

            //http://codepen.io/thehonestape/pen/yjlGi
            //http://thecodeplayer.com/walkthrough/spicing-up-the-html5-range-slider-input

            var d = newDiv();
            var ii = $('<input class="tagSlider" type = "range" value="0" min="0" max="100" _onchange="rangevalue.value=value"/>');
            ii.change(function(c) {
                app.setFocus(tag, parseInt(ii.val()) * 0.01);
            });
            d.html(ii);
            return d;
        };

        $('#sidebar').append(t);

    });

    app.setView('feed');

}


