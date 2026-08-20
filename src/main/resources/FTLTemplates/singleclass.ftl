<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <style>

        * {
            font-family: "Source Sans Pro", "Helvetica Neue", Arial, sans-serif;
        }
        .toggle-icon {
            cursor: pointer;
            user-select: none;
            display: inline-block;
            width: 18px;
            text-align: center;
            position: absolute !important;
            left: 0;
            margin: 0 !important;
            top: 50%;
            transform: translateY(-50%);

            /* prevent it from looking like a node-box */
            border: none !important;
            border-radius: 0 !important;
            padding: 0 !important;
            background: transparent !important;
            line-height: 1;
        }

        /* keep alignment for leaf nodes (empty toggle icon) */
        .toggle-icon.is-placeholder {
            visibility: hidden;
            pointer-events: none;
        }

        /* kill the connector "stem" coming from .tree span:before */
        .toggle-icon:before {
            content: none !important;
            display: none !important;
        }

        .tree-node a {
            text-decoration: none;
            color: inherit;
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
            text-align: center;
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

        .tree-node-wrap {
            display: inline-block;
            margin: 0 .2em .5em;
            position: relative;
            /* reserve left space for +/- icon and right space to keep node text centered */
            padding-left: 24px;
            padding-right: 24px;
        }

        .tree code,
        .tree-node {
            border: solid .1em #666;
            border-radius: .2em;
            display: inline-block;
            padding: .2em .5em;
            position: relative;
        }

        .tree ul:before,
        .tree code:before,
        .tree-node:before {
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
        .tree-node:before {
            top: -.55em;
        }

        .tree > li {
            margin-top: 0;
        }

        .tree > li:before,
        .tree > li:after,
        .tree > li > code:before,
        .tree > li > .tree-node-wrap > .tree-node:before {
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
            overflow-x: auto;
            position: relative;
        }

        .tree-controls {
            position: sticky;
            left: 0;
            top: 0;
            z-index: 2;
            display: flex;
            justify-content: flex-end;
            margin: 0.25em 0 0.5em;
            width: 100%;
            box-sizing: border-box;
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

        .copy-btn {
            position: absolute;
            top: 5px;
            right: 10px;
            background-color: #444;
            color: #fff;
            border: none;
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 0.9em;
            cursor: pointer;
            z-index: 2;
        }
        .copy-btn:hover {
            background-color: #666;
        }

        .code-block-container {
            position: relative;
            margin-top: 0.5em;
        }

        .llm-code-block {
            margin: 0;
            padding: 1em;
            border-radius: 5px;
            border-left: 4px solid #666;
            background: #f8f8f8;
            overflow-x: auto;
            white-space: pre;
            tab-size: 4;
            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
            font-size: 13px;
            line-height: 1.45;
        }

        .llm-code-block code {
            display: block;
            font-family: inherit;
            white-space: inherit;
        }

        .llm-code-block.secure {
            background: #f6fff6;
            border-left-color: #3c763d;
        }

        .llm-code-block.insecure {
            background: #fff6f6;
            border-left-color: #a94442;
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

        .llm-explanation {
            display: none;
        }

        .language-selector {
            margin-bottom: 15px;
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 10px;
        }

        .language-selector label {
            font-weight: bold;
            margin: 0;
        }

        .language-selector select {
            padding: 5px 10px;
            border-radius: 3px;
            border: 1px solid #ccc;
            background-color: white;
            min-width: 120px;
        }
        /* Add these styles to ensure consistent font sizes */
        h1, h2, h3, h4, h5, h6 {
            font-family: "Source Sans Pro", "Helvetica Neue", Arial, sans-serif !important;
            font-size: 1.2rem !important;
            margin: 0.5em 0 !important;
            font-weight: bold !important;
        }

        /* Update the existing LLM markdown styles to be more specific */
        #llm-md h1,
        #llm-md-en h1 {
            font-size: 1.2rem !important;
            margin: 0.5em 0 !important;
        }

        #llm-md h2,
        #llm-md-en h2 {
            font-size: 1.1rem !important;
            margin: 0.5em 0 !important;
        }

        #llm-md h3,
        #llm-md-en h3 {
            font-size: 1.05rem !important;
            margin: 0.4em 0 !important;
        }

        #llm-md p,
        #llm-md td,
        #llm-md th,
        #llm-md-en p,
        #llm-md-en td,
        #llm-md-en th {
            font-size: 0.95rem !important;
            line-height: 1.4 !important;
        }

        /* Ensure all paragraph text has consistent sizing */
        p, .pre {
            font-size: 0.95rem !important;
            font-family: "Source Sans Pro", "Helvetica Neue", Arial, sans-serif !important;
        }

        #llm-md-pt h1,
        #llm-md-pt h2,
        #llm-md-pt h3,
        #llm-md-pt h4,
        #llm-md-pt h5,
        #llm-md-pt h6 {
            font-size: 1.2rem !important;
            margin: 0.5em 0 !important;
            font-weight: bold !important;
        }

        #llm-md-pt p,
        #llm-md-pt td,
        #llm-md-pt th {
            font-size: 0.95rem !important;
            line-height: 1.4 !important;
        }

        /* Render LLM markdown as normal flow text (avoid preserving extra blank lines). */
        #llm-md,
        #llm-md-en,
        #llm-md-pt,
        #llm-md-de,
        #llm-md-fr {
            white-space: normal !important;
            line-height: 1.55 !important;
        }

        #llm-md h1,
        #llm-md h2,
        #llm-md h3,
        #llm-md h4,
        #llm-md h5,
        #llm-md h6,
        #llm-md-en h1,
        #llm-md-en h2,
        #llm-md-en h3,
        #llm-md-en h4,
        #llm-md-en h5,
        #llm-md-en h6,
        #llm-md-pt h1,
        #llm-md-pt h2,
        #llm-md-pt h3,
        #llm-md-pt h4,
        #llm-md-pt h5,
        #llm-md-pt h6,
        #llm-md-de h1,
        #llm-md-de h2,
        #llm-md-de h3,
        #llm-md-de h4,
        #llm-md-de h5,
        #llm-md-de h6,
        #llm-md-fr h1,
        #llm-md-fr h2,
        #llm-md-fr h3,
        #llm-md-fr h4,
        #llm-md-fr h5,
        #llm-md-fr h6 {
            margin: 0.75em 0 0.45em !important;
        }

        /* Keep markdown block spacing compact and consistent across languages. */
        #llm-md p,
        #llm-md-en p,
        #llm-md-pt p,
        #llm-md-de p,
        #llm-md-fr p {
            margin: 0.65em 0 !important;
        }

        #llm-md ul,
        #llm-md ol,
        #llm-md-en ul,
        #llm-md-en ol,
        #llm-md-pt ul,
        #llm-md-pt ol,
        #llm-md-de ul,
        #llm-md-de ol,
        #llm-md-fr ul,
        #llm-md-fr ol {
            margin: 0.6em 0 0.9em 1.25em !important;
            padding-left: 1.1em !important;
        }

        #llm-md li,
        #llm-md-en li,
        #llm-md-pt li,
        #llm-md-de li,
        #llm-md-fr li {
            margin: 0.35em 0 !important;
        }

        #llm-md li p,
        #llm-md-en li p,
        #llm-md-pt li p,
        #llm-md-de li p,
        #llm-md-fr li p {
            margin: 0.3em 0 !important;
        }

        /* Preserve readability for markdown code blocks inside explanations. */
        #llm-md pre,
        #llm-md-en pre,
        #llm-md-pt pre,
        #llm-md-de pre,
        #llm-md-fr pre {
            white-space: pre-wrap !important;
            overflow-x: auto !important;
            margin: 0.7em 0 !important;
        }
    </style>
</head>

<body style="background-color: #f1f1f1">
<script src="https://unpkg.com/d3@5.16.0/dist/d3.min.js"></script>
<script src="https://unpkg.com/@hpcc-js/wasm@0.3.11/dist/index.min.js"></script>
<script src="https://unpkg.com/d3-graphviz@3.0.5/build/d3-graphviz.js"></script>

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
    <div class="spoiler" id="spoiler-overview" style="display:none">
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
    <div class="spoiler" id="spoiler-order" style="display:none">
        <p class="help">Help:
            This section describes the secure order call sequences of ${rule.composedClassName}.
            Methods may contain an underscore(_) as a parameter.
            The underscore is a feature of CrySl to help writing CrySL rules for overloaded methods and not specify all overloaded methods in a CrySL rule.
            Conduct the <a target="_blank" rel="noopener noreferrer"
                           href="${rule.javaDocUrl}">JavaDoc</a> to see all parameters in detail.
        </p>
    </div>
    <p class="pre">${rule.numberOfMethods}
    </p>
    <pre style="overflow-x:auto"><#list rule.order as order>${order}
</#list></pre>
    <#if booleanA && booleanE>
        <div class="spoiler" id="spoiler-graph" style="display:none">
            <p class="help">Help:
                This section represents the order of the class as a state machine graph.
                The most left node is always the Start node.
                Double circled nodes are accepting states.
                The edge labels are the necessary methods to transition from one node to the next.
                The graph only shows the secure paths.
                A class is not securely used if method calls deviate from the displayed order.
            </p>
        </div>
        <div id="graph" style="text-align: center; width: 100%; overflow-x: auto; white-space: nowrap;">
        </div>
    </#if>
</div>
<button class="collapsible">Constraints</button>
<div class="content">
    <div class="spoiler" id="spoiler-constraints" style="display:none">
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
    <div class="spoiler" id="spoiler-predicates" style="display:none">
        <p class="help">Help:
            This section describes which Predicates the class ensures.
            Predicates are a construct from CrySL and allow to securely compose several classes depending on use cases.
            A class can ensure predicates for other classes and require predicates from other classes.
            Predicates are ensured after specific method calls or after the method calls seen in the Order section.
        </p>
    </div>
    <p class="pre" style="white-space: pre-line;"><#if rule.ensuresThisPredicates?has_content || rule.ensuresPredicates?has_content || rule.negatesPredicates?has_content>
            <#list rule.ensuresThisPredicates as etp>${etp}
            </#list>
            <#list rule.ensuresPredicates as ep>${ep}
            </#list>
            <#list rule.negatesPredicates as np>${np}
            </#list>
        <#else>
            There are no Predicates for this class.
        </#if>
    </p>

</div>
<#if booleanC>
    <button class="collapsible">Requires Tree</button>
    <div class="content">
        <div class="spoiler" id="spoiler-requires-tree" style="display:none">
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
        <div class="tree-controls">
            <button type="button" class="tree-toggle-btn" onclick="toggleAllTrees(this)">Expand</button>
        </div>
        <ul class="tree">
            <#macro reqTree treenode>
                <li>
                    <span class="tree-node-wrap">
                        <#if treenode.children?has_content>
                            <span class="toggle-icon" onclick="toggleNode(this)">+</span>
                        <#else>
                            <span class="toggle-icon is-placeholder"></span>
                        </#if>

                        <span class="tree-node">
                            <a href="${treenode.data}.html">${treenode.data}</a>
                        </span>
                    </span>

                    <#if treenode.children?has_content>
                        <ul style="display:none;">
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
        <div class="spoiler" id="spoiler-ensures-tree" style="display:none">
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
            <div class="tree-controls">
                <button type="button" class="tree-toggle-btn" onclick="toggleAllTrees(this)">Expand</button>
            </div>
            <ul class="tree">
                <#macro ensTree treenode>
                    <li>
                        <span class="tree-node-wrap">
                            <#if treenode.children?has_content>
                                <span class="toggle-icon" onclick="toggleNode(this)">+</span>
                            <#else>
                                <span class="toggle-icon is-placeholder"></span>
                            </#if>

                            <span class="tree-node">
                                <a href="${treenode.data}.html">${treenode.data}</a>
                            </span>
                        </span>

                        <#if treenode.children?has_content>
                            <ul style="display:none;">
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

<#--<button class="collapsible">LLM Explanation in English</button>-->
<#--<div class="content">-->
<#--    <div class="spoiler" id="llm-spoiler" style="display:none">-->
<#--        <p class="help">-->
<#--            Help: This explanation is generated by a Large Language Model (GPT-4o-mini) based on the CrySL rule for this class.-->
<#--            It gives a natural language summary of how to securely use the API.-->
<#--        </p>-->
<#--    </div>-->

<#--    <#if rule.llmExplanation["English"]?? && rule.llmExplanation["English"]?has_content>-->
<#--        <p class="pre"">-->
<#--${rule.llmExplanation["English"]}-->
<#--        </p>-->
<#--    <#else>-->
<#--        <p><em>No LLM explanation available for this rule.</em></p>-->
<#--    </#if>-->
<#--</div>-->

<#-- 0) Right above your Markdown drop-in, inject some scoped styles -->
<style>
  /* target only inside your LLM container */
  #llm-md h1 { font-size: 1.4rem; margin: 0.6em 0; }
  #llm-md h2 { font-size: 1.2rem; margin: 0.5em 0; }
  #llm-md h3 { font-size: 1.1rem; margin: 0.4em 0; }
  /* optional: tweak paragraph + table text too */
  #llm-md p,
  #llm-md td,
  #llm-md th { font-size: 0.95rem; line-height: 1.4; }
</style>

<#-- Pinned like the three scripts above: an unpinned jsDelivr path serves whatever is
     latest, so a future major release could change marked.parse and silently break every
     rendered explanation in already-published output. -->
<script src="https://cdn.jsdelivr.net/npm/marked@15.0.12/marked.min.js"></script>

<#--<button class="collapsible">LLM Explanation in English</button>-->
<#--<div class="content">-->
<#--  <#if rule.llmExplanation["English"]?? && rule.llmExplanation["English"]?has_content>-->
<#--    <div id="llm-md" style="white-space: pre-wrap;">${rule.llmExplanation["English"]?html}</div>-->
<#--    <script>-->
<#--      document.addEventListener("DOMContentLoaded", ()=>{-->
<#--        const container = document.getElementById("llm-md");-->
<#--        container.innerHTML = marked.parse(container.textContent);-->
<#--      });-->
<#--    </script>-->
<#--  <#else>-->
<#--    <p><em>No LLM explanation available for this rule.</em></p>-->
<#--  </#if>-->
<#--</div>-->

<button class="collapsible">LLM Explanation</button>
<div class="content">
    <div class="spoiler" id="llm-spoiler" style="display:none">
        <p class="help">
            Help: This explanation is generated by a Large Language Model (backend: ${llmBackend}) based on the CrySL rule for this class.
            It gives a natural language summary of how to securely use the API.
        </p>
    </div>

    <p class="help">
        <strong>Disclaimer:</strong> This documentation is automatically generated from a CrySL specification. It reflects the defined method usage rules and constraints. Explanatory notes and security-related guidance are provided for clarity and are not formal guarantees beyond the specification itself.
    </p>

    <div class="language-selector">
        <label for="llm-lang-select">Language:</label>
        <select id="llm-lang-select" onchange="showLLMExplanation()">
            <option value="English">English</option>
            <option value="Portuguese">Portuguese</option>
            <option value="German">German</option>
            <option value="French">French</option>
        </select>
    </div>

    <div id="llm-explanation-English" class="llm-explanation" style="display:block;">
        <#if rule.llmExplanation["English"]?? && rule.llmExplanation["English"]?has_content>
            <div id="llm-md-en">${rule.llmExplanation["English"]?html}</div>
        <#else>
            <p><em>No LLM explanation available for English.</em></p>
        </#if>
    </div>

    <div id="llm-explanation-Portuguese" class="llm-explanation">
        <#if rule.llmExplanation["Portuguese"]?? && rule.llmExplanation["Portuguese"]?has_content>
            <div id="llm-md-pt">${rule.llmExplanation["Portuguese"]?html}</div>
        <#else>
            <p><em>No LLM explanation available for Portuguese.</em></p>
        </#if>
    </div>


    <div id="llm-explanation-German" class="llm-explanation">
        <#if rule.llmExplanation["German"]?? && rule.llmExplanation["German"]?has_content>
            <div id="llm-md-de">${rule.llmExplanation["German"]?html}</div>
        <#else>
            <p><em>No LLM explanation available for German.</em></p>
        </#if>
    </div>

    <!-- French container -->
    <div id="llm-explanation-French" class="llm-explanation">
        <#if rule.llmExplanation["French"]?? && rule.llmExplanation["French"]?has_content>
            <div id="llm-md-fr">${rule.llmExplanation["French"]?html}</div>
        <#else>
            <p><em>No LLM explanation available for French.</em></p>
        </#if>
    </div>
</div>


<#--<button class="collapsible">LLM Explanation in Portuguese</button>-->
<#--<div class="content">-->
<#--    <div class="spoiler" id="llm-spoiler" style="display:none">-->
<#--        <p class="help">-->
<#--            Help: This explanation is generated by a Large Language Model (GPT-4o-mini) based on the CrySL rule for this class.-->
<#--            It gives a natural language summary of how to securely use the API.-->
<#--        </p>-->
<#--    </div>-->

<#--    <#if rule.llmExplanation["Portuguese"]?? && rule.llmExplanation["Portuguese"]?has_content>-->
<#--        <p class="pre" style="white-space: pre-line;">-->
<#--            ${rule.llmExplanation["Portuguese"]}-->
<#--        </p>-->
<#--    <#else>-->
<#--        <p><em>No LLM explanation available for this rule.</em></p>-->
<#--    </#if>-->
<#--</div>-->
<#--<button class="collapsible">LLM Explanation in German</button>-->
<#--<div class="content">-->
<#--    <div class="spoiler" id="llm-spoiler" style="display:none">-->
<#--        <p class="help">-->
<#--            Help: This explanation is generated by a Large Language Model (GPT-4o-mini) based on the CrySL rule for this class.-->
<#--            It gives a natural language summary of how to securely use the API.-->
<#--        </p>-->
<#--    </div>-->

<#--    <#if rule.llmExplanation["German"]?? && rule.llmExplanation["German"]?has_content>-->
<#--        <p class="pre" style="white-space: pre-line;">-->
<#--            ${rule.llmExplanation["German"]}-->
<#--        </p>-->
<#--    <#else>-->
<#--        <p><em>No LLM explanation available for this rule.</em></p>-->
<#--    </#if>-->
<#--</div>-->
<#--<button class="collapsible">LLM Explanation in French</button>-->
<#--<div class="content">-->
<#--    <div class="spoiler" id="llm-spoiler" style="display:none">-->
<#--        <p class="help">-->
<#--            Help: This explanation is generated by a Large Language Model (GPT-4o-mini) based on the CrySL rule for this class.-->
<#--            It gives a natural language summary of how to securely use the API.-->
<#--        </p>-->
<#--    </div>-->

<#--    <#if rule.llmExplanation["French"]?? && rule.llmExplanation["French"]?has_content>-->
<#--        <p class="pre" style="white-space: pre-line;">-->
<#--            ${rule.llmExplanation["French"]}-->
<#--        </p>-->
<#--    <#else>-->
<#--        <p><em>No LLM explanation available for this rule.</em></p>-->
<#--    </#if>-->
<#--</div>-->

<button class="collapsible">LLM Code Examples</button>
<div class="content">
    <div class="spoiler" id="llm-code-spoiler" style="display:none">
        <p class="help">
            Help: These code examples are generated by a Large Language Model (backend: ${llmBackend}) based on the CrySL rule for this class.<br>
            The <b>secure example</b> shows correct usage of the API, while the <b>insecure example</b> demonstrates a misuse pattern.<br>
            These are automatically generated and should be reviewed before use in production.
        </p>
    </div>

    <#if rule.secureExample?? && rule.secureExample?has_content>
        <h4>Secure Example</h4>
        <div class="code-block-container">
            <button class="copy-btn" onclick="copyToClipboard(this)">Copy</button>
            <pre class="llm-code-block secure"><code class="language-java">${rule.secureExample?html}</code></pre>
        </div>
    <#else>
        <p><em>No secure example available for this rule.</em></p>
    </#if>

    <#if rule.insecureExample?? && rule.insecureExample?has_content>
        <h4>Insecure Example</h4>
        <details>
            <summary style="cursor:pointer; font-weight:bold;">Click to reveal insecure example</summary>
            <div class="code-block-container">
                <button class="copy-btn" onclick="copyToClipboard(this)">Copy</button>
                <pre class="llm-code-block insecure"><code class="language-java">${rule.insecureExample?html}</code></pre>
            </div>
        </details>
    <#else>
        <p><em>No insecure example available for this rule.</em></p>
    </#if>
</div>

<#if booleanD>
    <button class="collapsible">CrySL Rule</button>
    <div class="content">
        <div class="spoiler" id="spoiler-crysl-rule" style="display:none">
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
                                                                           href="https://github.com/CROSSINGTUD/Crypto-API-Rules/blob/master/JavaCryptographicArchitecture/src/${rule.onlyRuleName}.crysl">Github</a>.
        </p>
        <#if rule.cryslRuleText?? && rule.cryslRuleText?has_content>
            <pre class="pre" style="background:#f4f4f4;padding:1em;border-radius:8px;white-space:pre-wrap;">${rule.cryslRuleText?html}</pre>
        <#else>
            <p><em>Raw CrySL rule text not available.</em></p>
        </#if>
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
                // Ensure first descendants are visible when a tree section opens.
                // Scoped to this section: a global call here silently undid the
                // user's manual collapses in every other tree on the page.
                if (content.querySelector && content.querySelector("ul.tree")) {
                    expandFirstTreeLevel(content);
                }
            }
        });
    }

    for (i = 0; i < coll.length; i++) {
        coll[i].click();
    }
    toggleAllBtn.click();
    coll[0].click();

    function toggleNode(element) {
        // element is the +/- span inside the node wrapper
        var li = element.closest("li");
        if (!li) return;

        // Find direct child <ul> (the subtree)
        var childUl = null;
        for (var j = 0; j < li.children.length; j++) {
            var el = li.children[j];
            if (el.tagName && el.tagName.toLowerCase() === "ul") {
                childUl = el;
                break;
            }
        }
        if (!childUl) return;

        if (childUl.style.display === "none") {
            childUl.style.display = "";   // revert to CSS default (your .tree ul uses display: table)
            element.textContent = "−";
        } else {
            childUl.style.display = "none";
            element.textContent = "+";
        }
    }

    // Expand only the first descendants of each tree by default.
    function expandFirstTreeLevel(scope) {
        var root = scope || document;
        var trees = root.querySelectorAll("ul.tree");
        for (var t = 0; t < trees.length; t++) {
            var rootLis = trees[t].children;
            for (var r = 0; r < rootLis.length; r++) {
                var li = rootLis[r];
                var childUl = null;
                var toggleIcon = li.querySelector(".tree-node-wrap > .toggle-icon");
                for (var c = 0; c < li.children.length; c++) {
                    var el = li.children[c];
                    if (el.tagName && el.tagName.toLowerCase() === "ul") {
                        childUl = el;
                    }
                }
                if (childUl) {
                    childUl.style.display = "";
                    if (toggleIcon) {
                        toggleIcon.textContent = "−";
                    }
                }
            }
        }
    }

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

    function showLLMExplanation() {
        var lang = document.getElementById('llm-lang-select').value;
        var explanations = document.getElementsByClassName('llm-explanation');

        // Hide all explanations
        for (var i = 0; i < explanations.length; i++) {
            explanations[i].style.display = 'none';
        }

        // Show selected language explanation
        var selectedExplanation = document.getElementById('llm-explanation-' + lang);
        if (selectedExplanation) {
            selectedExplanation.style.display = 'block';

            // Render markdown for English
            if (lang === 'English' && document.getElementById('llm-md-en')) {
                const container = document.getElementById('llm-md-en');
                if (container && !container.getAttribute('data-rendered')) {
                    container.innerHTML = marked.parse(container.textContent);
                    container.setAttribute('data-rendered', 'true');
                }
            }

            // Render markdown for Portuguese
            if (lang === 'Portuguese' && document.getElementById('llm-md-pt')) {
                const container = document.getElementById('llm-md-pt');
                if (container && !container.getAttribute('data-rendered')) {
                    container.innerHTML = marked.parse(container.textContent);
                    container.setAttribute('data-rendered', 'true');
                }
            }

            // Render markdown for German
            if (lang === 'German' && document.getElementById('llm-md-de')) {
                const container = document.getElementById('llm-md-de');
                if (container && !container.getAttribute('data-rendered')) {
                    container.innerHTML = marked.parse(container.textContent);
                    container.setAttribute('data-rendered', 'true');
                }
            }

            // Render markdown for French
            if (lang === 'French' && document.getElementById('llm-md-fr')) {
                const container = document.getElementById('llm-md-fr');
                if (container && !container.getAttribute('data-rendered')) {
                    container.innerHTML = marked.parse(container.textContent);
                    container.setAttribute('data-rendered', 'true');
                }
            }
        }
    }


    function copyToClipboard(button) {
        const codeBlock = button.nextElementSibling.querySelector('code');
        const text = codeBlock.innerText;

        function done(ok, err) {
            if (ok) {
                button.innerText = 'Copied!';
                setTimeout(() => { button.innerText = 'Copy'; }, 1500);
            } else {
                console.error('Failed to copy code:', err);
                button.innerText = 'Error';
                setTimeout(() => { button.innerText = 'Copy'; }, 1500);
            }
        }

        // navigator.clipboard only exists in secure contexts (https/localhost).
        // These pages are normally opened over file://, where it is undefined and
        // the call throws synchronously - so feature-detect and fall back.
        if (navigator.clipboard && window.isSecureContext) {
            try {
                navigator.clipboard.writeText(text).then(() => done(true), (err) => done(false, err));
                return;
            } catch (err) {
                // fall through to the legacy path below
            }
        }

        const helper = document.createElement('textarea');
        helper.value = text;
        helper.setAttribute('readonly', '');
        helper.style.position = 'fixed';
        helper.style.top = '-1000px';
        document.body.appendChild(helper);
        helper.select();
        try {
            done(document.execCommand('copy'), 'execCommand returned false');
        } catch (err) {
            done(false, err);
        } finally {
            document.body.removeChild(helper);
        }
    }

    function expandAllTrees(scope) {
        var root = scope || document;
        var trees = root.querySelectorAll("ul.tree");
        for (var t = 0; t < trees.length; t++) {
            var uls = trees[t].querySelectorAll("ul");
            for (var u = 0; u < uls.length; u++) {
                uls[u].style.display = "";
            }
            var icons = trees[t].querySelectorAll(".toggle-icon");
            for (var i = 0; i < icons.length; i++) {
                if (icons[i].textContent !== "") {
                    icons[i].textContent = "−";
                }
            }
        }
    }

    function toggleAllTrees(button) {
        if (!button) return;
        var scope = button.closest(".content") || document;
        var isExpanded = button.textContent.trim() === "Collapse";
        if (isExpanded) {
            collapseAllTrees(scope);
            button.textContent = "Expand";
        } else {
            expandAllTrees(scope);
            button.textContent = "Collapse";
        }
    }

    function collapseAllTrees(scope) {
        var root = scope || document;
        var trees = root.querySelectorAll("ul.tree");
        for (var t = 0; t < trees.length; t++) {
            var uls = trees[t].querySelectorAll("ul");
            for (var u = 0; u < uls.length; u++) {
                uls[u].style.display = "none";
            }
            var icons = trees[t].querySelectorAll(".toggle-icon");
            for (var i = 0; i < icons.length; i++) {
                if (icons[i].textContent !== "") {
                    icons[i].textContent = "+";
                }
            }
        }
    }

    <#if booleanA && booleanE>
    var dotString = `
        ${stateMachine}
`;

    // Two non-obvious requirements, both needed for the diagram to render at all:
    //
    // 1. wasmFolder() must be set WITHOUT a trailing slash. @hpcc-js/wasm 0.3.11 builds the
    //    URL as (folder || scriptDir) + "/" + "graphvizlib.wasm", and scriptDir already ends
    //    in "/", so the default yields ".../dist//graphvizlib.wasm" - which 404s and, being a
    //    404, carries no CORS header either.
    // 2. useWorker:false. The worker path re-derives the folder with vizURL.match(/.*\//),
    //    which keeps the trailing slash and reintroduces the same broken URL, overriding
    //    whatever was set on the main thread.
    //
    // With both applied the graph renders over http:// and file:// alike.
    window["@hpcc-js/wasm"].wasmFolder("https://unpkg.com/@hpcc-js/wasm@0.3.11/dist");

    d3.select("#graph").graphviz({useWorker: false})
        .renderDot(dotString);
    </#if>

    document.addEventListener("DOMContentLoaded", function() {
        const englishContainer = document.getElementById('llm-md-en');
        if (englishContainer && !englishContainer.getAttribute('data-rendered')) {
            englishContainer.innerHTML = marked.parse(englishContainer.textContent);
            englishContainer.setAttribute('data-rendered', 'true');
        }
    });


</script>
</body>
</html>
