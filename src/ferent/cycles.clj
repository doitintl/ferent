(ns ferent.cycles
  (:require
   [ferent.utils :refer [rotate-to-lowest sort-and-dedupe]]))

(defn- dfs [node path-so-far adj-map cycles]
  (cond
    (= node (first path-so-far))
    [(rotate-to-lowest path-so-far)]
    (some #{node} path-so-far)
    []
    :else
    (vec
     (filter not-empty
             (concat cycles
                     (mapcat (fn [child] (dfs child (conj path-so-far node) adj-map, cycles)) (get adj-map node [])))))))

(defn find-cycles [adj-map]
  (vec (sort-and-dedupe (reduce concat (for [node (keys adj-map)] (dfs node [] adj-map []))))))

