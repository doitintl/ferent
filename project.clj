(defproject ferent "0.1.0-SNAPSHOT"
  :description "Dependency measurements on the model of JDepend"
  :url "http://joshuafox.com/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "1.0.1"]
                 [vvvvalvalval/scope-capture "0.3.3-s1"]
                 [aysylu/loom "1.0.2"]]
  :repl-options {:init-ns ferent.core}
  :plugins [[lein-cljfmt "0.8.0"]])
