package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIAnalysis
import com.example.ui.components.AnalysisCard
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScreenshotAnalysisScreen(
    analysis: AIAnalysis?,
    isLoading: Boolean,
    onAnalyzeBitmap: (Bitmap, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedLabel by remember { mutableStateOf("No image selected") }

    // Android Photo Picker (zero permission required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    selectedLabel = "User Uploaded Chart Screenshot"
                }
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Section Title
        Text(
            text = "LOCAL SCREENSHOT ANALYSIS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = CyanPrimary,
                letterSpacing = 1.sp
            )
        )
        Text(
            text = "Upload any chart screenshot from TradingView or an exchange. Gemini Vision analyzes patterns, support/resistance, and candlestick structure independently of live feeds.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                lineHeight = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Image Selection / Preview Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                    ) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Chart Screenshot Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Select Image",
                                tint = CyanPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap to choose a chart screenshot from Gallery",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "PNG, JPG, or WEBP chart screenshots",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gallery picker button
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pick_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedBitmap == null) "Browse Device Gallery" else "Change Screenshot",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Preset Sample Charts
        Text(
            text = "OR TEST WITH SAMPLE PATTERN SCREENSHOTS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SamplePatternButton(
                title = "BTC Bull Flag",
                patternType = "bull_flag",
                modifier = Modifier.weight(1f)
            ) { bmp ->
                selectedBitmap = bmp
                selectedLabel = "Sample Chart: BTC Bull Flag Breakout"
            }
            SamplePatternButton(
                title = "ETH Double Bottom",
                patternType = "double_bottom",
                modifier = Modifier.weight(1f)
            ) { bmp ->
                selectedBitmap = bmp
                selectedLabel = "Sample Chart: ETH Double Bottom Reversal"
            }
            SamplePatternButton(
                title = "SOL Asc. Triangle",
                patternType = "triangle",
                modifier = Modifier.weight(1f)
            ) { bmp ->
                selectedBitmap = bmp
                selectedLabel = "Sample Chart: SOL Ascending Triangle"
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Run Analysis Button
        Button(
            onClick = {
                selectedBitmap?.let { bmp ->
                    onAnalyzeBitmap(bmp, selectedLabel)
                }
            },
            enabled = selectedBitmap != null && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("analyze_screenshot_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini Vision Analyzing...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analyze Screenshot with Gemini Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Result Card
        if (analysis != null) {
            AnalysisCard(analysis = analysis)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SamplePatternButton(
    title: String,
    patternType: String,
    modifier: Modifier = Modifier,
    onGenerate: (Bitmap) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .clickable {
                val sampleBitmap = createSampleChartBitmap(title, patternType)
                onGenerate(sampleBitmap)
            }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontSize = 11.sp
            )
        )
    }
}

// Generates a mock chart bitmap locally so the user can test Gemini Vision immediately
private fun createSampleChartBitmap(title: String, patternType: String): Bitmap {
    val width = 640
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint().apply { color = AndroidColor.parseColor("#0F172A") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Grid lines
    val gridPaint = Paint().apply {
        color = AndroidColor.parseColor("#1E293B")
        strokeWidth = 1f
    }
    for (y in 50 until height step 60) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    // Title
    val textPaint = Paint().apply {
        color = AndroidColor.parseColor("#F8FAFC")
        textSize = 24f
        isAntiAlias = true
        isFakeBoldText = true
    }
    canvas.drawText("$title (4H Horizon)", 30f, 40f, textPaint)

    // Draw stylized candles
    val greenPaint = Paint().apply { color = AndroidColor.parseColor("#00E676") }
    val redPaint = Paint().apply { color = AndroidColor.parseColor("#FF5252") }
    val wickPaint = Paint().apply { strokeWidth = 2f }

    val points = when (patternType) {
        "bull_flag" -> listOf(280, 220, 160, 120, 140, 130, 150, 135, 120, 90, 60)
        "double_bottom" -> listOf(140, 200, 260, 210, 180, 210, 260, 200, 140, 90, 70)
        else -> listOf(240, 220, 190, 210, 160, 180, 140, 150, 130, 110, 80)
    }

    val candleWidth = 28f
    val gap = 20f
    var startX = 50f

    points.forEachIndexed { i, p ->
        val prev = if (i > 0) points[i - 1] else p + 10
        val isBull = p <= prev
        val paint = if (isBull) greenPaint else redPaint
        wickPaint.color = paint.color

        val top = minOf(p, prev).toFloat()
        val bottom = maxOf(p, prev).toFloat()
        val wickHigh = (top - 18).coerceAtLeast(45f)
        val wickLow = (bottom + 18).coerceAtMost(height - 40f)

        // Wick
        canvas.drawLine(startX + candleWidth / 2f, wickHigh, startX + candleWidth / 2f, wickLow, wickPaint)
        // Body
        canvas.drawRect(startX, top, startX + candleWidth, bottom + 4f, paint)

        startX += candleWidth + gap
    }

    return bitmap
}
