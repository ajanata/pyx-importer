Card import for Pretend You're Xyzzy.

Given a spreadsheet of input card text and deck names (currently only xlsx supported), produces database output suitable for use with PYX. Any Hibernate dialect that PYX works with should also work here (only tested with Postgres and SQLite).


CONFIGURATION:

Copy importer.template.properties to importer.properties and fill in, at the minimum, the hibernate and import blocks. If it WARNs about any unhandled special characters, add them to the replace block to replace them with HTML entities (it'll probably work if they're Unicode literals, but for maximum compatibility PYX prefers everything that isn't in the basic Latin character set to be entities... except for deck watermarks (see below), as those have a lower maximum character limit).

The deckinfo block is optional, but makes the decks "look nicer" in PYX. You can also assign multiple ids to the same name to combine them, if your input source isn't self-consistent.


BUILDING:

You must have PYX installed in your local Maven repository. This is most easily accomplished by checking it out, and running ```mvn clean install```. The Hibernate ORM classes are used directly from that project.

After that, it should be a simple ```mvn clean package``` to produce a fat jar.


RUNNING:

```java -jar target/pyx-importer-0.0.1-SNAPSHOT-jar-with-dependencies.jar``` will run with default options.


OPTIONS:

```
-c, --configuration <File: filename>  Configuration file to use. (default:
                                        importer.properties)
--format [Boolean]                    Process rich-text formatting for card
                                        text. (default: true)
-h, --help                            Print this usage information.
--save [Boolean]                      Save parse results to database. (default:
                                        true)
--schema                              Output the required database schema and
                                        exit.
```
