package sh.siava.pixelxpert.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import sh.siava.pixelxpert.PixelXpert;
import com.topjohnwu.superuser.Shell;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // This is necessary because loading file:///data/adb directly will
        // result in ERR_FILE_NOT_FOUND or ERR_ACCESS_DENIED for regular apps
        // since /data/adb is strictly root-only readable on newer Androids.

        if (PixelXpert.get().hasRootAccess()) {
            try {
                // Read the HTML content via libsu Root Shell
                List<String> output = Shell.cmd("cat /data/adb/modules/PixelXpert-Minimal/webroot/index.html").exec().getOut();
                StringBuilder htmlBuilder = new StringBuilder();
                for (String line : output) {
                    htmlBuilder.append(line).append("\n");
                }

                String htmlContent = htmlBuilder.toString();
                if (htmlContent.trim().isEmpty()) {
                    throw new Exception("Webroot empty or module not installed.");
                }

                // KSU uses a specific environment, but since our GUI only executes shell commands,
                // we'll inject a mock `ksu` object that performs su commands via a JavascriptInterface
                // or just load the HTML if it relies on KSU app's internal injection.
                // NOTE: the KSU app automatically injects `ksu.exec` into webviews it owns,
                // but for our own app, we need to bridge it.

                webView.addJavascriptInterface(new RootBridge(), "ksuRootBridge");

                // Inject the JS bridge to replace `ksu.exec` with our own bridge
                String injectedHtml = htmlContent.replace("<script>",
                    "<script>\n" +
                    "window.ksu = { exec: function(cmd) { return JSON.parse(ksuRootBridge.exec(cmd)); } };\n"
                );

                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.loadDataWithBaseURL(null, injectedHtml, "text/html", "UTF-8", null);

            } catch (Exception e) {
                Toast.makeText(this, "Failed to load module WebGUI. Is PixelXpert installed in Magisk/KernelSU?", Toast.LENGTH_LONG).show();
            }
        } else {
             Toast.makeText(this, "Root access required to configure module.", Toast.LENGTH_LONG).show();
        }
    }

    public class RootBridge {
        @android.webkit.JavascriptInterface
        public String exec(String command) {
            try {
                Shell.Result result = Shell.cmd(command).exec();
                int exitCode = result.getCode();

                StringBuilder stdout = new StringBuilder();
                for (String line : result.getOut()) {
                    stdout.append(line).append("\n");
                }

                // Return a JSON string that mimics the KSU return format: { errno: 0, stdout: "..." }
                String json = "{\"errno\": " + exitCode + ", \"stdout\": \"" + stdout.toString().replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
                return json;
            } catch (Exception e) {
                return "{\"errno\": -1, \"stdout\": \"\"}";
            }
        }
    }
}
