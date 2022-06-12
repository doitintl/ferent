(ns ferent.metric-test
  (:require [clojure.test :refer :all]
            [ferent.build-graph :refer :all]
            [ferent.metrics :refer [metrics]]))

(deftest metrics-test
  (testing "p2 and p3 each depend on one other (p1), so each has 1 arrow-out.
  p1 has two projects that depend on it, and so has 2 arrow-in.
  p2 and p3 are relatively unstable because they depend on another project."
    (is (= {"p1"           {:arrowin 2 :arrowout 0 :instability 0}
            "p2"           {:arrowin 0 :arrowout 1 :instability 1}
            "p3"           {:arrowin 0 :arrowout 1 :instability 1}
            :project-count 3
            :cycles        []}
           (metrics {:arrowin  {"p1" #{"p2" "p3"}}
                     :arrowout {"p2" #{"p1"}
                                "p3" #{"p1"}}})))))

(deftest cycle-test
  (testing "a cycle" (is (= [["p1"   "p2"   "p3"]]
                            ((metrics
                              {:arrowin  {"p3" #{"p2"}, "p2" #{"p1"}, "p1" #{"p3"}},
                               :arrowout {"p1" #{"p2"}, "p2" #{"p3"}, "p3" #{"p1"}}}) :cycles)))))

(deftest  cycles-from-raw-data
  (testing "get cycles from the raw data"
    (is (= [["p1"   "p2"  "p3"]]
           ((metrics (build-graph {"p3" ["sa-p2"] "p2" ["sa-p1"] "p1" ["sa-p3"]}
                                  {"p2" ["sa-p2"] "p3" ["sa-p3"] "p1" ["sa-p1"]}
                                  false))  :cycles)))))