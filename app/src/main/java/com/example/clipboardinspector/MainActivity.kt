package com.example.clipboardinspector

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private const val TAG = "MainActivity"
private const val AUTHORITY = "com.example.clipboardinspector.contentprovider"

class ClipboardInspectorFileProvider : FileProvider(R.xml.file_paths) {}

class ClipboardInspectorContentProvider : ContentProvider() {
  private var provider = ClipboardInspectorFileProvider()

  override fun attachInfo(context: Context, info: ProviderInfo) {
    super.attachInfo(context, info)
    // FileProvider throws if it finds out this is exported.
    val exported = info.exported
    info.exported = false
    provider.attachInfo(context, info)
    info.exported = exported
  }

  override fun onCreate(): Boolean {
    return provider.onCreate()
  }
  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?
  ): Cursor? {
    val oldPolicy = StrictMode.allowThreadDiskReads()
    try {
      return provider.query(uri, projection, selection, selectionArgs, sortOrder)
    } finally {
      StrictMode.setThreadPolicy(oldPolicy)
      }
  }
  override fun getType(uri: Uri): String? {
    return provider.getType(uri)
  }
  @Throws(FileNotFoundException::class)
  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    return provider.openFile(uri, mode)
  }
  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    return provider.insert(uri, values)
  }
  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    return provider.delete(uri, selection, selectionArgs)
  }
  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?
  ): Int {
    return provider.update(uri, values, selection, selectionArgs)
  }
}

class MainActivity : AppCompatActivity() {
  var filePath: ValueCallback<Array<Uri>>? = null
  val getFile = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()) {
    if (it.resultCode == Activity.RESULT_CANCELED) {
      filePath?.onReceiveValue(null)
    } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
      filePath!!.onReceiveValue(
        WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data))
      filePath = null
    }
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }
    Log.i(TAG, "XXX copying asset files to " + getExternalFilesDir(null))
    for (filename in assets.list("")!!) {
      var inStream : InputStream? = null
      var outStream : OutputStream? = null
      try {
        inStream = assets.open(filename)
        outStream = FileOutputStream(File(getExternalFilesDir(null), filename))
        inStream.copyTo(outStream)
      } catch (e: IOException) {
        Log.i(TAG, "XXX failed to copy " + filename)
      } finally {
        inStream?.close()
        outStream?.close()
      }
    }
    var webview = findViewById<WebView>(R.id.webview)
    webview.settings.javaScriptEnabled = true
    webview.webChromeClient = object: WebChromeClient() {
      override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
      ): Boolean {
        this@MainActivity.filePath = filePathCallback
        val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentIntent.type = "*/*"
        contentIntent.addCategory(Intent.CATEGORY_OPENABLE)
        this@MainActivity.getFile.launch(contentIntent)
        return true
      }
    }
    webview.loadUrl("file:///android_asset/clipboard_inspector.html")
    findViewById<Button>(R.id.empty).setOnLongClickListener { view ->
      startDragAndDrop(view, null)
      true
    }
    findViewById<Button>(R.id.text).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataText())
      true
    }
    findViewById<Button>(R.id.html).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataHtml())
      true
    }
    findViewById<Button>(R.id.url).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataUrl())
      true
    }
    findViewById<Button>(R.id.file_text).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFileText())
      true
    }
    findViewById<Button>(R.id.file_xtx).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFileXtx())
      true
    }
    findViewById<Button>(R.id.file_text_multi).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFileTextMulti())
      true
    }
    findViewById<Button>(R.id.text_html_uri).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataTextHtmlUri())
      true
    }
    findViewById<Button>(R.id.file_jpg).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFileJpg())
      true
    }
    findViewById<Button>(R.id.file_png).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFilePng())
      true
    }
    findViewById<Button>(R.id.file_multi).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFileMulti())
      true
    }
    findViewById<Button>(R.id.file_pdf).setOnLongClickListener { view ->
      startDragAndDrop(view, getClipDataFilePdf())
      true
    }
    findViewById<EditText>(R.id.inspect).setOnDragListener { _, event ->
      if (event.action == DragEvent.ACTION_DRAG_STARTED) {
        inspect("drag-start", event.clipDescription, event.clipData)
      } else if (event.action == DragEvent.ACTION_DRAG_ENTERED) {
        inspect("drag-enter", event.clipDescription, event.clipData)
      } else if (event.action == DragEvent.ACTION_DRAG_EXITED) {
        inspect("drag-exit", event.clipDescription, event.clipData)
      } else if (event.action == DragEvent.ACTION_DROP) {
        val dropPermissions = requestDragAndDropPermissions(event)
        inspect("drop", event.clipDescription, event.clipData)
        dropPermissions?.release()
      }
      true
    }
  }

  private fun getUriForFile(context: Context, authority: String, file: File) : Uri {
    //val externalFilesDir = context.getExternalFilesDir(null)!!.path
    //assert(file.path.startsWith(externalFilesDir, false))
    //return Uri.parse("content://" + AUTHORITY + file.path.substring(externalFilesDir.length))
    return FileProvider.getUriForFile(context, authority, file)
  }

  private fun startDragAndDrop(view : View, clipData : ClipData?) {
    view.startDragAndDrop(clipData, View.DragShadowBuilder(view), null, View.DRAG_FLAG_GLOBAL + View.DRAG_FLAG_GLOBAL_URI_READ)
  }

  @RequiresApi(Build.VERSION_CODES.P)
  fun onCopyEmpty(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.clearPrimaryClip()
  }

  private fun getClipDataText() : ClipData {
    return ClipData.newPlainText("label", "plain text")
  }
  fun onCopyText(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataText())
  }

  private fun getClipDataHtml() : ClipData {
    //return ClipData( "label", arrayOf(ClipDescription.MIMETYPE_TEXT_HTML,ClipDescription.MIMETYPE_TEXT_PLAIN), ClipData.Item("html plain text", "html <b>formatted</b> text"))
    return ClipData.newHtmlText("label", "html plain text", "html <b>formatted</b> text")
  }
  fun onCopyHtml(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataHtml())
  }

  private fun getClipDataUrl() : ClipData {
    return ClipData( "label", arrayOf("text/x-moz-url", ClipDescription.MIMETYPE_TEXT_PLAIN), ClipData.Item("http://example.com", null))
  }
  fun onCopyUrl(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataUrl())
  }

  private fun getClipDataTextHtmlUri() : ClipData {
    val f = File(getExternalFilesDir(null), "PNG_Test.png")
    val uri = getUriForFile(this, AUTHORITY, f)
    val clipData = ClipData.newHtmlText(null, "test text", "test <b>html</b>")
    clipData.addItem(ClipData.Item(uri.toString(), null, uri))
    return clipData
    //return ClipData(ClipDescription("label", arrayOf("text/html")), ClipData.Item("text", "html", null, uri))
  }
  fun onCopyTextHtmlUri(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataTextHtmlUri())
  }

  private fun getClipDataFileText() : ClipData {
    val f = File(getExternalFilesDir(null), "hello.txt")
    val uri = getUriForFile(this, AUTHORITY, f)
    val clipData = ClipData.newUri(contentResolver,  "label", uri)
    return clipData
  }
  fun onCopyFileText(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFileText())
  }

  private fun getClipDataFileXtx() : ClipData {
    val f = File(getExternalFilesDir(null), "clanky.xtx")
    val uri = getUriForFile(this, AUTHORITY, f)
    val clipData = ClipData.newUri(contentResolver,  "label", uri)
    return clipData
  }
  fun onCopyFileXtx(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFileXtx())
  }

  private fun getClipDataFileTextMulti() : ClipData {
    val f1 = File(getExternalFilesDir(null), "hello.txt")
    val f2 = File(getExternalFilesDir(null), "clanky.xtx")
    val uri1 = getUriForFile(this, AUTHORITY, f1)
    val uri2 = getUriForFile(this, AUTHORITY, f2)
    val clipData = ClipData.newUri(contentResolver,  "label", uri1)
    clipData.addItem(ClipData.Item(uri2))
    return clipData
  }
  fun onCopyFileTextMulti(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFileTextMulti())
  }

  private fun getClipDataFileJpg() : ClipData {
    val f = File(getExternalFilesDir(null), "kitten.jpg")
    val uri = getUriForFile(this, AUTHORITY, f)
    return ClipData.newUri(contentResolver, "label", uri)
  }
  fun onCopyFileJpg(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFileJpg())
  }

  private fun getClipDataFileMulti() : ClipData {
    val f1 = File(getExternalFilesDir(null), "kittens.jpg")
    val f2 = File(getExternalFilesDir(null), "puppy.png")
    val f3 = File(getExternalFilesDir(null), "hello.txt")
    val f4 = File(getExternalFilesDir(null), "pdf-test.pdf")
    val uri1 = getUriForFile(this, AUTHORITY, f1)
    val uri2 = getUriForFile(this, AUTHORITY, f2)
    val uri3 = getUriForFile(this, AUTHORITY, f3)
    val uri4 = getUriForFile(this, AUTHORITY, f4)
    val clipData = ClipData("label", arrayOf("image/jpeg", "image/png"), ClipData.Item(uri1))
    clipData.addItem(ClipData.Item(uri2))
    clipData.addItem(ClipData.Item(uri3))
    clipData.addItem(ClipData.Item(uri4))
    return clipData
  }
  fun onCopyFileMulti(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFileMulti())
  }

  private fun getClipDataFilePng() : ClipData {
    val f = File(getExternalFilesDir(null), "PNG_Test.png")
    val uri = getUriForFile(this, AUTHORITY, f)
    return ClipData.newUri(contentResolver, "label", uri)
  }
  fun onCopyFilePng(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFilePng())
  }

  private fun getClipDataFilePdf() : ClipData {
    val f = File(getExternalFilesDir(null), "pdf-test.pdf")
    val uri = getUriForFile(this, AUTHORITY, f)
    return ClipData.newUri(contentResolver, "label", uri)
  }
  fun onCopyFilePdf(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(getClipDataFilePdf())
  }

  fun onPaste(v: View) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = findViewById<EditText>(R.id.inspect)
    if (!clipboard.hasPrimaryClip()) {
      text.setText("=== !clipboard.hasPrimaryClip() ===")
      return
    }
    inspect("paste", clipboard.primaryClipDescription!!, clipboard.primaryClip!!)
  }

  fun inspect(event: String, clipDescription: ClipDescription?, clipData: ClipData?) {
    val text = findViewById<EditText>(R.id.inspect)
    val mimeTypeCount = clipDescription?.mimeTypeCount ?: -1
    val itemCount = clipData?.itemCount ?: -1
    var output = "event=" + event + ", descriptionCount=" + mimeTypeCount + "\nmime=";
    for (i in 0 until mimeTypeCount) {
      output += clipDescription!!.getMimeType(i) + ":"
    }
    output += "\nitemCount=" + itemCount;
    for (i in 0 until itemCount) {
      val item = clipData!!.getItemAt(i)!!
      Log.i(TAG, "uri=" + item.uri)
      output +=
        "\n" +
                i.toString() +
                " : text=" +
                item.text +
                ", html=" +
                item.htmlText +
                ", uri=" +
                item.uri +
                "\ncontent:\n"
      if (item.uri != null) {
        val inputStream = contentResolver.openInputStream(item.uri)
        val buf = ByteArray(1024)
        val n = inputStream?.read(buf)
        output += String(buf, 0, n!!)
        inputStream.close()
      }
    }
    text.setText(output)
  }
}
