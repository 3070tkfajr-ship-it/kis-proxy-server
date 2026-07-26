import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KisProxyServer {

    // 🚨 키는 코드에 넣지 않고 환경변수(KIS_APP_KEY, KIS_APP_SECRET)에서 읽어옴.
    //    로컬에서 테스트할 땐 실행 전에 터미널에서 아래처럼 지정:
    //    (Windows) set KIS_APP_KEY=발급받은키
    //    (Mac/Linux) export KIS_APP_KEY=발급받은키
    private static final String APP_KEY = System.getenv("KIS_APP_KEY");
    private static final String APP_SECRET = System.getenv("KIS_APP_SECRET");

    // 브라우저에서 접근을 허용할 출처(HTML을 올릴 GitHub Pages 주소 등). 여러 개면 콤마로 구분.
    // 환경변수 CORS_ORIGIN을 안 넣으면 개발 편의상 전체 허용(*)으로 동작.
    private static final String CORS_ORIGIN = System.getenv().getOrDefault("CORS_ORIGIN", "*");

    // 클라우드 플랫폼(Render 등)이 지정해주는 포트. 없으면 로컬 기본값 8080 사용.
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    // API 도메인 (모의투자는 openapivts, 실전투자는 openapi)
    private static final String DOMAIN = "https://openapi.koreainvestment.com:29443";

    // 발급받은 토큰을 임시 저장할 변수
    private static String accessToken = "";

    public static void main(String[] args) throws Exception {
        if (APP_KEY == null || APP_SECRET == null) {
            System.out.println("🚨 KIS_APP_KEY / KIS_APP_SECRET 환경변수가 설정되지 않았습니다. 서버를 시작할 수 없습니다.");
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 브라우저가 "/data?code=114800"으로 찌르면 실행될 차트 데이터 핸들러
        server.createContext("/data", new DataHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("🚀 자바 실전 중계 서버가 " + PORT + " 포트에서 실행 중!");
    }

    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", CORS_ORIGIN);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // 1. 토큰이 없으면 한투 서버에서 새로 발급받기
                if (accessToken.isEmpty()) {
                    String tokenReqBody = String.format("{\"grant_type\":\"client_credentials\", \"appkey\":\"%s\", \"appsecret\":\"%s\"}", APP_KEY, APP_SECRET);
                    HttpRequest tokenReq = HttpRequest.newBuilder()
                            .uri(URI.create(DOMAIN + "/oauth2/tokenP"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(tokenReqBody))
                            .build();
                    HttpResponse<String> tokenRes = HttpClient.newHttpClient().send(tokenReq, HttpResponse.BodyHandlers.ofString());

                    // JSON에서 토큰 값만 쏙 빼오기
                    String body = tokenRes.body();
                    int start = body.indexOf("\"access_token\":\"") + 16;
                    int end = body.indexOf("\"", start);
                    accessToken = body.substring(start, end);
                    System.out.println("🔑 한투 보안 토큰 갱신 완료!");
                }

                // 2. 브라우저에서 보낸 종목코드(code) 확인 (기본값: KODEX 인버스)
                String query = exchange.getRequestURI().getQuery();
                String code = "114800";
                if (query != null && query.contains("code=")) {
                    code = query.split("code=")[1].split("&")[0];
                }

                // 3. 한투 API로 분봉(1분) 차트 데이터 요청 쏘기
                String url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
                        + "?FID_ETC_CLS_CODE="
                        + "&FID_COND_MRKT_DIV_CODE=J"
                        + "&FID_INPUT_ISCD=" + code
                        + "&FID_INPUT_HOUR_1=153000" // 당일 장 마감시간 기준 최근 30개
                        + "&FID_PW_DATA_INCU_YN=Y";

                HttpRequest dataReq = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("authorization", "Bearer " + accessToken)
                        .header("appkey", APP_KEY)
                        .header("appsecret", APP_SECRET)
                        .header("tr_id", "FHKST03010200") // 주식 분봉 TR 코드
                        .GET()
                        .build();

                HttpResponse<String> dataRes = HttpClient.newHttpClient().send(dataReq, HttpResponse.BodyHandlers.ofString());

                // 4. 받아온 데이터를 브라우저(HTML)로 그대로 쏴주기
                byte[] responseBytes = dataRes.body().getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

                System.out.println("📊 차트 데이터 브라우저로 전송 완료! (종목코드: " + code + ")");

            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"데이터 연동 실패\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes());
                os.close();
            }
        }
    }
}
