%language "Java"
%define parser_class_name "Parser"
%define public
%define package "Compiler"

%code imports {
  import java.io.StreamTokenizer;
  import java.io.InputStream;
  import java.io.InputStreamReader;
  import java.io.FileInputStream;
  import java.io.Reader;
  import java.io.IOException;
  import java.util.ArrayList;
}

%code {
	public int arrayCount = 0;
	public int stackSize = 0;
	public int parameterSize = 0;
	public int parameterCount = 0;
	public int currentParameterCount = 0;
	public String oldId = null;
	public String functionComment = null;
	public int oldLineNum = 0;
	
	public ArrayList<String> parameterTypes = new ArrayList<String>();
	public ArrayList<BasicDataType> typeList = new ArrayList<BasicDataType>();
}

%debug
%error-verbose
//%expect 5

%token IDENTIFIER 
%token INTEGER_CONSTANT FLOATING_CONSTANT CHARACTER_CONSTANT ENUMERATION_CONSTANT 
%token STRING_LITERAL 
%token SIZEOF
%token PTR_OP 
%token INC_OP DEC_OP 
%token LEFT_OP RIGHT_OP 
%token LE_OP GE_OP EQ_OP NE_OP
%token AND_OP OR_OP L_AND_OP L_OR_OP
%token MUL_ASSIGN DIV_ASSIGN MOD_ASSIGN ADD_ASSIGN SUB_ASSIGN 
%token LEFT_ASSIGN RIGHT_ASSIGN AND_ASSIGN XOR_ASSIGN OR_ASSIGN 
%token TYPEDEF_NAME

%token TYPEDEF EXTERN STATIC AUTO REGISTER
%token CHAR SHORT INT LONG SIGNED UNSIGNED FLOAT DOUBLE CONST VOLATILE VOID
%token STRUCT UNION ENUM ELIPSIS RANGE

// Character Tokens
%token SEMI OPEN_BRACE CLOSE_BRACE COMMA ASSIGN COLON PERIOD
%token OPEN_PAREN CLOSE_PAREN OPEN_BRACKET CLOSE_BRACKET QUESTION_MARK
%token MUL_OP DIV_OP MOD_OP ADD_OP SUB_OP
%token BIT_NOT_OP NOT_OP XOR_OP
%token LT_OP GT_OP
%token ERROR_TOK

%token CASE DEFAULT IF ELSE SWITCH WHILE DO FOR GOTO CONTINUE BREAK RETURN

%type<AST_node> translation_unit external_declaration function_definition
%type<AST_node> declaration declaration_list declaration_specifiers
%type<AST_node> storage_class_specifier type_specifier type_qualifier
%type<AST_node> struct_or_union_specifier struct_or_union
%type<AST_node> struct_declaration_list init_declarator_list
%type<AST_node> init_declarator struct_declaration specifier_qualifier_list
%type<AST_node> struct_declarator_list struct_declarator enum_specifier
%type<AST_node> enumerator_list enumerator declarator direct_declarator
%type<AST_node> pointer type_qualifier_list parameter_type_list parameter_list
%type<AST_node> parameter_declaration identifier_list initializer
%type<AST_node> initializer_list type_name abstract_declarator
%type<AST_node> direct_abstract_declarator statement labeled_statement
%type<AST_node> expression_statement compound_statement statement_list
%type<AST_node> selection_statement iteration_statement jump_statement
%type<AST_node> expression assignment_expression assignment_operator
%type<AST_node> conditional_expression constant_expression
%type<AST_node> logical_or_expression logical_and_expression
%type<AST_node> inclusive_or_expression exclusive_or_expression
%type<AST_node> and_expression equality_expression relational_expression
%type<AST_node> shift_expression additive_expression multiplicative_expression
%type<AST_node> cast_expression unary_expression unary_operator
%type<AST_node> postfix_expression primary_expression argument_expression_list
%type<AST_node> constant string identifier

%start translation_unit
%%

translation_unit
	: {Scanner.insertMode = true; } external_declaration {
		Driver.ast.setRoot(new AST_translation_unit($2, null, Driver.scanner.lineBuffer));
		$$ = new AST_translation_unit($2,null, Driver.scanner.lineBuffer);	
	}
	| translation_unit {Scanner.insertMode = true; } external_declaration {
		Driver.ast.setRoot(new AST_translation_unit($1, $3, Driver.scanner.lineBuffer));
		$$ = new AST_translation_unit($1,$3, Driver.scanner.lineBuffer);
	}
	;

external_declaration
	: function_definition {	
	AST_node ret = new AST_external_declaration($1, Driver.scanner.lineBuffer);
	
	$$ = ret;
	}
	| declaration {	
	AST_node ret = new AST_external_declaration($1, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

//FD1: {Scanner.functionType = true;};
//FD2: {Driver.st.pushLevel();};
FD3: {Scanner.functionType = false;};

function_definition
	: declarator FD3 compound_statement {	
	functionComment = $1.currentLine;
	AST_node ret = new AST_function_definition($1, $3, null, null,functionComment, stackSize + parameterSize);
	
	parameterSize = 0;
	parameterCount = 0;
	
	$$ = ret;
	
	}
	| declarator declaration_list FD3 compound_statement {
	functionComment = $1.currentLine;
	AST_node ret = new AST_function_definition($1, $2, $4, null, functionComment, stackSize + parameterSize);
	
	parameterSize = 0;
	parameterCount = 0;
	
	$$ = ret;
	
	}
	| declaration_specifiers declarator FD3 compound_statement {
	functionComment = $2.currentLine;
	Driver.st.popLevel();	
	
	
	String retType = (String) $1.getData();
	String funcID = null;
	
	if($2.getClass().getName().contains("identifier"))
		funcID = (String) $2.getData();
		
	else
		funcID = (String) $2.nodes.get(0).getData();
		
	String[] params = parameterTypes.toArray(new String[0]);
	
	//for(String s : params)
		//System.out.println(s);
	
	//System.out.println($2.getClass().getName());
	//System.out.println(retType + " " + funcID);
	
	SymbolNode funcNode = new SymbolNode(new FunctionDataType(retType,parameterCount, params), oldLineNum);	
	Driver.st.insert(funcID, funcNode);
	AST_node ret = new AST_function_definition($1, $2, $4, null, functionComment, stackSize + parameterSize, funcNode);
	parameterSize = 0;
	parameterCount = 0;
	
	parameterTypes.clear();
	
	
	$$ = ret;
	
	}
	| declaration_specifiers declarator declaration_list FD3 compound_statement {
	functionComment = $2.currentLine;
	Driver.st.popLevel();
	
	String retType = (String) $1.getData();
	String funcID = (String) $2.getData();
	
	String[] params = parameterTypes.toArray(new String[0]);
	
	SymbolNode funcNode = new SymbolNode(new FunctionDataType(retType,parameterCount, params), oldLineNum);
	Driver.st.insert(funcID, funcNode);
	AST_node ret = new AST_function_definition($1, $2, $3, $5, functionComment, stackSize, funcNode);
	parameterSize = 0;
	parameterCount = 0;
	
	parameterTypes.clear();
	

	
	$$ = ret;
	
	}
	;

declaration
	: declaration_specifiers SEMI {	
	AST_node ret = new AST_declaration($1, null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	//Scanner.insertMode = false;
	
	}
	| declaration_specifiers init_declarator_list SEMI {	
	AST_node ret = new AST_declaration($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	//Scanner.insertMode = false;
	
	}
	;

declaration_list
	: declaration {	
	AST_node ret = new AST_declaration_list($1, null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| declaration_list declaration {	
	AST_node ret = new AST_declaration_list($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

declaration_specifiers
	: storage_class_specifier {	
	AST_node ret = new AST_declaration_specifiers($1, null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	
	}
	| storage_class_specifier declaration_specifiers {	
	AST_node ret = new AST_declaration_specifiers($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	 
	| type_specifier {	
	AST_node ret = new AST_declaration_specifiers($1, null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	
	}
	| type_specifier declaration_specifiers {	
	AST_node ret = new AST_declaration_specifiers($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| type_qualifier  {	
	AST_node ret = new AST_declaration_specifiers($1, null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	}
	| type_qualifier declaration_specifiers {	
	AST_node ret = new AST_declaration_specifiers($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

storage_class_specifier
	: AUTO {	
	AST_node ret = new AST_storage_class_specifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| REGISTER {	
	AST_node ret = new AST_storage_class_specifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| STATIC {	
	AST_node ret = new AST_storage_class_specifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| EXTERN {	
	AST_node ret = new AST_storage_class_specifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| TYPEDEF {	
	AST_node ret = new AST_storage_class_specifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

type_specifier
	: VOID {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| CHAR {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SHORT {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| INT {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| LONG {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FLOAT {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	} 
	| DOUBLE {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SIGNED {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| UNSIGNED {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| struct_or_union_specifier {	
	AST_node ret = new AST_type_specifier("",$1, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| enum_specifier {	
	AST_node ret = new AST_type_specifier("",$1, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| TYPEDEF_NAME {	
	AST_node ret = new AST_type_specifier(Driver.scanner.yytext(),null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

type_qualifier
	: CONST {	
	AST_node ret = new AST_type_qualifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| VOLATILE {	
	AST_node ret = new AST_type_qualifier(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_or_union_specifier
	: struct_or_union identifier OPEN_BRACE struct_declaration_list CLOSE_BRACE {	
	AST_node ret = new AST_struct_or_union_specifier($1,$2,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| struct_or_union OPEN_BRACE struct_declaration_list CLOSE_BRACE {	
	AST_node ret = new AST_struct_or_union_specifier($1,$3,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| struct_or_union identifier {	
	AST_node ret = new AST_struct_or_union_specifier($1,$2,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_or_union
	: STRUCT {	
	AST_node ret = new AST_struct_or_union(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| UNION {	
	AST_node ret = new AST_struct_or_union(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_declaration_list
	: struct_declaration {	
	AST_node ret = new AST_struct_declaration_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	
	}
	| struct_declaration_list struct_declaration {	
	AST_node ret = new AST_struct_declaration_list($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

init_declarator_list
	: init_declarator {	
	AST_node ret = new AST_init_declarator_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	
	}
	| init_declarator_list COMMA init_declarator {	
	AST_node ret = new AST_init_declarator_list($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

init_declarator
	: declarator {	
	AST_node ret = new AST_init_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else
		$$ = ret;
	
	}
	| declarator ASSIGN initializer {	
	AST_node ret = new AST_init_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_declaration
	: specifier_qualifier_list struct_declarator_list SEMI {	
	AST_node ret = new AST_struct_declaration($1, $2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

specifier_qualifier_list
	: type_specifier {	
	AST_node ret = new AST_specifier_qualifier_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| type_specifier specifier_qualifier_list {	
	AST_node ret = new AST_specifier_qualifier_list($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| type_qualifier {	
	AST_node ret = new AST_specifier_qualifier_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| type_qualifier specifier_qualifier_list {	
	AST_node ret = new AST_specifier_qualifier_list($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_declarator_list
	: struct_declarator {	
	AST_node ret = new AST_struct_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| struct_declarator_list COMMA struct_declarator {	
	AST_node ret = new AST_struct_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

struct_declarator
	: declarator {	
	AST_node ret = new AST_struct_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| COLON constant_expression {	
	AST_node ret = new AST_struct_declarator($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| declarator COLON constant_expression {	
	AST_node ret = new AST_struct_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

enum_specifier
	: ENUM OPEN_BRACE enumerator_list CLOSE_BRACE {	
	AST_node ret = new AST_enum_specifier($3,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $3;
		
	else	
		$$ = ret;
	
	}
	| ENUM identifier OPEN_BRACE enumerator_list CLOSE_BRACE {	
	AST_node ret = new AST_enum_specifier($2,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| ENUM identifier {	
	AST_node ret = new AST_enum_specifier($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	;

enumerator_list
	: enumerator {	
	AST_node ret = new AST_enumerator_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| enumerator_list COMMA enumerator {	
	AST_node ret = new AST_enumerator_list($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

enumerator
	: identifier {	
	AST_node ret = new AST_enumerator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| identifier ASSIGN constant_expression {	
	AST_node ret = new AST_enumerator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

declarator
	: direct_declarator {	
	AST_node ret = new AST_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| pointer direct_declarator {	
	AST_node ret = new AST_declarator($1,$2, Driver.scanner.lineBuffer);

	$$ = ret;
	
	}
	;

direct_declarator
	: identifier { 	
	AST_node ret = new AST_direct_declarator($1,null, Driver.scanner.lineBuffer);
	
	arrayCount = 0;
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| OPEN_PAREN declarator CLOSE_PAREN {	
	AST_node ret = new AST_direct_declarator($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| direct_declarator OPEN_BRACKET CLOSE_BRACKET {	Driver.scanner.yyerror("No array size given");
	AST_node ret = new AST_direct_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	//////////////////////////////////////// WORK ON NESTED ARRAY ////////////////////////////////////
	| direct_declarator OPEN_BRACKET constant_expression CLOSE_BRACKET {
	
	Integer obj = (Integer)$3.getData();
	
	int size = (obj != null) ? obj.intValue() : 0;
	
	if(arrayCount == 0) {
		String id = Scanner.typeStack.pop();
		oldId = id;
		
		Driver.st.removeNode(id);
		Driver.st.insert(id, new SymbolNode(new ArrayDataType(Driver.scanner.currentType, size)));
	}
		
	else {
		//Driver.st.insert(oldId, new SymbolNode(new ArrayDataType(
		SymbolNode oldValue = Driver.st.removeNode(oldId);
		
		ArrayDataType oldType = (ArrayDataType) oldValue.type;
		
		oldType.addNestedArray(new ArrayDataType(Driver.scanner.currentType, size));
		
		Driver.st.insert(oldId, new SymbolNode(oldType));
	}
	
	AST_node ret = new AST_direct_declarator("[]",$1,$3, Driver.scanner.lineBuffer);
	
	arrayCount++;
	
	$$ = ret;
	
	}
	| direct_declarator OPEN_PAREN {Driver.st.pushLevel();} CLOSE_PAREN {	//System.out.println("Here: " + Driver.scanner.lineBuffer);
	
	//Driver.st.insert(Driver.scanner.lastID, new SymbolNode(new FunctionDataType(Driver.scanner.currentType,0), Driver.scanner.getLineNo()));
	oldLineNum = Driver.scanner.getLineNo();
	AST_node ret = new AST_direct_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| direct_declarator OPEN_PAREN {/*Driver.st.insert(Driver.scanner.lastID, new SymbolNode(new FunctionDataType(Driver.scanner.currentType,0), Driver.scanner.getLineNo())); */Driver.st.pushLevel();} parameter_type_list CLOSE_PAREN {	
	
	oldLineNum = Driver.scanner.getLineNo();
	
	AST_node ret = new AST_direct_declarator($1,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| direct_declarator OPEN_PAREN identifier_list CLOSE_PAREN {	//System.out.println("Here2: " + Driver.scanner.lineBuffer);
	AST_node ret = new AST_direct_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

pointer
	: MUL_OP {	
	AST_node ret = new AST_pointer(null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| MUL_OP type_qualifier_list {	
	AST_node ret = new AST_pointer($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| MUL_OP pointer {	
	AST_node ret = new AST_pointer($2,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| MUL_OP type_qualifier_list pointer {	
	AST_node ret = new AST_pointer($2,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

type_qualifier_list
	: type_qualifier {	
	AST_node ret = new AST_type_qualifier_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| type_qualifier_list type_qualifier {	
	AST_node ret = new AST_type_qualifier_list($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

parameter_type_list
	: parameter_list {	
	AST_node ret = new AST_parameter_type_list($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| parameter_list COMMA ELIPSIS {	
	AST_node ret = new AST_parameter_type_list($1, true, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	;

parameter_list
	: parameter_declaration {	
	
	parameterSize = Driver.st.offsets.peek().intValue();
	parameterCount++;
	AST_node ret = new AST_parameter_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| parameter_list COMMA parameter_declaration {	
	
	parameterSize = Driver.st.offsets.peek().intValue();
	parameterCount++;
	
	AST_node ret = new AST_parameter_list($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

parameter_declaration
	: declaration_specifiers declarator {	
	AST_node ret = new AST_parameter_declaration($1,$2, Driver.scanner.lineBuffer);
	
	parameterTypes.add((String)$1.getData());
	
	$$ = ret;
	
	}
	| declaration_specifiers {	
	AST_node ret = new AST_parameter_declaration($1,null, Driver.scanner.lineBuffer);
	
	parameterTypes.add((String)$1.getData());
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| declaration_specifiers abstract_declarator {	
	AST_node ret = new AST_parameter_declaration($1,$2, Driver.scanner.lineBuffer);
	
	parameterTypes.add((String)$1.getData());
	
	$$ = ret;
	
	}
	;

identifier_list
	: identifier {	
	AST_node ret = new AST_identifier_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| identifier_list COMMA identifier {	
	AST_node ret = new AST_identifier_list($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

initializer
	: assignment_expression {	
	AST_node ret = new AST_initializer($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| OPEN_BRACE initializer_list CLOSE_BRACE {	
	AST_node ret = new AST_initializer($2, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| OPEN_BRACE initializer_list COMMA CLOSE_BRACE {	
	AST_node ret = new AST_initializer($2, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	;

initializer_list
	: initializer {	
	AST_node ret = new AST_initializer_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| initializer_list COMMA initializer {	
	AST_node ret = new AST_initializer_list($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

type_name
	: specifier_qualifier_list {	
	AST_node ret = new AST_type_name($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| specifier_qualifier_list abstract_declarator {	
	AST_node ret = new AST_type_name($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

abstract_declarator
	: pointer {	
	AST_node ret = new AST_abstract_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| direct_abstract_declarator {	
	AST_node ret = new AST_abstract_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else
		$$ = ret;
	
	}
	| pointer direct_abstract_declarator {	
	AST_node ret = new AST_abstract_declarator($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

direct_abstract_declarator
	: OPEN_PAREN abstract_declarator CLOSE_PAREN {	
	AST_node ret = new AST_direct_abstract_declarator($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| OPEN_BRACKET CLOSE_BRACKET {	
	AST_node ret = new AST_direct_abstract_declarator(null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| OPEN_BRACKET constant_expression CLOSE_BRACKET {	
	AST_node ret = new AST_direct_abstract_declarator($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
	
	else	
		$$ = ret;
	
	}
	| direct_abstract_declarator OPEN_BRACKET CLOSE_BRACKET {	
	AST_node ret = new AST_direct_abstract_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| direct_abstract_declarator OPEN_BRACKET constant_expression CLOSE_BRACKET {	
	AST_node ret = new AST_direct_abstract_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| OPEN_PAREN CLOSE_PAREN {	
	AST_node ret = new AST_direct_abstract_declarator(null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| OPEN_PAREN parameter_type_list CLOSE_PAREN {	
	AST_node ret = new AST_direct_abstract_declarator($2,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
		
	else	
		$$ = ret;
	
	}
	| direct_abstract_declarator OPEN_PAREN CLOSE_PAREN {	
	AST_node ret = new AST_direct_abstract_declarator($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
		
	else	
		$$ = ret;
	
	}
	| direct_abstract_declarator OPEN_PAREN parameter_type_list CLOSE_PAREN {	
	AST_node ret = new AST_direct_abstract_declarator($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

statement
	: labeled_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| compound_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| expression_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| selection_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| iteration_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| jump_statement {	
	AST_node ret = new AST_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	;

labeled_statement
	: identifier COLON statement {	
	AST_node ret = new AST_labeled_statement("",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| CASE constant_expression COLON statement {	
	AST_node ret = new AST_labeled_statement("case",$2,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| DEFAULT COLON statement {	
	AST_node ret = new AST_labeled_statement("default",$3,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

expression_statement
	: SEMI {	
	AST_node ret = new AST_expression_statement(null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| expression SEMI {	
	AST_node ret = new AST_expression_statement($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	;

CS1:  { Scanner.insertMode = true; Driver.st.pushLevel();};

CS2:  {Scanner.insertMode = false;} ;

compound_statement
	: OPEN_BRACE CLOSE_BRACE {	
	AST_node ret = new AST_compound_statement(null,null, Driver.scanner.lineBuffer);
	
	//Driver.st.popLevel();
	
	$$ = ret;
	
	}
	//{$$= create_cs_node(NULL,NULL, Driver.scanner.lineBuffer); }
	| OPEN_BRACE CS2 statement_list CLOSE_BRACE {	
	AST_node ret = new AST_compound_statement($3,null, Driver.scanner.lineBuffer);
	
	stackSize = Driver.st.offsets.peek().intValue();
	//Driver.st.popLevel();
	
	if(Driver.reduceAST)
		$$ = $3;
	
	else	
		$$ = ret;
	
	}
	//{$$ = create_cs_node(NULL,$2, Driver.scanner.lineBuffer);}
	
	/********************************************** FIX *************************************/
	| OPEN_BRACE CS1 declaration_list CS2 CLOSE_BRACE { 	
	
	stackSize = Driver.st.offsets.peek().intValue();
	Driver.st.popLevel();
	
	AST_node ret = new AST_compound_statement($3,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $3;
	
	else	
		$$ = ret;
	
	}
	 //{$$= create_cs_node($3,NULL}; } //Optimize would replace $3 wih NULL and free $3's tree
	 /******************************************************************************************/
	| OPEN_BRACE CS1 declaration_list CS2 statement_list CLOSE_BRACE {
	
	stackSize = Driver.st.offsets.peek().intValue();
	Driver.st.popLevel();
	
	AST_node ret = new AST_compound_statement($3,$5, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	 // { $$= create_cs_node((decl_list *)($3, Driver.scanner.lineBuffer), $5, Driver.scanner.lineBuffer); }
	;

statement_list
	: statement {	
	AST_node ret = new AST_statement_list($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| statement_list statement {	
	AST_node ret = new AST_statement_list($1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

selection_statement
	: IF OPEN_PAREN expression CLOSE_PAREN statement {	
	AST_node ret = new AST_selection_statement("if",$3,$5,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| IF OPEN_PAREN expression CLOSE_PAREN statement ELSE statement {	
	AST_node ret = new AST_selection_statement("if-else",$3,$5,$7, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SWITCH OPEN_PAREN expression CLOSE_PAREN statement {	
	AST_node ret = new AST_selection_statement("switch",$3,$5,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

iteration_statement
	: WHILE OPEN_PAREN expression CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("while",$3,$5,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| DO statement WHILE OPEN_PAREN expression CLOSE_PAREN SEMI {	
	AST_node ret = new AST_iteration_statement("do-while",$2,$5,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN SEMI SEMI CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for1",$6,null,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN SEMI SEMI expression CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for2",$5,$7,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN SEMI expression SEMI CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for3",$4,$7,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN SEMI expression SEMI expression CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for4",$4,$6,$8,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN expression SEMI SEMI CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for5",$3,$7,null,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN expression SEMI SEMI expression CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for6",$3,$6,$8,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN expression SEMI expression SEMI CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for7",$3,$5,$8,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FOR OPEN_PAREN expression SEMI expression SEMI expression CLOSE_PAREN statement {	
	AST_node ret = new AST_iteration_statement("for8",$3,$5,$7,$9, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

jump_statement
	: GOTO identifier SEMI {	
	AST_node ret = new AST_jump_statement("goto",$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| CONTINUE SEMI {	
	AST_node ret = new AST_jump_statement("continue",null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| BREAK SEMI {	
	AST_node ret = new AST_jump_statement("break",null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| RETURN SEMI {	
	AST_node ret = new AST_jump_statement("return",null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| RETURN expression SEMI {	
	AST_node ret = new AST_jump_statement("return",$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

expression
	: assignment_expression {	
	AST_node ret = new AST_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| expression COMMA assignment_expression {	
	AST_node ret = new AST_expression($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

assignment_expression
	: conditional_expression {	
	AST_node ret = new AST_assignment_expression($1,null,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| unary_expression {
			//AST_node n = $1; 
			if($1.getClass().getName().contains("constant")) {
				Driver.scanner.decrementColumn(2);
				Driver.scanner.yyerror("Can't assign to constant value");
			} 
			} 
			assignment_operator assignment_expression {	
	AST_node ret = new AST_assignment_expression($1,$3,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

assignment_operator
	: ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| MUL_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| DIV_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| MOD_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| ADD_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SUB_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| LEFT_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| RIGHT_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| AND_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| XOR_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| OR_ASSIGN {	
	AST_node ret = new AST_assignment_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

conditional_expression
	: logical_or_expression {	
	AST_node ret = new AST_conditional_expression($1,null,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| logical_or_expression QUESTION_MARK expression COLON conditional_expression {	
	AST_node ret = new AST_conditional_expression($1,$3,$5, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

constant_expression
	: conditional_expression {	
	AST_node ret = new AST_constant_expression($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	;

logical_or_expression
	: logical_and_expression {	
	AST_node ret = new AST_logical_or_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| logical_or_expression L_OR_OP logical_and_expression {	
	AST_node ret = new AST_logical_or_expression($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

logical_and_expression
	: inclusive_or_expression {	
	AST_node ret = new AST_logical_and_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| logical_and_expression L_AND_OP inclusive_or_expression {	
	AST_node ret = new AST_logical_and_expression($1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

inclusive_or_expression
	: exclusive_or_expression {	
	AST_node ret = new AST_inclusive_or_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| inclusive_or_expression OR_OP exclusive_or_expression {	
	AST_node ret = new AST_inclusive_or_expression($1,$3, Driver.scanner.lineBuffer);
	
	String argType1 = $1.getClassType().toLowerCase();
	String argType2 = $3.getClassType().toLowerCase();
	
	if(argType1.contains("double") || argType2.contains("double") || argType1.contains("float") || argType2.contains("float"))
		Driver.scanner.yyerror("Cannot use bitwise OR on a floating point value");
	
	$$ = ret;
	
	}
	;

exclusive_or_expression
	: and_expression {	
	AST_node ret = new AST_exclusive_or_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| exclusive_or_expression XOR_OP and_expression {	
	AST_node ret = new AST_exclusive_or_expression($1,$3, Driver.scanner.lineBuffer);
	
	String argType1 = $1.getClassType().toLowerCase();
	String argType2 = $3.getClassType().toLowerCase();
	
	if(argType1.contains("double") || argType2.contains("double") || argType1.contains("float") || argType2.contains("float"))
		Driver.scanner.yyerror("Cannot use bitwise XOR on a floating point value");
	
	$$ = ret;
	
	}
	;

and_expression
	: equality_expression {	
	AST_node ret = new AST_and_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| and_expression AND_OP equality_expression {	
	AST_node ret = new AST_and_expression($1,$3, Driver.scanner.lineBuffer);
	
	String argType1 = $1.getClassType().toLowerCase();
	String argType2 = $3.getClassType().toLowerCase();
	
	if(argType1.contains("double") || argType2.contains("double") || argType1.contains("float") || argType2.contains("float"))
		Driver.scanner.yyerror("Cannot use bitwise AND on a floating point value");
	
	$$ = ret;
	
	}
	;

equality_expression
	: relational_expression {	
	AST_node ret = new AST_equality_expression("",$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| equality_expression EQ_OP relational_expression {	
	AST_node ret = new AST_equality_expression("==",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| equality_expression NE_OP relational_expression {	
	AST_node ret = new AST_equality_expression("!=",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

relational_expression
	: shift_expression {	
	AST_node ret = new AST_relational_expression("",$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| relational_expression LT_OP shift_expression {	
	AST_node ret = new AST_relational_expression("<",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| relational_expression GT_OP shift_expression {	
	AST_node ret = new AST_relational_expression(">",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| relational_expression LE_OP shift_expression {	
	AST_node ret = new AST_relational_expression("<=",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| relational_expression GE_OP shift_expression {	
	AST_node ret = new AST_relational_expression(">=",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

shift_expression
	: additive_expression {	
	AST_node ret = new AST_shift_expression("",$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| shift_expression LEFT_OP additive_expression {	
	AST_node ret = new AST_shift_expression("<<",$1,$3, Driver.scanner.lineBuffer);
	
	//System.out.println($1.getData().getClass().getName());
	String argType1 = $1.getClassType().toLowerCase();
	String argType2 = $3.getClassType().toLowerCase();
	
	if(argType1.contains("double") || argType2.contains("double") || argType1.contains("float") || argType2.contains("float"))
		Driver.scanner.yyerror("Cannot Shift a floating point value");
	
	$$ = ret;
	
	}
	| shift_expression RIGHT_OP additive_expression {	
	AST_node ret = new AST_shift_expression(">>",$1,$3, Driver.scanner.lineBuffer);
	
	String argType1 = $1.getClassType().toLowerCase();
	String argType2 = $3.getClassType().toLowerCase();
	
	if(argType1.contains("double") || argType2.contains("double") || argType1.contains("float") || argType2.contains("float"))
		Driver.scanner.yyerror("Cannot Shift a floating point value");
	
	$$ = ret;
	
	}
	;

additive_expression
	: multiplicative_expression {	
	AST_node ret = new AST_additive_expression('\0',$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| additive_expression ADD_OP multiplicative_expression {	
	
	AST_node ret = new AST_additive_expression('+',$1,$3, Driver.scanner.lineBuffer);
/*	
	Object arg1 = $1.getData();
	Object arg2 = $3.getData();
	
	if(arg1 == null || arg2 == null) 
		ret = new AST_additive_expression('+',$1,$3, Driver.scanner.lineBuffer);
		
		
	else {
		//System.out.println(arg1);
		//System.out.println(arg2);
	
		if(arg1.getClass().equals(arg2.getClass()))
			ret = new AST_additive_expression('+',$1,$3, Driver.scanner.lineBuffer);
		
		else
			ret = new AST_additive_expression('+',new AST_int_to_float($1, Driver.scanner.lineBuffer),$3, Driver.scanner.lineBuffer);
	}
	*/
	$$ = ret;
	
	}
	| additive_expression SUB_OP multiplicative_expression {	
	AST_node ret = new AST_additive_expression('-',$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

multiplicative_expression
	: cast_expression {	
	AST_node ret = new AST_multiplicative_expression('\0',$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| multiplicative_expression MUL_OP cast_expression {	
	AST_node ret = new AST_multiplicative_expression('*',$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| multiplicative_expression DIV_OP cast_expression {	
	AST_node ret = new AST_multiplicative_expression('/',$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| multiplicative_expression MOD_OP cast_expression {	
	AST_node ret = new AST_multiplicative_expression('%',$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

cast_expression
	: unary_expression {	
	AST_node ret = new AST_cast_expression($1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| OPEN_PAREN type_name CLOSE_PAREN cast_expression {	
	AST_node ret = new AST_cast_expression($2,$4, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

unary_expression
	: postfix_expression {	
	AST_node ret = new AST_unary_expression("",$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| INC_OP unary_expression {	
	AST_node ret = new AST_unary_expression("++",$2,null, Driver.scanner.lineBuffer);
	
	String type = $2.getClassType().toLowerCase();
	
	if(type.contains("float") || type.contains("double"))
		Driver.scanner.yywarning("using increment operator on floating point type");
	
	$$ = ret;
	
	}
	| DEC_OP unary_expression {	
	AST_node ret = new AST_unary_expression("--",$2,null, Driver.scanner.lineBuffer);
	
	String type = $2.getClassType().toLowerCase();
	
	if(type.contains("float") || type.contains("double"))
		Driver.scanner.yywarning("using decrement operator on floating point type");
	
	$$ = ret;
	
	}
	| unary_operator cast_expression {	
	AST_node ret = new AST_unary_expression("",$1,$2, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SIZEOF unary_expression {	
	AST_node ret = new AST_unary_expression("sizeof",$2,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SIZEOF OPEN_PAREN type_name CLOSE_PAREN {	
	AST_node ret = new AST_unary_expression("sizeof",$3,null, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

unary_operator
	: AND_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| MUL_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| ADD_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| SUB_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;	

	}
	| BIT_NOT_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| NOT_OP {	
	AST_node ret = new AST_unary_operator(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

postfix_expression
	: primary_expression {	
	AST_node ret = new AST_postfix_expression("",$1,null, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| postfix_expression OPEN_BRACKET expression CLOSE_BRACKET {	
	AST_node ret = new AST_postfix_expression("[]",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| postfix_expression OPEN_PAREN CLOSE_PAREN {	// FUNCTION CALL
	AST_node ret = new AST_postfix_expression("{}",$1,null, Driver.scanner.lineBuffer);
	
	SymbolNode symbol = (Driver.st.find((String)$1.getData())).node;
	
	if(!symbol.type.getClass().getName().toLowerCase().contains("function"))
		Driver.scanner.yyerror(String.format("%s is not a function!", (String)$1.getData()));
		
	int params = ((FunctionDataType) symbol.type).numParameters;
	
	if(params != currentParameterCount)
		Driver.scanner.yyerror("number of parameters do not match; expected " + 
							String.valueOf(params) + " -- found " + currentParameterCount);
	
	currentParameterCount = 0;
		
	$$ = ret;
	
	}
	| postfix_expression OPEN_PAREN argument_expression_list CLOSE_PAREN {	// FUNCTION CALL
	AST_node ret = new AST_postfix_expression("{arg}",$1,$3, Driver.scanner.lineBuffer);
	
	SymbolNode symbol = (Driver.st.find((String)$1.getData())).node;
	
	if(!symbol.type.getClass().getName().toLowerCase().contains("function"))
		Driver.scanner.yyerror(String.format("%s is not a function!", (String)$1.getData()));
		
	int params = ((FunctionDataType) symbol.type).numParameters;
	
	if(params != currentParameterCount)
		Driver.scanner.yyerror("number of parameters do not match; expected " + 
							String.valueOf(params) + " -- found " + currentParameterCount);
		
		
	// check types
	FunctionDataType functionType = (FunctionDataType) symbol.type;
	boolean coersion = false;
	
	for(int i = 0; i < functionType.numParameters; i++)
	{
		BasicDataType[] t = functionType.parameterTypes;
		
		if(t[i].type.equals("float") && typeList.get(i).type.equals("double")) {			
			Driver.scanner.yywarning("implicit cast from double to float in function call");
			coersion = true;
		}
		
		else if(t[i].type.equals("double") && typeList.get(i).type.equals("float"))
		{
			Driver.scanner.yywarning("implicit cast from float to double in function call");
			coersion = true;
		}
		
		if(!t[i].equals(typeList.get(i)) && !coersion)
			Driver.scanner.yyerror("parameter types do no match; expected " + t[i].type + " -- found " + typeList.get(i).type);
	}
	
	
	currentParameterCount = 0;
	typeList.clear();
	
	$$ = ret;
	
	}
	| postfix_expression PERIOD identifier {	
	AST_node ret = new AST_postfix_expression(".",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| postfix_expression PTR_OP identifier {	
	AST_node ret = new AST_postfix_expression("->",$1,$3, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| postfix_expression INC_OP {	
	AST_node ret = new AST_postfix_expression("++",$1,null, Driver.scanner.lineBuffer);
	
	String type = $1.getClassType().toLowerCase();
	
	if(type.contains("float") || type.contains("double"))
		Driver.scanner.yywarning("using increment operator on floating point type");
	
	$$ = ret;
	
	}
	| postfix_expression DEC_OP {	
	AST_node ret = new AST_postfix_expression("--",$1,null, Driver.scanner.lineBuffer);
	
	String type = $1.getClassType().toLowerCase();
	
	if(type.contains("float") || type.contains("double"))
		Driver.scanner.yywarning("using decrement operator on floating point type");
	
	$$ = ret;
	
	}
	;

primary_expression
	: identifier {	
	AST_node ret = new AST_primary_expression($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| constant {	
	AST_node ret = new AST_primary_expression($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| string {	
	AST_node ret = new AST_primary_expression($1, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| OPEN_PAREN expression CLOSE_PAREN {	
	AST_node ret = new AST_primary_expression($2, Driver.scanner.lineBuffer);
	
	if(Driver.reduceAST)
		$$ = $2;
	
	else	
		$$ = ret;
	
	}
	;

argument_expression_list
	: assignment_expression {	
	AST_node ret = new AST_argument_expression_list($1,null, Driver.scanner.lineBuffer);
	
	//parameterCount++;
	currentParameterCount++;
	
	String s;
	
	if($1.getClass().getName().contains("identifier"))
	{
		s = ((AST_identifier) $1).symbol_node.type.getType();
	}
	
	else
	{
		s = $1.getData().getClass().getName().toLowerCase();
	
		if(s.contains("integer"))
			s = "int";
		
		else if(s.contains("float"))
			s = "float";
		
		else if(s.contains("double"))
			s = "double";
		
		else if(s.contains("char"))
			s = "char";
		
		else if(s.contains("long"))
			s = "long";
	}

	typeList.add(new BasicDataType(s));
	
	if(Driver.reduceAST)
		$$ = $1;
	
	else	
		$$ = ret;
	
	}
	| argument_expression_list COMMA assignment_expression {	
	AST_node ret = new AST_argument_expression_list($1,$3, Driver.scanner.lineBuffer);
	
	//parameterCount++;
	currentParameterCount++;
	
	String s;
	
	if($3.getClass().getName().contains("identifier"))
	{
		s = ((AST_identifier) $3).symbol_node.type.getType();
	}
	
	else
	{
		s = $3.getData().getClass().getName().toLowerCase();
	
		if(s.contains("integer"))
			s = "int";
		
		else if(s.contains("float"))
			s = "float";
		
		else if(s.contains("double"))
			s = "double";
		
		else if(s.contains("char"))
			s = "char";
		
		else if(s.contains("long"))
			s = "long";
	}

	typeList.add(new BasicDataType(s));
	
	$$ = ret;
	
	}
	;

constant
	: INTEGER_CONSTANT {	
	AST_node ret = new AST_constant(Driver.scanner.getLVal(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| CHARACTER_CONSTANT {	
	AST_node ret = new AST_constant(Driver.scanner.getLVal(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| FLOATING_CONSTANT {	
	AST_node ret = new AST_constant(Driver.scanner.getLVal(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	| ENUMERATION_CONSTANT {	
	AST_node ret = new AST_constant(Driver.scanner.yytext(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

string
	: STRING_LITERAL {	
	String sBuffer = Driver.scanner.yytext().substring(1,Driver.scanner.yytext().length()-1);
	AST_node ret = new AST_string(sBuffer, Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;

identifier
	: IDENTIFIER {	
	AST_node ret = new AST_identifier(Driver.scanner.yytext(), (SymbolNode)Driver.scanner.getLVal(), Driver.scanner.lineBuffer);
	
	$$ = ret;
	
	}
	;
%%


