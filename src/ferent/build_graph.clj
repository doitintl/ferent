(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap]]
            [sc.api :refer :all])
  (:import (clojure.lang PersistentHashSet)))


(defn proj-for-service-accounts [service-accounts proj-to-serviceaccounts]
  "Get the projects associated with theseservice-accounts, given a mapping from proj-to-serviceaccounts."
  (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
    (map #(sa-to-proj %) service-accounts))
  )

(defn build-graph [project-to-sas-granted-role proj-to-its-sas]
  (let [

        ;todo improve ths. Maybe  replace comp below with -> so that the order of the functions can make more sense
        arrowin-with-empties (map (comp
                                    (fn [[proj deps]] [proj (set (remove #(= proj %) deps))]) ;remove self-dependency
                                    (fn [[proj deps]] [proj (filter #(some? %) deps)]) ;remove nil
                                    (fn [[proj service-accounts]] ;get proj based on service account
                                      [proj (proj-for-service-accounts service-accounts proj-to-its-sas)]))
                                  project-to-sas-granted-role)]
    (prn arrowin-with-empties)
    {:arrow-in  (into {} (remove (fn [[_ ^PersistentHashSet v]] (empty? v))    ; remove mapping where val is empty
                                 arrowin-with-empties))
     :arrow-out (invert-multimap (into {} (remove (fn [[_ v]] (empty? v)) ; remove mapping where val is empty
                                                  arrowin-with-empties)))}))
