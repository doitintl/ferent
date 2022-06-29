(ns ferent.metrics
  (:require
   [ferent.find-cycles :refer [to-digraph digraph-all-cycles]]
   [ferent.utils :refer [build-map]]
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
  (let [{:keys [arrow-in arrow-out]} dependency-graph
        projects (set (concat (keys arrow-in) (keys arrow-out)))]
    (build-map projects (fn [p] [(arrow-out p) (arrow-in p)]))))

(defn metrics [dependency-graph]
  (let [metrics-for-projs (into {} (map metric-for-project (by-project dependency-graph)))
        per-project-plus-global-stats (assoc metrics-for-projs
                                             :project-count (count metrics-for-projs)
                                             :cycles (digraph-all-cycles (to-digraph dependency-graph)))]
    (into (sorted-map-by #(compare (str %1) (str %2))) per-project-plus-global-stats)))
