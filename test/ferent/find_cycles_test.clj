(ns ferent.find-cycles-test
  (:require [clojure.test :refer :all]
            [ferent.cycles :refer [find-cycles]]))

(def adj-map1 {"x" ["x"]
               "a" ["b"]
               "b" ["a"]
               "c" ["d"]
               "d" ["e"]
               "e" ["f"]
               "f" ["c"]
               "p" ["q"]
               "q" ["r"]
               "r" ["s"]
               "s" ["p"]})

(def adj-map2 {"a" ["b"],
               "b" ["a"]
               "c" ["a"]})

(def adj-map3 {"a" ["b"],
               "b" ["a"]})

(def adj-map4 {"a" #{"b"}
               "b" #{"c" "d"}
               "c" #{"a"}
               "d" #{"c"}})

(deftest test-find-cycles
  (testing "Find Cycles"
    (is (=
         [["x"] ["a" "b"] ["c" "d" "e" "f"] ["p" "q" "r" "s"]]
         (find-cycles adj-map1)))
    (is (=
         [["a" "b"]]
         (find-cycles adj-map2)))
    (is (=
         [["a" "b"]]
         (find-cycles adj-map3)))
    (is (=
         [["a" "b" "c"] ["a" "b" "c" "d"]]
         (find-cycles adj-map4)))))





