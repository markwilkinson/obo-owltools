package owltools.cli;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousClassExpression;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.cli.tools.CLIMethod;
import owltools.graph.OWLGraphEdge;
import owltools.io.OWLPrettyPrinter;
import owltools.sim.DescriptionTreeSimilarity;
import owltools.sim.MultiSimilarity;
import owltools.sim.OWLObjectPair;
import owltools.sim.Reporter;
import owltools.sim.SimEngine;
import owltools.sim.SimEngine.SimilarityAlgorithmException;
import owltools.sim.SimSearch;
import owltools.sim.Similarity;

/**
 * Semantic similarity and information content.
 * 
 * @author cjm
 *
 */
public class SimCommandRunner extends SolrCommandRunner {

	private static final Logger LOG = Logger.getLogger(SimCommandRunner.class);

	private OWLOntology simOnt = null;
	private String similarityAlgorithmName = "JaccardSimilarity";

	@CLIMethod("--sim-method")
	public void setSimMethod(Opts opts) {
		opts.info("metric", "sets deafult similarity metric. Type --all to show all TODO");
		similarityAlgorithmName = opts.nextOpt();
	}
	
	@CLIMethod("--sim-all")
	public void simAll(Opts opts) throws SimilarityAlgorithmException {
		opts.info("", "calculates similarity between all pairs");
		Double minScore = null;
		SimEngine se = new SimEngine(g);
		if (opts.hasOpts()) {
			if (opts.nextEq("-m|--min")) {
				minScore = Double.valueOf(opts.nextOpt());
			}
			else if (opts.nextEq("-s|--subclass-of")) {
				se.comparisonSuperclass = resolveEntity(opts);
			}
		}
		//Similarity metric = se.getSimilarityAlgorithm(similarityAlgorithmName);
		//SimilarityAlgorithm metric = se.new JaccardSimilarity();
		se.calculateSimilarityAllByAll(similarityAlgorithmName, minScore);
		//System.out.println(metric.getClass().getName());
	}
	
	@CLIMethod("--save-sim")
	public void saveSim(Opts opts) throws Exception {
		opts.info("FILE", "saves similarity results as an OWL ontology. Use after --sim or --sim-all");
		pw.saveOWL(simOnt, opts.nextOpt());
	}
	
	@CLIMethod("--merge-sim")
	public void mergeSim(Opts opts) throws Exception {
		opts.info("FILE", "merges similarity results into source OWL ontology. Use after --sim or --sim-all");
		g.mergeOntology(simOnt);
	}
	
	@CLIMethod("--sim")
	public void sim(Opts opts) throws Exception {
		Reporter reporter = new Reporter(g);
		opts.info("[-m metric] A B", "calculates similarity between A and B");
		boolean nr = false;
		Vector<OWLObjectPair> pairs = new Vector<OWLObjectPair>();
		String subSimMethod = null;

		boolean isAll = false;
		SimEngine se = new SimEngine(g);
		while (opts.hasOpts()) {
			System.out.println("sub-opts for --sim");
			if (opts.nextEq("-m")) {
				similarityAlgorithmName = opts.nextOpt();
			}
			else if (opts.nextEq("-p")) {
				se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
			}
			else if (opts.nextEq("--min-ic")) {
				se.minimumIC = Double.valueOf(opts.nextOpt());
			}
			else if (opts.nextEq("--sub-method")) {
				opts.info("MethodName","sets the method used to compare all attributes in a MultiSim test");
				subSimMethod = opts.nextOpt();
			}
			else if (opts.nextEq("--query")) {
				OWLObject q = resolveEntity(opts.nextOpt());
				SimSearch search = new SimSearch(se, reporter);

				isAll = true;
				boolean isClasses = true;
				boolean isInstances = true;
				int MAX_PAIRS = 50; // todo - make configurable
				while (opts.hasOpts()) {
					if (opts.nextEq("-i"))
						isClasses = false;
					else if (opts.nextEq("-c"))
						isInstances = false;
					else if (opts.nextEq("--max-hits"))
						MAX_PAIRS = Integer.parseInt(opts.nextOpt());
					else
						break;
				}
				search.setMaxHits(MAX_PAIRS);
				OWLObject cc = resolveEntity(opts.nextOpt());
				Set<OWLObject> candidates = g.queryDescendants((OWLClass)cc, isInstances, isClasses);
				candidates.remove(cc);
				search.setCandidates(candidates);
				System.out.println("  numCandidates:"+candidates.size());

				List<OWLObject> hits = search.search(q);
				System.out.println("  hits:"+hits.size());
				int n = 0;
				for (OWLObject hit : hits) {
					if (n < MAX_PAIRS)
						pairs.add(new OWLObjectPair(q,hit));
					n++;
					System.out.println("HIT:"+n+"\t"+g.getLabelOrDisplayId(hit));
				}
				while (opts.nextEq("--include")) {
					OWLObjectPair pair = new OWLObjectPair(q,resolveEntity(opts.nextOpt()));

					if (!pairs.contains(pair)) {
						pairs.add(pair);
						System.out.println("adding_extra_pair:"+pair);
					}
					else {
						System.out.println("extra_pair_alrwady_added:"+pair);
					}
				}
			}
			else if (opts.nextEq("-a|--all")) {
				isAll = true;
				boolean isClasses = true;
				boolean isInstances = true;
				if (opts.nextEq("-i"))
					isClasses = false;
				if (opts.nextEq("-c"))
					isInstances = false;
				OWLObject anc = resolveEntity(opts.nextOpt());
				System.out.println("Set1:"+anc+" "+anc.getClass());
				Set<OWLObject> objs = g.queryDescendants((OWLClass)anc, isInstances, isClasses);
				objs.remove(anc);
				System.out.println("  Size1:"+objs.size());
				Set<OWLObject> objs2 = objs;
				if (opts.nextEq("--vs")) {
					OWLObject anc2 = resolveEntity(opts.nextOpt());
					System.out.println("Set2:"+anc2+" "+anc2.getClass());
					objs2 = g.queryDescendants((OWLClass)anc2, isInstances, isClasses);
					objs2.remove(anc2);
					System.out.println("  Size2:"+objs2.size());
				}
				for (OWLObject a : objs) {
					if (!(a instanceof OWLNamedObject)) {
						continue;
					}
					for (OWLObject b : objs2) {
						if (!(b instanceof OWLNamedObject)) {
							continue;
						}
						if (a.equals(b))
							continue;
						//if (a.compareTo(b) <= 0)
						//	continue;
						OWLObjectPair pair = new OWLObjectPair(a,b);
						System.out.println("Scheduling:"+pair);
						pairs.add(pair);
					}							
				}

			}
			else if (opts.nextEq("-s|--subclass-of")) {
				se.comparisonSuperclass = resolveEntity(opts);
			}
			else if (opts.nextEq("--no-create-reflexive")) {
				nr = true;
			}
			else {
				// not recognized - end of this block of opts
				break;
				//System.err.println("???"+opts.nextOpt());
			}
		}
		if (isAll) {
			// TODO
			//se.calculateSimilarityAllByAll(similarityAlgorithmName, 0.0);
		}
		else {
			pairs.add(new OWLObjectPair(resolveEntity(opts.nextOpt()),
					resolveEntity(opts.nextOpt())));

		}
		for (OWLObjectPair pair : pairs) {

			OWLObject oa = pair.getA();
			OWLObject ob = pair.getB();

			Similarity metric = se.getSimilarityAlgorithm(similarityAlgorithmName);
			if (nr) {
				((DescriptionTreeSimilarity)metric).forceReflexivePropertyCreation = false;
			}
			if (subSimMethod != null)
				((MultiSimilarity)metric).setSubSimMethod(subSimMethod);

			System.out.println("comparing: "+oa+" vs "+ob);
			Similarity r = se.calculateSimilarity(metric, oa, ob);
			//System.out.println(metric+" = "+r);
			metric.print();
			metric.report(reporter);
			if (simOnt == null) {
				simOnt = g.getManager().createOntology();
			}
			if (opts.hasOpt("--save-sim")) {
				metric.addResultsToOWLOntology(simOnt);
			}
		}
	}
	
	@CLIMethod("--lcsx")
	public void lcsx(Opts opts) {
		owlpp = new OWLPrettyPrinter(g);

		opts.info("LABEL", "anonymous class expression 1");
		OWLObject a = resolveEntity( opts);

		opts.info("LABEL", "anonymous class expression 2");
		OWLObject b = resolveEntity( opts);
		System.out.println(a+ " // "+a.getClass());
		System.out.println(b+ " // "+b.getClass());

		SimEngine se = new SimEngine(g);
		OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);

		System.out.println("LCS:"+owlpp.render(lcs));
	}
	
	@CLIMethod("--lcsx-all")
	public void lcsxAll(Opts opts) throws Exception {
		opts.info("LABEL", "ont 1");
		String ont1 = opts.nextOpt();

		opts.info("LABEL", "ont 2");
		String ont2 = opts.nextOpt();

		if (simOnt == null) {
			simOnt = g.getManager().createOntology();
		}

		SimEngine se = new SimEngine(g);

		Set <OWLObject> objs1 = new HashSet<OWLObject>();
		Set <OWLObject> objs2 = new HashSet<OWLObject>();

		System.out.println(ont1+" -vs- "+ont2);
		for (OWLObject x : g.getAllOWLObjects()) {
			if (! (x instanceof OWLClass))
				continue;
			String id = g.getIdentifier(x);
			if (id.startsWith(ont1)) {
				objs1.add(x);
			}
			if (id.startsWith(ont2)) {
				objs2.add(x);
			}
		}
		Set<OWLClassExpression> lcsh = new HashSet<OWLClassExpression>();
		owlpp = new OWLPrettyPrinter(g);
		owlpp.hideIds();
		for (OWLObject a : objs1) {
			for (OWLObject b : objs2) {
				OWLClassExpression lcs = se.getLeastCommonSubsumerSimpleClassExpression(a, b);
				if (lcs instanceof OWLAnonymousClassExpression) {
					if (lcsh.contains(lcs))
						continue;
					lcsh.add(lcs);
					String label = owlpp.render(lcs);
					IRI iri = IRI.create("http://purl.obolibrary.org/obo/U_"+
							g.getIdentifier(a).replaceAll(":", "_")+"_" 
							+"_"+g.getIdentifier(b).replaceAll(":", "_"));
					OWLClass namedClass = g.getDataFactory().getOWLClass(iri);
					// TODO - use java obol to generate meaningful names
					OWLEquivalentClassesAxiom ax = g.getDataFactory().getOWLEquivalentClassesAxiom(namedClass , lcs);
					g.getManager().addAxiom(simOnt, ax);
					g.getManager().addAxiom(simOnt,
							g.getDataFactory().getOWLAnnotationAssertionAxiom(
									g.getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
									iri,
									g.getDataFactory().getOWLLiteral(label)));
					LOG.info("LCSX:"+owlpp.render(a)+" -vs- "+owlpp.render(b)+" = "+label);
					//LOG.info("  Adding:"+owlpp.render(ax));
					LOG.info("  Adding:"+ax);

				}
			}					
		}
	}
	
	@CLIMethod("--get-ic")
	public void getIC(Opts opts) {
		opts.info("LABEL [-p COMPARISON_PROPERTY_URI]", "calculate information content for class");
		SimEngine se = new SimEngine(g);
		if (opts.nextEq("-p")) {
			se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
		}

		//System.out.println("i= "+i);
		OWLObject obj = resolveEntity( opts);
		System.out.println(obj+ " "+" // IC:"+se.getInformationContent(obj));

	}
	
	@CLIMethod("--ancestors-with-ic")
	public void getAncestorsWithIC(Opts opts) {
		opts.info("LABEL [-p COMPARISON_PROPERTY_URI]", "list edges in graph closure to root nodes, with the IC of the target node");
		SimEngine se = new SimEngine(g);
		if (opts.nextEq("-p")) {
			se.comparisonProperty =  g.getOWLObjectProperty(opts.nextOpt());
		}

		OWLObject obj = resolveEntity(opts);
		System.out.println(obj+ " "+obj.getClass());
		Set<OWLGraphEdge> edges = g.getOutgoingEdgesClosureReflexive(obj);

		for (OWLGraphEdge e : edges) {
			System.out.println(e);
			System.out.println("  TARGET IC:"+se.getInformationContent(e.getTarget()));
		}
	}
	
	@CLIMethod("--all-class-ic")
	public void allClassIC(Opts opts) throws Exception {
		opts.info("", "show calculated Information Content for all classes");
		SimEngine se = new SimEngine(g);
		Similarity sa = se.getSimilarityAlgorithm(similarityAlgorithmName);
		//  no point in caching, as we only check descendants of each object once
		g.getConfig().isCacheClosure = false;
		for (OWLObject obj : g.getAllOWLObjects()) {
			if (se.hasInformationContent(obj)) {
				System.out.println(obj+"\t"+se.getInformationContent(obj));
			}
		}
	}
}
