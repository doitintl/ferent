(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap]]
            [sc.api :refer :all]))

(defn build-graph [permissions proj-to-serviceaccounts ]
  (let [unknown "<UNKNOWN>"
        proj-for-service-accounts (fn [service-accounts proj-to-serviceaccounts]
                                    (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
                                      (map #(get sa-to-proj % unknown) service-accounts)))
        ;todo improve ths. Maybe  replace comp below with -> so that the order of the functions can make more sense
        arrowin-with-empties (map (comp
                                    (fn [[proj dep]] [proj (set (remove #(= proj %) dep))]) ;remove self-dependency
                                    (fn [[proj service-accounts]] ;remove UNKNOWN if flag is set
                                      [proj (remove
                                              (fn [sa] (= unknown sa)) service-accounts)])
                                    (fn [[proj service-accounts]] ;get proj based on service account
                                      [proj (proj-for-service-accounts service-accounts proj-to-serviceaccounts)]))
                                  permissions)
        arrow-in (into {} (remove (fn [[_ v]] (empty? v))    ; remove mapping where val is empty
                                 arrowin-with-empties))
        arrow-out (invert-multimap arrow-in)]
    {:arrow-in  arrow-in
     :arrow-out arrow-out}))
