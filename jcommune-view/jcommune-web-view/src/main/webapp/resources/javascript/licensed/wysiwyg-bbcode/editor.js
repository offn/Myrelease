/*
 WYSIWYG-BBCODE editor
 Copyright (c) 2009, Jitbit Sotware, http://www.jitbit.com/
 PROJECT HOME: http://wysiwygbbcode.codeplex.com/
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the <organization> nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY Jitbit Software ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL Jitbit Software BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

var body_id, textboxelement;
var baseHtmlElement_id, baseDivElement;
var html_content_id, htmlcontentelement;
var content;
var isIE = /msie|MSIE/.test(navigator.userAgent);
var editorVisible = false;

function BBtag(name) {
    this.name = name;
}

function findTag(tagName) {
    for (var i = 0; i < bbtags.length; i++) {
        if (bbtags[i].name == tagName) {
            return bbtags[i];
        }
    }
    return null;
}

var bbtags = [
    new BBtag("b"),
    new BBtag("i"),
    new BBtag("u"),
    new BBtag("s"),
    new BBtag("left"),
    new BBtag("center"),
    new BBtag("right"),
    new BBtag("quote"),
    new BBtag("code"),
    new BBtag("img"),
    new BBtag("highlight"),
    new BBtag("list"),
    new BBtag("color"),
    new BBtag("size"),
    new BBtag("indent"),
    new BBtag("url") ];

function initEditor(textAreaId, htmlAreaId, baseDivId) {
    body_id = textAreaId;
    baseHtmlElement_id = htmlAreaId;
    html_content_id = baseDivId;
    textboxelement = document.getElementById(textAreaId);
    baseDivElement = document.getElementById(htmlAreaId);
    htmlcontentelement = document.getElementById(baseDivId);
    htmlcontentelement.style.display = "none";
    content = textboxelement.value;
    editorVisible = false;
}

/**
 * Changes visual editor representation when toggling
 * preview mode.
 */
function SwitchEditor() {
    if (editorVisible) { // exit preview
        textboxelement.style.display = "";
        htmlcontentelement.style.display = "none";
        editorVisible = false;
        $(".hide-on-preview").show();
        $(".show-on-preview").hide();
        $("#preview")[0].value = $labelPreview;
    }
    else { // enter preview
        content = textboxelement.value;
        bbcode2html();
        $(".show-on-preview").show();
        $(".hide-on-preview").hide();
        $("#preview")[0].value = $labelEdit;
    }
}

// open tag regexp
var patternForOpenBBtag = "\\[([^\\/\\[\\]]*?)(=[^\\[\\]]*)?\\]";

// placeholders for special cases used internally during conversions.
// e.g. when user inputs characters %5D they will be replaced by 
// placeholder. And now when we send request to convert BB codes to html
// server will not treat these symbols as ]
var closeBracketCodePlaceholder = "@w0956756wo@";
var openBracketCodePlaceholder = "@ywdffgg434y@";
var slashCodePlaceholder = "14@123435vggv4f";
var lowerThenPlaceholder = "gertfgertgf@@@@@#4324234";

function bbcode2html() {
    var textdata = " " + textboxelement.value;
    textdata = textdata.replace(/%5D/gi, closeBracketCodePlaceholder);
    textdata = textdata.replace(/%5B/gi, openBracketCodePlaceholder);
    textdata = textdata.replace(/%22/gi, slashCodePlaceholder);
    textdata = textdata.replace(/</gi, lowerThenPlaceholder);
	
    textdata = encodeURI(textdata);
	
    textdata = textdata.replace(/%5D/gi, "]");
    textdata = textdata.replace(/%5B/gi, "[");
    textdata = textdata.replace(/%22/gi, "\"");
    textdata = textdata.replace(/%20/gi, " ");
	
    $.ajax({
        type:"POST",
        url:$root + '/posts/bbToHtml', //todo
        data:{bbContent:textdata},
        success:function (data) {
            var result;
            if (data.toString() == "[object XMLDocument]") {
                result = decodeURI(XMLtoString(data));
            } else {
                result = decodeURI(data);
            }
            result = result.replace(new RegExp(closeBracketCodePlaceholder, 'gi'), "%5D");
            result = result.replace(new RegExp(openBracketCodePlaceholder, 'gi'), "%5B");
            result = result.replace(new RegExp(slashCodePlaceholder, 'gi'), "%22");
            result = result.replace(new RegExp(lowerThenPlaceholder, 'gi'), "&lt;");

            $("#" + html_content_id).html(result.trim());
            htmlcontentelement.style.display = "";
            textboxelement.style.display = "none";

            editorVisible = true;
            
			//enable code highlight
			prettyPrint();
            //enable image preview
            $("a[rel^='prettyPhoto']").prettyPhoto({social_tools:false});
        }
    });
}

function XMLtoString(elem) {
    var serialized;

    try {
        // XMLSerializer exists in current Mozilla browsers
        serializer = new XMLSerializer();
        serialized = serializer.serializeToString(elem);
    }
    catch (e) {
        // Internet Explorer has a different approach to serializing XML
        serialized = elem.xml;
    }
    return serialized;
}

function closeTags() {
    var currentContent = textboxelement.value;

    currentContent = closeTag2(currentContent);

    currentContent = currentContent.replace(/\[size\]/gi, '[size=10]');
    currentContent = currentContent.replace(/\[color\]/gi, '[color=000000]');
    currentContent = currentContent.replace(/\[url\]/gi, '[url=]');
    currentContent = currentContent.replace(/\[indent\]/gi, '[indent=15]');

    content = currentContent;
    textboxelement.value = content;
}

function closeTag2(text) {
    var currentText = text;

    {
        var n = "&U999000A;";
        var space = "&999U000B;";
        var star = "&U999000C;";

        currentText = currentText.replace(/\n/gi, n);
        currentText = currentText.replace(/\s/gi, space);
        currentText = currentText.replace(/\[\*\]/gi, star);
    }

    var regexpForOpenBBtag = new RegExp(patternForOpenBBtag, 'ig');
    var regexpForOpenBBtagResult = regexpForOpenBBtag.exec(currentText);

    // find first(!) open tag
    if (regexpForOpenBBtagResult != null) {

        var tagName = regexpForOpenBBtagResult[1];

        var regTwoTags = '([^\\[\\]]*)(\\[(' + tagName + ')(=[^\\[\\]]*)?\\])(.*?)(\\[\/(' + tagName + ')\\])([^\\[\\]]*)(.*)';

        var domRegExp = new RegExp(regTwoTags, 'ig');
        /**
         * Example: "some [size=10][i]text[/i] for[/size] a [b]example[/b]...".
         * Tag name is "size".
         *
         * domResult[0] - full expression result
         * domResult[1] - prefix ("some ")
         * domResult[2] - full open tag ("[size=10]")
         * domResult[3] - tag name ("size")
         * domResult[4] - tag parameter value if exist ("=10")
         * domResult[5] - tag content ("[i]text[/i] for")
         * domResult[6] - close tag ("[/size]")
         * domResult[7] - tag name ("size")
         * domResult[8] - postffix without other tags (" a ")
         * domResult[9] - postffix with other tags if exist ("[b]example[/b]...")
         */

        var domResult = domRegExp.exec(currentText);
        if (domResult == null) {
            // add close tag
            currentText += "[/" + tagName + "]";
            // update domRegExp
            domRegExp = new RegExp(regTwoTags, 'ig');
            domResult = domRegExp.exec(currentText);
        }
        if (domResult != null) {
            currentText = closeTag2(domResult[1]) + domResult[2] + closeTag2(domResult[5]) + domResult[6] + closeTag2(domResult[8]) + closeTag2(domResult[9]);
        }
    }

    {
        currentText = currentText.replace(new RegExp(n, 'ig'), "\n");
        currentText = currentText.replace(new RegExp(space, 'ig'), " ");
        currentText = currentText.replace(new RegExp(star, 'ig'), "[*]");
    }

    return  currentText;
}

function doQuote() {
    if (!editorVisible) {
        AddTag('[quote]', '[/quote]');
    }
}

function doClick(command) {
    if (!editorVisible) {
        switch (command) {
            case 'bold':
                AddTag('[b]', '[/b]');
                break;
            case 'italic':
                AddTag('[i]', '[/i]');
                break;
            case 'underline':
                AddTag('[u]', '[/u]');
                break;
            case 'line-through':
                AddTag('[s]', '[/s]');
                break;
            case 'highlight':
                AddTag('[highlight]', '[/highlight]');
                break;
            case 'left':
                AddTag('[left]', '[/left]');
                break;
            case 'right':
                AddTag('[right]', '[/right]');
                break;
            case 'center':
                AddTag('[center]', '[/center]');
                break;
            case 'InsertUnorderedList':
                AddList('[list][*]', '[/list]');
                break;
            case 'listElement':
                AddTag('[*]', '');
                break;
        }
    }
}

function doSize(selectedElement) {
    if (!editorVisible) {
		var size = $(selectedElement).attr('value');
        AddTag('[size=' + size + ']', '[/size]');
    }
}

function doCode(selectedElement) {
    if (!editorVisible) {
		var code = $(selectedElement).attr('value');
        AddTag('[code=' + code + ']', '[/code]');
    }
}

function doIndent(selectedElement) {
    if (!editorVisible) {
        var indent = $(selectedElement).attr('value');
        AddTag('[indent=' + indent + ']', '[/indent]');
    }
}

var mylink = '';

function doLink() {
    mylink = '';
    var str;
    var element = textboxelement;
    if (isIE) {
        str = document.selection.createRange().text;
    } else if (typeof(element.selectionStart) != 'undefined') {
        var sel_start = element.selectionStart;
        var sel_end = element.selectionEnd;
        str = element.value.substring(sel_start, sel_end);
    }
    if (!editorVisible) {
        var bodyContent = createFormRow($labelUrlText, str, 'urlAltId', $labelUrlInfo, 'first') +
        createFormRow($labelUrl, '', 'urlId', $labelUrlRequired,'');
        var footerContent = '' +
            '<button id="bb-link-cancel" class="btn">' + $labelCancel + '</button> \
            <button id="bb-link-ok" class="btn btn-primary">' + $labelOk + '</button>';

        var submitFunc = function(e){
            e.preventDefault();
            if ($('#urlAltId')) {
                mylink = $('#urlAltId').val();
                var link = $.trim($('#urlId').val());
                if ((link != null) && (link != '')) {
                    if (mylink == null || mylink == '') {
                        mylink = link;
                    }
                    AddTag('[url=' + link + ']', '[/url]');
                    jDialog.closeDialog();
                } else {
                    ErrorUtils.removeErrorMessage('#urlId', $labelErrorsNotEmpty);
                    ErrorUtils.addErrorMessage('#urlId', $labelErrorsNotEmpty);
                    return false;
                }
            }
        }

        jDialog.createDialog({
            title: $labelUrlHeader,
            bodyContent: bodyContent,
            footerContent: footerContent,
            maxWidth: 450,
            tabNavigation: ['#urlAltId', '#urlId', '#bb-link-ok','#bb-link-cancel'],
            handlers: {
                '#bb-link-cancel': {'static':'close'},
                '#bb-link-ok': {'click': submitFunc}
            }
        });
    }
}

function createFormRow(text, value, idForElement, info, cls) {
    return '' +
        '<div class="control-group">' +
    		'<label for="' + idForElement + '" class="control-label">' + text + '</label>' +
    		'<div class="controls">' +
    			'<input id="' + idForElement + '" class="dialog-input ' + cls + '" type="text" value="' +
    				value + '" name="' + idForElement + '">' +
    				'<br>' +
    	    '</div>' +
    	    '<span class="dialog-info">' + info + '</span>' +
        '</div>';
    
}

function doImage() {
    if (!editorVisible) {
        var bodyContent = createFormRow($labelUrl, '', 'imgId', $labelUrlRequired, '');
        var footerContent = '' +
            '<button id="bb-img-cancel" class="btn">' + $labelCancel + '</button> \
            <button id="bb-img-ok" class="btn btn-primary">' + $labelOk + '</button>';

        var submitFunc = function(e){
            e.preventDefault();
            myimg = $('#imgId').val();
            if ((myimg != null) && (myimg != '')) {
                AddTag('[img]' + myimg + '[/img]', '');
                jDialog.closeDialog();
            }else {
                ErrorUtils.removeErrorMessage('#imgId', $labelErrorsNotEmpty);
                ErrorUtils.addErrorMessage('#imgId', $labelErrorsNotEmpty);
                return false;
            }
        }

        jDialog.createDialog({
            title: $labelImgHeader,
            bodyContent: bodyContent,
            footerContent: footerContent,
            maxWidth: 400,
            tabNavigation: ['#imgId', '#bb-img-ok','#bb-img-cancel'],
            handlers: {
                '#bb-img-ok': {'click': submitFunc},
                '#bb-img-cancel': {'static':'close'}
            }
        });
    }
}

//textarea-mode functions
function MozillaInsertText(element, text, pos) {
    element.value = element.value.slice(0, pos) + text + element.value.slice(pos);
}

function AddTag(t1, t2) {
    var element = textboxelement;
    if (isIE) {
        if (document.selection) {
            element.focus();

            var txt = element.value;
            var str = document.selection.createRange();

            if (str.text == "") {
                if (t2 == "[/url]") {
                    if (str.text != mylink) {
                        str.text = str.text + t1 + mylink + t2;
                    } else {
                        str.text = t1 + mylink + t2;
                    }
                } else {
                    str.text = t1 + t2;
                }
            }
            else if (txt.indexOf(str.text) >= 0) {
                str.text = t1 + str.text + t2;
            }
            else {
                if (t2 == "[/url]") {
                    element.value = txt + t1 + mylink + t2;
                } else {
                    element.value = txt + t1 + t2;
                }

            }
            str.select();
        }
    }
    else if (typeof(element.selectionStart) != 'undefined') {
        var sel_start = element.selectionStart;
        var sel_end = element.selectionEnd;
        if (element.value.substring(sel_start, sel_end) != mylink && t2 == "[/url]") {
            sel_start = sel_end;
        }
        MozillaInsertText(element, t1, sel_start);
        if (sel_start == sel_end && t2 == "[/url]") {
            MozillaInsertText(element, mylink + t2, sel_end + t1.length);
            element.selectionEnd = sel_end + t1.length + t2.length + mylink.length;
        } else {
            MozillaInsertText(element, t2, sel_end + t1.length);
            element.selectionEnd = sel_end + t1.length + t2.length;
        }
        element.selectionStart = sel_start;

        element.focus();
    }
    else {
        if (t2 == "[/url]") {
            element.value = element.value + t1 + mylink + t2;
        } else {
            element.value = element.value + t1 + t2;
        }
    }
}

function AddList(t1, t2) {
    var element = textboxelement;
    if (isIE) {
        if (document.selection) {
            element.focus();

            var txt = element.value;
            var str = document.selection.createRange();

            if (str.text == "") {
                str.text = t1 + t2;
            }
            else if (txt.indexOf(str.text) >= 0) {
                str.text = t1 + str.text + t2;
            }
            else {
                element.value = txt + t1 + t2;
            }

            var value1 = str.text;
            var nPos1 = value1.indexOf("\n", '[list]'.length);
            if (nPos1 > 0) {
                value1 = value1.replace(/\n/gi, "[*]");
            }
            str.text = value1;


            str.select();
        }
    }
    else if (typeof(element.selectionStart) != 'undefined') {
        var sel_start = element.selectionStart;
        var sel_end = element.selectionEnd;
        MozillaInsertText(element, t1, sel_start);
        MozillaInsertText(element, t2, sel_end + t1.length);

        element.selectionStart = sel_start;
        element.selectionEnd = sel_end + t1.length + t2.length;

        sel_start = element.selectionStart;
        sel_end = element.selectionEnd;

        var value = element.value.substring(sel_start, sel_end);
        var nPos = value.indexOf("\n", '[list]'.length);
        if (nPos > 0) {
            value = value.replace(/\n/gi, "[*]");
        }
        var elvalue = element.value;
        value = value.replace(/\[list\]\[\*\]/gi, '[list]\n[*]');
        value = value.replace(/\[\/list\]/gi, '\n[/list]');
        element.value = elvalue.substring(0, sel_start) + value + elvalue.substring(sel_end, elvalue.length);


        element.focus();
    }
    else {
        element.value = element.value + t1 + t2;
    }
}

//=======color picker
function getScrollY() {
    var scrOfX = 0, scrOfY = 0;
    if (typeof (window.pageYOffset) == 'number') {
        scrOfY = window.pageYOffset;
        scrOfX = window.pageXOffset;
    } else if (document.body && (document.body.scrollLeft || document.body.scrollTop)) {
        scrOfY = document.body.scrollTop;
        scrOfX = document.body.scrollLeft;
    } else if (document.documentElement && (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
        scrOfY = document.documentElement.scrollTop;
        scrOfX = document.documentElement.scrollLeft;
    }
    return scrOfY;
}

document.write("<style type='text/css'>.colorpicker201{visibility:hidden;display:none;position:absolute;background:#FFF;z-index:999;filter:progid:DXImageTransform.Microsoft.Shadow(color=#D0D0D0,direction=135);}.o5582brd{padding:0;width:12px;height:14px;border-bottom:solid 1px #DFDFDF;border-right:solid 1px #DFDFDF;}a.o5582n66,.o5582n66,.o5582n66a{font-family:arial,tahoma,sans-serif;text-decoration:underline;font-size:9px;color:#666;border:none;}.o5582n66,.o5582n66a{text-align:center;text-decoration:none;}a:hover.o5582n66{text-decoration:none;color:#FFA500;cursor:pointer;}.a01p3{padding:1px 4px 1px 2px;background:whitesmoke;border:solid 1px #DFDFDF;}</style>");

function getTop2() {
    csBrHt = 0;
    if (typeof (window.innerWidth) == 'number') {
        csBrHt = window.innerHeight;
    } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
        csBrHt = document.documentElement.clientHeight;
    } else if (document.body && (document.body.clientWidth || document.body.clientHeight)) {
        csBrHt = document.body.clientHeight;
    }
    ctop = ((csBrHt / 2) - 115) + getScrollY();
    return ctop;
}
var nocol1 = "&#78;&#79;&#32;&#67;&#79;&#76;&#79;&#82;",
    clos1 = "X";

function getLeft2() {
    var csBrWt = 0;
    if (typeof (window.innerWidth) == 'number') {
        csBrWt = window.innerWidth;
    } else if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) {
        csBrWt = document.documentElement.clientWidth;
    } else if (document.body && (document.body.clientWidth || document.body.clientHeight)) {
        csBrWt = document.body.clientWidth;
    }
    cleft = (csBrWt / 2) - 125;
    return cleft;
}

//function setCCbldID2(val, textBoxID) { document.getElementById(textBoxID).value = val; }
function setCCbldID2(val) {
    if (!editorVisible)
        AddTag('[color=' + val + ']', '[/color]');
}

function setCCbldSty2(objID, prop, val) {
    switch (prop) {
        case "bc":
            if (objID != 'none') {
                document.getElementById(objID).style.backgroundColor = val;
            }
            ;
            break;
        case "vs":
            document.getElementById(objID).style.visibility = val;
            break;
        case "ds":
            document.getElementById(objID).style.display = val;
            break;
        case "tp":
            document.getElementById(objID).style.top = val;
            break;
        case "lf":
            document.getElementById(objID).style.left = val;
            break;
    }
}

function putOBJxColor2(Samp, pigMent, textBoxId) {
    //document.getElementById("o5582n66").value='#' + pigMent;
    javascript:document.getElementById("o5582n66a").style.backgroundColor = '#' + pigMent;
    // title='#' + pigMent;
}

function showColorGrid2(Sam, textBoxId) {
    if (!editorVisible) {
        var objX = new Array('00', '33', '66', '99', 'CC', 'FF');
        var c = 0;
        var xl = '"' + Sam + '","x", "' + textBoxId + '"';

        var bodyContent = '';
        bodyContent += '<table border="0" cellpadding="0" cellspacing="0" style="border:solid 0px #F0F0F0;padding:2px;table-layout:fixed;width:350px;"><tr>';
        var br = 1;
        for (o = 0; o < 6; o++) {
            bodyContent += '</tr><tr>';
            for (y = 0; y < 6; y++) {
                if (y == 3) {
                    bodyContent += '</tr><tr>';
                }
                for (x = 0; x < 6; x++) {
                    var grid = '';
                    grid = objX[o] + objX[y] + objX[x];
                    var b = "'" + Sam + "','" + grid + "', '" + textBoxId + "'";
                    bodyContent += '<td class="o5582brd" style="background-color:#' + grid + '"><a class="o5582n66"  href="javascript:onclick=putOBJxColor2(' + b + ');"><div style="width:12px;height:14px;"></div></a></td>';
                    c++;
                }
            }
        }
        bodyContent += '</tr></table>';

        var titleContent = '<div style="height: 30px"> \
            <div class="left-aligned">' + $labelSelectedColor + '</div> \
            <div id="o5582n66a" class="left-aligned dialog-color-picker"></div></div>';

        var footerContent = '' +
            '<button id="bb-color-cancel" class="btn">' + $labelCancel + '</button> \
            <button id="bb-color-ok" class="btn btn-primary">' + $labelOk + '</button>';

        var submitFunc = function (e) {
            e.preventDefault();
            var rgb_color = $('#o5582n66a').css('backgroundColor');
            var hex_color = getHexRGBColor(rgb_color);
            AddTag('[color=' + hex_color + ']', '[/color]');
            jDialog.closeDialog();
        }

        jDialog.createDialog({
            title: titleContent,
            bodyContent: bodyContent,
            footerContent: footerContent,
            maxWidth: 380,
            fisrtFocus: false,
            tabNavigation: ['#bb-color-ok','#bb-color-cancel'],
            handlers: {
                '#bb-color-ok': {'click': submitFunc},
                '#bb-color-cancel': {'static':'close'}
            }
        });
    }

    function getHexRGBColor(color) {
        color = color.replace(/\s/g, '');
        var aRGB = color.match(/^rgb\((\d{1,3}[%]?),(\d{1,3}[%]?),(\d{1,3}[%]?)\)$/i);

        if (aRGB) {
            color = '';
            for (var i = 1; i <= 3; i++) color += Math.round((aRGB[i][aRGB[i].length - 1] == "%" ? 2.55 : 1) * parseInt(aRGB[i])).toString(16).replace(/^(.)$/, '0$1');
        }
        else color = color.replace(/^#?([\da-f])([\da-f])([\da-f])$/i, '$1$1$2$2$3$3');

        return color.toUpperCase();
    }
}

$(document).ready(function() {
	$('#select_size a').click(function() {
		doSize(this);
	});
	
	$('#select_code a').click(function() {
		doCode(this);
	});
	
	$('#select_indent a').click(function() {
		doIndent(this);
	});
});