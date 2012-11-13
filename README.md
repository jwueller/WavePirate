WavePirate
==========

Utility for extracting RIFF WAVE files from raw binary data like memory dumps.

Build
-----

Use Apache Ant to build the JAR by running the following command in the project
directory:

    ant clean jar

Usage
-----

Run this command to extract all wave files from the source-file. If the output
directory is omitted, all extracted files are placed in the current working
directory.

    java -jar WavePirate.jar source-file [output-directory]
