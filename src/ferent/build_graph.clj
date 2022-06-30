(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap]]
            [sc.api :refer :all]))


(defn proj-for-service-accounts [service-accounts proj-to-serviceaccounts]
  "Get the projects associated with theseservice-accounts, given a mapping from proj-to-serviceaccounts."
  (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
    (map #(sa-to-proj %) service-accounts))
  )

(defn build-graph [project-to-sas-granted-role proj-to-its-sas]
  (let [

        ;todo improve this. Maybe  replace comp below with -> so that the order of the functions can make more sense
        arrow-in0 (map (comp
                         (fn [[proj deps]] [proj (set (remove #(= proj %) deps))]) ;remove self-dependency
                         (fn [[proj deps]] [proj (filter #(some? %) deps)]) ;remove nil, where the project of the SA was not found
                         (fn [[proj service-accounts]]      ;get proj of the SA that was granted a role
                           [proj (proj-for-service-accounts service-accounts proj-to-its-sas)]))
                       project-to-sas-granted-role)
        ;todo use -> for the following
        arrow-in (into {} (remove (fn [[_ v]] (empty? v)) arrow-in0))
        ]

    {:arrow-in  arrow-in
     :arrow-out (invert-multimap arrow-in)}))
