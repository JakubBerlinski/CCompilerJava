
package Assembly;

import java.io.PrintWriter;
import java.util.*;

/**
	Class for allocating registers
*/
public class RegisterAllocator
{
	public RegisterEntry[] int_regTable;
	public RegisterEntry[] float_regTable;
	public RegisterEntry[] addTable;
	
	private PrintWriter fout;
	private int currentSpillIndex = 0;
	private int currentSpillAddress = 0;
	private int currentTableIndex = 0;
	
	public RegisterAllocator()
	{
		int_regTable = new RegisterEntry[8];
		float_regTable = new RegisterEntry[4];
				
		for(int i = 0; i <= 7; i++)
			int_regTable[i] = new RegisterEntry("$t" + String.valueOf(i), null);
			
		for(int i = 4, j = 0; i <= 10; i += 2, j++)
			float_regTable[j] = new RegisterEntry("$f" + i, null);
			
		//for(int i = 16; i < 31; i += 2)
			//float_regTable.put("$f" + i, null);
			
		addTable = new RegisterEntry[256];
		for(int i = 0; i < 256; i++)
			addTable[i] = new RegisterEntry(null,null);
			
		currentSpillAddress = 0;
		
		fout = AssemblyDriver.fout;
	}
	
	//@SuppressWarnings("unchecked")
	public RegisterEntry getRegister(String reg)
	{		
		RegisterEntry[] table = (reg.charAt(0) == 'f') ? float_regTable : int_regTable;
		
		// register already in table
		for(RegisterEntry entry : table)
			if(entry.owner != null && entry.owner.equals(reg)) {
				entry.isNew = false;
				return entry;
			}
		
		// register not in table, but room available		
		for(RegisterEntry entry : table)
			if(entry.owner == null) {
				entry.owner = reg;
				entry.isNew = true;
				return entry;
			}
		System.out.println("Reg: " + reg);
		// register not in table and table full -- spill
		// check if in address table
		for(int i = 0; i < addTable.length; i++) {
			if(addTable[i].register == null)
				break;
			
			else {
				// entry is in spill table
				if(addTable[i].owner != null && addTable[i].owner.equals(reg)) { System.out.println("HERE!!!!");
					RegisterEntry entry = table[currentTableIndex];
					entry.address = currentSpillAddress;
					
					addTable[i+1] = entry;
					String spillSpace = String.valueOf(entry.address) + "($s0)";
					AssemblyDriver.outputAssembly2("sw", entry.register, spillSpace, " spill");
					
					table[currentTableIndex].owner = addTable[i].owner;
					spillSpace = String.valueOf(addTable[i].address) + "($s0)";
					
					AssemblyDriver.outputAssembly2("lw", table[currentTableIndex].register, spillSpace, " spill");
					
					currentSpillIndex++;
					currentTableIndex++;
					currentTableIndex %= 8;
					
					addTable[i] = addTable[i+1];
					addTable[i+1] = new RegisterEntry(null,null);
					
					return table[currentSpillIndex-1];
				}
			}
		}
		
		// spill
		addTable[currentSpillIndex].register = table[currentTableIndex].register;
		addTable[currentSpillIndex].owner = table[currentTableIndex].owner;
		addTable[currentSpillIndex].address = currentSpillAddress;
		currentSpillAddress += 4;
		
		String spill = String.valueOf(addTable[currentSpillIndex].address) + "($s0)";
		
		AssemblyDriver.outputAssembly2("sw", table[currentTableIndex].register, spill, " spill");
		
		table[currentTableIndex].owner = reg;
		
		int index = currentTableIndex;
		currentTableIndex++;
		currentTableIndex %= 8;
		currentSpillIndex++;
		currentSpillIndex %= 256;
		
		return table[index];
		
		
		
		/*
		// register already in table
		if(table.containsValue(reg))
		{
			for(Map.Entry<String,String> entry : table.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				
				if(value != null && value.equals(reg))
					return new RegisterEntry(key, false);
			}
		}
		
		// register not in table, but room available
		else if(table.containsValue(null))
		{
			for(Map.Entry<String,String> entry : table.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				
				if(value == null) {
					table.put(key,reg);
					return new RegisterEntry(key, true);
				}
			}
		}
		
		// register not in table and table full -- spill
		else
		{
			int 
			currentSpillAddress = rand.nextInt(997);
			//fix later
			return null;
		}
		
		return null;
	*/
	}
	
	//@SuppressWarnings("unchecked")
	public void freeRegister(String reg)
	{
		if(reg == null)
			return;
			
		RegisterEntry[] table;
		if(reg.charAt(0) == '$')
			table = (reg.charAt(1) == 'f') ? float_regTable : int_regTable;
			
		else
			table = (reg.charAt(0) == 'f') ? float_regTable : int_regTable;

		
		for(RegisterEntry entry : table)
		{
			if(entry.owner != null && (entry.owner.equals(reg) || entry.register.equals(reg))) {
				entry.owner = null;
				return;
			}
		}
		
		
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("int\n");
		for(RegisterEntry entry : int_regTable)
			sb.append("[" + entry.register + ", " + entry.owner + "]\n");
			
		sb.append("\nfloat\n");
		for(RegisterEntry entry : float_regTable)
			sb.append("[" + entry.register + ", " + entry.owner + "]\n");
			
		sb.append("\naddress\n");
		for(RegisterEntry entry : addTable)
			if(entry.register != null)
				sb.append("[" + entry.register + ", " + entry.owner + "]\n");
			
		return sb.toString();
	 /*
		StringBuilder sb = new StringBuilder();
		
		sb.append("int\n");
		for(Map.Entry<String,String> entry : int_regTable.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			
			sb.append("[" + key + ", " + value + "]\n");
		}
		
		sb.append("\nfloat\n");
		for(Map.Entry<String,String> entry : float_regTable.entrySet())
		{
			String key = entry.getKey();
			String value = entry.getValue();
			
			sb.append("[" + key + ", " + value + "]\n");
		}
		return sb.toString();
		*/
	}
	
	class RegisterEntry
	{
		public String register;
		public String owner;
		public boolean isNew;
		public int address;
		
		public RegisterEntry(String reg, String own, boolean b)
		{
			register = reg;
			owner = own;
			isNew = b;
			address = 0;
		}
		
		public RegisterEntry(String reg, String own, int add)
		{
			register = reg;
			owner = own;
			isNew = false;
			address = add;	
		}
		
		public RegisterEntry(String reg, String own)
		{
			register = reg;
			owner = own;
			isNew = false;
			address = 0;
		}
		
		public RegisterEntry(String reg, boolean b)
		{
			register = reg;
			owner = null;
			isNew = b;
			address = 0;
		}
		
		@Override
		public boolean equals(Object other)
		{
			if(other == null)
				return false;
				
			RegisterEntry o = (RegisterEntry) other;
			
			if(owner == null && o.owner != null)
				return false;
				
			if(owner != null && o.owner == null)
				return false;
			
			if(!register.equals(o.register) || !owner.equals(o.owner))
				return false;
				
			return true;
		}
	}
}
