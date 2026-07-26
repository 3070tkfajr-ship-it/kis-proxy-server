import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class KisProxyServer {
    private static final String APP_KEY = System.getenv("KIS_APP_KEY");
    private static final String APP_SECRET = System.getenv("KIS_APP_SECRET");
    private static final String GEMINI_KEY = System.getenv("GEMINI_API_KEY");
    private static final String CLAUDE_KEY = System.getenv("CLAUDE_API_KEY");
    // 미국 주식 1분봉용 (Twelve Data, twelvedata.com에서 무료 발급. 무료 플랜: 하루 800회, 분당 8회, outputsize 최대 5000까지 지정 가능)
    private static final String TWELVE_DATA_KEY = System.getenv("TWELVE_DATA_KEY");

    private static final String CORS_ORIGIN = System.getenv().getOrDefault("CORS_ORIGIN", "*");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final String DOMAIN = "https://openapi.koreainvestment.com:9443";
    private static String accessToken = "";

private static final String AZIZ_PROMPT =
    "너는 앤드루 아지즈(Andrew Aziz) 데이트레이딩 기법의 마스터야. 첨부된 1분봉 차트 이미지를 보고 오직 'VWAP', 'EMA(5,9,20,200)', 'Stochastic RSI(14,14,3,3)' 3가지 지표만 딥(deep)하게 분석해서 타점을 평가해. 다른 지표나 뉴스·펀더멘털은 절대 언급하지 마.\n" +
    "너는 이 셋이 전부 후행성 지표라는 것과, 완벽한 타점은 존재하지 않는다는 것을 항상 전제로 깔고 말해. 확신에 찬 단정적 표현(반드시 오른다/무조건 진입해라) 대신 확률적 어조를 써.\n" +
    "모호한 일반론은 금지야.그리고 당일트레이딩이니 500개의 1분봉보다는 뒤의30개~50개사이의분봉이중요하다는점도 잊지마.  반드시 이 이미지에서 실제로 관찰되는 구체적 근거(예: 몇 번째 캔들에서 몇 선과 몇 선이 semi 교차했는지, 현재가와 VWAP의 대략적 이격 정도, %K/%D의 대략적 위치)를 최소 1개 이상 문장마다 인용해서 말해. 화면에서 확인 안 되는 내용은 추측하지 말고 '이미지에서 명확히 확인 안 됨'이라고 말해.\n\n" +
    "[분석 시점 구분]\n" +
    "- 이미지에 당일 전체(약 수백 개 봉)와 최근 확대 구간이 함께 있으면, VWAP·EMA200 같은 큰 맥락은 전체 차트로 보고, 실제 진입 타점 판단은 최근 30~60개 봉 구간의 정렬·Stoch·가격 행동에 더 무게를 둬.\n" +
    "- 확대 구간이 따로 없으면, 차트 오른쪽 끝(가장 최근) 30~50개 봉을 진입 판단의 핵심 구간으로 삼아.\n\n" +
    "[채점 기준: 총 96점 만점 (100점은 절대 주지 마. 사후 확정 정보가 아닌 실시간 판단은 항상 불확실성이 남아있기 때문)]\n\n" +
    "롱과 숏 점수는 반드시 동일한 분량과 동일한 깊이로 설명해. 한쪽이 유리하다고 판단되더라도, 불리한 쪽의 VWAP·EMA·Stoch 각 항목을 왜 감점했는지 구체적 근거를 최소 1문장씩 반드시 제시해. '불리해서 낮다'는 식의 한 줄 요약만 하고 끝내면 안 된다.\n\n" +
    "1. VWAP (최대 35점)\n" +
    " - 현재가가 VWAP 대비 위/아래 어느 쪽인지 먼저 판정하고, 그 방향에 부합하는 포지션(롱 또는 숏)에만 이 항목 점수를 부여해. 반대 방향엔 0~5점만 줘.\n" +
    " - 이격도가 좁을수록(추격 아님) 고득점, 넓을수록(이미 많이 간 자리, 추격매수/추격매도로 판단됨) 감점.\n" +
    " - 대략적 기준: VWAP 이격이 약 0.2~0.4% 이내면 고득점권, 0.8% 이상이면 추격으로 보고 강하게 감점.\n" +
    " - 추가로, 최근 여러 캔들에 걸쳐 가격이 VWAP을 지지 또는 저항으로 '실제로 지켜왔는지'(눌림 후 반등이 반복되는 패턴)를 확인해서, 그렇다면 가산점을 줘. 단순히 지금 순간 위/아래에 있다는 것만으론 만점 주지 마.\n" +
    " - 최근 5~10개 캔들 중 VWAP을 터치한 뒤 다시 같은 방향으로 회복한 횟수가 2회 이상이면 '존중'으로 가산, 한 번도 안 지키고 그냥 뚫고 지나갔으면 가산하지 마.\n\n" +
    "2. EMA 정렬 (최대 40점)\n" +
    " - 진입 방향에 맞는 정렬(예: 롱이면 5>9>20, 숏이면 5<9<20)이 되어 있는지 확인 (최대 20점).\n" +
    " - 200 EMA와의 관계(같은 방향으로 정렬됐는지, 방금 돌파했는지, 아직 200선 아래/위에 눌려있는지)를 별도로 평가 (최대 10점).\n" +
    " - 아지즈 원전의 핵심 개념을 반영해서, '9 EMA 또는 20 EMA가 최근 여러 캔들에 걸쳐 실제로 지지 또는 저항으로 존중되고 있는가'를 확인하고, 그렇다면 가산점을 줘. 방금 막 크로스가 발생한 초입 구간이면 추가 가산점 (최대 10점).\n" +
    " - '존중'의 기준: 최근 10개 캔들 안에서 가격이 9EMA 또는 20EMA에 근접(대략 0.2% 이내)한 뒤, 같은 방향으로 최소 2~3캔들 이상 이어진 경우가 보이면 존중으로 인정.\n" +
    " - 방금 막 크로스가 난 직후(1~3캔들 이내)이고, 아직 이격이 크지 않으면 '초입'으로 보고 가산. 이미 5캔들 이상 지나 이격이 벌어진 상태면 초입 가산 주지 마.\n" +
    " - 정배열/역배열이 진입 방향과 반대라면 이 항목은 10점 이하로 강하게 감점.\n\n" +
    "3. Stochastic RSI (최대 21점)\n" +
    " - %K와 %D의 위치, 그리고 둘 사이의 크로스 방향이 진입 방향과 같은 모멘텀을 가리키는지 확인.\n" +
    " - 롱 기준: 20 이하 과매도권에서 막 골든크로스가 나온 직후면 만점에 가깝게. 반대로 80 이상 과매수권에서 신규 롱 진입 형태라면 대폭 감점 (숏은 반대로 적용).\n" +
    " - 50 부근에서의 애매한 크로스는 절반 수준의 점수만 부여.\n" +
    " - 과매도(20 이하)에서 골든크로스 직후 바로 재하락해 실패한 흔적이 보이면, 그 실패 이후의 재진입인지 여부를 확인하고 말해. 실패 직후 재진입이면 점수를 낮춰.\n" +
    " - %K와 %D가 둘 다 50 위에 있으면서 롱을 주장하거나, 둘 다 50 아래에 있으면서 숏을 주장하는 경우는 '모멘텀 순방향'으로 보고 가산. 반대로 역행이면 감점.\n\n" +
    "[추가로 반드시 서술할 것 (점수에는 포함하지 않음)]\n" +
    "- 이미지에서 확인되는 가장 최근의 눈에 띄는 저점 또는 고점을 기준으로, 합리적인 손절가 후보를 1줄로 제시.\n" +
    "- 손절가 후보는 '이미지에서 확인되는 가장 최근의 의미 있는 저점/고점' 또는 '직전 스윙의 극값'을 우선 사용. 막연한 퍼센트(예: -1%)로 잡지 마.\n" +
    "- 그 손절폭 대비, 다음 저항/지지(또는 EMA/VWAP 라인)를 목표가로 삼았을 때의 대략적 손익비(예: 약 1.5:1, 2:1)를 추정해서 1줄로 언급. 손익비가 1:1도 안 나오면 '이 자리는 손익비가 불리하다'고 명확히 말해.\n" +
    "- 손익비가 1.2:1 미만이면 한 줄 평에서 '이 자리는 손익비가 불리하므로 관망 또는 축소 진입'을 반드시 포함해.\n" +
    "- 손익비가 2:1 이상이면 한 줄 평에서 그 사실을 명시적으로 긍정 요인으로 언급해.\n" +
    "- 이미지에 시간이 보인다면, 장 시작 후 어느 정도 지난 시점인지 가늠해서 '초반 모멘텀 구간' 인지 '점심 무렵 저변동성 구간'인지 등 한 줄 코멘트 추가 (시간이 안 보이면 이 항목은 생략).\n\n" +
    "[출력 형식 — 반드시 아래 HTML 태그를 사용해서 가독성 있게 작성. 롱에 유리한 근거는 <span style='color:#3ECF8E'>, 숏에 유리한 근거는 <span style='color:#E5484D'>, 애매하거나 주의할 부분은 <span style='color:#F5A623'>로 감싸서 색을 입혀]\n" +
    "<b>1. VWAP 분석 (OO/35점):</b> 상세 분석 (구체적 근거 인용 필수). 롱 기준 점수와 숏 기준 점수를 각각 밝히고, 불리한 쪽도 왜 그 점수인지 구체 근거를 1문장 이상 써.\n" +
    "<b>2. EMA 분석 (OO/40점):</b> 상세 분석 (구체적 근거 인용 필수). 롱 기준 점수와 숏 기준 점수를 각각 밝히고, 불리한 쪽도 왜 그 점수인지 구체 근거를 1문장 이상 써.\n" +
    "<b>3. Stoch RSI 분석 (OO/21점):</b> 상세 분석 (구체적 근거 인용 필수). 롱 기준 점수와 숏 기준 점수를 각각 밝히고, 불리한 쪽도 왜 그 점수인지 구체 근거를 1문장 이상 써.\n" +
    "<br><b>💡 롱 가능 점수: OO / 96점</b>\n" +
    "<b>💡 숏 가능 점수: OO / 96점</b>\n" +
    "<b>📍 손절가/목표가/손익비:</b> 한 줄 서술\n" +
    "<b>🔥 아지즈의 한 줄 평:</b> 롱/숏/관망 중 하나를 명확히 제시하고, 그 이유와 구체적 행동 지침(진입 기준가, 손절 기준)을 서술. 반드시 다음 세 가지를 포함: (1) 방향(롱/숏/관망), (2) 진입을 고려할 가격 조건 1개, (3) 무효화(손절) 조건 1개. 점수가 양쪽으로 모두 55점 미만이거나, 한쪽이 높아도 손익비가 1.2:1 미만이면 기본값은 '관망'으로 둬.\n" +
    "<br><span style='color:#6E7B8B; font-size:11px;'>※ 이 분석은 과거·후행 지표 기반의 연습·참고용 판단이며, 실제 투자 조언이 아닙니다.</span>";
    public static void main(String[] args) throws Exception {
        if (APP_KEY == null || APP_SECRET == null) {
            System.out.println("🚨 KIS_APP_KEY / KIS_APP_SECRET 환경변수가 설정되지 않았습니다.");
        }
        if (CLAUDE_KEY == null || CLAUDE_KEY.isEmpty()) {
            System.out.println("⚠️ CLAUDE_API_KEY 환경변수가 설정되지 않았습니다.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/data", new DataHandler());
        server.createContext("/analyze", new AnalyzeHandler());
        server.createContext("/analyze-claude", new ClaudeAnalyzeHandler());
        server.createContext("/us-data", new UsDataHandler());
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
                    String tokenReqBody = String.format(
                            "{\"grant_type\":\"client_credentials\", \"appkey\":\"%s\", \"appsecret\":\"%s\"}",
                            APP_KEY, APP_SECRET);
                    HttpRequest tokenReq = HttpRequest.newBuilder()
                            .uri(URI.create(DOMAIN + "/oauth2/tokenP"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(tokenReqBody))
                            .build();
                    HttpResponse<String> tokenRes = HttpClient.newHttpClient()
                            .send(tokenReq, HttpResponse.BodyHandlers.ofString());
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
                if ("D".equalsIgnoreCase(tf)) {
                    java.time.format.DateTimeFormatter dtf =
                            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
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
                    HttpResponse<String> dataRes = HttpClient.newHttpClient()
                            .send(dataReq, HttpResponse.BodyHandlers.ofString());
                    responseBodyJson = dataRes.body();
                } else {
                    java.util.List<String> combinedOutput2 = new java.util.ArrayList<>();
                    String targetHour = "153000";
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
                        HttpResponse<String> dataRes = HttpClient.newHttpClient()
                                .send(dataReq, HttpResponse.BodyHandlers.ofString());
                        String resBody = dataRes.body();
                        int out2Idx = resBody.indexOf("\"output2\":[");
                        if (out2Idx == -1) break;
                        int bracketEnd = resBody.indexOf("]", out2Idx);
                        if (bracketEnd == -1) break;
                        String itemsStr = resBody.substring(out2Idx + 11, bracketEnd);
                        if (itemsStr.trim().isEmpty()) break;
                        String[] items = itemsStr.split("\\},\\{");
                        if (items.length == 0) break;
                        String oldestTime = "";
                        for (int j = 0; j < items.length; j++) {
                            String item = items[j];
                            if (!item.startsWith("{")) item = "{" + item;
                            if (!item.endsWith("}")) item = item + "}";
                            combinedOutput2.add(item);
                            if (j == items.length - 1) {
                                int timeIdx = item.indexOf("\"stck_cntg_hour\":\"");
                                if (timeIdx != -1) {
                                    int tStart = timeIdx + 18;
                                    int tEnd = item.indexOf("\"", tStart);
                                    oldestTime = item.substring(tStart, tEnd);
                                }
                            }
                        }
                        if (oldestTime.isEmpty() || oldestTime.compareTo(targetHour) >= 0) break;
                        targetHour = oldestTime;
                        Thread.sleep(50);
                    }
                    responseBodyJson = "{\"output1\":{},\"output2\":["
                            + String.join(",", combinedOutput2)
                            + "],\"rt_cd\":\"000000\",\"msg1\":\"정상처리 되었습니다.\"}";
                }

                byte[] responseBytes = responseBodyJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                System.out.println("📊 차트 데이터 전송 완료! (종목: " + code + ", 방식: " + tf + ")");
            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"데이터 연동 실패\"}";
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();
            }
        }
    }

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
                String reqJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + AZIZ_PROMPT
                        + "\"},{\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\""
                        + base64Image + "\"}}]}]}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(geminiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                System.out.println("🤖 Gemini 차트 분석 완료!");
            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"AI 분석 실패\"}";
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();
            }
        }
    }

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

              // JSON 안전 이스케이프 (줄바꿈·따옴표 깨짐 방지)
                String escapedPrompt = AZIZ_PROMPT
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");

                String reqJson = "{"
                        + "\"model\":\"claude-sonnet-5\","
                        + "\"max_tokens\":16000"
                        + "\"thinking\":{\"type\":\"enabled\",\"budget_tokens\":7000},"
                        + "\"messages\":[{"
                        + "\"role\":\"user\","
                        + "\"content\":["
                        + "{\"type\":\"image\",\"source\":{\"type\":\"base64\",\"media_type\":\"image/png\",\"data\":\""
                        + base64Image + "\"}},"
                        + "{\"type\":\"text\",\"text\":\"" + escapedPrompt + "\"}"
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

                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
                System.out.println("🤖 Claude Sonnet 5 차트 분석 완료! (status: " + response.statusCode() + ")");
            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"Claude AI 분석 실패: "
                        + e.getMessage().replace("\"", "'") + "\"}";
                byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorBytes);
                os.close();
            }
        }
    }

    // 미국 주식 1분봉 프록시 (Twelve Data, 무료 플랜: 하루 800회, 분당 8회)
    static class UsDataHandler implements HttpHandler {
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
                if (TWELVE_DATA_KEY == null || TWELVE_DATA_KEY.isEmpty()) {
                    String error = "{\"error\": \"TWELVE_DATA_KEY 환경변수가 설정되지 않았습니다\"}";
                    byte[] b = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(500, b.length);
                    OutputStream os = exchange.getResponseBody(); os.write(b); os.close();
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                String symbol = "AAPL";
                if (query != null && query.contains("symbol=")) {
                    symbol = query.split("symbol=")[1].split("&")[0];
                }
                symbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8);

                String url = "https://api.twelvedata.com/time_series"
                        + "?symbol=" + symbol
                        + "&interval=1min"
                        + "&outputsize=500"
                        + "&apikey=" + TWELVE_DATA_KEY;

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());

                byte[] responseBytes = res.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

                System.out.println("🇺🇸 미국 주식 1분봉 전송 완료! (심볼: " + symbol + ")");

            } catch (Exception e) {
                e.printStackTrace();
                String error = "{\"error\": \"미국 주식 데이터 연동 실패\"}";
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
