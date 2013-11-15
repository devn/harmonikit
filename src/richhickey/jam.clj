(ns richhickey.jam
  (:require [richhickey.harmonikit :as h]
            [overtone.music.pitch :as opitch]
            [overtone.music.time :as otime]
            [overtone.sc.buffer :as obuf]
            [overtone.sc.node :as onode]
            [seesaw.core :as score]
            [seesaw.font :as sfont]
            [seesaw.graphics :as sgfx]
            [seesaw.keystroke :as skeystroke]
            [seesaw.dev :as sdev]))

;; Capslock Keyboard
(def all-chars (shuffle (map char (range 32 127))))

(def simple-degrees [:i :ii :iii :iv :v :vi :vii])

(defn octavize-degrees [oct]
  (map #(keyword (str (name %) oct)) simple-degrees))

(def degs (flatten (map octavize-degrees [3 4])))

(def char-to-note-map
  (zipmap all-chars (cycle simple-degrees)))

(defn lookup-degree [c]
  (get char-to-note-map c))

(defn rand-scale []
  (rand-nth [:major :minor :pentatonic]))

(defn rand-root []
  (rand-nth
   (map
    (comp keyword #(str % (int (+ 3 (rand 3)))) char)
    (range 97 104))))

(def num-notes (atom 0))
(def current-key-and-root (atom {:scale :major, :root :Eb4}))

(defn play-character [c]
  (let [scale     (:scale @current-key-and-root)
        root      (:root @current-key-and-root)
        note-list (opitch/degrees->pitches (list (lookup-degree c)) scale root)]
    (if (= @num-notes 12)
      (do (reset! num-notes 0)
          (swap! current-key-and-root assoc :root (rand-root))
          (swap! current-key-and-root assoc :scale (rand-scale)))
      (swap! num-notes inc))
    ;; (h/harmonikit (obuf/buffer-id h/b) note-list)
    (otime/apply-at (otime/now)
                    (partial h/harmonikit (obuf/buffer-id h/b))
                    note-list)))

(score/native!)
(def listener-label
  (score/label :text "This should be the note you just played, but it isn't...yet.",
               :background :black,
               :foreground "#eee"
               :enabled? true
               :focusable? true
               :listen [:key-pressed (fn [e]
                                       (let [c (.getKeyChar e)]
                                         (play-character c)
                                         (score/config! listener-label :text (str c " : "
                                                                                  (str (:scale @current-key-and-root))
                                                                                  " : "
                                                                                  (str (:root @current-key-and-root))))))
                        :key-released (fn [e]
                                        (onode/ctl h/harmonikit :gate 0))]
               :font (sfont/font :name :monospaced
                                 :style #{:bold :italic}
                                 :size 42)))

(def f (score/frame :title "capslock keyboard",
                    :on-close :exit,
                    :content listener-label,
                    :size [640 :by 480]))

(-> f score/show!)

(comment
  (onode/ctl (h/harmonikit (obuf/buffer-id h/b) 70) :amp 50)
  (onode/ctl h/harmonikit :gate 0)
  (osrv/stop))
