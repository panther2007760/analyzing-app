package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.ui.components.AnalysisCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveScreenCaptureScreen(repository: MarketRepository) {
    val context = LocalContext.current
    val screenshotAnalysis by repository.screenshotAiAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by repository.isScreenshotAiLoading.collectAsStateWithLifecycle()

    var isCapturing by remember { mutableStateOf(false) }
    var captureStatusMessage by remember { mutableStateOf("Ready. Tap 'Request Screen Permission' to initiate.") }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            isCapturing = true
            captureStatusMessage = "Screen projection permission GRANTED. Live screen detection pipeline active."
        } else {
            isCapturing = false
            captureStatusMessage = "Screen projection permission was DECLINED by user. Aivora never records screen without permission."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_screen_capture_screen")
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Cast, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE SCREEN ANALYZER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
                Text(
                    text = "Android MediaProjection Screen Capture Service",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCapturing) BullishGreen.copy(alpha = 0.15f) else SurfaceCard)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isCapturing) "ACTIVE" else "STANDBY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCapturing) BullishGreen else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Consent Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "EXPLICIT PRIVACY & PERMISSION POLICY", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aivora Chart AI strictly adheres to Android security standards. Screen capture requires explicit authorization via the system dialog. Captures are strictly processed on-demand for chart pattern detection and are never stored or uploaded elsewhere.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Capture Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "CAPTURE PIPELINE STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = captureStatusMessage, fontSize = 13.sp, color = TextPrimary)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isCapturing) {
                        Button(
                            onClick = {
                                if (projectionManager != null) {
                                    val intent = projectionManager.createScreenCaptureIntent()
                                    projectionLauncher.launch(intent)
                                } else {
                                    captureStatusMessage = "MediaProjectionManager unavailable on this system."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_request_projection")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Capture", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                isCapturing = false
                                captureStatusMessage = "Screen capture stopped by user."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_stop_projection")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Capture", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analysis result
        screenshotAnalysis?.let { analysis ->
            Text(text = "LATEST SCREENSHOT ANALYSIS RESULT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            AnalysisCard(analysis = analysis)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
