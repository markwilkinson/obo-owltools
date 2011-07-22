package owltools.ontologyrelease;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.InvalidXrefMapException;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatDanglingReferenceException;
import org.obolibrary.oboformat.parser.XrefExpander;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveAxiom;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;

/**
 * This class is a command line utility which builds an ontology release. The
 * command line argument --h or --help provides usage documentation of this
 * utility. This tool called through bin/ontology-release-runner.
 * 
 * @author Shahid Manzoor
 * 
 */
public class OboOntologyReleaseRunner {

	protected final static Logger logger = Logger
	.getLogger(OboOntologyReleaseRunner.class);

	// SimpleDateFormat is not thread safe
	private static ThreadLocal<DateFormat> dtFormat = new ThreadLocal<DateFormat>(){

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}

	};

	public enum MacroStrategy {
		NO_EXPANSION, GCI 
	}


	ParserWrapper parser;
	Mooncat mooncat;
	InferenceBuilder infBuilder;
	OWLPrettyPrinter owlpp;
	String reasonerName = InferenceBuilder.REASONER_HERMIT;
	boolean asserted = false;
	boolean simple = false;
	boolean allowFileOverWrite = false;
	// TODO - make this an option
	boolean isExportBridges = false;
	boolean isRecreateMireot = false;
	boolean isExpandMacros = false;
	boolean isCheckConsistency = true;


	private static void makeDir(File path) {
		if (!path.exists())
			path.mkdir();
	}

	/**
	 * Build Ontology version id for a particular release.
	 * @param base
	 * @return version
	 * @throws IOException
	 */
	private String buildVersionInfo(File base) throws IOException {

		File versionInfo = checkNew(new File(base, "VERSION-INFO"));

		Properties prop = new Properties();

		String version = dtFormat.get().format(Calendar.getInstance().getTime());

		prop.setProperty("version", version);

		FileOutputStream propFile = null;
		try {
			propFile = new FileOutputStream(versionInfo);
			prop.store(propFile,
				"Auto Generate Version Number. Please do not edit it");
		}
		finally {
			if (propFile != null) {
				try {
					propFile.close();
				} catch (Exception e) {
					logger.warn("Could not clouse output stream for file: "+versionInfo.getAbsolutePath(), e);
				}
			}
		}

		return version;
	}
	
	/**
	 * Check whether the file is new. Throw an {@link IOException}, 
	 * if the file already exists and {@link #allowFileOverWrite} 
	 * is not set to true.
	 * 
	 * @param file
	 * @return file return the same file to allow chaining with other operations
	 * @throws IOException
	 */
	private File checkNew(File file) throws IOException {
		if (!allowFileOverWrite && file.exists() && file.isFile()) {
			boolean allow = allowFileOverwrite(file);
			if (!allow) {
				throw new IOException("Trying to overwrite an existing file: "
						+ file.getAbsolutePath());
			}	
		}
		return file;
	}
	
	/**
	 *  Hook method to handle an unexpected file overwrite request.
	 *  Returns true, if the overwrite is allowed.
	 * 
	 * @param file
	 * @return boolean 
	 * @throws IOException
	 */
	protected boolean allowFileOverwrite(File file) throws IOException {
		/* 
		 * For the command line version this is always false. If the user 
		 * wants to override file the command-line flag '--allowOverwrite' 
		 * has to be used.
		 */
		return false;
	}

	private static void cleanBase(File base) {
		/*for (File f : base.listFiles()) {
			if (f.getName().endsWith(".obo"))
				f.delete();
			else if (f.getName().endsWith(".owl"))
				f.delete();
			else if (f.isDirectory() && f.getName().equals("subsets"))
				f.delete();
			else if (f.isDirectory() && f.getName().equals("extensions"))
				f.delete();
		}*/
	}

	private static String getPathIRI(String path){

		return path;
	}



	public String getReasonerName() {
		return reasonerName;
	}

	public void setReasonerName(String reasonerName) {
		this.reasonerName = reasonerName;
	}

	public boolean isAsserted() {
		return asserted;
	}

	public void setAsserted(boolean asserted) {
		this.asserted = asserted;
	}

	public boolean isSimple() {
		return simple;
	}

	public void setSimple(boolean simple) {
		this.simple = simple;
	}

	public boolean isExportBridges() {
		return isExportBridges;
	}

	public void setExportBridges(boolean isExportBridges) {
		this.isExportBridges = isExportBridges;
	}
	
	

	public boolean isAllowFileOverWrite() {
		return allowFileOverWrite;
	}

	public void setAllowFileOverWrite(boolean allowFileOverWrite) {
		this.allowFileOverWrite = allowFileOverWrite;
	}

	public static void main(String[] args) throws IOException,
	OWLOntologyCreationException, OWLOntologyStorageException,
	OBOFormatDanglingReferenceException {

		OWLOntologyFormat format = new RDFXMLOntologyFormat();
		// String outPath = ".";
		String baseDirectory = ".";

		OboOntologyReleaseRunner oorr = new OboOntologyReleaseRunner();

		int i = 0;
		Vector<String> paths = new Vector<String>();
		while (i < args.length) {
			String opt = args[i];
			i++;

			if (opt.trim().length() == 0)
				continue;

			logger.info("processing arg: " + opt);
			if (opt.equals("--h") || opt.equals("--help")) {
				usage();
				System.exit(0);
			}

			else if (opt.equals("-outdir")) { baseDirectory = args[i]; i++; }

			/*
			 * else if (opt.equals("-owlversion")) { version = args[i]; i++; }
			 */
			else if (opt.equals("-reasoner")) {
				oorr.reasonerName = args[i];
				i++;
			}
			/*
			 * else if (opt.equals("-oboincludes")) { oboIncludes = args[i];
			 * i++; }
			 */
			else if (opt.equals("--asserted")) {
				oorr.asserted = true;
			}
			else if (opt.equals("--simple")) {
				oorr.simple = true;
			}
			else if (opt.equals("--bridges")) {
				oorr.isExportBridges = true;
			}
			else if (opt.equals("--re-mireot")) {
				oorr.isRecreateMireot = true;
			}
			else if (opt.equals("--expand-macros")) {
				oorr.isExpandMacros = true;
			}
			else if (opt.equals("--allowOverwrite")) {
				oorr.allowFileOverWrite = true;
			}
			else {

				String tokens[] = opt.split(" ");
				for (String token : tokens)
					paths.add(token);
			}
		}

		File base = new File(baseDirectory);

		logger.info("Base directory path " + base.getAbsolutePath());

		if (!base.exists())
			throw new FileNotFoundException("The base directory at "
					+ baseDirectory + " does not exist");

		if (!base.canRead())
			throw new IOException("Cann't read the base directory at "
					+ baseDirectory);

		if (!base.canWrite())
			throw new IOException("Cann't write in the base directory "
					+ baseDirectory);

		oorr.createRelease(format, paths, base);

	}

	public void createRelease(OWLOntologyFormat format,
			Vector<String> paths, File base) 
	throws IOException,
	OWLOntologyCreationException, FileNotFoundException,
	OWLOntologyStorageException 
	{
		String path = null;


		// ----------------------------------------
		// Set up directories
		// ----------------------------------------
		File releases = new File(base, "releases");
		makeDir(releases);

		cleanBase(base);

		File todayRelease = new File(releases, dtFormat.get().format(Calendar
				.getInstance().getTime()));
		todayRelease = todayRelease.getCanonicalFile();
		makeDir(todayRelease);

		String version = buildVersionInfo(base);

		File subsets = new File(base, "subsets");
		makeDir(subsets);

		File extensions = new File(base, "extensions");
		makeDir(extensions);

		if (paths.size() > 0)
			path = getPathIRI( paths.get(0) );

		logger.info("Processing Ontologies: " + paths);

		parser = new ParserWrapper();
		mooncat = new Mooncat(parser.parseToOWLGraph(path));
		owlpp = new OWLPrettyPrinter(mooncat.getGraph());

		for (int k = 1; k < paths.size(); k++) {
			String p = getPathIRI(paths.get(k));
			mooncat.addReferencedOntology(parser.parseOWL(p));
		}

		if (version != null) {
			addVersion(mooncat.getOntology(), version, mooncat.getManager());
		}

		String ontologyId = Owl2Obo.getOntologyId(mooncat.getOntology());
		ontologyId = ontologyId.replaceAll(".obo$", ""); // temp workaround

		// ----------------------------------------
		// Bridge files
		// ----------------------------------------
		if (isExpandMacros) {
			OWLOntology ont = mooncat.getOntology();
			MacroExpansionVisitor mev = 
				new MacroExpansionVisitor(mooncat.getManager().getOWLDataFactory(), 
						ont, mooncat.getManager());
			ont = mev.expandAll();		
			mooncat.setOntology(ont);

		}
			
		// ----------------------------------------
		// Bridge files
		// ----------------------------------------
		if (isExportBridges) {
			logger.info("Creating Bridge Ontologies");

			// Note that this introduces a dependency on the oboformat-specific portion
			// of the oboformat code. Ideally we would like to make everything run
			// independent of obo
			XrefExpander xe;
			try {
				// TODO - make this configurable.
				// currently uses the name "MAIN-bridge-to-EXT" for all
				xe = new XrefExpander(parser.getOBOdoc(), ontologyId+"-bridge-to");
				xe.expandXrefs();
				for (OBODoc tdoc : parser.getOBOdoc().getImportedOBODocs()) {
					String tOntId = tdoc.getHeaderFrame().getClause(OboFormatTag.TAG_ONTOLOGY).getValue().toString();
					logger.info("Generating bridge ontology:"+tOntId);
					Obo2Owl obo2owl = new Obo2Owl();
					OWLOntology tOnt = obo2owl.convert(tdoc);
					saveOntologyInAllFormats(base, tOntId, format, tOnt);

					// TODO - save both obo and owl;
					// do this in a generic way, Don't Repeat Yourself..
				}
			} catch (InvalidXrefMapException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO - macro expansions
		}

		// ----------------------------------------
		// Asserted (non-classified)
		// ----------------------------------------


		if (asserted) {
			logger.info("Creating Asserted Ontology");
			saveInAllFormats(base, ontologyId, format, "non-classified");
			logger.info("Asserted Ontology Creation Completed");
		}

		// ----------------------------------------
		// Main (asserted plus non-redundant inferred links)
		// ----------------------------------------
		// this is the same as ASSERTED, with certain axiom ADDED

		// this is always on by default
		//  at some point we may wish to make this optional,
		//  but a user would rarely choose to omit the main ontology
		if (true) {

			logger.info("Merging Ontologies (only has effect if multiple ontologies are specified)");
			mooncat.mergeOntologies();

			logger.info("Creating basic ontology");

			if (reasonerName != null) {
				infBuilder = new InferenceBuilder(mooncat.getGraph(), reasonerName);

				logger.info("Creating inferences");
				buildInferences();
				logger.info("Inferences creation completed");
				
				if (this.isCheckConsistency) {
					logger.info("Checking consistency");
					List<String> incs = infBuilder.performConsistencyChecks();
					if (incs.size() > 0) {
						for (String inc  : incs) {
							logger.error(inc);
						}
					}
					logger.info("Checking consistency completed");
				}
				
				logger.info("Finding redundant axioms");

				for (OWLAxiom ax : infBuilder.getRedundantAxioms()) {
					// TODO - in future do not remove axioms that are annotated
					logger.info("Removing redundant axiom:"+ax+" // " + owlpp.render(ax));
					mooncat.getManager().applyChange(new RemoveAxiom(mooncat.getOntology(), ax));					
				}

				logger.info("Redundant axioms removed");
			}
			saveInAllFormats(base, ontologyId, format, null);

		}

		// ----------------------------------------
		// Simple (no MIREOTs, no imports)
		// ----------------------------------------
		// this is the same as MAIN, with certain axiom REMOVED
		if (simple) {

			logger.info("Creating simple ontology");

			/*
			logger.info("Creating Inferences");
			if (reasoner != null) {
				//buildInferredOntology(simpleOnt, manager, reasoner);
				buildInferences(mooncat.getGraph(), mooncat.getManager(), reasoner);

			}
			logger.info("Inferences creation completed");
			 */

			Owl2Obo owl2obo = new Owl2Obo();


			logger.info("Guessing core ontology (in future this can be overridden)");

			Set<OWLClass> coreSubset = new HashSet<OWLClass>();
			for (OWLClass c : mooncat.getOntology().getClassesInSignature()) {
				String idSpace = owl2obo.getIdentifier(c).replaceAll(":.*", "").toLowerCase();
				if (idSpace.equals(ontologyId.toLowerCase())) {
					coreSubset.add(c);
				}
			}

			logger.info("Estimated core ontology number of classes: "+coreSubset.size());
			if (coreSubset.size() == 0) {
				// TODO - make the core subset configurable
				logger.error("cannot determine core subset - simple file will include everything");
			}
			else {
				mooncat.removeSubsetComplementClasses(coreSubset, true);
			}

			saveInAllFormats(base, ontologyId, format, "simple");
			logger.info("Creating simple ontology completed");

		}		


		// ----------------------------------------
		// End of export file creation
		// ----------------------------------------

		logger.info("Copying files to release "
				+ todayRelease.getAbsolutePath());

		for (File f : base.listFiles()) {
			if (f.getName().endsWith(".obo") || f.getName().endsWith(".owl")
					|| f.getName().equals("VERSION-INFO")
					|| (f.isDirectory() && f.getName().equals("subsets"))
					|| (f.isDirectory() && f.getName().equals("extensions")))

				copy(f.getCanonicalFile(), todayRelease);
		}
	}

	/**
	 * Uses reasoner to obtained inferred subclass axioms, and then adds the non-redundant
	 * ones to he ontology
	 * 
	 * @param graph
	 * @param manager
	 * @param reasoner
	 * @return
	 */
	private List<OWLAxiom> buildInferences() {

		OWLGraphWrapper graph = mooncat.getGraph();

		List<OWLAxiom> axioms = infBuilder.buildInferences();

		// TODO: ensure there is a subClassOf axiom for ALL classes that have an equivalence axiom
		for(OWLAxiom ax: axioms){
			logger.info("New axiom:"+ax+" // " + owlpp.render(ax));
			mooncat.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), ax));
		}		
		return axioms;
	}

	private void saveInAllFormats(File base, String ontologyId, OWLOntologyFormat format, String ext) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		String fn = ext == null ? ontologyId :  ontologyId + "-" + ext;
		saveOntologyInAllFormats(base, fn, format, mooncat.getOntology());
	}

	private void saveOntologyInAllFormats(File base, String fn, OWLOntologyFormat format, OWLOntology ontologyToSave) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {

		logger.info("Saving: "+fn);

		String outputURI = checkNew(new File(base, fn +".owl")).getAbsolutePath();

		logger.info("saving to " + outputURI);
		FileOutputStream os = new FileOutputStream(new File(outputURI));
		mooncat.getManager().saveOntology(ontologyToSave, format, os);
		os.close();

		Owl2Obo owl2obo = new Owl2Obo();
		OBODoc doc = owl2obo.convert(ontologyToSave);

		outputURI = checkNew(new File(base, fn +".obo")).getAbsolutePath();
		logger.info("saving to " + outputURI);

		OBOFormatWriter writer = new OBOFormatWriter();

		BufferedWriter bwriter = new BufferedWriter(new FileWriter(
				new File(outputURI)));

		writer.write(doc, bwriter);

		bwriter.close();

	}

	private static void usage() {
		System.out.println("This utility builds an ontology release. This tool is supposed to be run " +
		"from the location where a particular ontology releases are to be maintained.");
		System.out.println("\n");
		System.out.println("bin/ontology-release-runner [OPTIONAL OPTIONS] ONTOLOGIES-FILES");
		System.out
		.println("Multiple obo or owl files are separated by a space character in the place of the ONTOLOGIES-FILES arguments.");
		System.out.println("\n");
		System.out.println("OPTIONS:");
		System.out
		.println("\t\t (-outdir ~/work/myontology) The path where the release will be produced.");
		System.out
		.println("\t\t (-reasoner pellet) This option provides name of reasoner to be used to build inference computation.");
		System.out
		.println("\t\t (--asserted) This unary option produces ontology without inferred assertions");
		System.out
		.println("\t\t (--simple) This unary option produces ontology without included/supported ontologies");
	}

	private static void addVersion(OWLOntology ontology, String version,
			OWLOntologyManager manager) {
		OWLDataFactory fac = manager.getOWLDataFactory();

		OWLAnnotationProperty ap = fac.getOWLAnnotationProperty(Obo2Owl
				.trTagToIRI(OboFormatTag.TAG_REMARK.getTag()));
		OWLAnnotation ann = fac
		.getOWLAnnotation(ap, fac.getOWLLiteral(version));

		if (ontology == null ||
				ontology.getOntologyID() == null ||
				ontology.getOntologyID().getOntologyIRI() == null) {
			// TODO: shahid - can you add a proper error mechanism
			System.err.println("Please set your ontology ID. \n"+
			"In obo-format this should be the same as your ID-space, all in lower case");
		}
		OWLAxiom ax = fac.getOWLAnnotationAssertionAxiom(ontology
				.getOntologyID().getOntologyIRI(), ann);

		manager.applyChange(new AddAxiom(ontology, ax));

	}


	/**
	 * Copies file/directory to destination from source.
	 * @param fromFile
	 * @param toFile
	 * @throws IOException
	 */
	public void copy(File fromFile, File toFile) throws IOException {

		if (toFile.isDirectory())
			toFile = new File(toFile, fromFile.getName());

		if(fromFile.isDirectory()){
			makeDir(toFile);
			for(File f: fromFile.listFiles()){
				if(f.getName().equals(".") || f.getName().equals(".."))
					continue;
				
				copy(f, toFile);
			}

			return;
		}

		checkNew(toFile);
		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} finally {
			if (from != null)
				try {
					from.close();
				} catch (IOException e) {
					logger.warn("Could not close file input stream: "+fromFile.getAbsolutePath(),e);
				}

			if (to != null)
				try {
					to.close();
				} catch (IOException e) {
					logger.warn("Could not close file output stream: "+toFile.getAbsolutePath(),e);
				}
		}
	}


}
