set -e -x
mkdir -p logs model

for lang in $@; do
  extra_flags="-sequential-touch"
  if [ $lang = "en" ]; then
    # Flags added to achieve the same performance in Lu (2015) (http://www.aclweb.org/anthology/P15-2121).
    # Also does multithreaded feature extraction. 
    extra_flags="-ratio-stop-criterion -precompute-test-feature-index"
  fi
  prefix=orig
  time java -Xmx144g -Djava.library.path=/usr/local/lib -cp bin/sp.jar com.statnlp.example.sp.main.SemTextExperimenter_Discriminative -thread 16 -lang $lang -optim lbfgs -l2 0.01 -iter 100 -save-iter 50 -save-prefix $prefix $extra_flags > logs/$prefix.$lang.out 2> logs/$prefix.$lang.err
done
