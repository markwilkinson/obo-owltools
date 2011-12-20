package owltools.ontologyrelease;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.obolibrary.macro.MacroExpansionGCIVisitor;
import org.obolibrary.macro.MacroExpansionVisitor;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.InvalidXrefMapException;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatDanglingReferenceException;
import org.obolibrary.oboformat.parser.XrefExpander;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.SetOntologyID;

import owltools.InferenceBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.mooncat.OntologyMetaDataTools;
import owltools.mooncat.OntologyMetaDataTools.AnnotationCardinalityException;
import owltools.ontologyrelease.OortConfiguration.MacroStrategy;
import owltools.ontologyverification.OntologyCheckHandler;
import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;

/**
 * This class is a command line utility which builds an ontology release. The
 * command line argument --h or --help provides usage documentation of this
 * utility. This tool called through bin/ontology-release-runner.
 * 
 * @author Shahid Manzoor
 * 
 */
public class OboOntologyReleaseRunner extends ReleaseRunnerFileTools {

	protected final static Logger logger = Logger .getLogger(OboOntologyReleaseRunner.class);

	ParserWrapper parser;
	Mooncat mooncat;
	InferenceBuilder infBuilder;
	OWLPrettyPrinter owlpp;
	OortConfiguration oortConfig;

	public OboOntologyReleaseRunner(OortConfiguration oortConfig, File base) throws IOException {
		super(base, logger);
		this.oortConfig = oortConfig; 
	}

	/**
	 * Check whether the file is new. Throw an {@link IOException}, 
	 * if the file already exists and {@link OortConfiguration#isAllowFileOverWrite} 
	 * is not set to true.
	 * 
	 * @param file
	 * @return file return the same file to allow chaining with other operations
	 * @throws IOException
	 */
	@Override
	protected File checkNew(File file) throws IOException {
		if (!oortConfig.isAllowFileOverWrite() && file.exists() && file.isFile()) {
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
		 * For the command line version this is always false, as no dialog 
		 * with the user is possible. If the user wants to override file 
		 * the command-line flag '--allowOverwrite' has to be used.
		 */
		return false;
	}

	private static String getPathIRI(String path){
		return path;
	}

	public static void main(String[] args) throws IOException,
	OWLOntologyCreationException, OWLOntologyStorageException,
	OBOFormatDanglingReferenceException {

		OortConfiguration oortConfig = new OortConfiguration();

		int i = 0;
		while (i < args.length) {
			String opt = args[i];
			i++;

			if (opt.trim().length() == 0)
				continue;

			logger.info("processing arg: " + opt);
			if (opt.equals("--h") || opt.equals("--help") || opt.equals("-h")) {
				usage();
				System.exit(0);
			}

			else if (opt.equals("-outdir") || opt.equals("--outdir")) { 
				oortConfig.setBase(new File(args[i])); i++; 
			}
			/*
			 * else if (opt.equals("-owlversion")) { version = args[i]; i++; }
			 */
			else if (opt.equals("-reasoner") || opt.equals("--reasoner")) {
				// TODO - deprecate "-reasoner"
				oortConfig.setReasonerName(args[i]);
				i++;
			}
			else if (opt.equals("--no-reasoner")) {
				oortConfig.setReasonerName(null);
			}
			else if (opt.equals("--prefix")) {
				oortConfig.addSourceOntologyPrefix(args[i]);
				i++;
			}
			else if (opt.equals("--enforceEL")) {
				// If this option is active, the ontology is 
				// restricted to EL before reasoning!
				oortConfig.setEnforceEL(true);
			}
			else if (opt.equals("--makeEL")) {
				// If this option is active, an EL restricted ontology 
				// is written after reasoning.
				oortConfig.setWriteELOntology(true);
			}
			/*
			 * else if (opt.equals("-oboincludes")) { oboIncludes = args[i];
			 * i++; }
			 */
			else if (opt.equals("--no-subsets")) {
				oortConfig.setWriteSubsets(false);
			}
			else if (opt.equals("--force")) {
				oortConfig.setForceRelease(true);
			}
			else if (opt.equals("--asserted")) {
				oortConfig.setAsserted(true);
			}
			else if (opt.equals("--simple")) {
				oortConfig.setSimple(true);
			}
			else if (opt.equals("--expand-xrefs")) {
				oortConfig.setExpandXrefs(true);
			}
			else if (opt.equals("--re-mireot")) {
				oortConfig.setRecreateMireot(true);
			}
			else if (opt.equals("--repair-cardinality")) {
				oortConfig.setRepairAnnotationCardinality(true);
			}
			else if (opt.equals("--justify")) {
				oortConfig.setJustifyAssertedSubclasses(true);
			}
			else if (opt.equals("--allow-equivalent-pairs")) {
				oortConfig.setAllowEquivalentNamedClassPairs(true);
			}
			else if (opt.equals("--expand-macros")) {
				oortConfig.setExpandMacros(true);
				oortConfig.setMacroStrategy(MacroStrategy.GCI);
			}
			else if (opt.equals("--expand-macros-inplace")) {
				oortConfig.setExpandMacros(true);
				oortConfig.setMacroStrategy(MacroStrategy.INPLACE);
			}
			else if (opt.equals("--allow-overwrite")) {
				oortConfig.setAllowFileOverWrite(true);
			}
			else if (opt.equals("--skip-ontology-checks")) {
				oortConfig.setExecuteOntologyChecks(false);
			}
			else if (opt.equals("--bridge-ontology") || opt.equals("-b")) {
				oortConfig.addBridgeOntology(args[i]);
				i++;
			}
			else if (opt.equals("--config-file")) {
				File file = new File(args[i]);
				OortConfiguration.loadConfig(file , oortConfig);
				i++;
			}
			else {
				String tokens[] = opt.split(" ");
				for (String token : tokens)
					oortConfig.addPath(token);
			}
		}

		logger.info("Base directory path " + oortConfig.getBase().getAbsolutePath());

		OboOntologyReleaseRunner oorr = new OboOntologyReleaseRunner(oortConfig, oortConfig.getBase());

		try {
			boolean success = oorr.createRelease(oortConfig.getPaths());
			String message;
			if (success) {
				message = "Finished release manager process";
			}
			else {
				message = "Finished release manager process, but no release was created.";
			}
			logger.info(message);
			logger.info("Done!");
		} catch (OboOntologyReleaseRunnerCheckException exception) {
			logger.error("Stopped Release process. Reason: "+exception.renderMessageString());
		} catch (AnnotationCardinalityException exception) {
			logger.error("Stopped Release process. Reason: "+exception.getMessage());
		} finally {
			logger.info("deleting lock file");
			oorr.deleteLockFile();
		}
	}

	public boolean createRelease(Vector<String> paths) throws IOException, 
	OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException,
	OboOntologyReleaseRunnerCheckException, AnnotationCardinalityException
	{
		String path = null;

		if (paths.size() > 0)
			path = getPathIRI( paths.get(0) );

		logger.info("Processing Ontologies: " + paths);

		parser = new ParserWrapper();
		mooncat = new Mooncat(parser.parseToOWLGraph(path));
		owlpp = new OWLPrettyPrinter(mooncat.getGraph());

		// A bridge ontology contains axioms connecting classes from different ontologies,
		// but no class declarations or class metadata.
		// Bridge ontologies are commonly used (e.g. GO, phenotype ontologies) to store
		// logical definitions such that the core ontology includes no dangling references.
		// Here we merge in the bridge ontologies into the core ontology
		for (String f : oortConfig.getBridgeOntologies()) {
			OWLOntology ont = parser.parse(f);
			logger.info("Merging "+ont+" loaded from "+f);
			mooncat.getGraph().mergeOntology(ont);
		}


		for (int k = 1; k < paths.size(); k++) {
			String p = getPathIRI(paths.get(k));
			OWLOntology ont = parser.parse(p);
			if (oortConfig.isAutoDetectBridgingOntology() && isBridgingOntology(ont))
				mooncat.mergeIntoReferenceOntology(ont);
			else
				mooncat.addReferencedOntology(ont);
		}
		if (oortConfig.getSourceOntologyPrefixes() != null) {
			mooncat.setSourceOntologyPrefixes(oortConfig.getSourceOntologyPrefixes());
		}

		if (oortConfig.isExecuteOntologyChecks()) {
			OntologyCheckHandler.DEFAULT_INSTANCE.afterLoading(mooncat .getGraph());
		}
		String version = OntologyVersionTools.getOntologyVersion(mooncat.getOntology());
		if (version != null) {
			if (OntologyVersionTools.isOBOOntologyVersion(version)) {
				Date versionDate = OntologyVersionTools.parseVersion(version);
				version = OntologyVersionTools.format(versionDate);
			}
		}
		else {
			version = OntologyVersionTools.getOboInOWLVersion(mooncat.getOntology());
		}

		if (version == null) {
			// TODO add an option to set the version manually
		}

		version = buildVersionInfo(version);
		OntologyVersionTools.setOboInOWLVersion(mooncat.getOntology(), version);
		// the versionIRI for in the ontologyID is set during write out, 
		// as they are specific to the file name

		String ontologyId = Owl2Obo.getOntologyId(mooncat.getOntology());
		ontologyId = ontologyId.replaceAll(".obo$", ""); // temp workaround

		List<String> reasonerReportLines = new ArrayList<String>();

		// ----------------------------------------
		// Macro expansion
		// ----------------------------------------
		OWLOntology gciOntology = null;
		if (oortConfig.isExpandMacros()) {
			logger.info("expanding macros");
			if (oortConfig.getMacroStrategy() == MacroStrategy.GCI) {
				MacroExpansionGCIVisitor gciVisitor = 
					new MacroExpansionGCIVisitor(mooncat.getOntology());
				gciOntology = gciVisitor.createGCIOntology();
				logger.info("GCI Ontology has "+gciOntology.getAxiomCount()+" axioms");
			}
			else {
				OWLOntology ont = mooncat.getOntology();
				MacroExpansionVisitor mev = 
					new MacroExpansionVisitor(ont);
				ont = mev.expandAll();		
				mooncat.setOntology(ont);
				logger.info("Expanded in place; Ontology has "+ont.getAxiomCount()+" axioms");
			}

		}

		// ----------------------------------------
		// Bridge files
		// ----------------------------------------
		if (oortConfig.isExpandXrefs()) {
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
					saveOntologyInAllFormats(tOntId, tOntId, tOnt, null);
				}
			} catch (InvalidXrefMapException e) {
				logger.info("Problem during Xref expansion: "+e.getMessage(), e);
			}

			// TODO - option to generate imports
		}

		// ----------------------------------------
		// Asserted (non-classified)
		// ----------------------------------------

		if (oortConfig.isAsserted()) {
			logger.info("Creating Asserted Ontology");
			saveInAllFormats(ontologyId, "non-classified", gciOntology);
			logger.info("Asserted Ontology Creation Completed");
		}

		// ----------------------------------------
		// Merge in external ontologies
		// ----------------------------------------
		if (oortConfig.isRecreateMireot()) {
			logger.info("Number of dangling classes in source: "+mooncat.getDanglingClasses().size());
			logger.info("Merging Ontologies (only has effect if multiple ontologies are specified)");
			mooncat.mergeOntologies();
			if (oortConfig.isRepairAnnotationCardinality()) {
				logger.info("Checking and repair annotation cardinality constrains");
				OntologyMetaDataTools.checkAnnotationCardinality(mooncat.getOntology());
			}
			saveInAllFormats(ontologyId, "merged", gciOntology);

			logger.info("Number of dangling classes in source (post-merge): "+mooncat.getDanglingClasses().size());

			// TODO: option to save as imports
		}

		if (oortConfig.isExecuteOntologyChecks()) {
			OntologyCheckHandler.DEFAULT_INSTANCE.afterMireot(mooncat.getGraph());
		}

		// ----------------------------------------
		// Main (asserted plus inference of non-redundant links)
		// ----------------------------------------
		// this is the same as ASSERTED, with certain axioms ADDED based on reasoner results

		// this is always on by default
		//  at some point we may wish to make this optional,
		//  but a user would rarely choose to omit the main ontology
		if (true) {

			logger.info("Creating basic ontology");

			if (oortConfig.getReasonerName() != null) {
				OWLDataFactory df = mooncat.getGraph().getDataFactory();
				Set<OWLSubClassOfAxiom> removedSubClassOfAxioms = new HashSet<OWLSubClassOfAxiom>();
				Set<RemoveAxiom> removedSubClassOfAxiomChanges = new HashSet<RemoveAxiom>();
				infBuilder = new InferenceBuilder(mooncat.getGraph(), oortConfig.getReasonerName(), oortConfig.isEnforceEL());

				// optionally remove a subset of the axioms we want to attempt to recapitulate
				if (oortConfig.isJustifyAssertedSubclasses()) {
					logger.info("Removing asserted subclasses between defined class pairs");
					for (OWLSubClassOfAxiom a : mooncat.getOntology().getAxioms(AxiomType.SUBCLASS_OF)) {
						OWLClassExpression subc = a.getSubClass();
						if (!(subc instanceof OWLClass)) {
							continue;
						}
						OWLClassExpression supc = a.getSuperClass();
						if (!(supc instanceof OWLClass)) {
							continue;
						}
						if (((OWLClass)subc).getEquivalentClasses(mooncat.getOntology()).size() == 0) {
							continue;
						}
						if (((OWLClass)supc).getEquivalentClasses(mooncat.getOntology()).size() == 0) {
							continue;
						}
						RemoveAxiom rmax = new RemoveAxiom(mooncat.getOntology(),a);
						removedSubClassOfAxiomChanges.add(rmax);
						removedSubClassOfAxioms.add(df.getOWLSubClassOfAxiom(a.getSubClass(),
								a.getSuperClass()));
					}
					for (RemoveAxiom rmax : removedSubClassOfAxiomChanges) {
						mooncat.getManager().applyChange(rmax);
					}
				}

				logger.info("Creating inferences");				
				List<OWLAxiom> axioms = infBuilder.buildInferences();

				// ASSERT INFERRED LINKS
				// TODO: ensure there is a subClassOf axiom for ALL classes that have an equivalence axiom
				for(OWLAxiom ax: axioms) {
					if (ax instanceof OWLSubClassOfAxiom && 
							((OWLSubClassOfAxiom)ax).getSuperClass().equals(df.getOWLThing())) {
						continue;
					}
					String ppax = owlpp.render(ax);

					mooncat.getManager().applyChange(new AddAxiom(mooncat.getOntology(), ax));
					String info;
					if (oortConfig.isJustifyAssertedSubclasses()) {
						if (removedSubClassOfAxioms.contains(ax)) {
							info = "EXISTS, ENTAILED";
						}
						else {
							info = "NEW, INFERRED";
						}
					}
					else {
						info = "NEW, INFERRED";
					}
					if (ax instanceof OWLSubClassOfAxiom && 
							!(((OWLSubClassOfAxiom)ax).getSuperClass() instanceof OWLClass)) {
						// because the reasoner API can only generated subclass axioms with named superclasses,
						// we assume that any that have anonymous expressions as superclasses were generated
						// by the inference builder in the process of translating equivalence axioms
						// to weaker subclass axioms
						info = "NEW, TRANSLATED";
					}
					String rptLine = info+"\t"+ppax;
					reasonerReportLines.add(rptLine);
					logger.info(rptLine);
				}
				if (oortConfig.isJustifyAssertedSubclasses()) {
					for (OWLSubClassOfAxiom ax : removedSubClassOfAxioms) {
						if (!axioms.contains(ax)) {
							reasonerReportLines.add("EXITS, NOT-ENTAILED\t"+owlpp.render(ax));
							// add it back.
							//  note that we won't have entailments that came from this
							mooncat.getManager().addAxiom(mooncat.getOntology(), ax);
						}
					}
				}
				logger.info("Inferences creation completed");

				// CONSISTENCY CHECK
				if (oortConfig.isCheckConsistency()) {
					logger.info("Checking consistency");
					List<String> incs = infBuilder.performConsistencyChecks();
					if (incs.size() > 0) {
						for (String inc  : incs) {
							String message = "INCONSISTENCY\t" + inc;
							reasonerReportLines.add(message);
							logger.error(message);
						}
						// TODO: allow --force option
						// TODO: proper exception mechanism - delay until end?
						if (!oortConfig.isForceRelease()) {
							saveReasonerReport(ontologyId, reasonerReportLines);
							throw new OboOntologyReleaseRunnerCheckException("Found inconsistencies during intial checks.",incs, "Use ForceRelease option to ignore this warning.");
						}
					}
					logger.info("Checking consistency completed");
				}

				// TEST FOR EQUIVALENT NAMED CLASS PAIRS
				if (true) {
					if (infBuilder.getEquivalentNamedClassPairs().size() > 0) {
						logger.warn("Found equivalencies between named classes");
						List<String> reasons = new ArrayList<String>();
						for (OWLEquivalentClassesAxiom eca : infBuilder.getEquivalentNamedClassPairs()) {
							String axiomString = owlpp.render(eca);
							reasons.add(axiomString);
							String message = "EQUIVALENT_CLASS_PAIR\t"+axiomString;
							reasonerReportLines.add(message);
							logger.warn(message);
						}
						if (oortConfig.isAllowEquivalentNamedClassPairs() == false) {
							// TODO: allow --force option
							// TODO: proper exception mechanism - delay until end?
							if (!oortConfig.isForceRelease()) {
								saveReasonerReport(ontologyId, reasonerReportLines);
								throw new OboOntologyReleaseRunnerCheckException("Found equivalencies between named classes.", reasons, "Use ForceRelease option to ignore this warning.");
							}
						}

					}
				}

				// REDUNDANT AXIOMS
				logger.info("Finding redundant axioms");
				for (OWLAxiom ax : infBuilder.getRedundantAxioms()) {
					// TODO - in future do not remove axioms that are annotated
					logger.info("Removing redundant axiom:"+ax+" // " + owlpp.render(ax));
					reasonerReportLines.add("REDUNDANT\t"+owlpp.render(ax));
					mooncat.getManager().applyChange(new RemoveAxiom(mooncat.getOntology(), ax));					
				}

				logger.info("Redundant axioms removed");
			}
			if (oortConfig.isExecuteOntologyChecks()) {
				OntologyCheckHandler.DEFAULT_INSTANCE.afterReasoning(mooncat.getGraph());
			}

			saveInAllFormats(ontologyId, null, gciOntology);

			saveReasonerReport(ontologyId, reasonerReportLines);


		}
		// ----------------------------------------
		// SUBSETS
		// ----------------------------------------
		// including: named subsets, profile subsets (e.g. EL), simple subsets

		if (oortConfig.isWriteSubsets()) {
			// named subsets
			logger.info("writing named subsets");
			Set<String> subsets = mooncat.getGraph().getAllUsedSubsets();
			for (String subset : subsets) {
				Set<OWLClass> objs = mooncat.getGraph().getOWLClassesInSubset(subset);
				logger.info("subset:"+subset+" #classes:"+objs.size());
				String fn = "subsets/"+subset;

				IRI iri = IRI.create("http://purl.obolibrary.org/obo/"+ontologyId+fn+".owl");
				OWLOntology subOnt = mooncat.makeSubsetOntology(objs,iri);
				logger.info("subOnt:"+subOnt+" #axioms:"+subOnt.getAxiomCount());
				saveOntologyInAllFormats(ontologyId, fn, subOnt, gciOntology);
			}

		}

		// write EL version
		if(oortConfig.isWriteELOntology()) {
			logger.info("Creating EL ontology");
			OWLGraphWrapper elGraph = InferenceBuilder.enforceEL(mooncat.getGraph());
			saveInAllFormats(ontologyId, "el", elGraph.getSourceOntology(), gciOntology);
			logger.info("Finished Creating EL ontology");
		}

		// ----------------------------------------
		// Simple (no MIREOTs, no imports)
		// ----------------------------------------
		// this is the same as MAIN, with certain axiom REMOVED
		if (oortConfig.isSimple()) {

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

			Set<OWLEquivalentClassesAxiom> rmAxs = mooncat.getOntology().getAxioms(AxiomType.EQUIVALENT_CLASSES);
			logger.info("Removing "+rmAxs.size()+" EquivalentClasses axioms from simple");
			mooncat.getManager().removeAxioms(mooncat.getOntology(), rmAxs);

			saveInAllFormats(ontologyId, "simple", gciOntology);
			logger.info("Creating simple ontology completed");

		}		


		// ----------------------------------------
		// End of export file creation
		// ----------------------------------------

		boolean success = commit(version);
		return success;
	}


	/**
	 * Uses reasoner to obtained inferred subclass axioms, and then adds the non-redundant
	 * ones to he ontology
	 * 
	 * @return axioms
	 */
	private List<OWLAxiom> buildInferences() {

		OWLGraphWrapper graph = mooncat.getGraph();

		List<OWLAxiom> axioms = infBuilder.buildInferences();

		// TODO: ensure there is a subClassOf axiom for ALL classes that have an equivalence axiom
		for(OWLAxiom ax: axioms){
			logger.info("New axiom:"+ax+" // " + owlpp.render(ax));
			mooncat.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), ax));
		}		

		// note: not used
		return axioms;
	}

	/**
	 * @param ontologyId
	 * @param ext
	 * @param gciOntology
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 */
	private void saveInAllFormats(String ontologyId, String ext, OWLOntology gciOntology) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		saveInAllFormats(ontologyId, ext, mooncat.getOntology(), gciOntology);
	}

	private void saveInAllFormats(String ontologyId, String ext, OWLOntology ontologyToSave, OWLOntology gciOntology) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {
		String fn = ext == null ? ontologyId :  ontologyId + "-" + ext;
		saveOntologyInAllFormats(ontologyId, fn, ontologyToSave, gciOntology);
	}

	private void saveOntologyInAllFormats(String ontologyId, String fileNameBase, OWLOntology ontologyToSave, OWLOntology gciOntology) throws OWLOntologyStorageException, IOException, OWLOntologyCreationException {

		logger.info("Saving: "+fileNameBase);

		final OWLOntologyManager manager = mooncat.getManager();

		// if we add a new ontology id, remember the change, to restore the original 
		// ontology id after writing into a file.
		SetOntologyID reset = null;
		Date date = null;

		// check if there is an existing version
		// if it is of unknown format do not modify
		String version = OntologyVersionTools.getOntologyVersion(ontologyToSave);
		if (version == null) {
			// did not find a version in the ontology id, try OboInOwl instead
			date = OntologyVersionTools.getOboInOWLVersionDate(ontologyToSave);
		}
		else if(OntologyVersionTools.isOBOOntologyVersion(version)) {
			// try to retrieve the existing version and parse it.
			date = OntologyVersionTools.parseVersion(version);

			// if parsing was unsuccessful, use current date
			if (date == null) {
				// fall back, if there was an parse error use current date.
				date = new Date();
			}
		}

		if (date != null) {
			SetOntologyID change = OntologyVersionTools.setOntologyVersion(ontologyToSave, date, ontologyId, fileNameBase);
			// create change axiom with original id
			reset = new SetOntologyID(ontologyToSave, change.getOriginalOntologyID());
		}
		OutputStream os = getOutputSteam(fileNameBase +".owl");
		manager.saveOntology(ontologyToSave, oortConfig.getDefaultFormat(), os);
		os.close();

		OutputStream osxml = getOutputSteam(fileNameBase +".owx");
		manager.saveOntology(ontologyToSave, oortConfig.getOwlXMLFormat(), osxml);
		osxml.close();

		if (reset != null) {
			// reset versionIRI
			// the reset is required, because each owl file 
			// has its corresponding file name in the version IRI.
			manager.applyChange(reset);
		}

		if (gciOntology != null) {
			OWLOntologyManager gciManager = gciOntology.getOWLOntologyManager();

			// create specific import for the generated owl ontology
			OWLImportsDeclaration importDeclaration = new OWLImportsDeclarationImpl(IRI.create(fileNameBase +".owl"));
			AddImport addImport = new AddImport(gciOntology, importDeclaration);
			RemoveImport removeImport = new RemoveImport(gciOntology, importDeclaration);

			gciManager.applyChange(addImport);
			OutputStream gciOS = getOutputSteam(fileNameBase +"-aux.owl");
			gciManager.saveOntology(gciOntology, oortConfig.getDefaultFormat(), gciOS);
			gciOS.close();

			OutputStream gciOSxml = getOutputSteam(fileNameBase +"-aux.owx");
			gciManager.saveOntology(gciOntology, oortConfig.getOwlXMLFormat(), gciOSxml);
			gciOSxml.close();

			gciManager.applyChange(removeImport);
		}

		Owl2Obo owl2obo = new Owl2Obo();
		OBODoc doc = owl2obo.convert(ontologyToSave);

		OBOFormatWriter writer = new OBOFormatWriter();

		BufferedWriter bwriter = getWriter(fileNameBase +".obo");

		writer.write(doc, bwriter);

		bwriter.close();

		if (oortConfig.isWriteMetadata()) {
			saveMetadata(fileNameBase, mooncat.getGraph());
		}
	}

	private void saveReasonerReport(String ontologyId,
			List<String> reasonerReportLines) {
		String fn = ontologyId + "-reasoner-report.txt";
		OutputStream fos;
		try {
			fos = getOutputSteam(fn);
			PrintStream stream = new PrintStream(new BufferedOutputStream(fos));
			Collections.sort(reasonerReportLines);
			for (String s : reasonerReportLines) {
				stream.println(s);
			}
			stream.close();
		} catch (IOException e) {
			logger.warn("Could not print reasoner report for ontolog: "+ontologyId, e);
		}
	}
	
	private void saveMetadata(String ontologyId,
			OWLGraphWrapper graph) {
		String fn = ontologyId + "-metadata.txt";
		OutputStream fos;
		try {
			fos = getOutputSteam(fn);
			PrintWriter pw = new PrintWriter(fos);
			OntologyMetadata omd = new OntologyMetadata(pw);
			omd.generate(graph);
			pw.close();
			fos.close();
		} catch (IOException e) {
			logger.warn("Could not print reasoner report for ontolog: "+ontologyId, e);
		}
	}


	private boolean isBridgingOntology(OWLOntology ont) {
		for (OWLClass c : ont.getClassesInSignature(true)) {

			if (ont.getDeclarationAxioms(c).size() > 0) {
				if (mooncat.getOntology().getDeclarationAxioms(c).size() >0) {
					// class already declared in main ontology - a 2ary ontology MUST
					// declare at least one of its own classes if it is a bone-fide non-bridging ontology
				}
				else if (mooncat.isDangling(ont, c)) {
					// a dangling class has no OWL annotations.
					// E.g. bp_xp_cl contains CL classes as dangling
				}
				else {
					logger.info(c+" has declaration axioms, is not in main, and is not dangling, therefore "+ont+" is NOT a bridging ontology");
					return false;
				}
				}
			}
			logger.info(ont+" is a bridging ontology");
			return true;
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
	}
