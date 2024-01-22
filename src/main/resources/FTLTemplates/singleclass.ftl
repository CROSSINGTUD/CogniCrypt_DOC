<html>
<head>
    <title>${title}</title>
    <style>

        * {
            font-family: "Source Sans Pro", "Helvetica Neue", Arial, sans-serif;
        }

        .tree,
        .tree ul,
        .tree li {
            list-style: none;
            margin: 0;
            padding: 0;
            position: relative;
        }

        .tree {
            margin: 0 0 1em;
            text-align: center;
        }

        .tree,
        .tree ul {
            display: table;
        }

        .tree ul {
            width: 100%;
        }

        .tree li {
            display: table-cell;
            padding: .5em 0;
            vertical-align: top;
        }

        .tree li:before {
            outline: solid 1px #666;
            content: "";
            left: 0;
            position: absolute;
            right: 0;
            top: 0;
        }

        .tree li:first-child:before {
            left: 50%;
        }

        .tree li:last-child:before {
            right: 50%;
        }

        .tree code,
        .tree span {
            border: solid .1em #666;
            border-radius: .2em;
            display: inline-block;
            margin: 0 .2em .5em;
            padding: .2em .5em;
            position: relative;
        }

        .tree ul:before,
        .tree code:before,
        .tree span:before {
            outline: solid 1px #666;
            content: "";
            height: .5em;
            left: 50%;
            position: absolute;
        }

        .tree ul:before {
            top: -.5em;
        }

        .tree code:before,
        .tree span:before {
            top: -.55em;
        }

        .tree > li {
            margin-top: 0;
        }

        .tree > li:before,
        .tree > li:after,
        .tree > li > code:before,
        .tree > li > span:before {
            outline: none;
        }


        .collapsible {
            background-color: #777;
            color: white;
            cursor: pointer;
            padding: 18px;
            width: 100%;
            border: none;
            text-align: left;
            outline: none;
            font-size: 15px;
        }

        .active, .collapsible:hover {
            background-color: #555;
        }

        .collapsible:after {
            content: '\002B';
            color: white;
            font-weight: bold;
            float: right;
            margin-left: 5px;
        }

        .active:after {
            content: "\2212";
        }

        .content {
            padding: 0 18px;
            display: none;
            overflow: hidden;
            background-color: #f1f1f1;
        }

        .pre {
            width: 100%;
            font-size: 14px;
        }

        pre {
            font-size: 14px;
        }

        .fortree {
            overflow-x: auto
        }

        .floatbutton {
            position: fixed;
            bottom: 10px;
            right: 10px;
        }

        a[target="_blank"]::after {
            content: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVR42qXKwQkAIAxDUUdxtO6/RBQkQZvSi8I/pL4BoGw/XPkh4XigPmsUgh0626AjRsgxHTkUThsG2T/sIlzdTsp52kSS1wAAAABJRU5ErkJggg==);
            margin: 0 3px 0 5px;
        }

        .help {
            overflow-wrap: break-word;
            font-size: 12px;
            border: 2px solid #555;
            display: inline-block;

        }

        .spoiler {
            font-size: 14px;
        }

        .tooltip {
            color: #555;
            text-decoration: underline;
        }

        .tooltip:hover {
            color: #222;
        }

        .tooltip .tooltiptext {
            display: inline-block;
            visibility: hidden;
            color: black;
            border-radius: 2px;
            position: absolute;
            border: 2px solid black;
            background-color: #f1f1f1;
            white-space: pre-line

        }

        .tooltip:hover .tooltiptext {
            white-space: pre-line;
            visibility: visible;
            opacity: 1;
        }


        a:link {
            color: #555;
            text-decoration: underline;
        }

        a:visited {
            color: #555;
            text-decoration: underline;
        }

        a:hover {
            color: #222;
            text-decoration: underline;
        }

        a:active {
            color: #222;
            text-decoration: underline;
        }
    </style>
</head>

<body style="background-color: #f1f1f1">

<#if booleanB>
    <button title="Click to Show" id="toggleBtn" value="Show Help" class="floatbutton" type="button" onclick="toggle()">
        Show Help
    </button>
</#if>
<button title="Click to Show" id="toggleAl" value="Collapse All" type="button"
        style="right: 50px; top: 20px; position: fixed;" onclick="toggleAl()">Collapse All
</button>

<button class="collapsible">Overview</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help"> Help is now displayed for the other sections!
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;">This page documents <b>${rule.composedClassName}</b>:
        ${rule.composedLink}
        An overview of all sections can be found on the <a href="../frontpage.html">frontpage</a>.
    </p>
</div>
<button class="collapsible">Order</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help">Help:
            This section describes the secure order call sequences of ${rule.composedClassName}.
            Methods may contain an underscore(_) as a parameter.
            The underscore is a feature of CrySl to help writing CrySL rules for overloaded methods and not specify all overloaded methods in a CrySL rule.
            Conduct the <a target="_blank" rel="noopener noreferrer"
                           href="https://docs.oracle.com/javase/8/docs/api/${rule.onlyLink}.html">JavaDoc</a> to see all parameters in detail.
        </p>
    </div>
    <p class="pre">${rule.numberOfMethods}
    </p>
    <pre style="overflow-x:auto"><#list rule.order as order>${order}
        </#list>
            </pre>
    <#if booleanA>
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                This section represents the order of the class as a state machine graph.
                The most left node is always the Start node.
                Double circled nodes are accepting states.
                The edge labels are the necessary methods to transition from one node to the next.
                The graph only shows the secure paths.
                A class is not securely used if method calls deviate from the displayed order.
            </p>
        </div>
        <div class="fortree">
            <img src="../dotFSMs/${rule.composedClassName}.svg" style="background-color: #f1f1f1">
        </div>
    </#if>
</div>
<button class="collapsible">Constraints</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help">Help:
            This section describes the parameters, which have constraints or that require a predicate from another
            class.
            Predicates are a construct from CrySL and allow to securely compose several classes depending on use cases.
            A class can ensure predicates for other classes and require predicates from other classes.
            E.g., ${rule.composedClassName} requires a predicate from another class and the predicate is not ensured, ${rule.composedClassName} is not used securely.
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;overflow-wrap: break-word"><#if rule.allConstraints?has_content>
            <#list rule.forbiddenMethods as fm>${fm}
            </#list>
            <#list rule.valueConstraints as vc>${vc}
            </#list>
            <#list rule.constrainedPredicates as cp>${cp}
            </#list>
            <#list rule.comparsionConstraints as cc>${cc}
            </#list>
            <#list rule.constrainedValueConstraints as cvc>${cvc}
            </#list>
            <#list rule.noCallToConstraints as nctc>${nctc}
            </#list>
            <#list rule.instanceOfConstraints as ioc>${ioc}
            </#list>
            <#list rule.constraintAndEncConstraints as caec>${caec}
            </#list>
        <#else>
            There are no Constraints for this class.
        </#if>
    </p>
</div>
<button class="collapsible">Predicates</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help">Help:
            This section describes which Predicates the class ensures.
            Predicates are a construct from CrySL and allow to securely compose several classes depending on use cases.
            A class can ensure predicates for other classes and require predicates from other classes.
            Predicates are ensured after specific method calls or after the method calls seen in the Order section.
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;"><#list rule.ensuresThisPredicates as etp>${etp}
        </#list>
        <#list rule.ensuresPredicates as ep>${ep}
        </#list>
        <#list rule.negatesPredicates as np>${np}
        </#list>
    </p>

</div>
<#if booleanC>
    <button class="collapsible">Requires Tree</button>
    <div class="content">
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                This section displays the Requires Tree.
                It displays the required predicate dependencies starting from ${rule.composedClassName}
                The read direction is from top to bottom.
                For e.g. ${rule.composedClassName} can require something from ...
                Furthermore, it shows for the next depending classes as well.
                There are two special cases:
                1. The class does not require a predicate. Therefore, only the classname itself is displayed.
                2. The class provides a predicate for itself. This is not displayed due to the tree nature.
            </p>
        </div>
        <div class="fortree">
            <ul class="tree">
                <#macro reqTree treenode>
                    <li>
                        <span> <a href="${treenode.data}.html">${treenode.data}</a></span>
                        <#if treenode.children?has_content>
                            <ul>
                                <#list treenode.children as child>
                                    <@reqTree child />
                                </#list>
                            </ul>
                        </#if>
                    </li>
                </#macro>
                <@reqTree requires />
            </ul>
        </div>
    </div>
    <button class="collapsible">Ensures Tree</button>
    <div class="content">
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                This section displays the Ensures Tree.
                It displays the ensured predicate dependencies starting from ${rule.composedClassName}
                The direction to read is from top to bottom.
                For e.g. ${rule.composedClassName} can ensure something for ...
                Furthermore, it shows for the next depending classes as well.
                There is one special case:
                1. The class ensures a predicate for itself. This is not displayed due to the tree nature.
            </p>
        </div>
        <div class="fortree">
            <ul class="tree">
                <#macro ensTree treenode>
                    <li>
                        <span> <a href="${treenode.data}.html">${treenode.data}</a> </span>
                        <#if treenode.children?has_content>
                            <ul>
                                <#list treenode.children as child>
                                    <@ensTree child />
                                </#list>
                            </ul>
                        </#if>
                    </li>
                </#macro>
                <@ensTree ensures />
            </ul>
        </div>
    </div>
</#if>
<#if booleanD>
    <button class="collapsible">CrySL Rule</button>
    <div class="content">
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                A CrySL rule consists always of the following sections:
                <b>SPEC</b> defines the fully qualified name.
                <b>OBJECTS</b> defines variable names and their type.
                <b>EVENTS</b> defines all methods that contribute to call the class secure.
                <b>ENSURES</b> defines what predicates the class provides.
                The following sections are optional:
                <b>FORBIDDEN</b> defines which methods are not to be called and what method instead.
                <b>REQUIRES</b> defines what predicates are necessary for the class.
                <b>NEGATES</b> defines predicates that are no longer ensured after using the class.
                There are several functions to allow easier specification:
                The first three are used to extract algorithm/mode/padding from transformation String.
                <b>alg(transformation)</b> extract algorithm from .getInstance call.
                <b>mode(transformation)</b> extract mode from .getInstance call.
                <b>padding(transformation)</b> extract padding from .getInstance call.
                <b>length(object)</b> retrieve length of object.
                <b>nevertypeof(object, type)</b> forbid object to be type.
                <b>callTo(method)</b> require call to method.
                <b>noCallTo(method)</b> forbid call to method.
            </p>
        </div>
        <p class="pre" style="white-space: pre-line;">The CrySL rule on <a target="_blank" rel="noopener noreferrer"
                                                                           href=https://github.com/CROSSINGTUD/Crypto-API-Rules/blob/master/JavaCryptographicArchitecture/src/${rule.onlyRuleName}.crysl>Github</a>.
        </p>
        <iframe src="../rules/${rule.onlyRuleName}.crysl"
        onload='(function(o){o.style.height=o.contentWindow.document.body.scrollHeight+"px";}(this));'
        style="height:100%;width:100%;border:none;overflow:hidden;"></iframe>
    </div>
</#if>
<script>
    var coll = document.getElementsByClassName("collapsible");
    var i;
    window.toggleAll = 1;
    var toggleAllBtn = document.getElementById("toggleAl");

    for (i = 0; i < coll.length; i++) {
        coll[i].addEventListener("click", function () {
            this.classList.toggle("active");
            var content = this.nextElementSibling;
            if (content.style.display === "block") {
                content.style.display = "none";
            } else {
                content.style.display = "block";
            }
        });
    }

    for (i = 0; i < coll.length; i++) {
        coll[i].click();
    }
    toggleAllBtn.click();
    coll[0].click();

    function toggleAl() {
        var btn = document.getElementById("toggleAl");
        if (btn.value === "Collapse All") {
            btn.value = "Expand All";
            btn.innerHTML = 'Expand All';
        } else {
            btn.value = "Collapse All";
            btn.innerHTML = 'Collapse All';
        }
        var conent = document.getElementsByClassName("content");
        for (i = 0; i < coll.length; i++) {
            if (window.toggleAll === 1) {
                if (conent[i].style.display === "block") {
                    coll[i].click();
                }
            } else {
                if (conent[i].style.display === "none") {
                    coll[i].click();
                }
            }
        }
        window.toggleAll = window.toggleAll === 0 ? 1 : 0;
    }

    function toggle() {
        var btn = document.getElementById("toggleBtn");
        if (btn.value === "Hide Help") {
            btn.value = "Show Help";
            btn.innerHTML = 'Show Help';
        } else {
            btn.value = "Hide Help";
            btn.innerHTML = 'Hide Help';
        }
        var spolier = document.getElementsByClassName('spoiler');
        for (i = 0; i < spolier.length; i++) {
            if (spolier[i].style.display === "none") {
                spolier[i].style.display = "block";
            } else {
                spolier[i].style.display = "none";
            }
        }
    }
</script>
</body>
</html>