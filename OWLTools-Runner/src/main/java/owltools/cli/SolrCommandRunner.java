package owltools.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.solrj.FlexSolrDocumentLoader;
import owltools.solrj.GafSolrDocumentLoader;
import owltools.solrj.OntologySolrLoader;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.SolrSchemaXMLWriter;

/**
 *  Solr/GOlr loading.
 */
public class SolrCommandRunner extends TaxonCommandRunner {

	private static final Logger LOG = Logger.getLogger(SolrCommandRunner.class);
	
	private String globalSolrURL = null;
	private ConfigManager aconf = null;

	/**
	 * Output (STDOUT) a XML segment to put into the Solr schema file after reading the YAML file.
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-config")
	public void configRead(Opts opts) {
		
		LOG.info("Grab configuration files.");

		// Try and munge all of the configs together.
		aconf = new ConfigManager();
		List<String> confList = opts.nextList();
		for( String fsPath : confList ){

			LOG.info("Trying config found at: " + fsPath);
		
			// Attempt to parse the given config file.
			try {
				aconf.add(fsPath);
				LOG.info("Using config found at: " + fsPath);
			} catch (FileNotFoundException e) {
				LOG.info("Failure with config file at: " + fsPath);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Output (STDOUT) XML segment to put into the Solr schema file after reading the YAML configuration file(s).
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-schema-dump")
	public void solrSchemaDump(Opts opts) {
		
		LOG.info("Dump Solr schema.");

		// Get the XML from the dumper into a string.
		String config_string = null;
		try {
			SolrSchemaXMLWriter ssxw = new SolrSchemaXMLWriter(aconf);
			config_string = ssxw.schema();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
//		// Run the XML through a regexp to just get the parts we want.
//		String output = null;
//		//LOG.info("Current XML schema:\n" + config_string);
//		Pattern pattern = Pattern.compile("<!--START-->(.*)<!--STOP-->", Pattern.DOTALL);
//		Matcher matcher = pattern.matcher(config_string);
//		boolean matchFound = matcher.find();
//		if (matchFound) {
//			output = matcher.group(1); // not the global match, but inside
//			//LOG.info("Found:\n" + output);
//		}
		
//		// Either we got it, and we dump to STDOUT, or exception.
//		if( output == null || output.equals("") ){
//			throw new Error();
//		}else{
//  		System.out.println(output);
		System.out.println(config_string);
//		}
	}

	/**
	 * Set an optional Solr URL to use with Solr options so they don't have to
	 * be specified separately for every option.
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-url")
	public void setSolrUrl(Opts opts) {
		globalSolrURL = opts.nextOpt(); // shift it off of null
		LOG.info("Globally use GOlr server at: " + globalSolrURL);
	}
	
	/**
	 * Manually purge the index to try again.
	 * Since this cascade is currently ordered, can be used to purge before we load.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-purge")
	public void purgeSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Wipe out the solr index at url.
		SolrServer server = new CommonsHttpSolrServer(url);
		try {
			server.deleteByQuery("*:*");
		} catch (SolrServerException e) {
			LOG.info("Purge at: " + url + " failed!");
			e.printStackTrace();
		}
		LOG.info("Purged: " + url);
	}
	
	/**
	 * Used for loading whatever ontology stuff we have into GOlr.
	 * 
	 * @param opts 
	 * @throws Exception
	 */
	@Deprecated
	@CLIMethod("--load-ontology-solr")
	public void loadOntologySolr(Opts opts) throws Exception {
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Actual ontology class loading.
		try {
			OntologySolrLoader loader = new OntologySolrLoader(url, g);
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Experimental flexible loader.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--solr-load-ontology")
	public void flexLoadOntologySolr(Opts opts) throws Exception {

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);				

		// Actual ontology class loading.
		try {
			FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader(url, aconf, g);
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
		}

//		// Check to see if the global url has been set.
//		String url = sortOutSolrURL(opts, globalSolrURL);				
//
//		// Load remaining docs.
//		List<String> files = opts.nextList();
//		for (String file : files) {
//			LOG.info("Parsing GAF: " + file);
//			FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader(url);
//			loader.setGafDocument(gafdoc);
//			loader.setGraph(g);
//			try {
//				loader.load();
//			} catch (SolrServerException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	/**
	 * Used for loading a list of GAFs into GOlr.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--load-gafs-solr")
	public void loadGafsSolr(Opts opts) throws Exception {
		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);

		List<String> files = opts.nextList();
		for (String file : files) {
			LOG.info("Parsing GAF: " + file);
			GafObjectsBuilder builder = new GafObjectsBuilder();
			gafdoc = builder.buildDocument(file);
			loadGAFDoc(url, gafdoc);
		}
	}
	
	/**
	 * Requires the --gaf argument (or something else that fills the gafdoc object).
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--load-gaf-solr")
	public void loadGafSolr(Opts opts) throws Exception {
		// Double check we're not going to do something silly, like try and
		// use a null variable...
		if( gafdoc == null ){
			System.err.println("No GAF document defined (maybe use '--gaf GAF-FILE') ");
			exit(1);
		}

		// Check to see if the global url has been set.
		String url = sortOutSolrURL(globalSolrURL);
		// Doc load.
		loadGAFDoc(url, gafdoc);
	}
	
	/*
	 * Convert all solr URL handling through here.
	 */
	//private String sortOutSolrURL(Opts opts, String globalSolrURL) throws Exception {
	private String sortOutSolrURL(String globalSolrURL) throws Exception {

		String url = null;
		if( globalSolrURL == null ){
			//url = opts.nextOpt();
		}else{
			url = globalSolrURL;
		}
		LOG.info("Use GOlr server at: " + url);

		if( url == null ){
			throw new Exception();
		}
		
		return url;
	}
	
	/*
	 * Wrapper multiple places where there is direct GAF loading.
	 */
	private void loadGAFDoc(String url, GafDocument gafdoc) throws IOException{

		// Doc load.
		GafSolrDocumentLoader loader = new GafSolrDocumentLoader(url);
		loader.setGafDocument(gafdoc);
		loader.setGraph(g);
		try {
			loader.load();
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}
}
