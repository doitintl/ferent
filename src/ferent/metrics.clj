(ns ferent.metrics
  (:require [ferent.find-cycles :refer [digraph digraph-all-cycles]]
            [sc.api :refer :all]))

(defn- metric-for-project [project-and-arrows-both-ways]
  "Return pairs of project and metrics for that project"
  (let [[proj [arrowout arrowin]] project-and-arrows-both-ways
        arrowout-count (count arrowout)
        arrowin-count (count arrowin)]
    [proj {:arrow-in    arrowin-count
           :arrow-out   arrowout-count
           :instability (try (/ arrowout-count (+ arrowout-count arrowin-count))
                             (catch ArithmeticException _ Double/NaN))}]))

(defn- by-project [dependency-graph]
  (let [arrowin (dependency-graph :arrow-in)
        arrowout (dependency-graph :arrow-out)
        projects (set (concat (keys arrowin) (keys arrowout)))
        pairs (for [k projects] [k [(arrowout k) (arrowin k)]])]
    (into {} pairs)))

(defn metrics [dependency-graph]
  (let [by-proj (by-project dependency-graph)
        metrcs (map metric-for-project by-proj)
        metrics-for-projs (into {} metrcs )
        cycles (digraph-all-cycles (digraph dependency-graph))
        merged (assoc metrics-for-projs
                      :project-count (count metrics-for-projs)
                      :cycles cycles)
        sorted (into (sorted-map-by #(compare (str %1) (str %2))) merged)]

    sorted))
