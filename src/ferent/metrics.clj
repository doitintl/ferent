(ns ferent.metrics
  (:require
   [ferent.find-cycles :refer [digraph-all-cycles to-digraph]]
   [ferent.utils :refer [build-map]]
   [sc.api :refer :all]))

(defn- metrics-for-project [project-and-arrows-both-ways]
  "Return pair of project and metrics for that project"
  (let [[proj [arrowout arrowin]] project-and-arrows-both-ways
        arrow-out-count (count arrowout)
        arrow-in-count (count arrowin)]
    [proj {:arrow-in    arrow-in-count
           :arrow-out   arrow-out-count
           :instability (try (/ arrow-out-count (+ arrow-out-count arrow-in-count))
                             (catch ArithmeticException _ Double/NaN))}]))

(defn- by-project [{:keys [arrow-in arrow-out]}]
  "Rearrange the map:
  arrow-in and  arrow-out are maps from project-IDs to sequences of projects that are their dependees/dependencies.
  This function returns a map where the keys are all project-IDs in those two input maps,
  and the values are a map with keys :arrow-in and :arrow-out,
  each with the projects that are the dependees/dependencies of that one project."
  (build-map (set (apply concat (map keys [arrow-in arrow-out]))) (fn [p] [(arrow-out p) (arrow-in p)])))

(defn metrics [dependency-graph]
  "Return metrics, a map where keys are each project, plus keys for the global statistics :project-count and :cycles.
  Param dependency-graph can be generated with build-graph/build-graph"
  (let [metrics-for-projs (into {} (map metrics-for-project (by-project dependency-graph)))
        per-project-plus-global-stats
        (assoc metrics-for-projs
               :project-count (count metrics-for-projs)
               :cycles (digraph-all-cycles (to-digraph dependency-graph)))]
    (into (sorted-map-by #(compare (str %1) (str %2))) per-project-plus-global-stats)))
