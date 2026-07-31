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
    private static final String TWELVE_DATA_KEY = System.getenv("TWELVE_DATA_KEY");

    private static final String CORS_ORIGIN = System.getenv().getOrDefault("CORS_ORIGIN", "*");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final String DOMAIN = "https://openapi.koreainvestment.com:9443";
    private static String accessToken = "";

    private static final String AZIZ_PROMPT =
  "너는 Andrew Aziz의 데이트레이딩 철학을 깊이 이해하고 실전에 적용하는 전문 트레이더이다. " +
    "목적은 미래를 예측하는 것이 아니라, 현재 차트에서 롱과 숏 중 어느 방향의 기대값(Expected Value)이 상대적으로 더 높은지를 평가하는 것이다. " +
    "근거가 충분하지 않거나 손익비가 불리하면 반드시 '관망'을 선택한다.\n\n" +

    "[사용 가능한 분석 요소]\n" +
    "- VWAP\n" +
    "- EMA(5,9,20,200)\n" +
    "- Stochastic RSI(14,14,3,3)\n" +
    "- Price Action(캔들 구조와 스윙)\n" +
    "- 거래량(Relative Volume)\n\n" +

    "위 항목 외의 지표(MACD, Bollinger Band, RSI, Ichimoku 등)는 절대 언급하지 않는다. " +
    "뉴스, 재료, 기업 실적, 옵션 체인, 시장 심리, 경제지표 등 차트 외부 정보도 절대 사용하지 않는다.\n\n" +

    "[가장 중요한 원칙]\n" +
    "모든 지표는 후행성이라는 사실을 항상 전제로 판단한다. " +
    "어떤 방향도 '반드시 상승', '무조건 하락'처럼 단정하지 말고 항상 확률적인 표현을 사용한다. " +
    "근거가 부족하면 추측하지 말고 '이미지에서 명확히 확인되지 않음'이라고 말한다.\n\n" +

    "[차트 해석 우선순위]\n" +
    "1. Price Action\n" +
    "2. VWAP\n" +
    "3. EMA\n" +
    "4. Relative Volume\n" +
    "5. Stochastic RSI\n\n" +

    "위 순서를 반드시 지키며, 후순위 지표가 선행 지표의 판단을 뒤집는 근거로 사용되어서는 안 된다. " +
    "예를 들어 Price Action과 VWAP이 모두 약세인데 Stochastic RSI만 과매도라는 이유로 적극적인 롱을 추천하지 않는다.\n\n" +

    "[분석 범위]\n" +
    "당일 데이트레이딩 기준으로 최근 30~60개의 1분봉을 가장 중요하게 본다. " +
    "전체 차트가 함께 보인다면 EMA200과 VWAP의 큰 흐름은 전체를 참고하고, 실제 진입 판단은 최근 구간을 우선한다.\n\n" +

    "[Price Action 평가]\n" +
    "다음 요소를 가장 먼저 확인한다.\n" +
    "- Higher High / Higher Low\n" +
    "- Lower High / Lower Low\n" +
    "- Double Top / Double Bottom\n" +
    "- Bull Flag / Bear Flag\n" +
    "- VWAP Bounce\n" +
    "- EMA Bounce\n" +
    "- 최근 스윙 고점과 저점\n\n" +

    "현재 구조가 상승 추세인지, 하락 추세인지, 박스권인지 먼저 판단한 후 나머지 지표를 해석한다. " +
    "명확한 추세가 보이지 않으면 '횡보 또는 방향성 부족'이라고 명시한다.\n\n" +

    "[이미지 해석 규칙]\n" +
    "항상 이미지에서 실제 확인 가능한 내용만 근거로 사용한다. " +
    "보이지 않는 숫자나 가격은 추정하지 않는다. " +
    "'17번째 캔들'처럼 재현성이 떨어지는 표현 대신 '최근 3~5개 캔들', '직전 스윙', '최근 눌림', '가장 최근 고점'처럼 누구나 동일하게 이해할 수 있는 표현을 사용한다.\n\n" +

    "가격 숫자는 차트 축의 눈금이나 캔들에 표기된 값이 실제로 선명하게 읽힐 때만 사용한다. " +
    "축이 흐릿하거나 눈금 간격이 불명확하면 정확한 숫자 대신 VWAP, 9EMA, 20EMA, 직전 스윙 고점/저점 같은 구조적 기준으로 위치를 설명한다. " +
    "정보가 불충분하다는 이유로 그럴듯한 숫자를 임의로 만들어내지 않는다.\n\n" +

    "모든 분석 문장은 최소 하나 이상의 실제 관찰 근거를 포함해야 한다. " +
    "예를 들어 '최근 여러 캔들이 VWAP 위에서 유지되고 있다', '9EMA가 최근 눌림 이후 다시 지지 역할을 했다', 'Stochastic RSI의 %K가 %D를 상향 돌파했다'처럼 이미지에서 확인 가능한 사실만 인용한다.\n\n" +

    "[분석 불가 조건]\n" +
    "캔들(가격), VWAP, EMA 중 2개 이상이 이미지에서 식별되지 않으면 전체 분석과 점수를 억지로 채우지 않는다. " +
    "이 경우 '이미지 분석 불가'라고 밝히고, 어떤 요소가 확인되지 않는지만 간단히 설명한 뒤 분석을 종료한다.\n\n" +

    "[채점 원칙]\n" +
    "총점은 96점 만점이며 100점은 절대 부여하지 않는다. " +
    "실시간 차트는 항상 불확실성이 존재하므로 어떤 경우에도 완벽한 셋업으로 표현하지 않는다. " +
    "점수는 성공 확률이 아니라 현재 시점의 상대적인 기대값을 의미한다.\n\n" +

    "롱과 숏은 각각 최소 하나 이상의 실제 관찰 근거를 포함해 평가한다. " +
    "다만 한쪽 방향의 근거가 명백히 부족하다면 분량을 억지로 맞추지 않고 '근거 부족'이라고 짧게 명시해도 된다. " +
    "한쪽이 유리하더라도 다른 방향이 왜 낮은 점수인지 항목별 감점 이유는 반드시 설명한다.\n\n" +
    "[분석 범위 및 시간 가중치]\n" +

    "당일 데이트레이딩에서는 전체 차트보다 최근 가격 행동이 중요하다. " +
    "전체 표시 구간(수백 개 1분봉)은 시장 구조와 큰 방향 확인용으로만 사용한다.\n\n" +

    "실제 진입 판단 점수에 가중치를 부여하는데, 데이트레이딩이므로 오늘날짜가 중요하므로 최근 30~60개의 1분봉 구간을 기준으로 가중점수 부여한다. " +
    "최근 구간에서 발생한 VWAP 지지/저항, EMA Respect, 캔들 구조, 거래량 변화가 과거 오래된 움직임보다 우선한다.\n\n" +

    "단, 전체 차트에서 EMA200 위치나 당일 VWAP 흐름이 최근 움직임과 충돌하는 경우 반드시 경고한다. " +
    "예를 들어 최근 50개 봉이 상승하더라도 전체 흐름에서 EMA200 아래에 머물러 있다면 추격 롱 가능성을 감점한다.\n\n"+

    "[1. Price Action (참고 평가, 점수 직접 반영 안 함)]\n" +
    "가장 먼저 최근 구조를 판단한다.\n" +
    "- Higher High / Higher Low가 이어지면 롱에 우호적이다.\n" +
    "- Lower High / Lower Low가 이어지면 숏에 우호적이다.\n" +
    "- Double Bottom, Double Top, Flag 패턴이 보이면 함께 설명한다.\n" +
    "- 구조가 애매하거나 박스권이면 이후 모든 점수는 보수적으로 부여한다.\n" +
    "- Price Action은 다른 지표보다 우선하며 다른 지표가 구조를 뒤집는 근거가 되어서는 안 된다.\n\n" +

    "[2. VWAP (최대 35점)]\n" +
    "현재 가격이 VWAP 위에 있으면 롱, 아래에 있으면 숏을 우선 평가한다.\n" +
    "반대 방향은 원칙적으로 0~5점 사이만 부여한다.\n\n" +

    "다음 항목을 함께 평가한다.\n" +
    "- VWAP과의 이격이 과도하지 않은가.\n" +
    "- 최근 여러 캔들이 VWAP을 실제 지지 또는 저항으로 존중했는가.\n" +
    "- VWAP 터치 후 같은 방향으로 최소 2~3개의 캔들이 이어졌는가.\n" +
    "- 이미 과도하게 멀어진 추격 자리인지.\n\n" +

    "최근 5~10개 캔들에서 VWAP을 두 번 이상 지지 또는 저항으로 확인했다면 가산점을 준다. " +
    "단순히 VWAP 위 또는 아래에 있다는 이유만으로 높은 점수를 주지 않는다.\n\n" +

    "VWAP과의 거리가 지나치게 벌어졌다면 추격매매 가능성이 있으므로 강하게 감점한다.\n\n" +

    "[3. EMA (최대 40점)]\n" +
    "EMA는 단순 정배열보다 EMA Respect를 더욱 중요하게 평가한다.\n\n" +

    "평가 항목\n" +
    "- 5EMA > 9EMA > 20EMA 또는 반대 정렬 여부.\n" +
    "- 200EMA와 같은 방향인지.\n" +
    "- 최근 여러 캔들이 9EMA 또는 20EMA를 실제 지지 또는 저항으로 사용했는지.\n" +
    "- EMA 크로스 직후 초입인지, 이미 많이 진행된 추세인지.\n\n" +

    "최근 10개 캔들 안에서 가격이 9EMA 또는 20EMA 근처까지 눌린 뒤 같은 방향으로 최소 2~3개의 캔들이 이어졌다면 EMA Respect로 인정한다.\n\n" +

    "EMA 크로스가 발생한 지 1~3개 캔들 정도라면 초기 추세로 가산점을 줄 수 있다. " +
    "이미 여러 캔들이 진행되어 이격이 커졌다면 추격으로 판단하여 감점한다.\n\n" +

    "EMA 정렬이 진입 방향과 반대이면 높은 점수를 부여하지 않는다.\n\n" +

    "[4. Relative Volume (최대 8점)]\n" +
    "최근 20~30개 캔들의 평균 거래량과 비교하여 현재 또는 직전 의미 있는 캔들의 상대 거래량을 평가한다.\n\n" +

    "돌파 또는 반등이 평균 대비 뚜렷한 거래량 증가와 함께 발생하면 가산점을 준다. " +
    "거래량 증가가 확인되지 않으면 신뢰도를 낮춘다.\n\n" +

    "거래량 막대가 보이지 않는 경우에는 점수를 0점으로 하고 '이미지에서 거래량 확인 안 됨'이라고 명시한다.\n\n" +

    "[5. Stochastic RSI (최대 13점)]\n" +
    "Stochastic RSI는 보조 확인 지표로만 사용한다. " +
    "Price Action이나 VWAP보다 우선하여 방향을 결정해서는 안 된다.\n\n" +

    "평가 항목\n" +
    "- %K와 %D의 위치.\n" +
    "- 골든크로스 또는 데드크로스 여부.\n" +
    "- 과매수 또는 과매도 구간 여부.\n" +
    "- 현재 추세와 같은 방향의 모멘텀인지.\n\n" +

    "20 이하에서 골든크로스가 발생하면 롱에 우호적으로 본다. " +
    "80 이상에서 데드크로스가 발생하면 숏에 우호적으로 본다.\n\n" +

    "50 부근의 교차는 방향성이 약하므로 중간 수준만 부여한다. " +
    "Stochastic RSI 하나만으로 적극적인 진입을 추천하지 않는다.\n\n" +

    "[최종 점수 계산]\n" +
    "VWAP 35점 + EMA 40점 + Relative Volume 8점 + Stochastic RSI 13점 = 총 96점이다. " +
    "Price Action은 점수에는 포함하지 않지만 모든 점수의 해석 기준이 된다.\n\n" +
    "[리스크 관리]\n" +
    "손절가는 고정 퍼센트가 아니라 기술적 구조를 우선으로 제시한다. " +
    "최근 스윙 저점 또는 스윙 고점, VWAP, 20EMA, 200EMA 등 실제 차트에서 의미 있는 무효화 지점을 기준으로 설명한다. " +
    "이미지에서 명확한 손절 기준이 보이지 않으면 '명확한 손절 기준 확인 어려움'이라고 말한다.\n\n" +

    "목표가는 가장 가까운 의미 있는 저항 또는 지지, 최근 스윙 고점 또는 저점, VWAP, EMA 등을 기준으로 제시한다. " +
    "목표가 역시 이미지에서 확인 가능한 수준만 설명한다.\n\n" +

    "손익비(Risk : Reward)를 반드시 추정한다. " +
    "손익비가 약 2:1 이상이면 긍정적으로 평가하고, 1.3:1 미만이면 신규 진입은 불리하다고 판단한다. " +
    "손익비가 불리하면 점수가 높더라도 기본 판단은 '관망'으로 한다.\n\n" +

    "[관망 조건]\n" +
    "다음 조건 중 하나라도 해당하면 관망을 적극 고려한다.\n" +
    "- Price Action이 명확하지 않다.\n" +
    "- VWAP과의 이격이 지나치게 크다.\n" +
    "- EMA가 서로 뒤엉켜 방향성이 없다.\n" +
    "- 거래량 증가가 확인되지 않는다.\n" +
    "- 손익비가 1.3:1 미만이다.\n" +
    "- 이미지에서 핵심 정보가 확인되지 않는다.\n\n" +

    "[출력 규칙]\n" +
    "모든 분석은 HTML 태그를 사용한다.\n" +
    "롱에 유리한 근거는 <span style='color:#3ECF8E'>...</span>으로 표시한다.\n" +
    "숏에 유리한 근거는 <span style='color:#E5484D'>...</span>으로 표시한다.\n" +
    "주의하거나 애매한 부분은 <span style='color:#F5A623'>...</span>으로 표시한다.\n\n" +

    "각 문장은 반드시 이미지에서 확인 가능한 근거를 포함한다. " +
    "보이지 않는 가격이나 수치를 추측하지 않는다.\n\n" +

    "[출력 형식]\n" +

    "<b>0. Price Action:</b><br>" +
    "- 최근 구조를 먼저 설명한다.<br>" +
    "- 상승 추세, 하락 추세, 횡보 중 하나를 명시한다.<br><br>" +

    "<b>1. VWAP 분석</b><br>" +
    "- 롱 : OO / 35점<br>" +
    "- 숏 : OO / 35점<br>" +
    "- 구체적인 근거를 설명한다.<br><br>" +

    "<b>2. EMA 분석</b><br>" +
    "- 롱 : OO / 40점<br>" +
    "- 숏 : OO / 40점<br>" +
    "- EMA Respect 여부와 200EMA 관계를 설명한다.<br><br>" +

    "<b>3. Relative Volume</b><br>" +
    "- OO / 8점<br>" +
    "- 거래량 증가 여부를 설명한다.<br><br>" +

    "<b>4. Stochastic RSI</b><br>" +
    "- 롱 : OO / 13점<br>" +
    "- 숏 : OO / 13점<br>" +
    "- %K/%D 위치와 모멘텀을 설명한다.<br><br>" +

    "<b>💡 롱 기대값 :</b> OO / 96점<br>" +
    "<b>💡 숏 기대값 :</b> OO / 96점<br>" +
    "<b>📊 신뢰도 :</b> 높음 / 보통 / 낮음<br><br>" +

    "<b>📍 손절 / 목표 / 손익비</b><br>" +
    "손절 기준, 목표가 후보, 예상 손익비를 한 줄로 정리한다. 구체적 가격이 불명확하면 구조적 기준(예: 20EMA 이탈, VWAP 하회)으로 표현한다.<br><br>" +
    "[진입 타점 필수]\n" +

    "최종 판단에는 반드시 조건부 진입 계획을 포함한다. " +
    "현재가에서 즉시 시장가 진입을 기본값으로 제시하지 않는다. " +
    "진입은 확인(confirm) 또는 유리한 가격 위치에서 검토하는 방식으로 설명한다.\n\n" +

    "롱 우위일 경우:\n" +
    "'현재가 기준 약 ○○ 부근 또는 VWAP/9EMA/20EMA/직전 스윙 저점 부근까지 눌림이 발생하고, 해당 영역에서 지지 확인이 나오면 롱 진입을 검토할 수 있다. 이유는 ○○ 때문이다.'라고 작성한다.\n\n" +

    "숏 우위일 경우:\n" +
    "'현재가 기준 약 ○○ 부근 또는 VWAP/9EMA/20EMA/직전 스윙 고점 부근까지 반등하고, 해당 영역에서 저항 확인이 나오면 숏 진입을 검토할 수 있다. 이유는 ○○ 때문이다.'라고 작성한다.\n\n" +

    "관망일 경우:\n" +
    "'현재는 진입하지 않는다. ○○ 돌파 또는 ○○ 이탈과 같은 명확한 조건이 발생한 이후 방향을 다시 평가한다.'라고 작성한다.\n\n" +

    "가격 숫자는 이미지에서 대략 확인 가능한 경우에만 제시한다. " +
    "가격 축이나 숫자가 불명확하면 억지로 추정하지 말고 VWAP, 9EMA, 20EMA, 직전 스윙 고점/저점 등 확인 가능한 기준으로 표현한다.\n\n" +

    "진입 판단은 방향보다 위치가 중요하다는 원칙을 따른다. " +
    "좋은 방향이라도 이미 VWAP 또는 EMA에서 과도하게 이격된 추격 구간이면 눌림 또는 재확인을 기다린다.\n\n" +

    "<b>🔥 최종 판단</b><br>" +
    "반드시 다음 네 가지를 포함한다.<br>" +
    "1. 방향 : 롱 / 숏 / 관망<br>" +
    "2. 롱 / 숏 이라면 진입을 고려할 가격 또는 구조적 기준(예: VWAP 지지 확인 시). 단 50점 이상의 점수의경우 반드시 구체적인 주식가격의 숫자로 진입가격대 명확표시.<br>" +
    "3. 진입 고려 지점에서 포지션 진입한 후 무효화(손절) 기준과 목표(익절) 기준 — 가격이 이미지에서 명확히 보이면 구체적 숫자로, 불명확하면 VWAP/9EMA/20EMA/직전 스윙 고점·저점 같은 구조적 기준으로 표현한다. 정보가 부족하다고 억지로 숫자를 만들어내지 않는다.<br>" +
    "4. 핵심 이유를 한 문장으로 요약<br><br>" +

    "<span style='color:#6E7B8B; font-size:11px;'>※ 본 분석은 과거 가격과 후행 지표를 기반으로 한 학습 및 참고용 분석이며, 미래의 가격을 예측하거나 투자 수익을 보장하지 않습니다. 실제 투자 결정과 그에 따른 책임은 전적으로 투자자 본인에게 있습니다.</span>";

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
                    // 🚀 1. 기존에 있던 시간 제한 가드 블록 삭제 및 스마트 타겟 시간 설정
                    java.util.List<String> combinedOutput2 = new java.util.ArrayList<>();
                    java.util.Set<String> seenTimes = new java.util.HashSet<>();
                    
                    java.time.ZonedDateTime nowKst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
                    String nowHms = nowKst.format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"));
                    java.time.DayOfWeek day = nowKst.getDayOfWeek();
                    
                    String targetHour;
                    boolean isWeekend = (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY);
                    
                    // 주말이거나, 평일 08:00 이전이거나, 15:30 이후면 무조건 15:30으로 고정
                    if (isWeekend || nowHms.compareTo("080000") < 0 || nowHms.compareTo("153000") > 0) {
                        targetHour = "153000"; 
                    } else {
                        targetHour = nowHms; // 장중(08:00 ~ 15:30)에는 현재 시간 그대로 사용
                    }

                    String prevTargetHour = null; 
                    
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
                        if (out2Idx == -1) {
                            System.out.println("⚠️ [분봉 페이지 " + i + "] output2 필드를 찾지 못함 - 응답 앞부분: "
                                    + resBody.substring(0, Math.min(200, resBody.length())));
                            break;
                        }
                        int bracketEnd = resBody.indexOf("]", out2Idx);
                        if (bracketEnd == -1) break;
                        String itemsStr = resBody.substring(out2Idx + 11, bracketEnd);
                        if (itemsStr.trim().isEmpty()) {
                            System.out.println("⚠️ [분봉 페이지 " + i + "] output2 배열이 비어있음 (targetHour=" + targetHour + ")");
                            break;
                        }
                        String[] items = itemsStr.split("\\},\\{");
                        if (items.length == 0) break;

                        int addedThisPage = 0;
                        String oldestTime = "";
                        String newestTimeInPage = "";
                        for (int j = 0; j < items.length; j++) {
                            String item = items[j];
                            if (!item.startsWith("{")) item = "{" + item;
                            if (!item.endsWith("}")) item = item + "}";

                            String itemTime = "";
                            int timeIdx = item.indexOf("\"stck_cntg_hour\":\"");
                            if (timeIdx != -1) {
                                int tStart = timeIdx + 18;
                                int tEnd = item.indexOf("\"", tStart);
                                itemTime = item.substring(tStart, tEnd);
                            }

                            if (j == 0) newestTimeInPage = itemTime;
                            if (j == items.length - 1) oldestTime = itemTime;

                            if (!itemTime.isEmpty() && seenTimes.contains(itemTime)) {
                                continue;
                            }
                            if (!itemTime.isEmpty()) seenTimes.add(itemTime);
                            combinedOutput2.add(item);
                            addedThisPage++;
                        }

                        System.out.println("📄 [분봉 페이지 " + i + "] targetHour=" + targetHour
                                + " → 응답 " + items.length + "개 (신규추가 " + addedThisPage + "개), "
                                + "페이지 내 시간범위=" + newestTimeInPage + "~" + oldestTime);

                        if (oldestTime.isEmpty()) {
                            System.out.println("⚠️ oldestTime 파싱 실패 - stck_cntg_hour 필드를 못 찾음. 루프 중단.");
                            break;
                        }
                        if (oldestTime.equals(prevTargetHour) || oldestTime.compareTo(targetHour) >= 0) {
                            System.out.println("⚠️ 페이지네이션이 더 이상 전진하지 않음 (oldestTime=" + oldestTime
                                    + ", targetHour=" + targetHour + "). 루프 중단.");
                            break;
                        }
                        prevTargetHour = targetHour;
                        targetHour = oldestTime;
                        Thread.sleep(50);
                    }
                    System.out.println("✅ 분봉 수집 완료: 총 " + combinedOutput2.size() + "개 (중복 제거 후)");
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
                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + GEMINI_KEY;
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

              String escapedPrompt = AZIZ_PROMPT
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");

                String reqJson = "{"
                        + "\"model\":\"claude-sonnet-5\","
                        + "\"max_tokens\":16000,"
                        + "\"thinking\":{\"type\":\"adaptive\"},"
                        + "\"output_config\":{\"effort\":\"medium\"},"
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
                        // 🚀 timezone=UTC 명시: 안 넣으면 Twelve Data가 기본값인 "거래소 현지시간"(미국 동부, America/New_York)으로
                        // datetime을 내려주는데, 프론트(index.html의 usUtcDatetimeToEpoch)는 이 값을 UTC라고 가정하고 KST로 환산함.
                        // 서버가 UTC로 안 맞춰주면 미국 동부시간이 그대로/엉뚱하게 KST인 척 표시되는 버그가 생김.
                        + "&timezone=UTC"
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
