
package Compiler;

import java.util.LinkedList;
import java.io.*;

/**
	Abstract Syntax Tree.  Contains the root and methods for outputing the tree's state.
*/
public class Ast
{
	/** Root node.								*/
	protected AST_node root;
	/** Reference to Symbol Table.				*/
	protected SymbolTable st = Driver.st;
	/** Used in writeDotFile function.			*/
	protected int nodeCount;
	/** Counter used for integer labels			*/
	public static int integerLabelCounter;
	/** Counter used for float labels			*/
	public static int floatLabelCounter;
	/** Counter used for counting labels		*/
	public static int labelCounter;
	/** Counter used for string labels			*/
	public static int stringLabelCounter;
	/** Counter used for character labels		*/
	public static int characterLabelCounter;
	
	/**
	*	Default Constructor.  Sets root to null.
	*/
	public Ast()
	{
		root = null;
	}
	
	/**
	* Constructor that sets root to the passed node.
	* @param node Node for root to be set to.
	*/
	public Ast(AST_node node)
	{
		root = node;
	}
	
	/**
	* Set root to new value.
	* @param newNode New Node for root to be set to.
	*/
	public void setRoot(AST_node newNode)
	{
		root = newNode;
	}
	
	/**
	* Function to print the tree.
	*/
	public void print()
	{
		printNode(root);
	}
	
	/**
	* Helper function for recursive printing.
	* @param node The node to print.
	*/
	private void printNode(AST_node node)
	{
		if(node == null)
			return;
			
		node.print();
		
		for(AST_node n : node.nodes)
			printNode(n);
	}
	
	/**
	* Function to output a graphviz dot file.  This will allow the tree to be visualized as an image.
	* @param filename Name of output file.
	*/
	public void writeDotFile(String filename)
	{
		PrintWriter fout;
		
		try
		{
			fout = new PrintWriter(filename);
			
			nodeCount = 0;
			
			fout.println("strict digraph AST {");
			fout.println("\tstart_node [shape=none label=\"\"];\n" + 
				"\tstart_node -> node1;");
			
			writeDotFileNode(null,root,fout,0);
			
			fout.println("}\n");
			
			fout.close();
		}
		
		catch(IOException e)
		{
			System.err.println("Critical IO Failure in Ast::writeDotFile");
			System.exit(1);
		}
		
		if(Driver.createImage)
		{
			try
			{
				String outFile = filename.substring(0,filename.lastIndexOf(".")) + ".png";
				Runtime.getRuntime().exec("dot -Tpng " + filename + " -o " + outFile);
			}
			
			catch(IOException e)
			{
				System.err.println("Critical error in image creation within Ast");
				System.exit(1);
			}
		}
	}
	
	/**
	* Helper function for writing the dot file.
	* @throws IOException
	*/
	private void writeDotFileNode(AST_node parent, AST_node node, PrintWriter fout, int parentCount) throws IOException
	{
		if(node != null)
		{
			nodeCount++;
			
			fout.println("\tnode" + String.valueOf(nodeCount) + " [label = \"" + node.toString() + "\"];");
			
			if(parent != null)
				fout.println("\tnode" + String.valueOf(parentCount) + " -> node" + String.valueOf(nodeCount) + ";");
				
			int count = nodeCount;
			
			for(AST_node n : node.nodes)
				writeDotFileNode(node,n,fout,count);

		}
	}
	
	/**
	* Public function for outputting 3 address code.
	* @param filename Name of file to write 3 address code to.
	*/
	public void write3AC(String filename)
	{
		// init counters
		integerLabelCounter = floatLabelCounter = stringLabelCounter = labelCounter = characterLabelCounter = 0;
		
		if(root == null)
			return;
			
		try
		{
			PrintWriter fout = new PrintWriter(filename);
			
			fout.println("# Program Start\n");
			
			root.gen3AC(fout);
			
			fout.close();
		}
		catch(IOException e)
		{
			System.err.println("Critical IO Failure in Ast::write3AC");
			System.exit(1);
		}
	}
	
	/**
	* Calculates how many nodes are in the tree.
	* @return Size of the tree.
	*/
	public int size()
	{
		nodeCount = 0;
		sizeRecursion(root);
		return nodeCount;
	}
	
	/**
	* Helper function for counting tree size.
	* @see #size()
	*/
	private void sizeRecursion(AST_node node)
	{
		if(node != null)
		{
			nodeCount++;
			
			for(AST_node n : node.nodes)
				sizeRecursion(n);
		}
	}
}

/**
	Base Class for all AST Nodes
*/
class AST_node
{
	/** Linked List containing all children nodes.						*/
	public LinkedList<AST_node> nodes = new LinkedList<AST_node>();
	/** Name of the node.  (ie Node Type)								*/
	public String name;
	/** Original Line of code node defined on							*/
	public String currentLine;
	
	/**
	* Will be used to create the 3 address code (3AC) for this node.
	*/
	public String gen3AC(PrintWriter fout) throws IOException
	{
		if(nodes.size() == 1 && nodes.get(0) != null)
			return nodes.get(0).gen3AC(fout);
			
		else
			for(AST_node n : nodes)
				if(n != null)
					n.gen3AC(fout);
		
		return null;
	}
	
	/**
	* Used for identifier node.
	*/
	public String gen3AC(PrintWriter fout, Action action) throws IOException
	{
		return gen3AC(fout);
	}

	/**
	* Base function used to print output.
	*/
	public void print()
	{
		System.out.format("%20s %50s\n", name,currentLine);
	}
	
	/**
	* Returns a String representation of this node.
	* @return Basic string representation of this node.
	*/
	@Override 
	public String toString()
	{
		return name;
	}
	
	public Object getData()
	{
		if(nodes.size() == 1)
			return nodes.get(0).getData();
			
		else
			return null;
	}
	
	/**
	* Adds a child node to this node.
	* @param node Node to be added to the list of nodes.
	*/
	public void addNode(AST_node node)
	{
		nodes.add(node);
	}
	
	/**
	* Replaces a child node with new node.
	* @param node New node to insert.
	* @param index Index to specify which node to replace.
	*/
	public void replaceNode(AST_node node, int index)
	{
		if(index < nodes.size() && index > 0)
			nodes.set(index,node);
			
		else {
			System.err.println("Out of Bounds in AST_node::replaceNode");
			System.exit(1);
		}
	}
	
	public String getClassType()
	{
		return getClass().getName();
	}
	
	/**
	* Used to append values to node strings.  (used for printing purposes)
	* @param n First Line of new String.
	* @param value Second Line of new String.
	* @return New String with appended value.
	*/
	public static String appendValue(String n, String value)
	{	
		String ret = n + "\\n" + value;
		
		return ret;
	}
	
	/**
	* Appends character value to string.
	* @param n First line of new String.
	* @param value Character to be added as second line of the string.
	* @see #appendValue(String,String)
	*/
	public static String appendValue(String n, char value)
	{
		if(value != '\0')
			return appendValue(n,String.valueOf(value));
			
		else
			return appendValue(n,"");
	}
	
	public static void output3AC(PrintWriter fout, String operand, String arg1, String arg2, String arg3, String comment) 
		throws IOException
	{
		if(comment != null && !comment.equals(""))
			comment = "# " + comment;
			
		fout.format("\t%-12s %-12s %-12s %-20s %-30s%n", operand, arg1, arg2, arg3, comment);
	}
	
	public static void output3ACLabel(PrintWriter fout, String label) throws IOException
	{
		fout.println(label + ":");
	}
	
	public static void output3ACComment(PrintWriter fout, String comment) throws IOException
	{
		fout.println("# " + comment);
	}
	
	public enum Action {
		STORE, LOAD, ALLOC, NONE
	}
}

/**
* AST Node for representing a translation unit.
*/
class AST_translation_unit extends AST_node
{
	public
	AST_translation_unit(AST_node arg1, AST_node arg2, String line) 
	{
		currentLine = line.trim();
		name = "Translation Unit";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing an external declaration.
*/
class AST_external_declaration extends AST_node
{
	public
	AST_external_declaration(AST_node arg1, String line) 
	{
		name = "External Declaration";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing a function definition.
*/
class AST_function_definition extends AST_node
{
	public
	AST_function_definition(AST_node arg1, AST_node arg2, AST_node arg3, AST_node arg4, String line, int size) 
	{
		name = "Function Definition";
		currentLine = line.trim();
		stackSize = size;
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
		nodes.add(arg4);
	}
	
	public
	AST_function_definition(AST_node arg1, AST_node arg2, AST_node arg3, AST_node arg4, String line, int size, SymbolNode node)
	{
		this(arg1,arg2,arg3,arg4,line,size);
		func_node = node;
	} 

	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String name = null;
		int numParams;
		
		//System.out.println(nodes.get(1).getClassType());
		if(nodes.get(1).getClass().getName().contains("identifier")) {
			name = ((AST_identifier) nodes.get(1)).identifier;
			//numParams = ((FunctionDataType) ((AST_identifier) nodes.get(1)).symbol_node.type).numParameters;
			numParams = func_node.type.getParamCount();//((AST_identifier) nodes.get(1)).symbol_node.type.getParamCount();
			
			//System.out.println("Class1: " + ((AST_identifier) nodes.get(1)).symbol_node.type.getClass().getName());
		}
			
		else {
			name = ((AST_identifier) nodes.get(1).nodes.get(0)).identifier;
			//numParams = ((FunctionDataType) ((AST_identifier) nodes.get(1).nodes.get(0)).symbol_node.type).numParameters;
			numParams = func_node.type.getParamCount();//((AST_identifier) nodes.get(1).nodes.get(0)).symbol_node.type.getParamCount();
			
			//System.out.println("Class: " + ((AST_identifier) nodes.get(1).nodes.get(0)).symbol_node.type.getClass().getName());
		}
		
		output3ACComment(fout, "begin function: " + name);
		output3AC(fout, "FUNC_BEGIN",name,"","",currentLine);
		output3ACLabel(fout,name);
		output3AC(fout, "ALLOC_FRAME", String.valueOf(stackSize),"","","");
		output3AC(fout, "NUM_PARAMS", String.valueOf(numParams), "","","");
			
		nodes.get(2).gen3AC(fout);
		
		output3AC(fout,"FUNC_END", name,"","",currentLine);
		output3ACComment(fout, "end function: " + name + "\n");
			
		return null;
	}
	
	public int stackSize;
	public SymbolNode func_node;
}

/**
* AST Node for representing a declaration.
*/
class AST_declaration extends AST_node
{
	public
	AST_declaration(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Declaration";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		nodes.get(1).gen3AC(fout, Action.NONE);
		
		return null;
	}
}

/**
* AST Node for representing a declaration list.
*/
class AST_declaration_list extends AST_node
{
	public
	AST_declaration_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Declaration List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing declaration specifiers.
*/
class AST_declaration_specifiers extends AST_node
{
	public
	AST_declaration_specifiers(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Declaration Specifiers";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a storage class specifier.
*/
class AST_storage_class_specifier extends AST_node
{
	public
	AST_storage_class_specifier(String _type, String line)
	{
		name = "Storage Class Specifier";
		currentLine = line.trim();
		type = _type;
	}
	
	@Override public String toString() {return appendValue(name,type);}
	
	public String type;
}

/**
* AST Node for representing a type specifier.
*/
class AST_type_specifier extends AST_node
{
	public
	AST_type_specifier(String _type, AST_node arg1, String line)
	{
		name = "Type Specifier";
		currentLine = line.trim();
		type = _type;
		
		nodes.add(arg1);
	}
	
	@Override public String toString() {return appendValue(name,type);}
	
	@Override public Object getData() {return type;}
	
	public String type;
}

/**
* AST Node for representing a type qualifier.
*/
class AST_type_qualifier extends AST_node
{
	public
	AST_type_qualifier(String _type, String line)
	{
		name = "Type Qualifier";
		currentLine = line.trim();
		type = _type;
	}
	
	@Override public String toString() {return appendValue(name,type);}
	
	public String type;
}

/**
* AST Node for representing a struct or union specifier.
*/
class AST_struct_or_union_specifier extends AST_node
{
	public
	AST_struct_or_union_specifier(AST_node arg1, AST_node arg2, AST_node arg3, String line) 
	
	{
		name = "Struct or Union Specifier";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
	}
}

/**
* AST Node for representing a struct or union.
*/
class AST_struct_or_union extends AST_node
{
	public
	AST_struct_or_union(String _type, String line) 
	{
		name = "Struct or Union";
		currentLine = line.trim();
		type = _type;
	}
	
	@Override public String toString() {return appendValue(name,type);}
	public String type;
}

/**
* AST Node for representing a struct declaration list.
*/
class AST_struct_declaration_list extends AST_node
{
	public
	AST_struct_declaration_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Struct Declaration List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing an init declarator list.
*/
class AST_init_declarator_list extends AST_node
{
	public
	AST_init_declarator_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Init Declarator List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing an init declarator. 
*/
class AST_init_declarator extends AST_node
{
	public
	AST_init_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Init Declarator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg = nodes.get(0).gen3AC(fout, Action.NONE);
		
		String val = nodes.get(1).gen3AC(fout);
		
		output3AC(fout,"M_STORE", reg, val,"",currentLine);
		
		return null;
	}
}

/**
* AST Node for representing a struct declaration.
*/
class AST_struct_declaration extends AST_node
{
	public
	AST_struct_declaration(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Struct Declaration";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a specifier qualifier list.
*/
class AST_specifier_qualifier_list extends AST_node
{
	public
	AST_specifier_qualifier_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Specifier Qualifier List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a struct declarator list.
*/
class AST_struct_declarator_list extends AST_node
{
	public
	AST_struct_declarator_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Struct Declarator List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a struct declarator.
*/
class AST_struct_declarator extends AST_node
{
	public
	AST_struct_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Struct Declarator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a enum specifier.
*/
class AST_enum_specifier extends AST_node
{
	public
	AST_enum_specifier(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Enum Specifier";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a enumerator list.
*/
class AST_enumerator_list extends AST_node
{
	public
	AST_enumerator_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Enumerator List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a enumerator.
*/
class AST_enumerator extends AST_node
{
	public
	AST_enumerator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Enumerator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a declarator.
*/
class AST_declarator extends AST_node
{
	public
	AST_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Declarator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		if(nodes.get(1) == null)
			return nodes.get(0).gen3AC(fout);
			
		else
			return nodes.get(1).gen3AC(fout);
	}
}

/**
* AST Node for representing a direct declarator.
*/
class AST_direct_declarator extends AST_node
{
	public
	AST_direct_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Direct Declarator";
		currentLine = line.trim();
		type = "";
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	public
	AST_direct_declarator(String _type, AST_node arg1, AST_node arg2, String line)
	{
		this(arg1,arg2,line);
		type = _type;
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg = null;
		
		if(type.equals("[]"))
		{
			AST_node node = nodes.get(0);
			
			if(node.getClassType().contains("declarator"))
				node = node.nodes.get(0);
				
			//while(node != null && !node.getClassType().contains("identifier"))
				//node = node.nodes.get(0);
				
			reg = node.gen3AC(fout,Action.NONE);
		}
					
		else
			for(AST_node n : nodes)
				if(n != null)
					n.gen3AC(fout);
					
		return reg;
	}
	
	public String type;
}

/**
* AST Node for representing a pointer.
*/
class AST_pointer extends AST_node
{
	public
	AST_pointer(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Pointer";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a type qualifier list.
*/
class AST_type_qualifier_list extends AST_node
{
	public
	AST_type_qualifier_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Type Qualifier List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a parameter type list.
*/
class AST_parameter_type_list extends AST_node
{
	public AST_parameter_type_list(AST_node arg1, String line)
	{
		name = "Parameter Type List";
		currentLine = line.trim();
		elipsis = false;
		nodes.add(arg1);
	}
	
	public
	AST_parameter_type_list(AST_node arg1, boolean _elipsis, String line) 
	{
		name = "Parameter Type List";
		currentLine = line.trim();
		elipsis = _elipsis;
		nodes.add(arg1);
	}
	
	public boolean elipsis;
}

/**
* AST Node for representing a parameter list.
*/
class AST_parameter_list extends AST_node
{
	public
	AST_parameter_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Parameter List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a parameter declaration.
*/
class AST_parameter_declaration extends AST_node
{
	public
	AST_parameter_declaration(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Parameter Declaration";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a identifier list.
*/
class AST_identifier_list extends AST_node
{
	public
	AST_identifier_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Identifier List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a initializer.
*/
class AST_initializer extends AST_node
{
	public
	AST_initializer(AST_node arg1, String line) 
	{
		name = "Initializer";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing a initializer list.
*/
class AST_initializer_list extends AST_node
{
	public
	AST_initializer_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Initializer List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a type name.
*/
class AST_type_name extends AST_node
{
	public
	AST_type_name(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Type Name";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing an abstract declarator.
*/
class AST_abstract_declarator extends AST_node
{
	public
	AST_abstract_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Abstract Declarator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a direct abstract declarator.
*/
class AST_direct_abstract_declarator extends AST_node
{
	public
	AST_direct_abstract_declarator(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Direct Abstract Declarator";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a statement.
*/
class AST_statement extends AST_node
{
	public
	AST_statement(AST_node arg1, String line) 
	{
		name = "Statement";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing a labeled statement.
*/
class AST_labeled_statement extends AST_node
{
	public
	AST_labeled_statement(String label, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Labeled Statement";
		currentLine = line.trim();
		label_type = label;
		 
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,label_type);}
	
	String label_type;
}

/**
* AST Node for representing a expression statement.
*/
class AST_expression_statement extends AST_node
{
	public
	AST_expression_statement(AST_node arg1, String line) 
	{
		name = "Expression Statement";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing a compound statement.
*/
class AST_compound_statement extends AST_node
{
	public
	AST_compound_statement(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Compound Statement";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a statement list.
*/
class AST_statement_list extends AST_node
{
	public
	AST_statement_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Statement List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
}

/**
* AST Node for representing a selection statement.
*/
class AST_selection_statement extends AST_node
{
	public
	AST_selection_statement(String type, AST_node arg1, AST_node arg2, AST_node arg3, String line) 
	{
		name = "Selection Statement";
		currentLine = line.trim();
		selection_type = type;
		 
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
	}
	
	@Override public String toString() {return appendValue(name,selection_type);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{	
		String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
		String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
				
		if(selection_type.equals("if"))
		{
			String expression = nodes.get(0).gen3AC(fout);
			output3AC(fout,"BNZ",expression,label1,"",currentLine);
			output3AC(fout,"BAL",label2,"","",currentLine);
			output3ACLabel(fout,label1);
			
			String statement1 = nodes.get(1).gen3AC(fout);
			
			output3ACLabel(fout,label2);
		}
		
		else if(selection_type.equals("if-else"))
		{
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			String expression = nodes.get(0).gen3AC(fout);
			output3AC(fout,"BNZ",expression,label1,"",currentLine);
			output3AC(fout,"BAL",label2,"","",currentLine);
			output3ACLabel(fout,label1);
			
			String statement1 = nodes.get(1).gen3AC(fout);
			output3AC(fout,"BAL",label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement2 = nodes.get(2).gen3AC(fout);
			output3ACLabel(fout,label3);
		}
		
		else if(selection_type.equals("switch"))
		{
			String expression = nodes.get(0).gen3AC(fout);
			
			String statement = nodes.get(1).gen3AC(fout);
		}
		return null;
	}
	
	public String selection_type;
}

/**
* AST Node for representing an iteration statement.
*/
class AST_iteration_statement extends AST_node
{
	public
	AST_iteration_statement(String loop, AST_node arg1, AST_node arg2, AST_node arg3, AST_node arg4, String line)  
	{
		name = "Iteration Statement";
		currentLine = line.trim();
		loop_type = loop;
		
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
		nodes.add(arg4);
	}
	
	@Override public String toString() {return appendValue(name,loop_type);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
	/*
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = (nodes.get(1) == null) ? "EMPTY" : nodes.get(1).gen3AC(fout);
		String reg3 = (nodes.get(2) == null) ? "EMPTY" : nodes.get(2).gen3AC(fout);
		String reg4 = (nodes.get(3) == null) ? "EMPTY" : nodes.get(3).gen3AC(fout);
	*/
		
		if(loop_type.equals("while"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String expression = nodes.get(0).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
		}
		
		else if(loop_type.equals("do-while"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String statement1 = nodes.get(0).gen3AC(fout);
			
			String expression = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
		}
		
		else if(loop_type.contains("for1"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String statement = nodes.get(0).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label2);
		}
		
		else if(loop_type.contains("for2"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String statement = nodes.get(0).gen3AC(fout);
			
			String statement2 = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label2);
			
		}
		
		else if(loop_type.contains("for3"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String expression = nodes.get(0).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
		}
		
		else if(loop_type.contains("for4"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			output3ACLabel(fout,label1);
			
			String expression = nodes.get(0).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement = nodes.get(2).gen3AC(fout);
			
			String statement2 = nodes.get(1).gen3AC(fout);
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
		}		
		
		else if(loop_type.contains("for5"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			String expression = nodes.get(0).gen3AC(fout);
			output3ACLabel(fout,label1);
			
			String statement = nodes.get(1).gen3AC(fout);
			output3AC(fout,"BAL", label1,"","",currentLine);
			
			output3ACLabel(fout,label2);
		}
		
		else if(loop_type.contains("for6"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			String expression = nodes.get(0).gen3AC(fout);
			output3ACLabel(fout,label1);
			
			String statement = nodes.get(2).gen3AC(fout);
			
			String statement2 = nodes.get(1).gen3AC(fout);
			output3AC(fout,"BAL", label1,"","",currentLine);
			
			output3ACLabel(fout,label2);
		}
		
		else if(loop_type.contains("for7"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			String statement1 = nodes.get(0).gen3AC(fout);
			output3ACLabel(fout,label1);
			
			String expression = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement = nodes.get(2).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
		}
		
		else if(loop_type.contains("for8"))
		{
			String label1 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label2 = "LABEL" + String.valueOf(Ast.labelCounter++);
			String label3 = "LABEL" + String.valueOf(Ast.labelCounter++);
			
			String statement1 = nodes.get(0).gen3AC(fout);
			output3ACLabel(fout,label1);
			
			String expression = nodes.get(1).gen3AC(fout);
			
			output3AC(fout,"BNZ", expression, label2,"",currentLine);
			output3AC(fout,"BAL", label3,"","",currentLine);
			output3ACLabel(fout,label2);
			
			String statement2 = nodes.get(3).gen3AC(fout);
			
			String statement3 = nodes.get(2).gen3AC(fout);
			
			output3AC(fout,"BAL",label1,"","",currentLine);
			output3ACLabel(fout,label3);
			
			System.out.println("Here!!!");
		}
				
		return null;
	}
	public String loop_type;
}

/**
* AST Node for representing a jump statement.
*/
class AST_jump_statement extends AST_node
{
	public
	AST_jump_statement(String op, AST_node arg1, String line)
	{
		name = "Jump Statement";
		currentLine = line.trim();
		jump_op = op;
		
		nodes.add(arg1);
	}
	
	@Override public String toString() {return appendValue(name,jump_op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = (nodes.get(0) == null) ? "EMPTY" : nodes.get(0).gen3AC(fout);
		
		if(jump_op.equals("goto"))
			output3AC(fout,"JUMP", reg1, "", "", currentLine);
		
		else if(jump_op.equals("continue"))
			output3AC(fout,"CONTINUE", "","","", currentLine);
		
		else if(jump_op.equals("break"))
			output3AC(fout,"BREAK","","","", currentLine);
			
		else if(jump_op.equals("return"))
		{
			if(reg1.equals("EMPTY"))
				output3AC(fout,"RETURN", "","","", currentLine);
				
			else {
				String reg2 = (reg1.charAt(0) == 'f') ? "f_ret" : "i_ret";
				
				output3AC(fout,"MOV", reg2, reg1, "", currentLine);
				output3AC(fout,"RETURN", reg2, "","", currentLine);
			}
		}
		
		return null;
	}
	
	public String jump_op;
}

/**
* AST Node for representing a expression.
*/
class AST_expression extends AST_node
{
	public
	AST_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
/*
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		
		
	}
*/
}

/**
* AST Node for representing an assignment expression.
*/
class AST_assignment_expression extends AST_node
{
	public
	AST_assignment_expression(AST_node arg1, AST_node arg2, AST_node arg3, String line) 
	{
		name = "Assignment Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		//String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(2).gen3AC(fout);
		String op = nodes.get(1).gen3AC(fout);
		String reg3 = null;
		String reg4 = null;
		String offset;
		short flag = 0;
		
		//AST_identifier id = (AST_identifier) nodes.get(0);
		if(nodes.get(0).getClass().getName().toLowerCase().contains("postfix"))
			offset = nodes.get(0).gen3AC(fout);
			
		else
			offset = ((AST_identifier) nodes.get(0)).gen3AC(fout,Action.NONE);
		
		if(op.equals("=")) {
			String reg = reg2;
			
			if(reg2.contains("off")) {
				reg = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				output3AC(fout,"M_LOAD", reg, reg2, "", currentLine);
			}
			
			output3AC(fout,"M_STORE", offset, reg, "", currentLine);
			return null;
		}
		
		String reg1 = nodes.get(0).gen3AC(fout);
		
		if(reg1.contains("i"))
		{
			reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			reg4 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			
			flag = 1;
			
		}
			
		else if(reg1.contains("f"))
		{
			reg3 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			reg4 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			
			flag = 2;
		}
			
		else if(reg1.contains("c"))
		{
			reg3 = "c_temp" + String.valueOf(Ast.characterLabelCounter++);
			reg4 = "c_temp" + String.valueOf(Ast.characterLabelCounter++);
			
			flag = 3;
		}
		
		else if(reg1.contains("s"))
		{
			reg3 = "s_temp" + String.valueOf(Ast.stringLabelCounter++);
		}
			
		if(op.equals("*=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"MUL_I", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					output3AC(fout,"MUL_F", reg4, reg1, reg2, currentLine);
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("/=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"DIV_I", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					output3AC(fout,"DIV_F", reg4, reg1, reg2, currentLine);
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("%=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"MOD_I", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("+=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"ADD_I", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					output3AC(fout,"ADD_F", reg4, reg1, reg2, currentLine);
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("-=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"SUB_I", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					output3AC(fout,"SUB_F", reg4, reg1, reg2, currentLine);
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("<<=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"LSHIFT", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals(">>=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"RSHIFT", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("&=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"AND", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("^=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"XOR", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
			
		else if(op.equals("|=")) {
			switch(flag)
			{
				case 1:
					output3AC(fout,"OR", reg4, reg1, reg2, currentLine);
					break;
					
				case 2:
					System.err.println("Error in Assignment Expression!!!");
					break;
					
				case 3:
					System.err.println("Can't do chars yet!");
					break;
			}
			output3AC(fout,"MOV",reg3, reg4, "", currentLine);
		}
		
		output3AC(fout,"M_STORE", offset,reg3,"",currentLine);
		return null;
	}
}

/**
* AST Node for representing an assignment operator. 
*/
class AST_assignment_operator extends AST_node
{
	public
	AST_assignment_operator(String _op, String line)
	{
		name = "Assignment Operator";
		currentLine = line.trim();
		op = _op;
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		return op;
	}
	
	@Override public Object getData() {return op;}
	
	public String op;
}

/**
* AST Node for representing a conditional expression.
*/
class AST_conditional_expression extends AST_node
{
	public
	AST_conditional_expression(AST_node arg1, AST_node arg2, AST_node arg3, String line) 
	{
		name = "Conditional Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
		nodes.add(arg3);
	}
}

/**
* AST Node for representing a constant expression.
*/
class AST_constant_expression extends AST_node
{
	public
	AST_constant_expression(AST_node arg1, String line) 
	{
		name = "Constant Expression";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing a logical or expression.
*/
class AST_logical_or_expression extends AST_node
{
	public
	AST_logical_or_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Logical OR Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		
		output3AC(fout, "L_OR", reg3, reg1, reg2, currentLine);
		
		return reg3;	
	}
}

/**
* AST Node for representing a logical and expression.
*/
class AST_logical_and_expression extends AST_node
{
	public
	AST_logical_and_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Logical AND Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		
		output3AC(fout, "L_AND", reg3, reg1, reg2, currentLine);
		
		return reg3;	
	}
}

/**
* AST Node for representing a inclusive or expression.
*/
class AST_inclusive_or_expression extends AST_node
{
	public
	AST_inclusive_or_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Inclusive OR Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		
		output3AC(fout, "OR", reg3, reg1, reg2, currentLine);
		
		return reg3;
	}
}

/**
* AST Node for representing a exclusive or expression.
*/
class AST_exclusive_or_expression extends AST_node
{
	public
	AST_exclusive_or_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Exclusive OR Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		
		output3AC(fout, "XOR", reg3, reg1, reg2, currentLine);
		
		return reg3;
	}
}

/**
* AST Node for representing an and expression.
*/
class AST_and_expression extends AST_node
{
	public
	AST_and_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "AND Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		
		output3AC(fout,"AND", reg3, reg1, reg2, currentLine);
		
		return reg3;
	}
}

/**
* AST Node for representing an equality expression.
*/
class AST_equality_expression extends AST_node
{
	public
	AST_equality_expression(String _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Equality Expression";
		currentLine = line.trim();
		
		op = _op;
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		String operand = null;
		
		if(op.equals("==")) {
			operand = "EQ";
		}
			
		else if(op.equals("!=")) {
			operand = "NE";
		}
		
		output3AC(fout,operand,reg3,reg1,reg2,currentLine);
		
		return reg3;		
	}
	
	public String op;
}

/**
* AST Node for representing a relational expression.
*/
class AST_relational_expression extends AST_node
{
	public
	AST_relational_expression(String _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Relational Expression";
		currentLine = line.trim();
		
		op = _op;
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		String operand = null;
		
		if(op.equals("<")) {
			operand = "LT";
		}
			
		else if(op.equals("<=")) {
			operand = "LE";
		}
			
		else if(op.equals(">")) {
			operand = "GT";
		}
			
		else if(op.equals(">=")) {
			operand = "GE";
		}
		
		output3AC(fout,operand,reg3,reg1,reg2,currentLine);
		
		return reg3;	
	}
	
	public String op;
}

/**
* AST Node for representing a shift expression.
*/
class AST_shift_expression extends AST_node
{
	public
	AST_shift_expression(String _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Shift Expression";
		currentLine = line.trim();
		op = _op;
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
		String operand = (op.equals("<<")) ? "LSHIFT" : "RSHIFT";
		
		output3AC(fout, operand, reg3, reg1, reg2, currentLine);
		
		return reg3;
		
	}
	
	public String op;
}

/**
* AST Node for representing a additive expression.
*/
class AST_additive_expression extends AST_node
{
	public
	AST_additive_expression(char _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Additive Expression";
		currentLine = line.trim();
		op = _op;
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String operand = (op == '+') ? "ADD" : "SUB";
		String reg3;
		boolean floatResult = false;

		System.out.println(reg1);
		System.out.println(reg2);
		
		if(reg1.contains("f_") || reg2.contains("f_"))
			floatResult = true;
			
		if(floatResult)
		{
			reg3 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			output3AC(fout, operand + "_F", reg3, reg1, reg2, currentLine);
		}
		
		else
		{
			reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			output3AC(fout, operand + "_I", reg3, reg1, reg2, currentLine);
		}
		
		return reg3;
		
	}
	
	public char op;
}

/**
* AST Node for representing a multiplicative expression.
*/
class AST_multiplicative_expression extends AST_node
{
	public
	AST_multiplicative_expression(char _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Multiplicative Expression";
		currentLine = line.trim();
		op = _op;
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		String reg2 = nodes.get(1).gen3AC(fout);
		String operand = (op == '*') ? "MUL" : "DIV";
		String reg3;
		boolean floatResult = false;
		
		if(reg1.contains("f_") || reg2.contains("f_"))
			floatResult = true;
			
		if(floatResult)
		{
			reg3 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			output3AC(fout, operand + "_F", reg3, reg1, reg2, currentLine);
		}
		
		else
		{
			reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			output3AC(fout, operand + "_I", reg3, reg1, reg2, currentLine);
		}
		
		return reg3;
		
	}
	
	public char op;
}

/**
* AST Node for representing a cast expression.
*/
class AST_cast_expression extends AST_node
{
	public
	AST_cast_expression(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Cast Expression";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String type = (String)nodes.get(0).getData();
		String reg1 = nodes.get(1).gen3AC(fout);
		String reg2;
		String operator;
		
		if(type.contains("float") || type.contains("double"))
		{
			reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			operator = "F_TO_I";
		}
			
		else
		{
			reg2 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			operator = "I_TO_F";
		}
		
		output3AC(fout, operator, reg2, reg1, "", currentLine);
		
		return reg2;		
	}
}

/**
* AST Node for representing a unary expression.
*/
class AST_unary_expression extends AST_node
{
	public
	AST_unary_expression(String _op, AST_node arg1, AST_node arg2, String line) 
	{
		name = "Unary Expression";
		currentLine = line.trim();
		op = _op;
		
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg1 = nodes.get(0).gen3AC(fout);
		//String reg2 = (nodes.get(1) == null) ? "EMPTY" : nodes.get(1).gen3AC(fout);
		String reg3;
		boolean floatType = false;
		
		if(reg1.contains("f"))
		{
			reg3 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			floatType = true;
		}
		
		else if(reg1.contains("i"))
			reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			
		else
			reg3 = "c_temp" + String.valueOf(Ast.characterLabelCounter++);
			
		if(op.equals("++")) {
			if(floatType)
				output3AC(fout,"ADD_F", reg3, reg1, "1.0", currentLine);
				
			else
				output3AC(fout,"ADD_I", reg3, reg1, "1", currentLine);
				
			String offset = ((AST_identifier) nodes.get(0)).gen3AC(fout, Action.NONE);
			
			output3AC(fout,"M_STORE", offset, reg3, "", currentLine);
		}
			
		else if(op.equals("--")) {
			if(floatType)
				output3AC(fout, "SUB_F", reg3, reg1, "1.0", currentLine);
				
			else
				output3AC(fout, "SUB_I", reg3, reg1, "1", currentLine);
				
			String offset = ((AST_identifier) nodes.get(0)).gen3AC(fout, Action.NONE);		
			output3AC(fout,"M_STORE", offset, reg3, "", currentLine);
		}
			
		else if(op.equals("sizeof")) {
			String size = "0";
			if(reg1.contains("f"))
				size = "4";
				
			else if(reg1.contains("i"))
				size = "4";
				
			else if(reg1.contains("c"))
				size = "1";
				
			output3AC(fout,"LOAD_I", reg3, size, "", currentLine);
		}
		
		return reg3;
	}
	
	public String op;
}

/**
* AST Node for representing a unary operator.
*/
class AST_unary_operator extends AST_node
{
	public
	AST_unary_operator(String _op, String line)
	{
		name = "Unary Operator";
		currentLine = line.trim();
		op = _op;
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public Object getData() {return op;}

	public String op;
}

/**
* AST Node for representing a postfix expression.
*/
class AST_postfix_expression extends AST_node
{
	public
	AST_postfix_expression(String _op, AST_node arg1, AST_node arg2, String line)
	{
		name = "Postfix Expression";
		currentLine = line.trim();
		op = _op;
		
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String toString() {return appendValue(name,op);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{	
	/*
		String reg1;
		//String reg1 = nodes.get(0).gen3AC(fout);
		//String reg2 = (nodes.get(1) == null) ? "EMPTY" : nodes.get(1).gen3AC(fout);
		String reg3;
		boolean floatType = false;
		
		AST_identifier idNode = (AST_identifier) nodes.get(0);
		
		reg1 = reg3 = idNode.gen3AC(fout);
		
		if(idNode.symbol_node.getClass().getName().toLowerCase().contains("array"))
		{
			if(idNode.symbol_node.type.toLowerCase().contains("float") || idNode.symbol_node.type.toLowerCase().contains("double"))
				floatType = true;
				
			else
				floatType = false;
				
		}
	
		else if(reg1.contains("f") || reg2.contains("f"))
		{
			reg3 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			floatType = true;
		}
		
		else
			reg3 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
	*/
		String reg1 = null, reg2 = null, func_name = null;
		String classType;
		int arraySize = 0;
		boolean floatType = false;
		
		if(!nodes.get(0).getClass().getName().toLowerCase().contains("identifier"))
			return "FIXME";
				
		AST_identifier idNode = (AST_identifier) nodes.get(0);
		
		classType = idNode.symbol_node.type.getClass().getName().toLowerCase();
		
		//System.out.println("Class Type: " + classType);
		
		if(classType.contains("array"))
		{
			reg1 = idNode.gen3AC(fout,Action.NONE);
		}
		
		else if(classType.contains("function"))
		{
			//System.out.println("Here!!");
			func_name = idNode.identifier;
		}
		
		else // identifier
		{
			reg1 = idNode.gen3AC(fout);
		}
		
		floatType = (idNode.symbol_node.type.getType().contains("float") || idNode.symbol_node.type.getType().contains("double"))
			? true : false;
		
		if(op.equals("++")) {
			if(floatType) {
				reg2 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
				
				output3AC(fout,"ADD_F", reg2, reg1, "1.0", currentLine);
			}
				
			else {
				reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				output3AC(fout,"ADD_I", reg2, reg1, "1", currentLine);
			}
			
			String offset = ((AST_identifier) nodes.get(0)).gen3AC(fout, Action.NONE);
			System.out.println("THING: " + offset);
			
			output3AC(fout,"M_STORE", offset, reg2, "", currentLine);
		}
			
		else if(op.equals("--")) {
			if(floatType) {
				reg2 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
				output3AC(fout, "SUB_F", reg2, reg1, "1.0", currentLine);
			}
				
			else {
				reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				output3AC(fout, "SUB_I", reg2, reg1, "1", currentLine);
			}
			
			String offset = ((AST_identifier) nodes.get(0)).gen3AC(fout, Action.NONE);
			System.out.println("THING: " + offset);
			
			output3AC(fout,"M_STORE", offset, reg2, "", currentLine);
		}
		
		else if(op.equals("{}"))
		{
			output3AC(fout, "CALL", func_name,"","",currentLine);
			
			if(idNode.symbol_node.type.getType().equals("void"))
				; // do nothing
				
			else if(floatType)
			{
				reg2 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
				
				output3AC(fout,"MOV", reg2, "f_ret", "", currentLine);
			}
			
			else
			{
				reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				
				output3AC(fout,"MOV", reg2, "i_ret", "", currentLine);
			}
				
			//else
				//output3AC(fout,"
		}
		
		else if(op.equals("{arg}"))
		{
			nodes.get(1).gen3AC(fout);
			
			output3AC(fout, "CALL", func_name,"","",currentLine);
			
			if(idNode.symbol_node.type.getType().equals("void"))
				; // do nothing
				
			else if(floatType)
			{
				reg2 = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
				
				output3AC(fout,"MOV", reg2, "f_ret", "", currentLine);
			}
			
			else
			{
				reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				
				output3AC(fout,"MOV", reg2, "i_ret", "", currentLine);
			}
		}
		
		else if(op.equals("[]"))
		{
			if(nodes.get(0).getClass().getName().contains("postfix") && 
					nodes.get(1).getClass().getName().contains("constant"))
			{ /*
				AST_identifier id = (AST_identifier) nodes.get(0).nodes.get(0);
				
				java.util.Scanner scanner = new java.util.Scanner(reg1);
				scanner.useDelimiter("\\(");
				int typeOffset = ((ArrayDataType)id.symbol_node.type).nestedArray.typeOffset;
				int offset = scanner.nextInt() - typeOffset;// - ((AST_identifier) nodes.get(0)).symbol_node.type.typeOffset;
				int index = Integer.parseInt(nodes.get(1).getData().toString());
				int arrayIndexOffset = index * typeOffset;
				
				System.out.println("Double Numbers: " + String.format("%d %d %d %d", typeOffset, offset, index, arrayIndexOffset));
							
				offset += arrayIndexOffset;
			
				reg2 = String.valueOf(offset) + "(off)";
				
				*/
			}
			
			else if(nodes.get(1).getClass().getName().contains("constant"))
			{
				int typeOffset = ((AST_identifier) nodes.get(0)).symbol_node.type.typeOffset;
				int offset = ((AST_identifier) nodes.get(0)).symbol_node.offset;
				int index = Integer.parseInt(nodes.get(1).getData().toString());
				int arrayIndexOffset = Integer.parseInt(nodes.get(1).getData().toString()) * typeOffset;
				
				System.out.println("Numbers: " + String.format("%d %d %d %d", typeOffset, offset, index, arrayIndexOffset));
							
				offset += arrayIndexOffset;
			
				reg2 = String.valueOf(offset) + "(off)";
			}
			
			else
			{
				String reg4 = nodes.get(1).gen3AC(fout);
				int offset = ((AST_identifier) nodes.get(0)).symbol_node.offset;
				int typeOffset = ((AST_identifier) nodes.get(0)).symbol_node.type.typeOffset;
				
				String reg5 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				String reg6 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);

				output3AC(fout,"MUL_I", reg5, reg4, String.valueOf(typeOffset), currentLine);
				output3AC(fout,"ADD_I", reg6, reg5, String.valueOf(offset), currentLine);
				
				reg2 = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
				
				output3AC(fout,"M_LOAD", reg2, reg6, "", currentLine);
				//reg2 = reg6;
				
				
			}
		}
		
		return reg2;
		
	}
	
	public String op;
}

/**
* AST Node for representing a primary expression.
*/
class AST_primary_expression extends AST_node
{
	public
	AST_primary_expression(AST_node arg1, String line) 
	{
		name = "Primary Expression";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

/**
* AST Node for representing an argument expression list.
*/
class AST_argument_expression_list extends AST_node
{
	public
	AST_argument_expression_list(AST_node arg1, AST_node arg2, String line) 
	{
		name = "Argument Expression List";
		currentLine = line.trim();
		nodes.add(arg1);
		nodes.add(arg2);
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		for(AST_node n : nodes)
			if(n != null)
			{
				if(n.getClass().getName().toLowerCase().contains("expression_list"))
					n.gen3AC(fout);
				
				else {	
					String reg = n.gen3AC(fout);
				
					output3AC(fout, "PARAM", reg, "", "", currentLine);
				}
			}
			
		return null;
	}
}

/**
* AST Node for representing a constant.
*/
class AST_constant extends AST_node
{
	public
	AST_constant(Object val, String line)
	{
		name = "Constant";
		currentLine = line.trim();
		const_val = val;
	}
	
	@Override public String toString() {return appendValue(name,const_val.toString());}
	@Override public Object getData() {return const_val;}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg;
		
		String type = (const_val == null) ? "ERROR" : const_val.getClass().getName();
		
		if(type.contains("Double") || type.contains("Float")) 
		{
			reg = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			
			output3AC(fout,"LOAD_F", reg, const_val.toString(), "", currentLine);
			//fout.format("%-10s %-12s %-12s %-12s %30s%n", "LOAD_F", reg, const_val.toString(), "", "# " + currentLine);
		}
			
		else if(type.contains("Integer"))
		{
			reg = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			
			output3AC(fout,"LOAD_I", reg, const_val.toString(), "", currentLine);
			//fout.format("%-10s %-12s %-12s %-12s %30s%n", "LOAD_F", reg, const_val.toString(), "", "# " + currentLine);
		}
			
			
			
		else
		{
			reg = "c_temp" + String.valueOf(Ast.characterLabelCounter++);
			output3AC(fout,"LOAD_C", reg, "'" + const_val.toString() + "'", "", currentLine);
		}
			
		return reg;
	}
	
	@Override public String getClassType()
	{
		return const_val.getClass().getName();
	}
	
	public Object const_val;
}

/**
* AST Node for representing a string.
*/
class AST_string extends AST_node
{
	public
	AST_string(String literal, String line)
	{
		name = "String Literal";
		currentLine = line.trim();
		str_literal = literal;
	}
	
	@Override public String toString() {return appendValue(name,str_literal);}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{
		String reg = "s_temp" + String.valueOf(Ast.stringLabelCounter++);
		
		output3AC(fout,"LOAD_S",reg, "\"" + str_literal + "\"","",currentLine);
		//fout.format("%-10s %-12s %-12s %-12s %30s%n", "LOAD_S", reg, str_literal, "", "# " + currentLine);
		
		return reg;
	}
	
	@Override public String getClassType()
	{
		return str_literal.getClass().getName();
	}
	
	public String str_literal;
}

/**
* AST Node for representing an identifier. 
*/
class AST_identifier extends AST_node
{
	public
	AST_identifier(String identifier_name, SymbolNode node, String line)
	{
		name = "Identifier";
		currentLine = line.trim();
		identifier = identifier_name;
		symbol_node = node;
	}
	
	@Override public String toString() {return appendValue(name,identifier);}
	
	@Override public String gen3AC(PrintWriter fout, Action action) throws IOException
	{
		String type = symbol_node.toString();
		String offset = String.valueOf(symbol_node.offset);
		String reg = null;
/*	
		if(action ==  || symbol_node.type.getClass().getName().contains("Array")) {
			reg = offset + "(off)";
			return reg;
		}
*/		
		switch(action)
		{
			case LOAD:		
				if(type.equals("int") || type.equals("short") || type.equals("long") || type.equals("long long"))
					reg = "i_temp" + String.valueOf(Ast.integerLabelCounter++);
			
				else if(type.equals("float") || type.equals("double"))
					reg = "f_temp" + String.valueOf(Ast.floatLabelCounter++);
			
				else if(type.equals("char"))
					reg = "c_temp" + String.valueOf(Ast.characterLabelCounter++);
			
				else
					reg = "s_temp" + String.valueOf(Ast.stringLabelCounter++);
			
				output3AC(fout,"M_LOAD", reg,offset + "(off)","",currentLine);
			break;
			
			case STORE:
				reg = offset + "(off)";
				//output3AC(fout,"M_STORE", offset + "(off)", reg,"",currentLine);
			break;
			
			case ALLOC:
				String alloc_size = String.valueOf(symbol_node.getOffset());
				
				if(Driver.verbose || Driver.debugLevel > 0)
					System.out.printf("Allocating %s byte(s) for id: %s\n", alloc_size, identifier, symbol_node.type.getClass().getName());
/*
				if(symbol_node.type.getClass().getName().contains("Array")) { System.out.println("Here!!!");
					alloc_size = String.valueOf(symbol_node.type.offset);
				}
*/				
				reg = alloc_size + "(off)";
				
				output3AC(fout,"ALLOC", alloc_size,"","",currentLine);
			break;
			
			case NONE:
				//System.out.println("Offset: " + offset);
				reg = offset + "(off)";
			break;
		}
		
		return reg;
	}
	
	@Override public String gen3AC(PrintWriter fout) throws IOException
	{	
		return gen3AC(fout,Action.LOAD);
	}
	
	@Override public String getClassType()
	{
		return symbol_node.toString();
	}
	
	@Override public Object getData()
	{
		return identifier;
	}
	
	public String identifier;
	public SymbolNode symbol_node;
}

class AST_int_to_float extends AST_node
{
	public AST_int_to_float(AST_node arg1, String line)
	{
		name = "Int to Float Cast";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}

class AST_float_to_int extends AST_node
{
	public AST_float_to_int(AST_node arg1, String line)
	{
		name = "Float to Int Cast";
		currentLine = line.trim();
		nodes.add(arg1);
	}
}
