package owltools.gaf.rules;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.gaf.GeneAnnotation;
import owltools.gaf.rules.AnnotationRuleViolation.ViolationType;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Checks if an annotation is valid according to taxon constraints.
 * 
 * @author cjm
 */
public class AnnotationTaxonRule extends AbstractAnnotationRule {
	
	private final OWLGraphWrapper graph;
	private final OWLObject rNever;
	private final OWLObject rOnly;
	
	/**
	 * @param graph The {@link OWLGraphWrapper} with merged-in taxon constraints
	 * @param neverId
	 * @param onlyId 
	 */
	public AnnotationTaxonRule(OWLGraphWrapper graph, String neverId, String onlyId) {
		
		if(graph == null){
			throw new IllegalArgumentException("OWLGraphWrapper may not be null");
		}
		this.graph = graph;
		rNever = graph.getOWLObjectPropertyByIdentifier(neverId);
		rOnly = graph.getOWLObjectPropertyByIdentifier(onlyId);
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {
		return _getRuleViolations(a.getCls(), a.getBioentityObject().getNcbiTaxonId(),a);
	}

	public boolean check(String annotationCls, String taxonCls) {
		return getRuleViolations(annotationCls, taxonCls).size() == 0;
	
	}

	public Set<AnnotationRuleViolation> getRuleViolations(String annotationCls, String taxonCls) {
		return _getRuleViolations(annotationCls, taxonCls,null);
	}
	
	private Set<AnnotationRuleViolation> _getRuleViolations(String annotationCls, String taxonCls, GeneAnnotation a) {
		
		if(taxonCls == null){
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Taxon id is null", a);
			return Collections.singleton(v);
		}
		
		OWLObject cls = graph.getOWLObjectByIdentifier(annotationCls);
		OWLObject tax = graph.getOWLObjectByIdentifier(taxonCls);
		
		if (cls == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Could not retrieve a class for annotationCls: "+annotationCls, a, ViolationType.Warning);
			return Collections.singleton(v);
		}
		if (tax == null) {
			AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), "Could not retrieve a class for taxonCls: "+taxonCls, a, ViolationType.Warning);
			return Collections.singleton(v);
		}
		
		Set<AnnotationRuleViolation> violations = new HashSet<AnnotationRuleViolation>();
		Set<OWLGraphEdge> edges = graph.getOutgoingEdgesClosure(cls);
		String evidenceCode = a.getEvidenceCls();

		for (OWLGraphEdge ge : edges) {
			OWLObject tgt = ge.getTarget();
			if (!(tgt instanceof OWLNamedObject))
				continue;
			OWLObject p = graph.getOWLClass(tgt);
			if (p == null) {
				continue;
			}
			OWLQuantifiedProperty qp = ge.getLastQuantifiedProperty();
			if (qp.isQuantified() && qp.getProperty().equals(rOnly)) {
				// ONLY
				if (!graph.getAncestorsReflexive(tax).contains(p)) {
					StringBuilder sb = new StringBuilder();
					sb.append("The term ");
					renderEntity(sb, cls, graph);
					sb.append(" requires the taxon ");
					renderEntity(sb, p, graph);
					sb.append(", but ");
					renderEntity(sb, tax, graph);
					sb.append(" does not match this constraint");
					sb.append(" (").append(evidenceCode).append(")");
					
					AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), sb.toString(), a);
					violations.add(v);
				}
			}
			else if (qp.isQuantified() && qp.getProperty().equals(rNever)) {
				// NEVER
				if (graph.getAncestorsReflexive(tax).contains(p)) {
					StringBuilder sb = new StringBuilder();
					sb.append("The term ");
					renderEntity(sb, cls, graph);
					sb.append(" may not be used with ");
					renderEntity(sb, tax, graph);
					sb.append(" reason: excluded ancestor ");
					renderEntity(sb, p, graph);
					sb.append(" (").append(evidenceCode).append(")");
					AnnotationRuleViolation v = new AnnotationRuleViolation(getRuleId(), sb.toString(), a);
					violations.add(v);
				}
			}
		}
		return violations;	
	}
	
	private void renderEntity(StringBuilder sb, OWLObject o, OWLGraphWrapper graph) {
		sb.append(graph.getIdentifier(o));
		String label = graph.getLabel(o);
		if (label != null) {
			sb.append(" '");
			sb.append(label);
			sb.append('\'');
		}
	}
}

