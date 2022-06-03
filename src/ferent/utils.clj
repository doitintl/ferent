(ns ferent.utils
  (:require [sc.api :refer :all]))

(defn pairs-to-multimap [seq-of-pairs]
  (let [grouped-pairs-by-key (group-by first (sort seq-of-pairs))
        vec-pairs-of-key-and-valuelist (for [[k v] grouped-pairs-by-key] [k (sort (map second v))])
        as-map (into {} vec-pairs-of-key-and-valuelist)]
    (into {} (for [[k vs] as-map] [k (set vs)]))))

(defn invert-multimap [multimap]
  (let [inverse (for [[k lst] multimap] (for [i lst] [i k]))
        flattened (apply concat inverse)]
    (pairs-to-multimap flattened)))

(defn invert-invertible-map [multimap]
  (let [inverse-multimap (invert-multimap multimap)]
    (assert (every? #(>= 1 (count %)) (vals inverse-multimap))
            (str "Each value should be associated with 1 and only 1 key"
                 inverse-multimap))

    (into {} (for [[k v] inverse-multimap] [k (first v)]))))
