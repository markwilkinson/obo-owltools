package owltools.ontologyrelease;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JOptionPane;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

import owltools.ontologyrelease.gui.OboOntologyReleaseRunnerParameters;
import owltools.ontologyrelease.gui.ReleaseGuiMainFrame;

/**
 * GUI access for ontology release runner.
 */
public class OboOntologyReleaseRunnerGui {

	private final static Logger logger = Logger.getLogger(OboOntologyReleaseRunnerGui.class);
	
	public static void main(String[] args) {
		
		// SimpleDateFormat is NOT thread safe
		// encapsulate as thread local
		final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>(){

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			}
			
		};
		
		Logger rootLogger = Logger.getRootLogger();
		final BlockingQueue<String> logQueue =  new ArrayBlockingQueue<String>(100);
		
		rootLogger.addAppender(new AppenderSkeleton() {
			
			public boolean requiresLayout() {
				return false;
			}
			
			public void close() {
				// do nothing
			}
			
			@Override
			protected void append(LoggingEvent event) {
				String message = event.getRenderedMessage();
				try {
					logQueue.put(df.get().format(new Date(event.timeStamp))+"  "+message);
				} catch (InterruptedException e) {
					logger.fatal("Interruped during wait for writing to the message panel.", e);
				}
			}
		});
		
		// Start GUI
		new ReleaseGuiMainFrameRunner(logQueue);
	}

	private static final class ReleaseGuiMainFrameRunner extends ReleaseGuiMainFrame {
		
		// generated
		private static final long serialVersionUID = -8690322825608397262L;
		
		private ReleaseGuiMainFrameRunner(BlockingQueue<String> logQueue) {
			super(logQueue);
		}
	
		@Override
		protected void executeRelease(final OboOntologyReleaseRunnerParameters parameters) {
			logger.info("Starting release manager process");
			try {
				Thread t = new Thread() {
					@Override
					public void run() {
						try {
							ReleaseGuiMainFrameRunner.this.disableReleaseButton();
							OWLOntologyFormat format = parameters.getFormat();
							String reasoner = parameters.getReasoner();
							boolean asserted = parameters.isAsserted();
							boolean simple = parameters.isSimple();
							Vector<String> paths = parameters.getPaths();
							File base = parameters.getBase();
							OboOntologyReleaseRunner.createRelease(format, reasoner, asserted, simple, paths, base);
							logger.info("Finished release manager process");
							JOptionPane.showMessageDialog(ReleaseGuiMainFrameRunner.this, "Finished making the release.");
						} catch (Exception e) {
							logger.error("Internal error: "+ e.getMessage(), e);
						} catch (Throwable e) {
							logger.fatal("Internal error: "+ e.getMessage(), e);
						}
						finally {
							ReleaseGuiMainFrameRunner.this.enableReleaseButton();
						}
					}
				};
				t.start();
			} catch (Exception e) {
				logger.error("Internal error: "+ e.getMessage(), e);
			} catch (Throwable e) {
				logger.fatal("Internal error: "+ e.getMessage(), e);
			}
			
		}
	}
}
