(ns dialogs
  (:require [reduce-fsm :as fsm]
            config
            lang
            persist)
  (:import java.lang.Thread))


(def user-format-mod-m {:c-new-definition lang/format-mod-new-definition :c-not-found lang/format-mod-not-found})

(defn- build-response[args fsm-resp evaled-event fsm-id fsm-event]
  (let [rtm-event (:rtm-event fsm-event)
        full-resp (concat [fsm-resp]
                          (when (:c-disposition evaled-event)
                            [{:c-dispatch :c-post
                              :c-team (:team fsm-id)
                              :c-channel (:mod-channel config/config)
                              :c-text (((:c-disposition evaled-event) user-format-mod-m) (:name rtm-event) (:real_name rtm-event) args)}]))]
    (persist/log-response [fsm-id args rtm-event evaled-event full-resp])
    full-resp))

(defn- find-initial-ts-channel[acc]
  (let [rtm-event (->> acc (map :c-event) (filter :ack) first :rtm-event)]
    [(-> rtm-event :c-payload :channel) (-> rtm-event :reply :ts)]))

(defn- action-new [command args evaled-event fsm-id acc fsm-event from-state to-state]
  (let [fsm-resp {:c-dispatch :c-ackpost :c-channel (-> fsm-event :rtm-event :channel) :c-text (:c-text evaled-event) :c-context fsm-event}
        actions (build-response args fsm-resp evaled-event fsm-id fsm-event)]
    (conj acc {:c-event fsm-event :c-actions actions})))

(defn- action-edit [command args evaled-event fsm-id acc fsm-event from-state to-state]
  (let [[channel ts] (find-initial-ts-channel acc)
        fsm-resp {:c-dispatch :c-update :c-ts ts :c-channel channel :c-text (:c-text evaled-event)}
        actions (build-response args fsm-resp evaled-event fsm-id fsm-event)]
    (conj acc {:c-event fsm-event :c-actions actions})))
;;TODO let mod know if somehting deleted/edited

(defn- action-delete [fsm-id acc fsm-event from-state to-state]
  (let [[channel ts] (find-initial-ts-channel acc)
        resp {:c-dispatch :c-delete :c-ts ts :c-channel channel}]
    (conj acc {:c-event fsm-event :c-actions [resp]})))

(defn- action-ignore [acc fsm-event from-state to-state]
  (conj acc {:c-event fsm-event :c-actions nil}))

;;TODO edit/delete for teach is tricky as we need to withdraw last definition
(fsm/defsm-inc sm-core
  [
   [:ready {:is-terminal false}
    {:command command :args args :c-evaled-event evaled-event :c-fsm-id fsm-id} -> {:action (partial action-new command args evaled-event fsm-id)} :ack-wait
    ]
   ;;TODO check not okay too
   [:ack-wait {:is-terminal false}
    ;;TODO it is possible (with throttling or without) that :edit/delete message will come before :ack - this is not handled yet and most likely will result in ignoring :edit/delete, unlikely to happen though
    {:ack :ok} -> {:action action-ignore} :responded
    ]
   [:responded {:is-terminal false};; this is only needed due to a bug in reduce-fsm
    {:command command :args args :c-evaled-event evaled-event :c-fsm-id fsm-id} -> {:action (partial action-edit command args evaled-event fsm-id)} :responded
    {:command :delete :c-fsm-id fsm-id} -> {:action (partial action-delete fsm-id)} :completed
    ;;TODO handle reacted
    ]
   [:completed {:is-terminal true}
    ]
   ]
  )

(defn mk-sm-core[] (atom (sm-core [])))

(defn run-fsm![c-fsm c-event] (swap! c-fsm fsm/fsm-event c-event))

;;TODO timeouts in general
;;TODO timeout: response disapearing too
;;TODO to cusotmer: it seems that it will take a bit more time to find out
;;TODO accomondation for fsm version, perhaps in path/file name
;;TODO communication between FSMs
;;TODO clover catching up with unanswered requests in case it is down
;;TODO add CHANGELOG, change rules
