(ns ferent.metrics
  (:require [ferent.find-cycles :refer [digraph digraph-all-cycles]]
            [sc.api :refer :all]))

(defn- metric-for-project [project-and-arrows-both-ways]
  "Return pairs of project and metrics for that project"
  (let [[proj [arrowout arrowin]] project-and-arrows-both-ways
        arrowout-count (count arrowout)
        arrowin-count (count arrowin)]
    [proj {:arrowin     arrowin-count
           :arrowout    arrowout-count
           :instability (try (/ arrowout-count (+ arrowout-count arrowin-count))
                             (catch ArithmeticException ae 0))}]))

(defn- by-project [dependency-graph]
  (let [arrowin (dependency-graph :arrowin)
        arrowout (dependency-graph :arrowout)
        projects (set (concat (keys arrowin) (keys arrowout)))
        pairs (for [k projects] [k [(arrowout k) (arrowin k)]])]
    (into {} pairs)))

(defn metrics [dependency-graph]
  (let [by-proj (by-project dependency-graph)
        metrcs (map metric-for-project by-proj)
        metrics-for-projs (into {}
                                (sort #(compare (first %1) (first %2))
                                      metrcs))
        digraph-all (digraph-all-cycles (digraph dependency-graph))
        merged (assoc metrics-for-projs
                      :project-count (count metrics-for-projs)
                      :cycles digraph-all)]

    merged))
