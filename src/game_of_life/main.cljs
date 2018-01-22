(ns game-of-life.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :refer [render]]
            [cljs.core.async :refer [<! chan]]
            [game-of-life.core :refer [create-state
                                       next-generation
                                       toggle-alive
                                       unhover
                                       hover]]
            [game-of-life.component.app :refer [app-component]]))

(enable-console-print!)

(defn handle-event [state-timeline {name :name data :data}]
  (println "Name" name data)
  (cond (= name :next-generation)
        (conj state-timeline (next-generation (first state-timeline)))

        (= name :cell-click)
        (conj state-timeline (toggle-alive (first state-timeline) data))

        (= name :prev-generation)
        (drop 1 state-timeline)

        (= name :cell-enter)
        (let [[head & tail] state-timeline]
          (conj tail (hover head data)))

        (= name :cell-leave)
        (let [[head & tail] state-timeline]
          (conj tail (unhover head data)))
        ))

(defn render! [state event-chan]
  (render [app-component {:state      state
                          :event-chan event-chan}]
                    (js/document.getElementById "app")))

(defn event-loop [state-timeline, event-chan]
  (render! (first state-timeline) event-chan)
  (go (event-loop
    (handle-event state-timeline (<! event-chan))
    event-chan)))

(def initial-state (create-state ""
                                 "###  "
                                 "  ###"
                                 " ### "
                                 "  #  "))

(go (event-loop (list initial-state) (chan)))