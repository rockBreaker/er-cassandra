(ns er-cassandra.session)

(defprotocol Session
  (execute [this statement])
  (close [this]))

(defprotocol SpySession
  (spy-log [this]))
