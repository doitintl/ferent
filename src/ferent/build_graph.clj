(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  remove-keys-with-empty-val]]
            [sc.api :refer :all]))

(defn- proj-for-service-accounts [service-accounts proj-to-serviceaccounts]
  "Get the projects associated with these service-accounts, given a mapping from proj-to-serviceaccounts."
  (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
    (map #(sa-to-proj %) service-accounts)))

(defn build-graph [project-to-sas-granted-role proj-to-its-sas]
  (let [arrow-in0 (; map of proj to its dependees, but may include self-dependency and empty-sets
                    update-vals project-to-sas-granted-role
                                (comp #(remove nil? %)      ;remove  those where the project of the SA was not found
                                      #(proj-for-service-accounts % proj-to-its-sas)) ;get proj of the SA that was granted a role
                                )
        no-self-deps (map (fn [[proj deps]] [proj (set (remove #(= proj %) deps))]) arrow-in0)
        arrow-in (remove-keys-with-empty-val no-self-deps)]

    {:arrow-in  arrow-in
     :arrow-out (invert-multimap arrow-in)}))




