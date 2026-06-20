package com.example.quiniela_virtual_app.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quiniela_virtual_app.domain.model.EstadoPartido

@Composable
fun EstadoBadge(estado: EstadoPartido) {
    val (etiqueta, color) = badgeData(estado)
    Text(
        text = etiqueta,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun badgeData(estado: EstadoPartido): Pair<String, Color> = when (estado) {
    EstadoPartido.PROGRAMADO -> "Programado" to Color(0xFF9E9E9E)
    EstadoPartido.EN_VIVO    -> "En vivo"    to Color(0xFF4CAF50)
    EstadoPartido.FINALIZADO -> "Finalizado" to Color(0xFF2196F3)
    EstadoPartido.SUSPENDIDO -> "Suspendido" to Color(0xFFF44336)
}
