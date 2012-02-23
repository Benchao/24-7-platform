/**
 * Command line run runtime
 * received several parameter with their options
 */
package com.deri.latc.utility;


import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionException;
import com.deri.latc.linkengine.LinkEngine;

/**
* Command line options for running runtime
* @author Nur Aini Rakhmawati 
* @since February 2011
*/

public class CommandLine {

	private final String       HELP             = "help";
	private final String       CONFIG_FILE      = "c";
	private final String       HADOOP_PATH      = "hadoop-path";
	private final String       HADOOP_USER      = "hadoop-user";
	private final String       LATC_CONSOLE_HOST= "latc-console-host";
	private final String       LINKS_FILE       = "links-file";
	private final String	   RESULTS_HOST	    = "results-host";
	private final String	   RESULT_LOCAL_DIR	= "result-local-dir";
	private final String	   SPEC_FILE		= "spec-file";
	private final String	   VOID_FILE		= "void-file";
	private final String	   API_KEY_CONSOLE			= "api-key-console";
	private final String	   API_KEY_MDS			= "api-key-mds";
	private final String	   MDS_HOST			= "mds-host";
	
	 private final OptionParser parser;
	 
	 public CommandLine() {
		 parser = new OptionParser();
		 parser.accepts(HELP, "print usage information");
		 parser.accepts(CONFIG_FILE, "[OPTIONAL] The path of configuration file").withRequiredArg().ofType(String.class);
		 parser.accepts(HADOOP_PATH, "[REQUIRED] The path of hadoop instalation directory").withRequiredArg().ofType(String.class);
		 parser.accepts(HADOOP_USER, "[OPTIONAL] HDFS (hadoop distributed filesystem) user for running at hbase").withRequiredArg().ofType(String.class);
		 parser.accepts(LATC_CONSOLE_HOST, "[REQUIRED] The URL of console host").withRequiredArg().ofType(String.class);
		 parser.accepts(MDS_HOST, "[REQUIRED] The URL of MDS host").withRequiredArg().ofType(String.class);
		 parser.accepts(RESULTS_HOST, "[REQUIRED] The URL of links generation").withRequiredArg().ofType(String.class);
		 parser.accepts(LINKS_FILE, "[OPTIONAL] The name of link file for storing the triples of links generation, default : links.nt").withRequiredArg().ofType(String.class);
		 parser.accepts(SPEC_FILE, "[OPTIONAL] The name of SILK specification file , default : spec.xml").withRequiredArg().ofType(String.class);
		 parser.accepts(VOID_FILE, "[OPTIONAL] The name of void file , default : void.ttl").withRequiredArg().ofType(String.class);
		 parser.accepts(RESULT_LOCAL_DIR, "[OPTIONAL] The local path result , default : results").withRequiredArg().ofType(String.class);
		 parser.accepts(API_KEY_CONSOLE, "[REQUIRED] The api key for posting report to onsole ").withRequiredArg().ofType(String.class);
		 parser.accepts(API_KEY_MDS, "[REQUIRED] The api key for posting report to onsole ").withRequiredArg().ofType(String.class);
		 
	}
	 
	 /**
	  * Parsing all the input line
	  * @param args	arguments typed by user 
	  * @throws Exception
	  */
	 public void run(final String[] args) throws Exception {
		  try {
			  final OptionSet options = parser.parse(args);
			  if (options.has(HELP)) {
			      parser.printHelpOn(System.out);
			      System.exit(0);
			    }
			  if (options.has(CONFIG_FILE))
			  {
				  LinkEngine le = new LinkEngine((String)options.valueOf(CONFIG_FILE));
				  le.execute();
				  System.exit(0);
			  }
			  if (!options.has(HADOOP_PATH) || !options.has(LATC_CONSOLE_HOST) || !options.has(RESULTS_HOST) || !options.has(API_KEY_CONSOLE) || !options.has(API_KEY_MDS) || !options.has(MDS_HOST)) {
			      System.out.println("Error parameter hadoop path, URL of console host, api key and URL of links generation are required");
			      parser.printHelpOn(System.out);
			      System.exit(0);
			    }
			 
			  
			  if (options.has(HADOOP_PATH))
				  Parameters.HADOOP_PATH = (String)options.valueOf(HADOOP_PATH);
			  if (options.has(HADOOP_USER))
				  Parameters.HADOOP_USER = (String)options.valueOf(HADOOP_USER);
			  if (options.has(LATC_CONSOLE_HOST))
				  Parameters.LATC_CONSOLE_HOST = (String)options.valueOf(LATC_CONSOLE_HOST);
			  if (options.has(MDS_HOST))
				  Parameters.MDS_HOST = (String)options.valueOf(MDS_HOST);
			  if (options.has(RESULTS_HOST))
				  Parameters.RESULTS_HOST = (String)options.valueOf(RESULTS_HOST);
			  if (options.has(LINKS_FILE))
				  Parameters.LINKS_FILE_STORE = (String)options.valueOf(LINKS_FILE);
			  if (options.has(SPEC_FILE))
				  Parameters.SPEC_FILE = (String)options.valueOf(SPEC_FILE);
			  if (options.has(VOID_FILE))
				  Parameters.VOID_FILE = (String)options.valueOf(VOID_FILE);
			  if (options.has(RESULT_LOCAL_DIR))
				  Parameters.RESULT_LOCAL_DIR = (String)options.valueOf(RESULT_LOCAL_DIR);
			  if (options.has(API_KEY_CONSOLE))
				  Parameters.API_KEY_CONSOLE = (String)options.valueOf(API_KEY_CONSOLE);
			  if (options.has(API_KEY_MDS))
				  Parameters.API_KEY_MDS = (String)options.valueOf(API_KEY_MDS);
			  
			  LinkEngine le = new LinkEngine();
			  le.execute();
			  
		  }
			  catch(OptionException e) {
			    	System.err.println(e.getMessage());
			    	parser.printHelpOn(System.out);
			        System.exit(0);
			    }
		  }
	/**
	 * @param args
	 * Option                                  Description
------                                  -----------
-c                                      [OPTIONAL] The path of configuration
                                          file
--hadoop-path                           [REQUIRED] The path of hadoop
                                          instalation directory
--hadoop-user                           [OPTIONAL] [OPTIONAL] HDFS (hadoop distributed filesystem) user for running at hbase
--help                                  print usage information
--latc-console-host                     [REQUIRED] The URL of console host
--links-file                            [OPTIONAL] The name of link file for
                                          storing the triples of links
                                          generation, default : links.nt
--result-local-dir                      [OPTIONAL] The local path result ,
                                          default : results
--results-host                          [REQUIRED] The URL of links generation
--spec-file                             [OPTIONAL] The name of SILK
                                          specification file , default : spec.
                                          xml
--void-file                             [OPTIONAL] The name of void file ,
                                          default : void.ttl

	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	final CommandLine cmd = new CommandLine();
	cmd.run(args);
	}

}
