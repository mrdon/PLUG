This is where we keep the source for test jars. Normally you don't have
to look in here, because the jars will be built and put in the main
tree as binaries.

The process is a bit too manual, because I don't foresee it needing to
be done more than once every six months or so.

1. 'maven jar' in simpletest, and copy the results into src/test/classLoadingTestFiles/plugins
2. unjar the jar you made in 1 into a temp dir
3. 'maven jar' in innerjarone and innerjartwo and copy the results into META-INF/lib
4. re-jar it as atlassian-plugins-innertest-1.0.jar
