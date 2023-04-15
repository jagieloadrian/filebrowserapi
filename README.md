# FileBrowserAPI

## Information

 Simple application to managed files and folders by http requests. You can here create, rename, delete folders. 

## Getting Started
How to run app locally

1. In `application.yaml`, set your workingPath, using Intellij/other IDE and build by gradle plugin and start by default compiler</br>
    </br>
    OR </br>
   </br>
2. Use gradle, build project, in working directory go to `build/libs` and in here set in command line </br>
`java -Dapplication.baseDiskPath="yourWorkingPath" -jar filebrowserapi-0.0.1-SNAPSHOT.jar`, where `yourWorkingPath` is a path direct to your folder, </br>
where app will be working. 

3. Path to swagger: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)
