package org.eclipse.test.performance.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.test.internal.performance.db.Scenario;
import org.eclipse.test.internal.performance.db.Variations;

public class Main {

	private String baseline;
	private String dimensionHistoryOutput;
	private String dimensionHistoryUrl;
	private String fpOutput;
	private String[] config;
	private Hashtable configDescriptors;
	private String currentBuildId;
	private ArrayList pointsOfInterest;
	private Variations variations;
	private Scenario[] scenarios;
	private String jvm;
	private String scenarioFilter;
	private boolean genFingerPrints = false;
	private boolean genScenarioSummaries =false;
	private boolean genAll = true;
	private Hashtable fingerPrints = new Hashtable();

	public static void main(String args[]) {
		Main main = new Main();
		main.parse(args);
		main.run();
	}

	private void run() {
		String tmpDimensionOutput=dimensionHistoryOutput;

		for (int i = 0; i < config.length; i++) {
			Utils.ConfigDescriptor cd=null;
			if (configDescriptors!=null){
				cd = (Utils.ConfigDescriptor) configDescriptors
					.get(config[i]);
				dimensionHistoryUrl = cd.url;
				if (cd.outputDir != null)
					dimensionHistoryOutput = cd.outputDir;
			}
			dimensionHistoryOutput=tmpDimensionOutput+"/"+config[i];
			run(config[i],cd);
		}

		Enumeration components = fingerPrints.keys();

		Utils.copyFile(Utils.class.getResource("FAIL.gif"),fpOutput+"/FAIL.gif");
		Utils.copyFile(Utils.class.getResource("OK.gif"),fpOutput+"/OK.gif");

		// print fingerprint/scenario status pages
		while (components.hasMoreElements()) {
			String component = components.nextElement().toString();
			try {		
				File output = new File(fpOutput + '/' + component + ".php");
				output.getParentFile().mkdirs();
				PrintStream os = new PrintStream(new FileOutputStream(fpOutput
						+ '/' + component + ".php"));
				os.println(Utils.HTML_OPEN);
				os.println(Utils.HTML_DEFAULT_CSS);
				os.println("<body>");
				Hashtable fps = (Hashtable) fingerPrints.get(component);
				Enumeration configs = fps.keys();
				
				int underScoreIndex=baseline.indexOf("_");
				String baselineName=(underScoreIndex!=-1)?baseline.substring(0, baseline.indexOf("_")):baseline;
				String title = "<h3>Performance of " + component + ": "
						+ currentBuildId + " relative to "
						+ baselineName
						+ "</h3>";
				if (component.equals("global"))
					title = "<h3>Performance of " + currentBuildId
							+ " relative to "
							+ baselineName
							+ "</h3>";
				os.println(title);
				while (configs.hasMoreElements()) {
					String config = configs.nextElement().toString();
					FingerPrint fp = (FingerPrint) fps.get(config);
					os.println(Utils.getImageMap(fp));
				}
				if (component != "") {
					char buildType = currentBuildId.charAt(0);

					// print the component scenario table beneath the
					// fingerprint
					variations.put("config", "%");
					ScenarioStatusTable sst = new ScenarioStatusTable(
							variations, component + "%", configDescriptors);
					os.println(sst.toString());
				}

				os.println(Utils.HTML_CLOSE);
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void run(String config, Utils.ConfigDescriptor cd) {
				
			scenarios = Utils.getScenarios("%", scenarioFilter, config, jvm);
			variations = Utils.getVariations("%", config, jvm);

			if (genScenarioSummaries || genAll) {
				System.out.print(config
						+ ": generating scenario summaries...");
				new ScenarioResults(scenarios, baseline,dimensionHistoryOutput,config,currentBuildId,cd, pointsOfInterest);
				System.out.println("done.");
			}

			if (genFingerPrints || genAll) {
				System.out.print(config + ": generating fingerprints...");
				FingerPrint global = new FingerPrint(null, config, baseline,
						currentBuildId, variations, fpOutput, configDescriptors);
				Hashtable t;
				if (fingerPrints.get("global") != null)
					t = (Hashtable) fingerPrints.get("global");
				else
					t = new Hashtable();

				t.put(config, global);
				fingerPrints.put("global", t);

				ArrayList components = Utils.getComponentNames(scenarios);
				for (int i = 0; i < components.size(); i++) {
					String component = components.get(i).toString();
					variations.put("config", config);
					FingerPrint componentFp = new FingerPrint(component,
							config, baseline, currentBuildId, variations,
							fpOutput, configDescriptors);
					if (fingerPrints.get(component) != null)
						t = (Hashtable) fingerPrints.get(component);
					else
						t = new Hashtable();
					t.put(config, componentFp);
					fingerPrints.put(component, t);
				}
				System.out.println("done.");
			}
		}
	

	private void parse(String args[]) {
		int i = 0;
		if (args.length == 0) {
			printUsage();
		}

		while (i < args.length) {
			String arg = args[i];
			
			if (arg.equals("-baseline")) {
				baseline = args[i + 1];
				if (baseline.startsWith("-")) {
					System.out.println("Missing value for -baseline parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-highlight.latest")) {
				if (args[i + 1].startsWith("-")) {
					System.out.println("Missing value for -highlight.latest parameter");
					printUsage();
				}
				String []ids=args[i + 1].split(",");
				pointsOfInterest=new ArrayList();
				for (int j=0;j<ids.length;j++){
					pointsOfInterest.add(ids[j]);
				}
				i++;
			}
			if (arg.equals("-current")) {
				currentBuildId = args[i + 1];
				if (currentBuildId.startsWith("-")) {
					System.out.println("Missing value for -current parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-jvm")) {
				jvm = args[i + 1];
				if (jvm.startsWith("-")) {
					System.out.println("Missing value for -jvm parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-fp.output")) {
				fpOutput = args[i + 1];
				if (fpOutput.startsWith("-")) {
					System.out.println("Missing value for -fp.output parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-dim.history.output")|| arg.equals("-dim.output")) {
				dimensionHistoryOutput = args[i + 1];
				if (dimensionHistoryOutput.startsWith("-")) {
					System.out.println("Missing value for -dim.output parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-config")) {
				String configs = args[i + 1];
				if (configs.startsWith("-")) {
					System.out.println("Missing value for -config parameter");
					printUsage();
				}
				config = configs.split(",");
				i++;
			}
			if (arg.equals("-config.properties")) {
				String configProperties = args[i + 1];
				if (configProperties.startsWith("-")) {
					System.out.println("Missing value for -config.properties parameter");
					printUsage();
				}
				configDescriptors = Utils
						.getConfigDescriptors(configProperties);
				i++;
			}
			if (arg.equals("-scenario.filter")||arg.equals("-scenario.pattern")) {
				scenarioFilter = args[i + 1];
				if (scenarioFilter.startsWith("-")) {
					System.out.println("Missing value for -baseline parameter");
					printUsage();
				}
				i++;
			}
			if (arg.equals("-fingerprints")) {
				genFingerPrints = true;
				genAll = false;
			}
			if (arg.equals("-scenarioresults")) {
				genScenarioSummaries = true;
				genAll = false;
			}
			i++;
		}
		
		if (baseline == null || fpOutput == null || config == null
				|| jvm == null
				|| currentBuildId == null)
			printUsage();
	}
	private void printUsage() {
		System.out
				.println("Usage:\n"
						
						+ "-baseline"
								+"\n\tBuild id against which to compare results."
								+"\n\tSame as value specified for the \"build\" key in the eclipse.perf.config system property.\n\n"
								
						+ "-current" 
								+"\n\tbuild id for which to generate results.  Compared to build id specified in -baseline parameter above."
								+"\n\tSame as value specified for the \"build\" key in the eclipse.perf.config system property. \n\n"
							
						+ "-jvm"
								+"\n\tValue specified in \"jvm\" key in eclipse.perf.config system property for current build.\n\n"
						
						+ "-config" 
								+"\n\tComma separated list of config names."
								+"\n\tSame as values specified in \"config\" key in eclipse.perf.config system property.\n\n"

						+ "-fp.output"
								+" \n\tPath to output directory for fingerprints.\n\n"		

						+ "-dim.output"
								+"\n\tPath to output directory for raw data of dimensions presented in  html tables and line graphs."
								+"\n\tOptional if information provided in config.properties (below)."
								+"\n\tLine graphs produced in subdirectory called \"graphs\".\n\n"

						+ "[-config.properties]"
								+"\n\tOptional.  Used by scenario status table to provide the following:"
								+"\n\t\talternate descriptions of config values to use in columns."
								+"\n\t\turl for hyperlinks from status icons for a scenario in each row."
								+"\n\tCan also provide an optional output directory for raw data tables and line graphs for the config."
								+"\n\tThe value should be specified in the following format:"
								+"\n\tname1,description1,url1 [,outputdir1];name2,description2,url2 [,outputdir2];etc..\n\n"

						+ "[-highlight.latest]"
								+"\n\tOptional.  Comma-separated list of build Id prefixes used to find most recent matching for each entry." +
										"\n\tResult used to highlight points in line graphs.\n\n"

						+ "[-scenario.pattern]"
								+"\n\tOptional.  Scenario prefix pattern to query database.  If not specified,"
								+"\n\tdefault of % used in query.\n\n"
							
						+ "[-fingerprints]" 
								+"\n\tOptional.  Use to generate fingerprints only.\n\n"
									
						+ "[-scenarioresults]"
								+"\n\tGenerates table of scenario reference and current data with line graphs.\n\n");
										
		System.exit(1);

	}

}