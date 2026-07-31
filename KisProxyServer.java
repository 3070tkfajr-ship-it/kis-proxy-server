<!DOCTYPE html>

<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>실전 단타 대시보드 & 시뮬레이터 (완전체)</title>
<style>
  :root{
    --bg: #0A0E13; --panel: #10161F; --panel-2: #151C27; --line: #232C38;
    --text: #DCE4EC; --muted: #6E7B8B; --red: #E5484D; --blue: #3B82F6;
    --amber: #F5A623; --cyan: #35C9C1; --pink: #E0629E; --grayline: #8A93A0;
    --good: #3ECF8E; --bad: #E5484D;
    --mono: 'SFMono-Regular','JetBrains Mono',ui-monospace,Menlo,Consolas,monospace;
    --sans: -apple-system,'Apple SD Gothic Neo','Pretendard',BlinkMacSystemFont,'Segoe UI',sans-serif;
  }
  *{box-sizing:border-box;}
  body{ margin:0; background:var(--bg); color:var(--text); font-family:var(--sans); -webkit-font-smoothing:antialiased; padding-bottom:52px; }
  #watchlistBarWrap{ position:fixed; left:0; right:0; bottom:0; z-index:50; background:var(--panel); border-top:1px solid var(--line); padding:8px 10px; box-sizing:border-box; }
  #watchlistBar{ display:flex; gap:6px; overflow-x:auto; -webkit-overflow-scrolling:touch; }
  .watch-btn{ flex:0 0 auto; padding:8px 12px; border-radius:8px; border:1px solid var(--line); background:var(--panel-2); color:var(--text); font-size:12px; font-weight:600; white-space:nowrap; cursor:pointer; }
  .watch-btn:active{ background:var(--cyan); color:#04342C; }
  .wrap{max-width:1040px;margin:0 auto;padding:18px 16px 60px;}
  .topbar{ display:flex; align-items:baseline; justify-content:space-between; border-bottom:1px solid var(--line); padding-bottom:14px; margin-bottom:14px; flex-wrap:wrap; gap:8px; }
  .topbar h1{font-size:17px; font-weight:700; margin:0; letter-spacing:-0.2px;}
  .topbar h1 span{color:var(--muted); font-weight:500; font-size:12px; display:block; margin-top:3px;}
  .tape{ font-family:var(--mono); font-size:12.5px; color:var(--grayline); display:flex; gap:14px; align-items:center; }
  .tape b{color:var(--text); font-weight:700;}
  .dot{width:7px;height:7px;border-radius:50%; display:inline-block; margin-right:5px;}
  .panel{background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:14px; margin-bottom:12px;}
  .panel h2{font-size:12px; text-transform:uppercase; letter-spacing:.06em; color:var(--muted); margin:0 0 10px;}
  .srcbar{display:flex; gap:8px; flex-wrap:wrap; align-items:center;}
  textarea{ width:100%; min-height:64px; background:var(--panel-2); color:var(--text); border:1px solid var(--line); border-radius:8px; font-family:var(--mono); font-size:11.5px; padding:8px; resize:vertical; margin-top:8px; }
  .fmt{font-size:11px; color:var(--muted); margin-top:6px; line-height:1.5;}
  button{ font-family:var(--sans); font-size:13px; font-weight:600; border-radius:8px; padding:9px 14px; border:1px solid var(--line); background:var(--panel-2); color:var(--text); cursor:pointer; transition:background .12s, transform .05s; }
  button:hover{background:#1B2432;} button:active{transform:scale(.98);} button:disabled{opacity:.35; cursor:not-allowed;}
  button.primary{background:var(--red); border-color:var(--red); color:#fff;} button.primary:hover{background:#F0575C;}
  button.buy{background:var(--good); border-color:var(--good); color:#062018; font-weight:700;} button.buy:hover{background:#49E0A0;}
  button.ghost{background:transparent;}
  button.live-btn {background:#2962ff; border-color:#2962ff; color:#fff;} button.live-btn:hover {background:#1544cb;}
  .badge{ display:inline-flex; align-items:center; gap:6px; font-size:11px; font-family:var(--mono); padding:4px 9px; border-radius:20px; border:1px solid var(--line); color:var(--muted); }
  .badge.live{color:var(--amber); border-color:#4A3B1A;} .badge.real{color:var(--good); border-color:#1C4433;}
  .chartTitle{display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;}
  .legend{display:flex; gap:12px; flex-wrap:wrap; font-family:var(--mono); font-size:10.5px; color:var(--muted);}
  .legend b{display:inline-block; width:16px; height:2px; margin-right:5px; vertical-align:middle; border-radius:2px;}
  canvas{width:100%; display:block; background:var(--panel-2); border-radius:8px;}
  #priceCanvas{cursor:crosshair;}
  .controls{display:flex; gap:8px; flex-wrap:wrap; margin-top:12px; align-items:center;}
  .warn{color:var(--amber); font-size:12px; font-family:var(--mono);}
  .resultGrid{display:grid; grid-template-columns:repeat(auto-fit,minmax(120px,1fr)); gap:10px; margin-top:12px;}
  .stat{background:var(--panel-2); border:1px solid var(--line); border-radius:8px; padding:10px;}
  .stat .k{font-size:10.5px; color:var(--muted); text-transform:uppercase; letter-spacing:.04em;}
  .stat .v{font-family:var(--mono); font-size:16px; font-weight:700; margin-top:3px;}
  .up{color:var(--red);} .down{color:var(--blue);}
  .snapshot{margin-top:12px; font-size:13px; line-height:1.9;}
  .snapshot code{font-family:var(--mono); background:var(--panel-2); padding:1px 6px; border-radius:5px; font-size:12px;}
  .scoreCard{display:flex; gap:16px; align-items:center; background:var(--panel-2); border:1px solid var(--line); border-radius:10px; padding:14px; margin-bottom:12px; flex-wrap:wrap;}
  .scoreCircle{font-family:var(--mono); font-weight:800; font-size:30px; min-width:64px; text-align:center;}
  .scoreBars{flex:1; min-width:220px; display:flex; flex-direction:column; gap:6px;}
  .scoreBarRow{display:flex; align-items:center; gap:8px; font-size:11.5px;}
  .scoreBarRow .lbl{width:92px; color:var(--muted); flex-shrink:0;}
  .scoreBarTrack{flex:1; background:#1B2432; border-radius:5px; height:8px; overflow:hidden;}
  .scoreBarFill{height:100%; border-radius:5px;}
  .scoreBarRow .num{font-family:var(--mono); width:48px; text-align:right; flex-shrink:0;}
  footer{color:var(--muted); font-size:11px; text-align:center; margin-top:26px; line-height:1.7;}
</style>
</head>
<body>
  <div id="watchlistBarWrap">
    <div id="watchlistBar"></div>
  </div>
<div class="wrap">

  <div class="topbar">
    <h1>실전 단타 대시보드 & 시뮬레이터<span>VWAP / EMA(5,9,20,200) / Stochastic RSI — 실시간 모니터링 및 복기 훈련</span></h1>
    <div class="tape" id="tape">
      <span><span class="dot" style="background:var(--muted)"></span>대기중</span>
    </div>
  </div>

  <div class="panel">
    <h2>데이터 소스 설정</h2>
    <div class="srcbar">
      <span id="srcBadge" class="badge live">● 합성(연습용) 데이터</span>
      <button id="btnLoadReal" class="live-btn">📡 실시간 모니터링 (한투 API)</button>
      <button id="btnLoadUs" class="live-btn">🇺🇸 미국 주식 불러오기 (Twelve Data)</button>
      <button id="btnToggleCsv" class="ghost">📁 HTS 과거 데이터 복기 ▾</button>
      <button id="btnNewRound" class="primary">🎲 과거 합성 차트 연습</button>
    </div>
    <div id="csvBox" style="display:none;">
      <textarea id="csvInput" placeholder="HTS에서 내보낸 분봉 데이터를 복사해서 붙여넣으세요. (날짜,시간,시가,고가,저가,종가,거래량)"></textarea>
      <div style="margin-top:8px;"><button id="btnLoadCsv">이 데이터로 라운드 시작</button></div>
    </div>
  </div>

   <!-- 🤖 AI 차트 스크린샷 분석 패널 -->

  <div class="panel" id="aiAnalysisPanel">
    <h2>🤖 AI 차트 스크린샷 분석</h2>
    <div style="font-size:12px; color:var(--muted); margin-bottom:12px;">
      토스증권 등 MTS/HTS에서 찍은 1분봉 차트 스크린샷을 업로드하면, 아지즈 기법 기준으로 타점을 분석해 줍니다.<br>
      <span style="color:var(--cyan);">Gemini</span> / <span style="color:var(--amber);">Claude Sonnet 5</span> 중 선택 가능
    </div>

<div class="srcbar">
  <input type="file" id="chartImageInput" accept="image/*" style="display: none;">
  <button class="ghost" onclick="document.getElementById('chartImageInput').click()">📸 스크린샷 선택</button>
  <button id="btnAnalyzeGemini" class="live-btn" disabled>✨ Gemini 분석</button>
  <button id="btnAnalyzeClaude" class="primary" disabled style="background:#D97706; border-color:#D97706;">🧠 Claude Sonnet 5 분석</button>
</div>

<!-- 이미지 미리보기 영역 -->
<div id="imagePreviewContainer" style="display:none; margin-top:15px; text-align:center;">
  <img id="chartPreview" style="max-width: 100%; border-radius: 8px; border: 1px solid var(--line);">
</div>

<!-- 분석 결과 텍스트 영역 -->
<div id="aiResultBox" style="display:none; margin-top:15px; padding:15px; background:var(--panel-2); border-left:4px solid var(--cyan); white-space:pre-wrap; line-height:1.6; font-size:13px;">
  분석 결과가 여기에 표시됩니다.
</div>

  </div>

  <div class="panel">
    <div class="chartTitle">
      <h2 style="margin:0;">가격 · VWAP · EMA(5/9/20/200)</h2>
      <div class="legend">
        <span><b style="background:var(--amber)"></b>VWAP</span>
        <span><b style="background:var(--cyan)"></b>EMA5</span>
        <span><b style="background:var(--pink)"></b>EMA9</span>
        <span><b style="background:var(--grayline)"></b>EMA20</span>
        <span><b style="background:#5B6BFF"></b>EMA200</span>
      </div>
    </div>
    <div id="priceChartWrap" style="position:relative;">
      <canvas id="priceCanvas" height="360"></canvas>
      <div id="crosshairLine" style="display:none; position:absolute; top:0; bottom:0; width:1px; background:rgba(220,228,236,0.35); pointer-events:none;"></div>
      <div id="chartTooltip" style="display:none; position:absolute; pointer-events:none; z-index:20; background:var(--panel-2); border:1px solid var(--line); border-radius:6px; padding:6px 10px; font-family:var(--mono); font-size:11px; color:var(--text); white-space:nowrap; line-height:1.6;"></div>
    </div>
    <h2 style="margin:10px 0 6px; font-size:11px;">거래량</h2>
    <canvas id="volumeCanvas" height="70"></canvas>
    <div class="fmt" id="selInfo" style="margin-top:6px;">차트를 클릭하면 그 시점을 진입 시점으로 선택할 수 있어요.</div>
    <div id="whatIfBox" style="display:none; margin-top:10px; background:var(--panel-2); border:1px solid var(--line); border-radius:8px; padding:12px;"></div>

<h2 style="margin:16px 0 8px;">Stochastic RSI (14,14,3,3)</h2>
<canvas id="stochCanvas" height="120"></canvas>

<div class="controls">
  <button id="btnStep">▶ 다음 1분 보기</button>
  <button id="btnRewindReset" class="ghost">⏹ 되감기 해제 (원래 시점으로)</button>
  <button id="btnViewAll" class="ghost">🔭 모든 차트 보기 (09:00~장마감)</button>
  <button id="btnAnalyzeCurrent" class="live-btn" style="display:none;">🤖 현재 차트 AI 분석</button>
  <span class="warn" id="closeWarn" style="display:none;">⚠ 장마감 임박 — 당일 청산 구간입니다 (오버나잇 금지)</span>
</div>
<div style="margin-top:10px;">
  <textarea id="rationale" placeholder="진입 근거 메모 (선택) — 예: VWAP 지지+EMA5/9 골든크로스+StochRSI 반등" style="min-height:40px;"></textarea>
</div>
<div class="controls">
  <button id="btnLong" class="buy">🟢 롱(매수) 진입</button>
  <button id="btnShort" class="primary">🔻 숏(공매도) 진입</button>
  <button id="btnPass" class="ghost">⏭ 패스(관망)</button>
</div>

  </div>

  <div class="panel" id="resultPanel" style="display:none;">
    <h2>결과 — 과거 차트가 실제로 갖고 있던 정답</h2>
    <div id="resultBody"></div>
    <div class="controls">
      <button id="btnCopy">📋 요약 복사 (나에게 피드백 요청용)</button>
      <button id="btnNextRound" class="primary">다음 라운드 ▶</button>
    </div>
    <div id="copyStatus" style="font-size:12px; color:var(--good); margin-top:6px; display:none;">복사됨 — 채팅창에 붙여넣어주세요!</div>
  </div>

</div>

<script>
/* ---------------- 유틸: 지표 계산 ---------------- */
function ema(values, period){
  const out = new Array(values.length).fill(null);
  if(values.length < period) return out;
  let seed = 0; for(let i=0;i<period;i++) seed += values[i]; seed/=period;
  out[period-1] = seed; const k = 2/(period+1); let prev = seed;
  for(let i=period;i<values.length;i++){ const v = values[i]*k + prev*(1-k); out[i]=v; prev=v; }
  return out;
}
function vwapSeries(candles){
  const out = new Array(candles.length).fill(null);
  let cumPV=0, cumV=0;
  for(let i=0;i<candles.length;i++){
    const c = candles[i]; const tp = (c.h+c.l+c.c)/3;
    cumPV += tp*c.v; cumV += c.v;
    out[i] = cumV>0 ? cumPV/cumV : c.c;
  }
  return out;
}
function atrSeries(candles, period){
  const n = candles.length; const tr = new Array(n).fill(null);
  for(let i=0;i<n;i++){
    const c = candles[i];
    if(i===0){ tr[i] = c.high-c.low; continue; }
    const prevClose = candles[i-1].close;
    tr[i] = Math.max(c.high-c.low, Math.abs(c.high-prevClose), Math.abs(c.low-prevClose));
  }
  const out = new Array(n).fill(null);
  if(n<period) return out;
  let seed=0; for(let i=0;i<period;i++) seed+=tr[i]; seed/=period; out[period-1]=seed;
  let prev=seed; const k=1/period;
  for(let i=period;i<n;i++){ const v = tr[i]*k + prev*(1-k); out[i]=v; prev=v; }
  return out;
}
function rsiSeries(closes, period){
  const out = new Array(closes.length).fill(null);
  if(closes.length<period+1) return out;
  let gains=0, losses=0;
  for(let i=1;i<=period;i++){ const d = closes[i]-closes[i-1]; if(d>=0) gains+=d; else losses-=d; }
  let avgG=gains/period, avgL=losses/period;
  out[period] = avgL===0?100:100-100/(1+avgG/avgL);
  for(let i=period+1;i<closes.length;i++){
    const d = closes[i]-closes[i-1]; const g = d>0?d:0, l = d<0?-d:0;
    avgG = (avgG*(period-1)+g)/period; avgL = (avgL*(period-1)+l)/period;
    out[i] = avgL===0?100:100-100/(1+avgG/avgL);
  }
  return out;
}
function sma(values, period){
  const out = new Array(values.length).fill(null);
  for(let i=0;i<values.length;i++){
    if(values[i]==null) continue; let sum=0, cnt=0, ok=true;
    for(let k=0;k<period;k++){ const idx=i-k; if(idx<0 || values[idx]==null){ok=false;break;} sum+=values[idx]; cnt++; }
    if(ok && cnt===period) out[i]=sum/period;
  }
  return out;
}
function stochRsiSeries(closes){
  const rsiP=14, stochP=14; const rsi = rsiSeries(closes, rsiP); const rawK = new Array(closes.length).fill(null);
  for(let i=0;i<closes.length;i++){
    if(rsi[i]==null) continue; let lo=Infinity, hi=-Infinity, ok=true;
    for(let k=0;k<stochP;k++){ const idx=i-k; if(idx<0 || rsi[idx]==null){ok=false;break;} lo=Math.min(lo,rsi[idx]); hi=Math.max(hi,rsi[idx]); }
    if(ok) rawK[i] = hi===lo ? 0 : (rsi[i]-lo)/(hi-lo)*100;
  }
  const K = sma(rawK,3); const D = sma(K,3); return {K,D};
}

/* ---------------- 데이터 파싱 & 생성 ---------------- */
function mulberry32(a){
  return function(){
    a|=0; a=a+0x6D2B79F5|0; let t=Math.imul(a^a>>>15,1|a); t=t+Math.imul(t^t>>>7,61|t)^t;
    return ((t^t>>>14)>>>0)/4294967296;
  }
}
/* ---------------- KST 시각 유틸 ---------------- */
// 브라우저 로컬 타임존과 무관하게 "지금"의 KST 연/월/일을 얻는다
function getKstYMD(date){
  const parts = new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Seoul', year:'numeric', month:'2-digit', day:'2-digit'}).formatToParts(date || new Date());
  const y = +parts.find(p=>p.type==='year').value, m = +parts.find(p=>p.type==='month').value, d = +parts.find(p=>p.type==='day').value;
  return {y,m,d};
}
// KST 벽시계 기준 y/m/d h:mi:s → 실제 UTC epoch(ms). KST는 연중 UTC+9 고정(서머타임 없음)이라 단순 뺄셈으로 충분.
function kstWallToEpoch(y,m,d,h,mi,s){ return Date.UTC(y, m-1, d, (h||0)-9, mi||0, s||0); }
// 한투 API 형식(YYYYMMDD, HHMMSS 문자열)을 epoch로 변환
function krDateTimeToEpoch(dateStr, hourStr){
  const ds = (dateStr && dateStr.length===8) ? dateStr : (()=>{ const t=getKstYMD(); return `${t.y}${String(t.m).padStart(2,'0')}${String(t.d).padStart(2,'0')}`; })();
  const hs = (hourStr && hourStr.length>=4) ? hourStr.padEnd(6,'0') : '153000';
  const y=+ds.slice(0,4), mo=+ds.slice(4,6), d=+ds.slice(6,8);
  const h=+hs.slice(0,2), mi=+hs.slice(2,4), s=+hs.slice(4,6);
  return kstWallToEpoch(y,mo,d,h,mi,s);
}
// Twelve Data "YYYY-MM-DD HH:mm:ss" (요청 시 &timezone=UTC 지정했다는 전제 하에 UTC로 해석)
function usUtcDatetimeToEpoch(s){
  if(!s) return null;
  const [datePart, timePart] = s.split(' ');
  const [y,mo,d] = datePart.split('-').map(Number);
  const [h,mi,se] = (timePart||'00:00:00').split(':').map(Number);
  return Date.UTC(y, mo-1, d, h, mi||0, se||0);
}

function genSyntheticSession(seed){
  const rnd = mulberry32(seed); const N = 380; const base = 40000 + Math.floor(rnd()*160)*500;
  const {y,m,d} = getKstYMD();
  const sessionStartEpoch = kstWallToEpoch(y,m,d,9,0,0); // 합성 데이터는 "오늘" 09:00 KST부터 시작한다고 가정
  let price = base; const candles = []; let regimeLen=0, drift=0;
  for(let i=0;i<N;i++){
    if(regimeLen<=0){ regimeLen = 15 + Math.floor(rnd()*35); drift = (rnd()-0.5)*0.0009; }
    regimeLen--;
    const tOfDay = i/N; let volMult = 1.0;
    if(tOfDay<0.08) volMult=1.8; else if(tOfDay>0.92) volMult=1.5; else if(tOfDay>0.45 && tOfDay<0.58) volMult=0.55;
    const noise = (rnd()-0.5)*2* (price*0.0018) * volMult;
    const open = price; let close = open + drift*open + noise;
    if(close<base*0.85) close = base*0.85 + rnd()*50;
    const wick = Math.abs(noise)*(0.4+rnd()*0.8) + price*0.0004;
    const high = Math.max(open,close) + wick*rnd(); const low = Math.min(open,close) - wick*rnd();
    const baseVol = 8000 + rnd()*6000; const volMultV = volMult*(1+Math.abs(close-open)/price*40);
    const volume = Math.max(500, Math.round(baseVol*volMultV*(0.6+rnd()*0.8)));
    candles.push({t:i, open, high, low, close: close, o:open, h:high, l:low, c:close, v:volume, epochMs: sessionStartEpoch + i*60000});
    price = close;
  }
  return {label:`합성 종목 #${seed}`, candles};
}
function parseCsv(text){
  const lines = text.split(/\r?\n/).map(l=>l.trim()).filter(l=>l.length>0); const candles = []; let idx=0;
  const {y,m,d} = getKstYMD(); const csvStartEpoch = kstWallToEpoch(y,m,d,9,0,0); // 날짜/시간 컬럼은 무시하고 있어 임시로 오늘 09:00부터로 표기 (참고용)
  for(const line of lines){
    const parts = line.split(/[,\t]/).map(s=>s.trim()).filter(s=>s.length>0); const numericTail = parts.slice(-5);
    if(numericTail.some(p=>isNaN(parseFloat(p.replace(/[^\d.\-]/g,''))))) continue;
    let o,h,l,c,v;
    if(parts.length>=6){ [o,h,l,c,v] = numericTail.map(p=>parseFloat(p.replace(/,/g,''))); } 
    else if(parts.length===5){ [o,h,l,c,v] = parts.map(p=>parseFloat(p.replace(/,/g,''))); } else continue;
    if([o,h,l,c,v].some(x=>isNaN(x))) continue;
    candles.push({t:idx, open:o, high:h, low:l, close:c, o,h,l,c,v, epochMs: csvStartEpoch + idx*60000}); idx++;
  }
  return candles;
}

/* ---------------- 상태 & 렌더링 ---------------- */
let session = null; let indicators = null; let cutoff = 0; let selectedIdx = 0; let maxCutoffReached = 0;
let lastPriceLayout = null; let bought = false; let direction = null; let entryIdx = -1;
let roundNo = 0; let dataMode = 'synthetic'; let whatIfMarkerIdx = null;
let viewAllMode = false; let viewAllPrevCutoff = 0;

const priceCanvas = document.getElementById('priceCanvas'); const stochCanvas = document.getElementById('stochCanvas');
const volumeCanvas = document.getElementById('volumeCanvas');
const tape = document.getElementById('tape'); const srcBadge = document.getElementById('srcBadge');
const closeWarn = document.getElementById('closeWarn'); const resultPanel = document.getElementById('resultPanel');
const resultBody = document.getElementById('resultBody');

function computeIndicators(candles){
  const closes = candles.map(c=>c.close);
  return { ema5: ema(closes,5), ema9: ema(closes,9), ema20: ema(closes,20), ema200: ema(closes,200), vwap: vwapSeries(candles), stoch: stochRsiSeries(closes), atr: atrSeries(candles,14) };
}

function startRound(newSession){
  session = newSession; indicators = computeIndicators(session.candles); roundNo++;
  bought=false; direction=null; entryIdx=-1; resultPanel.style.display='none';
  whatIfMarkerIdx = null; document.getElementById('whatIfBox').style.display='none';
  viewAllMode = false; document.getElementById('btnViewAll').textContent = '🔭 모든 차트 보기 (09:00~장마감)';
  document.getElementById('rationale').value=''; document.getElementById('rationale').disabled=false;
  
  const N = session.candles.length;
  if(dataMode === 'real_live') {
    cutoff = N - 1;
  } else {
    const minCut = Math.min(220, Math.floor(N*0.55)); const maxCut = N-35;
    cutoff = minCut + Math.floor(Math.random()*Math.max(1,(maxCut-minCut)));
  }
  selectedIdx = cutoff;
  maxCutoffReached = cutoff;
  
  const isLive = (dataMode === 'real_live');
  document.getElementById('btnLong').disabled = isLive;
  document.getElementById('btnShort').disabled = isLive;
  document.getElementById('btnPass').disabled = isLive;
  document.getElementById('btnStep').disabled = isLive;
  document.getElementById('btnRewindReset').disabled = isLive;
  document.getElementById('btnViewAll').disabled = isLive;
  if(isLive) document.getElementById('rationale').disabled = true;

  render();
}

function fmtTime(i){
  const cd = session && session.candles && session.candles[i];
  if(cd && cd.epochMs != null){
    return new Intl.DateTimeFormat('ko-KR', {timeZone:'Asia/Seoul', hour:'2-digit', minute:'2-digit', hour12:false}).format(new Date(cd.epochMs));
  }
  const m = 9*60+i; return `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`;
}
function fmtDateTime(i){
  const cd = session && session.candles && session.candles[i];
  if(cd && cd.epochMs != null){
    const dt = new Date(cd.epochMs);
    const datePart = new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Seoul', year:'numeric', month:'2-digit', day:'2-digit'}).format(dt);
    const timePart = new Intl.DateTimeFormat('ko-KR', {timeZone:'Asia/Seoul', hour:'2-digit', minute:'2-digit', second:'2-digit', hour12:false}).format(dt);
    return `${datePart} ${timePart} KST(GMT+9)`;
  }
  return fmtTime(i);
}

function render(){
  const c = session.candles;
  const revealEnd = direction ? Math.min(entryIdx+30, c.length-1) : cutoff;
  const visible = c.slice(0, revealEnd+1); const last = visible[visible.length-1];

  if(dataMode === 'real_live') {
    srcBadge.className = 'badge live'; srcBadge.textContent = '✅ 실시간 모니터링' + ` · ${session.label}`;
  } else if(dataMode === 'real') {
    srcBadge.className = 'badge real'; srcBadge.textContent = '✅ 실데이터 복기' + ` · ${session.label}`;
  } else {
    srcBadge.className = 'badge live'; srcBadge.textContent = '● 합성(연습용) 데이터' + ` · ${session.label}`;
  }

  const chg = visible.length > 1 ? ((last.close - visible[0].open)/visible[0].open*100) : 0;
  tape.innerHTML = `<span><span class="dot" style="background:var(--amber)"></span>라운드 ${roundNo}</span>
    <span>${dataMode==='real_live' ? '실시간 연동 중' : fmtTime(revealEnd)}</span>
    <span>현재가 <b>${Math.round(last.close).toLocaleString()}</b></span>
    <span class="${chg>=0?'up':'down'}">${chg>=0?'+':''}${chg.toFixed(2)}%</span>
    <span>VWAP <b>${Math.round(indicators.vwap[revealEnd]).toLocaleString()}</b></span>`;

  const remaining = c.length - cutoff;
  closeWarn.style.display = remaining < 25 && dataMode !== 'real_live' && !direction && !viewAllMode ? 'inline' : 'none';

  drawPriceChart(visible, revealEnd);
  drawVolumeChart(visible, revealEnd);
  drawStochChart(revealEnd);

  const selInfo = document.getElementById('selInfo');
  if(dataMode === 'real_live'){
    selInfo.innerHTML = `실시간 연동 중에는 타점 연습이 제한되며, <b>차트 모니터링</b>만 가능합니다.`;
  } else if(viewAllMode){
    selInfo.innerHTML = `<b style="color:var(--text)">전체 보기 모드</b> — 09:00부터 장마감까지 당일 차트 전체를 보고 있어요. 타점 연습에는 정답을 미리 보는 셈이라 진입 버튼은 잠겨있습니다. "연습 모드로 돌아가기"를 누르면 원래 보던 시점으로 복귀해요.`;
  } else if(!direction){
    const rewound = cutoff < maxCutoffReached;
    selInfo.innerHTML = `보고 있는 시점: <b style="color:var(--text)">${fmtTime(cutoff)}</b>`
      + (rewound ? ` — <span style="color:var(--amber)">최대 진행 시점(${fmtTime(maxCutoffReached)})에서 되감은 상태</span>. "되감기 해제"를 누르면 원래 시점으로 돌아갑니다.` : ' (가장 최근 시점)')
      + ` 차트를 클릭하면 이미 지나온 구간(${fmtTime(0)}~${fmtTime(maxCutoffReached)}) 내에서 <u>그 시점으로 되감깁니다</u> — 그 이후 캔들은 화면에서 사라져서, 미래를 미리 보고 고르는 게 아니에요.`;
  } else {
    selInfo.innerHTML = `정답 공개됨 — 위 차트에 실제 이후 흐름이 펼쳐져 있어요. <u>아무 지점이나 클릭</u>하면 그 시점에서 <b>롱을 골랐을 때/숏을 골랐을 때 점수</b>를 각각 보여드려요.`;
  }
}

function drawPriceChart(visible, upto){
  const ctx = priceCanvas.getContext('2d'); const W = priceCanvas.clientWidth || 1000;
  const H = 360; const axisH = 24; const Htotal = H + axisH; // axisH: 하단 시간축(시각/날짜) 표시용 여백
  priceCanvas.width = W*2; priceCanvas.height=Htotal*2; ctx.scale(2,2); ctx.clearRect(0,0,W,Htotal);
  const n = visible.length; const padL=54, padR=10, padT=10, padB=10; const plotW = W-padL-padR, plotH = H-padT-padB;
  lastPriceLayout = {padL, plotW, n};
  let lo=Infinity, hi=-Infinity; for(const cd of visible){ lo=Math.min(lo,cd.low); hi=Math.max(hi,cd.high); }
  for(const arr of [indicators.ema20, indicators.ema200, indicators.vwap]){
    for(let i=0;i<=upto;i++){ if(arr[i]!=null){ lo=Math.min(lo,arr[i]); hi=Math.max(hi,arr[i]); } }
  }
  const pad = (hi-lo)*0.08 || 10; lo-=pad; hi+=pad;
  const xAt = i => padL + (i/(Math.max(n-1,1)))*plotW; const yAt = p => padT + (1-(p-lo)/(hi-lo))*plotH;

  ctx.strokeStyle='#1C2530'; ctx.fillStyle='#5B6673'; ctx.font='10px monospace';
  for(let g=0; g<=4; g++){ const yy = padT + g*plotH/4; ctx.beginPath(); ctx.moveTo(padL,yy); ctx.lineTo(W-padR,yy); ctx.stroke(); ctx.fillText(Math.round(hi - g*(hi-lo)/4).toLocaleString(), 4, yy+3); }

  const cw = Math.max(1.5, plotW/n*0.6);
  for(let i=0;i<n;i++){
    const cd = visible[i]; const x = xAt(i); const up = cd.close>=cd.open;
    ctx.strokeStyle = up?'#E5484D':'#3B82F6'; ctx.fillStyle = up?'#E5484D':'#3B82F6';
    ctx.beginPath(); ctx.moveTo(x,yAt(cd.high)); ctx.lineTo(x,yAt(cd.low)); ctx.stroke();
    const yo=yAt(cd.open), yc=yAt(cd.close); ctx.fillRect(x-cw/2, Math.min(yo,yc), cw, Math.max(1,Math.abs(yo-yc)));
  }

  function drawLine(arr, color, width){
    ctx.strokeStyle=color; ctx.lineWidth=width; ctx.beginPath(); let started=false;
    for(let i=0;i<n;i++){ if(arr[i]==null) continue; const x=xAt(i), y=yAt(arr[i]); if(!started){ctx.moveTo(x,y); started=true;} else ctx.lineTo(x,y); } ctx.stroke();
  }
  drawLine(indicators.vwap.slice(0,n), '#F5A623', 1.6); drawLine(indicators.ema5.slice(0,n), '#35C9C1', 1.1); drawLine(indicators.ema9.slice(0,n), '#E0629E', 1.1); drawLine(indicators.ema20.slice(0,n), '#8A93A0', 1.3); drawLine(indicators.ema200.slice(0,n), '#5B6BFF', 1.8);

  if(!direction && selectedIdx!=null && selectedIdx>=0 && selectedIdx<n && dataMode!=='real_live'){
    const x = xAt(selectedIdx); ctx.save(); ctx.strokeStyle = '#F5A623'; ctx.setLineDash([3,3]); ctx.lineWidth=1;
    ctx.beginPath(); ctx.moveTo(x,padT); ctx.lineTo(x,H-padB); ctx.stroke(); ctx.restore();
  }

  if(direction && direction!=='pass' && entryIdx>=0 && entryIdx<n){
    const x=xAt(entryIdx), y=yAt(visible[entryIdx].close); const isLong = direction==='long';
    ctx.fillStyle = isLong ? '#3ECF8E' : '#E5484D';
    if(isLong){ ctx.beginPath(); ctx.moveTo(x,y+14); ctx.lineTo(x-6,y+24); ctx.lineTo(x+6,y+24); ctx.fill(); } 
    else { ctx.beginPath(); ctx.moveTo(x,y-14); ctx.lineTo(x-6,y-24); ctx.lineTo(x+6,y-24); ctx.fill(); }
    ctx.font='10px sans-serif'; ctx.fillText(isLong?'롱':'숏', x-8, isLong? y+35 : y-28);
  }

  if(direction && whatIfMarkerIdx!=null && whatIfMarkerIdx>=0 && whatIfMarkerIdx<n){
    const x = xAt(whatIfMarkerIdx);
    ctx.save(); ctx.strokeStyle = '#DCE4EC'; ctx.setLineDash([4,3]); ctx.lineWidth=1.4;
    ctx.beginPath(); ctx.moveTo(x,padT); ctx.lineTo(x,H-padB); ctx.stroke(); ctx.restore();
    ctx.fillStyle='#DCE4EC'; ctx.font='10px sans-serif'; ctx.fillText('복기', x-9, padT+11);
  }

  /* ---------------- 하단 시간축 (KST 시:분 + 날짜/월 바뀔 때 강조) ---------------- */
  const axisTop = H;
  ctx.strokeStyle = '#1C2530'; ctx.beginPath(); ctx.moveTo(0, axisTop+0.5); ctx.lineTo(W, axisTop+0.5); ctx.stroke();

  const timeFmt = new Intl.DateTimeFormat('ko-KR', {timeZone:'Asia/Seoul', hour:'2-digit', minute:'2-digit', hour12:false});
  const dateKeyFmt = new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Seoul', year:'numeric', month:'2-digit', day:'2-digit'});
  const monthFmt = new Intl.DateTimeFormat('ko-KR', {timeZone:'Asia/Seoul', month:'long'});

  const targetTicks = Math.max(2, Math.floor(plotW/90)); // 약 90px당 눈금 하나
  const tickStep = Math.max(1, Math.round((n-1)/targetTicks)) || 1;
  ctx.textAlign = 'center';
  let lastDateKey = null;
  for(let i=0; i<n; i+=tickStep){
    const cd = visible[i]; if(!cd || cd.epochMs==null) continue;
    const dt = new Date(cd.epochMs);
    const dateKey = dateKeyFmt.format(dt);
    const x = xAt(i);
    ctx.strokeStyle = '#232C38'; ctx.beginPath(); ctx.moveTo(x, axisTop); ctx.lineTo(x, axisTop+4); ctx.stroke();
    if(dateKey !== lastDateKey && lastDateKey !== null){
      // 날짜(일)가 바뀌는 지점: 시각 대신 "월" 라벨을 굵게 강조 (TradingView 스타일)
      ctx.fillStyle = '#DCE4EC'; ctx.font = 'bold 10px monospace';
      ctx.fillText(monthFmt.format(dt), x, axisTop+16);
    } else {
      ctx.fillStyle = '#6E7B8B'; ctx.font = '10px monospace';
      ctx.fillText(timeFmt.format(dt), x, axisTop+16);
    }
    lastDateKey = dateKey;
  }
  // 맨 오른쪽(최신 캔들) 시각은 항상 별도로 강조 표시 — 눈금 간격상 잘리기 쉬운 위치라 겹치지 않을 때만 그림
  if(n>0){
    const lastCd = visible[n-1];
    if(lastCd && lastCd.epochMs!=null){
      const lastTickIdx = Math.floor((n-1)/tickStep)*tickStep;
      const lastTickX = xAt(lastTickIdx);
      if((W-padR) - lastTickX > 40){
        ctx.fillStyle = '#F5A623'; ctx.font = 'bold 10px monospace'; ctx.textAlign = 'right';
        ctx.fillText(timeFmt.format(new Date(lastCd.epochMs)), W-padR, axisTop+16);
      }
    }
  }
  ctx.textAlign = 'left';
}

function drawStochChart(upto){
  const ctx = stochCanvas.getContext('2d'); const W = stochCanvas.clientWidth || 1000; const H = 120;
  stochCanvas.width=W*2; stochCanvas.height=H*2; ctx.scale(2,2); ctx.clearRect(0,0,W,H);
  const padL=54, padR=10, padT=8, padB=8; const plotW=W-padL-padR, plotH=H-padT-padB; const n = upto+1;
  const xAt = i => padL + (i/(Math.max(n-1,1)))*plotW; const yAt = v => padT + (1-v/100)*plotH;

  ctx.strokeStyle='#1C2530'; [20,50,80].forEach(v=>{ ctx.beginPath(); ctx.moveTo(padL,yAt(v)); ctx.lineTo(W-padR,yAt(v)); ctx.stroke(); });
  ctx.fillStyle='#5B6673'; ctx.font='10px monospace'; ctx.fillText('80',4,yAt(80)+3); ctx.fillText('20',4,yAt(20)+3);

  function drawLine(arr,color){
    ctx.strokeStyle=color; ctx.lineWidth=1.4; ctx.beginPath(); let started=false;
    for(let i=0;i<n;i++){ if(arr[i]==null) continue; const x=xAt(i), y=yAt(arr[i]); if(!started){ctx.moveTo(x,y); started=true;} else ctx.lineTo(x,y); } ctx.stroke();
  }
  drawLine(indicators.stoch.K.slice(0,n), '#35C9C1'); drawLine(indicators.stoch.D.slice(0,n), '#E0629E');
}

function drawVolumeChart(visible, upto){
  const ctx = volumeCanvas.getContext('2d'); const W = volumeCanvas.clientWidth || 1000; const H = 70;
  volumeCanvas.width = W*2; volumeCanvas.height = H*2; ctx.scale(2,2); ctx.clearRect(0,0,W,H);
  const n = visible.length; const padL=54, padR=10, padT=4, padB=4; const plotW = W-padL-padR, plotH = H-padT-padB;
  const xAt = i => padL + (i/(Math.max(n-1,1)))*plotW;

  // 극단적으로 큰 거래량 캔들 하나 때문에 나머지가 다 눌려 보이지 않도록,
  // 절대 최댓값이 아니라 95퍼센타일 값을 축의 상한으로 쓴다. 그 이상은 "잘림" 표시로 그린다.
  const vols = visible.map(cd => cd.v).slice().sort((a,b)=>a-b);
  const p95Idx = Math.min(vols.length-1, Math.floor(vols.length*0.95));
  let capV = vols[p95Idx] || vols[vols.length-1] || 1;
  if(capV <= 0) capV = Math.max(...vols, 1);
  const yAt = v => padT + (1 - Math.min(v,capV)/capV) * plotH;

  ctx.fillStyle='#5B6673'; ctx.font='10px monospace';
  ctx.fillText(Math.round(capV).toLocaleString()+' (95%)', 4, padT+9);
  ctx.fillText('0', 4, H-padB);

  const bw = Math.max(1.5, plotW/n*0.6);
  for(let i=0;i<n;i++){
    const cd = visible[i]; const x = xAt(i);
    const up = cd.close >= cd.open;
    const clipped = cd.v > capV;
    ctx.fillStyle = up ? (clipped?'rgba(229,72,77,0.95)':'rgba(229,72,77,0.65)') : (clipped?'rgba(59,130,246,0.95)':'rgba(59,130,246,0.65)');
    const y = yAt(cd.v);
    ctx.fillRect(x-bw/2, y, bw, (H-padB)-y);
    if(clipped){ // 상한을 넘는 캔들은 꼭대기에 작은 삼각형 표시로 "더 큼"을 알려준다 (정확한 값은 호버 툴팁 참고)
      ctx.beginPath(); ctx.moveTo(x, padT); ctx.lineTo(x-3, padT+5); ctx.lineTo(x+3, padT+5); ctx.closePath(); ctx.fill();
    }
  }
}

/* ---------------- 상호작용 및 결과 처리 ---------------- */
document.getElementById('btnStep').addEventListener('click', ()=>{
  if(direction || dataMode==='real_live') return;
  if(cutoff < session.candles.length-2){ cutoff++; selectedIdx = cutoff; maxCutoffReached = Math.max(maxCutoffReached, cutoff); render(); }
});

function takePosition(dir){
  if(direction || dataMode==='real_live') return; direction = dir; bought = (dir!=='pass'); entryIdx = selectedIdx;
  document.getElementById('btnStep').disabled=true; document.getElementById('btnRewindReset').disabled=true;
  document.getElementById('btnLong').disabled=true;
  document.getElementById('btnShort').disabled=true; document.getElementById('btnPass').disabled=true;
  render(); showResult();
}
document.getElementById('btnLong').addEventListener('click', ()=>takePosition('long'));
document.getElementById('btnShort').addEventListener('click', ()=>takePosition('short'));
document.getElementById('btnPass').addEventListener('click', ()=>takePosition('pass'));

priceCanvas.addEventListener('click', (e)=>{
  if(!lastPriceLayout || !session || dataMode==='real_live' || viewAllMode) return;
  const rect = priceCanvas.getBoundingClientRect(); const xCss = e.clientX - rect.left;
  const {padL, plotW, n} = lastPriceLayout; const idxFloat = (xCss-padL)/plotW*(n-1);
  let idx = Math.round(idxFloat);

  if(!direction){
    idx = Math.max(0, Math.min(maxCutoffReached, idx));
    cutoff = idx; selectedIdx = idx; render(); 
  } else {
    idx = Math.max(0, Math.min(n-1, idx));
    showWhatIf(idx);
  }
});

/* ---------------- 마우스/터치 오버 시 캔들 날짜·시각(KST) 툴팁 ---------------- */
const crosshairLine = document.getElementById('crosshairLine');
const chartTooltip = document.getElementById('chartTooltip');
function handleChartHover(clientX){
  if(!lastPriceLayout || !session){ crosshairLine.style.display='none'; chartTooltip.style.display='none'; return; }
  const rect = priceCanvas.getBoundingClientRect();
  const xCss = clientX - rect.left;
  const {padL, plotW, n} = lastPriceLayout;
  if(xCss < padL - 2 || xCss > padL + plotW + 2){ crosshairLine.style.display='none'; chartTooltip.style.display='none'; return; }
  const idxFloat = (xCss - padL) / plotW * (n - 1);
  const idx = Math.max(0, Math.min(n - 1, Math.round(idxFloat)));
  const cd = session.candles[idx];
  if(!cd){ crosshairLine.style.display='none'; chartTooltip.style.display='none'; return; }

  crosshairLine.style.display = 'block';
  crosshairLine.style.left = Math.max(padL, Math.min(padL + plotW, xCss)) + 'px';

  chartTooltip.style.display = 'block';
  chartTooltip.innerHTML =
    `<b>${fmtDateTime(idx)}</b><br>` +
    `시 ${Math.round(cd.open).toLocaleString()} · 고 ${Math.round(cd.high).toLocaleString()}<br>` +
    `저 ${Math.round(cd.low).toLocaleString()} · 종 ${Math.round(cd.close).toLocaleString()}<br>` +
    `거래량 ${Math.round(cd.v).toLocaleString()}`;
  const wrapWidth = rect.width;
  let tipLeft = xCss + 14;
  if(tipLeft + 148 > wrapWidth) tipLeft = xCss - 160;
  chartTooltip.style.left = Math.max(4, tipLeft) + 'px';
  chartTooltip.style.top = '6px';
}
priceCanvas.addEventListener('mousemove', (e)=> handleChartHover(e.clientX));
priceCanvas.addEventListener('mouseleave', ()=>{ crosshairLine.style.display='none'; chartTooltip.style.display='none'; });
let priceTouchStartX = null;
priceCanvas.addEventListener('touchstart', (e)=>{
  if(e.touches && e.touches[0]) priceTouchStartX = e.touches[0].clientX;
}, {passive:true});
priceCanvas.addEventListener('touchmove', (e)=>{
  if(!e.touches || !e.touches[0]) return;
  const dx = Math.abs(e.touches[0].clientX - (priceTouchStartX ?? e.touches[0].clientX));
  handleChartHover(e.touches[0].clientX);
  if(dx > 6) e.preventDefault(); // 일정 이상 드래그(스크럽)할 때만 페이지 스크롤을 막아, 짧은 탭(진입/되감기)은 그대로 동작하게 함
}, {passive:false});
priceCanvas.addEventListener('touchend', ()=>{ crosshairLine.style.display='none'; chartTooltip.style.display='none'; });

document.getElementById('btnRewindReset').addEventListener('click', ()=>{
  if(direction || dataMode==='real_live') return;
  cutoff = maxCutoffReached; selectedIdx = cutoff; render();
});

document.getElementById('btnViewAll').addEventListener('click', ()=>{
  if(direction || dataMode==='real_live') return;
  const btn = document.getElementById('btnViewAll');
  if(!viewAllMode){
    viewAllPrevCutoff = cutoff;
    viewAllMode = true;
    cutoff = session.candles.length-1; selectedIdx = cutoff;
    btn.textContent = '↩ 연습 모드로 돌아가기';
    document.getElementById('btnLong').disabled = true;
    document.getElementById('btnShort').disabled = true;
    document.getElementById('btnPass').disabled = true;
    document.getElementById('btnStep').disabled = true;
    document.getElementById('btnRewindReset').disabled = true;
  } else {
    viewAllMode = false;
    cutoff = viewAllPrevCutoff; selectedIdx = cutoff;
    btn.textContent = '🔭 모든 차트 보기 (09:00~장마감)';
    document.getElementById('btnLong').disabled = false;
    document.getElementById('btnShort').disabled = false;
    document.getElementById('btnPass').disabled = false;
    document.getElementById('btnStep').disabled = false;
    document.getElementById('btnRewindReset').disabled = false;
  }
  render();
});

function computeOutcomeStats(dir, idx){
  const c = session.candles;
  const entry = c[idx];
  const horizon = Math.min(30, c.length-1-idx);
  const future = c.slice(idx, idx+horizon+1);
  const sign = dir==='short' ? -1 : 1;
  let mfe=-Infinity, mae=Infinity;
  for(const f of future){ mfe=Math.max(mfe,f.high); mae=Math.min(mae,f.low); }
  const mfePct = dir==='short' ? -((mae-entry.close)/entry.close*100) : ((mfe-entry.close)/entry.close*100);
  const maePct = dir==='short' ? -((mfe-entry.close)/entry.close*100) : ((mae-entry.close)/entry.close*100);
  function at(minAfter){ const i=Math.min(idx+minAfter, c.length-1); const p=c[i].close; return {p, pct: sign*(p-entry.close)/entry.close*100}; }
  return { m5:at(5), m10:at(10), m20:at(20), m30:at(30), mfePct, maePct };
}

function showWhatIf(idx){
  whatIfMarkerIdx = idx;
  render();

  const longStats = computeOutcomeStats('long', idx);
  const shortStats = computeOutcomeStats('short', idx);
  const longScore = computeScore('long', idx, longStats.m30, longStats.mfePct, longStats.maePct);
  const shortScore = computeScore('short', idx, shortStats.m30, shortStats.mfePct, shortStats.maePct);

  const box = document.getElementById('whatIfBox');
  box.style.display='block';
  function sideHtml(label, color, score, stats){
    return `
      <div style="flex:1; min-width:160px;">
        <div style="color:${color}; font-weight:700; font-size:15px;">${label} → ${score.total}점 <span style="font-size:11px; font-weight:600;">(${score.grade.label})</span></div>
        <div style="font-size:11px; color:var(--muted); margin:2px 0 6px;">30분 손익 ${stats.m30.pct>=0?'+':''}${stats.m30.pct.toFixed(2)}% · MAE ${stats.maePct.toFixed(2)}%</div>
        <div style="font-size:10.5px; color:var(--muted); line-height:1.6;">${score.breakdown.map(b=>`${b.label} ${b.score}/${b.max}`).join(' · ')}</div>
      </div>`;
  }
  box.innerHTML = `
    <div style="font-weight:700; margin-bottom:10px;">복기 — <code>${fmtTime(idx)}</code> 시점에서 골랐다면</div>
    <div style="display:flex; gap:16px; flex-wrap:wrap;">
      ${sideHtml('롱', 'var(--good)', longScore, longStats)}
      ${sideHtml('숏', 'var(--red)', shortScore, shortStats)}
    </div>
    <div class="fmt" style="margin-top:8px;">내 실제 진입(${fmtTime(entryIdx)}, ${direction==='long'?'롱':direction==='short'?'숏':'관망'})과 비교해서, 다른 시점이었다면 어땠을지 확인해보세요. 차트를 다시 클릭하면 다른 지점으로 바꿀 수 있어요.</div>
  `;
}

let lastSummary = '';
function findBestEntry(dir, candles, refIdx, lookback=60, horizon=30){
  const start = Math.max(0, refIdx-lookback); let best = {idx:refIdx, pct:-Infinity};
  for(let i=start;i<=refIdx;i++){
    const entryClose = candles[i].close; const endIdx = Math.min(i+horizon, candles.length-1);
    let extreme = dir==='long' ? -Infinity : Infinity;
    for(let j=i;j<=endIdx;j++){ if(dir==='long') extreme = Math.max(extreme, candles[j].high); else extreme = Math.min(extreme, candles[j].low); }
    const pct = dir==='long' ? (extreme-entryClose)/entryClose*100 : (entryClose-extreme)/entryClose*100;
    if(pct>best.pct) best = {idx:i, pct};
  }
  return best;
}

function analyzeContext(entryIdx){
  const c = session.candles;
  const closes = c.map(x=>x.close);
  const vwap = indicators.vwap, ema5=indicators.ema5, ema9=indicators.ema9;
  const stochK = indicators.stoch.K;

  let above=0, tot=0;
  for(let i=Math.max(0,entryIdx-9); i<=entryIdx; i++){ if(vwap[i]!=null){ tot++; if(closes[i]>=vwap[i]) above++; } }
  const vwapPersist = tot? above/tot : null;

  let crossAgo=null, crossType=null;
  for(let k=0;k<15;k++){
    const i = entryIdx-k;
    if(i-1<0 || ema5[i]==null || ema9[i]==null || ema5[i-1]==null || ema9[i-1]==null) continue;
    const prevDiff = ema5[i-1]-ema9[i-1], curDiff = ema5[i]-ema9[i];
    if(prevDiff!==0 && curDiff!==0 && Math.sign(prevDiff)!==Math.sign(curDiff)){
      crossAgo = k; crossType = curDiff>0 ? 'golden' : 'dead'; break;
    }
  }

  let stochDelta=null;
  if(stochK[entryIdx]!=null && entryIdx-4>=0 && stochK[entryIdx-4]!=null) stochDelta = stochK[entryIdx]-stochK[entryIdx-4];

  const stochD = indicators.stoch.D;
  let stochCrossAgo=null, stochCrossType=null;
  for(let k=0;k<8;k++){
    const i = entryIdx-k;
    if(i-1<0 || stochK[i]==null || stochD[i]==null || stochK[i-1]==null || stochD[i-1]==null) continue;
    const prevDiff = stochK[i-1]-stochD[i-1], curDiff = stochK[i]-stochD[i];
    if(prevDiff!==0 && curDiff!==0 && Math.sign(prevDiff)!==Math.sign(curDiff)){
      stochCrossAgo = k; stochCrossType = curDiff>0 ? 'golden' : 'dead'; break;
    }
  }

  let volRatio=null; let sumV=0,cnt=0;
  for(let i=Math.max(0,entryIdx-20); i<entryIdx; i++){ sumV+=c[i].v; cnt++; }
  if(cnt>0) volRatio = c[entryIdx].v/(sumV/cnt);

  return {vwapPersist, crossAgo, crossType, stochDelta, stochCrossAgo, stochCrossType, volRatio};
}

function buildNarrative(direction, entryIdx, m30, mfePct, maePct){
  const c = session.candles;
  const price = c[entryIdx].close;
  const vwapNow = indicators.vwap[entryIdx];
  const e5=indicators.ema5[entryIdx], e9=indicators.ema9[entryIdx], e20=indicators.ema20[entryIdx], e200=indicators.ema200[entryIdx];
  const kK=indicators.stoch.K[entryIdx];
  const ctx = analyzeContext(entryIdx);
  const vsVwapPct = vwapNow ? (price-vwapNow)/vwapNow*100 : null;

  let align='혼조';
  if(e5!=null&&e9!=null&&e20!=null&&e200!=null){
    if(e5>e9&&e9>e20&&e20>e200) align='정배열';
    else if(e5<e9&&e9<e20&&e20<e200) align='역배열';
  }

  const lines = [];

  if(vsVwapPct==null) lines.push('VWAP 계산에 필요한 데이터가 아직 충분하지 않은 초반 구간이었습니다.');
  else if(Math.abs(vsVwapPct)<0.15) lines.push(`가격이 <u>VWAP 바로 근처(${vsVwapPct>=0?'+':''}${vsVwapPct.toFixed(2)}%)</u>에 붙어있어서, 이 라인이 지지로 작동할지 저항으로 작동할지 애매하게 걸쳐있는 자리였습니다.`);
  else if(vsVwapPct>0) lines.push(`가격은 <u>VWAP보다 ${vsVwapPct.toFixed(2)}% 위</u>에서 거래되고 있었고, 직전 10개 캔들 중 <u>${Math.round((ctx.vwapPersist||0)*100)}%</u>가 VWAP 위쪽에서 마감돼 이 자리를 지지선처럼 쓰고 있었습니다.`);
  else lines.push(`가격은 <u>VWAP보다 ${Math.abs(vsVwapPct).toFixed(2)}% 아래</u>로 눌려있었고, 직전 10개 캔들 중 <u>${Math.round((1-(ctx.vwapPersist||0))*100)}%</u>가 VWAP 아래쪽에서 마감돼 VWAP이 저항으로 작용하는 흐름이었습니다.`);

  let emaDesc;
  if(align==='정배열') emaDesc = `EMA는 <u>5&gt;9&gt;20&gt;200 정배열</u> 상태로, 단기·중기 추세가 모두 상승 쪽으로 정렬돼 있었습니다.`;
  else if(align==='역배열') emaDesc = `EMA는 <u>5&lt;9&lt;20&lt;200 역배열</u> 상태로, 단기·중기 추세가 모두 하락 쪽으로 정렬돼 있었습니다.`;
  else emaDesc = `EMA 배열은 <u>정배열도 역배열도 아닌 혼조</u> 상태라, 추세 자체의 방향성이 뚜렷하지 않았습니다.`;
  if(ctx.crossAgo!=null){
    emaDesc += ` 게다가 <u>${ctx.crossAgo===0?'바로 이 캔들에서':ctx.crossAgo+'분 전'} EMA5/9 ${ctx.crossType==='golden'?'골든크로스':'데드크로스'}</u>가 나와서, ${ctx.crossType==='golden'?'단기 상승 전환':'단기 하락 전환'} 신호가 막 확인된 시점이었습니다.`;
  }
  lines.push(emaDesc);

  let stochDesc;
  if(kK==null) stochDesc = 'Stochastic RSI 계산에 필요한 데이터가 아직 부족한 구간입니다.';
  else if(kK<20) stochDesc = `Stochastic RSI는 <u>과매도권(${kK.toFixed(1)})</u>에 있었습니다 — 이 구간은 대개 매도세가 이미 많이 소진된 자리라, 신규 숏을 여는 것보다는 반등 여부를 지켜보거나 롱 전환을 고려하는 쪽이 정석입니다.`;
  else if(kK>80) stochDesc = `Stochastic RSI는 <u>과매수권(${kK.toFixed(1)})</u>에 있었습니다 — 이 구간은 대개 매수세가 이미 많이 소진된 자리라, 신규 롱을 여는 것보다는 되돌림 여부를 지켜보거나 숏 전환을 고려하는 쪽이 정석입니다.`;
  else stochDesc = `Stochastic RSI는 <u>중립 구간(${kK.toFixed(1)})</u>에 있어서, 이 지표만으로는 뚜렷한 방향 근거가 되지 못하는 자리였습니다.`;
  if(ctx.stochDelta!=null) stochDesc += ` (직전 4분 대비 <u>${ctx.stochDelta>=0?'+':''}${ctx.stochDelta.toFixed(1)}p</u>로 ${ctx.stochDelta>=0?'상승':'하락'} 중)`;
  if(ctx.stochCrossAgo!=null) stochDesc += ` 또한 <u>${ctx.stochCrossAgo===0?'바로 이 캔들에서':ctx.stochCrossAgo+'분 전'} %K/%D ${ctx.stochCrossType==='golden'?'골든크로스':'데드크로스'}</u>가 나온 상태였습니다.`;
  lines.push(stochDesc);

  if(ctx.volRatio!=null){
    if(ctx.volRatio>1.5) lines.push(`진입 캔들의 거래량은 직전 20분 평균 대비 <u>${ctx.volRatio.toFixed(1)}배</u>로 크게 실렸습니다 — 방향에 실제 힘(매수/매도세)이 붙었을 가능성이 있는 자리였다는 뜻입니다.`);
    else if(ctx.volRatio<0.6) lines.push(`반면 거래량은 평균 대비 <u>${ctx.volRatio.toFixed(1)}배</u>로 오히려 줄어들어 있었습니다 — 힘이 실리지 않은 채 가격만 움직이는, 다소 신뢰도가 낮은 구간일 수 있습니다.`);
    else lines.push(`거래량은 평균과 비슷한 수준(<u>${ctx.volRatio.toFixed(1)}배</u>)이라, 특별히 힘이 실리거나 빠진 신호는 없었습니다.`);
  }

  let longSteps=0, shortSteps=0;
  if(e5!=null&&e9!=null&&e20!=null&&e200!=null){
    if(e5>e9) longSteps++; if(e9>e20) longSteps++; if(e20>e200) longSteps++;
    if(e5<e9) shortSteps++; if(e9<e20) shortSteps++; if(e20<e200) shortSteps++;
  }
  let suggestion='관망';
  if(longSteps>=2 && (vsVwapPct==null || vsVwapPct>=-0.1) && !(kK>80)) suggestion='롱';
  else if(shortSteps>=2 && (vsVwapPct==null || vsVwapPct<=0.1) && !(kK<20)) suggestion='숏';
  const dirLabel = direction==='long'?'롱':direction==='short'?'숏':'관망';
  lines.push(`위 조건들을 종합하면 교과서적으로는 <u>${suggestion}</u> 쪽에 조금 더 무게가 실리는 자리였습니다. 사용자님은 <u>${dirLabel}</u>을 선택하셨는데, 이는 그 판단과 <u>${suggestion===dirLabel?'같은 방향':'다른 방향'}</u>이었습니다.`);

  const finalPct = m30.pct;
  let resultDesc;
  if(direction==='pass'){
    resultDesc = `실제로는 이후 30분간 <u>${finalPct>=0?'+':''}${finalPct.toFixed(2)}%(롱 기준)</u> 움직였습니다. 관망을 선택했기 때문에 이 움직임은 놓쳤지만, 근거가 애매했던 자리였다면 반대 방향 손실도 피한 셈이라 나쁘지 않은 선택입니다.`;
  } else {
    resultDesc = `실제로는 이후 30분간 <u>${finalPct>=0?'+':''}${finalPct.toFixed(2)}%</u> 움직였고, 중간에 최대 <u>${maePct.toFixed(2)}%</u>까지 반대 방향으로 밀린 구간이 있었습니다. `;
    if(finalPct>=0 && suggestion===dirLabel) resultDesc += `근거(지표 정렬)와 결과(수익)가 <u>같은 방향</u>으로 맞아떨어진, 논리적으로 재현 가능성이 높은 케이스에 가깝습니다.`;
    else if(finalPct>=0 && suggestion!==dirLabel) resultDesc += `다만 지표 근거는 오히려 반대이거나 애매했던 자리라, 이번 수익은 <u>근거보다 결과가 앞선(운이 좋았을 수 있는)</u> 케이스로 보는 게 안전합니다. 같은 조건을 반복하면 다음엔 결과가 다를 수 있어요.`;
    else if(finalPct<0 && suggestion===dirLabel) resultDesc += `근거 자체는 맞는 방향이었는데 <u>단기 노이즈에 흔들린</u> 경우일 수 있습니다 — 진입 타이밍을 조금 더 기다리거나 손절선을 여유 있게 뒀다면 결과가 달랐을 수도 있습니다.`;
    else resultDesc += `근거도 약했고 결과도 안 좋았던 자리이니, <u>이 지표 조합에서 이 방향으로 들어가는 패턴</u>은 다음번엔 피하는 게 좋겠습니다.`;
  }
  lines.push(resultDesc);

  return lines;
}

function computeScore(direction, entryIdx, m30, mfePct, maePct){
  const c = session.candles;
  const price = c[entryIdx].close;
  const vwapNow = indicators.vwap[entryIdx];
  const e5=indicators.ema5[entryIdx], e9=indicators.ema9[entryIdx], e20=indicators.ema20[entryIdx], e200=indicators.ema200[entryIdx];
  const kK=indicators.stoch.K[entryIdx];
  const vsVwapPct = vwapNow ? (price-vwapNow)/vwapNow*100 : null;
  let align='혼조';
  if(e5!=null&&e9!=null&&e20!=null&&e200!=null){
    if(e5>e9&&e9>e20&&e20>e200) align='정배열';
    else if(e5<e9&&e9<e20&&e20<e200) align='역배열';
  }

  if(direction==='pass'){
    const bestL = findBestEntry('long', c, entryIdx);
    const bestS = findBestEntry('short', c, entryIdx);
    const maxBest = Math.max(bestL.pct, bestS.pct);
    const passScore = Math.round(Math.max(30, Math.min(95, 95 - (maxBest-0.3)*35)));
    let note;
    if(maxBest<=0.3) note = `이 구간은 롱 기준 최대 +${bestL.pct.toFixed(2)}%, 숏 기준 최대 +${bestS.pct.toFixed(2)}%로 실제로 큰 기회가 없던 구간이었습니다 — 관망이 합리적인 선택이었습니다.`;
    else if(maxBest<=1.0) note = `이 구간엔 롱 +${bestL.pct.toFixed(2)}% / 숏 +${bestS.pct.toFixed(2)}% 정도의 기회가 있었습니다 — 놓친 게 아주 크진 않지만, 약간의 기회비용은 있었습니다.`;
    else note = `이 구간엔 롱 +${bestL.pct.toFixed(2)}% / 숏 +${bestS.pct.toFixed(2)}%의 꽤 큰 기회가 있었습니다 — 관망 자체가 틀린 건 아니지만, 근거가 있었다면 놓치기엔 아쉬운 구간이었습니다.`;
    return {
      total: passScore, grade: gradeOf(passScore),
      breakdown: [{label:'기회비용 최소화', score:passScore, max:95}],
      note
    };
  }

  const dirLabel = direction==='long'?'롱':'숏';
  const ctx = analyzeContext(entryIdx);

  const best = findBestEntry(direction, c, entryIdx);
  const bestPct = Math.max(best.pct, 0.3);
  const resultRatio = Math.max(0, Math.min(1, m30.pct / bestPct));
  const scoreA = Math.round(resultRatio*30);

  const atrNow = indicators.atr[entryIdx];
  const atrPct = (atrNow!=null && price) ? (atrNow/price*100) : 0.6; 
  const riskBudget = Math.max(atrPct*1.5, 0.4); 
  const riskRatio = Math.max(0, Math.min(1, 1 - Math.abs(maePct)/riskBudget));
  const scoreB = Math.round(riskRatio*25);

  let scoreVwap = 0;
  if(direction==='long'){ if(vsVwapPct==null || vsVwapPct>=-0.1) scoreVwap = 8; }
  else { if(vsVwapPct==null || vsVwapPct<=0.1) scoreVwap = 8; }

  let alignSteps = 0;
  if(e5!=null&&e9!=null&&e20!=null&&e200!=null){
    if(direction==='long'){ if(e5>e9) alignSteps++; if(e9>e20) alignSteps++; if(e20>e200) alignSteps++; }
    else { if(e5<e9) alignSteps++; if(e9<e20) alignSteps++; if(e20<e200) alignSteps++; }
  }
  const scoreEma = Math.round((alignSteps/3)*10);

  let scoreStoch = 0;
  if(kK!=null){
    const badExtreme = direction==='long' ? kK>80 : kK<20;
    if(!badExtreme) scoreStoch += 3;
    const favorableDelta = direction==='long' ? (ctx.stochDelta!=null && ctx.stochDelta>0) : (ctx.stochDelta!=null && ctx.stochDelta<0);
    if(favorableDelta) scoreStoch += 2;
    const favorableCross = direction==='long' ? ctx.stochCrossType==='golden' : ctx.stochCrossType==='dead';
    if(favorableCross) scoreStoch += 2;
  }
  scoreStoch = Math.min(7, scoreStoch);

  const scoreC = scoreVwap + scoreEma + scoreStoch;

  const diffMin = Math.abs(entryIdx-best.idx);
  const timingRatio = Math.max(0, Math.min(1, 1 - diffMin/30));
  const scoreD = Math.round(timingRatio*20);

  const rawTotal = scoreA+scoreB+scoreC+scoreD;
  const total = Math.min(96, rawTotal); 

  return {
    total, grade: gradeOf(total),
    breakdown: [
      {label:'결과 활용도', score:scoreA, max:30},
      {label:'리스크 관리(ATR대비)', score:scoreB, max:25},
      {label:'VWAP 위치', score:scoreVwap, max:8},
      {label:'EMA 단계정렬', score:scoreEma, max:10},
      {label:'StochRSI 모멘텀', score:scoreStoch, max:7},
      {label:'타이밍 정밀도', score:scoreD, max:20}
    ],
    note: diffMin===0
      ? `참고로 이 순간이 이 구간의 이론상 최적 ${dirLabel} 타점과 정확히 일치했습니다. 다만 그 타점도 사후에 확정된 정보라, 실전에서 그 확신을 갖고 들어가긴 어려웠을 거라 100점을 드리진 않습니다.`
      : `이론상 최적 ${dirLabel} 타점(${fmtTime(best.idx)})과는 ${diffMin}분 차이가 있었습니다. (리스크 점수는 이 구간 ATR ${atrPct.toFixed(2)}% 기준으로 정규화했습니다)`
  };
}
function gradeOf(score){
  if(score>=85) return {label:'S', desc:'거의 교과서적인 판단'};
  if(score>=70) return {label:'A', desc:'좋은 판단'};
  if(score>=55) return {label:'B', desc:'평범, 개선 여지 있음'};
  if(score>=40) return {label:'C', desc:'근거 또는 리스크관리 부족'};
  return {label:'D', desc:'근거가 약한 진입'};
}

// breakdown 순서(롱/숏 전용): [0]결과활용도 [1]리스크관리 [2]VWAP위치 [3]EMA단계정렬 [4]StochRSI모멘텀 [5]타이밍정밀도
// [2]+[3]+[4] = 근거정합성(25점 만점)에 대응
function classifyVerdict(score){
  if(!score.breakdown || score.breakdown.length < 6) return null; // 관망(pass) 라운드는 판정 제외
  const scoreRisk = score.breakdown[1].score;
  const scoreLogic = score.breakdown[2].score + score.breakdown[3].score + score.breakdown[4].score;
  const total = score.total;

  if(total>=70 && scoreLogic>=17){
    return {label:'✅ 반복할 가치 있는 패턴', color:'var(--good)',
      desc:'결과와 근거가 모두 탄탄했습니다. 같은 조건(비슷한 VWAP/EMA/StochRSI 조합)이 다시 나오면 반복해도 좋은 진입입니다.'};
  }
  if(total>=55 && total<70 && scoreLogic>=17 && scoreRisk>=15){
    return {label:'🔁 로직은 유효, 이번엔 운이 안 따랐을 뿐', color:'var(--good)',
      desc:'근거 정합성과 리스크 관리는 충분히 좋았습니다. 결과가 크게 따라주지 않았을 뿐, 판단 자체는 유지해도 되는 패턴입니다.'};
  }
  if(total<=40){
    return {label:'🚫 반복 금지 — 근거 자체가 약함', color:'var(--red)',
      desc:'총점과 무관하게, 이 지표 조합에서 이 방향으로 들어가는 패턴 자체를 다음엔 피하는 게 좋습니다.'};
  }
  if(scoreLogic<=8 && total>=55){
    return {label:'🎲 결과는 좋았지만 근거는 약함 (운에 가까움)', color:'var(--amber)',
      desc:'총점만 보고 이 방식을 반복하면 위험합니다 — 지표 근거가 낮았다는 걸 꼭 기억하세요.'};
  }
  return {label:'🤔 애매함 — 좀 더 지켜봐야 할 패턴', color:'var(--muted)',
    desc:'근거와 결과가 뚜렷하게 일치하지도, 완전히 어긋나지도 않았습니다. 비슷한 셋업이 또 나오면 유심히 비교해보세요.'};
}

function showResult(){
  const c = session.candles; const entry = c[entryIdx]; const horizon = Math.min(30, c.length-1-entryIdx);
  const future = c.slice(entryIdx, entryIdx+horizon+1); const sign = direction==='short' ? -1 : 1;
  let mfe=-Infinity, mae=Infinity;
  for(const f of future){ mfe=Math.max(mfe,f.high); mae=Math.min(mae,f.low); }
  const mfePct = direction==='short' ? -((mae-entry.close)/entry.close*100) : ((mfe-entry.close)/entry.close*100);
  const maePct = direction==='short' ? -((mfe-entry.close)/entry.close*100) : ((mae-entry.close)/entry.close*100);

  function at(minAfter){ const idx = Math.min(entryIdx+minAfter, c.length-1); const p = c[idx].close; return {p, pct: sign*(p-entry.close)/entry.close*100}; }
  const m5=at(5), m10=at(10), m20=at(20), m30=at(30);
  const vwapNow = indicators.vwap[entryIdx]; const e5=indicators.ema5[entryIdx], e9=indicators.ema9[entryIdx], e20=indicators.ema20[entryIdx], e200=indicators.ema200[entryIdx];
  const kK=indicators.stoch.K[entryIdx], kD=indicators.stoch.D[entryIdx];
  let align = '혼조'; if(e5!=null && e9!=null && e20!=null && e200!=null){ if(e5>e9 && e9>e20 && e20>e200) align='정배열'; else if(e5<e9 && e9<e20 && e20<e200) align='역배열'; }
  let stochState = '데이터 부족'; if(kK!=null) stochState = kK<20 ? `과매도권 (${kK.toFixed(1)})` : kK>80 ? `과매수권 (${kK.toFixed(1)})` : `중립 (${kK.toFixed(1)})`;
  const vsVwap = vwapNow ? ((entry.close-vwapNow)/vwapNow*100) : null;
  const dirLabel = direction==='long' ? '롱(매수)' : direction==='short' ? '숏(공매도)' : '관망(패스)';
  const rationale = document.getElementById('rationale').value.trim();

  let bestHtml = '', bestText = '';
  if(direction==='long' || direction==='short'){
    const best = findBestEntry(direction, c, entryIdx); const diff = entryIdx - best.idx;
    bestHtml = `<b>이 구간에서 가장 좋았던 ${dirLabel} 타점</b><br><code>${fmtTime(best.idx)}</code> (내 진입보다 ${diff===0?'동일 시점':diff+'분 '+(diff>0?'전':'후')}) — 잠재수익 <b class="up">+${best.pct.toFixed(2)}%</b><br><br>`;
    bestText = `이 구간 최적 ${dirLabel} 타점: ${fmtTime(best.idx)} (내 진입 대비 ${diff===0?'동일':diff+'분 '+(diff>0?'전':'후')}) → 잠재수익 +${best.pct.toFixed(2)}%\n`;
  }

  const narrativeLines = buildNarrative(direction, entryIdx, m30, mfePct, maePct);
  const narrativeHtml = narrativeLines.map(l=>`<p style="margin:0 0 10px;">${l}</p>`).join('');
  const narrativeText = narrativeLines.map(l=>l.replace(/<\/?u>/g,'').replace(/&gt;/g,'>').replace(/&lt;/g,'<')).join('\n');

  resultPanel.style.display='block';
  const score = computeScore(direction, entryIdx, m30, mfePct, maePct);
  const gradeColor = score.grade.label==='S' ? 'var(--good)' : score.grade.label==='A' ? '#8FE3B0' : score.grade.label==='B' ? 'var(--amber)' : 'var(--red)';
  const scoreBarsHtml = score.breakdown.map(b=>{
    const pct = Math.max(0, Math.min(100, (b.score/b.max)*100));
    const barColor = pct>=70?'var(--good)':pct>=40?'var(--amber)':'var(--red)';
    return `<div class="scoreBarRow"><span class="lbl">${b.label}</span><div class="scoreBarTrack"><div class="scoreBarFill" style="width:${pct}%; background:${barColor};"></div></div><span class="num">${b.score}/${b.max}</span></div>`;
  }).join('');
  const verdict = direction!=='pass' ? classifyVerdict(score) : null;
  const verdictHtml = verdict ? `<div style="margin:10px 0; padding:10px 12px; border-radius:8px; background:var(--panel-2); border-left:3px solid ${verdict.color};">
      <div style="font-weight:700; color:${verdict.color}; font-size:13.5px;">${verdict.label}</div>
      <div style="font-size:12px; color:var(--muted); margin-top:3px;">${verdict.desc}</div>
    </div>` : '';
  const scoreHtml = `
    <div class="scoreCard">
      <div class="scoreCircle" style="color:${gradeColor};">${score.total}<div style="font-size:11px; color:var(--muted); font-weight:600;">${score.grade.label} · ${score.grade.desc}</div></div>
      <div class="scoreBars">${scoreBarsHtml}</div>
    </div>
    ${verdictHtml}
    <div class="fmt" style="margin:-4px 0 12px;">${score.note}</div>
  `;
  const scoreText = `[진입 점수] ${score.total}/100 (${score.grade.label} · ${score.grade.desc})\n` +
    score.breakdown.map(b=>`  - ${b.label}: ${b.score}/${b.max}`).join('\n') + `\n` +
    (verdict ? `  판정: ${verdict.label} — ${verdict.desc}\n` : '') +
    `  ※ ${score.note}\n`;

  resultBody.innerHTML = `
    <div class="badge ${direction==='pass'?'':(direction==='long'?'real':'live')}" style="margin-bottom:10px;">${dirLabel} · ${fmtTime(entryIdx)}</div>
    ${scoreHtml}
    <div class="snapshot">${bestHtml}</div>
    <div class="resultGrid">
      <div class="stat"><div class="k">진입가</div><div class="v">${Math.round(entry.close).toLocaleString()}</div></div>
      <div class="stat"><div class="k">5분 후 손익</div><div class="v ${m5.pct>=0?'up':'down'}">${m5.pct>=0?'+':''}${m5.pct.toFixed(2)}%</div></div>
      <div class="stat"><div class="k">10분 후 손익</div><div class="v ${m10.pct>=0?'up':'down'}">${m10.pct>=0?'+':''}${m10.pct.toFixed(2)}%</div></div>
      <div class="stat"><div class="k">20분 후 손익</div><div class="v ${m20.pct>=0?'up':'down'}">${m20.pct>=0?'+':''}${m20.pct.toFixed(2)}%</div></div>
      <div class="stat"><div class="k">30분 후 손익</div><div class="v ${m30.pct>=0?'up':'down'}">${m30.pct>=0?'+':''}${m30.pct.toFixed(2)}%</div></div>
      <div class="stat"><div class="k">최대이익(MFE)</div><div class="v up">+${mfePct.toFixed(2)}%</div></div>
      <div class="stat"><div class="k">최대손실(MAE)</div><div class="v down">${maePct.toFixed(2)}%</div></div>
    </div>
    <div class="snapshot" style="font-size:14px; line-height:1.8;">
      <h2 style="margin:14px 0 8px;">이 구간, 왜 ${dirLabel}(이었어야) 했나</h2>
      ${narrativeHtml}
      ${rationale?`<p style="color:var(--muted); font-size:12.5px;">내가 적은 진입 근거: <code>${rationale}</code></p>`:''}
    </div>
  `;

  lastSummary = `[리플레이 트레이닝 결과 — 라운드 ${roundNo}]
${scoreText}데이터: ${dataMode==='real'?'실데이터':'합성(연습용)'} / ${session.label}
진입시각: ${fmtTime(entryIdx)} / 방향: ${dirLabel} / 진입가: ${Math.round(entry.close).toLocaleString()}
진입 근거 메모: ${rationale || '(없음)'}
--- 진입 시점 지표 ---
VWAP 대비: ${vsVwap==null?'—':(vsVwap>=0?'+':'')+vsVwap.toFixed(2)+'%'}
EMA 배열: ${align}
Stochastic RSI %K/%D: ${kK!=null?kK.toFixed(1):'—'} / ${kD!=null?kD.toFixed(1):'—'} (${stochState})
--- 이후 결과 ---
5분: ${m5.pct>=0?'+':''}${m5.pct.toFixed(2)}%  10분: ${m10.pct>=0?'+':''}${m10.pct.toFixed(2)}%  30분: ${m30.pct>=0?'+':''}${m30.pct.toFixed(2)}%
구간 내 최대이익(MFE): +${mfePct.toFixed(2)}%  최대손실(MAE): ${maePct.toFixed(2)}%
${bestText}--- 상세 분석 ---
${narrativeText}
이 판단에 대해 피드백 부탁해!`;
}

document.getElementById('btnCopy').addEventListener('click', ()=>{
  const status = document.getElementById('copyStatus');
  function shown(){ status.style.display='block'; setTimeout(()=>status.style.display='none', 2500); }
  if(navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(lastSummary).then(shown);
  else { const ta = document.createElement('textarea'); ta.value = lastSummary; document.body.appendChild(ta); ta.select(); document.execCommand('copy'); document.body.removeChild(ta); shown(); }
});

/* ---------------- API 통신 및 버튼 이벤트 ---------------- */
document.getElementById('btnNewRound').addEventListener('click', ()=>{ dataMode='synthetic'; startRound(genSyntheticSession(Math.floor(Math.random()*99999)+1)); });
document.getElementById('btnNextRound').addEventListener('click', ()=>{ if(dataMode==='synthetic') startRound(genSyntheticSession(Math.floor(Math.random()*99999)+1)); else startRound(session); });
document.getElementById('btnToggleCsv').addEventListener('click', ()=>{ const box = document.getElementById('csvBox'); box.style.display = box.style.display==='none' ? 'block':'none'; });
document.getElementById('btnLoadCsv').addEventListener('click', ()=>{
  const text = document.getElementById('csvInput').value; const candles = parseCsv(text);
  if(candles.length<40){ alert('파싱된 캔들이 너무 적어요 (최소 40개 이상 필요). 형식을 확인해주세요.'); return; }
  dataMode='real'; startRound({label:`실데이터 · ${candles.length}개 캔들`, candles}); document.getElementById('csvBox').style.display='none';
});

// 한투 API 실시간 연동 (일봉/분봉 500개 지원 + 실시간 분석 버튼 활성화)
async function loadDomestic(code, tf) {
    tf = tf || "1m";
    try {
        const res = await fetch(`https://kis-proxy-server-di40.onrender.com/data?code=${code}&tf=${tf}`); 
        const jsonData = await res.json();
        
        if(!jsonData.output2 || jsonData.output2.length === 0) { 
            alert("데이터를 불러오지 못했습니다. 장 운영 시간이 아니거나 종목코드를 확인해 줘. (" + (jsonData.msg1 || '') + ")"); return; 
        }
        
        const rawCandles = jsonData.output2;

        // 🚀 정렬 키: 날짜(yyyyMMdd)를 먼저, 시각(HHmmss)을 뒤에 붙여야 실제 시간순으로 정렬됨.
        // (시각을 먼저 붙이면 여러 날짜가 섞였을 때 날짜가 아니라 시:분:초 기준으로 먼저 정렬되는 버그가 생김)
        function sortKey(c){
            const date = c.stck_bsop_date || '';
            const time = c.stck_cntg_hour || '000000';
            return date + time;
        }
        rawCandles.sort((a, b) => sortKey(a).localeCompare(sortKey(b)));

        const candles = [];
        const seenKeys = new Set(); // 서버에서 중복 제거가 됐어도, 프론트에서 한 번 더 방어
        for(let i=0; i<rawCandles.length; i++) {
            const c = rawCandles[i];
            const key = sortKey(c);
            if(seenKeys.has(key)) continue;
            seenKeys.add(key);

            const o = parseFloat(c.stck_oprc);
            const h = parseFloat(c.stck_hgpr);
            const l = parseFloat(c.stck_lwpr);
            const close = parseFloat(c.stck_prpr || c.stck_clpr);
            const v = parseFloat(c.cntg_vol || c.acml_vol);

            // 값이 없거나(NaN) 0 이하인 이상치 캔들, 그리고 거래량이 0인 "가짜 채움/무거래" 캔들은 걸러낸다.
            // (원인이 API 버그든 실제 거래정지/VI든, 거래량 0인 구간은 VWAP·EMA 계산에 넣으면 지표만 왜곡되므로 제외)
            if(!(o > 0) || !(h > 0) || !(l > 0) || !(close > 0)) continue;
            if(!(v > 0)) continue;

            candles.push({t: candles.length, open: o, high: h, low: l, close: close, o, h, l, c: close, v: isNaN(v) ? 0 : v,
                epochMs: krDateTimeToEpoch(c.stck_bsop_date, c.stck_cntg_hour)});
        }

        if(candles.length < 2) {
            alert("유효한 캔들이 거의 없습니다 (정렬/필터 후 " + candles.length + "개). 원본 응답을 콘솔에서 확인해줘.");
            console.warn('원본 output2:', jsonData.output2);
            return;
        }
        
        dataMode = 'real_live'; 
        const labelText = tf === "D" ? `한투 일봉 차트 (${code})` : `한투 1분봉 500개 차트 (${code})`;
        startRound({label: labelText, candles});

        // 🚀 핵심 포인트: 실시간 모니터링 화면이 떴을 때도 AI 분석 버튼을 바로 쓸 수 있게 강제 활성화!
        document.getElementById('btnAnalyzeCurrent').style.display = 'inline-block';
        document.getElementById('aiAnalysisPanel').style.display = 'block';
        
    } catch(e) { 
        alert("서버와 연결할 수 없어. Render 서버가 깨어나는 중(최대 1분)일 수 있으니 잠시 후 다시 시도해줘!"); 
    }
}

document.getElementById('btnLoadReal').addEventListener('click', () => {
    const input = prompt("조회할 종목코드 (기본 1분봉 500개. 일봉을 보려면 뒤에 ,D 입력)\n예시: 114800 (1분봉 500개) / 114800,D (일봉)", "114800"); 
    if(!input) return;
    const parts = input.split(",");
    loadDomestic(parts[0].trim(), parts.length > 1 ? parts[1].trim().toUpperCase() : "1m");
});

// 🇺🇸 Twelve Data 미국 주식 1분봉 연동 (무료: 하루 800회, 분당 8회)
async function loadUs(symbol) {
    try {
        const res = await fetch(`https://kis-proxy-server-di40.onrender.com/us-data?symbol=${encodeURIComponent(symbol)}`);
        const jsonData = await res.json();

        if(jsonData.status === 'error' || jsonData.code){
            alert("Twelve Data 요청 실패: " + (jsonData.message || '알 수 없는 오류') + "\n(요청 한도를 넘었거나, 티커/키 설정을 확인해줘)");
            return;
        }
        if(jsonData.error){ alert("데이터를 불러오지 못했어: " + jsonData.error); return; }

        const values = jsonData.values;
        if(!values || values.length === 0){ alert("데이터가 비어있어. 티커를 확인하거나(예: AAPL), 미국 장 마감/휴장 시간이라 최근 데이터가 없을 수 있어."); return; }

        const chronological = [...values].reverse(); // Twelve Data는 최신순으로 주므로 과거→현재 순으로 뒤집기
        const candles = [];
        for(const c of chronological){
            const o = parseFloat(c.open), h = parseFloat(c.high), l = parseFloat(c.low), close = parseFloat(c.close), v = parseFloat(c.volume);
            if(!(o > 0) || !(h > 0) || !(l > 0) || !(close > 0)) continue; // 이상치 방어 (국내 데이터와 동일 기준)
            candles.push({t: candles.length, open:o, high:h, low:l, close, o, h, l, c:close, v: isNaN(v) ? 0 : v,
                epochMs: usUtcDatetimeToEpoch(c.datetime)});
        }
        if(candles.length < 2){ alert("유효한 캔들이 거의 없습니다."); return; }

        dataMode = 'real_live';
        startRound({label: `미국 주식 1분봉 · ${symbol.toUpperCase()} (Twelve Data)`, candles});

        // 실시간 모니터링 화면에서도 AI 분석 버튼을 바로 쓸 수 있게 활성화
        document.getElementById('btnAnalyzeCurrent').style.display = 'inline-block';
        document.getElementById('aiAnalysisPanel').style.display = 'block';

    } catch(e) {
        alert("서버(kis-proxy-server)와 연결할 수 없어. Render가 슬립 상태면 첫 요청은 30~60초 걸릴 수 있으니 잠시 후 다시 시도해줘!");
    }
}

document.getElementById('btnLoadUs').addEventListener('click', () => {
    const symbol = prompt("조회할 미국 주식 티커를 입력하세요 (예: AAPL, TSLA, NVDA)", "AAPL");
    if(!symbol) return;
    loadUs(symbol);
});

/* ---------------- ⭐ 관심종목 바로가기 ---------------- */
const WATCHLIST = [
  {label:'삼성전자', code:'005930', market:'kr'},
  {label:'SK하이닉스', code:'000660', market:'kr'},
  {label:'KODEX 200', code:'069500', market:'kr'},
  {label:'KODEX 인버스', code:'114800', market:'kr'},
  {label:'삼성증권', code:'016360', market:'kr'},
  {label:'SOXL', code:'SOXL', market:'us'},
  {label:'QQQ', code:'QQQ', market:'us'},
  {label:'VOO', code:'VOO', market:'us'}
];
const watchlistBar = document.getElementById('watchlistBar');
WATCHLIST.forEach(item => {
  const btn = document.createElement('button');
  btn.className = 'watch-btn';
  btn.textContent = item.label;
  btn.addEventListener('click', () => {
    if(item.market === 'kr') loadDomestic(item.code, '1m');
    else loadUs(item.code);
  });
  watchlistBar.appendChild(btn);
});
/* ---------------- 🚀 AI 스크린샷 분석 로직 ---------------- */
const imageInput = document.getElementById('chartImageInput');
const chartPreview = document.getElementById('chartPreview');
const previewContainer = document.getElementById('imagePreviewContainer');
const btnAnalyzeGemini = document.getElementById('btnAnalyzeGemini');
const btnAnalyzeClaude = document.getElementById('btnAnalyzeClaude');
const aiResultBox = document.getElementById('aiResultBox');

let base64Image = null;

// 1. 이미지 선택 시 미리보기
imageInput.addEventListener('change', function(e) {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = function(event) {
    base64Image = event.target.result;
    chartPreview.src = base64Image;
    previewContainer.style.display = 'block';
    btnAnalyzeGemini.disabled = false;
    btnAnalyzeClaude.disabled = false;
    aiResultBox.style.display = 'none';
  };
  reader.readAsDataURL(file);
});

// 공통 AI 응답 파싱 (Claude / Gemini 두 형식 모두 방어적으로 처리)
function extractAiText(data, isClaude){
  if(isClaude){
    if(data.content && Array.isArray(data.content)){
      const textBlock = data.content.find(c => c.type === 'text' && c.text);
      if(textBlock) return textBlock.text;
    }
    if(data.error) throw new Error(typeof data.error === 'string' ? data.error : (data.error.message || 'Claude 오류'));
    return null;
  }
  // Gemini 표준 응답: candidates[0].content.parts[].text (버전별로 살짝 다를 수 있어 방어적으로 여러 형태 시도)
  try{
    const cand = data.candidates && data.candidates[0];
    if(cand && cand.content && Array.isArray(cand.content.parts)){
      const text = cand.content.parts.map(p=>p.text||'').join('').trim();
      if(text) return text;
    }
    if(typeof data.text === 'string' && data.text.trim()) return data.text;
    if(typeof data.response === 'string' && data.response.trim()) return data.response;
  }catch(e){ /* fallthrough to error/null below */ }
  if(data.error) throw new Error(typeof data.error === 'string' ? data.error : (data.error.message || 'Gemini 오류'));
  console.warn('Gemini 응답을 파싱하지 못했습니다. 원본 응답:', data); // 서버가 실제로 뭘 반환하는지 콘솔에서 확인 가능
  return null;
}

// 공통 분석 함수
async function requestAiAnalysis(provider) {
  if (!base64Image) return;
  const isClaude = provider === 'claude';
  const btn = isClaude ? btnAnalyzeClaude : btnAnalyzeGemini;
  const otherBtn = isClaude ? btnAnalyzeGemini : btnAnalyzeClaude;
  const endpoint = isClaude
    ? 'https://kis-proxy-server-di40.onrender.com/analyze-claude'
    : 'https://kis-proxy-server-di40.onrender.com/analyze';
  const modelName = isClaude ? 'Claude Sonnet 5' : 'Gemini';

  btn.textContent = modelName + ' 분석 중... ⏳';
  btn.disabled = true;
  otherBtn.disabled = true;
  aiResultBox.style.display = 'block';
  aiResultBox.innerHTML = '<span style="color:var(--muted);">' + modelName + ' 분석 중... (최대 30초)</span>';

  try {
    const base64Data = base64Image.split(',')[1];
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image: base64Data })
    });
    const data = await response.json();
    let aiText = null;
    aiText = extractAiText(data, isClaude);

    if (aiText) {
      aiText = aiText.replace(/\n/g, '<br>');
      aiResultBox.innerHTML = '<div style="font-size:11px; color:var(--muted); margin-bottom:8px;">모델: <b style="color:' + (isClaude ? 'var(--amber)' : 'var(--cyan)') + '">' + modelName + '</b></div>' + aiText;
    } else {
      aiResultBox.innerHTML = '<span style="color:var(--amber);">결과를 읽을 수 없습니다.</span>';
    }
  } catch (error) {
    console.error(error);
    aiResultBox.innerHTML = '<span style="color:var(--red);">에러: ' + (error.message || '서버 연결 확인') + '</span>';
  } finally {
    btn.textContent = isClaude ? '🧠 Claude Sonnet 5 분석' : '✨ Gemini 분석';
    btn.disabled = false;
    otherBtn.disabled = false;
  }
}

btnAnalyzeGemini.addEventListener('click', () => requestAiAnalysis('gemini'));
btnAnalyzeClaude.addEventListener('click', () => requestAiAnalysis('claude'));

/* ---------------- 현재 차트 원클릭 분석 ---------------- */
document.getElementById('btnAnalyzeCurrent').addEventListener('click', async function() {
  const btn = this;
  const originalText = btn.textContent;
  const choice = prompt("분석할 AI 선택:\n1 = Gemini\n2 = Claude Sonnet 5", "2");
  if (!choice) return;

  const isClaude = choice.trim() === '2';
  const endpoint = isClaude
    ? 'https://kis-proxy-server-di40.onrender.com/analyze-claude'
    : 'https://kis-proxy-server-di40.onrender.com/analyze';
  const modelName = isClaude ? 'Claude Sonnet 5' : 'Gemini';

  btn.textContent = modelName + ' 분석 중... ⏳';
  btn.disabled = true;
  aiResultBox.style.display = 'block';
  aiResultBox.innerHTML = '<span style="color:var(--muted);">현재 차트 캡처 → ' + modelName + ' 분석 중...</span>';
  document.getElementById('aiAnalysisPanel').scrollIntoView({ behavior: 'smooth', block: 'start' });

  try {
    const priceCanvas = document.getElementById('priceCanvas');
    const volCanvasEl = document.getElementById('volumeCanvas');
    const stochCanvas = document.getElementById('stochCanvas');
    const combined = document.createElement('canvas');
    const gap = 12;
    const legendH = 50; // 범례 바 높이 (다른 캔버스와 동일한 2배 해상도 기준)
    combined.width = priceCanvas.width;
    combined.height = legendH + gap + priceCanvas.height + gap + volCanvasEl.height + gap + stochCanvas.height;
    const ctx = combined.getContext('2d');
    ctx.fillStyle = '#10161F';
    ctx.fillRect(0, 0, combined.width, combined.height);

    // 🚀 범례를 이미지 안에 직접 그려 넣는다 (HTML 텍스트 범례는 캡처에 안 잡히므로,
    // AI가 "이 주황색 선이 VWAP인지 EMA인지" 스스로 못 알아내던 문제를 해결)
    const legendItems = [
      {label:'VWAP', color:'#F5A623'},
      {label:'EMA5', color:'#35C9C1'},
      {label:'EMA9', color:'#E0629E'},
      {label:'EMA20', color:'#8A93A0'},
      {label:'EMA200', color:'#5B6BFF'}
    ];
    ctx.font = 'bold 22px sans-serif';
    let lx = 20;
    for(const item of legendItems){
      ctx.fillStyle = item.color;
      ctx.fillRect(lx, 14, 34, 22);
      ctx.fillStyle = '#DCE4EC';
      ctx.fillText(item.label, lx + 42, 31);
      lx += 42 + ctx.measureText(item.label).width + 34;
    }
    ctx.fillStyle = '#6E7B8B'; ctx.font = '18px sans-serif';
    ctx.fillText('위: 가격·VWAP·EMA / 중간: 거래량(빨강=상승,파랑=하락) / 아래: Stochastic RSI(청록=%K,분홍=%D)', lx + 10, 31);

    let yOff = legendH + gap;
    ctx.drawImage(priceCanvas, 0, yOff); yOff += priceCanvas.height + gap;
    ctx.drawImage(volCanvasEl, 0, yOff); yOff += volCanvasEl.height + gap;
    ctx.drawImage(stochCanvas, 0, yOff);

    const base64Data = combined.toDataURL('image/png').split(',')[1];
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ image: base64Data })
    });
    const data = await response.json();
    let aiText = null;
    aiText = extractAiText(data, isClaude);
    if (aiText) {
      aiText = aiText.replace(/\n/g, '<br>');
      aiResultBox.innerHTML = '<div style="font-size:11px; color:var(--muted); margin-bottom:8px;">모델: <b style="color:' + (isClaude ? 'var(--amber)' : 'var(--cyan)') + '">' + modelName + '</b> (현재 차트)</div>' + aiText;
    } else {
      aiResultBox.innerHTML = '<span style="color:var(--amber);">응답 파싱 실패</span>';
    }
  } catch (error) {
    console.error(error);
    aiResultBox.innerHTML = '<span style="color:var(--red);">요청 실패</span>';
  } finally {
    btn.textContent = originalText;
    btn.disabled = false;
  }
});
</script>

</body>
</html>
