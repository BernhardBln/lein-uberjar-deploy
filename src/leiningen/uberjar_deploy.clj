(ns leiningen.uberjar-deploy
  (:use
    [leiningen.uberjar :only [uberjar]]
    [leiningen.deploy :only [deploy]]
    [leiningen.pom :only [pom]])

  (:require 
    [clojure.data.xml :as xml]
    [clojure.zip :as zip]
    [clojure.pprint] 
    [clojure.data.zip.xml :as zx]))

; E.g., com.foo
(defn get-group [project] (format "%s" (project :group)))
; E.g., bigproject
(defn get-name [project] (format "%s" (project :name)))
; com.foo/bigproject
(defn get-group-and-name [project] (format "%s/%s" (get-group project) (get-name project)))

; E.g., 0.1.0-SNAPSHOT
(defn get-version [project] (format "%s" (project :version)))

; E.g., target/bigproject-0.1.0-SNAPSHOT-standalone.jar
(defn get-uberjar-file-path [project] (format "target/%s-%s-standalone.jar" (get-name project) (get-version project)))
(def  get-pom-file-path "pom.xml")

(defn get-target [project]
  (if (.endsWith (get-version project) "-SNAPSHOT")
    "snapshots"
    "releases"))

(defn abort [message]
  (binding [*out* *err*] 
    (println message)
    (System/exit 1)))

(defn get-repo-values [project name-to-find]
  (filter (fn[x](= name-to-find (first x))) (:deploy-repositories project)))

(defn get-repo-value [project value]
  (let [entry (get-repo-values project (get-target project))]
    (nth (find (get (first entry) 1) value) 1)))

(defn confirm-repo-defined-in-project [project]
  (if (nil? (get-repo-value project :url))
    (abort (format ":url not found for \"%s\" entry in project's :deploy-repositories" (get-target project))))

  (if (nil? (get-repo-value project :id))
    (abort (format ":id not found for \"%s\" entry in project's :deploy-repositories" (get-target project)))))

(defn check-config [project]
  (confirm-repo-defined-in-project project))

(defn uberjar-deploy
  "Deploy project's uberjar and pom.xml. 

A :deploy-repositories entry must be present in the project.clj, containing the
snapshots and releases :url locations. For example,

  :deploy-repositories [
    [\"snapshots\" {:id \"nexus\" :url \"http://host:8081/nexus/content/repositories/snapshots\"}]
    [\"releases \" {:id \"nexus\" :url \"http://host:8081/nexus/content/repositories/releases\"}]
  ]

Snapshots or releases is chosen based on the version in the project.clj.

Note that Leiningen will try to sign releases by default; this may be turned off by adding 
\":sign-releases false\" to the \"releases\" map.
(see https://github.com/technomancy/leiningen/blob/master/doc/GPG.md)

The :id must match a server specified in ~/.m2/settings.xml, from which the username and password 
are obtained."

  [project & args]
  (check-config project)
    (uberjar project)
    (pom project)
    (deploy project (get-target project) (get-group-and-name project) (get-version project) (get-uberjar-file-path project) get-pom-file-path))

