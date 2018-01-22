(ns game-of-life.component.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [game-of-life.core :refer [cell-alive? cell-hovered?]]
            [cljs.core.async :refer [>!]]))

(defn create-cell [coordinates state viewport-size event-chan]
  [:div {:key            (first coordinates)
         :on-mouse-enter (fn [] (go (>! event-chan {:name :cell-enter
                                                :data coordinates})))
         :on-mouse-leave (fn [] (go (>! event-chan {:name :cell-leave
                                                :data coordinates})))
         :on-click       (fn [] (go (>! event-chan {:name :cell-click
                                           :data coordinates})))
         :style          {:position         "relative"
                          :display          "inline-block"
                          :width            (str (/ 100 viewport-size) "%")
                          :border           "1px solid white"
                          :background-color "lightgray"
                          :padding-bottom   (str (/ 100 viewport-size) "%")}}
   [:div {:style {:position         "absolute"
                  :background-color "tomato"
                  :top              "12.5%"
                  :left             "12.5%"
                  :border-radius    "50%"
                  :height           "75%"
                  :width            "75%"
                  :transition       "all 500ms ease"
                  :transform        (cond
                                      (cell-alive? state coordinates)
                                      (if (cell-hovered? state coordinates)
                                        "scale(0.8)"
                                        "scale(1)")
                                      (cell-hovered? state coordinates)
                                      "scale(0.3)"
                                      true
                                      "scale(0)")}}]])

(defn create-cell-row [y state viewport-width event-chan]
  (println (str "Create row of width " viewport-width))
  [:div {:class "row"
         :key   y
         :style {:display "flex"}}
   (map (fn [x] (create-cell [x y] state viewport-width event-chan)) (range viewport-width))])

(defn app-component [{state      :state
                      event-chan :event-chan}]
  [:div
   [:h1 {:style {:text-align "center"}}
    "Game of life"]
   [:div
    (let [viewport-height 10
          viewport-width 10]
      (map (fn [y] (create-cell-row y state viewport-width event-chan))
           (range viewport-height)))]
   [:button {:on-click (fn []
                         (go (>! event-chan {:name :prev-generation})))}
    "PREV"]
   [:button {:on-click (fn []
                         (go (>! event-chan {:name :next-generation})))}
    "NEXT"]])
