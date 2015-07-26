package ashc.load;

import java.io.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import ashc.error.*;
import ashc.library.*;
import ashc.main.*;
import ashc.semantics.*;

/**
 * Ash
 *
 * @author samtebbs, 20:15:46 - 23 May 2015
 */
public class TypeImporter {

    public static ClassLoader loader = ClassLoader.getSystemClassLoader();

    public static ashc.semantics.Member.Type loadClass(final String path, final String alias) {
	// Attempt to find an Ash source file relative to current working directory
	String filePath = path.replace('.', File.separatorChar);
	File srcFile = new File(filePath+".ash");
	if(srcFile.exists() && srcFile.isFile()){
	    if(importAshSource(srcFile)) return Semantics.getType(alias).get();
	}
	// Attempt to find a java source file
	srcFile = new File(filePath+".java");
	if(srcFile.exists() && srcFile.isFile()){
	    compileJavaSource(srcFile);
	    try {
		// Import the resulting .class file
		return importStream(new FileInputStream(new File(filePath+".class")));
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
	try {
	    final InputStream stream = Library.getClassStream(path);
	    return importStream(stream);
	} catch (final IOException e) {
	    AshError.compilerError("Cannot find class: " + path);
	}
	return null;
    }

    private static ashc.semantics.Member.Type importStream(InputStream stream) throws IOException {
	if (stream != null) {
		final ashc.semantics.Member.Type type = readClass(stream);
		Semantics.addType(type, false);
		return type;
	    }
	return null;
    }

    private static void compileJavaSource(File srcFile) {
	ProcessBuilder pb = new ProcessBuilder("javac", srcFile.toString());
	try {
	    Process process = pb.start();
	    process.waitFor();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line = null;
	    while((line = reader.readLine()) != null) System.out.println(line);
	    reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
	    line = null;
	    while((line = reader.readLine()) != null) System.err.println(line);
	} catch (IOException | InterruptedException e) {
	    e.printStackTrace();
	}
    }

    private static boolean importAshSource(File srcFile) {
	try {
		AshCompiler compiler = new AshCompiler(srcFile.toString());
		compiler.parse();
		compiler.preAnalyse();
		compiler.analyse();
		if(compiler.errors == 0){
		    compiler.generate();
		    return true;
		}
	    } catch (IOException e) {
		AshError.compilerError("Cannot find source file: " + srcFile.toString());
	    }
	return false;
    }

    private static ashc.semantics.Member.Type readClass(final InputStream stream) throws IOException {
	final ClassReader reader = new ClassReader(stream);
	final ClassNode node = new ClassNode();
	reader.accept(node, 0);
	stream.close();
	return new ashc.semantics.Member.Type(node);
    }

}
