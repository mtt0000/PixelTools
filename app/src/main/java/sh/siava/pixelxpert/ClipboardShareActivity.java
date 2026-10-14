package sh.siava.pixelxpert;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ClipboardShareActivity extends Activity {
    private static final String TAG = "ClipboardShareActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                handleSend(intent);
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                handleSendMultiple(intent);
            }
        }

        finish();
    }

    private void handleSend(Intent intent) {
        StringBuilder contentBuilder = new StringBuilder();

        // Handle text
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                contentBuilder.append(text);
            }
        }

        // Handle file URI
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                String fileContent = readFileContent(uri);
                if (fileContent != null && !fileContent.isEmpty()) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append(fileContent);
                }
            }
        }

        copyToClipboard(contentBuilder.toString());
    }

    private void handleSendMultiple(Intent intent) {
        StringBuilder contentBuilder = new StringBuilder();

        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (imageUris != null) {
                for (Uri uri : imageUris) {
                    if (uri != null) {
                        String fileContent = readFileContent(uri);
                        if (fileContent != null && !fileContent.isEmpty()) {
                            if (contentBuilder.length() > 0) {
                                contentBuilder.append("\n\n---\n\n");
                            }
                            contentBuilder.append(fileContent);
                        }
                    }
                }
            }
        } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            // Unlikely to have multiple texts without stream, but just in case
            ArrayList<CharSequence> texts = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT);
            if (texts != null) {
                for (CharSequence text : texts) {
                    if (text != null) {
                        if (contentBuilder.length() > 0) {
                            contentBuilder.append("\n\n---\n\n");
                        }
                        contentBuilder.append(text);
                    }
                }
            }
        }

        copyToClipboard(contentBuilder.toString());
    }

    private String readFileContent(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading file content from URI: " + uri, e);
            // Non fatale per permettere a eventuali altri file di essere elaborati, e per dare almeno un feedback
            return "Error reading file: " + e.getMessage();
        }
        return stringBuilder.toString();
    }

    private void copyToClipboard(String content) {
        if (content == null || content.isEmpty()) {
            Toast.makeText(this, "No content found to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Copied Content", content);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to get clipboard service", Toast.LENGTH_SHORT).show();
        }
    }
}
