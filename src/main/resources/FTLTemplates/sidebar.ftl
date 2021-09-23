<html>
<head>
    <title>${title}</title>
    <base target="content">
    <style>
    ul {
    list-style-type: none;
    padding: 0;
    border: 1px hidden #ddd;
    }

    ul li {
    padding: 8px 0px;
    border-bottom: 1px hidden #ddd;
    }

    ul li:last-child {
    border-bottom: none
    }

    input {
        width: 100%;
        padding: 0;
    }
    a {
        padding: 0;
    }
    </style>




</head>

<body style="background-color: #f1f1f1">
<input type="text" id="search" onkeyup="myFunction()" placeholder="Search Classes" title="Type in a class">
<ul id="classes">
    <#list rules as rule>
        <li><a href="composedRules/${rule.composedClassName}.html">${rule.composedClassName}</a></li>
    </#list>
</ul>



<script>
    function myFunction() {
        var input, filter, ul, li, a, i;
        input = document.getElementById("search");
        filter = input.value.toUpperCase();
        ul = document.getElementById("classes");
        li = ul.getElementsByTagName("li");
        for (i = 0; i < li.length; i++) {
            a = li[i].getElementsByTagName("a")[0];
            if (a.innerHTML.toUpperCase().indexOf(filter) > -1) {
                li[i].style.display = "";
            } else {
                li[i].style.display = "none";
            }
        }
    }
</script>
</body>
</html>