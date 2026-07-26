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
    private static final String CLAUDE_KEY = System.getenv("CLAUDE_API_KEY");  // ← 추가

    // CORS 및 포트 설정
    private static final String CORS_ORIGIN = System.getenv().getOrDefault("CORS_ORIGIN", "*");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    // 🚨 오타 수정 완료: 실전투자용 올바른 도메인 및 포트
    private static final String DOMAIN = "https://openapi.koreainvestment.com:9443";
    private static String accessToken = "";

    // 공통 프롬프트 (아지즈 기법)
    private static final String AZIZ_PROMPT =
            "너는 앤드루 아지즈(Andrew Aziz) 데이트레이딩 기법의 마스터야. 첨부된 1분봉 차트 이미지를 보고 오직 'VWAP', 'EMA(5, 9, 20, 200)', 'Stochastic RSI(14,14,3,3)' 3가지 지표만을 딥(deep)하게 분석해서 타점을 평가해. 다른 지표나 펀더멘털 분석 등은 절대 언급하지 마. 후행성 지표이고 완벽한 타점은 없다.\\n" +
            "총점은 96점 만점 기준으로 아래 채점표에 따라 엄격하게 계산해. 롱 가능 점수와 숏 가능 점수를 각각 따로 계산해서 제시하고, 둘 다 의미 있는 점수가 아니면 '관망'으로 제시해.\\n\\n" +
            "[채점 기준: 총 96점 만점]\\n" +
            "1. VWAP (최대 35점): 주가가 VWAP 위면 롱, 아래면 숏 관점. 주가가 VWAP에 가까울수록 손익비가 좋아 고득점 부여. VWAP과 너무 이격되어 있다면 추격매수로 간주해 감점.\\n" +
            "2. EMA 정렬 (최대 40점): 진입 방향에 맞게 이평선이 정렬(예: 롱은 5>9>20>200)되었는지 확인. 정렬이 완벽하고 5선/9선의 의미 있는 크로스가 발생한 초입일수록 고득점. 200선 돌파 여부도 확인.\\n" +
            "3. Stoch RSI (최대 21점): %K와 %D가 진입에 유리한 모멘텀인지 평가. 롱 기준, 20 이하 과매도권에서 갓 탈출하며 골든크로스 발생 시 최고점. 반대로 과매수/과매도 극단값에 역행하면 감점.\\n\\n" +
            "[출력 형식 (반드시 HTML 태그 <b>, <span style='color:...'> 등을 사용해 가독성 있게 작성할 것)]\\n" +
            "<b>1. VWAP 분석 (OO/35점):</b> 상세 분석 내용\\n" +
            "<b>2. EMA 분석 (OO/40점):</b> 상세 분석 내용\\n" +
            "<b>3. Stoch RSI 분석 (OO/21점):</b> 상세 분석 내용\\n" +
            "<br><b>💡 롱 가능 점수: OO / 96점</b>\\n" +
            "<b>💡 숏 가능 점수: OO / 96점</b>\\n" +
            "<b>🔥 아지즈의 한 줄 평:</b> (롱/숏/관망 중 하나를 명확히 제시하고, 구체적인 행동 지침 작성)";

    public static void main(String[] args) throws Exception {
        if (APP_KEY == null || APP_SECRET == null) {
            System.out.println("🚨 KIS_APP_KEY / KIS_APP_SECRET 환경변수가 설정되지 않았습니다.");
        }
        if (CLAUDE_KEY == null || CLAUDE_KEY.isEmpty()) {
            System.out.println("⚠️ CLAUDE_API_KEY 환경변수가 설정되지 않았습니다. /analyze-claude 사용 불가.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 1. 차트 데이터 중계 핸들러
        server.createContext("/data", new DataHandler());

        // 2. 🤖 Gemini AI 스크린샷 분석 중계 핸들러
        server.createContext("/analyze", new AnalyzeHandler());

        // 3. 🤖 Claude Sonnet 5 분석 중계 핸들러 (신규)
        server.createContext("/analyze-claude", new ClaudeAnalyzeHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("🚀 자바 실전 중계 서버가 " + PORT + " 포트에서 실행 중! (Gemini + Claude Sonnet 5 탑재 완료)");
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
                String tf = "1m";

                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) code = param.split("=")[1];
                        if (param.startsWith("tf=")) tf = param.split("=")[1];
                    }
                }
                String responseBodyJson = "";
                // 일봉(D)인 경우 기존대로 100일치 조회
                if ("D".equalsIgnoreCase(tf)) {
                    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
                    String today = java.time.LocalDate.now().format(dtf);
                    String past = java.time.LocalDate.now().minusDays(100).format(dtf);

                    String url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
                            + "?FID_COND_MRKT_DIV_CODE=J"
                            + "&FID_INPUT_ISCD=" + code
                            + "&FID_INPUT_DATE_1=" + past
                            + "&FID_INPUT_DATE_2=" + today
                            + "&FID_PERIOD_DIV_CODE=D"
                            + "&FID_ORG_ADJ_PRC=0";
                    HttpRequest dataReq = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json; charset=utf-8")
                            .header("authorization", "Bearer " + accessToken)
                            .header("appkey", APP_KEY)
                            .header("appsecret", APP_SECRET)
                            .header("tr_id", "FHKST03010100")
                            .GET()
                            .build();
                    HttpResponse<String> dataRes = HttpClient.newHttpClient().send(dataReq, HttpResponse.BodyHandlers.ofString());
                    responseBodyJson = dataRes.body();
                } else {
                    // 🚀 1분봉 500개 긁어오기 (30개씩 연속 조회 루프)
                    java.util.List<String> combinedOutput2 = new java.util.ArrayList<>();
                    String targetHour = "153000"; // 시작 기준 시간 (장마감 혹은 현재 시간)

                    // 500개를 채우려면 30개씩 약 17번 호출 필요
                    for (int i = 0; i < 17; i++) {
                        String url = DOMAIN + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice"
                                + "?FID_ETC_CLS_CODE="
                                + "&FID_COND_MRKT_DIV_CODE=J"
                                + "&FID_INPUT_ISCD=" + code
                                + "&FID_INPUT_HOUR_1=" + targetHour
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
                        String resBody = dataRes.body();
                        // output2 배열 추출 및 병합
                        int out2Idx = resBody.indexOf("\"output2\":[");
                        if (out2Idx == -1) break;

                        int bracketEnd = resBody.indexOf("]", out2Idx);
                        if (bracketEnd == -1) break;
                        String itemsStr = resBody.substring(out2Idx + 11, bracketEnd);
                        if (itemsStr.trim().isEmpty()) break;
                        // 개별 캔들 아이템들 분리
                        String[] items = itemsStr.split("\\},\\{");
                        if (items.length == 0) break;
                        String oldestTime = "";
                        for (int j = 0; j < items.length; j++) {
                            String item = items[j];
                            if (!item.startsWith("{")) item = "{" + item;
                            if (!item.endsWith("}")) item = item + "}";

                            combinedOutput2.add(item);
                            // 가장 오래된 시간(마지막 아이템의 stck_cntg_hour) 추출
                            if (j == items.length - 1) {
                                int timeIdx = item.indexOf("\"stck_cntg_hour\":\"");
                                if (timeIdx != -1) {
                                    int tStart = timeIdx + 18;
                                    int tEnd = item.indexOf("\"", tStart);
                                    oldestTime = item.substring(tStart, tEnd);
                                }
                            }
                        }
                        // 더 이상 과거 데이터가 없거나 중단되면 탈출
                        if (oldestTime.isEmpty() || oldestTime.compareTo(targetHour) >= 0) break;
                        targetHour = oldestTime;
                        // 한투 API 제한 회피용 살짝 대기 (0.05초)
                        Thread.sleep(50);
                    }
                    // 500개 모은 데이터를 하나의 JSON 구조로 조립
                    responseBodyJson = "{\"output1\":{},\"output2\":[" + String.join(",", combinedOutput2) + "],\"rt_cd\":\"000000\",\"msg1\":\"정상처리 되었습니다.\"}";
                }
                byte[] responseBytes = responseBodyJson.getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                System.out.println("📊 대용량 차트 데이터 전송 완료! (종목: " + code + ", 방식: " + tf + ")");
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

    // 🤖 구글 Gemini AI 요청 처리 (기존)
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

                String reqJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + AZIZ_PROMPT + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}]}";
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
                System.out.println("🤖 Gemini AI 차트 분석 완료 및 브라우저 전송 성공!");
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

    // 🤖 Claude Sonnet 5 분석 핸들러 (신규)
    static class ClaudeAnalyzeHandler implements HttpHandler {
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

                if (CLAUDE_KEY == null || CLAUDE_KEY.isEmpty()) {
                    throw new Exception("Claude API Key(CLAUDE_API_KEY)가 서버에 설정되지 않았습니다.");
                }
                if (base64Image.isEmpty()) {
                    throw new Exception("이미지 base64 데이터가 없습니다.");
                }

                // Anthropic Messages API 요청 본문 구성
                String reqJson = "{"
                        + "\"model\":\"claude-sonnet-5\","
                        + "\"max_tokens\":2048,"
                        + "\"messages\":[{"
                        + "\"role\":\"user\","
                        + "\"content\":["
                        + "{\"type\":\"image\",\"source\":{\"type\":\"base64\",\"media_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}},"
                        + "{\"type\":\"text\",\"text\":\"" + AZIZ_PROMPT + "\"}"
                        + "]"
                        + "}]"
                        + "}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", CLAUDE_KEY)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                System.out.println("🤖 Claude Sonnet 5 차트 분석 완료! (status: " + response.statusCode() + ")");
            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"Claude AI 분석 실패: " + e.getMessage().replace("\"", "'") + "\"}";
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();
            }
        }
    }
}
