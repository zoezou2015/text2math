
# Arithmetic word problem solving
for dataset in addsub illinois; do
    l2=0.02
	time java -Xmx500g -Djava.library.path=/usr/local/lib -cp lib/json-20140107.jar:lib/zmq.jar:lib/junit-4.11.jar:lib/hamcrest-core-1.3.jar:lib/msgpack-core-0.8.8.jar:lib/commons-math3-3.6.1.jar:lib/commons-io-2.6.jar:bin/text2math.jar com.example.mathsolver.MathSolverMain -save-iter 100  -thread 16 -gram 2  -l2 $l2 -dataset $dataset -no-x -no-reverse -eval-sol  -save-prefix $dataset.l2.$l2  > logs/$dataset.l2-$l2.out 2> logs/$dataset.l2-$l2.err
done

# Equation parsing
l2=0.04
time java -Xmx150g -Djava.library.path=/usr/local/lib -cp bin/text2math.jar com.statnlp.example.equationparse.EquationParserMain -save-iter 100 -l2 $l2 -pred-num -save-prefix equation_parser > logs/equation_parser.out 2> logs/equation_parser.err


