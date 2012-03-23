package owltools.gfx;

import java.io.File;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class DrawOmmatidiumTest extends OWLToolsTestBasics {

	@Test
	public void testRenderCARO() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("omma.owl"));
		OWLGraphLayoutRenderer r = new OWLGraphLayoutRenderer(g);
		//OWLObject ob = g.getOWLObjectByIdentifier("FBbt:00004199");
		OWLObject ob = g.getOWLObjectByIdentifier("FBbt:00001766");
		System.out.println("drawing: "+ob);
		r.addObject(ob);
		File folder = new File("out/"+getClass().getSimpleName());
		folder.mkdirs();
		r.renderHTML(folder);
	}
	
}
