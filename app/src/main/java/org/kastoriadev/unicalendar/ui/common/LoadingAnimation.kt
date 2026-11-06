package org.kastoriadev.unicalendar.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R

/**
 * Very small, elegant startup loading screen featuring the app icon
 * with a subtle breathing pulse and a 3-dot wave animation.
 */
@Composable
fun StartupLoadingScreen(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StartupIconPulse")
    
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IconScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(92.dp)
                    .scale(iconScale)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            PulsingDotsLoading(
                dotColor = MaterialTheme.colorScheme.primary,
                dotSize = 7.dp
            )
        }
    }
}

/**
 * A small, lightweight animated 3-dot wave loading indicator.
 */
@Composable
fun PulsingDotsLoading(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 6.dp,
    spacing: Dp = 5.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotsWave")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot1Offset"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot2Offset"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot3Offset"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

/**
 * A small rotating sync icon indicator for top bar actions.
 */
@Composable
fun RotatingSyncLoading(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SyncRotationAngle"
    )

    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Sync,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}
