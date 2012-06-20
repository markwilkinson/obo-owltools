package owltools.flex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.GOlrField;

/**
 * Pull defined sources into a middle state for output, loading into Solr, etc.
 * Essentially, a Solr document workalike.
 */
public class FlexCollection implements Iterable<FlexDocument> {
	
	private static Logger LOG = Logger.getLogger(FlexCollection.class);
	protected transient ConfigManager config = null;
	protected transient OWLGraphWrapper graph = null;
	
	protected ArrayList<FlexDocument> docs = null;
	
	/**
	 * More fun init.
	 * 
	 * @param aconf
	 * @param in_graph
	 */
	public FlexCollection() {
		docs = new ArrayList<FlexDocument>();
	}

	/**
	 * More fun init.
	 * 
	 * @param aconf
	 * @param in_graph
	 */
	public FlexCollection(ConfigManager aconf, OWLGraphWrapper in_graph) {

		graph = in_graph;
		config = aconf;

		docs = new ArrayList<FlexDocument>();
//		//GOlrConfig config = getConfig();
//		LOG.info("Trying to load with config: " + config.id);

		if( graph == null ){
			LOG.info("ERROR? OWLGraphWrapper graph is not apparently defined...");
		}else{
			for (OWLObject obj : graph.getAllOWLObjects()) {
				docs.add(wring(obj, config));
				//add(collect(obj, graph, config));
			}	
			//addAllAndCommit();
		}
	}

	/**
	 * Try and pull out right OWLGraphWrapper function.
	 * 
	 * @param owlfunction
	 * @return
	 */
	private Method getExtMethod(String owlfunction){

		java.lang.reflect.Method method = null;
		try {
			method = graph.getClass().getMethod(owlfunction, OWLObject.class);
		} catch (SecurityException e) {
			LOG.info("ERROR: apparently a security problem with: " + owlfunction);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			LOG.info("ERROR: couldn't find method: " + owlfunction);
			e.printStackTrace();
		}

		return method;
	}
	
	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @param oobj
	 * @param owlfunction
	 * @return a (possibly null) string return value
	 */
	private String getExtString(OWLObject oobj, String owlfunction){

		String retval = null;
		
		// Try and pull out right OWLGraphWrapper function.
		java.lang.reflect.Method method = getExtMethod(owlfunction);
		
		// Try to invoke said method.
		if( method != null ){
			try {
				retval = (String) method.invoke(graph, oobj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();				
			}
		}
		
		return retval;
	}

	/**
	 * Get properly formatted output from the OWLGraphWrapper.
	 * 
	 * @param oobj
	 * @param owlfunction
	 * @return a (possibly empty) string list of returned values
	 */
	@SuppressWarnings("unchecked")
	private List<String> getExtStringList(OWLObject oobj, String owlfunction){

		List<String> retvals = new ArrayList<String>();

		// Try and pull out right OWLGraphWrapper function.
		java.lang.reflect.Method method = getExtMethod(owlfunction);
		
		// Try to invoke said method.
		if( method != null ){
			try {
				// TODO: anybody got a better idea about this?
				retvals = (List<String>) method.invoke(graph, oobj);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();				
			}
		}
		
		return retvals;
	}
	
//	/**
//	 * Private helper to take care of the annoying busywork.
//	 * 
//	 * @param car
//	 * @param cdr
//	 * @return
//	 */
//	private ArrayList<String> joinLine(String car, String cdr) {
//		ArrayList<String> c = new ArrayList<String>();
//		c.add(car);
//		c.add(cdr);
//		return c;
//	}
	
	/**
	 * Main wrapping for adding ontology documents to GOlr.
	 * Also see GafSolrDocumentLoader for the others.
	 *
	 * TODO: Bad Seth. We have hard-coded document_category here (and the GAF loader).
	 * The proper way would be to pair conf files and the file to be loaded, that is not happening
	 * quite yet, so we punt on this bad thing.
	 *
	 * @param owlObject, graph, and a config.
	 * @return an input doc for add()
	 */
	public FlexDocument wring(OWLObject obj, ConfigManager config) {

		FlexDocument cls_doc = new FlexDocument();

		///
		/// TODO/BUG: use object to create proper load sequence.
		/// Needs better cooperation from OWLTools to make is truly flexible.
		/// See Chris.
		///
		
		//LOG.info("Trying to load a(n): " + config.id);

		// Special loading for document_category.
		//LOG.info("Add: " + fixedField.id + ":" + fixedField.value);
		//
		
		cls_doc.add(new FlexLine("document_category", "ontology_class"));
					
		// Dynamic fields--have to get dynamic info to cram into the index.
		for( GOlrField field : config.getFields() ){

			String did = field.id;
			String prop_meth = field.property;
			String card = field.cardinality;

			// Select between the single and multi styles.
			if( card.equals("single") ){
				String val = getExtString(obj, prop_meth);
				if( val != null ){
					cls_doc.add(new FlexLine(did, val));
				}
			}else{
				List<String> vals = getExtStringList(obj, prop_meth);
				if( vals != null && ! vals.isEmpty() ){
					for (String val : vals) {
						cls_doc.add(new FlexLine(did, val));
					}
				}
			}
		}
		
		return cls_doc;
	}

	@Override
	public Iterator<FlexDocument> iterator() {
		return docs.iterator();
	}
}
