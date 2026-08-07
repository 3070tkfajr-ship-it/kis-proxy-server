FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY KisProxyServer.java .

# 컨테이너 빌드 시점에 컴파일 (Maven/Gradle 불필요)
# 🩹 아이폰에서 복사/붙여넣기 할 때 예쁜따옴표(" ")로 바뀌거나 마크다운 백틱(```)이 섞여 들어오는 문제를
#    컴파일 직전에 자동으로 고쳐줌 (앞으로 이 문제로 빌드가 깨지는 일이 없어짐)
RUN sed -i 's/“/"/g; s/”/"/g' KisProxyServer.java && \
    sed -i '/^[[:space:]]*`\{1,\}[[:space:]]*$/d' KisProxyServer.java && \
    javac KisProxyServer.java

# 클라우드가 지정하는 PORT 환경변수를 자바 코드가 읽어서 사용함
CMD ["java", "KisProxyServer"]
