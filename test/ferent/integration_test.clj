(ns ferent.integration-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [ferent.core :refer :all]
            [ferent.metrics :refer :all]))

(def resources-dir "./resources")

(deftest test-build-graph-with-testdata

  (testing "with data based on  a real project, where dependencies are only between
   projects owned by the same person (using the name as a prefix) like
        {:arrowin  {\"fred-proj1\"     #{\"fred-proj2}
        :arrowout  {\"fred-proj2\"     #{\"fred-proj1}} "

    (let [pfx (fn [s] (first (str/split s #"-")))
          analysis (build-graph-from-dir resources-dir false)
          [arrowin-sample-k arrowin-sample-set] (first (vec (analysis :arrowin)))
          arrowin-sample-k-pfx (pfx arrowin-sample-k)
          arrowin-sample-v-pfx (pfx (first arrowin-sample-set))]

      (assert (= arrowin-sample-k-pfx arrowin-sample-v-pfx)
              (str "Expect dependencies only between related projects, which
                         in the sample always start with same prefix before dash. Found "
                   arrowin-sample-k-pfx
                   " and "
                   arrowin-sample-v-pfx)))))
