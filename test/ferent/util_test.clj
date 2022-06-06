(ns ferent.util-test
  (:require [clojure.test :refer :all]
            [ferent.core :refer :all]
            [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  pairs-to-multimap
                                  twolevel-sort
                                  rotate-to-lowest]]))

(deftest test-utilities
  (testing "invert-multimap"
    (is (= {1 #{:a} 2 #{:a} 3 #{:a :b} 4 #{:b} 5 #{:b}}
           (invert-multimap {:a #{1 2 3} :b [3 4 5]})))) \

  (testing "invert-invertible-map"
    (is (= {1 :a 2 :b 3 :c 4 :d}
           (invert-invertible-map {:a [1] :b [2] :c [3] :d [4]})))
    (is (= {1 :a 2 :b 22 :b 11 :a}
           (invert-invertible-map {:a #{1 11} :b [2 22]})))
    (is (thrown? AssertionError "Each value should be associated with 1 and only 1 key"
                 (invert-invertible-map {:a [1] :b [1] :c [2] :d [1]}))))

  (testing "pairs-to-multimap"
    (is (= {:a #{1 11} :b #{2}}
           (pairs-to-multimap [[:a 1] [:b 2] [:a 11]]))))

  (testing "twolevel-sort"
    (is (= [[1 2 3] [1 5 6]]
           (twolevel-sort  [[6 1 5] [3 2  1]]))))
  (testing "rotate-to-lowest "
    (is (= ["p1" "p2" "p3"] (rotate-to-lowest ["p2"
                                               "p3"
                                               "p1"])))))


