
package Assembly;

import java.io.*;
//import java.util.Scanner;

/**
* Static class for initializing program and parsing command line arguments.
*/
public class AssemblyDriver 
{
	public static String inFile = null;
	public static String outFile = "output.s";
	public static boolean verbose = false;
	public static int debugLevel = 1;
	public static String[] lines;
	
	public static AssemblyScanner scanner;
	public static AssemblyParser parser;
	public static RegisterAllocator ra;
	
	public static PrintWriter fout;
	
	public static void main(String[] args) throws IOException
	{
		parseArgs(args);
		
		if(inFile == null)
		{
			System.err.println("Error: No input file supplied");
			System.exit(1);
		}
		
		if(verbose)
		{
			System.out.println("Input File:  " + inFile);
			System.out.println("Output File: " + outFile);
		}
		
		FileInputStream fin = new FileInputStream(inFile);
		
		readInput(fin);
		
		fin.close();
		
		fout = new PrintWriter(outFile);
		
		ra = new RegisterAllocator();
		fin = new FileInputStream(inFile);
		scanner = new AssemblyScanner(fin);
		parser = new AssemblyParser(scanner);
		
		// output program start
		outputAssemblyComment("program start");
		fout.println("\t.data");
		fout.println("spill: .space 1024");
		fout.println("\t.text");
		fout.println("\t la $s0, spill");
		fout.println("\t j main");
		
		parser.parse();
		
		fin.close();
		fout.close();

	}
	
	public static void outputAssembly(String command, String comment)
	{
		if(comment != null && !comment.equals("") && comment.charAt(0) != '#')
			comment = "# " + comment;
		
		fout.format("\t%-12s %-12s  %-12s  %-25s %-30s%n", command, "","","", comment);
	}
	
	public static void outputAssembly1(String command, String reg1, String comment)
	{
		if(comment != null && !comment.equals("") && comment.charAt(0) != '#')
			comment = "# " + comment;
		
		//fout.printf("\t%s %s %s %s %s\n", command, reg1, "", "", comment);
		//fout.printf("\t%12s %12s %12 %20s %30s%n", command, reg1, "","", comment);
		fout.format("\t%-12s %-12s  %-12s  %-25s %-30s%n", command, reg1, "", "", comment);
	}
	
	public static void outputAssembly2(String command, String reg1, String reg2, String comment)
	{
		if(comment != null && !comment.equals("") && comment.charAt(0) != '#')
			comment = "# " + comment;
		
		fout.format("\t%-12s %-12s, %-12s  %-25s %-30s%n", command, reg1, reg2, "", comment);
	}
	
	public static void outputAssembly3(String command, String reg1, String reg2, String reg3, String comment)
	{
		if(comment != null && !comment.equals("") && comment.charAt(0) != '#')
			comment = "# " + comment;
		
		fout.format("\t%-12s %-12s, %-12s, %-25s %-30s%n", command, reg1, reg2, reg3, comment);
	}
	
	public static void outputAssemblyLabel(String label)
	{
		fout.println(label);
	}
	
	public static void outputAssemblyComment(String comment)
	{
		fout.println("# " + comment);
	}
	
	public static void readInput(FileInputStream fin)
	{
		java.util.Scanner reader = new java.util.Scanner(fin);
		StringBuilder sb = new StringBuilder();
		
		while(reader.hasNextLine())
			sb.append(reader.nextLine()).append("\n");
			
		lines = sb.toString().split("\n");
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
			
			else if(currentToken.contains(".s"))
			{
				inFile = currentToken;
		/*
				if(outFile == null)
				{
					outFile = inFile.substring(0,inFile.lastIndexOf(".")) + ".s";
					
					if(outFile.contains("/"))
						outFile = outFile.substring(outFile.lastIndexOf("/")+1, outFile.length());
				}
		*/		
			}
				
			else if(currentToken.equals("-h") || currentToken.equals("--help"))
				commandLineHelp(null);
				
			else if(currentToken.equals("--verbose"))
				verbose = true;
				
			else if(currentToken.equals("-d"))
			{
				if(cmdParser.hasNext("-.*"))
					commandLineHelp("Invalid use of argument -d");
				
				else if(cmdParser.hasNextInt())
					debugLevel = cmdParser.nextInt();
					
				else
					System.out.println("No value specified for -d option.  Using default.");
			}
				
			else
				commandLineHelp("Unknown option: " + currentToken);
		}
	}
	
	/**
	* Print information on command line usage. 
	* @param error Error statement to be print. If null no error is printed.
	*/
	public static void commandLineHelp(String error)
	{
		if(error != null)
			System.err.println("\nError: " + error + "\n");
		
		System.err.println("Compiler Options\n" +
		"\t-h, --help           Display this help message.\n" +
		"\t-o <outputfile>      Sets output file name.\n" +
		"\t--verbose            Generate verbose output.\n");
		
		System.exit(0);
	}
}
