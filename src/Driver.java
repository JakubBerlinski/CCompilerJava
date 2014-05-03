
package Compiler;

import java.io.*;

/**
*	Main Driver for the Compiler.  This is a static class that acts as a holder for global data.<br/>
*	Command-line options for this program are listed below.<br/>
*	@see #commandLineHelp(String)
*/
public class Driver
{
	/**	Input file name. 											*/
	public static String inFile = null;	
	/**	Output file name. 											*/
	public static String outFile = null;
	/**	Level of debugging to be performed. 						*/
	public static int debugLevel = 0;
	/**	Verbose output flag. 										*/
	public static boolean verbose = false;
	/**	Compile output flag. 										*/
	public static boolean compileFlag = false;
	/**	Intermediate code generation only flag. 					*/
	public static boolean intermediateFlag = true;
	/**	Output assembly file flag. 									*/
	public static boolean assemblyFlag = false;
	/**	Create AST output image. 									*/
	public static boolean createImage = false;
	/** Reduce Symbol Table Output									*/
	public static boolean reduceAST = true;
	/** Run both Intermediate Generation and Assembly Generation 	*/
	public static boolean fullOutput = false;
	
	/**	Symbol Table. 												*/
	public static SymbolTable st = new SymbolTable();
	/**	Parser. 													*/
	public static Parser parser;
	/**	Lexical Analyzer. 											*/
	public static Scanner scanner;
	/**	Abstract Syntax Tree. 										*/
	public static Ast ast = new Ast();
	

	/**
	* Main Function for program start.
	* @param args Array of command line arguments.
	* @throws IOException This can be thrown from File IO features.
	*/
	public static void main(String[] args) throws IOException
	{
		// parse all command line arguments and set appropriate flags
		parseArgs(args);

		// report error in no input supplied
		if(inFile == null)
		{
			System.err.println("Error: No input file supplied");
			System.exit(1);
		}
		
		if(verbose)
		{
			System.out.println("Input File:        " + inFile);
			System.out.println("Output File:       " + outFile);
			System.out.println("Debug Level:       " + String.valueOf(debugLevel));
			System.out.println("Compiler Flag:     " + String.valueOf(compileFlag));
			System.out.println("Intermediate Flag: " + String.valueOf(intermediateFlag));
			System.out.println("Assembly Flag:     " + String.valueOf(assemblyFlag));
			System.out.println("Image Flag:        " + String.valueOf(createImage) + "\n");
		}
		
		// create scanner and parser from file input
		FileInputStream fin = new FileInputStream(inFile);
		scanner = new Scanner(fin);
		parser = new Parser(scanner);
		
		// set debug level and appropriate output if > 0
		parser.setDebugLevel(debugLevel);
		if(debugLevel > 0)
			parser.setDebugStream(new PrintStream("list_file.txt"));
		
		// parse file
		parser.parse();
		
		fin.close();
		
		if(verbose)
			System.out.println("AST contains " + String.valueOf(ast.size()) + " nodes.");
		
		if(createImage)
			ast.writeDotFile("ast.dot");
			
		if(intermediateFlag)
			ast.write3AC("3ac.s");
			
		if(fullOutput)
		{
			try
			{
				Runtime.getRuntime().exec("java -jar bin/Assembly.jar 3ac.s -o " + outFile);
			}
			catch(IOException e)
			{
				System.err.println("Error: Unable to run full assembly generation.");
				System.exit(1);
			}
		}
		
	}
	
	/**
	* Parses the command line arguments and sets the appropriate flags / variables or calls the appropriate function.
	* @param args The command line arguments to be parsed.
	*/
	public static void parseArgs(String[] args)
	{
		String options;
		String currentToken;
		StringBuilder sb = new StringBuilder();
		
		for(String token : args)
			sb.append(token + " ");
			
		options = sb.toString();
			
		java.util.Scanner cmdParser = new java.util.Scanner(options);
		cmdParser.useDelimiter(" ");
		
		
		while(cmdParser.hasNext())
		{
			currentToken = cmdParser.next();
			
			if(currentToken.equals("-o"))
			{
				if(cmdParser.hasNext("-.*"))
					commandLineHelp("Invalid use of argument -o");
				
				else if(cmdParser.hasNext())
					outFile = cmdParser.next();
					
				else
					System.out.println("No file specified for -o option.  Using default.");
			}
			
			else if(currentToken.equals("-d"))
			{	
				if(cmdParser.hasNextInt())
					debugLevel = cmdParser.nextInt();
					
				else if(cmdParser.hasNext())
					commandLineHelp("Invalid use of argument -d");
					
				else
					System.out.println("No value specified for -d option.  Using default.");
			}
			
			else if(currentToken.contains(".c"))
			{
				inFile = currentToken;
				
				if(outFile == null)
				{
					outFile = inFile.substring(0,inFile.lastIndexOf(".")) + ".s";
					
					if(outFile.contains("/"))
						outFile = outFile.substring(outFile.lastIndexOf("/")+1, outFile.length());
				}
			}
				
			else if(currentToken.equals("--verbose"))
				verbose = true;
				
			else if(currentToken.equals("-c"))
				compileFlag = true;
				
			else if(currentToken.equals("-q"))
				intermediateFlag = false;
				
			else if(currentToken.equals("-S"))
				assemblyFlag = true;
				
			else if(currentToken.equals("--ast-image"))
				createImage = true;
				
			else if(currentToken.equals("-h") || currentToken.equals("--help"))
				commandLineHelp(null);
				
			else if(currentToken.equals("--no-reduce-ast"))
				reduceAST = false;
				
			else if(currentToken.equals("--full"))
			{
				intermediateFlag = true;
				fullOutput = true;
				assemblyFlag = true;
			}
				
			else
				commandLineHelp("Unknown option: " + currentToken);
			
		}
	}
	
	/**
	* Print information on command line usage.<br/><br/>
	* <table align="left">
	*	<tr>
	*	<th>Option</th>
	*	<th>Effect</th>
	*	</tr>
	*	<tr><td>-h, --help</td>       <td>Display this help message.</td></tr>
	*	<tr><td>-o &lt;outputfile&gt;</td>      <td>Sets output file name.</td></tr>
	*	<tr><td>-d &lt;debugLevel&gt;</td>      <td>Sets debug level.</td></tr>
	*	<tr><td>-c</td>                   <td>Compile only flag.</td></tr>
	*	<tr><td>-q</td>                   <td>Output intermediade code.</td></tr>
	*	<tr><td>-S</td>                   <td>Output assembly file.</td></tr>
	*	<tr><td>--verbose</td>            <td>Generate verbose output.</td></tr>
	*	<tr><td>--ast-image</td>          <td>Generate image representation of the AST.</td></tr>
	*	<tr><td>--no-reduce-ast</td>      <td>Do not reduce AST Output. (WARNING: Currently Breaks Program)</td></tr>
	*	<tr><td>--full</td>               <td>Generate Intermediate Code and Run Assembly Generator</td></tr>
	* </table>
	* @param error Error statement to be print.  If null no error is printed.
	*/
	public static void commandLineHelp(String error)
	{
		if(error != null)
			System.err.println("\nError: " + error + "\n");
		
		System.err.println("Compiler Options\n" +
		"\t-h, --help           Display this help message.\n" +
		"\t-o <outputfile>      Sets output file name.\n" +
		"\t-d <debugLevel>      Sets debug level.\n" +
		"\t-c                   Compile only flag.\n" +
		"\t-q                   Output intermediade code.\n" +
		"\t-S                   Output assembly file.\n" +
		"\t--verbose            Generate verbose output.\n" +
		"\t--ast-image          Generate image representation of the AST.\n" +
		"\t--no-reduce-ast      Do not reduce AST Output. (WARNING: Currently Breaks Program)\n" + 
		"\t--full               Generate Intermediate Code and Run Assembly Generator\n");
		
		System.exit(0);
	}
}
