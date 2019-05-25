set -e -x
mkdir -p logs

for lang in $@; do
  prefix=orig-syn
  pretrain="-model model/orig.$lang.99.model"
  extra_flags="-sequential-touch"
  if [ $lang = "en" ]; then
    pretrain="-model model/orig.$lang.93.model"
    extra_flags="-ratio-stop-criterion -precompute-test-feature-index"
  fi
  time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative -thread 16 -lang $lang -optim lbfgs -l2 0.01 -iter 100 -save-iter 50 -save-prefix $prefix -decode syn $pretrain $extra_flags > logs/$prefix.$lang.out 2> logs/$prefix.$lang.err
done

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

for window in 2 1; do
  for lang in $@; do
    pretrain="-pretrain model/orig.$lang.99.model"
    if [ $lang = "en" ]; then
      pretrain="-pretrain model/orig.$lang.93.model"
    fi
    saveprefix="$lang"-window$window-syn
    nnsaveprefix="$lang"-window$window
    time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative -thread $thread $config -lang $lang -neural -optim $optim -lr $lr -l2 $L2 -iter $iter -save-iter $saveiter -save-prefix $saveprefix -num-layer $num_layer -hidden $hidden -embedding $emb -embedding-size $embsize -window $window -dropout $dropout $pretrain $fixpre -neural-save-prefix $nnsaveprefix -decode syn > logs/"$saveprefix".out 2> logs/"$saveprefix".err
  done
done

