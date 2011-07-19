/**
   <h2>OBO Release Manager<h2>

   This is an ant based command line tool which produces ontologies
   versions releases.  The command 'bin/ontology-release-runner'
   builds an ontology release. This tool is supposed to be run from
   the location where a particular ontology release are to be
   maintained.  In the process of producing a particular release this
   tool labels the release with a auto generated version id.  The
   version id is maintained in the VERSION-INFO file. All files
   produced for a particular release are assembled in the directory of
   name by the date of the release. Run the
   'bin/ontology-release-runner --h' tool with the --h option to get
   help which parameter to pass the tool.  To build the latest jar for
   the ontology release manager run the 'ant ontology-release-jar' ant
   command.  '

   <h3>Building the release manager</h3>

   Eventually this will have its own installer.

   For now, make sure you have the most up to date version:

   <pre>
   cd OWLTools
   svn update
   </pre>

   The oboformat.jar should be up to date - but to be sure:

   <pre>
   cp ../oboformat/lib/oboformat.jar runtime/
   </pre>

   Developers do this to build (for the OWLTools dir):

   <pre>
   ant ontology-release-jar
   </pre>

   Then run like this:
   <pre>
   java -Xmx2524M -classpath $PATH_TO_OWLTOOLS/OWLTools/runtime/ontologyrelease.jar owltools.ontologyrelease.OboOntologyReleaseRunner [ARGS]
   </pre>


 */
package owltools.ontologyrelease;

import owltools.ontologyrelease.OboOntologyReleaseRunner;
