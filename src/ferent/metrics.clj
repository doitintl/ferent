(ns ferent.metrics
  (:require [clojure.data.csv :as csv]
            [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  pairs-to-multimap]]
            [sc.api :refer :all]))

(defn- metric-for-project [[proj [arrowout arrowin]]]
  [proj {:arrowin  (count arrowin)
         :arrowout (count arrowout)}])
(defn by-project [analysis]
  (let [depees (analysis :arrowin)
        arrowout (analysis :arrowout)
        ks (set (concat (keys depees) (keys arrowout)))
        pairs (for [k ks] [k [(vec (arrowout k)) (vec (depees k))]])]
    (into {} pairs)))

(defn metrics [analysis]

  (let [by-proj (by-project analysis)]
    (into {} (map metric-for-project by-proj))))
