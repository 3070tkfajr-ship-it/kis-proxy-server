import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class KisProxyServer {

    // 🚨 환경변수(Render 금고)에서 키를 안전하게 읽어옴
    private static final String APP_KEY = System.getenv("KIS_APP_KEY");
    private static final String APP_SECRET = System.getenv("KIS_APP_SECRET");
    private static final String GEMINI_KEY = System.getenv("GEMINI_API_KEY");

    // CORS 및 포트 설정
    private static final String CORS_ORIGIN = System.getenv().getOrDefault("CORS_ORIGIN", "*");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    // 🚨 오타 수정 완료: 실전투자용 올바른 도메인 및 포트
    private static final String DOMAIN = "https://openapi.koreainvestment.com:9443";

    private static String accessToken = "";

    public static void main(String[] args) throws Exception {
        if (APP_KEY == null || APP_SECRET == null) {
            System.out.println("🚨 KIS_APP_KEY / KIS_APP_SECRET 환경변수가 설정되지 않았습니다.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 1. 차트 데이터 중계 핸들러
        server.createContext("/data", new DataHandler());
        // 2. 🤖 AI 스크린샷 분석 중계 핸들러 추가
        server.createContext("/analyze", new AnalyzeHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("🚀 자바 실전 중계 서버가 " + PORT + " 포트에서 실행 중! (AI 기능 탑재 완료)");
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
                if (APP_KEY == null || APP_SECRET == null) {
                    throw new Exception("KIS API Key가 서버에 설정되지 않았습니다.");
                }

                if (accessToken.isEmpty()) {
                    String tokenReqBody = String.format("{\"grant_type\":\"client_credentials\", \"appkey\":\"%s\", \"appsecret\":\"%s\"}", APP_KEY, APP_SECRET);
                    HttpRequest tokenReq = HttpRequest.newBuilder()
                            .uri(URI.create(DOMAIN + "/oauth2/tokenP"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(tokenReqBody))
                            .build();
                    HttpResponse<String> tokenRes = HttpClient.newHttpClient().send(tokenReq, HttpResponse.BodyHandlers.ofString());
                    String body = tokenRes.body();
                    int start = body.indexOf("\"access_token\":\"") + 16;
                    int end = body.indexOf("\"", start);
                    accessToken = body.substring(start, end);
                    System.out.println("🔑 한투 보안 토큰 갱신 완료!");
                }

                String query = exchange.getRequestURI().getQuery();
                String code = "114800";
                if (query != null && query.contains("code=")) {
                    code = query.split("code=")[1].split("&")[0];
                }

                String url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
                        + "?FID_ETC_CLS_CODE="
                        + "&FID_COND_MRKT_DIV_CODE=J"
                        + "&FID_INPUT_ISCD=" + code
                        + "&FID_INPUT_HOUR_1=153000"
                        + "&FID_PW_DATA_INCU_YN=Y";

                HttpRequest dataReq = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("authorization", "Bearer " + accessToken)
                        .header("appkey", APP_KEY)
                        .header("appsecret", APP_SECRET)
                        .header("tr_id", "FHKST03010200")
                        .GET()
                        .build();

                HttpResponse<String> dataRes = HttpClient.newHttpClient().send(dataReq, HttpResponse.BodyHandlers.ofString());

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

    // 🤖 구글 Gemini AI 요청 처리
    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", CORS_ORIGIN);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                
                String base64Image = "";
                int imgStart = body.indexOf("\"image\":\"") + 9;
                if (imgStart > 8) {
                    int imgEnd = body.indexOf("\"", imgStart);
                    base64Image = body.substring(imgStart, imgEnd);
                }

                if (GEMINI_KEY == null || GEMINI_KEY.isEmpty()) {
                    throw new Exception("Gemini API Key가 서버에 설정되지 않았습니다.");
                }

                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_KEY;
                String prompt = "너는 앤드루 아지즈 매매법(데이트레이딩) 전문가야. 업로드된 1분봉 차트를 보고 VWAP, EMA(5/9/20/200), 스토캐스틱 RSI를 분석해 줘. 1. 지표 상태 2. 모멘텀 3. 타점 점수(100점 만점) 및 한 줄 평 형식으로 짧고 명확하게 작성해 줘. HTML 태그(<b> 등)를 적절히 섞어서 가독성 좋게 꾸며줘.";
                
                String reqJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}]}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(geminiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

                System.out.println("🤖 AI 차트 분석 완료 및 브라우저 전송 성공!");

            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"AI 분석 실패\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes());
                os.close();
            }
        }
    }
}
