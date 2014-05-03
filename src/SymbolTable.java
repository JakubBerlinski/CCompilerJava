package Compiler;

import java.io.*;
import java.util.*;

/**
	Class containing references to all stored data in the program.
*/
public class SymbolTable
{
	/** Stack containing nodes to Binary Search Trees.					*/
	protected Stack<TreeMap<String,SymbolNode>> stack;
	/** Stack containing nodes to basic data types (for typedefs)		*/
	protected Stack<BasicDataType> typeTable;
	/** Binary Search Tree used when viewing the top of the stack. 		*/
	private TreeMap<String,SymbolNode> bst;
	/** Stack containing current total offset for allocation frame.		*/
	protected Stack<Integer> offsets;
	
	/**
	* Default Constructor. Allocates all stacks.
	*/
	public SymbolTable()
	{
		stack = new Stack<TreeMap<String,SymbolNode>>();
		typeTable = new Stack<BasicDataType>();
		offsets = new Stack<Integer>();
	}
	
	/**
	* Pushes a new level onto all stacks.
	*/
	public void pushLevel()
	{
		// inherit previous scopes type table
		if(typeTable.size() > 0)
			typeTable.push(new BasicDataType(typeTable.peek()));
		
		// else create default table
		else
			typeTable.push(new BasicDataType());
		
		// push new BST onto main stack
		stack.push(new TreeMap<String,SymbolNode>());
		
		// add new level to offset stack with default value of 0
		if(offsets.size() > 1)
			offsets.push(new Integer(offsets.peek().intValue()));
			
		else
			offsets.push(new Integer(0));
	}
	
	/**
	* Removes the top level from all stacks.
	*/
	public void popLevel()
	{
		if(stack.size() > 0)
		{
			stack.pop();
			typeTable.pop();
			offsets.pop();
		}
	}
	
	/**
	* Gets a pointer to the top of the stack.
	* @return Pointer to the BST on the top level of the stack.
	*/
	public TreeMap<String,SymbolNode> getTopLevel()
	{
		return stack.peek();
	}
	
	/**
	* Inserts a new value into the top level of the Symbol Table.
	* @param key The key (Identifier Name) to use when inserting.
	* @param node The SymbolNode to be used when inserting.
	*/
	public void insert(String key, SymbolNode node)
	{
		// push a level if the stack is empty
		if(stack.size() == 0)
			pushLevel();
			
		// get pointer to top level of the stack
		bst = stack.peek();

		// calculate new offset for node that's to be inserted
		if(node.type.getClass().getName().toLowerCase().contains("function"))
			node.origOffset = 0;
			
		Integer i = offsets.pop();
		offsets.push(new Integer(node.origOffset + i.intValue()));
		node.offset = i.intValue();
	
		// insert into the tree
		bst.put(key,node);
	}
	
	/**
	* Removes a node from the top level of the main stack.
	* @param key The key of the node to be removed.
	* @return The node that was removed.
	*/
	public SymbolNode removeNode(String key)
	{
		SymbolNode value = stack.peek().remove(key);
		
		Integer i = offsets.pop();
		
		int newOffset = i.intValue() - value.origOffset;
		
		offsets.push(new Integer(newOffset));
		
		return value;
	}
	
	/**
	* Get current total offset for top level of the main stack.
	* @return Current total offset.
	*/
	public int getOffset()
	{
		return offsets.peek().intValue();
	}
	
	
	/**
	* Calls find with print set to false.
	* @param key The key to search for.
	* @see #find(String,boolean)
	*/
	public FindReturn find(String key)
	{
		return find(key,false);
	}
	
	/**
	* Finds a node in the Symbol Table.  This node can be on any level of the Symbol Table.
	* @param key The key (Identifier) to search for.
	* @param print Enable printing for debugging.
	* @return A pointer to the FindReturn class if found, otherwise null.
	*/
	public FindReturn find(String key, boolean print)
	{
		boolean found = false;
		SymbolNode node = null;
		int numLevels = 0;
		
		Stack<TreeMap<String,SymbolNode>> tempStack = new Stack<TreeMap<String,SymbolNode>>();
		
		if(stack.size() == 0)
			return null;
			
		bst = stack.peek();
		
		if(bst.containsKey(key))
			return new FindReturn(bst.get(key), 0);
			
		// search all stack levels for node
		while(!found && stack.size() > 1)
		{
			tempStack.push(bst);
			stack.pop();
			
			bst = stack.peek();
			
			if(bst.containsKey(key))
			{
				node = bst.get(key);
				found = true;
			}
			
			numLevels++;
		}
		
		// replace all stack levels
		while(tempStack.size() > 0)
		{
			stack.push(tempStack.peek());
			tempStack.pop();
		}
		
		if(!found && print)
			System.out.println("Symbol Not Found");
			
		else if(print)
			System.out.println("Symbol Found " + String.valueOf(numLevels) + " level(s) up.");
			
		return new FindReturn(node,numLevels);
	}
	
	/**
	* Outputs the contents of the Symbol Table to a file.
	* @param filename The name of the file to be written.
	*/
	public void writeToFile(String filename)
	{
		int currentLevel = stack.size();
		Stack<TreeMap<String,SymbolNode>> tempStack = new Stack<TreeMap<String,SymbolNode>>();
		
		PrintWriter fout;
		
		try
		{
			fout = new PrintWriter(filename);
				
			while(currentLevel > 0)
			{
				bst = stack.peek();
				
				fout.println("Stack Level: " + String.valueOf(currentLevel));
				
				for(Map.Entry<String,SymbolNode> entry : bst.entrySet())
				{
					String key = entry.getKey();
					SymbolNode node = entry.getValue();
					
					fout.println("\t[" + node.toString() + ((node.type.pointer) ? "* " : " ") + key + "]");
				}
				
				fout.println();
				
				tempStack.push(stack.peek());
				stack.pop();
				
				currentLevel--;
			}
			
			while(tempStack.size() > 0)
			{
				stack.push(tempStack.peek());
				tempStack.pop();
			}
			
			fout.close();
			
		}
		
		catch(IOException e)
		{
			System.err.println("Error: Critical IO Failure in SymbolTable::writeToFile()");
			System.exit(1);
		}
	}
	
	/**
	* Converts the contents of the Symbol Table to a string so they can be printed easily.
	* @return String containing Symbol Table contents.
	*/
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		int currentLevel = stack.size();
		Stack<TreeMap<String,SymbolNode>> tempStack = new Stack<TreeMap<String,SymbolNode>>();
		
		while(currentLevel > 0)
		{
			bst = stack.peek();
			
			sb.append("Stack Level: " + String.valueOf(currentLevel) + "\n");
			
			for(Map.Entry<String,SymbolNode> entry : bst.entrySet())
			{
				String key = entry.getKey();
				SymbolNode node = entry.getValue();
				
				sb.append("\t[" + node.toString() + " " + key + "]\n");
			}
			
			sb.append("\n");
			
			tempStack.push(stack.peek());
			stack.pop();
			
			currentLevel--;
		}
		
		while(tempStack.size() > 0)
		{
			stack.push(tempStack.peek());
			tempStack.pop();
		}
		
		return sb.toString();
	}
	
	/**
	* Class used for returning multiple values from SymbolTable::find function.
	*/
	public class FindReturn
	{
		/** Symbol Node to return							*/
		public SymbolNode node;
		/** int containing the level the node was found on 	*/
		public int level;
		
		/**
		* Only constructor for FindReturn class.
		*/
		public FindReturn(SymbolNode n, int l)
		{
			node = n;
			level = l;
		}
	}
}

/**
* Class for storing the contents of a symbol.
*/
class SymbolNode
{
	/** Object containing the data type							*/
	public BasicDataType type;
	/** int containing the line the symbol was defined on 		*/
	public int lineNum;
	/** int containing the offset of this node (for assembly) 	*/
	public int offset;
	/** int containing the nodes original offset (for assembly) */
	public int origOffset;
	
	/**
	* Instantiates a symbol node with defualt values.
	*/
	public SymbolNode()
	{
		type = null;
		lineNum = 0;
	}

	
	/**
	* Instantiates a symbol node with defualt values.
	* @param t The type to store.
	*/
	public SymbolNode(BasicDataType t)
	{
		type = t;
		lineNum = 0;
		
		origOffset = offset = t.getOffset();
	}
	
	/**
	* Instantiates a symbol node with defualt values. (Main Constructor Used)
	* @param t The type to store.
	* @param lineNumber The line that the symbol was declared on.
	*/
	public SymbolNode(BasicDataType t, int lineNumber)
	{
		type = t;
		lineNum = lineNumber;
		
		origOffset = offset = t.getOffset();
	}
	
	/**
	* Converts the node to a string for printing.
	* @return String representation of this object.
	*/
	@Override
	public String toString()
	{
		return type.toString();
	}
	
	/**
	* Returns the offset of this node.
	* @return Current offset for this node.
	*/
	public int getOffset()
	{
		return type.getOffset();
	}
}

/**
* Class for containing basic data types.
*/
class BasicDataType
{
	/** String containing the data type.		*/
	public String type;
	/** int containing the offset of the type.	*/
	public int offset;
	/** int containing the type offset.			*/
	public int typeOffset;
	/** int containing the size of the data.	*/
	public int dataSize;
	/** boolean to determine if pointer type	*/
	public boolean pointer;	
	/** List of defined types.					*/
	public ArrayList<String> types = new ArrayList<String>(Arrays.asList(
	"int", "float", "char", "double", "long", "long long", "short", "void"
	));
	
	/**
	* Default constructor for this class (rarely used).
	*/
	public BasicDataType()
	{
		type = "void";
		dataSize = offset = 0;
		pointer = false;
	}
	
	/**
	* Constructor for initializing this class with a type.
	* @param _type Data type.
	*/
	public BasicDataType(String _type)
	{
		this(_type,false);
	}
	
	public BasicDataType(String _type, boolean ptr)
	{
		type = _type;
		pointer = ptr;
		
		if(!types.contains(type))
			Driver.scanner.yyerror("undefined data type");
			
		if(pointer)
			offset = 4;
			
		if(type.equals("int"))
			offset = 4;
			
		else if(type.equals("short"))
			offset = 4;
		
		else if(type.equals("float"))
			offset = 4;
			
		else if(type.equals("char"))
			offset = 4;
			
		else if(type.equals("double"))
			offset = 4;
			
		else if(type.equals("long"))
			offset = 4;
			
		else if(type.equals("long long"))
			offset = 4;	
			
		else if(type.equals("void"))
			offset = 0;
			
		dataSize = offset;
	}
	/**
	* Copy Constructor.
	* @param other BasicDataType to be copied.
	*/
	public BasicDataType(BasicDataType other)
	{
		types = new ArrayList<String>(other.types);
		type = new String(other.type);
		offset = other.offset;
		dataSize = other.dataSize;
		
	}
	
	/**
	* Adds a type to the list of known types.
	* @param _type Type to be added.
	*/
	public void addType(String _type)
	{
		types.add(_type);
	}
	
	/**
	* Returns offset of this type.
	* @return Offset of this data type.
	*/
	public int getOffset()
	{
		return offset;
	}
	
	/**
	* Returns data type (String) of this class.
	* @return Underlying data type of this class.
	*/
	public String getType()
	{
		return type;
	}
	
	/**
	* Returns String representation of this class.
	* @return String containing underlying data type.
	*/
	@Override
	public String toString()
	{
		return type;
	}
	
	/**
	* Compares two basic data types.
	* @return True if data types are equal, false otherwise.
	*/
	public boolean equals(BasicDataType other)
	{
		if(!getClass().getName().equals(other.getClass().getName()))
			return false;
			
		else if(!type.equals(other.type))
			return false;
			
		else
			return true;
	}
	
	public int getParamCount() {
		return 0;
	}
}

/**
* Class for containing array data types.
*/
class ArrayDataType extends BasicDataType
{
	/** int containing the size of the array (for bounds checking).	*/
	public int size;
	/** int containing the total data size of this array.			*/
	public int totalSize;
	/** Nested ArrayDataType for multi-dementional arrays.			*/
	public ArrayDataType nestedArray = null;
	
	/**
	* Default Constructor (rarely used).
	*/
	public ArrayDataType()
	{
		super();
		size = 0;
	}
	
	/**
	* Constructor to initialize this array with a underlying data type.
	* @param _type Underlying data type for this array.
	*/
	public ArrayDataType(String _type)
	{
		super(_type);
		totalSize = size = 0;
	}
	
	/**
	* Constructor to initialize with a size.
	* @param _size Size of the array.
	*/
	public ArrayDataType(int _size)
	{
		super();
		totalSize = size = _size;
	}
	
	/**
	* Constructor initializing this array with a type and size (most comonly used constructor).
	* @param _type Underlying data type for this array.
	* @param _size Size of the array.
	*/
	public ArrayDataType(String _type, int _size)
	{
		super(_type);
		totalSize = size = _size;
		typeOffset = offset;
		offset *= size;
		offset -= typeOffset;
		
		dataSize = size * typeOffset;
		
		//System.out.(dataSize);
	}

	/**
	* Used to add a nested array. Used for multi-dementional arrays.
	* @param arr Array to be nested.
	*/
	public void addNestedArray(ArrayDataType arr)
	{
		nestedArray = arr;
		totalSize *= arr.size;
		//offset = typeOffset;
		offset = (typeOffset * totalSize) - typeOffset;
		
		typeOffset *= arr.size;
		
		dataSize = size * arr.size * typeOffset;
		
		//System.out.println(dataSize);
	}
	
	/**
	* Returns data size.
	* @return Size of the entire array.
	*/
	@Override
	public int getOffset()
	{
		return dataSize;
	}
	
	/**
	* Returns a string representation of this Data Type.
	* @return String representing this data type.
	*/
	@Override
	public String toString()
	{
		if(nestedArray == null)
			return String.format("%s array[%d]", type, size);
			
		else
			return String.format("%s array[%d][%d]", type, size, nestedArray.size);
	}
}

/**
* Class for containing function types.
*/
class FunctionDataType extends BasicDataType
{
	/** int containing number of parameters for this function.		*/
	public int numParameters;
	/** BasicDataType array for holding data types of parameters.	*/
	public BasicDataType[] parameterTypes;
	
	/**
	* Default constructor (rarely used).
	*/
	public FunctionDataType()
	{
		super();
		numParameters = 0;
	}
	
	/**
	* Constructor initializing with a number of parameters.
	* @param _numParameters Number of parameters to be set.
	*/
	public FunctionDataType(int _numParameters)
	{
		super();
		numParameters = _numParameters;
		
		parameterTypes = new BasicDataType[numParameters];
	}
	
	/**
	* Constructor initializing with return Type and number of parameters.
	* @param returnType Return type of this function.
	* @param _numParameters Number of parameters to be set.
	*/
	public FunctionDataType(String returnType, int _numParameters)
	{
		super(returnType);
		numParameters = _numParameters;
		
		parameterTypes = new BasicDataType[numParameters];
	}
	
	/**
	* Constructor to initialize this function with return type, number of parameters, and parameter data types.
	* @param returnType Return data type of this function.
	* @param _numParameters Number of parameters for this function.
	* @param _parameterTypes Data types of parameters.
	*/
	public FunctionDataType(String returnType, int _numParameters, BasicDataType... _parameterTypes)
	{
		super(returnType);
		numParameters = _numParameters;
		parameterTypes = _parameterTypes;
	}
	
	/**
	* Constructor to initialize this function with return type, number of parameters, and parameter data types.
	* @param returnType Return data type of this function.
	* @param _numParameters Number of parameters for this function.
	* @param _parameterTypes String representation of the parameter data types.
	*/
	public FunctionDataType(String returnType, int _numParameters, String... _parameterTypes)
	{
		this(returnType,_numParameters);
		
		for(int i = 0; i < _parameterTypes.length; i++)
			parameterTypes[i] = new BasicDataType(_parameterTypes[i]);
	}
	
	/**
	* Returns a string representation of this data type.
	* @return String to represent this data type.
	*/
	@Override
	public String toString()
	{
		return type + " function(" + String.valueOf(numParameters) + " params)";
	}
	
	@Override
	public int getParamCount() {
		return numParameters;
	}
}

/**
* Class for containing struct data types.
*/
class StructDataType extends BasicDataType
{
	/** BST for containing struct elements.			*/
	public TreeMap<String,BasicDataType> elements = new TreeMap<String,BasicDataType>();
	
	/**
	* Default constructor.
	*/
	public StructDataType()
	{
		super("struct");
	}
	
	/**
	* Adds an element to the struct type.
	* @param _identifier Identifier of new element.
	* @param _type Data type of new element.
	*/
	public void addElement(String _identifier, BasicDataType _type)
	{
		elements.put(_identifier,_type);
	}
}
