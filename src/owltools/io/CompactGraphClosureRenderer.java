package owltools.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

/**
 * Similar to GraphClosureRenderer, writes in a more compact format
 * 
 * @author cjm
 *
 */
public class CompactGraphClosureRenderer extends AbstractClosureRenderer implements GraphRenderer {

	String base = "http://purl.obolibrary.org/obo/";
	
	public CompactGraphClosureRenderer(PrintStream stream) {
		super(stream);
	}

	public CompactGraphClosureRenderer(String file) {
		super(file);
	}

	public void render(OWLGraphWrapper g) {
		g.getConfig().isCacheClosure = false;
		int i = 0;
		for (OWLObject obj : g.getAllOWLObjects()) {
			if (!(obj instanceof OWLNamedObject)) {
				return;
			}

			i++;
			crender((OWLNamedObject) obj);
			for (OWLGraphEdge e : g.getOutgoingEdgesClosure(obj)) {
				render(e);
			}
			nl();
		}
		stream.close();
	}

	public void render(OWLGraphEdge e) {
		if (!(e.getTarget() instanceof OWLNamedObject)) {
			return;
		}
		sep();
		int n = 0;
		for (OWLQuantifiedProperty qp : e.getQuantifiedPropertyList()) {
			if (n>0) {
				stream.print(",");
			}
			if (qp.hasProperty()) {
				crender(qp.getProperty());
				stream.print(" ");
			}
			stream.print(qp.getQuantifier());

			n++;
		}
		stream.print(",");
		crender(e.getTarget());
		stream.print(","+e.getDistance());
	}
	
	protected void crender(OWLObject obj) {
		String s = ((OWLNamedObject) obj).getIRI().toString();
		if (s.startsWith(base)) {
			stream.print(s.replaceFirst(base, ""));
		}
		else {
			stream.print(s);
		}
	}


}
