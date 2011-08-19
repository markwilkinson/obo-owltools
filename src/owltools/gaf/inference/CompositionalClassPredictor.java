package owltools.gaf.inference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.*;
import owltools.graph.OWLGraphWrapper;

/**
 * 
 * Given an annotation to classes c1, c2, ..., cn, we
 * generate suggestions of annotations to class D if
 * D EquivalentTo d1 and d2 and dm
 * and forall d in d1..dm,
 * there exists a class c in c1...cn such that:
 *   Path(c,d)
 * OR
 *   d = r some e, and Path(c,e)
 *   (i.e. we ignore the relation in the differentia)
 * 
 * note the list c1..cn is drawn from direct annotations,
 * AND the set of classes that are the subject of relations in c16.
 * 
 * here Path means there is some positive path
 * (TODO: include has_part?)
 * 
 * @author cjm
 *
 */
public class CompositionalClassPredictor extends AbstractAnnotationPredictor implements AnnotationPredictor {

	protected static Logger LOG = Logger.getLogger(CompositionalClassPredictor.class);
	Map<OWLClass,Set<OWLClassExpression>> simpleDefMap = new HashMap<OWLClass,Set<OWLClassExpression>>();

	public CompositionalClassPredictor(GafDocument gafDocument,
			OWLGraphWrapper graph) {
		super(gafDocument, graph);
		buildSimpleDefMap();
	}





	protected OWLClass getOWLClass(String id) {
		return (OWLClass) getGraph().getOWLObjectByIdentifier(id);
	}

	public Set<Prediction> predict(String bioentity) {
		Set<Prediction> predictions = new HashSet<Prediction>();
		Set<GeneAnnotation> anns = getGafDocument().getGeneAnnotations(bioentity);
		Set<OWLClass> aClasses = new HashSet<OWLClass>();
		LOG.info("collecting from:"+bioentity);
		for (GeneAnnotation ann : anns) {
			if (ann.getEvidenceCls().equals("ND")) {
				continue;
			}
			aClasses.add(getOWLClass(ann.getCls()));
			// we also include col16, ignoring relationship type
			for (ExtensionExpression ee : ann.getExtensionExpressions()) {
				aClasses.add(getOWLClass(ee.getCls()));
			}
		}
		LOG.info("  aClasses: "+aClasses);
		
			Set<OWLClass> ancs = new HashSet<OWLClass>();
			
			for (OWLClass aClass : aClasses) {
				if (ancs.contains(aClass)) {
					continue;
				}
				for (OWLObject a : getGraph().getAncestorsReflexive(aClass)) {
					if (a instanceof OWLClass) {
						// TODO - include class expressions
						ancs.add((OWLClass) a);
					}
				}
			}
			LOG.info("     ancs: "+ancs);

		
		// naively iterate through every class that has a logical definition;
		// note this means we waste time testing C when we already know C', and C' is subsumed by C
		for (OWLClass c : simpleDefMap.keySet()) {
			// can we infer bioentity to be of type c?
			boolean allConditionsSatisfied = true;
			String with = null;

			// every single one of the conjunctive conditions must be satisfied
			for (OWLClassExpression x : simpleDefMap.get(c)) {
				if (!ancs.contains(x)) {
					allConditionsSatisfied = false;
					break;
				}
				// todo - use actual annotate class
				String withCls = getGraph().getIdentifier(x);
				if (with == null) {
					with = withCls;
				}
				else {
					with = with + "|" + withCls;
				}

				/*
				boolean thisConditionSatisfied = false;
				for (OWLClass aClass : aClasses) {
					if (getGraph().getAncestorsReflexive(aClass).contains(x)) {
						thisConditionSatisfied = true;
						String withCls = getGraph().getIdentifier(aClass);
						if (with == null) {
							with = withCls;
						}
						else {
							with = with + "|" + withCls;
						}
						break;
					}
				}
				if (!thisConditionSatisfied) {
					allConditionsSatisfied = false;
					break;
				}
				*/
			}
			if (allConditionsSatisfied) {
				predictions.add(getPrediction(c, bioentity, with));
			}
		}
		this.setAndFilterRedundantPredictions(predictions, aClasses);
		return predictions;
	}

	protected Prediction getPrediction(OWLClass c, String bioentity, String with) {
		Prediction prediction = new Prediction();
		GeneAnnotation annP = new GeneAnnotation();
		annP.setBioentity(bioentity);
		annP.setCls(getGraph().getIdentifier(c));
		annP.setEvidenceCls("IC");
		annP.setWithExpression(with);
		// TODO - evidence
		prediction.setGeneAnnotation(annP);
		LOG.info("prediction="+prediction);
		return prediction;
	}

	//-------------

	private void buildSimpleDefMap() {
		simpleDefMap = new HashMap<OWLClass,Set<OWLClassExpression>>();
		OWLOntology o = getGraph().getSourceOntology();
		for (OWLClass c : o.getClassesInSignature()) {
			for (OWLEquivalentClassesAxiom eca : o.getEquivalentClassesAxioms(c)) {
				Set<OWLClassExpression> elts = new HashSet<OWLClassExpression>();
				for (OWLClassExpression x : eca.getClassExpressions()) {
					// assume one logical definitionper class - otherwise choose arbitrary
					if (x instanceof OWLObjectIntersectionOf) {
						if (getReachableOWLClasses(x, elts) && elts.size() > 0) {
							//LOG.info(c+" def= "+elts);
							simpleDefMap.put(c, elts);						
						}
					}
				}
			}
		}
	}

	private boolean getReachableOWLClasses(OWLClassExpression c, Set<OWLClassExpression> elts) {
		if (c instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression x : ((OWLObjectIntersectionOf)c).getOperands()) {
				if (x instanceof OWLClass) {
					elts.add((OWLClass) x);
				}
				else if (x instanceof OWLObjectSomeValuesFrom) {
					OWLObjectPropertyExpression p = ((OWLObjectSomeValuesFrom)x).getProperty();
					String pLabel = getGraph().getLabel(p);
					if (pLabel != null && pLabel.contains("regulates")) {
						// fairly hacky:
						//  fail on this for now - no inference for regulates
						//elts.add(x);
						return false;
					}
					else {
						OWLClassExpression filler = ((OWLObjectSomeValuesFrom)x).getFiller();
						if (!getReachableOWLClasses( filler, elts)) {
							return false;
						}
					}
				}
				else {
					return false;
				}

			}
			return true;
		}
		else if (c instanceof OWLClass) {
			elts.add((OWLClass) c);
			return true;
		}
		return false;
	}

}
