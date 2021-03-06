(ns er-cassandra.model.alia.upsert-test
  (:require
   [er-cassandra.model.util.test :as tu
    :refer [fetch-record insert-record delete-record upsert-instance]]
   [clojure.test :as test :refer [deftest is are testing use-fixtures]]
   [schema.test :as st]
   [clj-uuid :as uuid]
   [er-cassandra.session.alia :as als]
   [er-cassandra.record :as r]
   [er-cassandra.model.util.timestamp :as ts]
   [er-cassandra.model.types :as t]
   [er-cassandra.model.alia.lookup :as l]
   [er-cassandra.model.alia.upsert :as u]))

(use-fixtures :once st/validate-schemas)
(use-fixtures :each (tu/with-model-session-fixture))

(defn create-simple-entity
  []
  (tu/create-table :simple_upsert_test
                   "(id timeuuid primary key, nick text)")
  (t/create-entity
   {:primary-table {:name :simple_upsert_test :key [:id]}}))

(defn create-secondary-entity
  []
  (tu/create-table :secondary_upsert_test
                   "(org_id timeuuid, id timeuuid, nick text, primary key (org_id, id))")
  (tu/create-table :secondary_upsert_test_by_nick
                   "(org_id timeuuid, nick text, id timeuuid, primary key (org_id, nick))")
  (t/create-entity
   {:primary-table {:name :secondary_upsert_test :key [:org_id :id]}
    :secondary-tables [{:name :secondary_upsert_test_by_nick
                        :key [:org_id :nick]}]}))

(defn create-mixed-lookup-entity
  []
  (tu/create-table
   :upsert_mixed_lookup_test
   "(org_id timeuuid, id timeuuid, nick text, email set<text>, phone list<text>, stuff text,  primary key (org_id, id))")
  (tu/create-table
   :upsert_mixed_lookup_test_by_nick
   "(nick text, org_id timeuuid, id timeuuid, primary key (org_id, nick))")
  (tu/create-table
   :upsert_mixed_lookup_test_by_email
   "(email text primary key, org_id timeuuid, id timeuuid)")
  (tu/create-table
   :upsert_mixed_lookup_test_by_phone
   "(phone text primary key, org_id timeuuid, id timeuuid)")
  (t/create-entity
   {:primary-table {:name :upsert_mixed_lookup_test :key [:org_id :id]}
    :lookup-tables [{:name :upsert_mixed_lookup_test_by_nick
                         :key [:org_id :nick]}
                        {:name :upsert_mixed_lookup_test_by_email
                         :key [:email]
                         :collections {:email :set}}
                        {:name :upsert_mixed_lookup_test_by_phone
                         :key [:phone]
                         :collections {:phone :list}}]}))

(defn create-lookup-and-secondaries-entity
  []
  (tu/create-table
   :upsert_lookup_and_secondaries_test
   "(org_id timeuuid, id timeuuid, nick text, email set<text>, phone list<text>, stuff text, thing text, primary key (org_id, id))")
  (tu/create-table
   :upsert_lookup_and_secondaries_test_by_nick
   "(nick text, org_id timeuuid, id timeuuid, primary key (org_id, nick))")
  (tu/create-table
   :upsert_lookup_and_secondaries_test_by_email
   "(email text primary key, org_id timeuuid, id timeuuid)")
  (tu/create-table
   :upsert_lookup_and_secondaries_test_by_phone
   "(phone text primary key, org_id timeuuid, id timeuuid)")
  (tu/create-table
   :upsert_lookup_and_secondaries_test_by_thing
   "(org_id timeuuid, id timeuuid, nick text, email set<text>, phone list<text>, stuff text, thing text, primary key (org_id, thing))"
   )
  (t/create-entity
   {:primary-table {:name :upsert_lookup_and_secondaries_test :key [:org_id :id]}
    :lookup-tables [{:name :upsert_lookup_and_secondaries_test_by_nick
                         :key [:org_id :nick]}
                        {:name :upsert_lookup_and_secondaries_test_by_email
                         :key [:email]
                         :collections {:email :set}}
                        {:name :upsert_lookup_and_secondaries_test_by_phone
                         :key [:phone]
                         :collections {:phone :list}}]
    :secondary-tables [{:name :upsert_lookup_and_secondaries_test_by_thing
                        :key [:org_id :thing]}]}))

(defn create-unique-lookup-secondaries-entity
  []
  (tu/create-table
   :upsert_unique_lookup_secondaries_test
   "(org_id timeuuid, id timeuuid, nick text, email set<text>, phone list<text>, thing text, title text, tag set<text>, dept list<text>, stuff text, primary key (org_id, id))")

  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_nick
   "(nick text, org_id timeuuid, id timeuuid, primary key (org_id, nick))")
  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_email
   "(email text primary key, org_id timeuuid, id timeuuid)")
  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_phone
   "(phone text primary key, org_id timeuuid, id timeuuid)")

  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_thing
   "(org_id timeuuid, id timeuuid, nick text, email set<text>, phone list<text>, thing text, title text, tag set<text>, dept list<text>, stuff text, primary key (org_id, thing))")

  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_title
   "(title text, org_id timeuuid, id timeuuid, primary key (org_id, title))")
  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_tag
   "(tag text primary key, org_id timeuuid, id timeuuid)")
  (tu/create-table
   :upsert_unique_lookup_secondaries_test_by_dept
   "(dept text primary key, org_id timeuuid, id timeuuid)")

  (t/create-entity
   {:primary-table {:name :upsert_unique_lookup_secondaries_test :key [:org_id :id]}
    :unique-key-tables [{:name :upsert_unique_lookup_secondaries_test_by_nick
                         :key [:org_id :nick]}
                        {:name :upsert_unique_lookup_secondaries_test_by_email
                         :key [:email]
                         :collections {:email :set}}
                        {:name :upsert_unique_lookup_secondaries_test_by_phone
                         :key [:phone]
                         :collections {:phone :list}}]
    :secondary-tables [{:name :upsert_unique_lookup_secondaries_test_by_thing
                        :key [:org_id :thing]}]
    :lookup-tables [{:name :upsert_unique_lookup_secondaries_test_by_title
                         :key [:org_id :title]}
                        {:name :upsert_unique_lookup_secondaries_test_by_tag
                         :key [:tag]
                         :collections {:tag :set}}
                        {:name :upsert_unique_lookup_secondaries_test_by_dept
                         :key [:dept]
                         :collections {:dept :list}}]}))


(deftest delete-index-record-test
  (let [m (create-simple-entity)
        id (uuid/v1)
        _ (tu/insert-record :simple_upsert_test {:id id :nick "foo"})
        [status
         detail
         reason] @(u/delete-index-record tu/*model-session*
                                         m
                                         (:primary-table m)
                                         [id]
                                         (ts/default-timestamp-opt))]
    (is (= :ok status))
    (is (= {:table :simple_upsert_test
            :key [:id]
            :key-value [id]} detail))
    (is (= :deleted reason))
    (is (= nil (fetch-record :simple_upsert_test :id id)))))

(deftest insert-index-record-test
  (let [m (create-simple-entity)
        id (uuid/v1)
        r {:id id :nick "foo"}
        [status
         record
         reason] @(u/insert-index-record tu/*model-session*
                                         m
                                         (:primary-table m)
                                         r
                                         (ts/default-timestamp-opt))]
    (is (= :ok status))
    (is (= r record))
    (is (= :upserted reason))
    (is (= r (fetch-record :simple_upsert_test :id id)))))

(deftest stale-secondary-key-value-test
  (let [m (t/create-entity
           {:primary-table {:name :foos :key [:id]}
            :secondary-tables [{:name :foos_by_bar :key [:bar]}]})
        st (-> m :secondary-tables first)]

    (testing "ignores secondary keys when missing from new-record"
      (is (= nil
             (u/stale-secondary-key-value
              m
              {:id :a :bar :b}
              {:id :a}
              (-> m :secondary-tables first)))))

    (testing "correctly identifies a stale singular secondary key value"
      (is (= [:b]
             (u/stale-secondary-key-value
              m
              {:id :a :bar :b}
              {:id :a :bar nil}
              (-> m :secondary-tables first)))))))

(deftest stale-secondary-key-value-with-null-non-key-cols-test
  (let [m (t/create-entity
           {:primary-table {:name :secondary_with_nil_non_key_cols_test :key [:id]}
            :secondary-tables [{:name :secondary_with_nil_non_key_cols_test_by_other_id
                                :key [:other_id :id]}]})
        [id other-id] [:an-id :an-other-id]]
    (is (= nil
           (u/stale-secondary-key-value
            m
            {:id id :other_id other-id :nick "foo"}
            {:id id :other_id other-id :nick nil}
            (-> m :secondary-tables first))))))

(deftest delete-stale-secondaries-test
  (let [m (create-secondary-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]
        r {:org_id org-id :id id :nick "foo"}
        _ (tu/insert-record :secondary_upsert_test_by_nick r)
        old-r (fetch-record :secondary_upsert_test_by_nick [:org_id :nick] [org-id "foo"])
        [status
         record
         reason] @(u/delete-stale-secondaries
                   tu/*model-session*
                   m
                   old-r
                   {:org_id org-id :id id :nick "bar"}
                   (ts/default-timestamp-opt))]
    (is (= r old-r))
    (is (= nil (fetch-record :secondary_upsert_test_by_nick [:org_id :nick] [org-id "foo"])))))

(defn create-secondary-entity-for-nil-non-key-cols
  []
  (tu/create-table :secondary_with_nil_non_key_cols_test
                   "(id uuid primary key, other_id uuid, nick text)")
  (tu/create-table :secondary_with_nil_non_key_cols_test_by_other_id
                   "(id timeuuid, other_id uuid, nick text, primary key (other_id, id))")
  (t/create-entity
   {:primary-table {:name :secondary_with_nil_non_key_cols_test :key [:id]}
    :secondary-tables [{:name :secondary_with_nil_non_key_cols_test_by_other_id
                        :key [:other_id :id]}]}))

;; unit test for a problem observed with relationship {:cascade :null}
;; it seems that if a record originally created with update is later
;; updated to have all nil non-pk cols then the record gets deleted
;; https://ajayaa.github.io/cassandra-difference-between-insert-update/
;; since this is undesirable for secondary tables, insert should be
;; used instead - this test ensures that insert is used
(deftest dont-delete-secondaries-with-nil-non-key-cols-test
  (let [m (create-secondary-entity-for-nil-non-key-cols)
        [id other-id] [(uuid/v1) (uuid/v1)]
        r {:id id :other_id other-id :nick "foo"}

        i1 (upsert-instance m r)
        f1 (fetch-record :secondary_with_nil_non_key_cols_test [:id] [id])
        fi1 (fetch-record :secondary_with_nil_non_key_cols_test_by_other_id
                          [:other_id :id] [other-id id])

        nnr (assoc r :nick nil)
        i2 (upsert-instance m nnr)

        f2 (fetch-record :secondary_with_nil_non_key_cols_test [:id] [id])
        fi2 (fetch-record :secondary_with_nil_non_key_cols_test_by_other_id
                          [:other_id :id] [other-id id])]
    (is (= r f1))
    (is (= r fi1))

    (is (= nnr f2))
    (is (= nnr fi2))))

(deftest upsert-secondaries-test
  (let [m (create-secondary-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]
        r {:org_id org-id :id id :nick "bar"}
        [status
         record
         reason] @(u/upsert-secondaries
                   tu/*model-session*
                   m
                   r
                   (ts/default-timestamp-opt))]
    (is (= r (fetch-record :secondary_upsert_test_by_nick [:org_id :nick] [org-id "bar"])))))


(deftest delete-stale-lookups-test
  (let [m (create-mixed-lookup-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]

        nick-foo-r {:org_id org-id :id id :nick "foo"}

        _ (insert-record :upsert_mixed_lookup_test_by_nick nick-foo-r)
        _ (is (= nick-foo-r (fetch-record
                             :upsert_mixed_lookup_test_by_nick
                             [:org_id :nick] [org-id "foo"])))

        email-foobar-r {:org_id org-id :id id :email "foo@bar.com"}
        _ (insert-record :upsert_mixed_lookup_test_by_email email-foobar-r)
        _ (is (= email-foobar-r (fetch-record
                                 :upsert_mixed_lookup_test_by_email
                                 [:email] ["foo@bar.com"])))
        email-foobaz-r {:org_id org-id :id id :email "foo@baz.com"}
        _ (insert-record :upsert_mixed_lookup_test_by_email email-foobaz-r)
        _ (is (= email-foobaz-r (fetch-record
                                 :upsert_mixed_lookup_test_by_email
                                 [:email] ["foo@baz.com"])))

        phone-123-r {:org_id org-id :id id :phone "123"}
        _ (insert-record :upsert_mixed_lookup_test_by_phone phone-123-r)
        _ (is (= phone-123-r (fetch-record
                              :upsert_mixed_lookup_test_by_phone
                              [:phone] "123")))

        phone-456-r {:org_id org-id :id id :phone "456"}
        _ (insert-record :upsert_mixed_lookup_test_by_phone phone-456-r)
        _ (is (= phone-456-r (fetch-record
                              :upsert_mixed_lookup_test_by_phone
                              [:phone] "456")))

        [status
         record
         reason] @(u/delete-stale-lookups
                   tu/*model-session*
                   m
                   {:org_id org-id :id id
                    :nick "foo"
                    :email #{"foo@bar.com" "foo@baz.com"}
                    :phone ["123" "456"]}
                   {:org_id org-id :id id
                    :nick "bar"
                    :email #{"foo@bar.com" "blah@wah.com"}
                    :phone ["123" "789"]}
                   (ts/default-timestamp-opt))]

    (is (= nil (fetch-record :upsert_mixed_lookup_test_by_nick
                             [:org_id :nick] [org-id "foo"])))
    (is (= email-foobar-r (fetch-record :upsert_mixed_lookup_test_by_email
                                        [:email] ["foo@bar.com"])))
    (is (= nil (fetch-record :upsert_mixed_lookup_test_by_email
                             [:email] ["foo@baz.com"])))
    (is (= phone-123-r (fetch-record :upsert_mixed_lookup_test_by_phone
                                     [:phone] "123")))
    (is (= nil (fetch-record :upsert_mixed_lookup_test_by_phone
                             [:phone] "456")))))




(deftest upsert-lookups-test
  (let [m (create-mixed-lookup-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]

        nick-foo-r {:org_id org-id :id id :nick "foo"}

        [status
         record
         reason] @(u/upsert-lookups
                   tu/*model-session*
                   m
                   nil
                   {:org_id org-id :id id
                    :nick "foo"
                    :email #{"foo@bar.com" "foo@baz.com"}
                    :phone ["123" "456"]}
                   (ts/default-timestamp-opt))]

    (is (= {:org_id org-id :id id :nick "foo"}
           (fetch-record :upsert_mixed_lookup_test_by_nick
                         [:org_id :nick] [org-id "foo"])))
    (is (= {:org_id org-id :id id :email "foo@bar.com"}
           (fetch-record :upsert_mixed_lookup_test_by_email
                         [:email] ["foo@bar.com"])))
    (is (= {:org_id org-id :id id :email "foo@baz.com"}
           (fetch-record :upsert_mixed_lookup_test_by_email
                         [:email] ["foo@baz.com"])))
    (is (= {:org_id org-id :id id :phone "123"}
           (fetch-record :upsert_mixed_lookup_test_by_phone
                         [:phone] "123")))
    (is (= {:org_id org-id :id id :phone "456"}
           (fetch-record :upsert_mixed_lookup_test_by_phone
                         [:phone] "456")))))

(deftest copy-unique-keys-test
  (let [m (t/create-entity
           {:primary-table {:name :upsert_copy_unique_keys_test :key [:org_id :id]}
            :unique-key-tables [{:name :upsert_copy_unique_keys_test_by_nick
                                 :key [:org_id :nick]}
                                {:name :upsert_copy_unique_keys_test_by_email
                                 :key [:email]
                                 :collections {:email :set}}
                                {:name :upsert_copy_unique_keys_test_by_phone
                                 :key [:phone]
                                 :collections {:phone :list}}]})
        [org-id id] [(clj-uuid/v1) (clj-uuid/v1)]
        r {:org_id org-id
           :id id
           :nick "foo"
           :email #{"foo@bar.com" "foo@baz.com"}
           :phone ["123" "456"]}]
    (is (= (select-keys r [:nick :email :phone])
           (u/copy-unique-keys
            m
            r
            {})))))

(deftest has-lookups?-test
  (let [m (t/create-entity
           {:primary-table {:name :has_lookups_test :key [:org_id :id]}
            :unique-key-tables [{:name :has_lookups_test_by_nick
                                 :key [:org_id :nick]}]})]
    (is (= false (u/has-lookups? m))))
  (let [m (t/create-entity
           {:primary-table {:name :has_lookups_test :key [:org_id :id]}
            :lookup-tables [{:name :has_lookups_test_by_nick
                                 :key [:org_id :nick]}]})]
    (is (= true (u/has-lookups? m))))
  (let [m (t/create-entity
           {:primary-table {:name :has_lookups_test :key [:org_id :id]}
            :secondary-tables [{:name :has_lookups_test_by_nick
                                :key [:org_id :nick]}]})]
    (is (= true (u/has-lookups? m)))))

(deftest update-secondaries-and-lookups-test
  (let [m (create-lookup-and-secondaries-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]]

    (testing "first insert"
      (let [record {:org_id org-id
                    :id id
                    :nick "foo"
                    :stuff "whateva"
                    :thing "innit"
                    :email #{"foo@bar.com" "foo@baz.com"}
                    :phone ["123" "456"]}
            r @(u/update-secondaries-and-lookups
                tu/*model-session*
                m
                nil
                record
                (ts/default-timestamp-opt))]
        (is (= {:org_id org-id :id id :nick "foo"}
               (fetch-record :upsert_lookup_and_secondaries_test_by_nick
                             [:org_id :nick] [org-id "foo"])))
        (is (= record
               (fetch-record :upsert_lookup_and_secondaries_test_by_thing
                             [:org_id :thing] [org-id "innit"])))
        (is (= {:org_id org-id :id id :email "foo@bar.com"}
               (fetch-record :upsert_lookup_and_secondaries_test_by_email
                             [:email] ["foo@bar.com"])))
        (is (= {:org_id org-id :id id :email "foo@baz.com"}
               (fetch-record :upsert_lookup_and_secondaries_test_by_email
                             [:email] ["foo@baz.com"])))
        (is (= {:org_id org-id :id id :phone "123"}
               (fetch-record :upsert_lookup_and_secondaries_test_by_phone
                             [:phone] "123")))
        (is (= {:org_id org-id :id id :phone "456"}
               (fetch-record :upsert_lookup_and_secondaries_test_by_phone
                             [:phone] "456")))))))

(deftest upsert*-test
  (let [m (create-unique-lookup-secondaries-entity)
        [org-id id] [(uuid/v1) (uuid/v1)]

        record {:org_id org-id
                :id id

                :stuff "whateva"

                :nick "foo"
                :email #{"foo@bar.com" "foo@baz.com"}
                :phone ["123" "456"]

                :thing "blah"

                :title "mr"
                :tag #{"quick" "slow"}
                :dept ["hr" "dev"]}

        _ @(u/upsert* tu/*model-session* m record (ts/default-timestamp-opt))]

    (is (= record (fetch-record :upsert_unique_lookup_secondaries_test
                                [:org_id :id] [org-id id])))))

(deftest upsert*-if-not-exists-test
  (let [m (create-simple-entity)
        id (uuid/v1)

        record-foo {:id id :nick "foo"}
        record-bar {:id id :nick "bar"}]

    (testing "insert record-foo if-not-exists"
      (let [[r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-foo
                    (ts/default-timestamp-opt
                     {:if-not-exists true}))]
        (is (= record-foo r))))

    (testing "doesn't insert record-bar if-not-exists"
      (let [[r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-bar
                    (ts/default-timestamp-opt
                     {:if-not-exists true}))

            fr (fetch-record :simple_upsert_test :id id)]
        (is (= nil r))
        (is (= record-foo fr))))))

(deftest upsert*-if-exists-test
  (let [m (create-simple-entity)
        id (uuid/v1)

        record-foo {:id id :nick "foo"}
        record-bar {:id id :nick "bar"}

        _ (insert-record :simple_upsert_test record-bar)]

    (testing "upsert record-foo if-exists and it does exist"
      (let [[r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-foo
                    (ts/default-timestamp-opt
                     {:if-exists true}))
            fr (fetch-record :simple_upsert_test :id id)]
        (is (= record-foo fr))))

    (testing "doesn't insert record-bar if-exists and it doesn't exist"
      (let [_ (delete-record :simple_upsert_test :id id)

            [r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-bar
                    (ts/default-timestamp-opt
                     {:if-exists true}))

            fr (fetch-record :simple_upsert_test :id id)]
        (is (= nil r))
        (is (= nil fr))))))

(deftest upsert*-only-if-test
  (let [m (create-simple-entity)
        id (uuid/v1)

        record-foo {:id id :nick "foo"}
        record-bar {:id id :nick "bar"}

        _ (insert-record :simple_upsert_test record-bar)]

    (testing "upsert record-foo if condition holds"
      (let [[r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-foo
                    (ts/default-timestamp-opt
                     {:only-if [[:= :nick "bar"]]}))
            fr (fetch-record :simple_upsert_test :id id)]
        (is (= record-foo fr))))

    (testing "doesn't insert record-bar if condition fails"
      (let [[r e] @(u/upsert*
                    tu/*model-session*
                    m
                    record-bar
                    (ts/default-timestamp-opt
                     {:only-if [[:= :nick "bar"]]}))

            fr (fetch-record :simple_upsert_test :id id)]
        (is (= nil r))
        (is (= record-foo fr))))))
