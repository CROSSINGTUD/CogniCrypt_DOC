<html>
<head>
    <title>${title}</title>
    <style>
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
            font-family: Montserrat, Helvetica Neue, Helvetica, Arial, sans-serif;
            font-size: 14px;
        }

        pre {
            font-family: Montserrat, Helvetica Neue, Helvetica, Arial, sans-serif;
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
            white-space: pre-line;
            font-family: Montserrat, Helvetica Neue, Helvetica, Arial, sans-serif;
            font-size: 12px;
            border: 2px solid #555;
            display: inline-block;

        }

        .spoiler {
            font-family: Montserrat, Helvetica Neue, Helvetica, Arial, sans-serif;
            font-size: 14px;
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
    <p class="pre" style="white-space: pre-line;">This document page for <b>${rule.composedClassName}</b> consists of the following sections:
        <b>Order</b>: Describes how the class has to be used<#if booleanA> and a state machine graph</#if>.
        <b>Constraints</b>: Describes all constraints of the class.
        <b>Predicates</b>: Describes the predicates the class provides.
        <#if booleanD><b>Requires Tree</b>: Shows the required predicates tree.
            <b>Ensures Tree</b>: Shows the ensures predicates tree.</#if>
        <#if booleanE><b>CrySL Rule</b>: Provides a link to the CrySL Github and the CrySL rule itself.</#if>
        On the top right is a <b>button</b> that collapses/expands all sections.
        On the bottom right is a <b>button</b> to show descriptions for each section excluding this.

        ${rule.composedLink}
    </p>
</div>
<button class="collapsible">Order</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help">Help:
            This section describes the order, in which the methods of this class can be called.
            And the order represented as a state machine graph.
            The first node is always the Start node.
            The edge labels are the neccessary methods to transition from one node to the next.
            Double circled nodes are accepting states. That means if this node is reached the class was called in a
            secure order.
        </p>
    </div>
    <p class="pre">${rule.numberOfMethods}
    </p>
    <pre style="overflow-x:auto"><#list rule.order as order>${order}
        </#list>
            </pre>
    <#if booleanA>
        <div class="fortree">
            <img src="../dotFSMs/${rule.composedClassName}.svg" style="background-color: #f1f1f1">
        </div>
    </#if>
</div>
<button class="collapsible">Constraints</button>
<div class="content">
    <div class="spoiler" id="spoiler" style="display:none">
        <p class="help">Help:
            This section describes the parameters, which have constraints.
            And the parameters that require a predicate from another class.
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;"><#if rule.allConstraints?has_content>
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
            This section describes what Predicates the class provides.
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;"><#list rule.ensuresThisPredicates as etp>
            ${etp}
        </#list>
        <#list rule.ensuresPredicates as ep>
            ${ep}
        </#list>
        <#list rule.negatesPredicates as np>
            ${np}
        </#list>
    </p>
    <#if booleanD>
</div>
    <button class="collapsible">Requires Tree</button>
    <div class="content">
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                This section displays the Requires Tree.
                The root of the tree is always the currently viewed class.
                The read direction is from top to bottom.
                For e.g. ${rule.composedClassName} requires something from ...
                Furthermore, it shows for the next depending classes as well.
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
                The root of the tree is always currently viewed class.
                The direction to read is from top to bottom.
                For e.g. ${rule.composedClassName} ensures something for ...
                Furthermore, it shows for the next depending classes as well.
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
<#if booleanE>
    <button class="collapsible">CrySL Rule</button>
    <div class="content">
        <div class="spoiler" id="spoiler" style="display:none">
            <p class="help">Help:
                CrySLrule
            </p>
        </div>
        <p class="pre" style="white-space: pre-line;">The CrySL rule on <a target="_blank" rel="noopener noreferrer"
                                                                           href=https://github.com/CROSSINGTUD/Crypto-API-Rules/blob/master/JavaCryptographicArchitecture/src/${rule.onlyRuleName}.crysl>Github</a>.
        </p>
        <iframe src="${pathToRules}/${rule.onlyRuleName}.crysl"
                onload='javascript:(function(o){o.style.height=o.contentWindow.document.body.scrollHeight+"px";}(this));'
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