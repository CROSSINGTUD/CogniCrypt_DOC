<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<style>
    .section {
        border-bottom-width: 1px;
        border-bottom-style: solid;
        border-bottom-color: #777;

    }

    .section:last-of-type{
        border-bottom-width: 0;
    }

    a[target="_blank"]::after {
        content: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVR42qXKwQkAIAxDUUdxtO6/RBQkQZvSi8I/pL4BoGw/XPkh4XigPmsUgh0626AjRsgxHTkUThsG2T/sIlzdTsp52kSS1wAAAABJRU5ErkJggg==);
        margin: 0 3px 0 5px;
    }
</style>
<body style="background-color: #f1f1f1">
<div class="frontpage">
    <div class="section">
        <h1>Documentation generated by CogniCrypt<sub>DOC</sub></h1>
        <p>This documentation was created by CogniCrypt<sub>DOC</sub> based on CrySL rules.</p>
        <h2>First Steps</h2>
        <p>Are you new to documentation generated by CogniCrypt<sub>DOC</sub>? Read the following instructions to get
            started!</p>
        <p>Click on the left to read the documentation for a single class.</p>
    </div>
    <div class="section">
        <h2>Getting Help</h2>
        <p>Something is unclear? Press the Help Button on the bottom right of a class documentation to get explanations!</p>
    </div>
    <div class="section">
        <h2>How the class documentation is organized</h2>
        <p>The documentation of a class follows always the same structure:</p>
        <ul>
            <li><b>Overview</b> provides the name and a link to the JavaDoc of the class.</li>
            <li><b>Order</b> provides a description how to securely call the methods of the class.</li>
            <li><b>Constraints</b> explains different constraints for the class.</li>
            <li><b>Predicates</b> displays what predicates the class provides.</li>
            <li><b>Requires Tree</b> displays the possible required predicate dependencies starting from the class.</li>
            <li><b>Ensures Tree</b> displays the possible ensuring predicate dependencies starting from the class.</li>
            <li><b>CrySL Rule</b> displays the CrySL rule itself.</li>
        </ul>
    </div>
    <div class="section">
        <h2>Other Helpful Resources</h2>
        <p>Not finding the information you're looking for? Have a look here!</p>
        <ul>
            <li>The official <a target="_blank" rel="noopener noreferrer"
                                   href="https://docs.oracle.com/javase/8/docs/api/java/security/package-summary.html">JavaDoc</a></li>
            <li>The official <a target="_blank" rel="noopener noreferrer"
                                    href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html">JCA Reference Guide</a> </li>
            </ul>
        <p>Want to check, test or generate secure code automatically? Find other use cases based on CrySL rules!</p>
        <ul>
            <li><a target="_blank" rel="noopener noreferrer" href="https://github.com/eclipse-cognicrypt/CogniCrypt">CogniCrypt<sub>GEN</sub></a>:
                Generate Secure Code.
            </li>
            <li><a target="_blank" rel="noopener noreferrer" href="https://github.com/CROSSINGTUD/CryptoAnalysis">CogniCrypt<sub>SAST</sub></a>:
                Analyse Java code for cryptographic misuses.
            </li>
            <li><a target="_blank" rel="noopener noreferrer"
                   href="https://github.com/CROSSINGTUD/CogniCrypt_TESTGEN/tree/develop">CogniCrypt<sub>TESTGEN</sub></a>: Generate
                test cases.
            </li>
        </ul>
    </div>
</div>
</body>
</html>
