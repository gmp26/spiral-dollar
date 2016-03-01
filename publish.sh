#!/bin/bash

cd ~/clojure/gotit-no-frills
lein clean
lein cljsbuild once min
rsync -av resources/public/ gmp26@pan.maths.org:/www/nrich/html/gotit-island
