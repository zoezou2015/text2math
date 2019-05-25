
set -e -x
debug=""
thread=16
iter=100
saveiter=50
optim=adam
lr=0.001
config="-config neural_server/neural.sp.config"
hidden=100
num_layer=1

emb=polyglot
embsize=64

dropout=0.0
fixpre="-fix-pretrain"
fixemb=""


mkdir -p logs model

for window in 0 1 2 3 4 5 6; do
	# Arithmetic word problem solving
	for dataset in addsub illinois; do 
		L2=0.02
		pretrain="-pretrain model/$dataset.l2.$L2"
		saveprefix="$dataset-window$window-$iter"
		time java -Xmx500g -Djava.library.path=/usr/local/lib -cp lib/json-20140107.jar:lib/zmq.jar:lib/junit-4.11.jar:lib/hamcrest-core-1.3.jar:lib/msgpack-core-0.8.8.jar:lib/commons-math3-3.6.1.jar:bin/text2math.jar com.example.mathsolver.MathSolverMain -thread $thread $config -neural -gram 2 -eval-sol -no-x -no-reverse -dataset $dataset -optim $optim -lr $lr -l2 $L2 -iter $iter -save-iter 1000 -save-prefix $saveprefix -num-layer $num_layer -hidden $hidden -embedding $emb -embedding-size $embsize -window $window -dropout $dropout $pretrain $fixpre -neural-save-prefix $saveprefix > logs/"$saveprefix-L2$L2".sol.out 2> logs/"$saveprefix-L2$L2".sol.err
	done
	# Equation parsing
	L2=0.04
	pretrain="-pretrain model/equation_parser"
	saveprefix="equation_parser-window$window-$iter"
	time java -Xmx500g -Djava.library.path=/usr/local/lib -cp lib/json-20140107.jar:lib/zmq.jar:lib/junit-4.11.jar:lib/hamcrest-core-1.3.jar:lib/msgpack-core-0.8.8.jar:lib/commons-math3-3.6.1.jar:bin/text2math.jar com.example.equationparse.EquationParserMain -thread $thread $config -neural -gram 2 -pred-num -optim $optim -lr $lr -l2 $L2 -iter $iter -save-iter 1000 -save-prefix $saveprefix -num-layer $num_layer -hidden $hidden -embedding $emb -embedding-size $embsize -window $window -dropout $dropout $pretrain $fixpre -neural-save-prefix $saveprefix > logs/"$saveprefix".sol.out 2> logs/"$saveprefix".sol.err
	
done

