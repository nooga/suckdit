(defproject suckdit "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "suckdit"
              :source-paths ["src"]
              :compiler {
                :output-to "suckdit.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
