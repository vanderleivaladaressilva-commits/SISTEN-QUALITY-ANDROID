package com.sisten.quality;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;

                } catch (ActivityNotFoundException e) {
                    return false;
                }
            }
        });

        /*
         * Permite anexar arquivos pelo celular.
         */
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback
                            .onReceiveValue(null);
                }

                MainActivity.this.filePathCallback =
                        filePathCallback;

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

        /*
         * Permite baixar relatórios e arquivos.
         */
        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimetype,
                    long contentLength) {

                try {

                    android.app.DownloadManager.Request request =
                            new android.app.DownloadManager.Request(
                                    Uri.parse(url)
                            );

                    request.setTitle("SISTEN QUALITY");
                    request.setDescription(
                            "Baixando relatório"
                    );

                    if (mimetype != null) {
                        request.setMimeType(mimetype);
                    }

                    request.setNotificationVisibility(
                            android.app.DownloadManager
                                    .Request
                                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    );

                    String extensao = "";

                    if (mimetype != null
                            && mimetype.contains("pdf")) {

                        extensao = ".pdf";
                    }

                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "SISTEN_QUALITY_"
                                    + System.currentTimeMillis()
                                    + extensao
                    );

                    android.app.DownloadManager downloadManager =
                            (android.app.DownloadManager)
                                    getSystemService(
                                            DOWNLOAD_SERVICE
                                    );

                    downloadManager.enqueue(request);

                    Toast.makeText(
                            MainActivity.this,
                            "Download iniciado. Verifique a pasta Downloads.",
                            Toast.LENGTH_LONG
                    ).show();

                } catch (Exception e) {

                    try {

                        Intent intent =
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                );

                        startActivity(intent);

                    } catch (Exception ignored) {

                        Toast.makeText(
                                MainActivity.this,
                                "Não foi possível baixar o arquivo.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        });

        /*
         * Abre o sistema SISTEN QUALITY.
         */
        webView.loadUrl(
                "https://sistenlab.netlify.app"
        );
    }

    /*
     * Recebe o arquivo escolhido pelo usuário.
     */
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

            if (resultCode == RESULT_OK
                    && data != null) {

                /*
                 * Vários arquivos.
                 */
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

                /*
                 * Um arquivo.
                 */
                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
                }
            }

            filePathCallback.onReceiveValue(results);

            filePathCallback = null;
        }
    }

    /*
     * Botão voltar do Android.
     */
    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}