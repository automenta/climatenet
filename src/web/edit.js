"use strict";

//nobject edit
class NObjectEdit {

    /** target div, id, obj */
    constructor(target, obj) {

        //if (obj && typeof(obj) === "string")
        //    obj = new NObject(id, obj);

        var tag = {id: 'NoTag', name: 'Not Tagged' };

        this.obj = obj;
        this.target = target;

        var d = $('<div class="ui modal inlineModal">');
        d.append('<i class="close icon"></i>');


        var subj = newSubjectDropdown();
        subj.css('padding', '0');

        d.append($('<div class="header"/>').append('Tag ', subj,  ' with: ', $('<i>' + tag.name + '</em>')));

        var c = $('<div class="content"/>').appendTo(d);
        c.append('<div class="ui image"><i class="icon tags"></i></div>');

        var cc = $('<div class="description"/>').appendTo(c);
        //cc.append('<div class="ui header"></div>');

        var option = function(id, label) {
            cc.append('<div class="ui checkbox"><input type="checkbox" name="' + id + '"><label>' + label + '</label></div>');
        }

        option('can', 'Can');
        option('need', 'Need');
        option('not', 'Not');




        /*

         <div class="description">
         <div class="ui header">We've auto-chosen a profile image for you.</div>
         <p>We've grabbed the following image from the <a href="https://www.gravatar.com" target="_blank">gravatar</a> image associated with your registered e-mail address.</p>
         <p>Is it okay to use this photo?</p>
         </div>
         */
        var tagid = tag.id;

        var a = $('<div class="actions"/>').appendTo(d);;
        a.append('<div class="ui black button">Cancel</div>');
        a.append('<div class="ui positive right labeled icon button">Save<i class="checkmark icon"></i></div>');


        target.html(d);


    }



}

function newWikiTagger(tag) {
    //http://semantic-ui.com/modules/modal.html#/usage

    var d = newDiv();
    new NObjectEdit(d, uuid());
    return d;

}
