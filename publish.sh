#!/bin/bash

cd ~/clojure/silver-dollar
lein clean
lein cljsbuild once min
rsync -av resources/public/ gmp26@pan.maths.org:/www/nrich/html/slippery
