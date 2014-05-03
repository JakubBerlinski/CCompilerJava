
JAVA = AssemblyScanner.java AssemblyParser.java
JAR = bin/Assembly.jar

.PHONY: build

ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif

build: move
	ant build

move: gen
	mv -f $(JAVA) src/Assembly/

gen: AssemblyParser.y AssemblyScanner.lex
	jflex -q AssemblyScanner.lex
	bison AssemblyParser.y -o AssemblyParser.java
	
run:
	java -jar bin/Assembly.jar $(RUN_ARGS)
	
doc: move
	javadoc -d doc -private -noqualifier all src/*.java
	
rm-doc:
	rm -rf doc

clean:
	ant clean
	rm -f src/Assembly/AssemblyScanner.java src/Assembly/AssemblyParser.java *.txt *.dot *.png *.*~
	
clean-output:
	rm -f *.txt *.dot *.png *.*~
