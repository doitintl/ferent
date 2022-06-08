(ns ferent.main
  (:require
   [clojure.pprint :refer [pprint]]
   [ferent.core :refer [build-graph build-graph-from-dir]]
   [ferent.metrics :refer [metrics]]))

(defn -main [& args] ; & creates a list of var-args
  (pprint (metrics (build-graph {"p3" ["sa-p2"] "p2" ["sa-p1"] "p1" ["sa-p3"]}
                                {"p2" ["sa-p2"] "p3" ["sa-p3"] "p1" ["sa-p1"]}
                                false)))
  (comment (if (seq args)

             false

; Handle failure however here
             (throw (Exception. "Must have at least one argument!")))))