# FileBrowserAPI

## Information

 Simple application to managed files and folders by http requests. You can here create, rename, delete folders and files.


 Most free solutions and apps don't meet with my expectations.<br>
 I wanted to create an app to manage folders and files, supporting most extensions.<br>
 The application is lightweight, doesn't have UI, it's served only via http request.<br>

 I use in this project: 
 * Kotlin 1.7.22  
 * SpringBoot 3.0.5 
 * Gradle 7.6.1
 * Kotest

### Future Features
* add database to easier managing of paths, serializing files
* add keycloak to managing security
* add support/configuration for docker (create image)
* add configuration for kubernetes 

## Prerequisites
Java 17 or above

## Getting Started
How to run app locally
* In `application.yaml`, set your workingPath, using Intellij/other IDE and build by gradle plugin and start by default compiler</br>

   *OR* 
* Use gradle, build project, in working directory go to `build/libs` and in here set in command line
   `java -Dapplication.baseDiskPath="yourWorkingPath" -jar filebrowserapi-0.0.1-SNAPSHOT.jar`, where `yourWorkingPath` is a path direct to your folder,
   where app will be working.

 Path to swagger: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

   Remember in your path you need create directory with name "filebrowser", but do not provide this name in configuration
