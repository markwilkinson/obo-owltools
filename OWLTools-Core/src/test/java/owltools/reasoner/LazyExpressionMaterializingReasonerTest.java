package owltools.reasoner;

import static org.junit.Assert.*;

import org.junit.Test;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.SimEngine;

/**
 * tests getLeastCommonSubsumerSimpleClassExpression
 * 
 * @author cjm
 *
 */
public class LazyExpressionMaterializingReasonerTest extends AbstractReasonerTest {

	OWLGraphWrapper g;
	SimEngine se;
	OWLPrettyPrinter pp;

	@Test
	public void testLEMR() throws Exception{
		g =  getOntologyWrapper("q-in-e-v1.omn");
		LazyExpressionMaterializingReasonerFactory rf = new LazyExpressionMaterializingReasonerFactory();
		OWLReasoner r = rf.createReasoner(g.getSourceOntology());
		
		parser = new ManchesterSyntaxTool(g.getSourceOntology(), g.getSupportOntologySet());
		pp = new OWLPrettyPrinter(g);


		
		findDescendants(r, "part_of some limb", 3);

		findDescendants(r, "inheres_in_part_of some limb", 4);
			
		findDescendants(r, "limb", 3);
		findDescendants(r, "abnormal_morphology");
		findDescendants(r, "develops_from some limb_bud", 6);
		findDescendants(r, "part_of some hindlimb", 1);
		findDescendants(r, "autopod", 3);
		findDescendants(r, "part_of some hindlimb", 1);
		//findDescendants(r, "autopod and (part_of some hindlimb)", 1); ?? equiv
		findDescendants(r, "autopod and (part_of some limb)", 2);

		findDescendants(r, "inheres_in some hand", 1);
		findDescendants(r, "inheres_in some limb", 2);
		findDescendants(r, "inheres_in some (part_of some limb)", 2);
		findDescendants(r, "inheres_in_part_of some limb", 4);
		findDescendants(r, "hyperplastic and inheres_in_part_of some limb", 2);
		findDescendants(r, "hyperplastic and inheres_in some (part_of some (anterior_to some hindlimb))", 1);
		findDescendants(r, "(anterior_to some hindlimb)", 1);
		findDescendants(r, "part_of some (anterior_to some hindlimb)", 1);
		//findDescendants(r, "hyperplastic and inheres_in some hand", 1);
		findDescendants(r, "hyperplastic_hand", 1);
		findDescendants(r, "hyperplastic", 3);
		
		findIndividuals(r, "metazoan", 5);
		findIndividuals(r, "human", 3);
		findIndividuals(r, "has_phenotype some hyperplastic_hand", 3);
		findIndividuals(r, "human and has_phenotype some hyperplastic_hand", 2); // underestimation without pre-reasoning
		findIndividuals(r, "human and has_phenotype some abnormal_morphology", 3);
		findIndividuals(r, "human and has_phenotype some hyperplastic", 3);
		////findIndividuals(r, "has_phenotype some (not hyperplastic)", 2);
		findIndividuals(r, "fly and has_phenotype some (inheres_in some compound_eye)", 1);
		findIndividuals(r, "fly and has_phenotype some (inheres_in some (has_part some ommatidium))", 1);

	}





}
