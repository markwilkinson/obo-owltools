package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Renders the graph closure - i.e. the inferred paths emanating from all named entities
 * 
 * @author cjm
 *
 */
public abstract class AbstractClosureRenderer implements GraphRenderer {

	protected OWLGraphWrapper graph;
	protected OWLPrettyPrinter prettyPrinter;
	protected PrintStream stream;

	public AbstractClosureRenderer(PrintStream stream) {
		super();
		this.stream = stream;
	}

	public AbstractClosureRenderer(String file) {
		super();
		setStream(file);
	}
	
	
	
	public PrintStream getStream() {
		return stream;
	}

	public void setStream(PrintStream stream) {
		this.stream = stream;
	}
	
	public void setStream(String file) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			this.stream = new PrintStream(new BufferedOutputStream(fos));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void render(OWLGraphWrapper g) {
		graph = g;
		
		Set<OWLObject> objs = new HashSet<OWLObject>(g.getSourceOntology().getClassesInSignature(false));
		objs.addAll(g.getSourceOntology().getIndividualsInSignature(false));

		for (OWLObject obj : objs) {
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				render(e);
			}
		}
		stream.close();
	}
	

	public abstract void render(OWLGraphEdge e);
	
	protected void print(OWLObject obj) {
		stream.print(prettyPrinter.render(obj));
	}
	
	protected void print(String s) {
		stream.print(s);
	}


	protected void sep() {
		stream.print("\t");
	}

	protected void nl() {
		stream.print("\n");
	}

}

