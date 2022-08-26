(ns ferent.find-cycles-test
  (:require [clojure.test :refer :all]
            [ferent.cycles :refer [find-cycles]]))

(deftest test-find-cycles
  (testing "Find Cycles"
    (are [expect adj-map]
         (= expect (find-cycles adj-map))
      [["a" "b"]]
      {"a" ["b"], "b" ["a"] "c" ["a"]}
      [["a" "b"]]
      {"a" ["b"], "b" ["a"]}
      [["a" "b" "c"] ["a" "b" "c" "d"]]
      {"a" #{"b"} "b" #{"c" "d"} "c" #{"a"} "d" #{"c"}}
      [["x"] ["a" "b"] ["c" "d" "e" "f"] ["p" "q" "r" "s"]]
      {"x" ["x"] "a" ["b"] "b" ["a"] "c" ["d"] "d" ["e"] "e" ["f"]
       "f" ["c"] "p" ["q"] "q" ["r"] "r" ["s"] "s" ["p"]})))
