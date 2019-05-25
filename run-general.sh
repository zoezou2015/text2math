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
L2=0.01

emb=polyglot
embsize=64

dropout=0.0
fixpre="-fix-pretrain"
fixemb=""

mkdir -p logs model

for lang in $@; do
  prefix=orig
  # 1) Pre-train hybrid tree model
  time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative -thread 16 -lang $lang -optim lbfgs -l2 0.01 -iter $iter -save-iter $saveiter -save-prefix $prefix $debug > logs/$prefix.$lang.out 2> logs/$prefix.$lang.err
done

for window in 2 1 0; do
  for lang in $@; do
	pretrain="-pretrain model/orig.$lang.$((iter-1)).model"
	saveprefix="$lang"-window$window
	nnsaveprefix="$lang"-window$window
	args="-thread $thread $config -lang $lang -neural -optim $optim -lr $lr -l2 $L2 -iter $iter -save-iter $saveiter -save-prefix $saveprefix -num-layer $num_layer -hidden $hidden -embedding $emb -embedding-size $embsize -window $window -dropout $dropout $pretrain $fixpre $debug"
	# 2) Train neural model
	time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative $args -train > logs/"$saveprefix".out 2> logs/"$saveprefix".err
	# 3) Decode the test set
	time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative $args -decode test -neural-save-prefix $nnsaveprefix  >> logs/"$saveprefix".out 2>> logs/"$saveprefix".err
  done
done
