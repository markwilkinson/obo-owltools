package owltools.sim.preprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.reasoner.Node;

import owltools.sim.OWLClassExpressionPair;

public abstract class LCSEnabledSimPreProcessor extends AbstractSimPreProcessor {

	protected Map<OWLClassExpressionPair, OWLClass> lcsCache = new HashMap<OWLClassExpressionPair, OWLClass>();

	
	protected void generateLeastCommonSubsumersForAttributeClasses() {
		Set<OWLClass> types = getAttributeClasses();
		generateLeastCommonSubsumers(types, types);
	}
	
	public void generateLeastCommonSubsumers(Set<OWLClass> set1, Set<OWLClass> set2) {

		for (OWLClass a : set1) {
			LOG.info("  "+a+" vs ALL");		
			for (OWLClass b : set2) {
				// LCS operation is symmetric, only pre-compute one way
				//if (a.compareTo(b) > 0) {
					OWLClass lcs = getLowestCommonSubsumerClass(a, b);
					//System.out.println("LCS( "+pp(a)+" , "+pp(b)+" ) = "+pp(lcs));
				//}
			}
		}
		LOG.info("DONE all x all");		
	}

	
	// ----------- ----------- ----------- -----------
	// SUBSUMERS AND LOWEST COMMON SUBSUMERS
	// ----------- ----------- ----------- -----------

	public Set<Node<OWLClass>> getNamedSubsumers(OWLClassExpression a) {
		return getReasoner().getSuperClasses(a, false).getNodes();
	}
	
	public Set<Node<OWLClass>> getNamedSubsumers(OWLNamedIndividual a) {
		return getReasoner().getTypes(a, false).getNodes();
	}

	public Set<Node<OWLClass>> getNamedReflexiveSubsumers(OWLClassExpression a) {
		// TODO: consider caching
		Set<Node<OWLClass>> nodes =  new HashSet<Node<OWLClass>>(getReasoner().getSuperClasses(a, false).getNodes());
		nodes.add(getReasoner().getEquivalentClasses(a));
		return nodes;
	}

	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedReflexiveSubsumers(a);
		nodes.retainAll(getNamedReflexiveSubsumers(b));
		return nodes;
	}
	public Set<Node<OWLClass>> getNamedCommonSubsumers(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<Node<OWLClass>> nodes = getNamedSubsumers(a);
		nodes.retainAll(getNamedSubsumers(b));
		return nodes;
	}

	// TODO - cache this
	public Set<Node<OWLClass>> getNamedLowestCommonSubsumers(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> nodes = getNamedCommonSubsumers(a, b);
		Set<Node<OWLClass>> rNodes = new HashSet<Node<OWLClass>>();
		for (Node<OWLClass> node : nodes) {
			rNodes.addAll(getReasoner().getSuperClasses(node.getRepresentativeElement(), false).getNodes());
		}
		nodes.removeAll(rNodes);
		return nodes;
	}

	/**
	 * gets the LCS. If multiple named LCSs exist in the ontology, then
	 * a class expression is generated from the filtered intersection
	 * 
	 * @param a
	 * @param b
	 * @return OWLClassExpression
	 */
	public OWLClassExpression getLowestCommonSubsumer(OWLClassExpression a, OWLClassExpression b) {
		if (a.equals(b)) {
			return a;
		}
		if (a instanceof OWLClass && b instanceof OWLClass) {
			if (getReasoner().getSuperClasses(a, false).getFlattened().contains(b)) {
				return b;
			}
			//LOG.info("Getting superclasses of "+b);
			if (getReasoner().getSuperClasses(b, false).getFlattened().contains(a)) {
				return a;
			}
		}

		// returns the set of named LCSs that already exist
		Set<Node<OWLClass>> ccs = getNamedLowestCommonSubsumers(a,b);

		// single LCS - return this directly
		if (ccs.size() == 1) {
			return ccs.iterator().next().getRepresentativeElement();
		}

		// make a class expression from the filtered intersection of LCSs

		Set<OWLClass> ops = new HashSet<OWLClass>();
		Set<OWLClass> skips = new HashSet<OWLClass>();
		OWLClass best = null;
		int bestAncScore = 0;
		for (Node<OWLClass> n : ccs) {
			OWLClass c = n.getRepresentativeElement();
			// TODO: add additional custom filtering here
			if (isUpperLevel(c))
				continue;
			boolean skip = false;
			
			int numAncestors = this.getNamedReflexiveSubsumers(c).size();
			if (numAncestors > bestAncScore) {
				bestAncScore = numAncestors;
				best = c;
			}
			if (numAncestors < 3) { // TODO: configure
				// non-grouping attribute
				LOG.info("SKIPPING: "+c+" too few parents");
				//LOG.info("SKIPPING: "+c+" IC="+ic);
				continue;
			}
			
			// don't make intersections of similar elements
			for (Node<OWLClass> n2 : ccs) {
				OWLClass c2 = n2.getRepresentativeElement();
				
				int numAncestors2 = this.getNamedReflexiveSubsumers(c2).size();
				// prioritize the one with highest score.
				// it it's a tie, then choose one deterministically
				if (numAncestors < numAncestors2 || (numAncestors==numAncestors2 && c.compareTo(c2) > 0)) {
					// TODO: configure simj thresh
					if (getJaccardIndex(c, c2) > 0.5) {
						LOG.info("SKIPPING: "+c+" too similar to "+n2);
						skip = true;
					}
				}
			}
			if (skip)
				continue;
			
			// not filtered
			ops.add(c);
		}

		if (ops.size() == 1) {
			// only one passes
			return ops.iterator().next();
		}
		if (ops.size() == 0) {
			// none pass: choose the best representative of the intersection
			return best;
		}
		return getOWLDataFactory().getOWLObjectIntersectionOf(ops);
	}

	/**
	 * generates a LCS expression and makes it a class if it is a class expression
	 * 
	 * @param a
	 * @param b
	 * @return named class representing LCS
	 */
	public OWLClass getLowestCommonSubsumerClass(OWLClassExpression a, OWLClassExpression b) {
		
		OWLClassExpressionPair pair = new OWLClassExpressionPair(a,b);
		if (lcsCache.containsKey(pair)) {
			return lcsCache.get(pair);
		}

		OWLClassExpression x = getLowestCommonSubsumer(a,b);
		OWLClass lcs;
		/*
		if (lcsExpressionToClass.containsKey(x)) {
			lcs = lcsExpressionToClass.get(x);
		}
		*/
		if (x instanceof OWLClass)
			lcs = (OWLClass)x;
		else if (x instanceof OWLObjectIntersectionOf)
			lcs = makeClass((OWLObjectIntersectionOf) x);
		else
			lcs = null;
		lcsCache.put(pair, lcs);
		//LOG.info("LCS of "+a+" + "+b+" = "+lcs);
		return lcs;
	}
	
	/**
	 * given a CE of the form A1 and A2 and ... An,
	 * get a class with an IRI formed by concatenating parts of the IRIs of Ai,
	 * and create a label assertion, where the label is formed by concatenating label(A1)....
	 * 
	 * note that the reasoner will need to be synchronized after new classes are made
	 * 
	 * Note: modifies ontology but does not flush
	 * 
	 * @param x
	 * @return class
	 */
	public OWLClass makeClass(OWLObjectIntersectionOf x) {
		//StringBuffer id = new StringBuffer();
		StringBuffer label = new StringBuffer();
		int n = 0;
		int nlabels = 0;
		
		LOG.info("LCS INTERSECTION: "+x);

		IRI intersectionIRI = null;
		// TODO - optimize following
		for (OWLClassExpression op : x.getOperands()) {
			n++;
			// throw exception if ops are not named
			OWLClass opc = (OWLClass)op;
			//String opid = opc.getIRI().toString();
			String oplabel = getAnyLabel(opc);

			if (n == 1) {
				intersectionIRI = opc.getIRI();
				// make base
				/*
				String prefix = opid.toString();
				prefix = prefix.replaceAll("#.*","#");
				if (prefix.startsWith(OBO_PREFIX))
					id.append(OBO_PREFIX);
				else 
					id.append(prefix);
					*/				
			}
			else {
				intersectionIRI = makeViewClassIRI(intersectionIRI, opc.getIRI());
			}
			
			if (n > 1) {
				label.append(" and ");
			}
			if (oplabel != null) {
				nlabels++;
				label.append(oplabel);
			}
			else {
				label.append("?"+opc.getIRI().toString());
			}

		}
		OWLClass c = getOWLDataFactory().getOWLClass(intersectionIRI);
		Set<OWLAxiom> newAxioms = new HashSet<OWLAxiom>();
		newAxioms.add( getOWLDataFactory().getOWLEquivalentClassesAxiom(c, x) );
		newAxioms.add( getOWLDataFactory().getOWLDeclarationAxiom(c));
		//LOG.info(" Generated label("+c+")="+label.toString());
		if (nlabels > 0) {
			newAxioms.add( getOWLDataFactory().getOWLAnnotationAssertionAxiom(getOWLDataFactory().getRDFSLabel(), c.getIRI(), 
					getOWLDataFactory().getOWLLiteral(label.toString())));
		}
		//TODO
		//lcsExpressionToClass.put(x, c);
		LOG.info(" new LCS: "+c+" label: "+label.toString()+" == "+x);
		this.addAxiomsToOutput(newAxioms, false);
		return c;
	}

	// Note: an identical method is present in SimpleOwlSim. It is only replicated here
	// as a heuristic in intersection generation
	private float getJaccardIndex(OWLClassExpression a, OWLClassExpression b) {
		Set<Node<OWLClass>> ci = getNamedCommonSubsumers(a,b); // a /\ b
		Set<Node<OWLClass>> cu = getNamedReflexiveSubsumers(a); 
		cu.addAll(getNamedReflexiveSubsumers(b));   // a \/ u
		return ci.size() / (float)cu.size();
	}


}
