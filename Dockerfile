    FROM openjdk:17-jdk-slim 
    copy ..
    Run mvn clean package -DskipTests

    From open jdk:17-jdk-slim
    copy --from=0/traget/BibleQuizeApplication-0.0.1-SNAPSHOT.jar BibleQuizeApplication.jar
    EXPOSE 8081
    EntryPoint["java","-jar","/BibleQuizeApplication.jar"]
