(ns ferent.cycles
  (:require
    [ferent.utils :refer [rotate-to-lowest sort-and-dedupe]]))

(defn- depth-first-search [node path adjacency-map cycles]
  (cond
    (= node (first path))
    [(rotate-to-lowest path)]
    (some #{node} path)
    []
    :else
    (concat cycles
            (filter not-empty
                    (mapcat
                      (fn [child] (depth-first-search child (conj path node) adjacency-map cycles))
                      (get adjacency-map node []))))))

(defn find-cycles [adj-map]
  (vec (sort-and-dedupe (reduce concat (for [node (keys adj-map)] (depth-first-search node [] adj-map []))))))

