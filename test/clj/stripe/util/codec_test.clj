(ns stripe.util.codec-test
  (:require [stripe.util.codec :as codec]
            [clojure.test :refer :all]))


(deftest test-form-encode
  (testing "strings"
    (are [x y] (= (codec/form-encode x) y)
      "foo bar" "foo+bar"
      "foo+bar" "foo%2Bbar"
      "foo/bar" "foo%2Fbar")
    (is (= (codec/form-encode "foo/bar" "UTF-16") "foo%FE%FF%00%2Fbar")))
  (testing "maps"
    (are [x y] (= (codec/form-encode x) y)
      {"a" "b"}                        "a=b"
      {:a "b"}                         "a=b"
      {"a" 1}                          "a=1"
      {"a" nil}                        "a="
      {"a" "&"}                        "a=%26"
      {"&" "a"}                        "%26=a"
      {"a" "b" "c" "d"}                "a=b&c=d"
      {"a" "b c"}                      "a=b+c"
      {"a" ["b"]}                      "a[]=b"
      {"a" ["b c"]}                    "a[]=b+c"
      {"a" ["b" "c"]}                  "a[]=b&a[]=c"
      {"a" ["c" "b"]}                  "a[]=c&a[]=b"
      {"a" ["b" nil]}                  "a[]=b&a[]="
      {"&" ["a" "b"]}                  "%26[]=a&%26[]=b"
      {"a" (seq [1 2])}                "a[]=1&a[]=2"
      {:a (seq [{:b "c"} {:d "e f"}])} "a[0][b]=c&a[1][d]=e+f"
      {"a" #{"c" "b"}}                 "a[]=b&a[]=c"
      {:a {"b" "c"}}                   "a[b]=c"
      {"a" {"b" "c"}}                  "a[b]=c"
      {"&" {"a" "b"}}                  "%26[a]=b"
      {:a {:nil nil}}                  "a[nil]="
      {:a {:b "c"}}                    "a[b]=c"
      {:a {:b {:c {:d "e?"}}}}         "a[b][c][d]=e%3F"
      {"&" [{:a "b c"}]}               "%26[0][a]=b+c"
      {"a" [{:b "c"} {:d "e"}]}        "a[0][b]=c&a[1][d]=e")
    (is (= (codec/form-encode {"a" "foo/bar"} "UTF-16") "a=foo%FE%FF%00%2Fbar"))))

(deftest future-tests
  (is (= "a[0][a][c]=5&a[0][a][d][0][a]=1&a[0][a][d][1][b]=2"
         (codec/form-encode {:a [{:a {:c 5 :d [{:a 1} {:b 2}]}}]})))
  (is (= "a[b][c][0][d]=e&a[b][c][1][f]=g"
         (codec/form-encode {:a {:b {:c [{:d "e"} {:f "g"}]}}})))
  (is (= "a[b][]=c&a[b][]=d+e"
         (codec/form-encode {:a {:b ["c" "d e"]}}))))
