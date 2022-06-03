(ns ferent.metrics
  (:require [clojure.data.csv :as csv]
            [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  pairs-to-multimap]]
            [sc.api :refer :all])
  (:import (clojure.lang PersistentVector)))

(defn- metric-for-project  [x]
  "Return pairs of project and metrics for that project"
  (let [[proj [arrowout  arrowin]] x
        arrowout-count (count arrowout)
        arrowin-count (count arrowin)]
    [proj {:arrowin     arrowin-count
           :arrowout    arrowout-count
           :instability (try (/ arrowout-count (+ arrowout-count arrowin-count))
                             (catch ArithmeticException ae 0))}]))

(defn by-project [analysis]
  (let [arrowin (analysis :arrowin)
        arrowout (analysis :arrowout)
        projects (set (concat (keys arrowin) (keys arrowout)))
        pairs (for [k projects] [k [(vec (arrowout k)) (vec (arrowin k))]])]
    (into {} pairs)))

(defn metrics [analysis]
  (let [proj-analysis (dissoc analysis :projects)
        by-proj (by-project proj-analysis)
        metrics-for-projects (into {} (map metric-for-project by-proj))]
    (assoc metrics-for-projects :project-count (count metrics-for-projects))))
