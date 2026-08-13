package com.myhomechores.app.features.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TryActivitiesContent(
    onOpenEnglish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var unavailableActivity by remember { mutableStateOf<TryActivity?>(null) }
    unavailableActivity?.let { activity ->
        AlertDialog(
            onDismissRequest = { unavailableActivity = null },
            title = { Text(activity.title) },
            text = { Text("Раздел скоро появится") },
            confirmButton = {
                TextButton(onClick = { unavailableActivity = null }) { Text("Хорошо") }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Попробуй новое", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Выбери занятие по настроению. Оно не влияет на обязательные дела.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        tryActivities.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { activity ->
                    TryActivityCard(
                        activity = activity,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (activity.available) onOpenEnglish() else unavailableActivity = activity
                        },
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TryActivityCard(
    activity: TryActivity,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(154.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.available) Color(0xFFD9F5EA) else Color(0xFFE8DEFF),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(48.dp).background(
                    color = if (activity.available) MaterialTheme.colorScheme.primary else Color(0xFF7653C9),
                    shape = CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(activity.marker, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            Spacer(Modifier.height(9.dp))
            Text(activity.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                activity.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
