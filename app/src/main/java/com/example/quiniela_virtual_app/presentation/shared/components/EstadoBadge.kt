package com.example.quiniela_virtual_app.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quiniela_virtual_app.domain.model.EstadoPartido

@Composable
fun EstadoBadge(estado: EstadoPartido) {
    val (etiqueta, fondo, texto) = badgeData(estado)
    Text(
        text = etiqueta,
        color = texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color = fondo, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private data class BadgeEstilo(val etiqueta: String, val fondo: Color, val texto: Color)

private fun badgeData(estado: EstadoPartido): BadgeEstilo = when (estado) {
    EstadoPartido.PROGRAMADO -> BadgeEstilo("Prog",    Color(0xFF546E7A), Color.White)
    EstadoPartido.EN_VIVO    -> BadgeEstilo("En vivo", Color(0xFF1B8A3B), Color.White)
    EstadoPartido.FINALIZADO -> BadgeEstilo("Final",   Color(0xFF1565C0), Color.White)
    EstadoPartido.SUSPENDIDO -> BadgeEstilo("Susp",    Color(0xFFC62828), Color.White)
}
