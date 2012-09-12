package owltools.cli;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Test;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class AbstractCommandRunnerTest extends OWLToolsTestBasics {
	
	protected CommandRunner runner;
	
	protected void init() {
		runner = new CommandRunner();
	}
	
	protected void load(String file) throws Exception {
		String path = getResource(file).getAbsolutePath();
		System.out.println("Loading: "+path);
		run(path);		
	}
	
	protected void run(String[] args) throws Exception {
		runner.run(args);
	}
	protected void run(String argStr) throws Exception {
		run(argStr.split(" "));
	}
	
	protected void run(String[] args, String[] expectedLines) {
		// TODO
	}
	
	
	
}
