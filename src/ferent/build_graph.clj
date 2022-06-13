(ns ferent.build-graph
  (:require [clojure.data.csv :as csv]
            [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  pairs-to-multimap]]
            [sc.api :refer :all]))

(set! *warn-on-reflection* true)

(defn build-graph [permissions proj-to-serviceaccounts show-unknown]
  (let [unknown "<UNKNOWN>"
        proj-for-service-accounts (fn [service-accounts proj-to-serviceaccounts]
                                    (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
                                      (map #(get sa-to-proj % unknown) service-accounts)))
        ;todo Should I somehow replace comp below with -> so that the order of the functions can make more sense
        arrowin-with-empties (map (comp
                                    (fn [[proj dep]] [proj (set (remove #(= proj %) dep))]) ;remove self-dependency
                                    (fn [[proj service-accounts]] ;remove UNKNOWN if flag is set
                                      [proj (remove
                                              (fn [sa] (if show-unknown false (= unknown sa))) service-accounts)])
                                    (fn [[proj service-accounts]] ;get proj based on service account
                                      [proj (proj-for-service-accounts service-accounts proj-to-serviceaccounts)]))
                                  permissions)
        arrowin (into {} (remove (fn [[_ v]] (empty? v))    ; remove mapping where val is empty
                                 arrowin-with-empties))
        arrowout (invert-multimap arrowin)]
    {:arrowin  arrowin
     :arrowout arrowout}))

(defn build-graph-from-dir
  ([resources-dir show-unknown]
   (letfn [(load-csv [local-file-path] (csv/read-csv (slurp (str resources-dir local-file-path))))
           (load-to-multimap [local-file-path] (pairs-to-multimap (load-csv local-file-path)))]
     (build-graph (load-to-multimap "/permissions_granted_by_project.csv")
                  (load-to-multimap "/sa_in_project.csv")
                  show-unknown)))

  ([resources-dir]
   (build-graph-from-dir resources-dir true)))