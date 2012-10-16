package org.geneontology.lego.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

/**
 * Tool to convert the OWL representation of annotations into the rendering model.
 * 
 * @see LegoNode
 * @see LegoLink
 */
public class LegoTools {
	
	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;
	private final Set<OWLObjectProperty> enabled_by;
	private final Set<OWLObjectProperty> occurs_in;
	private final Set<OWLObjectProperty> part_of;
	private final OWLClass mf;
	private final OWLClass bp;
	
	/**
	 * @param graph
	 * @param reasoner
	 */
	public LegoTools(OWLGraphWrapper graph, OWLReasoner reasoner) {
		this(graph, reasoner,
			findProperties(graph, "http://purl.obolibrary.org/obo/enabled_by"), // enabled_by
			findProperties(graph, 
					"http://purl.obolibrary.org/obo/BFO_0000066", // occurs_in 
					"http://purl.obolibrary.org/obo/occurs_in"),
			findProperties(graph, 
					"http://purl.obolibrary.org/obo/BFO_0000050", // part_of
					"http://purl.obolibrary.org/obo/part_of"),
			graph.getOWLClassByIdentifier("GO:0003674"), // molecular function 
			graph.getOWLClassByIdentifier("GO:0008150")); // biological process 
	}

	/**
	 * @param graph
	 * @param reasoner
	 * @param enabled_by
	 * @param occurs_in
	 * @param part_of
	 * @param mf
	 * @param bp 
	 */
	protected LegoTools(OWLGraphWrapper graph, OWLReasoner reasoner,
			Set<OWLObjectProperty> enabled_by, Set<OWLObjectProperty> occurs_in,
			Set<OWLObjectProperty> part_of,
			OWLClass mf,
			OWLClass bp)
	{
		this.graph = graph;
		this.reasoner = reasoner;
		this.enabled_by = enabled_by;
		this.occurs_in = occurs_in;
		this.part_of = part_of;
		this.mf = mf;
		this.bp = bp;
	}
	
	private static Set<OWLObjectProperty> findProperties(OWLGraphWrapper graph, String...iris) {
		Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
		for (String iri : iris) {
			properties.add(graph.getDataFactory().getOWLObjectProperty(IRI.create(iri)));
		}
		return properties;
	}

	public static class UnExpectedStructureException extends Exception {

		// generated
		private static final long serialVersionUID = -3343544020570925182L;

		/**
		 * @param message
		 * @param cause
		 */
		public UnExpectedStructureException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public UnExpectedStructureException(String message) {
			super(message);
		}
		
	}

	public Collection<LegoNode> createLegoNodes(Collection<OWLNamedIndividual> individuals) throws UnExpectedStructureException {
		List<LegoNode> nodes = new ArrayList<LegoNode>(individuals.size());
		final OWLOntology ontology = graph.getSourceOntology();
		for (OWLNamedIndividual individual : individuals) {
			Set<OWLClassAssertionAxiom> axioms = ontology.getClassAssertionAxioms(individual);
			final LegoNode node = createNode(individual, axioms);
			
			// links
			List<LegoLink> links = new ArrayList<LegoLink>();
			Set<OWLObjectPropertyAssertionAxiom> propertyAxioms = ontology.getObjectPropertyAssertionAxioms(individual);
			for (OWLObjectPropertyAssertionAxiom propertyAxiom : propertyAxioms) {
				OWLIndividual object = propertyAxiom.getObject();
				if (object instanceof OWLNamedIndividual == false) {
					throw new UnExpectedStructureException("Expected a named individual for a link: "+propertyAxiom);
				}
				OWLNamedIndividual namedTarget = (OWLNamedIndividual) object;
				OWLObjectPropertyExpression property = propertyAxiom.getProperty();
				links.add(new LegoLink(namedTarget, property));
			}
			if (!links.isEmpty()) {
				node.setLinks(links);
			}
			
			nodes.add(node);
		}
		
		return nodes;
	}
	
	private LegoNode createNode(OWLNamedIndividual individual, Set<OWLClassAssertionAxiom> axioms)
			throws UnExpectedStructureException
	{
		final OWLClassExpression type = getType(individual);
		LegoNode node = new LegoNode(individual, type);
		final NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(type, false);
		if (superClasses.containsEntity(mf) || mf.equals(type)) {
			// is a molecular function
			node.setMf(true);
			
			// check for composed molecular function
			/*
			 * search all axioms for part_of relations to this individual
			 * 
			 * TODO: in theory this could be done using a reasoner, 
			 * but ELK does not support inverseOf for has_part and part_of
			 * at the moment
			 * 
			 * CMF === MF and has_part some MF
			 */
			Set<OWLIndividual> parts = new HashSet<OWLIndividual>();
			Set<OWLObjectPropertyAssertionAxiom> assertions = graph.getSourceOntology().getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
			for (OWLObjectPropertyAssertionAxiom assertion : assertions) {
				final OWLIndividual object = assertion.getObject();
				final OWLObjectPropertyExpression property = assertion.getProperty();
				if (individual.equals(object) && part_of.contains(property)) {
					parts.add(assertion.getSubject());
				}
			}
			if (parts.isEmpty() == false) {
				node.setCmf(true);
			}
		}
		else if (superClasses.containsEntity(bp) || bp.equals(type)) {
			node.setBp(true);
		}
		else {
			throw new UnExpectedStructureException("The individual: "+renderIndividualName(individual)+" is neither molecular_function or biological_process as a parent.");
		}
		
		List<OWLClassExpression> cellularLocations = new ArrayList<OWLClassExpression>();
		List<OWLClassExpression> unknowns = new ArrayList<OWLClassExpression>();
		
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression expression = axiom.getClassExpression();
			if (expression.isClassExpressionLiteral()) {
				// ignore, use reasoner to retrieve type
			}
			else if (expression instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
				OWLObjectPropertyExpression property = object.getProperty();
				OWLClassExpression clsExp = object.getFiller();
				if (enabled_by.contains(property) && !clsExp.isAnonymous()) {
					// active entity
					OWLClass activeEntity = node.getActiveEntity();
					if (activeEntity != null) {
						throw new UnExpectedStructureException("The individual: "+renderIndividualName(individual)+" has multiple 'enabled_by' declarations.");
					}
					node.setActiveEntity(clsExp.asOWLClass());
				}
				else if (occurs_in.contains(property)) {
					// cellular location
					cellularLocations.add(clsExp);
				}
				else {
					unknowns.add(expression);
				}
			}
			else {
				unknowns.add(expression);
			}
		}
		
		if (cellularLocations.isEmpty()) {
			// check super classes for cellular location information
			OWLClassExpression cellularLocation = CellularLocationTools.searchCellularLocation(node.getType(), graph, occurs_in);
			if (cellularLocation != null) {
				cellularLocations.add(cellularLocation);
			}
		}
		node.setCellularLocation(cellularLocations);
		node.setUnknowns(unknowns);
		
		return node;
	}
	
	private String renderIndividualName(OWLNamedIndividual individual) {
		final String iri = individual.getIRI().toQuotedString();
		String label = graph.getLabel(individual);
		if (label != null) {
			return "'"+label+"' "+iri;
		}
		return iri;
	}
	
	private OWLClassExpression getType(OWLNamedIndividual individual) {
		NodeSet<OWLClass> types = reasoner.getTypes(individual, true);
		if (types.isEmpty() || types.isBottomSingleton() || types.isTopSingleton()) {
			return null;
		}
		Set<OWLClass> set = types.getFlattened();
		
		if (set.size() == 1) {
			return set.iterator().next();
		}
		OWLDataFactory fac = graph.getManager().getOWLDataFactory();
		OWLObjectIntersectionOf intersectionOf = fac.getOWLObjectIntersectionOf(set);
		return intersectionOf;
		
	}
	
}
