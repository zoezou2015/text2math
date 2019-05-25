for lang in en; do
    python pkl2txt.py polyglot-$lang.pkl > polyglot-$lang.txt
    th bintot7.lua polyglot-$lang.txt polyglot-$lang.t7
done
