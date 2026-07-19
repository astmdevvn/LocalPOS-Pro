package com.localpos.pro.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localpos.pro.BuildConfig
import com.localpos.pro.R

@Composable
fun AslAboutDialog(visible: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val ink = Color(0xFF1D2747)
    val blue = Color(0xFF2998EE)

    BackHandler(enabled = visible, onBack = onClose)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA1D2747)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(horizontal = 26.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.localpos_icon),
                    contentDescription = "LocalPOS Pro",
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "LOCALPOS PRO",
                    color = ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = "Ứng dụng bán hàng và quản lý shop nhỏ, giúp quét mã, thanh toán, theo dõi tồn kho và công nợ ngay trên điện thoại.",
                    color = Color(0xFF657087),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Dữ liệu chỉ được lưu trên thiết bị của bạn. LocalPOS Pro không tự động tải dữ liệu bán hàng lên máy chủ.",
                    color = Color(0xFF176B4D),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(7.dp))
                Text("Phiên bản ${BuildConfig.VERSION_NAME}", color = Color(0xFF8A93A5), fontSize = 12.sp)

                Spacer(Modifier.height(18.dp))

                Image(
                    painter = painterResource(R.drawable.asl_logo),
                    contentDescription = "ASLDEV.VN",
                    modifier = Modifier.fillMaxWidth(0.70f).height(82.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Copyright: © 2026 ", color = Color(0xFF657087), fontSize = 14.sp)
                    Text(
                        text = "ASLDEV.VN",
                        color = blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://asldev.vn")))
                            }
                            .padding(horizontal = 3.dp, vertical = 5.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = blue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Quay lại", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}
