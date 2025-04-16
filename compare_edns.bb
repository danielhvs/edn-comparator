#!/usr/bin/env bb

(ns compare-edns
  (:require
   [clojure.edn                 :as edn]
   [lambdaisland.deep-diff2     :as ddiff]))

(defn read-config
  "From an edn file to clj data"
  [config-file]
  (-> config-file
      slurp
      edn/read-string))

(defn vector->map-coll [v]
  (map-indexed
   (fn [index item]
     {index item})
   v))

(defn collect-all-keys
  "Returns a map with paths to where there are some keys"
  [v]
  (let [the-maps (vector->map-coll v)]
    (reduce
     (fn [acc entry]
       (let [the-value (second (first entry))
             the-key   (ffirst entry)]
         (cond (map? the-value)
               (assoc acc the-key the-value)
               (vector? the-value)
               (assoc acc the-key (collect-all-keys the-value))
               :else
               acc)))
     {}
     the-maps)))
(comment (collect-all-keys  [1 {:x 41 :a 0}
                             {:x 42}
                             [{:y 43} [9 8 {:zz 44} 10]]
                             3]))

(defn one-level-map? [m]
  (and (map? m) (empty? (filter map? (vals m)))))
(comment
  (one-level-map? {:x 2 :y 3})
  (one-level-map? {:x 2 :y 3 :z {:lol 42}}))

(defn compose-keys** [k v]
  (println "v:" v)
  (println "k:" k)
  (let [the-keys (keys v)]
    (set (map #(str k "." %)
              (into #{}
                    (for [k the-keys] (str k)))))))
(comment (compose-keys** 0 {:yy 42}))

(defn compose-keys* [[k v]]
  (println "one-level?" v)
  (cond (one-level-map? v)
        (compose-keys** k v)
        (map? v)
        (into #{} (map compose-keys* (update-keys v #(str k "." %))))
        :else
        #{k}))
(comment
  (compose-keys* [0 {:a 1 :b 2}])
  (compose-keys* [0 {1 {:a 2 :b 3}}])
  (compose-keys* [0 {1 {:a 2 :b 3}
                     2 {:x 4 :y 5}}])
  (compose-keys* [0 {1 {:a 2
                        :b 3
                        :c {:x 4 :y 5}}}]))

(defn compose-keys
  "Return a coll of sets"
  [m]
  (let [entries (seq m)]
    (map compose-keys* entries)))

(comment
  (compose-keys {:x 42})
  (compose-keys  {:x {:inner 42}
                  :y 99})
  (compose-keys {0 {:a 1 :b 2}})
  (compose-keys {3 {0 {:y 43}}})
  (compose-keys {1 {:x 41}, 2 {:x 42}, 3 {0 {:y 43}, 1 {2 {:zz 44}}}})
  (compose-keys (collect-all-keys  [1
                                    {:x 41}
                                    {:x 42}
                                    [{:y 43} [9 8 {:zz 44} 10]]
                                    3])))

(defn normalize
  "Main function: normalizes the config map. Returns a map to a set of keys."
  [m]
  (reduce (fn [acc [k the-value]]
            (assoc acc k
                   (cond (map? the-value)
                         (reduce conj (compose-keys the-value))
                         (vector? the-value)
                         (reduce conj (compose-keys (collect-all-keys the-value)))
                         :else
                         #{})))
          {}
          m))
(comment
  (normalize {:a 1 :x 42 :y [{:yy 43}]})
  (normalize {:x {:inner 42}
              :y 99})
  (normalize {:x [[[{:inner 42}]]]})
  (normalize {:x [1 [{:y 0}]]})
  (normalize {:x 42}))

(defn- select-config-keys [config]
  (-> config read-config normalize))

(defn config-diffs
  "Print the leftover keys from both files"
  [config1 config2]
  (-> (ddiff/diff (select-config-keys config2)
                  (select-config-keys config1))
      (ddiff/minimize)
      (ddiff/pretty-print)))
(comment (config-diffs "sample1.edn" "sample2.edn"))

(defn print-help []
  (println "Usage: ./compare_edn.bb <file1.edn> <file2.edn>"))

(defn -main
  [& [config1 config2]]
  (if (and config1 config2)
    (do
      (println "Comparing files"  config1  "and"  config2)
      (config-diffs config1 config2))
    (print-help)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
