# CogniCrypt_DOC

 Bachelor Thesis Topic:
 ```
 Improving Documentation Generation for Cryptographic APIs - A reinterpretation of CogniCryptDOC
 ```

    
 Master Thesis Topic : 
 
        CogniCrypt_DOC
    Transforming API Usage
    Specification to API Documentation

This project generates html-based documentation utilsing a domain specific language called CrySL.
The "Output" folder contains the generated documentation of the master thesis implementation for each class.

## Build
CogniCryptDOC uses Maven as build tool. You can compile and build this project via

```mvn clean install```

The jar is found in the generated target folder.

## Usage

CogniCryptDOC requires four arguments:

```
java -jar <path-to-docgen-jar> 
      --rulesDir <absolute-path-to-crysl-source-code-format-rules> 
      --FTLtemplatesPath <absolute_path_to_ftl_templates>
      --LANGtemplatesPath <absolute_path_to_lang_templates>
      --reportPath <absolute_path_to_generate_documentation>
```

Other additional arguments:

```
--booleanA <To hide state machine graph>
--booleanB <To hide help>
--booleanC <To turn of graphviz generation
--booleanD <To hide dependency trees>
--booleanE <To hide CrySL rule>
--booleanF <To copy CrySL rules into documentation folder>
```

Sets of natural language templates, FTL templates and CrySL rules can be found in `src/main/resources/`
The generated documentation can be viewed by a browser. <absolute_path_to_generate_documentation>/`rootpage.html` is the entry point of the documentation.
