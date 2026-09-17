package com.sisten.quality;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }

                if ("blob".equalsIgnoreCase(scheme)) {
                    return false;
                }

                try {
                    startActivity(
                            new Intent(Intent.ACTION_VIEW, uri)
                    );
                    return true;

                } catch (ActivityNotFoundException e) {
                    return false;
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams fileChooserParams) {

                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }

                MainActivity.this.filePathCallback = callback;

                try {

                    Intent intent =
                            fileChooserParams.createIntent();

                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (ActivityNotFoundException e) {

                    MainActivity.this.filePathCallback = null;

                    Toast.makeText(
                            MainActivity.this,
                            "Não foi possível abrir o seletor de arquivos.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return false;
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimetype,
                    long contentLength) {

                baixarArquivo(
                        url,
                        userAgent,
                        contentDisposition,
                        mimetype
                );
            }
        });

        webView.loadUrl("https://sistenlab.netlify.app");
    }

    private void baixarArquivo(
            String url,
            String userAgent,
            String contentDisposition,
            String mimetype) {

        try {

            Uri uri = Uri.parse(url);

            android.app.DownloadManager.Request request =
                    new android.app.DownloadManager.Request(uri);

            request.setTitle("SISTEN QUALITY");
            request.setDescription("Baixando arquivo");

            request.setNotificationVisibility(
                    android.app.DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            String extensao = ".pdf";

            if (mimetype != null) {

                if (mimetype.contains("pdf")) {
                    extensao = ".pdf";

                } else if (mimetype.contains("image")) {
                    extensao = ".jpg";

                } else if (mimetype.contains("word")) {
                    extensao = ".docx";

                } else if (mimetype.contains("excel")
                        || mimetype.contains("spreadsheet")) {
                    extensao = ".xlsx";
                }
            }

            String nomeArquivo =
                    "SISTEN_QUALITY_"
                            + System.currentTimeMillis()
                            + extensao;

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    nomeArquivo
            );

            String cookie =
                    CookieManager
                            .getInstance()
                            .getCookie(url);

            if (cookie != null) {
                request.addRequestHeader(
                        "Cookie",
                        cookie
                );
            }

            if (userAgent != null) {
                request.addRequestHeader(
                        "User-Agent",
                        userAgent
                );
            }

            android.app.DownloadManager manager =
                    (android.app.DownloadManager)
                            getSystemService(DOWNLOAD_SERVICE);

            if (manager != null) {

                manager.enqueue(request);

                Toast.makeText(
                        MainActivity.this,
                        "Download iniciado. Verifique a pasta Downloads.",
                        Toast.LENGTH_LONG
                ).show();

            } else {

                abrirArquivo(url);
            }

        } catch (Exception e) {

            abrirArquivo(url);
        }
    }

    private void abrirArquivo(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Não foi possível abrir ou baixar o arquivo.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (filePathCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count =
                            data.getClipData()
                                    .getItemCount();

                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {

                        results[i] =
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri();
                    }

                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
       