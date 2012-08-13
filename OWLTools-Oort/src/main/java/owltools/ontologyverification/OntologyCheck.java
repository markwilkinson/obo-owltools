package owltools.ontologyverification;

import java.util.Collection;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Methods for executing and describing an ontology check.
 */
public interface OntologyCheck {

	/**
	 * Run the check for the given ontology.
	 * 
	 * @param graph ontology
	 * @param allOwlObjects shortcut to all {@link OWLObject}s in the ontology.
	 * @return list of warnings
	 */
	public Collection<CheckWarning> check(OWLGraphWrapper graph, Collection<OWLObject> allOwlObjects);
	
	/**
	 * @return the printable label for this check.
	 */
	public String getLabel();
	
	/**
	 * @return the ID for this check.
	 */
	public String getID();
	
	/**
	 * @return true if this a failing check is fatal and should stop OORT.
	 */
	public boolean isFatal();
	
	/**
	 * Set the fatal parameter of the check.
	 * 
	 * @param fatal
	 */
	public void setFatal(boolean fatal);
}
