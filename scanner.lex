package Compiler;

import java.io.*;
import java.util.*;

%%

%{
	/**	String containing the current line.							*/
	public String lineBuffer = "";
	/**	Object for holding semantic values.							*/
	private Object yylval;
	/**	Flag to tell us to insert or lookup from Symbol Table.		*/
	public static boolean insertMode = false;
	public static boolean functionType = false;
	public static boolean arrayType = false;
	public static boolean structType = false;
	public static boolean pointerType = false;
	/**	Holder of the current node for the parser.					*/
	public static SymbolNode currentNode = null;
	/**	String containing the current identifier type.				*/
	public static String currentType;
	/**	String containing the last identifier string.				*/
	public static String lastID;
	/** Stack for evaluating type of identifier						*/
	public static Stack<String> typeStack = new Stack<String>();
	
	public int getLineNo() {
		return yyline+1;
	}

	public void decrementColumn(int decValue) {
		yycolumn -= decValue;
	}
	
	/**
	* Getter function to return yylval.
	* @return A pointer to yylval.
	*/
	public Object getLVal() {
		return yylval;
	}
	
	/**
	* Prints an error message to stderr including line number and column number.
	* Also prints the current line with a reference to where the error occured.
	* @param errorMessage The message to be displayed describing the error.
	*/
	public void yyerror(String errorMessage) {
		FileInputStream fin = null;
		
		// get line from file if first line
		if(yyline == 0) {
			lineBuffer = new String();
			try {
				int data;
				fin = new FileInputStream(Driver.inFile);				
				data = fin.read();
				while(data != '\n') {
					data = fin.read();
					lineBuffer = lineBuffer.concat(String.valueOf((char)data));
				}
				fin.close();
			} catch(IOException e) {
				System.err.println("Error: Critical IO Failure in yyerror()");
				System.exit(1);
			}
		}
		int count = trimLineBuffer();
		System.err.println(Driver.inFile + ": error on line " + String.valueOf(yyline+1) + " column " + String.valueOf(yycolumn+1));
		System.err.println(lineBuffer);
		errorMessage = errorMessage.substring(errorMessage.indexOf(",")+1);
		for(int i = 0; i <= yycolumn - count - 1; i++)
			System.err.print(' ');
		System.err.println("^ " + errorMessage);
		System.err.println("\nfatal error. compilation terminated");
		System.exit(1);
	}
	
	/**
	* Prints a warning message to stderr including line number and column number.
	* Also prints the current line with a reference to where the warning occured.
	* @param warningMessage The message to be displayed describing the error.
	*/
	public void yywarning(String warningMessage) {
		FileInputStream fin = null;
		
		// get line from file if first line
		if(yyline == 0)
		{
			lineBuffer = new String();
			try {
				int data;
				fin = new FileInputStream(Driver.inFile);
				data = fin.read();
				while(data != '\n') {
					data = fin.read();
					lineBuffer = lineBuffer.concat(String.valueOf((char)data));
				}
				fin.close();
			} catch(IOException e) {
				System.err.println("Error: Critical IO Failure in yywarning()");
				System.exit(1);
			}
		}
		int count = trimLineBuffer();
		System.err.println(Driver.inFile + ": warning on line " + String.valueOf(yyline+1) + " column " + String.valueOf(yycolumn+1));
		System.err.println(lineBuffer);
		for(int i = 0; i <= yycolumn - count - 1; i++)
			System.err.print(' ');
		System.err.println("^ " + warningMessage + "\n");
	}
	
	/**
	* Removes all leading white space in the line buffer.
	* @return The count (number of spaces) removed.
	*/
	public int trimLineBuffer() {
		int count = 0;
		char[] arr = lineBuffer.toCharArray();
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] == ' ') {
				count++;
			} else if(arr[i] == '\t') {
				count += 4;
			} else
				break;
		}
		lineBuffer = lineBuffer.trim();
		return count;
	}
%}

%class Scanner
%public
%line
%column
%byaccj
%table
%implements Parser.Lexer

delim =						[ ]
ws =						{delim}+
digit =						[0-9]
letter =					[a-zA-Z_]
hex	=						[a-fA-F0-9]
E =							[Ee][+-]?{digit}+
FS =						(f|F|l|L)
IS =						(u|U|l|L)*
linecomment = 				"//"(.*|\n)
blockcomment = 				"/*" [^*] ~"*/" | "/*" "*"+ "/"
identifier = 				{letter}({letter}|{digit})*
int_lit_one = 				0[xX]{hex}+{IS}?
int_lit_two = 				0{digit}+{IS}?
int_lit_three = 			{digit}+{IS}?
char_lit = 					L?'(\\.|[^\\'])+'
float_lit_one = 			{digit}+{E}{FS}?
float_lit_two = 			{digit}*"."{digit}+({E})?{FS}?
float_lit_three = 			{digit}+"."{digit}*({E})?{FS}?
str_literal = 				L?"\""([^\n\"\\]*(\\[.\n])*)*"\""

%%
{ws} 						{/* no action taken */}
{linecomment} 				{lineBuffer = yytext(); }
{blockcomment} 				{lineBuffer = yytext(); }

"\n".*						{lineBuffer = yytext().substring(1); yypushback(lineBuffer.length());}
"\t"						{yycolumn += 3;}	
"auto"						{return(Parser.AUTO);}
"break"						{return(Parser.BREAK);}
"case"						{return(Parser.CASE);}
"char"						{currentType = yytext(); return(Parser.CHAR);}
"const"						{return(Parser.CONST);}
"continue"					{return(Parser.CONTINUE);}
"default"					{return(Parser.DEFAULT);}
"do"						{return(Parser.DO);	}
"double"					{currentType = yytext(); return(Parser.DOUBLE);}
"else"						{return(Parser.ELSE);}
"enum"						{return(Parser.ENUM);}
"extern"					{return(Parser.EXTERN);}
"float"						{currentType = yytext(); return(Parser.FLOAT);}
"for"						{return(Parser.FOR);}
"goto"						{return(Parser.GOTO);}
"if"						{return(Parser.IF);}
"int"						{currentType = yytext(); return(Parser.INT);}
"long"						{currentType = yytext(); return(Parser.LONG);}
"register"					{return(Parser.REGISTER);}
"return"					{return(Parser.RETURN);}
"short"						{currentType = yytext(); return(Parser.SHORT);}
"signed"					{return(Parser.SIGNED);}
"sizeof"					{return(Parser.SIZEOF);}
"static"					{return(Parser.STATIC);}
"struct"					{return(Parser.STRUCT);}
"switch"					{return(Parser.SWITCH);}
"typedef"					{return(Parser.TYPEDEF);}
"union"						{return(Parser.UNION);}
"unsigned"					{return(Parser.UNSIGNED);}
"void"						{currentType = yytext(); return(Parser.VOID);}
"volatile"					{return(Parser.VOLATILE);}
"while"						{return(Parser.WHILE);}
"..."						{return(Parser.ELIPSIS);}
">>="						{return(Parser.RIGHT_ASSIGN);}
"<<="						{return(Parser.LEFT_ASSIGN);}
"+="						{return(Parser.ADD_ASSIGN);}
"-="						{return(Parser.SUB_ASSIGN);}
"*="						{return(Parser.MUL_ASSIGN);}
"/="						{return(Parser.DIV_ASSIGN);}
"%="						{return(Parser.MOD_ASSIGN);}
"&="						{return(Parser.AND_ASSIGN);}
"^="						{return(Parser.XOR_ASSIGN);}
"|="						{return(Parser.OR_ASSIGN);}
">>"						{return(Parser.RIGHT_OP);}
"<<"						{return(Parser.LEFT_OP);}
"++"						{return(Parser.INC_OP);}
"--"						{return(Parser.DEC_OP);}
"->"						{return(Parser.PTR_OP);}
"&&"						{return(Parser.L_AND_OP);}
"||"						{return(Parser.L_OR_OP);}
"<="						{return(Parser.LE_OP);}
">="						{return(Parser.GE_OP);}
"=="						{return(Parser.EQ_OP);}
"!="						{return(Parser.NE_OP);}
";"							{return(Parser.SEMI);}
"{"							{return(Parser.OPEN_BRACE);}
"}"							{return(Parser.CLOSE_BRACE);}
","							{return(Parser.COMMA);}
":"							{return(Parser.COLON);}
"="							{return(Parser.ASSIGN);}
"("							{return(Parser.OPEN_PAREN);}
")"							{return(Parser.CLOSE_PAREN);}
"["							{return(Parser.OPEN_BRACKET);}
"]"							{return(Parser.CLOSE_BRACKET);}
"."							{return(Parser.PERIOD);}
"&"							{return(Parser.AND_OP);}
"!"							{return(Parser.NOT_OP);}
"~"							{return(Parser.BIT_NOT_OP);}
"-"							{return(Parser.SUB_OP);}
"+"							{return(Parser.ADD_OP);}
"*"							{pointerType = true; return(Parser.MUL_OP);}
"/"							{return(Parser.DIV_OP);}
"%"							{return(Parser.MOD_OP);}
"<"							{return(Parser.LT_OP);}
">"							{return(Parser.GT_OP);}
"^"							{return(Parser.XOR_OP);}
"|"							{return(Parser.OR_OP);}
"?"							{return(Parser.QUESTION_MARK);}
"!!S"						{Driver.st.writeToFile("st_debug.txt");}
"!!S"{digit}+				{Driver.st.writeToFile("st_debug" + yytext().substring(3) + ".txt");}
"!!L"						{insertMode = false;}

{identifier} { 
	if(insertMode) {
		SymbolTable.FindReturn ret = Driver.st.find(yytext());
		if(ret != null) {
			SymbolNode node = ret.node;
			//SymbolNode node = Driver.st.find(yytext(),true);
		
			if(node != null && ret.level == 0)
				yyerror("identifier (" + yytext() + ") already defined on line " + String.valueOf(node.lineNum));
			else if(node != null)
				yywarning("shadowing variable (" + yytext() + ") declared on line " + String.valueOf(node.lineNum));
		}
		currentNode = new SymbolNode(new BasicDataType(currentType, pointerType),yyline+1);	
		Driver.st.insert(yytext(), currentNode);
		typeStack.push(yytext());
	} else {
		currentNode = Driver.st.find(yytext()).node;
		if(currentNode == null)
			yyerror("undefined symbol: " + yytext());
	}
	pointerType = false;
	lastID = yytext();
	yylval = currentNode;
	return(Parser.IDENTIFIER);
}

{int_lit_one} {
	String numVal = yytext().substring(2);
	// detect overflow
	try {		
		long overFlow = (long)Integer.MAX_VALUE * 2L;
		long value = Long.parseLong(numVal,16);
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("integer overflow");
	}
	yylval = new Integer(Integer.parseInt(numVal));
	return(Parser.INTEGER_CONSTANT);
}

{int_lit_two} {
	String numVal = yytext().substring(1);
	// detect overflow
	try {
		long overFlow = (long)Integer.MAX_VALUE * 2L;
		long value = Long.parseLong(numVal,8);
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("integer overflow");
	}		
	yylval = new Integer(Integer.parseInt(numVal));
	return(Parser.INTEGER_CONSTANT);
}

{int_lit_three} {
	// detect overflow
	try {
		long overFlow = (long)Integer.MAX_VALUE * 2L;
		long value = Long.parseLong(yytext());		
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("integer overflow");
	}
	lastID = "";
	yylval = new Integer(Integer.parseInt(yytext()));
	return(Parser.INTEGER_CONSTANT); 
}

{char_lit} {
	yylval = new Character(yytext().charAt(1));
	return(Parser.CHARACTER_CONSTANT);
}

{float_lit_one} {
	// detect overflow
	try {
		double overFlow = (double)Float.MAX_VALUE;
		double value = Double.parseDouble(yytext());
		System.out.println(overFlow);
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("floating point overflow");
	}
	yylval = new Double(Double.parseDouble(yytext()));
	return(Parser.FLOATING_CONSTANT); 	
}

{float_lit_two} {
	// detect overflow
	try {
		double overFlow = (double)Float.MAX_VALUE;
		double value = Double.parseDouble(yytext());
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("floating point overflow");
	}
	yylval = new Double(Double.parseDouble(yytext()));
	return(Parser.FLOATING_CONSTANT); 	
}

{float_lit_three} {
	// detect overflow
	try {
		double overFlow = (double)Float.MAX_VALUE;
		double value = Double.parseDouble(yytext());
		if(value > overFlow)
			throw new NumberFormatException();
	} catch(NumberFormatException e) {
		yyerror("floating point overflow");
	}
	yylval = new Double(Double.parseDouble(yytext()));
	return(Parser.FLOATING_CONSTANT);
}

{str_literal} {
	yylval = yytext();
	return(Parser.STRING_LITERAL);
}

.	{yyerror("undefined symbol " + yytext()); return(Parser.ERROR_TOK);}


