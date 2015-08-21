(ns hello2.core
  (:require [org.httpkit.server :refer [run-server]]
            [ring.util.response :as rur]
            [ring.middleware.resource :as rmr]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.session :refer [wrap-session]]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :refer [generate-string parse-string]]
           ;; [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [compojure.route :as route]
            [compojure.core :as core]))

(let [db-host "localhost"
      db-port 5432
      db-name "mydb"]
  (def db-spec {:subprotocol "postgresql"
                :subname (str "//" db-host ":" db-port "/" db-name)
                :user "aaa"
                :password "123"}))

;; (core/defroutes my-routes
;;   (core/DELETE "/works/:id" [id :as r]
;;     (str r)))

(core/defroutes my-routes
  (core/context "" [id :as {uri :uri}]
    (core/GET "/works/:id" []
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (-> (jdbc/with-db-connection [conn db-spec]
                   (jdbc/query conn ["select * from works where id = ?"
                                     (Integer/parseInt (str (last uri)))])))})
    (core/DELETE "/works/:id" []
      (do (jdbc/with-db-connection [conn db-spec]
            (jdbc/delete! conn :works ["id = ?" (Integer/parseInt (str (last uri)))]))
          {:status 200
           :headers {"Content-Type" "text/plain"}
           :body (str "work_id = " id " deleted!")}))
    ))



;; (defn handler [request]
;;   (let [{:keys [request-method uri params]} request]
;;     (case request-method
;;       :get {:status 200
;;             :headers {"Content-Type" "application/json"}
;;             :body (-> (jdbc/with-db-connection [conn db-spec]
;;                         (jdbc/query conn ["select * from works where id = ?" (Integer/parseInt (str (last uri)))]))
;;                       first
;;                       generate-string)}
;;       :post {:status 200
;;              :headers {"Content-Type" "text/plain"}
;;              :body (first (jdbc/with-db-connection [conn db-spec]
;;                             (jdbc/insert! conn :works
;;                                           (parse-string (slurp (:body request)) true))))}
;;       :patch (do
;;                (jdbc/with-db-connection [conn db-spec]
;;                  (jdbc/update! conn :works {:price (Integer/parseInt (params "price"))} ["id = ?" (Integer/parseInt (str (last uri)))]))

;;                {:status 200
;;                :headers {"Content-Type" "text/plain"}
;;                 :body "ok"})
;;       :delete (do
;;                 (jdbc/with-db-connection [conn db-spec]
;;                   (jdbc/delete! conn :works ["id = ?" (Integer/parseInt (str (last uri)))]))
;;                 {:status 200
;;                  :headers {"Content-Type" "text/plain"}
;;                  :body "delete!"})
;;       {:status 200
;;        :header {"Content-Type" "text/plain"}
;;        :body ("hello world")})))


;;(defn wrap-content-type [handler content-type]
;;(fn [request]
;;(let [response (handler request)]
;;(assoc-in response [:header "Content-Type"] content-type))))

;; (defn wrap-prn-request [handler]
;;   (fn [request]
;;     (let [response (handler request)]
;;       (println "Debug: " (clojure.pprint/pprint request))
;;       response)))

(def app my-routes)

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (reset! server (run-server #'app {:port 3000}))
  (prn "Server started!"))
