(ns ferent.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.climate.claypoole :as cp]
            [sc.api])
  (:import (java.io IOException PushbackReader)))

(def thread-count
  "Highly IO-bound processes should get more threads than we have processors.
  The multiplier below  empirically gave the best results in a few tests."
  (* 5 (.. Runtime getRuntime availableProcessors)))

(defn pairs-to-multimap [seq-of-pairs]
  (let [grouped-pairs-by-key (group-by first (sort seq-of-pairs))
        vec-pairs-of-key-and-valuelist (for [[k v] grouped-pairs-by-key] [k (sort (map second v))])
        as-map (into {} vec-pairs-of-key-and-valuelist)]
    (into {} (for [[k vs] as-map] [k (set vs)]))))

(defn invert-multimap [multimap]
  (let [inverse (for [[k sequence] multimap] (for [i sequence] [i k]))
        flattened (apply concat inverse)]
    (pairs-to-multimap flattened)))

(defn invert-invertible-map [multimap]
  "Invert a multimap; however, if the elements across all vals of the multimap are
  not unique, throw an error."
  (let [inverse-multimap (invert-multimap multimap)]
    (assert (every? #(>= 1 (count %)) (vals inverse-multimap))
            (str "Each value should be associated with 1 and only 1 key. Duplicates: "
                 (remove #(>= 1 (count %)) (vals inverse-multimap))))
    ; replace   v (which is a seq if length1)  with its first member
    (into {} (for [[k v] inverse-multimap] [k (first v)]))))

(defn twolevel-sort [sequence-sequence]
  "For a nested list of lists, sort each internal list and sort across all lists."
  (sort (map (comp vec sort) sequence-sequence)))

(defn rotate-to-lowest [sequence]
  "Rotate sequence so that the 0th element is (the first appearance of) its minimal element."
  (let [indexed (map-indexed vector sequence)
        minimal-element (reduce (fn [a b] (if (>= 0 (compare (second a) (second b))) a b)) indexed)]
    ;(sc.api/spy)
    (vec (take (count sequence) (drop (first minimal-element) (cycle sequence))))))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (PushbackReader. r)))
    (catch IOException e
      (throw (Exception. (str "Couldn't open " (.getMessage e)))))
    (catch RuntimeException e
      (throw (Exception. (str "Error parsing edn file " (.getMessage e)))))))

(defn pfilter [pred coll]
  "Filter in parallel"
  (map first (filter second (map vector coll (cp/pmap (cp/threadpool thread-count) pred coll)))))

; TODO could make the following lazy
(defn paginated-query [query-function]
  "Repeatedly call query-function.
  The query-function should return a map with keys :items and :token.
  If key :token has value nil, stop the iteration. If it is non-nil, it is passed
  into the  query-function on the next iteration."
  (loop [pagination-token nil
         accumulator []]
    (let [{items                       :items
           pagination-token-from-query :token} (query-function pagination-token)
          accumulated-items (concat accumulator items)]
      (if (nil? pagination-token-from-query)
        accumulated-items
        (recur pagination-token-from-query accumulated-items)))))

(defn remove-keys-with-empty-val [map-or-sequence-of-pairs]
  "remove items were the second element is empty"
  (into {} (remove (fn [[_ v]] (empty? v))) map-or-sequence-of-pairs))

(defmacro timer
  "Evaluates expr and prints the time it took.  Returns the value of
 expr. Copied from clojure.core/time, but allows you to provide a tag"
  [tag expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (.println *err* (str ~tag ": " (Math/round (/ (double (- (. System (nanoTime)) start#)) 1000000000.0)) " seconds"))
     ret#))

(defn build-map [sequence f]
  "Build a map where the keys are the elements of sequence and the values are f applied to that key."
  (into {} (map (fn [k] [k (f k)]) sequence)))

(defn sort-and-dedupe [vec-vec]
  (dedupe (twolevel-sort vec-vec)))

(defn update-vals-in-kv
  "Returns a map with the same keys as `m` and with the values transformed by `f`. `f` must be a `2-ary` function that receives the key and the value as arguments.
~~~klipse
  (update-vals-in-kv list {:a 1 :b 2 :c 3})
~
  "
  [f m]
  (into {} (map (fn [[a b]] [a (f a b)]) m)))
