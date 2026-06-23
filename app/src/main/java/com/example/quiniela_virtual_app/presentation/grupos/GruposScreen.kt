package com.example.quiniela_virtual_app.presentation.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.quiniela_virtual_app.domain.model.PosicionGrupo
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator
import com.example.quiniela_virtual_app.presentation.shared.enEspañol

private val COL_PJ  = 28.dp
private val COL_STAT = 24.dp
private val COL_PTS  = 34.dp

@Composable
fun GruposScreen(viewModel: GruposViewModel = hiltViewModel()) {
    val tablas by viewModel.tablas.collectAsState()
    when (val estado = tablas) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error   -> ErrorMessage(estado.mensaje)
        is UiState.Success -> if (estado.data.isEmpty()) EstadoVacio() else TablaLista(estado.data)
    }
}

@Composable
private fun TablaLista(tablas: Map<String, List<PosicionGrupo>>) {
    LazyColumn(Modifier.fillMaxSize()) {
        tablas.forEach { (grupo, posiciones) ->
            stickyHeader(key = "hdr_$grupo") { GrupoHeader(grupo) }
            item(key = "cols_$grupo") { FilaEncabezados() }
            itemsIndexed(
                items = posiciones,
                key = { i, pos -> "${grupo}_${pos.equipo.nombre}_$i" },
            ) { index, pos ->
                FilaPosicion(index + 1, pos)
                if (index < posiciones.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun GrupoHeader(grupo: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Grupo $grupo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FilaEncabezados() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Equipo",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        ColEncabezado("PJ",  COL_PJ)
        ColEncabezado("G",   COL_STAT)
        ColEncabezado("E",   COL_STAT)
        ColEncabezado("P",   COL_STAT)
        ColEncabezado("PTS", COL_PTS)
    }
}

@Composable
private fun ColEncabezado(texto: String, ancho: Dp) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(ancho),
    )
}

@Composable
private fun FilaPosicion(pos: Int, posicion: PosicionGrupo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$pos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(18.dp),
            )
            SubcomposeAsyncImage(
                model = posicion.equipo.crestUrl,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
                error = {
                    Text(
                        text = posicion.equipo.iso.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
            Text(
                text = posicion.equipo.nombre.enEspañol(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ColDato(posicion.pj.toString(),  COL_PJ)
        ColDato(posicion.g.toString(),   COL_STAT)
        ColDato(posicion.e.toString(),   COL_STAT)
        ColDato(posicion.p.toString(),   COL_STAT)
        ColDato(
            texto = posicion.pts.toString(),
            ancho = COL_PTS,
            bold  = true,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ColDato(
    texto: String,
    ancho: Dp,
    bold: Boolean = false,
    color: Color = Color.Unspecified,
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(ancho),
    )
}

@Composable
private fun EstadoVacio() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Los resultados de grupo aparecerán\naquí una vez finalicen partidos.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(32.dp),
        )
    }
}
