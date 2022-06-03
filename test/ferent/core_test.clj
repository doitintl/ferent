(ns ferent.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [ferent.core :refer :all]
            [ferent.metrics :refer [metrics]]
            [ferent.utils :refer [invert-invertible-map
                                  invert-multimap
                                  pairs-to-multimap]]))

(def test-resources-dir "./test_resources")
(def resources-dir "./resources")

(deftest test-analyze
  (testing "p2 and p3 depend on p1, because p1 grants permissions to Service Account from p2 and p3"
    (is (= {:arrowin  {:p1 #{:p2 :p3}}
            :arrowout {:p2 #{:p1}
                       :p3 #{:p1}}}
           (analyze {:p1 [:sa-p2 :sa-p3]}
                    {:p2 [:sa-p2] :p3 [:sa-p3]}
                    false)))))
(deftest test-metrics
  (testing "p2 and p3 each depend on one other (p1), so each has 1 arrow-out.
  p1 has two projects that depend on it, and so has 2 arrow-in.
  p2 and p3 are relatively unstable because they depend on another project."
    (is (= {:p1            {:arrowin 2 :arrowout 0 :instability 0}
            :p2            {:arrowin 0 :arrowout 1 :instability 1}
            :p3            {:arrowin 0 :arrowout 1 :instability 1}
            :project-count 3}
           (metrics {:arrowin  {:p1 #{:p2 :p3}}
                     :arrowout {:p2 #{:p1}
                                :p3 #{:p1}}})))))

(deftest test-analyze-with-testdata
  (testing "with fake data
project1 gives permission to sa-a-project2, so project2 depends on project1
(project1 gives permission to sa-a-project1, which is ignored as a self-dependency)
project1 gives permission to sa-a-project-unknown, so an unknown project depends on project1"

    (is (= {:arrowout {"project2" #{"project1"}}
            :arrowin  {"project1" #{"project2"}}}
           (analyze-dir test-resources-dir false)))

    (is (= {:arrowin  {"project1" #{"<UNKNOWN>"
                                    "project2"}}
            :arrowout {"<UNKNOWN>" #{"project1"}
                       "project2"  #{"project1"}}}
           (analyze-dir test-resources-dir true))))

  (testing "with data based on  a real project, where dependencies are only between
   projects owned by the same person (using the name as a prefix) like
        {:arrowin  {\"fred-proj1\"     #{\"fred-proj2}
        :arrowout  {\"fred-proj2\"     #{\"fred-proj1}} "
    (let [pfx (fn [s] (first (str/split s #"-")))
          analysis (analyze-dir resources-dir false)
          [arrowin-sample-k arrowin-sample-set] (first (vec (analysis :arrowin)))
          arrowin-sample-k-pfx (pfx arrowin-sample-k)
          arrowin-sample-v-pfx (pfx (first arrowin-sample-set))]
       (assert (= arrowin-sample-k-pfx arrowin-sample-v-pfx)
              (str "Expect dependencies only between related projects, which
                         in the sample always start with same prefix before dash. Found "
                   arrowin-sample-k-pfx
                   " and "
                   arrowin-sample-v-pfx)))))

(deftest test-utilities
  (testing "invert-multimap"
    (is (= {1 #{:a} 2 #{:a} 3 #{:a :b} 4 #{:b} 5 #{:b}}
           (invert-multimap {:a #{1 2 3} :b [3 4 5]}))))
  (testing "invert-invertible-map"
    (is (= {1 :a 2 :b 3 :c 4 :d}
           (invert-invertible-map {:a [1] :b [2] :c [3] :d [4]})))
    (is (= {1 :a 2 :b 22 :b 11 :a}
           (invert-invertible-map {:a #{1 11} :b [2 22]})))
    (is (thrown? AssertionError "Each value should be associated with 1 and only 1 key"
                 (invert-invertible-map {:a [1] :b [1] :c [2] :d [1]}))))

  (testing "pairs-to-multimap"
    (is (= {:a #{1 11} :b #{2}}
           (pairs-to-multimap [[:a 1] [:b 2] [:a 11]])))))