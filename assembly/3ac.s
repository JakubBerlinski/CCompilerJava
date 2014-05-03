# Program Start

# begin function: strange_add
	FUNC_BEGIN   strange_add                                    # int strange_add(int a, int b) {
strange_add:
	ALLOC_FRAME  20                                                                           
	NUM_PARAMS   2                                                                            
	M_LOAD       i_temp0      0(off)                            # if(a > 5)                   
	LOAD_I       i_temp1      5                                 # if(a > 5)                   
	GT           i_temp2      i_temp0      i_temp1              # if(a > 5)                   
	BNZ          i_temp2      LABEL0                            # x = a + 2;                  
	BAL          LABEL1                                         # x = a + 2;                  
LABEL0:
	M_LOAD       i_temp3      0(off)                            # x = a + b;                  
	M_LOAD       i_temp4      4(off)                            # x = a + b;                  
	ADD_I        i_temp5      i_temp3      i_temp4              # x = a + b;                  
	M_STORE      8(off)       i_temp5                           # x = a + b;                  
	BAL          LABEL2                                         # x = a + 2;                  
LABEL1:
	M_LOAD       i_temp6      0(off)                            # x = a + 2;                  
	LOAD_I       i_temp7      2                                 # x = a + 2;                  
	ADD_I        i_temp8      i_temp6      i_temp7              # x = a + 2;                  
	M_STORE      8(off)       i_temp8                           # x = a + 2;                  
LABEL2:
	M_LOAD       i_temp9      8(off)                            # return x;                   
	MOV          i_ret        i_temp9                           # return x;                   
	RETURN       i_ret                                          # return x;                   
	FUNC_END     strange_add                                    # int strange_add(int a, int b) {
# end function: strange_add

# begin function: main
	FUNC_BEGIN   main                                           # void main(int argc, char **argv)
main:
	ALLOC_FRAME  20                                                                           
	NUM_PARAMS   2                                                                            
	LOAD_I       i_temp10     6                                 # g = strange_add(6,2);       
	PARAM        i_temp10                                       # g = strange_add(6,2);       
	LOAD_I       i_temp11     2                                 # g = strange_add(6,2);       
	PARAM        i_temp11                                       # g = strange_add(6,2);       
	CALL         strange_add                                    # g = strange_add(6,2);       
	MOV          i_temp12     i_ret                             # g = strange_add(6,2);       
	M_STORE      8(off)       i_temp12                          # g = strange_add(6,2);       
	LOAD_I       i_temp13     0                                 # return 0;                   
	MOV          i_ret        i_temp13                          # return 0;                   
	RETURN       i_ret                                          # return 0;                   
	FUNC_END     main                                           # void main(int argc, char **argv)
# end function: main

