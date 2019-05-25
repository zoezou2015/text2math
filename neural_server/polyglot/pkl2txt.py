import sys
import pickle
import numpy
words, embeddings = pickle.load(open(sys.argv[1], 'rb'))
print >> sys.stderr, "Embeddings shape is {}".format(embeddings.shape)
for w,e in zip(words,embeddings):
    print w.encode('utf-8')," ".join([str(x) for x in e.tolist()])
