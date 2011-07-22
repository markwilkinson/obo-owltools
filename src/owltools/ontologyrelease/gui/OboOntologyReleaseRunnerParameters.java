package owltools.ontologyrelease.gui;

import java.io.File;
import java.util.Vector;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.InferenceBuilder;

/**
 * Simple wrapper for the parameters for the release manager.
 */
public class OboOntologyReleaseRunnerParameters {
	private OWLOntologyFormat format = new RDFXMLOntologyFormat();
	private String reasoner = InferenceBuilder.REASONER_HERMIT;
	private boolean asserted = true; // set selected as default (different from the CLI parameter!)
	private boolean simple = true; // set selected as default (different from the CLI parameter!)
	private Vector<String> paths;
	private File base = new File(".");
	private boolean allowOverwrite = false;

	/**
	 * @return the format
	 */
	public OWLOntologyFormat getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(OWLOntologyFormat format) {
		this.format = format;
	}

	/**
	 * @return the reasoner
	 */
	public String getReasoner() {
		return reasoner;
	}

	/**
	 * @param reasoner the reasoner to set
	 */
	public void setReasoner(String reasoner) {
		this.reasoner = reasoner;
	}

	/**
	 * @return the asserted
	 */
	public boolean isAsserted() {
		return asserted;
	}

	/**
	 * @param asserted the asserted to set
	 */
	public void setAsserted(boolean asserted) {
		this.asserted = asserted;
	}

	/**
	 * @return the simple
	 */
	public boolean isSimple() {
		return simple;
	}

	/**
	 * @param simple the simple to set
	 */
	public void setSimple(boolean simple) {
		this.simple = simple;
	}

	/**
	 * @return the paths
	 */
	public Vector<String> getPaths() {
		return paths;
	}

	/**
	 * @param paths the paths to set
	 */
	public void setPaths(Vector<String> paths) {
		this.paths = paths;
	}

	/**
	 * @return the base
	 */
	public File getBase() {
		return base;
	}

	/**
	 * @param base the base to set
	 */
	public void setBase(File base) {
		this.base = base;
	}

	/**
	 * @return the allowOverwrite
	 */
	public boolean isAllowOverwrite() {
		return allowOverwrite;
	}

	/**
	 * @param allowOverwrite the allowOverwrite to set
	 */
	public void setAllowOverwrite(boolean allowOverwrite) {
		this.allowOverwrite = allowOverwrite;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OboOntologyReleaseRunnerParameters [");
		if (format != null) {
			builder.append("format=");
			builder.append(format.getClass().getSimpleName());
			builder.append(", ");
		}
		if (reasoner != null) {
			builder.append("reasoner=");
			builder.append(reasoner);
			builder.append(", ");
		}
		builder.append("asserted=");
		builder.append(asserted);
		builder.append(", simple=");
		builder.append(simple);
		builder.append(", ");
		builder.append(", allowOverwrite=");
		builder.append(allowOverwrite);
		builder.append(", ");
		if (paths != null) {
			builder.append("paths=");
			builder.append(paths);
			builder.append(", ");
		}
		if (base != null) {
			builder.append("base=");
			builder.append(base);
		}
		builder.append("]");
		return builder.toString();
	}
}
