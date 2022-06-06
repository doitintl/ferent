(ns ferent.metrics
  (:require [ferent.graph :refer [digraph digraph-all-cycles]]
            [sc.api :refer :all]))

(defn- metric-for-project  [project-and-arrows-both-ways]
  "Return pairs of project and metrics for that project"
  (let [[proj [arrowout  arrowin]] project-and-arrows-both-ways
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
        pairs (for [k projects] [k [(vec (arrowout k)) (vec (arrowin k))]])]
    (into {} pairs)))
(set! *warn-on-reflection* true)
(defn metrics [dependency-graph]
  ; dependency-graph looks like
  ; {:arrowin  {"p1" #{"p2" "p3"}}
  ; :arrowout {"p2" #{"p1"}
  ;            "p3" #{"p1"}}}
  (let [by-proj (by-project dependency-graph)
        metrics-for-projects (into {} (map metric-for-project by-proj))]

    (merge metrics-for-projects
           {:project-count (count metrics-for-projects)
            :cycles        (digraph-all-cycles (digraph dependency-graph))})))
