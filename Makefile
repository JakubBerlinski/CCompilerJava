
JAVA = Scanner.java Parser.java
JAR = bin/Compiler.jar

.PHONY: build move clean clean-output clean-all all

ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif

build: move
	ant build

move: gen
	mv -f $(JAVA) src/

gen: parser.y scanner.lex
	jflex -q scanner.lex
	bison parser.y -o Parser.java
	
run:
	java -jar bin/Compiler.jar $(RUN_ARGS)
	
doc: move
	javadoc -d doc -private -noqualifier all src/*.java
	
all: build
	cd assembly; make
	cp assembly/bin/Assembly.jar bin/
	
rm-doc:
	rm -rf doc
	
clean-all: clean
	cd assembly; make clean
	
clean:
	ant clean
	rm -f src/Scanner.java src/Parser.java *.txt *.dot *.png *.*~ *.s
	
clean-output:
	rm -f *.txt *.dot *.png *.*~ *.s
