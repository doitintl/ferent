(ns ferent.graph-building-test
  (:require [clojure.test :refer :all]
            [ferent.build-graph :refer :all]))

(def test-resources-dir "./test_resources")

(deftest test-build-graph
  (testing "p2 and p3 depend on p1, because p1 grants permissions to Service Account from p2 and p3"
    (is (= {:arrow-in  {"p1" #{"p2" "p3"}}
            :arrow-out {"p2" #{"p1"}
                        "p3"  #{"p1"}}}
           (build-graph {"p1" ["sa-p2" "sa-p3"]}
                        {"p2" ["sa-p2"] "p3" ["sa-p3"]}
                        false)))))

(deftest test-build-graph-with-testdata
  (testing "with fake data
project1 gives permission to sa-a-project2, so project2 depends on project1
(project1 gives permission to sa-a-project1, which is ignored as a self-dependency)
project1 gives permission to sa-a-project-unknown, so an unknown project depends on project1"

    (is (= {:arrow-out {"project2" #{"project1"}}
            :arrow-in  {"project1" #{"project2"}}}
           (build-graph-from-dir test-resources-dir false)))

    (is (= {:arrow-in  {"project1" #{"<UNKNOWN>"
                                     "project2"}}
            :arrow-out {"<UNKNOWN>" #{"project1"}
                        "project2"   #{"project1"}}}
           (build-graph-from-dir test-resources-dir true)))))
