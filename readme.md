# The StatNLP Semantic Parser Version 0.2-n

Code for the AAAI-17 paper: Semantic Parsing with Neural Hybrid Trees. This code implements a neural, discriminative hybrid tree framework for semantic parsing.

## Installation

This code has been tested on Amazon AWS `r3.8xlarge` instance and AMI `ami-b36981d8`. Follow the instructions below to install dependencies.

Java 1.8

```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
```

ZeroMQ

```
git clone https://github.com/zeromq/libzmq
cd libzmq
./autogen.sh && ./configure && make -j 4
make check && sudo make install && sudo ldconfig
```

JZMQ

```
git clone https://github.com/zeromq/jzmq
cd jzmq/jzmq-jni
./autogen.sh && ./configure && make
sudo make install
```

Torch dependency libraries

```
luarocks install lzmq
luarocks install lua-messagepack
luarocks install luautf8
```

Swipl (for evaluation)

```
sudo apt install swi-prolog-nox
```

A jar file in provided in `bin/`. One easy way to re-compile the code is to create a new project in Eclipse and export a runnable JAR file for the main class: `SemTextExperimenter_Discriminative`.

## Word embedding

Download the .pkl files from [Polyglot]( https://sites.google.com/site/rmyeid/projects/polyglot#TOC-Download-the-Embeddings). Put these files in `neural_server/polyglot`, then run the following to preprocess for Torch:

```
bash prepare_torch.sh
```

## Running the code

The following script will pre-train a hybrid tree model, train the neural network, and perform evaluation on the standard GeoQuery dataset.

```
bash run-general.sh <lang1> <lang2> ... <langN>
```

## Reproducing the experimental results

First, pre-train the hybrid tree models (Lu, 2015) for multiple languages:

```
bash pretrain.sh <lang1> <lang2> ... <langN>
```

Run a neural net server that listens on port 5556 and specify the `gpuid` (>= 0 for GPU, -1 for CPU)

```
th server.lua -port 5556 -gpuid -1
```

Train neural hybrid tree models for multiple languages:

```
bash run.sh <lang1> <lang2> ... <langN>
```

Model robustness experiments (this requires the above trained models):

```
bash noise_test.sh <lang1> <lang2> ... <langN>
```

## Contact

Raymond Hendy Susanto and Wei Lu, Singapore University of Technology and Design

Please drop an email at raymond_susanto@sutd.edu.sg for questions.
