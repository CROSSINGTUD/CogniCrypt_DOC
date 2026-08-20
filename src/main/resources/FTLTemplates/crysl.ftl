<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <title>How CrySL works</title>
    <style>
        * { font-family: "Source Sans Pro","Helvetica Neue",Arial,sans-serif; box-sizing: border-box; }
        body { margin: 0; }
        .page { background-color:#f1f1f1; padding:18px 22px; }
        .card { background:#fff; border:1px solid #ddd; padding:14px 16px; margin:12px 0; border-radius: 6px; }
        h1 { margin: 0 0 8px 0; }
        h2 { margin: 0 0 8px 0; }
        p  { margin: 8px 0; line-height: 1.45; }
        ul { margin: 8px 0 8px 18px; }
        code, pre { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }
        pre { background:#fafafa; border:1px solid #eee; padding:10px 12px; overflow:auto; border-radius: 6px; }
        .note { border-left: 4px solid #888; padding-left: 10px; }
        .pill { display:inline-block; padding:2px 8px; border:1px solid #ddd; border-radius: 999px; background:#fafafa; font-size: 12px; }
    </style>
</head>
<body>
<div class="page">
    <h1>How CrySL works (how to read this documentation)</h1>

    <div class="card">
        <p>
            <b>CrySL</b> is a specification language that lets cryptography experts describe <b>correct and secure usages</b>
            of cryptographic APIs (e.g., Java JCA). Tools can then use these rules to support developers—e.g., for
            documentation, checking, and guidance.
        </p>

        <div class="note">
            <p style="margin-top:0;">
                <b>Important mental model (Whitelist / Positive Listing):</b>
                CrySL is designed to specify <b>secure uses explicitly</b>, and it generally assumes that
                <b>deviations</b> from the specified “secure norm” are insecure or at least not guaranteed by the rule.
                So if something is <b>not mentioned</b>, it is <b>not</b> automatically “ok”—it is simply <b>not covered</b>
                by the rule’s secure specification.
            </p>
        </div>
        <p style="margin-bottom:0;">
            This design choice keeps CrySL rules concise: in crypto there are many misuses, but comparatively few secure patterns.
        </p>
    </div>

    <div class="card">
        <h2>Core idea: One rule per class</h2>
        <p>
            A CrySL rule is written for exactly one reference type (class/interface), declared via <code>SPEC</code>.
            The rule is split into sections (separation of concerns) so the secure usage is readable and reusable.
        </p>
        <ul>
            <li><b>SPEC</b>: the class this rule describes.</li>
            <li><b>OBJECTS</b>: typed variables used in later sections (includes primitives like <code>int</code> and objects like <code>String</code>).</li>
            <li><b>EVENTS</b>: method “event patterns” (labeled) that matter for correct usage (can include return binding).</li>
            <li><b>ORDER</b>: a usage pattern as a <b>regular expression</b> over those events (typestate).</li>
            <li><b>CONSTRAINTS</b>: restrictions on parameter values (strings/integers), including implication constraints (<code>A =&gt; B</code>).</li>
            <li><b>ENSURES</b>: predicates guaranteed after correct usage (for interactions across classes).</li>
        </ul>
        <p style="margin-bottom:0;">
            Optional sections: <b>REQUIRES</b>, <b>FORBIDDEN</b>, <b>NEGATES</b> (explained below).
        </p>
    </div>

    <div class="card">
        <h2>EVENTS: method patterns, labels, aggregates</h2>
        <p>
            In <code>EVENTS</code>, each relevant method call pattern gets a <b>label</b> (e.g., <code>g1</code>, <code>i2</code>).
            Patterns can:
        </p>
        <ul>
            <li>Bind parameters using variables from <code>OBJECTS</code></li>
            <li>Use <code>_</code> as a “don’t care” placeholder (e.g., to combine overloads)</li>
            <li>Bind return values (e.g., <code>key = generateKey()</code>)</li>
            <li>Define <b>aggregates</b> as a disjunction of labels (e.g., <code>GetInstance := g1 | g2;</code>)</li>
        </ul>

        <pre>
SPEC javax.crypto.KeyGenerator

OBJECTS
  java.lang.String algorithm;
  int keySize;
  javax.crypto.SecretKey key;

EVENTS
  g1: getInstance(algorithm);
  g2: getInstance(algorithm, _);
  GetInstance := g1 | g2;

  i1: init(keySize);
  i2: init(keySize, _);
  i3: init(_);
  i4: init(_, _);

  Init := i1 | i2 | i3 | i4;


  GenKey: key = generateKey();</pre>
        <p style="margin-bottom:0;">
            Aggregates reduce repetition when writing <code>ORDER</code> patterns.
        </p>
    </div>

    <div class="card">
        <h2>ORDER: typestate as a regular expression</h2>
        <p>
            <code>ORDER</code> defines the allowed call sequence (typestate). It is a <b>regular expression</b> over event labels/aggregates.
            Common operators:
        </p>
        <ul>
            <li><code>A, B</code> (A followed by B)</li>
            <li><code>A | B</code> (A or B)</li>
            <li><code>A?</code> optional</li>
            <li><code>A*</code> 0 or more</li>
            <li><code>A+</code> 1 or more</li>
            <li><code>(...)</code> grouping</li>
        </ul>
        <pre>
ORDER
  GetInstance, Init?, GenKey</pre>
        <p style="margin-bottom:0;">
            Intuition: you must call one of the <code>getInstance</code> variants, optionally <code>init</code>, then <code>generateKey</code>.
        </p>
    </div>

    <div class="card">
        <h2>CONSTRAINTS: allowed parameter values (incl. implications)</h2>
        <p>
            Crypto APIs often take strings/integers that select algorithms, modes, paddings, key sizes, etc.
            CrySL supports constraints like:
        </p>
        <ul>
            <li><code>x in {"AES","Blowfish"}</code></li>
            <li><code>A =&gt; B</code> meaning “if A holds, then B must hold”</li>
        </ul>
        <pre>
CONSTRAINTS
  algorithm in {"AES", "Blowfish"};
  algorithm in {"AES"}      => keySize in {128, 192, 256};
  algorithm in {"Blowfish"} => keySize in {128, 192, 256, 320, 384, 448};</pre>
        <p style="margin-bottom:0;">
            This captures secure choices directly, instead of listing every insecure alternative.
        </p>
    </div>

    <div class="card">
        <h2>Predicates: REQUIRES / ENSURES / NEGATES (interactions across classes)</h2>
        <p>
            CrySL supports rely/guarantee reasoning via <b>predicates</b>.
            A correct usage of one class can <b>ENSURE</b> a predicate that another class can <b>REQUIRE</b>.
        </p>
        <pre>
ENSURES
  generatedKey[key, algorithm];</pre>
        <p>
            Example intuition: if a <code>KeyGenerator</code> is used correctly, it ensures a predicate stating that
            a key was generated for a specific algorithm. A <code>Cipher</code> rule may require that predicate before it can be used securely.
        </p>

        <p>
            Predicates can also be tied to a specific point using <code>after</code> (generate/negate right after some event),
            and existing predicates can be invalidated via <code>NEGATES</code> (useful for lifetimes, e.g., password clearing).
        </p>

        <pre>
ENSURES
  keyspec[this, keylength] after create;

NEGATES
  keyspec[this, _];</pre>
    </div>

    <div class="card">
        <h2>FORBIDDEN: explicitly disallowed calls (and suggested alternatives)</h2>
        <p>
            Although CrySL is mostly whitelist-based, it supports an explicit <code>FORBIDDEN</code> section
            for methods that are always insecure (or should never be used).
            CrySL can also specify an alternative secure event using <code>=&gt;</code> (useful for guidance/fixes/docs).
        </p>

        <pre>
FORBIDDEN
  PBEKeySpec(char[]) => create;
  PBEKeySpec(char[],byte[],int) => create;
  </pre>

        <p style="margin-bottom:0;">
            <b>Meaning:</b> these constructors are forbidden; use the secure <code>create</code> pattern instead
            (the constructor that takes password + salt + iterations + keylength).
        </p>
    </div>


    <div class="card">
        <h2>Built-in helper functions you may see in rules</h2>
        <p>
            CrySL includes small helper functions to express common crypto constraints, e.g. extracting algorithm/mode/padding from a transformation string.
        </p>
        <ul>
            <li><code>alg(transformation)</code>, <code>mode(transformation)</code>, <code>padding(transformation)</code></li>
            <li><code>length(object)</code></li>
            <li><code>neverTypeOf(object, type)</code></li>
            <li><code>callTo(method)</code>, <code>noCallTo(method)</code> (conditional required/forbidden calls)</li>
        </ul>
    </div>

    <div class="card">
        <h2>How to interpret CrySLDoc pages</h2>
        <ul>
            <li>The class page describes <b>the secure usage pattern</b> encoded in the CrySL rule for that class.</li>
            <li>If your code deviates from the described pattern/constraints, it may be insecure or unsupported by the rule.</li>
            <li>Some pages also show <b>secure</b> and <b>insecure</b> examples to clarify typical mistakes.</li>
        </ul>
    </div>

</div>
</body>
</html>
