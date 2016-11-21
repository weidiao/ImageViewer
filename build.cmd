mkdir bin
javac -encoding utf8 -d bin ImageViewer.java
cd bin
jar cvfm ../haha.jar manifest.txt *
cd ..