function TagIndexAccordion(tagIndex) {

    var roots = tagIndex.activateRoots(1);

    var addChild = function (parent, tag) {
        var id = tag.id;
        var label = tag.name || tag.content || id;

        var y = $('<div class="title small"/>').text(label);
        var z = $('<div class="content"/>').attr('tag', id);
        parent.append(y, z);
    }

    var x;

    //http://semantic-ui.com/modules/accordion.html#/settings

    x = $('<div class="ui styled accordion" style="max-height: 100%; overflow: scroll" />').accordion({

        exclusive: false,

        duration: 150,

        onOpen: function (e, z) {

            //console.log('open',this, e,z);

            var opened = $(this);

            var t = opened.attr('tag');

            if (x.newElementHeader)
                h = x.newElementHeader(t);
            else
                h = '';

            opened.html(h); //TODO maybe not remove if not changed; will also remove subtrees and this could be expensive


            if (t) {
                var nodes = [], edges = [];

                tagIndex.graphize(t, 1, nodes, edges);

                //opened.append(JSON.stringify(nodes));

                _.each(nodes, function (c) {
                    if (c.id !== t)
                        addChild(opened, c);
                });
            }
        }
    });


    var update = function (d) {
        d.accordion('refresh');
    }


    _.each(roots, function (r) {
        addChild(x, r);
    });

    /*
     <div class="ui styled accordion">
     <div class="active title">
     <i class="dropdown icon"></i>
     Level 1
     </div>
     <div class="active content">
     Welcome to level 1
     <div class="accordion">
     <div class="active title">
     <i class="dropdown icon"></i>
     Level 1A
     </div>  */
    return x;

}