package owltools.graph;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.graph.shunt.OWLShuntEdge;
import owltools.graph.shunt.OWLShuntGraph;
import owltools.graph.shunt.OWLShuntNode;

public class OWLGraphGOTest extends OWLToolsTestBasics {
	
	/*
	 * Testing the some of the relation functions in the graph wrapper on GO.
	 */
	@Test
	public void testGOGraph() throws Exception{
		OWLGraphWrapper wrapper = getOntologyWrapper("go.owl");
		
		// First, let's look at the world of GO:0022008; specifically, the neighborhood above.
		OWLObject x1 = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "GO_0022008");

		// In this loop we're checking that GO:0022008 has two known parents, one is_a and one part_of.
		// Anything else is an error.
		for (OWLGraphEdge e : wrapper.getOutgoingEdges(x1)) {
			OWLObject t = e.getTarget();

			if (t instanceof OWLNamedObject){				

				// Figure out object the bits.
				String objectID = wrapper.getIdentifier(t);
				//String objectLabel = wrapper.getLabel(t);
				String elabel = wrapper.getEdgeLabel(e);

				if( objectID.equals("GO:0030154") ){
					assertEquals("GO:0030154 part_of parent of GO_0022008:", elabel, "is_a");					
				}else if( objectID.equals("GO:0007399") ){
					assertEquals("GO:0007399 part_of parent of GO_0022008:", elabel, "part_of");
				}else{
					assertEquals("not a parent of GO_0022008: " + objectID, false, true);
				}
			}
		}
		
		// First, let's look at the world of GO:0022008; specifically, the neighborhood above.
		OWLObject x2 = wrapper.getOWLClass(OWLGraphWrapper.DEFAULT_IRI_PREFIX + "GO_0007399");

		// In this loop we're checking that GO:0007399, known from above, has GO:0022008 as a part_of child somewhere.
		// Anything else is an error.
		boolean kid_p = false;
		for (OWLGraphEdge e : wrapper.getIncomingEdges(x2)) {
			OWLObject s = e.getSource();

			if (s instanceof OWLNamedObject){				

				// Figure out subject the bits.
				String subjectID = wrapper.getIdentifier(s);
				//String subjectLabel = wrapper.getLabel(s);
				String elabel = wrapper.getEdgeLabel(e);

				if( subjectID.equals("GO:0022008") ){
					assertEquals("GO:0022008 part_of child of GO_0007399 (saw: " + elabel + ")", elabel, "part_of");
					kid_p = true;
				}
			}
		}
		assertEquals("saw GO:0022008 as a child of GO_0007399:", kid_p, true);
	}
	
	private OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getResource(file));
		return new OWLGraphWrapper(ontology);
	}
}