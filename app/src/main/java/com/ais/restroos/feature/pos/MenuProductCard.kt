package com.ais.restroos.feature.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ais.restroos.core.theme.DarkNavy
import com.ais.restroos.core.theme.RoyalBlue
import com.ais.restroos.domain.model.POSMenuItem
import java.text.DecimalFormat

@Composable
fun MenuProductCard(
    item: POSMenuItem,
    quantity: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priceText = if (item.variants.isNotEmpty()) {
        val minP = item.variants.minOf { it.price }
        if (minP % 1.0 == 0.0) "From ₹${minP.toInt()}" else "From ₹${DecimalFormat("#,##0.00").format(minP)}"
    } else if (item.price % 1.0 == 0.0) {
        "₹${item.price.toInt()}"
    } else {
        "₹${DecimalFormat("#,##0.00").format(item.price)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (quantity == 0) onAddToCart() else onIncrement()
            }
    ) {
        // Image Container Card with Price Badge & Add/Quantity Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Product Image or Stylized Placeholder
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when ((Math.abs(item.id.hashCode()) % 4)) {
                                    0 -> Color(0xFF332115)
                                    1 -> Color(0xFF6E4018)
                                    2 -> Color(0xFF2A3D29)
                                    else -> Color(0xFF4A1F26)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Top-Right Translucent Price Badge
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 8.dp, topEnd = 12.dp, bottomEnd = 0.dp),
                    color = Color(0x99000000),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Bottom-Right Add / Quantity Selector Button
                if (quantity == 0) {
                    // White Circular/Square Add (+) Button
                    Surface(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to cart",
                                tint = RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Quantity Selector Pill: [-] qty [+]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoyalBlue,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onDecrement() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onIncrement() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Product Name Below Image
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}