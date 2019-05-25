for lang in en de el th id zh fa sv; do
    python pkl2txt.py polyglot-$lang.pkl > polyglot-$lang.txt
    th bintot7.lua polyglot-$lang.txt polyglot-$lang.t7
done
