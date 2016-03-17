(defproject spiral "A game template"
  :description "Generates an impartial game"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [devcards "0.2.1-5"]
                 [sablono "0.5.3"]
                 [cljsjs/jquery "2.1.4-0"]
                 [cljsjs/bootstrap "3.3.6-0"]

                 ;; need to specify this for sablono
                 ;; when not using devcards
                 [cljsjs/react "0.14.3-0"]
                 [cljsjs/react-dom "0.14.3-1"]
                 [cljsjs/react-dom-server "0.14.3-0"]
                 [secretary "1.2.3"]
                 [rum "0.6.0"]
                 [figwheel-sidecar "0.5.0"]
                 ]

  :plugins [                                                ;[lein-figwheel "0.5.0-6"]
            [lein-cljsbuild "1.1.2" :exclusions [org.clojure/clojure]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :source-paths ["src" "script"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "slippery.main"
                                   :asset-path "js/compiled/slippery"
                                   :output-to  "resources/public/js/compiled/slippery.js"
                                   :output-dir "resources/public/js/compiled/slippery"
                                   :source-map-timestamp true }}

                       {:id "devcards"
                        :source-paths ["src"]
                        :figwheel { :devcards true } ;; <- note this
                        :compiler { :main       "slippery.devcards"
                                   :asset-path "js/compiled/devcards_out"
                                   :output-to  "resources/public/js/compiled/devcards.js"
                                   :output-dir "resources/public/js/compiled/devcards_out"
                                   :source-map-timestamp true }}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:main       "slippery.main"
                                   :externs ["resources/externs/svg.js"]
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/slippery.js"
                                   :optimizations :advanced}}
                       ]
              }

  :figwheel {:css-dirs ["resources/public/css"]
             ;:open-file-command "/usr/local/bin/open-in-intellij"
             })
