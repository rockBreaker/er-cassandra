(ns er-cassandra.key)

(defn make-sequential
  [v]
  (cond (nil? v) v
        (sequential? v) v
        :else [v]))

(defn partition-key
  "given a primary key spec, return the partition key,
   which is the first element of the primary key spec"
  [key]
  (let [key (make-sequential key)]
    (make-sequential (first key))))

(defn extract-key-value*
  ([key record-or-key-value {:keys [key-value]}]
   (let [key (flatten (make-sequential key)) ;; flatten partition key
         key-value (or (make-sequential key-value)
                       (if-not (map? record-or-key-value)
                         (make-sequential record-or-key-value)
                         (repeat (count key) nil)))
         record (when (map? record-or-key-value)
                  record-or-key-value)
         dkv (map (fn [k ev]
                    (or ev (get record k)))
                  key
                  key-value)]
     (when (and
            (= (count key) (count key-value))
            (not (some nil? dkv)))
       dkv))))

(defn extract-key-value
  "extract a key value from some combination of explicit value
   and a record"

  ([key record-or-key-value]
   (extract-key-value key record-or-key-value {}))

  ([key record-or-key-value {:keys [collection] :as opts}]
   (extract-key-value* key record-or-key-value opts)))

(defn key-equality-clause
  [key key-value]
  (let [key (flatten (make-sequential key))
        key-value (make-sequential key-value)]
    (mapv (fn [k v]
            (if (sequential? v)
              [:in k v]
              [:= k v]))
          key
          key-value)))

(defn extract-key-equality-clause
  "returns a Hayt key equality clause for use in a (when...) form"

  ([key record-or-key-value]
   (extract-key-equality-clause key record-or-key-value {}))

  ([key record-or-key-value opts]
   (let [key (make-sequential key)
         kv (extract-key-value key record-or-key-value opts)]
     (key-equality-clause key kv))))

(defn extract-collection-key-components
  [coll record-or-key-value]
  (cond
    (map? coll) (filter identity (keys coll))
    (sequential? coll) (filter identity coll)
    (set? coll) (disj coll nil )
    :else (throw (ex-info
                  "not a supported key collection"
                  {:coll coll
                   :record-or-key-value record-or-key-value}))))

(defn extract-key-value-collection
  ([key record-or-key-value]
   (extract-key-value-collection key record-or-key-value {}))

  ([key record-or-key-value opts]
   (when-let [kv (not-empty
                  (extract-key-value* key record-or-key-value opts))]
     (let [pre (into [] (take (dec (count kv)) kv))
           coll (last kv)
           lkvs (extract-collection-key-components coll opts)]
       (->> lkvs
            (map (fn [lkv] (conj pre lkv )))
            set)))))
