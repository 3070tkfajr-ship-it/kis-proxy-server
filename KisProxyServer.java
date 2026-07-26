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
                String tf = "1m"; // 기본값은 1분봉
                
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) code = param.split("=")[1];
                        if (param.startsWith("tf=")) tf = param.split("=")[1];
                    }
                }

                String url;
                String trId;

                // tf=D (일봉) 요청 시 API 주소 및 파라미터 완전 변경
                if ("D".equalsIgnoreCase(tf)) {
                    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
                    String today = java.time.LocalDate.now().format(dtf);
                    String past = java.time.LocalDate.now().minusDays(100).format(dtf); // 최근 100일
                    
                    url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                            + "?FID_COND_MRKT_DIV_CODE=J"
                            + "&FID_INPUT_ISCD=" + code
                            + "&FID_INPUT_DATE_1=" + past
                            + "&FID_INPUT_DATE_2=" + today
                            + "&FID_PERIOD_DIV_CODE=D"
                            + "&FID_ORG_ADJ_PRC=0";
                    trId = "FHKST03010100"; // 일봉 전용 TR 코드
                } else {
                    // 기존 당일 1분봉 로직
                    url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
                            + "?FID_ETC_CLS_CODE="
                            + "&FID_COND_MRKT_DIV_CODE=J"
                            + "&FID_INPUT_ISCD=" + code
                            + "&FID_INPUT_HOUR_1=153000"
                            + "&FID_PW_DATA_INCU_YN=Y";
                    trId = "FHKST03010200"; // 분봉 전용 TR 코드
                }

                HttpRequest dataReq = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("authorization", "Bearer " + accessToken)
                        .header("appkey", APP_KEY)
                        .header("appsecret", APP_SECRET)
                        .header("tr_id", trId)
                        .GET()
                        .build();

                HttpResponse<String> dataRes = HttpClient.newHttpClient().send(dataReq, HttpResponse.BodyHandlers.ofString());

                byte[] responseBytes = dataRes.body().getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

                System.out.println("📊 차트 데이터 전송 완료! (종목: " + code + ", 기준: " + (tf.equals("D")?"일봉":"1분봉") + ")");

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

                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + GEMINI_KEY;
               String prompt = "너는 앤드루 아지즈(Andrew Aziz) 데이트레이딩 기법의 마스터야. 첨부된 1분봉 차트 이미지를 보고 오직 'VWAP', 'EMA(5, 9, 20, 200)', 'Stochastic RSI(14,14,3,3)' 3가지 지표만을 딥(deep)하게 분석해서 타점을 평가해. 다른 지표나 펀더멘털 분석 등은 절대 언급하지 마. 후행성 지표이고 완벽한 타점은 없다.\n" +
                "총점은 96점 만점 기준으로 아래 채점표에 따라 엄격하게 계산해. 롱 가능 점수와 숏 가능 점수를 각각 따로 계산해서 제시하고, 둘 다 의미 있는 점수가 아니면 '관망'으로 제시해.\n\n" +
                "[채점 기준: 총 96점 만점]\n" +
                "1. VWAP (최대 35점): 주가가 VWAP 위면 롱, 아래면 숏 관점. 주가가 VWAP에 가까울수록 손익비가 좋아 고득점 부여. VWAP과 너무 이격되어 있다면 추격매수로 간주해 감점.\n" +
                "2. EMA 정렬 (최대 40점): 진입 방향에 맞게 이평선이 정렬(예: 롱은 5>9>20>200)되었는지 확인. 정렬이 완벽하고 5선/9선의 의미 있는 크로스가 발생한 초입일수록 고득점. 200선 돌파 여부도 확인.\n" +
                "3. Stoch RSI (최대 21점): %K와 %D가 진입에 유리한 모멘텀인지 평가. 롱 기준, 20 이하 과매도권에서 갓 탈출하며 골든크로스 발생 시 최고점. 반대로 과매수/과매도 극단값에 역행하면 감점.\n\n" +
                "[출력 형식 (반드시 HTML 태그 <b>, <span style='color:...'> 등을 사용해 가독성 있게 작성할 것)]\n" +
                "<b>1. VWAP 분석 (OO/35점):</b> 상세 분석 내용\n" +
                "<b>2. EMA 분석 (OO/40점):</b> 상세 분석 내용\n" +
                "<b>3. Stoch RSI 분석 (OO/21점):</b> 상세 분석 내용\n" +
                "<br><b>💡 롱 가능 점수: OO / 96점</b>\n" +
                "<b>💡 숏 가능 점수: OO / 96점</b>\n" +
                "<b>🔥 아지즈의 한 줄 평:</b> (롱/숏/관망 중 하나를 명확히 제시하고, 구체적인 행동 지침 작성)";
                
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
