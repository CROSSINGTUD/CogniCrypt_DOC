# CogniCrypt_DOC
This repo contains the implementation of CogniCrypt_DOC. The project generates html-based documentation utilising a domain specific language called CrySL. The master thesis developed a prototype that generates natural language documentation based on CrySL rules and templates. The Bachelor thesis built on top of that prototype to improve the tool and the generated documentation by utilising FTL templates.

The html-based documentation can be found in the zip file `generated_doc_and_code_example`. The entrypoint of the documentation is the `rootpage.html` file.

 Bachelor Thesis Topic:
 ```
 Improving Documentation Generation for Cryptographic APIs - A reinterpretation of CogniCryptDOC
 ```

    
 Master Thesis Topic : 
 
        CogniCrypt_DOC
    Transforming API Usage
    Specification to API Documentation

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

By default are all features of the html-based documentation enabled. To turn off features speficy the following additional arguments:

```
--booleanA <To hide state machine graph>
--booleanB <To hide help button>
--booleanC <To hide dependency trees sections>
--booleanD <To hide CrySL rule section>
--booleanE <To turn of graphviz generation>
--booleanF <To copy CrySL rules into documentation folder>
```

Sets of natural language templates, FTL templates and CrySL rules can be found in `src/main/resources/`
The generated documentation can be viewed by a browser. <absolute_path_to_generate_documentation>/`rootpage.html` is the entry point of the documentation.
