package ashc.main;

import java.io.*;
import java.util.*;

import ashc.codegen.*;
import ashc.grammar.*;
import ashc.grammar.Node.NodeFile;

/**
 * Ash
 *
 * @author samtebbs, 15:14:41 - 23 May 2015
 */
public class AshCompiler {

    private final Parser parser;
    private final Lexer lexer;
    private NodeFile fileNode;
    public String relFilePath, parentPath;
    public int errors;

    public static Stack<AshCompiler> compilers = new Stack<AshCompiler>();

    public AshCompiler(final String relFilePath) throws FileNotFoundException, IOException {
	final File file = new File(relFilePath);
	parentPath = file.getParentFile() != null ? file.getParentFile().getPath() : "";
	this.relFilePath = relFilePath;
	lexer = new Lexer(new BufferedReader(new FileReader(file)));
	parser = new Parser(lexer);
	compilers.push(this);
    }

    public void parse() {
	fileNode = parser.start();
    }

    public void preAnalyse() {
	if (fileNode != null) fileNode.preAnalyse();
    }

    public void analyse() {
	if (fileNode != null) fileNode.analyse();
    }

    public void generate() {
	if (fileNode != null) {
	    fileNode.generate();
	    GenNode.generate();
	}
    }

    public static AshCompiler get() {
	return compilers.peek();
    }

}
