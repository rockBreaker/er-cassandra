(ns er-cassandra.model.alia.lookup-test
  (:require
   [er-cassandra.model.util.test :as tu
    :refer [fetch-record insert-record delete-record upsert-instance]]
   [clojure.test :as test :refer [deftest is are testing use-fixtures]]
   [schema.test :as st]
   [clj-uuid :as uuid]
   [clojure.string :as str]
   [manifold.deferred :as d]
   [er-cassandra.session.alia :as als]
   [er-cassandra.record :as r]
   [er-cassandra.model.util.timestamp :as ts]
   [er-cassandra.model.types :as t]
   [er-cassandra.model.alia.lookup :as l]))

(use-fixtures :once st/validate-schemas)
(use-fixtures :each (tu/with-model-session-fixture))

(deftest stale-lookup-key-values-for-table-test
  (let [m (t/create-entity
           {:primary-table {:name :foos :key [:id]}
            :lookup-tables [{:name :foos_by_bar :key [:bar]}
                                {:name :foos_by_baz
                                 :key [:baz]
                                 :collections {:baz :set}}]})]

    (testing "ignores lookup keys when missing from new-record"
      (is (empty?
           @(l/stale-lookup-key-values-for-table
             tu/*model-session*
             m
             {:id :a :bar :b}
             {:id :a}
             (-> m :lookup-tables first)))))

    (testing "treats nil new-record as deletion for singular key values"
      (is (= [[:b]]
             @(l/stale-lookup-key-values-for-table
               tu/*model-session*
               m
               {:id :a :bar :b}
               nil
               (-> m :lookup-tables first)))))

    (testing "correctly identifies a stale singular lookup key values"
      (is (= [[:b]]
             @(l/stale-lookup-key-values-for-table
               tu/*model-session*
               m
               {:id :a :bar :b}
               {:id :a :bar nil}
               (-> m :lookup-tables first)))))

    (testing "correctly identifiers stale collection lookup key values"
      (is (= #{[:b] [:d]}
             (set
              @(l/stale-lookup-key-values-for-table
                tu/*model-session*
                m
                {:id :a :baz #{:b :c :d}}
                {:id :a :baz #{:c}}
                (-> m :lookup-tables second))))))

    (testing "correctly identifiers stale collection lookup key values on delete"
      (is (= #{[:b] [:c] [:d]}
             (set
              @(l/stale-lookup-key-values-for-table
                tu/*model-session*
                m
                {:id :a :baz #{:b :c :d}}
                nil
                (-> m :lookup-tables second))))))))

(defn generator-fn-lookup-test-generator
  [session entity table old-record
   {id :id
    r-name :name
    :as record}]
  (for [s (str/split r-name #"\s+")]
    {:id id :name s}))

(defn create-generator-fn-lookup-entity
  []
  (tu/create-table
   :generator_fn_lookup_test
   "(id uuid primary key, name text)")
  (tu/create-table
   :generator_fn_lookup_test_by_name
   "(name_char text primary key, id uuid)")
  (t/create-entity
   {:primary-table {:name :generator_fn_lookup_test :key [:id]}
    :lookup-tables [{:name :generator_fn_lookup_test_by_name_char
                     :key [:name]
                     :generator-fn generator-fn-lookup-test-generator}]}))


(deftest lookup-record-generator-fn-test
  (let [m (create-generator-fn-lookup-entity)
        id (uuid/v1)

        lookups (l/generate-lookup-records-for-table
                 tu/*model-session*
                 m
                 (-> m :lookup-tables first)
                 nil
                 {:id id
                  :name "foo bar baz"})]

    (testing "creates the expected lookups"
      (is (= #{{:id id :name "foo"}
               {:id id :name "bar"}
               {:id id :name "baz"}}
             (set lookups))))))
