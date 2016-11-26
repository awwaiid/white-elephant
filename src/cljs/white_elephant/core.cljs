(ns white-elephant.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [ajax.core :refer [GET POST]]
              [accountant.core :as accountant]))

(defn dlog [thing]
  (.log js/console thing)
  thing)


; ;; -------------------------
; ;; Views

; (def Swipe (reagent/adapt-react-class js/ReactSwipe))
; (def Swipe (reagent/adapt-react-class (aget js/ReactSwipe ReactSwipe)))
; (def Button (reagent/adapt-react-class (aget js/ReactBootstrap "Button")))


(defn about-page []
  ; (let [swipe (reagent/adapt-react-class js/ReactSwipe)]
  [:div
     [:center
      [:h1 "White Elephant Gift Selector"]
      [:a {:href "/intro"} "Intro"]
      " - "
      [:a {:href "/about"} "About"]
      " - "
      [:a {:href "/triage"} "Start triage!"]
      [:h2 "About: What is this thing?!"]]
     [:p "I (Brock - awwaiid@thelackthereof.org) work for blinq.com, and
         thought it would be cool to make a gift picker in my free time."]
     [:a { :href "https://github.com/awwaiid/white-elephant" } "Github"]
     " - "
     [:a { :href "https://twitter.com/awwaiid" } "@awwaiid"]])
     ; [:div
     ;  "Hello"
     ;  [swipe]
     ;  ]
     ; ]))
    ; [Swipe
    ;  [:div "pane1"]
    ;  [:div "pane2"]
    ;  [:div "pane3"]]

(defonce app-state (atom {
                          :text "Hello to all the really strange wonderful weird new world!"
                          :min-price 0
                          :max-price 2000
                          :possible-products []
                          :total-products 32
                          ; :product {
                          ;           :img ""
                          ;           :title ""
                          ;           :url ""
                          ;           :price ""
                          ;           }
                          :next-products []
                          }))

(def prefetch-count 10)

(declare next-product)

(defn prefetch-product-handler [response]
  ; (swap! app-state update-in [:next-products] conj response)
  (swap! app-state update-in [:next-products] concat [response])
  ; Pre-fetch the image, specifically
  (aset (js/Image.) "src" (get response :img))
  (if (not (@app-state :product)) (next-product)))

(defn prefetch-product []
  (GET "/random-product" {
                          :response-format :json
                          :keywords? true
                          :error-handler prefetch-product
                          :handler prefetch-product-handler}))

(defn prefetch-products []
  (if (< (count (@app-state :next-products)) prefetch-count)
    (dotimes [n (- prefetch-count (count (@app-state :next-products)))]
      (prefetch-product))))

(defn next-product []
  (let [product (first (@app-state :next-products))]
    (swap! app-state assoc :product product)
    (swap! app-state update-in [:next-products] rest)
    (prefetch-products)))

(defn seq-contains? [coll target] (some #(= target %) coll))

(defn save-product []
  (if (@app-state :product)
    (if (not (seq-contains? (map :title (@app-state :possible-products)) (get-in @app-state [:product :title])))
      (do (
        (swap! app-state update-in [:possible-products] conj (@app-state :product))
        (swap! app-state assoc :product {})
        ; (swap! app-state update-in [:possible-products] shuffle))
          ))))
    (next-product))

(defn product [product-info]
  (dlog (str "Re-render product:" (get product-info :title)))
  [:div.product.clearfix
   { :key (get product-info :title) }
   [:img.photo { :src (get product-info :img) }]
   [:div.desc
    [:h3.title (get product-info :title)]
    [:div.price "Price " (get product-info :price)]
    [:a.buy-link { :href (get product-info :url) :target "_blank" } "Buy it on BLINQ"]]])

(defn possible-products-count []
  (let [c (count (@app-state :possible-products))]
    [:div.selected-count
     [:strong.count (str c)]
     (str " products selected")
     [:div.instruction
      (str "16 or more needed for the tournament to begin")]]))

(defn triage-products []
  ; (if (< (count (@app-state :possible-products)) 1)
  ;   (secretary/dispatch! "/"))
  [:div

   [:center
    [:h1 "White Elephant Gift Selector"]
    [:a {:href "/intro"} "Intro"]
    " - "
    [:a {:href "/about"} "About"]
    [:h2 "Triage: Build Your Product List"]]

   [possible-products-count]
   (if (>= (count (@app-state :possible-products)) 16)
      [:div [:a.onward {:href "/tournament"
                        :on-click #( do (
         (swap! app-state update-in [:possible-products] shuffle)

  (secretary/dispatch! "/tournament"))

                                   )
                        } (str (count (@app-state :possible-products)) " is enough... Tournament time!")]
   [:br]])

   [:div.current
      [:h3 "Worth Adding To Your List?"]
      [product (get @app-state :product)]
      [:div.actions.clearfix
        [:a.another { :on-click #(next-product) } "Not For Me"]
        [:a.thisone { :on-click #(save-product) } "Keep it! "]
        ]
      ]

   [:div.contenders

     ; For debugging
     ; [:div (str (count (@app-state :next-products)) " products prefetched")]
     ; (map product (@app-state :next-products))
     ; [:hr]

     (map product (@app-state :possible-products))]
   ])

(defn tournament-keep [product]
    (dlog (str "Keeping product " product))
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] rest)
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] rest)
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] concat [product])
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (dlog "Done keeping"))

(defn tournament []
  (let [products (@app-state :possible-products)
        product-count (count products)
        product-1 (first products)]
    [:div
     [:center
      [:h1 "White Elephant Gift Selector"]
      [:a {:href "/intro"} "Intro"]
      " - "
      [:a {:href "/about"} "About"]
      " - "
      [:a {:href "/triage"} "Back to Triage Phase"]
      [:h2 "Tournament: There can be only one!"]]

     (if (= product-count 1)

       [:div
         [:h2 "WE HAVE A WINNER! YOU FOUND YOUR GIFT!"]
         [product product-1]]

       (let [product-2 (second products)]

         [:div

         [:h4.remaining-count
          [:span "There are "]
          [:span.count (- (count products) 2)]
          [:span " match-ups after this!"]]

         [:div.clearfix
          [:div.compare
           [:div.clearfix [:a.another { :on-click #(tournament-keep product-1) } "Thing one"]]
           [product product-1]]

          [:div.compare
           [:div.clearfix [:a.thisone { :on-click #(tournament-keep product-2) } "Thing two"]]
           [product product-2]]]

         ]))]))

(defn intro []
  (swap! app-state assoc :intro-seen true)
  [:div
   [:center
    [:h1 "White Elephant Gift Selector"]
    [:a {:href "/intro"} "Intro"]
    " - "
    [:a {:href "/about"} "About"]]

   [:img { :src "/white_elephant.png" :width "200px" :style { :float "left" :margin "1em" } } ]

   [:p "So you're going to a White Elephant Gift Exchange Party!  It is
       VITAL that you show up with a great gift. But there are so many choices!
       What to do?"]
   [:p
    [:strong "Triage Phase: "]
    "We'll look through a bunch of random products, keeping the potential
    gifts. Pick out at least 16. You decide what is worthy for consideration...
    Funny? Work-appropriate? Kinda Awesome?"]
   [:p
    [:strong "Tournament Phase: "]
    "Now that you have some potentials, it's time to pick THE BEST! Two enter
    the ring, one leaves... in the end there can be only one!"]


   [:center [:a.thisone { :href "/triage" } "Begin the triage!"]]

   ])


; (defn on-js-reload []
;   ;; optionally touch your app-state to force rerendering depending on
;   ;; your application
;   ;; (swap! app-state update-in [:__figwheel_counter] inc)
; )


(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'intro))

(secretary/defroute "/triage" []
  (session/put! :current-page #'triage-products))

(secretary/defroute "/intro" []
  (session/put! :current-page #'intro))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/tournament" []
  (session/put! :current-page #'tournament))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler (fn [path] (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))})
  ; (secretary/dispatch! "/")
  (accountant/dispatch-current!)
  (next-product)
  (mount-root)
  )
