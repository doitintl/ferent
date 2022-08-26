(ns ferent.build-graph
  (:require [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  remove-keys-with-empty-val
                                  update-vals-in-kv]]
            [sc.api :refer :all]))

(defn- proj-for-service-accounts [service-accounts proj-to-serviceaccounts]
  "Get the projects associated with these service-accounts, given a mapping from proj-to-serviceaccounts."
  (let [sa-to-proj (invert-invertible-map proj-to-serviceaccounts)]
    (map #(sa-to-proj %) service-accounts)))

(defn- proj-to-dependees [project-to-sas-granted-role proj-to-its-sas]
  "Return a map of proj to its dependees, but may include self-dependency and empty-sets"
  (update-vals project-to-sas-granted-role
               ;remove those where the project of the SA was not found
               (comp #(remove nil? %) #(proj-for-service-accounts % proj-to-its-sas))))

(defn remove-self-dependencies [proj-to-dependees]
  (update-vals-in-kv (fn [proj deps] (set (remove #{proj} deps)))
                     proj-to-dependees))

(defn arrows-both-ways [arrow-in]
  {:arrow-in  arrow-in
   :arrow-out (invert-multimap arrow-in)})

(defn build-graph [project-to-sas-granted-role proj-to-its-sas]
  (-> (proj-to-dependees project-to-sas-granted-role proj-to-its-sas)
      remove-self-dependencies
      remove-keys-with-empty-val
      arrows-both-ways))

