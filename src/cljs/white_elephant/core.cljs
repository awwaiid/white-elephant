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

(defn about-page []
  [:div [:h2 "About White Elephant Gift Selector"]
   [:div [:a {:href "/"} "go to the home page"]]
   [:div [:p
          "This is a tool to pick out the BEST White Elephant Gift!"]]
   ])

(enable-console-print!)

(println "Edits to this text should show up in my developer console.")

(defonce app-state (atom {
                      :text "Hello to all the really strange wonderful weird new world!"
                      :min-price 0
                      :max-price 2000
                      :possible-products []
                      :product {
                                :img "https://www.blinq.com/search/thumb.php?s=172&aspect=true&f=https%3a%2f%2fin…sl.fastly.net%2f1588078%2ftwo_fifty%2f9750412txAJlbgL.jpg.jpg%3f1447436187"
                                :title "Calabria 91348 Bi-Focal Safety Glasses UV Protection in Smoke ; +1.00"
                                :url "https://blinq.resultspage.com/search?p=R&ts=infinitescroll&uid=554603337&w=…afety-glasses-uv-protection-in-smoke-1-00%2f590946%3fcondition%3dbrand-new"
                      :price "$17.01"
                                }
                      :next-product {
                                :img "https://www.blinq.com/search/thumb.php?s=172&aspect=true&f=https%3a%2f%2fin…sl.fastly.net%2f1588078%2ftwo_fifty%2f9750412txAJlbgL.jpg.jpg%3f1447436187"
                                :title "Calabria 91348 Bi-Focal Safety Glasses UV Protection in Smoke ; +1.00"
                                :url "https://blinq.resultspage.com/search?p=R&ts=infinitescroll&uid=554603337&w=…afety-glasses-uv-protection-in-smoke-1-00%2f590946%3fcondition%3dbrand-new"
                      :price "$17.01"
                                }
                      }))

(defn next-product-handler [response]
  (.log js/console (str response))
  (swap! app-state assoc :next-product response)
  ; Pre-fetch the image, specifically
  (aset (js/Image.) "src" (get response :img))
  ; (.log js/console (str "App state:" @app-state))
  )

(defn next-product []
  (swap! app-state assoc :product (get @app-state :next-product))
  (GET "/random-product" {
                          :response-format :json
                          :keywords? true
                          :handler next-product-handler}))

(defn save-product []
  (swap! app-state update-in [:possible-products] conj (get @app-state :product))
  (next-product))

(defn product [product-info]
  (dlog (str "Re-render product:" (get product-info :title)))
   [:div.product
     [:img { :src (get product-info :img) }]
     [:div.price "Price " (get product-info :price)]
     [:a.link { :href (get product-info :url) } "Product Details / Buy"]
     [:h3.title (get product-info :title)]])

(defn possible-products-count []
  (let [c (count (@app-state :possible-products))]
    [:div.selected-count
     (str c " products selected")]))

(defn triage-products []
  [:div

   [:h2 "White Elephant Gift Selector"]
   [:h3 "Phase 1: Triage"]

   [possible-products-count]

   [:div.current
   [:div
    [:a {:href "/about"} "go to about page"]
    [:a {:href "/tournament"} "go to phase 2"]
    ]
    [:h3 "Worthy for consideration?"]
     [:div.actions
      [:div { :class "another" :onClick next-product } "Next!"]
      [:div { :class "thisone" :onClick save-product } "Keep it!"]]
   [:hr]
    [product (get @app-state :product)]]

  [:div.contenders
  ; For debugging
  (map product (@app-state :possible-products))
   ]
  ]
  )

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
  (dlog "Re-render tournament")
  ; (let [product-1 (first (@app-state :possible-products))
  ;       product-2 (second (@app-state :possible-products))]
  (let [products (@app-state :possible-products)
        product-count (count products)
        product-1 (first products)]
    (dlog (str "Currently there are " (count (@app-state :possible-products))))
    (dlog (str "product-1: " (product-1 :title)))
    ; (dlog (str "product-2: " (product-2 :title)))
    (if (= product-count 1)
      [:div
        [:h2 "White Elephant Gift Selector"]
        [:h3 "FINAL WINNER"]
        [product product-1]]
      (let [product-2 (second products)]
        [:div
         [:h2 "White Elephant Gift Selector"]
         [:h3 "Phase 2: Tournament"]

         [:h4 (str "There are " (- (count products) 2) " match-ups after this!")]

         [:div { :class "another" :onClick #(tournament-keep product-1) } "This one"]
         [product product-1]

         [:div { :class "thisone" :onClick #(tournament-keep product-2) } "This one"]
         [product product-2]

         ]))))


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
  (session/put! :current-page #'triage-products))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/tournament" []
  (session/put! :current-page #'tournament))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
