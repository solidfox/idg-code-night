(ns game-of-life.core
  (:require [ysera.test :refer [is= is is-not]]))

; A function declaration
; defn [name doc-string? attr-map? [params*] prepost-map? body]
;
; if [test then else?]
; (if (= 3 3) "That's true" "This will never happen") => "That's true"
(if (= 1 2) 1 3)
; pos? [num]
; (pos? 5) => true
; (pos? -1) => false

(defn abs                                                   ; name
  "The absolute value of a number."                         ; doc-string?
  {:test (fn []
           (is= (abs 5) 5)
           (is= (abs -2) 2)
           (is= (abs 0) 0))}                                ; attr-map?
  [x]                                                       ; [params*]
  {:pre [(number? x)]}                                      ;  prepost-map?
  (if (pos? x)
    x
    (- x)))


; Higher order functions
; map [f coll*]
;
; (map inc [1 3 7]) => [2 4 8]
;
; (map (fn [x] (inc x)) [1 3 7]) => [2 4 8]
;
; (map inc [1 3 7] [2 4 9]) => Wrong number of args (2) passed to inc
;
; (map + [1 3 7] [2 4 9]) => [3 7 16]
;
; (map (fn [x y] (+ x y)) [1 3 7] [2 4 9]) => [3 7 16]
;
;
; apply [f coll]
;
; (apply max [4 2 3 -1]) => 4
;
; (max 4 2 3 -1) => 4

(map inc [1 3 4])
(map (fn [x] (+ x 1)) [1 3 4])

(map + [1 2 3] [2 5 7])

(defn distance
  "Calculates the distance between two cells."
  {:test (fn []
           (is= (distance [0 0] [0 0]) 0)
           (is= (distance [1 1] [3 1]) 2)
           (is= (distance [1 1] [3 3]) 2)
           (is= (distance [1 1] [1 3]) 2)
           (is= (distance [4 3] [1 1]) 3))}
  [cell-1 cell-2]
  (->> (map - cell-1 cell-2)
       (map abs)
       (apply max)))

(defn neighbours?
  "Determines if cells are neighbours."
  {:test (fn []
           (is-not (neighbours? [0 0] [5 5]))
           (is-not (neighbours? [0 0] [0 0]))
           (is (neighbours? [0 0] [0 1]))
           (is (neighbours? [0 0] [1 1])))}
  [cell-1 cell-2]
  (= (distance cell-1 cell-2) 1))

;; To map with an index, use map-indexed [f coll]
;; then f is two variable function of index and item.

(apply + (map inc (filter even? [1 2 3 4 5 6 7 8])))

(->> [1 2 3 4 5 6 7 8]
     (filter even?)
     (map inc)
     (apply +))

(defn create-state
  "Creates the state model for the game."
  {:test (fn []
           (is= (create-state " #  "
                              "####"
                              "  # ")
                {:living-cells #{[1 0]
                                 [0 1] [1 1] [2 1] [3 1]
                                 [2 2]}
                 :hovered-cells #{}})
           (is= (create-state " *  "
                              "****"
                              "  * ")
                {:living-cells  #{}
                 :hovered-cells #{[1 0]
                                  [0 1] [1 1] [2 1] [3 1]
                                  [2 2]}}))}
  [& strings]
  {:living-cells  (->> strings
                       (map-indexed (fn [y string]
                                      (map-indexed (fn [x character]
                                                     (when (= character \#)
                                                       [x y]))
                                                   string)))
                       (apply concat)
                       (remove nil?)
                       (into #{}))
   :hovered-cells (->> strings
                       (map-indexed (fn [y string]
                                      (->> string
                                           (map-indexed (fn [x character]
                                                          (when (= character \*)
                                                            [x y]))))))
                       (apply concat)
                       (remove nil?)
                       (into #{}))})

(defn neighbours-of
  "Returns a set of all cells that are neighbours to the given cell."
  {:test (fn []
           (is= (neighbours-of [0 0])
                #{[-1 -1] [0 -1] [1 -1] [-1 0] [1 0] [-1 1] [0 1] [1 1]})
           (is= (neighbours-of [2 2])
                #{[1 1] [2 1] [3 1] [1 2] [3 2] [1 3] [2 3] [3 3]}))}
  [cell]
  (->> [[-1 -1] [0 -1] [1 -1] [-1 0] [1 0] [-1 1] [0 1] [1 1]]
       (map (fn [direction]
              (map + cell direction)))
       (into #{})))

(defn cell-hovered?
  "Determines if the given cell is hovered."
  {:test (fn []
           (let [state (create-state "##"
                                     " *")]
             (is (cell-hovered? state [1 1]))
             (is-not (cell-hovered? state [1 0]))
             (is-not (cell-hovered? state [0 0]))
             (is-not (cell-hovered? state [0 1]))))}
  [state cell]
  (contains? (:hovered-cells state) cell))

(defn cell-alive?
  "Determines if the given cell is alive."
  {:test (fn []
           (let [state (create-state "# ")]
             (is (cell-alive? state [0 0]))
             (is-not (cell-alive? state [1 0]))
             (is-not (cell-alive? state [-10 -20]))))}
  [state cell]
  (contains? (:living-cells state) cell))

(defn alive-neighbours-of
  {:test (fn []
           (is= (-> (create-state "##"
                                  " #")
                    (alive-neighbours-of [0 0]))
                #{[1 0] [1 1]}))}
  [state cell]
  (->> (neighbours-of cell)
       (filter (fn [c]
                 (cell-alive? state c)))
       (into #{})))
;
;; 1 Any live cell with fewer than two live neighbours dies, as if caused by underpopulation.
;; 2 Any live cell with two or three live neighbours lives on to the next generation.
;; 3 Any live cell with more than three live neighbours dies, as if by overpopulation.
;; 4 Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.

(defn next-generation
  {:test (fn []
           (let [state (create-state "##"
                                     "##")]
             (is= (next-generation state)
                  state))
           (is= (-> (create-state "   "
                                  "###"
                                  "   ")
                    (next-generation))
                (create-state " # "
                              " # "
                              " # ")))}
  [state]
  (update state :living-cells
          (fn [living-cells]
            (->> living-cells
                 (map neighbours-of)
                 (apply concat)
                 (distinct)
                 (filter (fn [cell]
                           (let [number-of-alive-neighbours (count (alive-neighbours-of state cell))]
                             (cond (and (not (cell-alive? state cell))
                                        (= number-of-alive-neighbours 3))
                                   true

                                   (and (cell-alive? state cell)
                                        (or (= number-of-alive-neighbours 2)
                                            (= number-of-alive-neighbours 3)))
                                   true

                                   :else
                                   false))))
                 (into #{})))))



(defn toggle-alive
  {:test (fn []
           (is (-> (create-state "")
                   (toggle-alive [0 0])
                   (cell-alive? [0 0])))
           (is-not (-> (create-state "#")
                       (toggle-alive [0 0])
                       (cell-alive? [0 0]))))}
  [state cell]
  (update state :living-cells
          (fn [living-cells]
            (if (cell-alive? state cell)
              (clojure.set/difference living-cells #{cell})
              (conj living-cells cell)))))

(defn unhover
  {:test (fn []
           (is-not (-> (create-state "*")
                       (unhover [0 0])
                       (cell-hovered? [0 0]))))}
  [state cell]
  (println "Unhovering")
  (update state :hovered-cells
          (fn [hovered-cells]
            (clojure.set/difference hovered-cells #{cell}))))

(defn hover
  {:test (fn []
           (is (-> (create-state "")
                   (hover [0 0])
                   (cell-hovered? [0 0]))))}
  [state cell]
  (update state :hovered-cells
          (fn [hovered-cells]
            (conj hovered-cells cell))))









