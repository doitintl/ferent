(ns ferent.graph-cycles-test
  (:require [ferent.find-cycles :refer [digraph-all-cycles]]
            [ferent.utils :refer [twolevel-sort]]
            [clojure.test :refer :all]
            [loom.graph]))

(def sample-loom-graph (loom.graph/digraph ["a" "b"]
                                           ["b" "c"]
                                           ["b" "d"]
                                           ["c" "a"]
                                           ["d" "c"]))

(def sample-ferent-graph (ferent.find-cycles/digraph {:arrow-out
                                                      {"a" #{"b"}
                                                       "b" #{"c" "d"}
                                                       "c" #{"a"}
                                                       "d" #{"c"}}}))
(deftest test-convert-graph
  (testing " "
    (is (=
         (loom.graph/digraph ["p1" "p2"]
                             ["p1" "p3"]
                             ["p2" "p1"]
                             ["p3" "p1"])
         (ferent.find-cycles/digraph {:arrow-out {"p2" #{"p1"}
                                                  "p3"  #{"p1"}
                                                  "p1"  #{"p2" "p3"}}})))

    (is (=   sample-loom-graph
             sample-ferent-graph))))

(deftest test-digraph-all-cycles
  (testing " "
    (is (=
         (twolevel-sort [["a" "b" "c"] ["a" "b" "d" "c"]])
         (twolevel-sort (digraph-all-cycles (loom.graph/digraph ["a" "b"]
                                                                ["b" "c"]
                                                                ["b" "d"]
                                                                ["c" "a"]
                                                                ["d" "c"]))))))

  (is (=
       (twolevel-sort (digraph-all-cycles sample-loom-graph)))
      (twolevel-sort
       (digraph-all-cycles sample-ferent-graph))))



