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

mkdir -p logs

for window in 2 1 0; do
  for lang in $@; do

pretrain="-pretrain model/orig.$lang.99.model"
if [ $lang = "en" ]; then
  pretrain="-pretrain model/orig.$lang.93.model"
  use_ratio_stop_criterion="-ratio-stop-criterion"
fi
saveprefix="$lang"-window$window

time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative -thread $thread $config -lang $lang -neural -optim $optim -lr $lr -l2 $L2 -iter $iter -save-iter $saveiter -save-prefix $saveprefix -num-layer $num_layer -hidden $hidden -embedding $emb -embedding-size $embsize -window $window -dropout $dropout $pretrain $fixpre $use_ratio_stop_criterion > logs/"$saveprefix".out 2> logs/"$saveprefix".err

  done
done
