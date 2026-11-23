package com.dxhub.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String SERVER_URL = "https://dxhub-server.onrender.com";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // स्क्रीनशॉट और स्क्रीन रिकॉर्डिंग ब्लॉक (Security)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);

        webView.addJavascriptInterface(new DXBridge(), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://t.me/") || url.startsWith("tg://")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });

        String deepVidId = "";
        boolean autoUnlock = false;
        Uri data = getIntent().getData();
        if (data != null) {
            String path = data.getPath();
            if (path != null && path.length() > 1) {
                deepVidId = path.substring(1);
            }
            if ("DX1234".equals(data.getQueryParameter("unlock"))) {
                autoUnlock = true;
            }
        }

        webView.loadDataWithBaseURL(SERVER_URL, buildAppHtml(deepVidId, autoUnlock), "text/html", "UTF-8", null);
    }

    public class DXBridge {
        @JavascriptInterface
        public void downloadEncryptedVideo(String vidId, String fileName) {
            new Thread(() -> {
                try {
                    File vaultDir = new File(getFilesDir(), "dx_vault");
                    if (!vaultDir.exists()) vaultDir.mkdirs();
                    File outFile = new File(vaultDir, vidId + ".dxv");

                    URL url = new URL(SERVER_URL + "/stream/" + vidId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outFile);
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                    fos.close();
                    is.close();

                    runOnUiThread(() -> webView.evaluateJavascript("onDownloadComplete('" + vidId + "', '" + fileName + "')", null));
                } catch (Exception e) {
                    runOnUiThread(() -> webView.evaluateJavascript("alert('Download Error: " + e.getMessage() + "')", null));
                }
            }).start();
        }

        @JavascriptInterface
        public String getLocalVideoPath(String vidId) {
            File f = new File(new File(getFilesDir(), "dx_vault"), vidId + ".dxv");
            return f.exists() ? "file://" + f.getAbsolutePath() : "";
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private String buildAppHtml(String deepVidId, boolean autoUnlock) {
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>" +
               "<style>" +
               "*{box-sizing:border-box;font-family:sans-serif;margin:0;padding:0}" +
               "body{background:#090d11;color:#fff;padding-bottom:30px}" +
               ".header{background:#111820;padding:15px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #1f2937}" +
               ".logo{font-size:20px;font-weight:800;color:#10b981;letter-spacing:1px}" +
               ".badge{background:#f59e0b;color:#000;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:bold}" +
               ".card{background:#131b24;margin:12px;padding:15px;border-radius:14px;border:1px solid #1e293b}" +
               ".btn{background:#10b981;color:#fff;border:none;padding:13px;width:100%;border-radius:10px;font-size:15px;font-weight:bold;margin-top:10px;cursor:pointer}" +
               ".btn-gold{background:linear-gradient(90deg,#f59e0b,#d97706);color:#000}" +
               ".btn-blue{background:#2563eb}" +
               "input{width:100%;padding:12px;border-radius:10px;border:1px solid #334155;background:#090d11;color:#fff;margin-top:8px;font-size:15px}" +
               ".ad-mrec{width:300px;height:250px;background:#1e293b;border:1px dashed #475569;margin:12px auto;display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:10px;color:#94a3b8;text-align:center}" +
               ".ad-banner{width:100%;height:55px;background:#1e293b;border-bottom:1px solid #334155;display:flex;align-items:center;justify-content:center;font-size:12px;color:#94a3b8}" +
               ".hidden{display:none!important}" +
               "video{width:100%;background:#000;max-height:240px}" +
               ".grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:10px}" +
               ".vitem{background:#0f172a;padding:8px;border-radius:10px;font-size:12px}" +
               ".vitem img{width:100%;height:85px;object-fit:cover;border-radius:6px}" +
               ".overlay{position:fixed;inset:0;background:rgba(0,0,0,0.95);z-index:999;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:20px;text-align:center}" +
               "</style></head><body>" +

               "<!-- LOCKER SCREEN -->" +
               "<div id='lockScreen' class='overlay'>" +
               "<h2 style='color:#10b981;margin-bottom:8px'>DX Vault Calc</h2>" +
               "<p style='color:#94a3b8;font-size:13px;margin-bottom:15px'>Enter 4-Digit Security PIN to Unlock</p>" +
               "<input type='password' id='pinInput' maxlength='4' placeholder='Enter PIN (1234)' style='max-width:220px;text-align:center;font-size:20px;letter-spacing:6px'>" +
               "<button class='btn' style='max-width:220px' onclick='checkPin()'>UNLOCK</button>" +
               "</div>" +

               "<!-- REWARDED 15S AD OVERLAY -->" +
               "<div id='rewardOverlay' class='overlay hidden'>" +
               "<div class='badge'>REWARDED VIDEO AD</div>" +
               "<h2 style='margin:15px 0' id='adTimerText'>Reward in 15s...</h2>" +
               "<div class='ad-mrec'><b style='color:#fff'>SPONSORED AD</b><br><small id='adUnitLabel'>Test Rewarded Ad (15s)</small></div>" +
               "<button id='closeAdBtn' class='btn hidden' style='max-width:220px' onclick='finishRewardAd()'>Continue to File ▶</button>" +
               "</div>" +

               "<!-- MAIN APP -->" +
               "<div id='mainApp' class='hidden'>" +
               "<div class='header'><div class='logo'>DX HUB</div><div id='vipBadge' class='badge' onclick='openPremium()'>GO PREMIUM</div></div>" +

               "<!-- HOME VIEW -->" +
               "<div id='homeView'>" +
               "<div class='card'><h3>🔗 Paste Video Link</h3>" +
               "<input id='linkBox' placeholder='Paste dxhub link or Video ID...'>" +
               "<button class='btn' onclick='openFromInput()'>Open & Stream Video</button></div>" +

               "<div id='homeMrec' class='ad-mrec'><b>MREC AD (300x250)</b><small>Auto-Playing Ad Slot</small></div>" +

               "<div class='card'><h3>📥 Your Downloads (Offline Vault)</h3>" +
               "<p style='font-size:12px;color:#94a3b8;margin-top:4px'>Saved inside DX HUB only (Encrypted)</p>" +
               "<div id='downloadsList' style='margin-top:10px;font-size:13px;color:#cbd5e1'>No offline videos yet.</div></div>" +

               "<div class='card' style='border-color:#f59e0b'><h3>👑 Go Premium (Ad-Free)</h3>" +
               "<p style='font-size:13px;color:#cbd5e1;margin-top:5px'>Remove all 15s Rewarded & MREC Ads for 30 Days (₹299/month).</p>" +
               "<button class='btn btn-gold' onclick=\"window.location.href='https://t.me/helponn_bot'\">Buy Key via @helponn_bot</button>" +
               "<input id='vipKeyInput' placeholder='Enter VIP Key (DXVIP-XXXX)'>" +
               "<button class='btn btn-blue' onclick='activateVip()'>Activate VIP Key</button></div>" +
               "</div>" +

               "<!-- UNLOCKER PAGE 1 -->" +
               "<div id='unlockerView' class='hidden'>" +
               "<div id='mrec1' class='ad-mrec'><b>MREC AD #1 (300x250)</b><small>Sponsored</small></div>" +
               "<div class='card' style='text-align:center'><h3 id='unlockFileTitle'>File Ready</h3>" +
               "<p style='font-size:12px;color:#94a3b8;margin-top:5px'>Click below & watch 15s ad to unlock stream</p>" +
               "<button class='btn' onclick=\"triggerReward('stream')\">🔓 Open File Now</button></div>" +
               "<div id='mrec2' class='ad-mrec'><b>MREC AD #2 (300x250)</b><small>Sponsored</small></div>" +
               "</div>" +

               "<!-- PLAYER VIEW -->" +
               "<div id='playerView' class='hidden'>" +
               "<div id='topBanner' class='ad-banner'>HEADER BANNER AD (Auto-Refresh 30s)</div>" +
               "<video id='vidPlayer' controls playsinline></video>" +
               "<div class='card'><h3 id='playerTitle'>Video</h3>" +
               "<button class='btn' onclick=\"triggerReward('download')\">⬇️ Download Offline (In-App)</button>" +
               "<button class='btn' style='background:#334155' onclick='showHome()'>⬅ Back to Home</button></div>" +
               "<div id='playerMrec' class='ad-mrec'><b>MREC BOX AD (Auto-Refresh 30s)</b><small>300x250 Display</small></div>" +
               "<div class='card'><h3>📂 More Videos</h3><div id='moreGrid' class='grid'></div></div>" +
               "</div>" +

               "<!-- ADMIN PANEL (PIN 9999) -->" +
               "<div id='adminView' class='hidden'>" +
               "<div class='card'><h2>👑 DX HUB Admin Control</h2>" +
               "<p style='font-size:12px;color:#94a3b8'>Control Ads without rebuilding APK</p>" +
               "<label style='display:block;margin-top:12px'>Ad Status:</label>" +
               "<select id='admAdsOn' style='width:100%;padding:10px;background:#090d11;color:#fff;margin-top:5px'><option value='1'>Ads Enabled (ON)</option><option value='0'>Ads Disabled (OFF)</option></select>" +
               "<input id='admBanner' placeholder='AppLovin Banner Unit ID'>" +
               "<input id='admMrec' placeholder='AppLovin MREC Unit ID'>" +
               "<input id='admReward' placeholder='AppLovin Rewarded Unit ID'>" +
               "<button class='btn' onclick='saveAdmin()'>Save Admin Settings</button>" +
               "<button class='btn' style='background:#334155' onclick='showHome()'>Exit Admin</button></div>" +
               "</div>" +
               "</div>" +

               "<script>" +
               "const SERVER='" + SERVER_URL + "';" +
               "let currentVid=''; let pendingAction=''; let isVip=localStorage.getItem('dx_vip')==='1';" +
               "let adCfg=JSON.parse(localStorage.getItem('dx_adcfg')||'{\"on\":true,\"banner\":\"TEST\",\"mrec\":\"TEST\",\"reward\":\"TEST\"}');" +

               "function checkPin(){" +
               "  const p=document.getElementById('pinInput').value;" +
               "  if(p==='1234'){ unlockApp(); }" +
               "  else if(p==='9999'){ unlockApp(); showAdmin(); }" +
               "  else{ alert('Wrong PIN'); }" +
               "}" +
               "function unlockApp(){" +
               "  document.getElementById('lockScreen').classList.add('hidden');" +
               "  document.getElementById('mainApp').classList.remove('hidden');" +
               "  updateVipUI(); renderDownloads();" +
               "}" +
               "function updateVipUI(){" +
               "  if(isVip){" +
               "    document.getElementById('vipBadge').innerText='VIP ACTIVE 👑';" +
               "    document.querySelectorAll('.ad-mrec,.ad-banner').forEach(e=>e.classList.add('hidden'));" +
               "  } else if(!adCfg.on){" +
               "    document.querySelectorAll('.ad-mrec,.ad-banner').forEach(e=>e.classList.add('hidden'));" +
               "  }" +
               "}" +
               "function openFromInput(){" +
               "  let val=document.getElementById('linkBox').value.trim();" +
               "  if(!val) return;" +
               "  let id=val.includes('/watch/')?val.split('/watch/')[1].split('?')[0]:val;" +
               "  openUnlocker(id);" +
               "}" +
               "function openUnlocker(id){" +
               "  currentVid=id;" +
               "  document.getElementById('homeView').classList.add('hidden');" +
               "  document.getElementById('playerView').classList.add('hidden');" +
               "  document.getElementById('unlockerView').classList.remove('hidden');" +
               "  document.getElementById('unlockFileTitle').innerText='File: '+id+'_.mp4';" +
               "}" +
               "function triggerReward(act){" +
               "  pendingAction=act;" +
               "  if(isVip || !adCfg.on){ finishRewardAd(); return; }" +
               "  document.getElementById('rewardOverlay').classList.remove('hidden');" +
               "  document.getElementById('closeAdBtn').classList.add('hidden');" +
               "  document.getElementById('adUnitLabel').innerText='Unit: '+adCfg.reward;" +
               "  let sec=15; document.getElementById('adTimerText').innerText='Reward in '+sec+'s...';" +
               "  let iv=setInterval(()=>{" +
               "    sec--; document.getElementById('adTimerText').innerText=sec>0?'Reward in '+sec+'s...':'Reward Unlocked!';" +
               "    if(sec<=0){ clearInterval(iv); document.getElementById('closeAdBtn').classList.remove('hidden'); }" +
               "  },1000);" +
               "}" +
               "function finishRewardAd(){" +
               "  document.getElementById('rewardOverlay').classList.add('hidden');" +
               "  if(pendingAction==='stream') startPlayer(currentVid);" +
               "  else if(pendingAction==='download'){" +
               "    alert('Downloading inside DX HUB Vault...');" +
               "    AndroidBridge.downloadEncryptedVideo(currentVid, currentVid+'_.mp4');" +
               "  }" +
               "}" +
               "function startPlayer(id){" +
               "  currentVid=id;" +
               "  document.getElementById('unlockerView').classList.add('hidden');" +
               "  document.getElementById('homeView').classList.add('hidden');" +
               "  document.getElementById('playerView').classList.remove('hidden');" +
               "  document.getElementById('playerTitle').innerText=id+'_.mp4';" +
               "  let local=AndroidBridge.getLocalVideoPath(id);" +
               "  document.getElementById('vidPlayer').src=local?local:(SERVER+'/stream/'+id);" +
               "  document.getElementById('vidPlayer').play();" +
               "  fetch(SERVER+'/api/video/'+id).then(r=>r.json()).then(d=>{" +
               "    let g=document.getElementById('moreGrid'); g.innerHTML='';" +
               "    (d.more_videos||[]).forEach(v=>{" +
               "      g.innerHTML+=\"<div class='vitem' onclick=\\\"openUnlocker('\"+v.vid_id+\"')\\\"><img src='\"+SERVER+\"/api/thumb/\"+v.vid_id+\"' onerror=\\\"this.style.display='none'\\\"><div>\"+v.safe_name+\"</div></div>\";" +
               "    });" +
               "  });" +
               "}" +
               "function onDownloadComplete(id, name){" +
               "  let list=JSON.parse(localStorage.getItem('dx_dl')||'[]');" +
               "  if(!list.includes(id)) list.push(id);" +
               "  localStorage.setItem('dx_dl', JSON.stringify(list));" +
               "  alert('Saved in DX HUB Offline Vault!'); renderDownloads();" +
               "}" +
               "function renderDownloads(){" +
               "  let list=JSON.parse(localStorage.getItem('dx_dl')||'[]');" +
               "  let box=document.getElementById('downloadsList');" +
               "  if(!list.length){ box.innerHTML='No offline videos yet.'; return; }" +
               "  box.innerHTML=list.map(id=>\"<div style='padding:8px;background:#0f172a;border-radius:8px;margin-top:6px;display:flex;justify-content:space-between'><span>🔒 \"+id+\"_.mp4</span><b style='color:#10b981' onclick=\\\"startPlayer('\"+id+\"')\\\">PLAY</b></div>\").join('');" +
               "}" +
               "function activateVip(){" +
               "  let k=document.getElementById('vipKeyInput').value.trim();" +
               "  fetch(SERVER+'/api/verify_vip/'+k).then(r=>r.json()).then(d=>{" +
               "    if(d.valid){ isVip=true; localStorage.setItem('dx_vip','1'); updateVipUI(); alert('VIP Activated! All Ads Removed.'); }" +
               "    else alert('Invalid or Expired VIP Key');" +
               "  });" +
               "}" +
               "function showHome(){" +
               "  document.getElementById('vidPlayer').pause();" +
               "  document.getElementById('playerView').classList.add('hidden');" +
               "  document.getElementById('unlockerView').classList.add('hidden');" +
               "  document.getElementById('adminView').classList.add('hidden');" +
               "  document.getElementById('homeView').classList.remove('hidden');" +
               "}" +
               "function showAdmin(){" +
               "  document.getElementById('homeView').classList.add('hidden');" +
               "  document.getElementById('adminView').classList.remove('hidden');" +
               "}" +
               "function saveAdmin(){" +
               "  adCfg={on:document.getElementById('admAdsOn').value==='1',banner:document.getElementById('admBanner').value||'TEST',mrec:document.getElementById('admMrec').value||'TEST',reward:document.getElementById('admReward').value||'TEST'};" +
               "  localStorage.setItem('dx_adcfg',JSON.stringify(adCfg));" +
               "  alert('Admin Settings Saved!'); showHome(); updateVipUI();" +
               "}" +
               "setInterval(()=>{" +
               "  if(!isVip && adCfg.on){" +
               "    document.getElementById('topBanner').innerText='HEADER BANNER ('+adCfg.banner+') • Refreshed '+new Date().toLocaleTimeString();" +
               "    document.getElementById('playerMrec').innerHTML='<b>MREC BOX ('+adCfg.mrec+')</b><small>Refreshed '+new Date().toLocaleTimeString()+'</small>';" +
               "  }" +
               "},30000);" +
               (autoUnlock ? "unlockApp();" : "") +
               (!deepVidId.isEmpty() ? "openUnlocker('" + deepVidId + "');" : "") +
               "</script></body></html>";
    }
}
