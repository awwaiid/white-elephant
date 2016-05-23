(ns white-elephant.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]

            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]

            [prone.middleware :refer [wrap-exceptions]]

            [net.cgrand.enlive-html :as enlive]

            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]

            [clj-http.client :as http]
            [environ.core :refer [env]]))

(def mount-target
  [:div#app
   [:h3 "ClojureScript has not been compiled!"]
   [:p "please run "
    [:b "lein figwheel"]
    " in order to start the compiler"]])

(def loading-page
  (html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      (include-css "https://fonts.googleapis.com/css?family=Lato:400,100,300,700,900")
      (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
     [:body
      mount-target
      (include-js "js/app.js")]]))

(defn fetch-url [url]
  (enlive/html-resource (java.net.URL. url)))

(defn random-product-url []
  (let [offset (rand-int 9000)
        low-price 0
        high-price 2000]
    (printf "Offset: %d\n" offset)
    (str "https://www.blinq.com/search/go?p=Q&lbc=blinq&w=*&"
         "af=price%3a%5b" low-price "%2c" high-price "%5d"
         "&isort=price&method=and&view=grid&ts=infinitescroll&"
         "srt=" offset)))

(defn random-product
  "Grab a random product, extracting out the essentials"
  []
  (let [page (fetch-url (random-product-url))
        title (enlive/text (first
                             (enlive/select
                               page
                               [[:li (enlive/nth-of-type 1)] :h3.tile-desc])))
        img (get-in (first (enlive/select
                             page
                             [[:li (enlive/nth-of-type 1)] :div.tile-img :img])) [:attrs :src])
        url (get-in (first (enlive/select
                             page
                             [[:li (enlive/nth-of-type 1)] :a.tile-link])) [:attrs :href])
        price (enlive/text (first
                             (enlive/select
                               page
                               [[:li (enlive/nth-of-type 1)] :span.live_saleprice])))]
    (response {:img img
               :title title
               :brock 5
               :price price
               :url url})))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/triage" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/intro" [] loading-page)
  (GET "/tournament" [] loading-page)
  (GET "/random-product" [] (random-product))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-json-response (wrap-defaults #'routes site-defaults))]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))

