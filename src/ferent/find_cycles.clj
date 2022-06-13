(ns
  ^{:doc "Code from https://github.com/npcoder2k14/loom/blob/master/src/loom/alg.cljc"}
  ferent.find-cycles
  (:require [ferent.utils :refer [rotate-to-lowest]]
            [loom.graph :refer [directed? nodes successors]]))
(set! *warn-on-reflection* true)

(defn digraph [arrows]
  (apply loom.graph/digraph (apply concat (map (fn [[k vals]] (map (fn [v] [k v]) vals)) (arrows :arrowout)))))

(defn- insert-in-blocked-map
  "Helper function for digraph-all-cycles.
  When cycle is not found insert current node in
  blocked-map of all of its children"
  [cycle-data curr children]
  (reduce (fn [{:keys [bmap] :as acc} child]
            (if (contains? bmap child)
              (update-in acc [:bmap child] conj curr)
              (assoc-in acc [:bmap child] #{curr})))
          cycle-data children))

(defn- unblock-nodes
  "Helper function for digraph-all-cycles.
  Unblock nodes from bset and bmap."
  [{:keys [bmap] :as cycle-data} curr unblocked]
  (if (contains? unblocked curr)
    cycle-data
    (as-> cycle-data cd
          (update cd :bset disj curr)
          (reduce (fn [acc node-to-unblock]
                    (unblock-nodes acc node-to-unblock (conj unblocked curr)))
                  cd (get bmap curr))
          (update cd :bmap dissoc curr))))

(defn- find-all-cycles
  "Helper function for digraph-all-cycles.
  Returns all cycles originating from a point 'start'"
  [g start curr cycle path rset bset bmap]
  (as-> {:cycle?     cycle
         :all-cycles []
         :bset       (conj bset curr)
         :rset       rset
         :bmap       bmap} cycle-data
        (reduce
          (fn [{:keys [bset rset bmap] :as acc} child]
            (cond
              (= child start) (-> acc
                                  (assoc :cycle? true)
                                  (update :all-cycles conj path))

              ;; Since cycle is found
              (or (contains? rset child)
                  (contains? bset child)) acc

              :else
              (let [new-acc (find-all-cycles g start child false (conj path child)
                                             rset bset bmap)]
                (-> new-acc
                    (update :cycle? #(or %1 %2) (:cycle? acc))
                    (update :all-cycles concat (:all-cycles acc))))))
          ;; Function end
          cycle-data (successors g curr))
        (if (:cycle? cycle-data)
          ;; Last argument is unblocked set to avoid
          ;; unblock-nodes function going into infinite loop
          (unblock-nodes cycle-data curr #{})
          (insert-in-blocked-map cycle-data curr (successors g curr)))))

(defn digraph-all-cycles [grph]
  "This function returns all simple cycles present in a directed graph.
Implemented algorithm as mentioned in
 https://www.cs.tufts.edu/comp/150GA/homeworks/hw1/Johnson%2075.PDF
"
  (letfn [(make-digraph [g] (if (directed? g)
                              (as-> {:ans [] :rset #{}} cycle-data
                                    (reduce (fn [{:keys [ans rset]} curr]
                                              (let [{:keys [all-cycles rset]}
                                                    (find-all-cycles g curr curr false [curr] rset #{} {})
                                                    updated-rset (conj rset curr)]
                                                {:ans  (concat ans all-cycles)
                                                 :rset updated-rset}))

                                            cycle-data (nodes g))
                                    (:ans cycle-data))
                              ::not-a-directed-graph))]
    (sort (map rotate-to-lowest (make-digraph grph)))))