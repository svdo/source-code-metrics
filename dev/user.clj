(ns user
  (:require [crux.api :as crux])
  (:import crux.api.ICruxAPI))

(comment
  ;; Start Crux system in-memory
  (def ^crux.api.ICruxAPI system
    (crux/start-standalone-system {:kv-backend "crux.kv.memdb.MemKv"
                                   :db-dir "data/db-dir-1"
                                   :event-log-dir "data/eventlog-1"}))

  ;; Start Crux system with RocksDB
  (def ^crux.api.ICruxAPI system
    (crux/start-standalone-system {:kv-backend "crux.kv.rocksdb.RocksKv"
                                   :db-dir "data/db-dir-1"
                                   :event-log-dir "data/eventlog-1"}))

  ;; Submit a transaction
  (crux/submit-tx
    system
    [[:crux.tx/put
      {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
       :name "Pablo"
       :last-name "Picasso"}
      #inst "2018-05-18T09:20:27.966-00:00"]]) ; valid time

  ;; Query
  (crux/q (crux/db system)
          '{:find [e]
            :where [[e :name "Pablo"]]})

  ;; Entity Query
  (crux/entity (crux/db system) :dbpedia.resource/Pablo-Picasso)

  ;; Delete
  (crux/submit-tx
    system
    [[:crux.tx/delete :dbpedia.resource/Pablo-Picasso
      #inst "2019-01-01"]])

  (crux/entity (crux/db system) :dbpedia.resource/Pablo-Picasso) ; -> nil
  (crux/entity (crux/db system #inst "2018-10-10") :dbpedia.resource/Pablo-Picasso) ; -> pablo

  ;; Evict
  (crux/submit-tx
    system
    [[:crux.tx/evict :dbpedia.resource/Pablo-Picasso]])

  (crux/entity (crux/db system #inst "2018-10-10") :dbpedia.resource/Pablo-Picasso) ; -> nil

  ;; Stop Crux system
  (.close system))
