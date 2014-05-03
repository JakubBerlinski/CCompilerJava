%language "Java"
%define parser_class_name "AssemblyParser"
%define public
%define package "Assembly"

%code imports {
  import java.io.StreamTokenizer;
  import java.io.InputStream;
  import java.io.InputStreamReader;
  import java.io.FileInputStream;
  import java.io.Reader;
  import java.io.IOException;
  import java.io.PrintWriter;
}

%code {
	public int debugLevel = AssemblyDriver.debugLevel;
	public int frame;
	public int parameterIndex = 0;
	
	private RegisterAllocator ra = AssemblyDriver.ra;
	private PrintWriter fout = AssemblyDriver.fout;
}

%debug
%error-verbose

%token REGISTER RETVAL INT_LITERAL FLOAT_LITERAL CHAR_LITERAL STRING_LITERAL PARAM CALL
%token MLOAD MSTORE LOADS LOADI LOADC LOADF ADDI ADDF MULI MULF NUM_PARAMS SUBI SUBF
%token FBEGIN AFRAME FEND BNZ BAL JUMP CONT BREAK RETURN MOV
%token LOR LAND OR XOR AND IDENTIFIER LABEL LT LTE GT GTE EQ NE

%start start
%%

start
	: start statement { }
	| 
	;

statement
	: FBEGIN IDENTIFIER { 
		AssemblyScanner.lastFunction = AssemblyScanner.lastString; 
		AssemblyDriver.outputAssembly1(".ent", AssemblyScanner.lastString, AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
	}
	| NUM_PARAMS INT_LITERAL {
		int count = AssemblyScanner.lastInt;	
		for(int i = 0; i < count; i++)
			AssemblyDriver.outputAssembly2("sw", "$a" + i, String.valueOf(4 * i) + "($sp)", ""); 
			
		AssemblyDriver.scanner.currentRegister = 0;
	}
	| AFRAME INT_LITERAL { 
		frame = AssemblyScanner.lastInt;
		while(frame % 8 != 0)
			frame++;
			
		AssemblyDriver.outputAssembly3("subu", "$sp", "$sp", String.valueOf(frame), AssemblyScanner.comment);
		AssemblyDriver.outputAssembly2("sw", "$31", String.valueOf(frame-4) + "($sp)", AssemblyScanner.comment);
		AssemblyDriver.outputAssembly2(".mask","0x80000000", "-4", AssemblyScanner.comment);
		AssemblyDriver.outputAssembly3(".frame", "$sp", String.valueOf(frame), "$31", AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;

	}
	| LABEL { 
		AssemblyDriver.outputAssemblyLabel(AssemblyScanner.lastLabel);
		AssemblyDriver.scanner.currentRegister = 0;

	}
	| LOADS REGISTER STRING_LITERAL {
	  	AssemblyDriver.scanner.currentRegister = 0;
	}
	| LOADI REGISTER INT_LITERAL {
		AssemblyDriver.outputAssembly2("li", AssemblyScanner.registers[0], String.valueOf(AssemblyScanner.lastInt),AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
	}
	| LOADC REGISTER CHAR_LITERAL { 
		AssemblyDriver.outputAssembly2("li", AssemblyScanner.registers[0], String.valueOf(AssemblyScanner.lastChar), AssemblyScanner.comment);
	 	AssemblyDriver.scanner.currentRegister = 0;
	}
	| LOADF REGISTER FLOAT_LITERAL { 
		AssemblyDriver.outputAssembly2("li.s", AssemblyScanner.registers[0], String.valueOf(AssemblyScanner.lastFloat),AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
	}
	| MLOAD REGISTER REGISTER {
		if(AssemblyScanner.registers[1].charAt(0) == '$') {
			AssemblyDriver.outputAssembly3("addu", AssemblyScanner.registers[1], AssemblyScanner.registers[1], "$sp", AssemblyScanner.comment);
			AssemblyScanner.registers[1] = "0(" + AssemblyScanner.registers[1] + ")";
		}
		AssemblyDriver.outputAssembly2("lw", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
	}
	| MSTORE REGISTER REGISTER { 			
		if(AssemblyScanner.registers[0].charAt(0) == '$') {
			AssemblyDriver.outputAssembly3("addu", AssemblyScanner.registers[0], AssemblyScanner.registers[0], "$sp", AssemblyScanner.comment);
			AssemblyScanner.registers[0] = "0(" + AssemblyScanner.registers[0] + ")";
		}
		AssemblyDriver.outputAssembly2("sw", AssemblyScanner.registers[1], AssemblyScanner.registers[0], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);	
	}
	| ADDI REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("addu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);	
	}
	| ADDI REGISTER REGISTER INT_LITERAL { 
		AssemblyDriver.outputAssembly3("addu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], String.valueOf(AssemblyScanner.lastInt), AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		//ra.freeRegister(AssemblyScanner.registers[2]);
	}
	| ADDF REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("add.s", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);	
	}
	| SUBI REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("subu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);	
	}
	| SUBI REGISTER REGISTER INT_LITERAL { 
		AssemblyDriver.outputAssembly3("subu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], String.valueOf(AssemblyScanner.lastInt), AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		//ra.freeRegister(AssemblyScanner.registers[2]);
	}
	| SUBF REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("sub.s", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);	
	}
	| MULI REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("mulu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	| MULI REGISTER REGISTER INT_LITERAL { 
		AssemblyDriver.outputAssembly3("mulu", AssemblyScanner.registers[0], AssemblyScanner.registers[1], String.valueOf(AssemblyScanner.lastInt), AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);		
	}
	| MULF REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("mul.s", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	| FEND IDENTIFIER { 	
		if(AssemblyScanner.lastFunction.equals("main")) {
			AssemblyDriver.outputAssembly2("li", "$v0", "17", "Exit Program");
			AssemblyDriver.outputAssembly("syscall", "Exit Program");
		} else {
			AssemblyDriver.outputAssembly2("lw", "$31", String.valueOf(frame-4) + "($sp)", AssemblyScanner.comment);
			AssemblyDriver.outputAssembly3("addu", "$sp", "$sp", String.valueOf(frame), AssemblyScanner.comment);
			AssemblyDriver.outputAssembly1("jr","$31",AssemblyScanner.comment);
			AssemblyDriver.outputAssembly1(".end", AssemblyScanner.lastString, AssemblyScanner.comment + "\n\n");
		}
		AssemblyDriver.scanner.currentRegister = 0; 
	}
	| BNZ REGISTER IDENTIFIER { 
		AssemblyDriver.outputAssembly2("bnez", AssemblyScanner.registers[0], AssemblyScanner.lastString, AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		AssemblyDriver.ra.freeRegister(AssemblyScanner.registers[0]);

	}
	| BAL IDENTIFIER { 
		AssemblyDriver.outputAssembly1("b", AssemblyScanner.lastString, AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		AssemblyDriver.ra.freeRegister(AssemblyScanner.registers[0]);

	}
	| JUMP REGISTER { 
		AssemblyDriver.outputAssembly1("j", AssemblyScanner.registers[0], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		AssemblyDriver.ra.freeRegister(AssemblyScanner.registers[0]);
	}
	| CONT {

	}
	| BREAK {

	}
	| PARAM REGISTER {
		AssemblyDriver.outputAssembly2("move", "$a" + parameterIndex, AssemblyScanner.registers[0], AssemblyScanner.comment);
		parameterIndex++;
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[0]);
	}	
	| CALL IDENTIFIER {
		AssemblyDriver.outputAssembly1("jal", AssemblyScanner.lastString, AssemblyScanner.comment);
		parameterIndex = 0; 
		AssemblyDriver.scanner.currentRegister = 0;

	}
	| RETURN REGISTER {
		String reg, command;
		if(AssemblyScanner.registers[0].charAt(1) == 't') {
			reg = "$v0";
			command = "move";
		} else {
			reg = "$f0";
			command = "mov.s";
		} 
		AssemblyDriver.outputAssembly2(command, reg, AssemblyScanner.registers[0], AssemblyScanner.comment);
	 	AssemblyDriver.scanner.currentRegister = 0;
	}
	| RETURN RETVAL	{
		String reg, command;
		if(AssemblyScanner.registers[0].charAt(1) == 't') {
			reg = "$v0";
			command = "move";
		} else {
			reg = "$f0";
			command = "mov.s";
		} 
		AssemblyDriver.outputAssembly2(command, reg, AssemblyScanner.registers[0], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
	}
	| MOV REGISTER REGISTER { 
		String command;
		if(AssemblyScanner.registers[1].charAt(1) == 't')
			command = "move";
		else
			command = "mov.s";
		AssemblyDriver.outputAssembly2(command, AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
	}
	| MOV RETVAL REGISTER {
		String command;		
		if(AssemblyScanner.registers[1].charAt(1) == 't')
			command = "move";
		else
			command = "mov.s";
		AssemblyDriver.outputAssembly2(command, AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0; 
		ra.freeRegister(AssemblyScanner.registers[1]);
	}
	
	| MOV REGISTER RETVAL {
		String command;
		if(AssemblyScanner.registers[0].charAt(1) == 't')
			command = "move";
		else
			command = "mov.s";
		AssemblyDriver.outputAssembly2(command, AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.comment);
		AssemblyDriver.scanner.currentRegister = 0; 
		ra.freeRegister(AssemblyScanner.registers[1]);
	}
	
	| LOR REGISTER REGISTER REGISTER {
		AssemblyDriver.outputAssembly3("or", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| LAND REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("and", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| OR REGISTER REGISTER REGISTER {
		AssemblyDriver.outputAssembly3("or", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| XOR REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("xor", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| AND REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("and", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| LT REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("slt", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| LTE REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("sle", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| GT REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("sgt", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| GTE REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("sge", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| EQ REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("seq", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	
	| NE REGISTER REGISTER REGISTER { 
		AssemblyDriver.outputAssembly3("sne", AssemblyScanner.registers[0], AssemblyScanner.registers[1], AssemblyScanner.registers[2], AssemblyScanner.comment); 
		AssemblyDriver.scanner.currentRegister = 0;
		ra.freeRegister(AssemblyScanner.registers[1]);
		ra.freeRegister(AssemblyScanner.registers[2]);
	}
	;

%%

