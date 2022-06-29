(defproject ferent "0.1.0-SNAPSHOT"
  :description "Dependency measurements on the model of JDepend"
  :url "http://github.com/JoshuaFox"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.csv "1.0.1"]
                 [vvvvalvalval/scope-capture "0.3.3-s1"]
                 [aysylu/loom "1.0.2"]
                 [org.clojure/data.json "2.4.0"]
                 [com.climate/claypoole "1.1.4"]
                 [com.google.apis/google-api-services-cloudresourcemanager "v3-rev20220523-1.32.1"]
                 [com.google.apis/google-api-services-discovery "v1-rev20200806-1.32.1"]
                 [com.google.auth/google-auth-library-oauth2-http "1.7.0"]
                 [com.google.apis/google-api-services-iam "v1-rev20220526-1.32.1"]
                 [babashka/process "0.1.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 ]
  :repl-options {:init-ns ferent.build-graph}
  :plugins [[lein-cljfmt "0.8.0"]
            [lein-ubersource "0.1.1"]
            ]
  :main ferent.ferent)
