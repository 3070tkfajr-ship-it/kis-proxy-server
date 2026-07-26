FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY KisProxyServer.java .

# 컨테이너 빌드 시점에 컴파일 (Maven/Gradle 불필요)
RUN javac KisProxyServer.java

# 클라우드가 지정하는 PORT 환경변수를 자바 코드가 읽어서 사용함
CMD ["java", "KisProxyServer"]
