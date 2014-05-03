package Assembly;

import java.io.*;
import java.util.ArrayList;

%%

%{
	public String lineBuffer = "";
	public static String comment = "";
	public String statement;
	public static String lastLabel;
	public static int lastInt;
	public static float lastFloat;
	public static char lastChar;
	public static String lastString;
	public static int lastFrame;
	public static String lastOffset;
	public static String lastFunction;
	public int currentRegister = 0;
	public static String[] registers = new String[3];
	
	//public ArrayList<String> operands = new ArrayList();
	
	protected Object yylval;
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
				fin = new FileInputStream(AssemblyDriver.inFile);				
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
		System.err.println(AssemblyDriver.inFile + ": error on line " + String.valueOf(yyline+1) + " column " + String.valueOf(yycolumn+1));
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
		if(yyline == 0) {
			lineBuffer = new String();
			try {
				int data;
				fin = new FileInputStream(AssemblyDriver.inFile);				
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
		System.err.println(AssemblyDriver.inFile + ": warning on line " + String.valueOf(yyline+1) + " column " + String.valueOf(yycolumn+1));
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

%class AssemblyScanner
%public
%line
%column
%byaccj
%table
%implements AssemblyParser.Lexer

ws = 				[ \t]+
digit =				[0-9]
letter =			[a-zA-Z_]

identifier = 		{letter}({letter}|{digit})*
int_literal = 		{digit}+
char_literal = 		L?'(\\.|[^\\'])+'
float_literal = 	{digit}+"."{digit}+
string_literal = 	L?"\""([^\n\"\\]*(\\[.\n])*)*"\""
retVal = 			("f" | "i")"_ret"
register = 			{letter}"_temp"({digit})+
offset =	 		({digit})+"(off)"
label = 			.*":"

%%
{ws}				{/* No action taken */}
"\n".*				{
					  lineBuffer = yytext().substring(1); int index = lineBuffer.indexOf("#") + 1;
					  comment = (index < 1) ? comment : lineBuffer.substring(index);
					  yypushback(lineBuffer.length());
					}
"#".*"\n"			{ /* Comment */ yypushback(1); }
{retVal}			{ registers[currentRegister++] = AssemblyDriver.ra.getRegister(yytext()).register; return AssemblyParser.RETVAL;			}
{register} 			{ registers[currentRegister++] = AssemblyDriver.ra.getRegister(yytext()).register; return AssemblyParser.REGISTER;}
{int_literal}		{ lastInt = Integer.parseInt(yytext()); return AssemblyParser.INT_LITERAL;}
{char_literal}		{ lastChar = yytext().charAt(0); return AssemblyParser.CHAR_LITERAL;}
{float_literal} 	{ lastFloat = Float.parseFloat(yytext()); return AssemblyParser.FLOAT_LITERAL;}
{string_literal}	{ lastString = yytext(); return AssemblyParser.STRING_LITERAL;}
{label}				{ lastLabel = yytext(); return AssemblyParser.LABEL;}
{offset}			{ registers[currentRegister++] = yytext().replace("off","$sp");	return AssemblyParser.REGISTER;}

"M_LOAD"			{ return AssemblyParser.MLOAD;}
"M_STORE"			{ return AssemblyParser.MSTORE;}
"LOAD_S"			{ return AssemblyParser.LOADS;}
"LOAD_I"			{ return AssemblyParser.LOADI;}
"LOAD_C"			{ return AssemblyParser.LOADC;}
"LOAD_F"			{ return AssemblyParser.LOADF;}
"ADD_I"				{ return AssemblyParser.ADDI;}
"ADD_F"				{ return AssemblyParser.ADDF;}
"SUB_I"				{ return AssemblyParser.SUBI;}
"SUB_F"				{ return AssemblyParser.SUBF;}
"MUL_I"				{ return AssemblyParser.MULI;}
"MUL_F"				{ return AssemblyParser.MULF;}

"FUNC_BEGIN"		{ return AssemblyParser.FBEGIN;}
"ALLOC_FRAME"		{ return AssemblyParser.AFRAME;}
"FUNC_END"			{ return AssemblyParser.FEND;}
"BNZ"				{ return AssemblyParser.BNZ;}
"BAL"				{ return AssemblyParser.BAL;}
"JUMP"				{ return AssemblyParser.JUMP;}
"CONTINUE"			{ return AssemblyParser.CONT;}
"BREAK"				{ return AssemblyParser.BREAK;}
"RETURN"			{ return AssemblyParser.RETURN;}
"MOV"				{ return AssemblyParser.MOV;}
"L_OR"				{ return AssemblyParser.LOR;}
"L_AND"				{ return AssemblyParser.LAND;}
"OR"				{ return AssemblyParser.OR;}
"XOR"				{ return AssemblyParser.XOR;}
"AND"				{ return AssemblyParser.AND;}
"LT"				{ return AssemblyParser.LT;}
"LE"				{ return AssemblyParser.LTE;}
"GT"				{ return AssemblyParser.GT;}
"GE"				{ return AssemblyParser.GTE;}
"EQ"				{ return AssemblyParser.EQ;}
"NE"				{ return AssemblyParser.NE;}
"PARAM"				{ return AssemblyParser.PARAM;}
"CALL"				{ return AssemblyParser.CALL;}
"NUM_PARAMS"		{ return AssemblyParser.NUM_PARAMS;}
"!!RA"				{ System.out.println("\n" + AssemblyDriver.ra.toString()); }
{identifier}		{ lastString = yytext(); return AssemblyParser.IDENTIFIER;}
.					{ yyerror("unkown token " + yytext()); }






