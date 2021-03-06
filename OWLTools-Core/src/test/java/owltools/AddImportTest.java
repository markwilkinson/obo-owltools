package owltools;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class AddImportTest extends OWLToolsTestBasics {

	@Test
	public void testMakeImport() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); // persist?
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		IRI iri1 = getResourceIRI("caro_mireot_test_noimport.owl");
		OWLOntology o1 = manager.loadOntologyFromOntologyDocument(iri1);
		IRI iri2 = getResourceIRI("caro.owl");
		OWLOntology o2 = manager.loadOntologyFromOntologyDocument(iri2);
		
		AddImport ai = new AddImport(o1, dataFactory.getOWLImportsDeclaration(iri2));
		manager.applyChange(ai);
		
		for (OWLSubClassOfAxiom a : o1.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			System.out.println(a);
		}
		for (OWLSubClassOfAxiom a : o2.getAxioms(AxiomType.SUBCLASS_OF, true)) {
			System.out.println("O2:"+a);
		}
	}
	
}
