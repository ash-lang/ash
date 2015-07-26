package ashc.main;

import java.io.*;
import java.nio.file.*;

import ashc.error.*;
import ashc.library.*;
import ashc.load.*;

/**
 * Grammar
 *
 * @author samtebbs, 19:52:26 - 20 May 2015
 */
public class AshMain {

    public static String outputDir, inputFile = null;
    public static boolean warningsEnabled = true, verboseMsgEnabled = false;;

    public static void main(final String[] args) throws FileNotFoundException, IOException, AshError {
	Library.findLibs();
	// System.out.println(System.getProperty("sun.boot.class.path"));
	TypeImporter.loadClass("java.lang.String", "String");
	TypeImporter.loadClass("java.lang.Object", "Object");
	TypeImporter.loadClass("java.lang.System", "System");
	TypeImporter.loadClass("java.lang.Iterable", "Iterable");
	parseArgs(args);
	if (outputDir == null) outputDir = "./";
	else if (!outputDir.endsWith(File.separator)) outputDir += File.separatorChar;
	final AshCompiler compiler = new AshCompiler(inputFile);
	compiler.parseSourceFile();
	compiler.preAnalyse();
	compiler.analyse();
	if (AshError.numErrors == 0) compiler.generate();
	System.exit(AshError.numErrors == 0 ? 0 : 1);
    }

    static void parseArgs(final String[] args) {
	for (int i = 0; i < args.length; i++) {
	    final String arg = args[i];
	    switch (arg) {
		case "-o":
		    if (i < (args.length - 1)) outputDir = Paths.get(args[++i]).toString();
		    else AshError.compilerError("Expected output directory path for option \"-o\"");
		    break;
		case "-w":
		    warningsEnabled = false;
		    break;
		case "-v":
		    verboseMsgEnabled = true;
		    break;
		default:
		    if (inputFile != null) AshError.compilerWarning("Stray argument \"" + arg + "\"");
		    inputFile = arg;
	    }
	}
    }

}
