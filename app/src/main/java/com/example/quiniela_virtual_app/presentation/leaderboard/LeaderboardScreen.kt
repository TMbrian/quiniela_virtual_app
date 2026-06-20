package com.example.quiniela_virtual_app.presentation.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.quiniela_virtual_app.domain.model.PosicionLeaderboard
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import com.example.quiniela_virtual_app.presentation.shared.UiState
import com.example.quiniela_virtual_app.presentation.shared.components.ErrorMessage
import com.example.quiniela_virtual_app.presentation.shared.components.LoadingIndicator

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    when (val estado = uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error   -> ErrorMessage(estado.mensaje)
        is UiState.Success -> LeaderboardList(estado.data)
    }
}

@Composable
private fun LeaderboardList(posiciones: List<PosicionLeaderboard>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(posiciones, key = { _, p -> p.uid }) { index, posicion ->
            PosicionItem(posicion = posicion, numero = index + 1)
        }
    }
}

@Composable
private fun PosicionItem(posicion: PosicionLeaderboard, numero: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$numero", modifier = Modifier.width(32.dp))
        Text(posicion.nombre, modifier = Modifier.weight(1f))
        Text("${posicion.puntosTotales} pts")
    }
}

@Preview(showBackground = true, name = "Leaderboard")
@Composable
private fun LeaderboardPreview() {
    QuinielaTheme {
        LeaderboardList(
            posiciones = listOf(
                PosicionLeaderboard("u1", "Brian Tzuc", null, 12, 3, 3, 6, emptyMap()),
                PosicionLeaderboard("u2", "Ana García", null, 9, 2, 3, 5, emptyMap()),
                PosicionLeaderboard("u3", "Carlos López", null, 6, 1, 3, 4, emptyMap()),
            )
        )
    }
}
